package iadgov.visualpipeline;

import javafx.beans.binding.DoubleBinding;

import java.util.concurrent.BlockingQueue;

public class QueueStateDoubleBinding extends DoubleBinding {
    private final BlockingQueue queue;

    public QueueStateDoubleBinding(final BlockingQueue<?> queue, final StatePollingScheduler scheduler) {
        this.queue = queue;

        scheduler.add(this);
    }

    @Override
    public double computeValue() {
        if(this.queue == null) {
            return -1.0;
        } else {
            final double capacity = (double) queue.remainingCapacity() + (double) queue.size();
            return (double) queue.size() / capacity;
        }
    }
}
