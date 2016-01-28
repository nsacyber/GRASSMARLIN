package prefuse.data;

import java.util.ArrayList;
import java.util.Iterator;

import javax.swing.event.TableModelEvent;

import prefuse.data.column.Column;
import prefuse.data.column.ColumnMetadata;
import prefuse.data.event.EventConstants;
import prefuse.data.event.ExpressionListener;
import prefuse.data.event.ProjectionListener;
import prefuse.data.event.TableListener;
import prefuse.data.expression.BooleanLiteral;
import prefuse.data.expression.Expression;
import prefuse.data.expression.Predicate;
import prefuse.data.tuple.TableTuple;
import prefuse.data.util.AcceptAllColumnProjection;
import prefuse.data.util.CascadedRowManager;
import prefuse.data.util.ColumnProjection;
import prefuse.util.collections.CompositeIterator;
import prefuse.util.collections.IntIterator;


/**
 * <p>Table subclass featuring a "cascaded" table design - a CascadedTable can
 * have a parent table, from which it inherits a potentially filtered set of
 * rows and columns. Child tables may override the columns of the parent by
 * having a column of the same name as that of the parent, in which case the
 * parent's column will not be accessible.</p>
 * 
 * <p>Table rows of the parent table can be selectively included by providing
 * a {@link prefuse.data.expression.Predicate} that filters the parent rows.
 * Columns of the parent table can be selectively included by providing
 * a {@link prefuse.data.util.ColumnProjection} indicating the columns to
 * include.</p>
 * 
 * <p>Tuple instances backed by a CascadedTable will be not be equivalent to
 * the tuples backed by the parent table. However, setting a value in a
 * CascadedTable that is inherited from a parent table <em>will</em> update
 * the value in the parent table.</p>
 * 
 * @author <a href="http://jheer.org">jeffrey heer</a>
 */
public class CascadedTable extends Table {

    /** Cascaded parent table */ 
    protected Table      m_parent;
    /** List of included parent column names */
    protected ArrayList  m_pnames;
    
    /** ColumnProjection determining which columns of the parent table
     * are included in this table. */
    protected ColumnProjection m_colFilter;
    /** Selection Predicate determining which rows of the parent table
     * are included in this table. */
    protected Predicate    m_rowFilter;
    
    /** An internal listener class */
    protected Listener m_listener;
    
    // ------------------------------------------------------------------------
    // Constructor
    
    /**
     * Create a new CascadedTable. By default all rows and columns of the
     * parent table are included in this one.
     * @param parent the parent Table to use
     */
    public CascadedTable(Table parent) {
        this(parent, null, null);
    }
    
    /**
     * Create a new CascadedTable. By default all columns of the parent
     * table are included in this one.
     * @param parent the parent Table to use
     * @param rowFilter a Predicate determining which rows of the parent
     * table to include in this one.
     */
    public CascadedTable(Table parent, Predicate rowFilter) {
        this(parent, rowFilter, null);
    }
    
    /**
     * Create a new CascadedTable. By default all rows of the parent
     * table are included in this one.
     * @param parent the parent Table to use
     * @param colFilter a ColumnProjection determining which columns of the
     * parent table to include in this one.
     */
    public CascadedTable(Table parent, ColumnProjection colFilter) {
        this(parent, null, colFilter);
    }
    
    /**
     * Create a new CascadedTable.
     * @param parent the parent Table to use
     * @param rowFilter a Predicate determining which rows of the parent
     * table to include in this one.
     * @param colFilter a ColumnProjection determining which columns of the
     * parent table to include in this one.
     */
    public CascadedTable(Table parent, Predicate rowFilter, 
                         ColumnProjection colFilter)
    {
        this(parent, rowFilter, colFilter, TableTuple.class);
    }
    
    /**
     * Create a new CascadedTable.
     * @param parent the parent Table to use
     * @param rowFilter a Predicate determining which rows of the parent
     * table to include in this one.
     * @param colFilter a ColumnProjection determining which columns of the
     * parent table to include in this one.
     * @param tupleType the class type of the Tuple instances to use
     */
    protected CascadedTable(Table parent, Predicate rowFilter, 
            ColumnProjection colFilter, Class tupleType)
    {
        super(0, 0, tupleType);
        m_parent = parent;
        m_pnames = new ArrayList();
        m_rows = new CascadedRowManager(this);
        m_listener = new Listener();
        
        setColumnProjection(colFilter);
        setRowFilter(rowFilter);
        m_parent.addTableListener(m_listener);
    }
    
