package grassmarlin.common.fxobservables;

import grassmarlin.Event;
import javafx.beans.binding.DoubleBinding;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;

public class QueueStateBinding extends DoubleBinding {
    private final BlockingQueue<Object> queue;
    private final AtomicBoolean valid;
    private final Event.IAsyncExecutionProvider uiProvider;

    public QueueStateBinding(final Event.IAsyncExecutionProvider ui, final BlockingQueue<Object> queue) {
        this.queue = queue;
        this.valid = new AtomicBoolean(true);
        this.uiProvider = ui;
    }

    public void clear() {
        this.queue.clear();
        if(this.valid.getAndSet(false)) {
            this.uiProvider.runLater(this::invalidate);
        }
    }
    public Object take() throws InterruptedException {
        final Object result = this.queue.take();
        if(this.valid.getAndSet(false)) {
            this.uiProvider.runLater(this::invalidate);
        }
        return result;
    }
    public boolean offer(final Object o) {
        final boolean result = this.queue.offer(o);
        if(this.valid.getAndSet(false)) {
            this.uiProvider.runLater(this::invalidate);
        }
        return result;
    }

    @Override
    protected double computeValue() {
        this.valid.getAndSet(true);
        if(this.queue == null) {
            return -1.0;
        } else {
            return (double) this.queue.size() / ((double) this.queue.remainingCapacity() + (double) this.queue.size());
        }
    }
}
