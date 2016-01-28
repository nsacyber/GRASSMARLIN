/**
 * Copyright (c) 2004-2006 Regents of the University of California.
 * See "license-prefuse.txt" for licensing terms.
 */
package prefuse.visual.tuple;

import java.util.Iterator;

import prefuse.data.Graph;
import prefuse.data.Table;
import prefuse.data.expression.Predicate;
import prefuse.data.util.FilterIterator;
import prefuse.visual.AggregateItem;
import prefuse.visual.AggregateTable;
import prefuse.visual.VisualItem;

/**
 * AggregateItem implementation that uses data values from a backing
 * AggregateTable.
 * 
 * @author <a href="http://jheer.org">jeffrey heer</a>
 */
public class TableAggregateItem extends TableVisualItem 
    implements AggregateItem
{   
    /**
     * Initialize a new TableAggregateItem for the given table and row. This
     * method is used by the appropriate TupleManager instance, and should not
     * be called directly by client code, unless by a client-supplied custom
     * TupleManager.
     * @param table the data Table
     * @param graph ignored by this class
     * @param row the table row index
     */
    protected void init(Table table, Graph graph, int row) {
        m_table = table;
        m_row = m_table.isValidRow(row) ? row : -1;
    }

    /**
     * @see prefuse.visual.AggregateItem#getAggregateSize()
     */
    public int getAggregateSize() {
        return ((AggregateTable)m_table).getAggregateSize(m_row);
    }

    /**
     * @see prefuse.visual.AggregateItem#containsItem(prefuse.visual.VisualItem)
     */
    public boolean containsItem(VisualItem item) {
        return ((AggregateTable)m_table).aggregateContains(m_row, item);
    }

    /**
     * @see prefuse.visual.AggregateItem#addItem(prefuse.visual.VisualItem)
     */
    public void addItem(VisualItem item) {
        ((AggregateTable)m_table).addToAggregate(m_row, item);
    }

    /**
     * @see prefuse.visual.AggregateItem#removeItem(prefuse.visual.VisualItem)
     */
    public void removeItem(VisualItem item) {
        ((AggregateTable)m_table).removeFromAggregate(m_row, item);
    }

    /**
     * @see prefuse.visual.AggregateItem#removeAllItems()
     */
    public void removeAllItems() {
        ((AggregateTable)m_table).removeAllFromAggregate(m_row);
    }

    /**
     * @see prefuse.visual.AggregateItem#items()
     */
    public Iterator items() {
        return ((AggregateTable)m_table).aggregatedTuples(m_row);
    }
    
    /**
     * @see prefuse.visual.AggregateItem#items()
     */
    public Iterator items(Predicate filter) {
        return new FilterIterator(
            ((AggregateTable)m_table).aggregatedTuples(m_row), filter);
    }

} // end of class TableAggregateItem
