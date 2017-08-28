package grassmarlin;

import grassmarlin.ui.common.tasks.SynchronousPlatform;
import javafx.application.Platform;

import java.lang.ref.WeakReference;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

public class Event<TArgs> {
    @FunctionalInterface
    public interface EventListener<TArgs> {
        void handle(Event<TArgs> source, TArgs arguments);
    }

    public interface IAsyncExecutionProvider {
        void runNow(final Runnable task);
        void runLater(final Runnable task);
        boolean isExecutionThread();
    }

    public static IAsyncExecutionProvider PROVIDER_IN_THREAD = new IAsyncExecutionProvider() {
        @Override
        public void runNow(final Runnable task) {
            task.run();
        }

        @Override
        public void runLater(final Runnable task) {
            task.run();
        }

        @Override
        public boolean isExecutionThread() {
            return true;
        }
    };
    public static IAsyncExecutionProvider PROVIDER_JAVAFX = new IAsyncExecutionProvider() {
        @Override
        public void runNow(final Runnable task) {
            SynchronousPlatform.runNow(task);
        }

        @Override
        public void runLater(final Runnable task) {
            Platform.runLater(task);
        }

        @Override
        public boolean isExecutionThread() {
            return Platform.isFxApplicationThread();
        }
    };
    private final static AtomicInteger nextIndex = new AtomicInteger(0);
    public static IAsyncExecutionProvider createThreadQueueProvider() {
        return new IAsyncExecutionProvider() {
            private final static int CAPACITY = 1000;
            private final ArrayBlockingQueue<Runnable> queue = new ArrayBlockingQueue<>(CAPACITY);
            private final Thread threadProcess = new Thread(() -> {
                while(true) {
                    Runnable taskNext = queue.poll();
                    if(taskNext != null) {
                        taskNext.run();
                    } else {
                        try {
                            Thread.sleep(1);
                        } catch(InterruptedException ex) {
                            //Ignore
                        }
                    }
                }
            });

            private void startThread() {
                if(!threadProcess.isAlive()) {
                    threadProcess.setDaemon(true);
                    threadProcess.setName(String.format("ThreadQueueProvider #%d", nextIndex.getAndIncrement()));
                    threadProcess.start();
                }
            }

            @Override
            public void runNow(final Runnable task) {
                if (isExecutionThread()) {
                    task.run();
                } else {
                    final CountDownLatch doneLatch = new CountDownLatch(1);
                    queue.add(() -> {
                        try {
                            task.run();
                        } finally {
                            doneLatch.countDown();
                        }
                    });
                    this.startThread();

                    try {
                        doneLatch.await();
                    } catch (InterruptedException ex) {
                        //If interrupted just resume.
                    }
                }
            }

            @Override
            public void runLater(final Runnable task) {
                while(!queue.offer(task)) {
                    Thread.yield();
                }
                this.startThread();
            }

            @Override
            public boolean isExecutionThread() {
                return Thread.currentThread() == threadProcess;
            }
        };
    }

    private final CopyOnWriteArraySet<WeakReference<EventListener<TArgs>>> handlers;
    private final IAsyncExecutionProvider executor;

    public Event(IAsyncExecutionProvider executor) {
        this.handlers = new CopyOnWriteArraySet<>();
        this.executor = executor;
    }

    public synchronized EventListener<TArgs> addHandler(EventListener<TArgs> handler) {
        //Cull dead references
        handlers.removeIf(reference -> reference.get() == null);

        if(handler == null) {
            return null;
        }

        for(WeakReference<EventListener<TArgs>> reference : handlers) {
            final EventListener<TArgs> existing = reference.get();
            if(existing != null && existing.equals(handler)) {
                return existing;
            }
        }

        handlers.add(new WeakReference<>(handler));
        return handler;
    }

    public synchronized void removeHandler(final EventListener<TArgs> handler) {
        handlers.remove(handler);
    }

    public synchronized void clearHandlers() {
        handlers.clear();
    }

    public void call(final TArgs args) {
        //Cull dead references
        final int cntBefore = handlers.size();
        handlers.removeIf(reference -> reference.get() == null);
        final int cntAfter = handlers.size();
        if(cntBefore != cntAfter) {
            Logger.log(Logger.Severity.PEDANTIC_DEVELOPER_SPAM, "Dead handlers were culled: %d -> %d", cntBefore, cntAfter);
        }

        for(WeakReference<EventListener<TArgs>> reference : handlers) {
            final EventListener<TArgs> handler = reference.get();
            if(handler != null) {
                try {
                    if (this.executor.isExecutionThread()) {
                        handler.handle(this, args);
                    } else {
                        this.executor.runLater(() -> handler.handle(this, args));
                    }
                } catch(IllegalStateException ex) {
                    //Generally this means there is no JavaFx toolkit; probably running in console mode.
                    handler.handle(this, args);
                }
            }
        }
    }

    public void callProfiled(final String context, final TArgs args) {
        //Cull dead references
        try(Profiler.Block blockRoot = Profiler.start(context + "::root")) {
            try(Profiler.Block blockCull = Profiler.start(context + "::cull")) {
                final int cntBefore = handlers.size();
                handlers.removeIf(reference -> reference.get() == null);
                final int cntAfter = handlers.size();
                if (cntBefore != cntAfter) {
                    Logger.log(Logger.Severity.PEDANTIC_DEVELOPER_SPAM, "Dead handlers were culled: %d -> %d", cntBefore, cntAfter);
                }
            }

            for (WeakReference<EventListener<TArgs>> reference : handlers) {
                try(Profiler.Block blockIteration = Profiler.start(context + "::iterate::" + reference.toString())) {
                    final EventListener<TArgs> handler = reference.get();
                    if (handler != null) {
                        try {
                            if (this.executor.isExecutionThread()) {
                                try(Profiler.Block blockHandleInThread = Profiler.start(context + "::handleinthread")) {
                                    handler.handle(this, args);
                                }
                            } else {
                                try(Profiler.Block blockHandleLater = Profiler.start(context + "::handlelater")) {
                                    this.executor.runLater(() -> handler.handle(this, args));
                                }
                            }
                        } catch (IllegalStateException ex) {
                            //Generally this means there is no JavaFx toolkit; probably running in console mode.
                            try(Profiler.Block blockFail = Profiler.start(context + "::handleaserror")) {
                                handler.handle(this, args);
                            }
                        }
                    }
                }
            }
        }
    }
}
