package prefuse.data.util;

import java.util.BitSet;
import java.util.NoSuchElementException;

import prefuse.util.collections.IntIterator;

/**
 * IntIterator over rows that ensures that no duplicates appear in the
 * iteration. Uses a bitset to note rows it has has seen and not pass along
 * duplicate row values.
 * 
 * @author <a href="http://jheer.org">jeffrey heer</a>
 */
public class UniqueRowIterator extends IntIterator {

    private IntIterator m_iter;
    private int m_next;
    private BitSet m_visited;
    
    /**
     * Create a new UniqueRowIterator.
     * @param iter a source iterator over table rows
     */
    public UniqueRowIterator(IntIterator iter) {
        m_iter = iter;
        m_visited = new BitSet();
        advance();
    }
    
    private void advance() {
        int r = -1;
        while ( r == -1 && m_iter.hasNext() ) {
            r = m_iter.nextInt();
            if ( visit(r) )
                r = -1;
        }
        m_next = r;
    }
    
    private boolean visit(int row) {
        if ( m_visited.get(row) ) {
            return true;
        } else {
            m_visited.set(row);
            return false;
        }
    }
    
    /**
     * @see java.util.Iterator#hasNext()
     */
    public boolean hasNext() {
        return m_next != -1;
    }

    /**
     * @see prefuse.util.collections.LiteralIterator#nextInt()
     */
    public int nextInt() {
        if ( m_next == -1 )
            throw new NoSuchElementException();
        int retval = m_next;
        advance();
        return retval;
    }
    
    /**
     * Not supported.
     * @see java.util.Iterator#remove()
     */
    public void remove() {
        throw new UnsupportedOperationException();
    }

} // end of class UniqueRowIterator
