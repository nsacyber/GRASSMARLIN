package prefuse.data.expression;

import java.util.Iterator;

import prefuse.data.Tuple;

/**
 * Predicate representing an "xor" clause of sub-predicates.
 * 
 * @author <a href="http://jheer.org">jeffrey heer</a>
 */
public class XorPredicate extends CompositePredicate {

    /**
     * Create an empty XorPredicate. Empty XorPredicates return false
     * by default.
     */
    public XorPredicate() {
    }
    
    /**
     * Create a new XorPredicate.
     * @param p1 the sole clause of this predicate
     */
    public XorPredicate(Predicate p1) {
        add(p1);
    }
    
    /**
     * Create a new XorPredicate.
     * @param p1 the first clause of this predicate
     * @param p2 the second clause of this predicate
     */
    public XorPredicate(Predicate p1, Predicate p2) {
        super(p1, p2);
    }
    
    /**
     * @see prefuse.data.expression.Expression#getBoolean(prefuse.data.Tuple)
     */
    public boolean getBoolean(Tuple t) {
        if ( m_clauses.size() == 0 )
            return false;
        
        boolean val = false;
        Iterator iter = m_clauses.iterator();
        if ( iter.hasNext() ) {
            val = ((Predicate)iter.next()).getBoolean(t);
        }
        while ( iter.hasNext() ) {
            val ^= ((Predicate)iter.next()).getBoolean(t);
        }
        return val;
    }
    
    /**
     * @see java.lang.Object#toString()
     */
    public String toString() {
        return ( size() == 0 ? "FALSE" : toString("XOR") );
    }

} // end of class XorPredicate