    // -- non-cascading version -----------------------------------------------
    
    /**
     * Create a CascadedTable without a backing parent table.
     */
    protected CascadedTable() {
        this(TableTuple.class);
    }

    /**
     * Create a CascadedTable without a backing parent table.
     * @param tupleType the class type of the Tuple instances to use
     */
    protected CascadedTable(Class tupleType) {
        super(0, 0, tupleType);
        m_pnames = new ArrayList();
    }
    
    // ------------------------------------------------------------------------
    // Filter Methods
    
    /**
     * Determines which columns are inherited from the backing parent table.
     */
    protected void filterColumns() {
        if ( m_parent == null ) return;
        
        for ( int i=0; i<m_pnames.size(); ++i ) {
            String name = (String)m_pnames.get(i);
            Column col = m_parent.getColumn(i);
            boolean contained = m_names.contains(name);
            if ( !m_colFilter.include(col, name) || contained ) {
                m_pnames.remove(i--);
                if ( !contained ) {
                    ((ColumnEntry)m_entries.get(name)).dispose();
                    m_entries.remove(name);
                }
                
                // fire notification
                fireTableEvent(m_rows.getMinimumRow(), 
                               m_rows.getMaximumRow(), 
                               i, EventConstants.DELETE);
            }
        }
        
        m_pnames.clear();

        Iterator pcols = m_parent.getColumnNames();
        for ( int i=0, j=m_columns.size(); pcols.hasNext(); ++i ) {
            String name = (String)pcols.next();
            Column col  = m_parent.getColumn(i);
            
            if ( m_colFilter.include(col, name) && !m_names.contains(name) ) {
                m_pnames.add(name);
                ColumnEntry entry = (ColumnEntry)m_entries.get(name);
                if ( entry == null ) {
                    entry = new ColumnEntry(j++, col, 
                            new ColumnMetadata(this, name));
                    m_entries.put(name, entry);
                    // fire notification
                    fireTableEvent(m_rows.getMinimumRow(), 
                                   m_rows.getMaximumRow(), 
                                   i, EventConstants.INSERT);
                } else {
                    entry.colnum = j++;
                }
                m_lastCol = m_columns.size()-1;
            }
        }
        
    }
    
    /**
     * Manually trigger a re-filtering of the rows of this table. If the
     * filtering predicate concerns only items within this table, calling
     * this method should be unnecessary. It is only when the filtering
     * predicate references data outside of this table that a manual
     * re-filtering request may be necessary. For example, filtering
     * valid edges of a graph from a pool of candidate edges will depend
     * on the available nodes.
     * @see prefuse.data.util.ValidEdgePredicate
     */
    public void filterRows() {
        if ( m_parent == null ) return;
        
        CascadedRowManager rowman = (CascadedRowManager)m_rows;
        IntIterator crows = m_rows.rows();
        while ( crows.hasNext() ) {
            int crow = crows.nextInt();
            if ( !m_rowFilter.getBoolean(
                    m_parent.getTuple(rowman.getParentRow(crow))) )
            {
                removeCascadedRow(crow);
            }
        }
        
        Iterator ptuples = m_parent.tuples(m_rowFilter);
        while ( ptuples.hasNext() ) {
            Tuple pt = (Tuple)ptuples.next();
            int prow = pt.getRow();
            if ( rowman.getChildRow(prow) == -1 )
                addCascadedRow(prow);
        }
    }
    
    /**
     * Get the ColumnProjection determining which columns of the
     * parent table are included in this one.
     * @return the ColumnProjection of this CascadedTable
     */
    public ColumnProjection getColumnProjection() {
    	return m_colFilter;
    }
    
    /**
     * Sets the ColumnProjection determining which columns of the
     * parent table are included in this one.
     * @param colFilter a ColumnProjection determining which columns of the
     * parent table to include in this one.
     */
    public void setColumnProjection(ColumnProjection colFilter) {
        if ( m_colFilter != null ) {
        	m_colFilter.removeProjectionListener(m_listener);
        }
        m_colFilter = colFilter==null ? new AcceptAllColumnProjection() : colFilter;
        m_colFilter.addProjectionListener(m_listener);
        filterColumns();
    }
    
