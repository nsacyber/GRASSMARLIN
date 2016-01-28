package prefuse.data.expression;

import java.util.ArrayList;
import java.util.Iterator;

/**
 * Abstract base class for Predicate instances that maintain one or
 * more sub-predicates (clauses).
 * 
 * @author <a href="http://jheer.org">jeffrey heer</a>
 */
public abstract class CompositePredicate extends AbstractPredicate {

    protected ArrayList m_clauses = new ArrayList(2);
    
    /**
     * Create a new, empty CompositePredicate.
     */
    public CompositePredicate() {
    }
    
    /**
     * Create a new CompositePredicate.
     * @param p1 the first sub-predicate
     * @param p2 the second sub-predicate
     */
    public CompositePredicate(Predicate p1, Predicate p2) {
        m_clauses.add(p1);
        m_clauses.add(p2);
    }
    
    // ------------------------------------------------------------------------
    
    /**
     * Add a new clause.
     * @param p the Predicate clause to add
     */
    public void add(Predicate p) {
        if ( m_clauses.contains(p) ) {
            throw new IllegalArgumentException("Duplicate predicate.");
        }
        m_clauses.add(p);
        fireExpressionChange();
    }
    
    /**
     * Remove a new clause.
     * @param p the Predicate clause to remove
     * @return true if removed, false if not found
     */
    public boolean remove(Predicate p) {
        if ( m_clauses.remove(p) ) {
            fireExpressionChange();
            return true;
        } else {
            return false;
        }
    }
    
    /**
     * Remove all clauses.
     */
    public void clear() {
        removeChildListeners();
        m_clauses.clear();
        fireExpressionChange();
    }
    
    /**
     * Get the number of sub-predicate clauses.
     * @return the number of clauses
     */
    public int size() {
        return m_clauses.size();
    }
    
    /**
     * Get the sub-predicate at the given index.
     * @param idx the index to lookup
     * @return the sub-predicate at the given index
     */
    public Predicate get(int idx) {
        return (Predicate)m_clauses.get(idx);
    }
    
    /**
     * Set the given predicate to be the only clause of thie composite.
     * @param p the new sole sub-predicate clause
     */
    public void set(Predicate p) {
        removeChildListeners();
        m_clauses.clear();
        m_clauses.add(p);
        if ( hasListeners() ) addChildListeners();
        fireExpressionChange();
    }
    
    /**
     * Set the given predicates to be the clauses of thie composite.
     * @param p the new sub-predicate clauses
     */
    public void set(Predicate[] p) {
        removeChildListeners();
        m_clauses.clear();
        for ( int i=0; i<p.length; ++i ) {
            if ( !m_clauses.contains(p) )
                m_clauses.add(p[i]);
        }
        if ( hasListeners() ) addChildListeners();
        fireExpressionChange();
    }
    
    /**
     * Get a predicate instance just like this one but without
     * the given predicate as a clause.
     * @param p the predicate clause to ignore
     * @return a clone of this predicate, only without the input predicate
     */
    public Predicate getSubPredicate(Predicate p) {
        CompositePredicate cp = null;
        try {
            cp  = (CompositePredicate)this.getClass().newInstance();
        } catch (InstantiationException e) {
            // won't happen
        } catch (IllegalAccessException e) {
            // won't happen
        }
        for ( int i=0; i<m_clauses.size(); ++i ) {
            Predicate pp = (Predicate)m_clauses.get(i);
            if ( p != pp ) {
                cp.add(pp);
            }
        }
        return cp;
    }
    
    // ------------------------------------------------------------------------
    
    /**
     * @see prefuse.data.expression.Expression#visit(prefuse.data.expression.ExpressionVisitor)
     */
    public void visit(ExpressionVisitor v) {
        v.visitExpression(this);
        Iterator iter = m_clauses.iterator();
        while ( iter.hasNext() ) {
            v.down();
            ((Expression)iter.next()).visit(v);
            v.up();
        }
    }
    
    // ------------------------------------------------------------------------
    
    /**
     * @see prefuse.data.expression.AbstractExpression#addChildListeners()
     */
    protected void addChildListeners() {
        Iterator iter = m_clauses.iterator();
        while ( iter.hasNext() ) {
            ((Expression)iter.next()).addExpressionListener(this);
        }
    }
    
    /**
     * @see prefuse.data.expression.AbstractExpression#removeChildListeners()
     */
    protected void removeChildListeners() {
        Iterator iter = m_clauses.iterator();
        while ( iter.hasNext() ) {
            ((Expression)iter.next()).removeExpressionListener(this);
        }
    }
    
    // ------------------------------------------------------------------------
    
    /**
     * Return a String representation of this predicate.
     * @param op a String describing the operation this Predicate performs
     * @return a String representing this Expression
     */
    protected String toString(String op) {
        if ( m_clauses.size() == 1 ) {
            return m_clauses.get(0).toString();
        }
        
        StringBuffer sbuf = new StringBuffer();
        sbuf.append('(');

        Iterator iter = m_clauses.iterator();
        while ( iter.hasNext() ) {
            sbuf.append(iter.next().toString());
            if ( iter.hasNext() ) {
                sbuf.append(" ");
                sbuf.append(op);
                sbuf.append(" ");
            }
        }
        
        sbuf.append(')');
        return sbuf.toString();
    }

} // end of abstract class CompositePredicate
