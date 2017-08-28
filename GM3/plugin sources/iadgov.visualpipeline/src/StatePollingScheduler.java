package iadgov.visualpipeline;

import javafx.beans.binding.Binding;

import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicBoolean;

public class StatePollingScheduler {
    private final Thread threadInvalidation;
    private final AtomicBoolean terminate;
    private final long interval = 41;   //~24 FPS

    private final Set<Binding<?>> bindings;

    public StatePollingScheduler() {
        this.terminate = new AtomicBoolean(false);
        this.bindings = new ConcurrentSkipListSet<>((o1, o2) -> Integer.compare(o1.hashCode(), o2.hashCode()));

        this.threadInvalidation = new Thread(this::Thread_Invalidate);
        this.threadInvalidation.setDaemon(true);
        this.threadInvalidation.setName("iadgov.visualpipeline.StatePollingScheduler Worker");
        this.threadInvalidation.start();
    }

    public void terminate() {
        this.terminate.set(true);
    }

    public void add(final Binding<?> target) {
        this.bindings.add(target);
    }

    private void Thread_Invalidate() {
        while(!terminate.get()) {
            try {
                Thread.sleep(this.interval);
            } catch(InterruptedException ex) {
                continue;
            }

            for(final Binding<?> binding : this.bindings) {
                binding.invalidate();
            }
        }
    }
}
