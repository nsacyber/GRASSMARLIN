package grassmarlin.common;

import java.util.LinkedList;

public class LengthCappedList<T> extends LinkedList<T> {
    private final int limit;

    public LengthCappedList(final int limit) {
        this.limit = limit;
    }

    public LengthCappedList(final int limit, final T defaultValue) {
        this(limit);

        for(int idx = 0; idx < limit; idx++) {
            super.add(defaultValue);
        }
    }

    @Override
    public synchronized boolean add(T object) {
        super.add(object);
        if(this.size() > limit) {
            this.remove(0);
        }
        return true;
    }
}
