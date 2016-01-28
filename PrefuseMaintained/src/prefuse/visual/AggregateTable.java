/**
 * Copyright (c) 2004-2006 Regents of the University of California.
 * See "license-prefuse.txt" for licensing terms.
 */
package prefuse.visual;

import java.util.HashSet;
import java.util.Iterator;

import prefuse.Visualization;
import prefuse.data.Schema;
import prefuse.data.Table;
import prefuse.data.Tuple;
import prefuse.data.event.EventConstants;
import prefuse.data.util.Index;
import prefuse.util.collections.IntIterator;
import prefuse.visual.tuple.TableAggregateItem;

/**
 * VisualTable instance that maintains visual items representing aggregates
 * of items. This class maintains both a collection of AggregateItems and
 * a mapping between AggregateItems and the VisualItems contained within
 * those aggregates.
 * 
 * @author <a href="http://jheer.org">jeffrey heer</a>
 */
public class AggregateTable extends VisualTable {

    /**
     * Table storing the 1->Many aggregation mappings
     */
    protected Table m_aggregated;
    
    /**
     * Create a new AggregateTable.
     * @param vis the Visualization associated with the table
     * @param group the data group the table contents belongs to
     */
    public AggregateTable(Visualization vis, String group) {
        this(vis, group, VisualItem.SCHEMA);
    }

    /**
     * Create a new AggregateTable.
     * @param vis the Visualization associated with the table
     * @param group the data group the table contents belongs to
     * @param schema the Schema to use for this table
     */
    public AggregateTable(Visualization vis, String group, Schema schema) {
        super(vis, group, schema, TableAggregateItem.class);
        m_aggregated = AGGREGATED_SCHEMA.instantiate();
        m_aggregated.index(AGGREGATE);
        m_aggregated.index(MEMBER_HASH);
    }
    
    // ------------------------------------------------------------------------
    
    /**
     * Get the size of the aggregate represented at the given table row.
     * Returns the number of visual items contained in the aggregation.
     * @return the aggregate size for the given row
     */
    public int getAggregateSize(int row) {
        int size = 0;
        AggregatedIterator ati = new AggregatedIterator(row);
        for ( ; ati.hasNext(); ++size, ati.next() );
        return size;
    }
    
    /**
     * Add an item to the aggregation at the given row.
     * @param row the row index of the aggregate
     * @param member the item to add to the aggregation
     */
    public void addToAggregate(int row, VisualItem member) {
        validRowCheck(row, true);
        if ( !aggregateContains(row, member) ) {
            int ar = m_aggregated.addRow();
            m_aggregated.setInt(ar, AGGREGATE, row);
            m_aggregated.setInt(ar, MEMBER_HASH, getHashCode(member));
            m_aggregated.set(ar, MEMBER, member);
            fireTableEvent(row, row,
                    EventConstants.ALL_COLUMNS, EventConstants.UPDATE);
        }
    }
    
    /**
     * Remove an item from the aggregation at the given row
     * @param row the row index of the aggregate
     * @param member the item to remove from the aggregation
     */
    public void removeFromAggregate(int row, VisualItem member) {
        validRowCheck(row, true);
        int ar = getAggregatedRow(row, member);
        if ( ar >= 0 ) {
            m_aggregated.removeRow(ar);
            fireTableEvent(row, row,
                EventConstants.ALL_COLUMNS, EventConstants.UPDATE);
        }
    }
    
    /**
     * Remove all items contained in the aggregate at the given row
     * @param row the row index of the aggregate
     */
    public void removeAllFromAggregate(int row) {
        clearAggregateMappings(row, true);
    }
    
    /**
     * Clears all aggregates mappings for the aggregate at the given row,
     * optionally issuing a table update.
     * @param row the table row of the aggregate
     * @param update indicates whether or not to fire a table update
     */
    protected void clearAggregateMappings(int row, boolean update) {
        Index index = m_aggregated.index(AGGREGATE);
        boolean fire = false;
        for ( IntIterator rows = index.rows(row); rows.hasNext(); ) {
            int r = rows.nextInt();
            // this removal maneuver is ok because we know we are
            // pulling row values directly from an index
            // with intervening iterators, remove might throw an exception
            rows.remove();
            m_aggregated.removeRow(r);
            fire = true;
        }
        if ( update && fire ) 
            fireTableEvent(row, row,
                EventConstants.ALL_COLUMNS, EventConstants.UPDATE);
    }
    
    /**
     * Indicates if an item is a member of the aggregate at the given row
     * @param row the table row of the aggregate
     * @param member the item to check from containment
     * @return true if the item is in the aggregate, false otherwise
     */
    public boolean aggregateContains(int row, VisualItem member) {
        return getAggregatedRow(row, member) >= 0;
    }
    
