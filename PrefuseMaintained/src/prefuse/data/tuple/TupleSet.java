package prefuse.data.tuple;

import java.beans.PropertyChangeListener;
import java.util.Iterator;

import prefuse.data.Schema;
import prefuse.data.Tuple;
import prefuse.data.event.TupleSetListener;
import prefuse.data.expression.Expression;
import prefuse.data.expression.Predicate;
import prefuse.data.util.Sort;

/**
 * A collection of data tuples. This is the top level interface for all
 * data collections in the prefuse framework.
 * 
 * @author <a href="http://jheer.org">jeffrey heer</a>
 * @see prefuse.data.Tuple
 */
public interface TupleSet {

    /**
     * An empty, zero-length array of tuples.
     */
    public static final Tuple[] EMPTY_ARRAY = new Tuple[0];
    
    /**
     * Add a Tuple to this TupleSet. This method is optional, and may result
     * in an UnsupportedOperationException on some TupleSet implementations.
     * @param t the Tuple add
     * @return the actual Tuple instance stored in the TupleSet. This may be
     * null to signify that the add failed, may be a new Tuple with
     * values copied from the input tuple, or may be the input Tuple itself.
     */
    public Tuple addTuple(Tuple t);
    
    /**
     * Set the TupleSet contents to be a single Tuple. This method is
     * optional, and may result in an UnsupportedOperationException on some
     * TupleSet implementations.
     * @param t the Tuple to set as the content of this TupleSet
     * @return the actual Tuple instance stored in the TupleSet. This may be
     * null to signify that the add failed, may be a new Tuple with
     * values copied from the input tuple, or may be the input Tuple itself.
     */
    public Tuple setTuple(Tuple t);
    
    /**
     * Remove a Tuple from this TupleSet.
     * @param t the Tuple to remove
     * @return true if the Tuple was found and removed, false otherwise
     */
    public boolean removeTuple(Tuple t);
    
    /**
     * Clear this TupleSet, removing all contained Tuples.
     */
    public void clear();
    
    /**
     * Indicates if a given Tuple is contained within this TupleSet.
     * @param t the tuple to check for containment
     * @return true if the Tuple is in this TupleSet, false otherwise
     */
    public boolean containsTuple(Tuple t);
    
    /**
     * Get the number of tuples in this set.
     * @return the tuple count
     */
    public int getTupleCount();
    
    /**
     * Indicates if this TupleSet supports the addition of data columns to
     * its contained Tuple instances.
     * @return true is add column operations are supported, false otherwise
     */
    public boolean isAddColumnSupported();
    
    /**
     * Add the data fields of the given Schema to the Tuples in this TupleSet.
     * If this TupleSet contains heterogeneous Tuples (i.e., those from
     * different source tables), all backing tables involved will be updated.
     * @param s the Schema of data columns to add to this TupleSet
     */
    public void addColumns(Schema s);
    
    /**
     * Add a data field / column to this TupleSet's members.
     * @param name the name of the data field
     * @param type the type of the data field
     */
    public void addColumn(String name, Class type);
    
    /**
     * Add a data field / column to this TupleSet's members.
     * @param name the name of the data field
     * @param type the type of the data field
     * @param defaultValue the defaultValue of the data field
     */
    public void addColumn(String name, Class type, Object defaultValue);
    
    /**
     * Add a data field / column to this TupleSet's members.
     * @param name the name of the data field
     * @param expr an uncompiled expression in the prefuse expression
     * language. This will be compiled to a valid expression, and the
     * results of applying the expression to a Tuple will become the
     * data field value for that Tuple.
     * @see prefuse.data.expression
     * @see prefuse.data.expression.parser.ExpressionParser
     */
    public void addColumn(String name, String expr);
    
    /**
     * Add a data field / column to this TupleSet's members.
     * @param name the name of the data field
     * @param expr a compiled expression in the prefuse expression
     * language. The results of applying the expression to a Tuple will
     * become the data field value for that Tuple.
     * @see prefuse.data.expression
     */
    public void addColumn(String name, Expression expr);
    
    /**
     * Return an iterator over the tuples in this tuple set.
     * @return an iterator over this set's tuples
     */
    public Iterator tuples();
    
    /**
     * Return an iterator over the tuples in this tuple set, filtered by
     * the given predicate.
     * @param filter predicate to apply to tuples in this set, only tuples
     * for which the predicate evaluates to true are included in the iteration
     * @return a filtered iterator over this set's tuples
     */
    public Iterator tuples(Predicate filter);
    
    /**
     * Return an iterator over the tuples in this tuple set, filtered by
     * the given predicate
     * @param filter predicate to apply to tuples in this set, only tuples
     * for which the predicate evaluates to true are included in the iteration.
     * If this value is null, no filtering will be performed.
     * @param sort the sorting criteria by which to order the returned tuples
     * @return a filtered, sorted iterator over this set's tuples
     */
    public Iterator tuples(Predicate filter, Sort sort);
    
    
    // -- Listeners -----------------------------------------------------------
    
    /**
     * Add a listener to this tuple set that will be notified when tuples
     * are added and removed from the set.
     * @param tsl the TupleSetListener to add
     */
    public void addTupleSetListener(TupleSetListener tsl);
    
    /**
     * Remove a listener from this tuple set.
     * @param tsl the TupleSetListener to remove
     */
    public void removeTupleSetListener(TupleSetListener tsl);
    
    
    // -- Client Properties ---------------------------------------------------
    
    /**
     * Add a PropertyChangeListener to be notified of changes to the properties
     * bounds to this TupleSet.
     * @param lstnr the PropertyChangeListener to add
     */
    public void addPropertyChangeListener(PropertyChangeListener lstnr);

    /**
     * Add a PropertyChangeListener to be notified of changes to a
     * specific property bound to this TupleSet.
     * @param key the specific key for which to listen to properties changes
     * @param lstnr the PropertyChangeListener to add
     */
    public void addPropertyChangeListener(String key, 
                                          PropertyChangeListener lstnr);

    /**
     * Remove a PropertyChangeListener from this TupleSet.
     * @param lstnr the PropertyChangeListener to remove
     */
    public void removePropertyChangeListener(PropertyChangeListener lstnr);
    
    /**
     * Remove a PropertyChangeListener from this TupleSet for a specific
     * bound property.
     * @param key the specific key for which to remove the listener
     * @param lstnr the PropertyChangeListener to remove
     */
    public void removePropertyChangeListener(String key,
                                             PropertyChangeListener lstnr);
    
    /**
     * Set an arbitrary client property with this TupleSet
     * @param key the name of the property to set
     * @param value the value of the property to use
     */
    public void putClientProperty(String key, Object value);
    
    /**
     * Get an client property bound to this TupleSet
     * @param key the name of the property to retrieve
     * @return the client property value, or null if no value was
     * found for the given key.
     */
    public Object getClientProperty(String key);
    
} // end of interface TupleSet
