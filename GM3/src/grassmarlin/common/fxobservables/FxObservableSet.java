package grassmarlin.common.fxobservables;

import com.sun.javafx.collections.SetListenerHelper;
import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.collections.ObservableSet;
import javafx.collections.SetChangeListener;

import java.util.Collection;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentSkipListSet;

/**
 * This is derived from the ObservableSetWrapper, except it fires events in the Fx thread.
 * @param <E>
 */
public class FxObservableSet<E> implements ObservableSet<E> {
    private final Set<E> backingSet;
    private SetListenerHelper<E> listenerHelper;

    public FxObservableSet() {
        this(new ConcurrentSkipListSet<>());
    }
    public FxObservableSet(final Set<E> set) {
        this.backingSet = set;
    }

    private class SimpleChange extends SetChangeListener.Change<E> {
        private final E element;
        private final boolean added;

        public SimpleChange(final E element, boolean wasAdded) {
            super(FxObservableSet.this);

            this.element = element;
            this.added = wasAdded;
        }

        @Override
        public boolean wasAdded() {
            return this.added;
        }
        @Override
        public boolean wasRemoved() {
            return !this.added;
        }
        @Override
        public E getElementAdded() {
            return this.added ? this.element : null;
        }
        @Override
        public E getElementRemoved() {
            return this.added ? null : this.element;
        }

        @Override
        public String toString() {
            return (added ? "added " : "removed ") + element;
        }
    }

    private void callObservers(SetChangeListener.Change<E> change) {
        try {
            if (!Platform.isFxApplicationThread()) {
                Platform.runLater(() -> SetListenerHelper.fireValueChangedEvent(listenerHelper, change));
            } else {
                SetListenerHelper.fireValueChangedEvent(listenerHelper, change);
            }
        } catch(IllegalStateException ex) {
            //Fx not initialized--possibly not an Fx UI mode, so just run in-thread
            SetListenerHelper.fireValueChangedEvent(listenerHelper, change);
        }
    }

    @Override
    public void addListener(final InvalidationListener listener) {
        listenerHelper = SetListenerHelper.addListener(listenerHelper, listener);
    }

    @Override
    public void removeListener(InvalidationListener listener) {
        listenerHelper = SetListenerHelper.removeListener(listenerHelper, listener);
    }

    @Override
    public void addListener(SetChangeListener<? super E> observer) {
        listenerHelper = SetListenerHelper.addListener(listenerHelper, observer);
    }

    @Override
    public void removeListener(SetChangeListener<? super E> observer) {
        listenerHelper = SetListenerHelper.removeListener(listenerHelper, observer);
    }

    @Override
    public int size() {
        return backingSet.size();
    }

    @Override
    public boolean isEmpty() {
        return backingSet.isEmpty();
    }

    @Override
    public boolean contains(final Object o) {
        return backingSet.contains(o);
    }

    @Override
    public Iterator<E> iterator() {
        return new Iterator<E>() {
            private final Iterator<E> backingIterator = backingSet.iterator();
            private E lastElement;

            @Override
            public boolean hasNext() {
                return backingIterator.hasNext();
            }

            @Override
            public E next() {
                lastElement = backingIterator.next();
                return lastElement;
            }

            @Override
            public void remove() {
                backingIterator.remove();
                callObservers(new SimpleChange(lastElement, false));
            }
        };
    }

    @Override
    public Object[] toArray() {
        return backingSet.toArray();
    }

    @Override
    public <T> T[] toArray(T[] a) {
        return backingSet.toArray(a);
    }

    @Override
    public synchronized boolean add(E o) {
        boolean ret = backingSet.add(o);
        if(ret) {
            callObservers(new SimpleChange(o, true));
        }
        return ret;
    }

    @Override
    @SuppressWarnings("unchecked")
    public synchronized boolean remove(Object o) {
        boolean ret = backingSet.remove(o);
        if(ret) {
            //Cast to E is safe since we removed o from the set.  There is always the possibility that o was designed to match an E without being one, but you're accepting this risk if you do that.
            callObservers(new SimpleChange((E)o, false));
        }
        return ret;
    }

    @Override
    public boolean containsAll(final Collection<?> c) {
        return backingSet.containsAll(c);
    }

    @Override
    public boolean addAll(final Collection<? extends E> c) {
        boolean ret = false;
        for(E element : c) {
            ret |= add(element);
        }
        return ret;
    }

    @Override
    public boolean retainAll(final Collection<?> c) {
        return removeRetain(c, false);
    }

    @Override
    public boolean removeAll(final Collection<?> c) {
        return removeRetain(c, true);
    }

    private synchronized boolean removeRetain(final Collection<?> c, final boolean remove) {
        boolean removed = false;
        for(Iterator<E> i = backingSet.iterator(); i.hasNext(); ) {
            final E element = i.next();
            if(remove == c.contains(element)) {
                removed = true;
                i.remove();
                callObservers(new SimpleChange(element, false));
            }
        }
        return removed;
    }

    @Override
    public synchronized void clear() {
        for(Iterator<E> i = backingSet.iterator(); i.hasNext(); ) {
            final E element = i.next();
            i.remove();
            callObservers(new SimpleChange(element, false));
        }
    }

    @Override
    public String toString() {
        return backingSet.toString();
    }

    @SuppressWarnings("EqualsWhichDoesntCheckParameterClass")
    @Override
    public boolean equals(Object obj) {
        return backingSet.equals(obj);
    }

    @Override
    public int hashCode() {
        return backingSet.hashCode();
    }
}