    /**
     * Gets ths Predicate determining which rows of the parent
     * table are included in this one.
     * @return the row filtering Predicate of this CascadedTable
     */
    public Predicate getRowFilter() {
    	return m_rowFilter;
    }
    
    /**
     * Sets the Predicate determining which rows of the parent
     * table are included in this one.
     * @param rowFilter a Predicate determining which rows of the parent
     * table to include in this one.
     */
    public void setRowFilter(Predicate rowFilter) {
    	if ( m_rowFilter != null ) {
    		m_rowFilter.removeExpressionListener(m_listener);
    	}
        m_rowFilter = rowFilter==null ? BooleanLiteral.TRUE : rowFilter;
        if ( m_rowFilter != BooleanLiteral.TRUE )
            m_rowFilter.addExpressionListener(m_listener);
        filterRows();
    }
    
    // ------------------------------------------------------------------------
    // Table Metadata
    
    /**
     * @see prefuse.data.Table#getColumnCount()
     */
    public int getColumnCount() {
        return m_columns.size() + m_pnames.size();
    }
    
    /**
     * Get the number of columns explicitly stored by this table (i.e., all
     * columns that are not inherited from the parent table).
     * @return the number of locally stored columns
     */
    public int getLocalColumnCount() {
        return m_columns.size();
    }
    
    // ------------------------------------------------------------------------
    // Parent Table Methods
    
    /**
     * Get the parent table from which this cascaded table inherits values.
     * @return the parent table
     */
    public Table getParentTable() {
        return m_parent;
    }
    
    /**
     * Given a row in this table, return the corresponding row in the parent
     * table.
     * @param row a row in this table
     * @return the corresponding row in the parent table
     */
    public int getParentRow(int row) {
        return ((CascadedRowManager)m_rows).getParentRow(row);
    }
    
    /**
     * Given a row in the parent table, return the corresponding row, if any,
     * in this table.
     * @param prow a row in the parent table
     * @return the corresponding row in this table, or -1 if the given parent
     * row is not inherited by this table
     */
    public int getChildRow(int prow) {
        return ((CascadedRowManager)m_rows).getChildRow(prow);
    }
    
    // ------------------------------------------------------------------------
    // Row Operations
    
    /**
     * @see prefuse.data.Table#addRow()
     */
    public int addRow() {
        if ( m_parent != null ) {
            throw new IllegalStateException(
                "Add row not supported for CascadedTable.");
        } else {
            return super.addRow();
        }
    }
    
    /**
     * @see prefuse.data.Table#addRows(int)
     */
    public void addRows(int nrows) {
        if ( m_parent != null ) {
            throw new IllegalStateException(
                "Add rows not supported for CascadedTable.");
        } else {
            super.addRows(nrows);
        }
    }
    
    /**
     * @see prefuse.data.Table#removeRow(int)
     */
    public boolean removeRow(int row) {
        if ( m_parent != null ) {
            throw new IllegalStateException(
                "Remove row not supported for CascadedTable.");
        } else {
            return super.removeRow(row);
        }
    }

    /**
     * Internal method for adding a new cascaded row backed by
     * the given parent row.
     * @param prow the parent row to inherit
     * @return the row number ofr the newly added row in this table
     */
    protected int addCascadedRow(int prow) {
        int r = m_rows.addRow();
        ((CascadedRowManager)m_rows).put(r, prow);
        updateRowCount();
        
        fireTableEvent(r, r, TableModelEvent.ALL_COLUMNS,
                       TableModelEvent.INSERT);        
        return r;
    }
    
    /**
     * Internal method for removing a cascaded row from this table.
     * @param row the row to remove
     * @return true if the row was successfully removed, false otherwise
     */
    protected boolean removeCascadedRow(int row) {
        boolean rv = super.removeRow(row);
        if ( rv )
            ((CascadedRowManager)m_rows).remove(row);
        return rv;
    }
    
    // ------------------------------------------------------------------------
    // Column Operations
    
    /**
     * @see prefuse.data.Table#getColumnName(int)
     */
    public String getColumnName(int col) {
        int local = m_names.size();
        if ( col >= local ) {
            return (String)m_pnames.get(col-local);
        } else {
            return (String)m_names.get(col);
        }
    }
    
