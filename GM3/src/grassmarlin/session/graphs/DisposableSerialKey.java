package grassmarlin.session.graphs;

import java.util.concurrent.atomic.AtomicLong;

public class DisposableSerialKey implements IHasKey {
    private final static AtomicLong idNext = new AtomicLong(0);

    private final long id;

    public DisposableSerialKey() {
        this.id = idNext.getAndIncrement();
    }

    @Override
    public String getKey() {
        return Long.toString(id);
    }
}
