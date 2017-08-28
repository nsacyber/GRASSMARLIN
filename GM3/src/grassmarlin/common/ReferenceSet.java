package grassmarlin.common;

import java.util.*;

public class ReferenceSet<T> implements Set<T> {
    private IdentityHashMap<T, T> backingMap;

    public ReferenceSet() {
        backingMap = new IdentityHashMap<>();
    }

    public boolean add(T val) {
        return backingMap.put(val, val) == null;
    }

    @Override
    public boolean remove(Object o) {
        return backingMap.remove(o) != null;
    }

    @Override
    public boolean containsAll(Collection<?> c) {
        return backingMap.keySet().containsAll(c);
    }

    @Override
    public boolean addAll(Collection<? extends T> c) {
        boolean result = false;
        for(T e : c) {
            result |= add(e);
        }
        return result;
    }

    @Override
    public boolean retainAll(Collection<?> c) {
        boolean result = false;
        final List<Object> keysToRemove = new ArrayList<>(backingMap.keySet());
        keysToRemove.removeAll(c);
        return this.removeAll(keysToRemove);
    }

    @Override
    public boolean removeAll(Collection<?> c) {
        boolean result = false;
        for(Object o : c) {
            result |= backingMap.remove(o) != null;
        }
        return result;
    }

    @Override
    public void clear() {
        backingMap.clear();
    }

    @Override
    public int size() {
        return backingMap.size();
    }

    @Override
    public boolean isEmpty() {
        return backingMap.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        return backingMap.containsKey(o);
    }

    @Override
    public Iterator<T> iterator() {
        return backingMap.keySet().iterator();
    }

    @Override
    public Object[] toArray() {
        return backingMap.keySet().toArray();
    }

    @Override
    public <T1> T1[] toArray(T1[] a) {
        return backingMap.keySet().toArray(a);
    }
}
