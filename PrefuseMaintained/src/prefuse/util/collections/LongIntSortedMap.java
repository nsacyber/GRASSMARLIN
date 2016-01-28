package prefuse.util.collections;

/**
 * Sorted map that maps from a long key to an int value.
 * 
 * @author <a href="http://jheer.org">jeffrey heer</a>
 */
public interface LongIntSortedMap extends IntSortedMap {

    public long firstKey();

    public long lastKey();

    public boolean containsKey(long key);
    
    public IntIterator valueRangeIterator(long fromKey, boolean fromInc, 
                                          long toKey,   boolean toInc);
    
    public LiteralIterator keyIterator();

    public LiteralIterator keyRangeIterator(long fromKey, boolean fromInc, 
                                            long toKey,   boolean toInc);

    public int get(long key);

    public int remove(long key);
    
    public int remove(long key, int value);

    public int put(long key, int value);
    
} // end of interface LongIntSortedMap
