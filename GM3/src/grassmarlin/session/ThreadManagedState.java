package grassmarlin.session;

import grassmarlin.Event;
import grassmarlin.RuntimeConfiguration;
import grassmarlin.common.ReferenceSet;
import javafx.collections.ObservableList;

import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

public abstract class ThreadManagedState {
    private final static Object objAll = new Object();

    private final AtomicBoolean isUpdatePending;
    private final ReferenceSet<Object> flags;
    private final long msThreshold;
    private long msLastCalled;
    private final String txtName;
    //HACK: Needed to reference this in some loading code and making executionProvider public was the easiest way to accomplish that.
    public final Event.IAsyncExecutionProvider executionProvider;

    protected ThreadManagedState(final long msThreshold, final String txtName, final Event.IAsyncExecutionProvider executionProvider) {
        this.isUpdatePending = new AtomicBoolean(false);
        this.flags = new ReferenceSet<>();
        this.msThreshold = msThreshold;
        this.msLastCalled = 0;
        this.txtName = txtName;
        this.executionProvider = executionProvider;
    }
    protected ThreadManagedState() {
        this(RuntimeConfiguration.UPDATE_INTERVAL_MS, null, Event.PROVIDER_IN_THREAD);
    }

    public final void invalidate() {
        synchronized(this.flags) {
            this.flags.clear();
            this.flags.add(objAll);
            if (!isUpdatePending.getAndSet(true)) {
                final long msNow = System.currentTimeMillis();
                if(msNow >= this.msLastCalled + this.msThreshold) {
                    this.executionProvider.runLater(this::process);
                } else {
                    new Thread(() -> {
                        try {
                            Thread.sleep(this.msLastCalled + this.msThreshold - msNow);
                        } catch(InterruptedException ex) {

                        }
                        ThreadManagedState.this.executionProvider.runLater(ThreadManagedState.this::process);
                    }).start();
                }
            }
        }
    }

    public final void invalidate(final Object flag) {
        this.invalidate(flag, (Runnable)null);
    }
    public final void invalidate(final Object flag, final Supplier<Boolean> task) {
        synchronized(this.flags) {
            synchronized(flag) {
                if(task != null) {
                    if(!task.get()) {
                        return;
                    }
                }
                if (this.flags.add(flag) && !isUpdatePending.getAndSet(true)) {
                    final long msNow = System.currentTimeMillis();
                    if (msNow >= this.msLastCalled + this.msThreshold) {
                        this.executionProvider.runLater(this::process);
                    } else {
                        new Thread(() -> {
                            try {
                                Thread.sleep(this.msLastCalled + this.msThreshold - msNow);
                            } catch (InterruptedException ex) {

                            }
                            ThreadManagedState.this.executionProvider.runLater(ThreadManagedState.this::process);
                        }).start();
                    }
                }
            }
        }
    }
    public final void invalidate(final Object flag, final Runnable task) {
        synchronized(this.flags) {
            synchronized(flag) {
                this.flags.add(flag);
                if(task != null) {
                    try {
                        task.run();
                    } catch(Exception ex) {
                        ex.printStackTrace();
                    }
                }
                if (!isUpdatePending.getAndSet(true)) {
                    final long msNow = System.currentTimeMillis();
                    if (msNow >= this.msLastCalled + this.msThreshold) {
                        this.executionProvider.runLater(this::process);
                    } else {
                        new Thread(() -> {
                            try {
                                Thread.sleep(this.msLastCalled + this.msThreshold - msNow);
                            } catch (InterruptedException ex) {

                            }
                            this.executionProvider.runLater(ThreadManagedState.this::process);
                        }).start();
                    }
                }
            }
        }
    }

    public void waitForValid() {
        while(isUpdatePending.get()) {
            if(executionProvider.isExecutionThread()) {
                this.process();
            } else {
                try {
                    Thread.sleep(1);
                } catch (InterruptedException ex) {
                    return;
                }
            }
        }
    }

    public boolean isValid() {
        return !isUpdatePending.get();
    }

    private void process() {
        synchronized(this.flags) {
            this.msLastCalled = System.currentTimeMillis();
            try {
                this.validate();
            } finally {
                this.flags.clear();
                this.isUpdatePending.set(false);
            }
            final long msDuration = System.currentTimeMillis() - this.msLastCalled;
            if(msDuration > this.msThreshold) {
                //System.out.println(String.format("[%s] executed in %d ms", this.txtName, msDuration));
            }
        }
    }

    protected final boolean hasFlag(final Object flag) {
        //HACK: We use a lot of collections as flags and we need to prevent writes to those collections while checking for the flag.  This tends to fit the model.
        synchronized(flag) {
            return flags.contains(flag) || flags.contains(objAll);
        }
    }

    protected abstract void validate();

    protected static <T> void rebuildList(final ObservableList<T> list, final List<T> values) {
        if(values == null) {
            list.clear();
        } else {
            list.retainAll(values);
            values.removeAll(list);
            list.addAll(values);
        }
    }

}