    /**
     * @see prefuse.data.Table#getColumnNumber(prefuse.data.column.Column)
     */
    public int getColumnNumber(Column col) {
        int idx = m_columns.indexOf(col);
        if ( idx == -1 && m_parent != null ) {
            idx = m_parent.getColumnNumber(col);
            if ( idx == -1 ) return idx;
            String name = m_parent.getColumnName(idx);
            idx = m_pnames.indexOf(name);
            if ( idx != -1 ) idx += m_columns.size();
        }
        return idx;
     }
    
    /**
     * @see prefuse.data.Table#getColumn(int)
     */
    public Column getColumn(int col) {
        m_lastCol = col;
        int local = m_names.size();
        if ( col >= local && m_parent != null ) {
            return m_parent.getColumn((String)m_pnames.get(col-local));
        } else {
            return (Column)m_columns.get(col);
        }
    }
    
    /**
     * @see prefuse.data.Table#hasColumn(java.lang.String)
     */
    protected boolean hasColumn(String name) {
        int idx = getColumnNumber(name);
        return idx >= 0 && idx < getLocalColumnCount();
    }
    
    /**
     * @see prefuse.data.Table#getColumnNames()
     */
    protected Iterator getColumnNames() {
        if ( m_parent == null ) {
            return m_names.iterator();
        } else {
            return new CompositeIterator(m_names.iterator(),
                                         m_pnames.iterator());
        }
    }
    
    /**
     * Invalidates this table's cached schema. This method should be called
     * whenever columns are added or removed from this table.
     */
    protected void invalidateSchema() {
        super.invalidateSchema();
        this.filterColumns();
    }
    
    // ------------------------------------------------------------------------
    // Listener Methods
    
    /**
     * Internal listener class handling updates from the backing parent table,
     * the column projection, or the row selection predicate.
     */
    private class Listener
        implements TableListener, ProjectionListener, ExpressionListener
    {
        public void tableChanged(Table t, int start, int end, int col, int type) {
            // must come from parent
            if ( t != m_parent )
                return;
            
            CascadedRowManager rowman = (CascadedRowManager)m_rows;
            
            // switch on the event type
            switch ( type ) {
            case EventConstants.UPDATE:
            {
                // do nothing if update on all columns, as this is only
                // used to indicate a non-measurable update.
                if ( col == EventConstants.ALL_COLUMNS ) {
                    break;
                }
                
                // process each update, check if filtered state changes
                for ( int r=start, cr=-1; r<=end; ++r ) {
                    if ( (cr=rowman.getChildRow(r)) != -1 ) {
                        // the parent row has a corresponding row in this table
                        if ( m_rowFilter.getBoolean(m_parent.getTuple(r)) ) {
                            // row still passes the filter, check the column
                            int idx = getColumnNumber(m_parent.getColumnName(col));
                            if ( idx >= getLocalColumnCount() )
                                fireTableEvent(cr, cr, idx, EventConstants.UPDATE);
                        } else {
                            // row no longer passes the filter, remove it
                            removeCascadedRow(cr);
                        }
                    } else {
                        // does it now pass the filter due to the update?
                        if ( m_rowFilter.getBoolean(m_parent.getTuple(r)) ) {
                            if ( (cr=rowman.getChildRow(r)) < 0 )
                                addCascadedRow(r);
                        }
                    }
                }
                break;
            }
            case EventConstants.DELETE:
            {
                if ( col == EventConstants.ALL_COLUMNS ) {
                    // entire rows deleted
                    for ( int r=start, cr=-1; r<=end; ++r ) {
                        if ( (cr=rowman.getChildRow(r)) != -1 )
                            removeCascadedRow(cr);
                    }
                } else {
                    // column deleted
                    filterColumns();
                }
                break;
            }
            case EventConstants.INSERT:
                if ( col == EventConstants.ALL_COLUMNS ) {
                    // entire rows added
                    for ( int r=start; r<=end; ++r ) {
                        if ( m_rowFilter.getBoolean(m_parent.getTuple(r)) ) {
                            if ( rowman.getChildRow(r) < 0 )
                                addCascadedRow(r);
                        }
                    }
                } else {
                    // column added
                    filterColumns();
                }
                break;
            }
        }
    
        public void projectionChanged(ColumnProjection projection) {
            if ( projection == m_colFilter )
                filterColumns();
        }
    
        public void expressionChanged(Expression expr) {
            if ( expr == m_rowFilter )
                filterRows();
        }
    }
    
} // end of class CascadedTable
