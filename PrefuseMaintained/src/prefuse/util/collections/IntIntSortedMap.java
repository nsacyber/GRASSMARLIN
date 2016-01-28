package prefuse.util.collections;

/**
 * Sorted map that maps from an int key to an int value.
 * 
 * @author <a href="http://jheer.org">jeffrey heer</a>
 */
public interface IntIntSortedMap extends IntSortedMap {

    public int firstKey();

    public int lastKey();

    public boolean containsKey(int key);
    
    public IntIterator valueRangeIterator(int fromKey, boolean fromInc, 
                                          int toKey,   boolean toInc);
    
    public LiteralIterator keyIterator();
    
    public LiteralIterator keyRangeIterator(int fromKey, boolean fromInc, 
                                            int toKey,   boolean toInc);

    public int get(int key);

    public int remove(int key);
    
    public int remove(int key, int value);

    public int put(int key, int value);
    
    public int getLast(int key);
    
    public int getNextValue(int key, int value);
    
    public int getPreviousValue(int key, int value);
    
} // end of interface IntIntSortedMap
