package prefuse.util.collections;

import java.util.Comparator;

/**
 * Comparator that makes comparison using an ordered list of
 * individual comparators;
 * 
 * @author <a href="http://jheer.org">jeffrey heer</a>
 */
public class CompositeComparator implements Comparator {

    private static final int INCREMENT = 2;
    private Comparator[] m_cmp;
    private int m_rev = 1;
    private int m_size = 0;

    /**
     * Creates an empty CompositeComparator with the given capacity.
     * @param size the starting capacity of this comparator
     */
    public CompositeComparator(int size) {
        this(size, false);
    }
    
    /**
     * Creates an empty CompositeComparator with the given capacity.
     * @param size the starting capacity of this comparator
     * @param reverse when true, reverses the sort order of the included
     * comparators, when false, objects are sorted as usual
     */
    public CompositeComparator(int size, boolean reverse) {
        m_cmp = new Comparator[size];
        m_rev = reverse ? -1 : 1;
    }
    
    /**
     * Creates a new CompositeComparator.
     * @param cmp the constituent comparators of this composite
     */
    public CompositeComparator(Comparator[] cmp) {
        this(cmp, false);
    }
    
    /**
     * Creates a new CompositeComparator.
     * @param cmp the constituent comparators of this composite
     * @param reverse when true, reverses the sort order of the included
     * comparators, when false, objects are sorted as usual
     */
    public CompositeComparator(Comparator[] cmp, boolean reverse) {
        this(cmp.length, reverse);
        System.arraycopy(cmp, 0, m_cmp, 0, cmp.length);
        m_size = cmp.length;
    }
    
    /**
     * Adds an additional comparator to this composite.
     * @param c the Comparator to add
     */
    public void add(Comparator c) {
        if ( c == null ) return;
        if ( m_cmp.length == m_size ) {
            Comparator[] cmp = new Comparator[m_size+INCREMENT];
            System.arraycopy(m_cmp, 0, cmp, 0, m_size);
            m_cmp = cmp;
        }
        m_cmp[m_size++] = c;
    }
    
    /**
     * Removes a comparator from this composite.
     * @param c the Comparator to remove
     * @return true if the comparator was successfully removed,
     * false otherwise
     */
    public boolean remove(Comparator c) {
        for ( int i=0; i<m_size; ++i ) {
            if ( m_cmp[i].equals(c) ) {
                System.arraycopy(m_cmp, i+1, m_cmp, i, m_size-i);
                --m_size;
                return true;
            }
        }
        return false;
    }
    
    // ------------------------------------------------------------------------
    
    /**
     * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
     */
    public int compare(Object o1, Object o2) {
        for ( int i=0; i<m_cmp.length; ++i ) {
            int c = m_cmp[i].compare(o1, o2);
            if ( c != 0 ) {
                return m_rev*c;
            }
        }
        return 0;
    }

} // end of class CompositeComparator
