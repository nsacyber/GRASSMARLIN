package prefuse.data.tuple;

import java.util.Iterator;
import java.util.logging.Logger;

import prefuse.data.Graph;
import prefuse.data.Table;
import prefuse.data.Tuple;
import prefuse.util.StringLib;
import prefuse.util.collections.IntIterator;

/**
 * Manager class for Tuples. There is a unique Tuple for each row of a table.
 * All data structures and Tuples are created lazily, on an as-needed basis.
 * When a row is deleted from the table, it's corresponding Tuple (if created)
 * is invalidated before being removed from this data structure, ensuring that
 * any other live references to the Tuple can't be used to corrupt the table.
 * 
 * @author <a href="http://jheer.org">jeffrey heer</a>
 */
public class TupleManager {

    protected Graph        m_graph;
    protected Table        m_table;
    protected Class        m_tupleType;
    
    private   TableTuple[] m_tuples;
    
    /**
     * Create a new TupleManager for the given Table.
     * @param t the data Table to generate Tuples for
     */
    public TupleManager(Table t, Graph g, Class tupleType) {
        init(t, g, tupleType);
    }
    
    /**
     * Initialize this TupleManager for use with a given Table.
     * @param t the data Table to generate Tuples for
     */
    public void init(Table t, Graph g, Class tupleType) {
        if ( m_table != null ) {
            throw new IllegalStateException(
                "This TupleManager has already been initialized");
        }
        m_table = t;
        m_graph = g;
        m_tupleType = tupleType;
        m_tuples = null;
    }
    
    /**
     * Get the type of Tuple instances to generate.
     * @return the tuple type, as a Class instance
     */
    public Class getTupleType() {
        return m_tupleType;
    }
    
    /**
     * Ensure the tuple array exists.
     */
    private void ensureTupleArray(int row) {
        int nrows = Math.max(m_table.getRowCount(), row+1);
        if ( m_tuples == null ) {
            m_tuples = new TableTuple[nrows];
        } else if ( m_tuples.length < nrows ) {
            int capacity = Math.max((3*m_tuples.length)/2 + 1, nrows);
            TableTuple[] tuples = new TableTuple[capacity];
            System.arraycopy(m_tuples, 0, tuples, 0, m_tuples.length);
            m_tuples = tuples;
        }
    }
    
    /**
     * Get a Tuple corresponding to the given row index.
     * @param row the row index
     * @return the Tuple corresponding to the given row
     */
    public Tuple getTuple(int row) {
        if ( m_table.isValidRow(row) ) {
            ensureTupleArray(row);
            if ( m_tuples[row] == null ) {
                return (m_tuples[row] = newTuple(row));
            } else {
                return m_tuples[row];
            }
        } else {
            // TODO: return null instead?
            throw new IllegalArgumentException("Invalid row index: "+row);
        }
    }
    
    /**
     * Instantiate a new Tuple instance for the given row index.
     * @param row the row index of the tuple
     * @return the newly created Tuple
     */
    protected TableTuple newTuple(int row) {
        try {
            TableTuple t = (TableTuple)m_tupleType.newInstance();
            t.init(m_table, m_graph, row);
            return t;
        } catch ( Exception e ) {
            Logger.getLogger(getClass().getName()).warning(
                e.getMessage()+"\n"+StringLib.getStackTrace(e));
            return null;
        }
    }
    
    /**
     * Invalidate the tuple at the given row.
     * @param row the row index to invalidate
     */
    public void invalidate(int row) {
        if ( m_tuples == null || row < 0 || row >= m_tuples.length ) {
            return;
        } else if ( m_tuples[row] != null ) {
            m_tuples[row].invalidate();
            m_tuples[row] = null;
        }
    }
    
    /**
     * Invalidate all tuples managed by this TupleManager
     */
    public void invalidateAll() {
        if ( m_tuples == null ) return;
        for ( int i=0; i<m_tuples.length; ++i )
            invalidate(i);
    }
    
    /**
     * Return an iterator over the tuples in this manager.
     * @param rows an iterator over table rows
     * @return an iterator over the tuples indicated by the input row iterator
     */
    public Iterator iterator(IntIterator rows) {
        return new TupleManagerIterator(this, rows);
    }
    
    // ------------------------------------------------------------------------
    // TupleManagerIterator
    
    /**
     * Iterator instance for iterating over tuples managed in a TupleManager.
     */
    public class TupleManagerIterator implements Iterator {

        private TupleManager m_tuples;
        private IntIterator  m_rows;
        
        /**
         * Create a new TupleManagerIterator.
         * @param tuples the TupleManager from which to get the tuples
         * @param rows the rows to iterate over
         */
        public TupleManagerIterator(TupleManager tuples, IntIterator rows) {
            m_tuples = tuples;
            m_rows = rows;
        }
        
        /**
         * @see java.util.Iterator#hasNext()
         */
        public boolean hasNext() {
            return m_rows.hasNext();
        }

        /**
         * @see java.util.Iterator#next()
         */
        public Object next() {
            return m_tuples.getTuple(m_rows.nextInt());
        }

        /**
         * @see java.util.Iterator#remove()
         */
        public void remove() {
            // TODO: check to see if this is safe
            m_rows.remove();
        }

    } // end of inner class TupleManagerIterator
    
} // end of class TupleManager
