package grassmarlin.common.fxobservables;

import grassmarlin.Event;
import grassmarlin.RuntimeConfiguration;
import javafx.beans.binding.LongBinding;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public class LongAccumulator extends LongBinding {
    private final AtomicLong value;
    private final AtomicBoolean valid;
    private final Event.IAsyncExecutionProvider ui;

    public LongAccumulator(final Event.IAsyncExecutionProvider ui, final long initial) {
        this.value = new AtomicLong(initial);
        this.valid = new AtomicBoolean(true);
        this.ui = ui;
    }
    public LongAccumulator(final RuntimeConfiguration config, final long initial) {
        this.value = new AtomicLong(initial);
        this.valid = new AtomicBoolean(true);
        this.ui = config.getUiEventProvider();
    }

    public void set(final long value) {
        this.value.set(value);
        if(this.valid.getAndSet(false)) {
            this.ui.runLater(this::invalidate);
        }
    }
    public void increment(final long value) {
        this.value.addAndGet(value);
        if(this.valid.getAndSet(false)) {
            this.ui.runLater(this::invalidate);
        }
    }

    @Override
    protected long computeValue() {
        this.valid.set(true);
        return value.get();
    }
}
