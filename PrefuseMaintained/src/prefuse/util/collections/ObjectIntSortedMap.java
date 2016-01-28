package prefuse.util.collections;

import java.util.Iterator;

/**
 * Sorted map that maps from an Object key to an int value.
 * 
 * @author <a href="http://jheer.org">jeffrey heer</a>
 */
public interface ObjectIntSortedMap extends IntSortedMap {

    public static final Object MAX_KEY = new Object();
    public static final Object MIN_KEY = new Object();
    
    public Object firstKey();

    public Object lastKey();

    public boolean containsKey(Object key);
    
    public IntIterator valueRangeIterator(Object fromKey, boolean fromInc, 
                                          Object toKey,   boolean toInc);
    
    public Iterator keyIterator();

    public Iterator keyRangeIterator(Object fromKey, boolean fromInc, 
                                     Object toKey,   boolean toInc);

    public int get(Object key);

    public int remove(Object key);
    
    public int remove(Object key, int val);

    public int put(Object key, int value);
    
} // end of interface ObjectIntSortedMap
