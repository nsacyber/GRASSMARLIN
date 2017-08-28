package iadgov.visualpipeline;

import javafx.beans.binding.LongBinding;

import java.util.concurrent.atomic.AtomicLong;

public class AtomicLongBinding extends LongBinding {
    private final AtomicLong value;

    public AtomicLongBinding(final AtomicLong value, final StatePollingScheduler scheduler) {
        this.value = value;

        scheduler.add(this);
    }

    @Override
    public long computeValue() {
        return this.value.get();
    }
}
