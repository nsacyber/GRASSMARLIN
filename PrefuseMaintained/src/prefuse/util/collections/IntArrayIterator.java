/**
 * Copyright (c) 2004-2006 Regents of the University of California.
 * See "license-prefuse.txt" for licensing terms.
 */
package prefuse.util.collections;

import java.util.NoSuchElementException;

/**
 * IntIterator implementation that provides an iteration over the
 * contents of an int array.
 * 
 * @author <a href="http://jheer.org">jeffrey heer</a>
 */
public class IntArrayIterator extends IntIterator {

    private int[] m_array;
    private int m_cur;
    private int m_end;
    
    public IntArrayIterator(int[] array, int start, int len) {
        m_array = array;
        m_cur = start;
        m_end = start+len;
    }
    
    /**
     * @see prefuse.util.collections.IntIterator#nextInt()
     */
    public int nextInt() {
        if ( m_cur >= m_end )
            throw new NoSuchElementException();
        return m_array[m_cur++];
    }

    /**
     * @see java.util.Iterator#hasNext()
     */
    public boolean hasNext() {
        return m_cur < m_end;
    }
    
    public void remove() {
        throw new UnsupportedOperationException();
    }

} // end of class IntArrayIterator
