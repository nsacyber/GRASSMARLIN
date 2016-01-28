package prefuse.util.collections;


/**
 * Sorted map that maps from a float key to an int value.
 * 
 * @author <a href="http://jheer.org">jeffrey heer</a>
 */
public interface FloatIntSortedMap extends IntSortedMap {

    public float firstKey();

    public float lastKey();

    public boolean containsKey(float key);
    
    public IntIterator valueRangeIterator(float fromKey, boolean fromInc, 
                                          float toKey,   boolean toInc);
    
    public LiteralIterator keyIterator();

    public LiteralIterator keyRangeIterator(float fromKey, boolean fromInc, 
                                            float toKey,   boolean toInc);

    public int get(float key);

    public int remove(float key);
    
    public int remove(float key, int value);

    public int put(float key, int value);
    
} // end of interface FloatIntSortedMap
