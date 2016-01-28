package prefuse.util.collections;

import java.util.Comparator;


/**
 * @author <a href="http://jheer.org">jeffrey heer</a>
 */
public interface IntSortedMap {
  
    public int getMinimum();
    public int getMaximum();
    public int getMedian();
    public int getUniqueCount();
    
    public boolean isAllowDuplicates();
    public int size();
    public boolean isEmpty();
    public Comparator comparator();
    
    public void clear();
    public boolean containsValue(int value);
    public IntIterator valueIterator(boolean ascending);
    
}
