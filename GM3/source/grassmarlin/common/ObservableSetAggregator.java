package grassmarlin.common;

import com.sun.javafx.collections.ObservableSetWrapper;
import grassmarlin.Logger;
import javafx.collections.ObservableSet;
import javafx.collections.SetChangeListener;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;

public class ObservableSetAggregator<TElement> {
    private final Map<TElement, AtomicInteger> referenceCounts;
    private final ObservableSet<TElement> aggregate;
    private final List<ObservableSet<TElement>> components;

    public ObservableSetAggregator() {
        this.referenceCounts = new HashMap<>();
        this.aggregate = new ObservableSetWrapper<>(new HashSet<>());
        this.components = new LinkedList<>();
    }

    public ObservableSet<TElement> getAggregate() {
        return this.aggregate;
    }

    public void listenTo(final ObservableSet<TElement> set) {
        for(final TElement element : set) {
            final AtomicInteger count = referenceCounts.get(element);
            if(count == null) {
                referenceCounts.put(element, new AtomicInteger(1));
            } else {
                count.incrementAndGet();
            }
            this.aggregate.add(element);
        }
        components.add(set);
        set.addListener(this.handler_setChanged);
    }

    public void stopListeningTo(final ObservableSet<TElement> set) {
        if(components.remove(set)) {
            set.removeListener(this.handler_setChanged);
            for(final TElement element : set) {
                final int references = this.referenceCounts.get(element).decrementAndGet();
                if(references < 0) {
                    Logger.log(Logger.Severity.ERROR, "Reference counting has detected a race condition resulting in a negative count [%s in %s, while removing %s]", element, this, set);
                    this.referenceCounts.get(element).set(0);
                    this.aggregate.remove(element);
                } else if(references == 0) {
                    this.aggregate.remove(element);
                }
            }
        }
    }

    private final SetChangeListener<TElement> handler_setChanged = this::handle_setChanged;
    private void handle_setChanged(SetChangeListener.Change<? extends TElement> change) {
        if(change.wasRemoved()) {
            if(0 == this.referenceCounts.get(change.getElementRemoved()).decrementAndGet()) {
                this.aggregate.remove(change.getElementRemoved());
            }
        }
        if(change.wasAdded()) {
            final AtomicInteger count = this.referenceCounts.get(change.getElementAdded());
            if(count == null) {
                this.referenceCounts.put(change.getElementAdded(), new AtomicInteger(1));
                this.aggregate.add(change.getElementAdded());
            } else {
                if(count.incrementAndGet() == 1) {
                    this.aggregate.add(change.getElementAdded());
                }
            }
        }
    }
}
