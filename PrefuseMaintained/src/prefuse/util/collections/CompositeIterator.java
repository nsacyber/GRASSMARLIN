package prefuse.util.collections;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Iterator implementation that combines the results of multiple iterators.
 * 
 * @author <a href="http://jheer.org">jeffrey heer</a>
 */
public class CompositeIterator implements Iterator {

    private Iterator[] m_iters;
    private int m_cur;
    
    public CompositeIterator(int size) {
        m_iters = new Iterator[size];
    }
    
    public CompositeIterator(Iterator iter1, Iterator iter2) {
        this(new Iterator[] {iter1, iter2});
    }
    
    public CompositeIterator(Iterator[] iters) {
        m_iters = iters;
        m_cur = 0;
    }

    public void setIterator(int idx, Iterator iter) {
        m_iters[idx] = iter;
    }
    
    /**
     * Not supported.
     * @see java.util.Iterator#remove()
     */
    public void remove() {
        throw new UnsupportedOperationException();
    }
    
    /**
     * @see java.util.Iterator#next()
     */
    public Object next() {
        if ( hasNext() ) {
            return m_iters[m_cur].next();
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
            if ( m_cur >= m_iters.length ) {
                m_iters = null;
                return false;
            } if ( m_iters[m_cur] == null ) {
                ++m_cur;
            } else if ( m_iters[m_cur].hasNext() ) {
                return true;
            } else {
                ++m_cur;
            }
        }
    }

} // end of class CompositeIterator
