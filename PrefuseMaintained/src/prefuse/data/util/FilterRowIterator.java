package prefuse.data.util;

import java.util.NoSuchElementException;

import prefuse.data.Table;
import prefuse.data.expression.Predicate;
import prefuse.util.collections.IntIterator;

/**
 * Iterator over table rows that filters the output by a given predicate. For
 * each table row, the corresponding tuple is checked against the predicate.
 * Only rows whose tuples pass the filter are included in this iteration.
 * 
 * @author <a href="http://jheer.org">jeffrey heer</a>
 */
public class FilterRowIterator extends IntIterator {
    
    private Predicate predicate;
    private IntIterator rows;
    private Table t;
    private int next;
    
    /**
     * Create a new FilterRowIterator.
     * @param rows an iterator over table rows
     * @param t the whos rows are being iterated over
     * @param p the filter predicate to use
     */
    public FilterRowIterator(IntIterator rows, Table t, Predicate p) {
        this.predicate = p;
        this.rows = rows;
        this.t = t;
        next = advance();
    }
    
    private int advance() {
        while ( rows.hasNext() ) {
            int r = rows.nextInt();
            if ( predicate.getBoolean(t.getTuple(r)) ) {
                return r;
            }
        }
        rows = null;
        next = -1;
        return -1;
    }

    /**
     * @see prefuse.util.collections.LiteralIterator#nextInt()
     */
    public int nextInt() {
        if ( !hasNext() ) {
            throw new NoSuchElementException("No more elements");
        }
        int retval = next;
        next = advance();
        return retval;
    }
    
    /**
     * @see java.util.Iterator#hasNext()
     */
    public boolean hasNext() {
        return ( rows != null );
    }
    
    /**
     * Not supported.
     * @see java.util.Iterator#remove()
     */
    public void remove() {
        throw new UnsupportedOperationException();
    }
    
} // end of class FilterRowIterator
