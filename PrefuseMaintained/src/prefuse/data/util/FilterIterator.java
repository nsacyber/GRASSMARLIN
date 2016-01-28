package prefuse.data.util;

import java.util.Iterator;
import java.util.NoSuchElementException;

import prefuse.data.Tuple;
import prefuse.data.expression.Predicate;

/**
 * Iterator over tuples that filters the output by a given predicate.
 * 
 * @author <a href="http://jheer.org">jeffrey heer</a>
 */
public class FilterIterator implements Iterator {
    
    private Predicate predicate;
    private Iterator tuples;
    private Tuple next;
    
    /**
     * Create a new FilterIterator.
     * @param tuples an iterator over tuples
     * @param p the filter predicate to use
     */
    public FilterIterator(Iterator tuples, Predicate p) {
        this.predicate = p;
        this.tuples = tuples;
        next = advance();
    }
    
    private Tuple advance() {
        while ( tuples.hasNext() ) {
            Tuple t = (Tuple)tuples.next();
            if ( predicate.getBoolean(t) ) {
                return t;
            }
        }
        tuples = null;
        next = null;
        return null;
    }

    /**
     * @see java.util.Iterator#next()
     */
    public Object next() {
        if ( !hasNext() ) {
            throw new NoSuchElementException("No more elements");
        }
        Tuple retval = next;
        next = advance();
        return retval;
    }
    
    /**
     * @see java.util.Iterator#hasNext()
     */
    public boolean hasNext() {
        return ( tuples != null );
    }
    
    /**
     * Not supported.
     * @see java.util.Iterator#remove()
     */
    public void remove() {
        throw new UnsupportedOperationException();
    }
    
} // end of class FilterIterator
