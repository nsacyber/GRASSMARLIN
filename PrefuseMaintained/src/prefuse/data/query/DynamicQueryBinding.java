package prefuse.data.query;

import javax.swing.JComponent;

import prefuse.data.expression.Predicate;
import prefuse.data.tuple.TupleSet;

/**
 * <p>Abstract base class for dynamic query bindings, which support
 * data queries that can be dynamically edited with direct manipulation
 * user interface components. DynamicQueryBinding instances
 * take a particular field of a table, create a
 * {@link prefuse.data.expression.Predicate} instance for filtering Tuples
 * based on the values of that data field, and bind that Predicate to any
 * number of user interface components that can be used to manipulate the
 * parameters of the predicate.</p>
 * 
 * <p>Examples include dynamically filtering over a particular range of
 * values ({@link RangeQueryBinding}), isolating specific categories of
 * data ({@link ListQueryBinding}), and performing text search over
 * data ({@link SearchQueryBinding}).</p>
 * 
 * @author <a href="http://jheer.org">jeffrey heer</a>
 */
public abstract class DynamicQueryBinding {
    
    /** The actual query over Table data. */
    protected Predicate m_query;
    /** The TupleSet processed by the query. */
    protected TupleSet m_tuples;
    /** The data field processed by the query. */
    protected String m_field;
    
    /**
     * Create a new DynamicQueryBinding. Called by subclasses.
     * @param tupleSet the TupleSet to query
     * @param field the data field (Table column) to query
     */
    protected DynamicQueryBinding(TupleSet tupleSet, String field) {
        m_tuples = tupleSet;
        m_field = field;
    }
    
    // ------------------------------------------------------------------------
    
    /**
     * Returns the query predicate bound to this dynamic query. The predicate's
     * behavior can vary dynamically based on interaction with user interface
     * components created by this binding. To automatically monitor changes to
     * this predicate, clients should register an 
     * {@link prefuse.data.event.ExpressionListener} with the
     * {@link prefuse.data.expression.Predicate} returned by this method.
     * @return the dynamic query {@link prefuse.data.expression.Predicate}
     */
    public Predicate getPredicate() {
        return m_query;
    }
    
    /**
     * Sets the dynamic query predicate. For class-internal use only.
     * @param p the predicate to set
     */
    protected void setPredicate(Predicate p) {
        m_query = p;
    }
    
    /**
     * Generates a new user interface component for dynamically adjusting
     * the query values. The type of the component depends on the subclass
     * of DynamicQueryBinding being used. Some subclasses can generate
     * multiple types of user interface components. Such classes will include
     * additional methods for generating the specific kinds of components
     * supported.
     * @return a user interface component for adjusting the query.
     */
    public abstract JComponent createComponent();
    
} // end of class DynamicQueryBinding