    /**
     * Get the row index to the aggregate mapping table for the given
     * aggregate and contained VisualItem.
     * @param row the table row of the aggregate
     * @param member the VisualItem to look up
     * @return the row index into the internal aggregate mapping table for the
     * mapping between the given aggregate row and given VisualItem
     */
    protected int getAggregatedRow(int row, VisualItem member) {
        Index index = m_aggregated.index(MEMBER_HASH);
        int hash = getHashCode(member);
        int ar = index.get(hash);
        if ( ar < 0 ) {
            return -1;
        } else if ( m_aggregated.getInt(ar, AGGREGATE) == row ) {
            return ar;
        } else {
            for ( IntIterator rows = index.rows(hash); rows.hasNext(); ) {
                ar = rows.nextInt();
                if ( m_aggregated.getInt(ar, AGGREGATE) == row )
                    return ar;
            }
            return -1;
        }
    }
    
    /**
     * Get all VisualItems within the aggregate at the given table row.
     * @param row the table row of the aggregate
     * @return an iterator over the items in the aggregate
     */
    public Iterator aggregatedTuples(int row) {
        return new AggregatedIterator(row);
    }
    
    /**
     * Get an iterator over all AggregateItems that contain the given Tuple.
     * @param t the input tuple
     * @return an iterator over all AggregateItems that contain the input Tuple
     */
    public Iterator getAggregates(Tuple t) {
        int hash = getHashCode(t);
        IntIterator iit = m_aggregated.getIndex(MEMBER_HASH).rows(hash);
        HashSet set = new HashSet();
        while ( iit.hasNext() ) {
            int r = iit.nextInt();
            set.add(getTuple(m_aggregated.getInt(r, AGGREGATE)));
        }
        return set.iterator();
    }
    
    /**
     * Get a hashcode that uniquely identifies a particular tuple
     * @param t the tuple to compute the hash for
     * @return a unique identifier for the tuple
     */
    protected int getHashCode(Tuple t) {
        // this works for now because hashCode is not overloaded on
        // the provided Tuple implementations
        return t.hashCode();
    }
    
    /**
     * Check a row for validity, optionally throwing an exception when an
     * invalid row is found.
     * @param row the row to check
     * @param throwException indicates if an exception should be thrown when an
     * invalid row is encountered
     * @return true if the row was valid, false otherwise
     */
    protected boolean validRowCheck(int row, boolean throwException) {
        if ( isValidRow(row) ) {
            return true;
        } else if ( throwException ) {
            throw new IllegalArgumentException("Invalid row value: "+row);
        } else {
            return false;
        }
    }
    
    // ------------------------------------------------------------------------
    // Table Listener Interception
    
    /**
     * Clear all aggregate mappings for a row when it is deleted.
     */
    protected void fireTableEvent(int row0, int row1, int col, int type) {
        if ( col==EventConstants.ALL_COLUMNS && type==EventConstants.DELETE ) {
            for ( int r=row0; r<=row1; ++r )
                clearAggregateMappings(r, false);
        }
        super.fireTableEvent(row0, row1, col, type);
    }
    
    
    // ------------------------------------------------------------------------
    // Aggregated Iterator
    
    /**
     * Iterator instance that iterates over the items contained in an aggregate.
     */
    protected class AggregatedIterator implements Iterator {
        private IntIterator m_rows;
        private Tuple m_next = null;

        public AggregatedIterator(int row) {
            Index index = m_aggregated.index(AGGREGATE);
            m_rows = index.rows(row);
            advance();
        }
        public boolean hasNext() {
            return m_next != null;
        }
        public Object next() {
            Tuple retval = m_next;
            advance();
            return retval;
        }
        private void advance() {
            while ( m_rows.hasNext() ) {
                int ar = m_rows.nextInt();
                Tuple t = (Tuple)m_aggregated.get(ar, MEMBER);
                if ( t.isValid() ) {
                    m_next = t;
                    return;
                } else {
                    m_aggregated.removeRow(ar);
                }
            }
            m_next = null;
        }
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }
    
    // ------------------------------------------------------------------------
    // Aggregated Table Schema
    
    protected static final String AGGREGATE = "aggregate";
    protected static final String MEMBER_HASH = "hash";
    protected static final String MEMBER = "member";
    protected static final Schema AGGREGATED_SCHEMA = new Schema();
    static {
        AGGREGATED_SCHEMA.addColumn(AGGREGATE, int.class);
        AGGREGATED_SCHEMA.addColumn(MEMBER_HASH, int.class);
        AGGREGATED_SCHEMA.addColumn(MEMBER, Tuple.class);
    }
    
} // end of class AggregateTable
