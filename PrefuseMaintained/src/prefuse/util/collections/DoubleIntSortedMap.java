package prefuse.util.collections;


/**
 * Sorted map that maps from a double key to an int value.
 * 
 * @author <a href="http://jheer.org">jeffrey heer</a>
 */
public interface DoubleIntSortedMap extends IntSortedMap {

    public double firstKey();

    public double lastKey();

    public boolean containsKey(double key);
    
    public IntIterator valueRangeIterator(double fromKey, boolean fromInc, 
                                          double toKey,   boolean toInc);
    
    public LiteralIterator keyIterator();
    
    public LiteralIterator keyRangeIterator(double fromKey, boolean fromInc, 
                                            double toKey,   boolean toInc);

    public int get(double key);

    public int remove(double key);
    
    public int remove(double key, int value);

    public int put(double key, int value);
    
} // end of interface DoubleIntSortedMap
