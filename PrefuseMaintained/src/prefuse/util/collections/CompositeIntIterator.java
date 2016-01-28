package prefuse.util.collections;

import java.util.NoSuchElementException;

/**
 * IntIterator implementation that combines the results of multiple
 * int iterators.
 * 
 * @author <a href="http://jheer.org">jeffrey heer</a>
 */
public class CompositeIntIterator extends IntIterator {

    private IntIterator[] m_iters;
    private int m_cur;
    
    public CompositeIntIterator(IntIterator iter1, IntIterator iter2) {
        this(new IntIterator[] { iter1, iter2 });
    }
    
    public CompositeIntIterator(IntIterator[] iters) {
        m_iters = iters;
        m_cur = 0;
    }
    
    /**
     * @see prefuse.util.collections.IntIterator#nextInt()
     */
    public int nextInt() {
        if ( hasNext() ) {
            return m_iters[m_cur].nextInt();
        } else {
            throw new NoSuchElementException();
        }
    }

    /**
     * @see java.util.Iterator#hasNext()
     */
    public boolean hasNext() {
        if ( m_iters == null )
            return false;
        
        while ( true ) {
            if ( m_iters[m_cur].hasNext() ) {
                return true;
            } else if ( ++m_cur >= m_iters.length ) {
                m_iters = null;
                return false;
            }
        }
    }
    
    public void remove() {
        throw new UnsupportedOperationException();
    }

} // end of class CompositeIntIterator
