package prefuse.util.collections;

/**
 * Sorted map that maps from a boolean key to an int value.
 * 
 * @author <a href="http://jheer.org">jeffrey heer</a>
 */
public interface BooleanIntSortedMap extends IntSortedMap {

    public boolean firstKey();

    public boolean lastKey();

    public boolean containsKey(boolean key);
    
    public IntIterator valueRangeIterator(boolean fromKey, boolean fromInc, 
                                          boolean toKey,   boolean toInc);
    
    public LiteralIterator keyIterator();

    public LiteralIterator keyRangeIterator(boolean fromKey, boolean fromInc, 
                                            boolean toKey,   boolean toInc);

    public int get(boolean key);

    public int remove(boolean key);
    
    public int remove(boolean key, int value);

    public int put(boolean key, int value);
    
} // end of interface LongIntSortedMap
