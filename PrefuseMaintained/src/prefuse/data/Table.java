package prefuse.data;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;

import javax.swing.event.TableModelEvent;

import prefuse.data.column.Column;
import prefuse.data.column.ColumnFactory;
import prefuse.data.column.ColumnMetadata;
import prefuse.data.event.ColumnListener;
import prefuse.data.event.EventConstants;
import prefuse.data.event.TableListener;
import prefuse.data.expression.Expression;
import prefuse.data.expression.Predicate;
import prefuse.data.expression.parser.ExpressionParser;
import prefuse.data.tuple.AbstractTupleSet;
import prefuse.data.tuple.TableTuple;
import prefuse.data.tuple.TupleManager;
import prefuse.data.util.FilterIteratorFactory;
import prefuse.data.util.Index;
import prefuse.data.util.RowManager;
import prefuse.data.util.Sort;
import prefuse.data.util.TableIterator;
import prefuse.data.util.TreeIndex;
import prefuse.util.TypeLib;
import prefuse.util.collections.CopyOnWriteArrayList;
import prefuse.util.collections.IncompatibleComparatorException;
import prefuse.util.collections.IntIterator;


/**
 * <p>A Table organizes a collection of data into rows and columns, each row 
 * containing a data record, and each column containing data values for a
 * named data field with a specific data type. Table data can be accessed
 * directly using the row number and column name, or rows can be treated
 * in an object-oriented fashion using {@link prefuse.data.Tuple}
 * instances that represent a single row of data in the table. As such,
 * tables implement the {@link prefuse.data.tuple.TupleSet} interface.</p>
 * 
 * <p>Table rows can be inserted or deleted. In any case, none of the other
 * existing table rows are effected by an insertion or deletion. A deleted
 * row simply becomes invalid--any subsequent attempts to access the row
 * either directly or through a pre-existing Tuple instance will result
 * in an exception. However, if news rows are later added to the table,
 * the row number for previously deleted nodes can be reused. In fact, the
 * lower row number currently unused is assigned to the new row. This results
 * in an efficient reuse of the table rows, but carries an important side
 * effect -- rows do not necesarily maintain the order in which they were
 * added once deletions have occurred on the table. If not deletions
 * occur, the ordering of table rows will reflect the order in which
 * rows were added to the table.</p>
 * 
 * <p>Collections of table rows can be accessed using both iterators over
 * the actual row numbers and iterators over the Tuple instances that
 * encapsulate access to that row. Both types of iteration can also be
 * filtered by providing a {@link prefuse.data.expression.Predicate},
 * allowing tables to be queried for specific values.</p>
 * 
 * <p>Columns (alternativele referred to as data fields) can be added to
 * the Table using {@link #addColumn(String, Class)} and a host of
 * similar methods. This method will automatically determine the right
 * kind of backing column instance to use. Furthermore, Table columns
 * can be specified using a {@link Schema} instance, which describes
 * the column names, data types, and default values. The Table class
 * also maintains its own internal Schema, which be accessed (in a
 * read-only way) using the {@link #getSchema()} method.</p>
 * 
 * <p>Tables also support additional structures. The {@link ColumnMetadata}
 * class returned by the {@link #getMetadata(String)} method supports
 * calculation of different statistics for a column, including minimum
 * and maximum values, and the number of unique data values in the column.
 * {@link prefuse.data.util.Index} instances can be created and retrieved
 * using the {@link #index(String)} method and retrieved without triggering
 * creation using {@link #getIndex(String)} method. An index keeps a
 * sorted collection of all data values in a column, accelerating the creation
 * of filtered iterators by optimizing query calculations and also providing
 * faster computation of many of the {@link ColumnMetadata} methods. If
 * you will be issuing a number of queries (i.e., requesting filtered
 * iterators) dependent on the values of a given column, indexing that column
 * may result in a significant performance increase, though at the cost
 * of storing and maintaining the backing index structure.</p>  
 * 
 * @author <a href="http://jheer.org">jeffrey heer</a>
 */
public class Table extends AbstractTupleSet implements ColumnListener {
    
    /** Listeners for changes to this table */
    protected CopyOnWriteArrayList m_listeners;
    
    /** Locally stored data columns */
    protected ArrayList m_columns;
    /** Column names for locally store data columns */
    protected ArrayList m_names;
    
    /** Mapping between column names and column entries
     * containing column, metadata, and index references */ 
    protected HashMap m_entries;
    
    /** Manager for valid row indices */
    protected RowManager m_rows;
    
    /** manager for tuples, which are object representations for rows */
    protected TupleManager m_tuples;
    
    /** Tracks the number of edits of this table */
    protected int m_modCount = 0;
    
    /** Memoize the index of the last column operated on,
     * used to expedite handling of column updates. */
    protected int m_lastCol = -1;
    
    /** A cached schema instance, loaded lazily */
    protected Schema m_schema;
    
    // ------------------------------------------------------------------------
    // Constructors
    
    /**
     * Create a new, empty Table. Rows can be added to the table using
     * the {@link #addRow()} method.
     */
    public Table() {
        this(0, 0);
    }
    
    /**
     * Create a new Table with a given number of rows, and the starting
     * capacity for a given number of columns.
     * @param nrows the starting number of table rows
     * @param ncols the starting capacity for columns 
     */
    public Table(int nrows, int ncols) {
        this(nrows, ncols, TableTuple.class);
    }
    
    /**
     * Create a new Table.
     * @param nrows the starting number of table rows
     * @param ncols the starting capacity for columns 
     * @param tupleType the class of the Tuple instances to use
     */
    protected Table(int nrows, int ncols, Class tupleType) {
        m_listeners = new CopyOnWriteArrayList();
        m_columns = new ArrayList(ncols);
        m_names = new ArrayList(ncols);
        m_rows = new RowManager(this);
        m_entries = new HashMap(ncols+5);        
        m_tuples = new TupleManager(this, null, tupleType);

        if ( nrows > 0 )
            addRows(nrows);
    }
    
    // ------------------------------------------------------------------------
    // Table Metadata
    
    /**
     * Get the number of columns / data fields in this table.
     * @return the number of columns 
     */
    public int getColumnCount() {
        return m_columns.size();
    }

    /**
     * Get the data type of the column at the given column index.
     * @param col the column index
     * @return the data type (as a Java Class) of the column
     */
    public Class getColumnType(int col) {
        return getColumn(col).getColumnType();
    }

    /**
     * Get the data type of the column with the given data field name.
     * @param field the column / data field name
     * @return the data type (as a Java Class) of the column
     */
    public Class getColumnType(String field) {
        Column c = getColumn(field); 
        return (c==null ? null : c.getColumnType());
    }

    /**
     * Get the number of rows in the table.
     * @return the number of rows
     */
    public int getRowCount() {
        return m_rows.getRowCount();
    }
    
    /**
     * Get the minimum row index currently in use by this Table.
     * @return the minimum row index
     */
    public int getMinimumRow() {
        return m_rows.getMinimumRow();
    }

    /**
     * Get the maximum row index currently in use by this Table.
     * @return the maximum row index
     */
    public int getMaximumRow() {
        return m_rows.getMaximumRow();
    }
    
    /**
     * Indicates if the value of the given table cell can be changed. 
     * @param row the row number
     * @param col the column number
     * @return true if the value can be edited/changed, false otherwise
     */
    public boolean isCellEditable(int row, int col) {
        if ( !m_rows.isValidRow(row) ) {
            return false;
        } else {
            return getColumn(col).isCellEditable(row);
        }
    }
    
    /**
     * Get the number of times this Table has been modified. Adding rows,
     * deleting rows, and updating table cell values all contribute to
     * this count.
     * @return the number of modifications to this table
     */
    public int getModificationCount() {
        return m_modCount;
    }
    
    /**
     * Sets the TupleManager used by this Table. Use this method
     * carefully, as it will cause all existing Tuples retrieved
     * from this Table to be invalidated.
     * @param tm the TupleManager to use
     */
    public void setTupleManager(TupleManager tm) {
        m_tuples.invalidateAll();
        m_tuples = tm;
    }
    
    /**
     * Returns this Table's schema. The returned schema will be
     * locked, which means that any attempts to edit the returned schema
     * by adding additional columns will result in a runtime exception.
     * 
     * If this Table subsequently has columns added or removed, this will not
     * be reflected in the returned schema. Instead, this method will need to
     * be called again to get a current schema. Accordingly, it is not
     * recommended that Schema instances returned by this method be stored
     * or reused across scopes unless that exact schema snapshot is
     * desired.
     * 
     * @return a copy of this Table's schema
     */
    public Schema getSchema() {
        if ( m_schema == null ) {
            Schema s = new Schema();
            for ( int i=0; i<getColumnCount(); ++i ) {
                s.addColumn(getColumnName(i), getColumnType(i), 
                            getColumn(i).getDefaultValue());
            }
            s.lockSchema();
            m_schema = s;
        }
        return m_schema;
    }
    
    /**
     * Invalidates this table's cached schema. This method should be called
     * whenever columns are added or removed from this table.
     */
    protected void invalidateSchema() {
        m_schema = null;
    }
    
    // ------------------------------------------------------------------------
    // Row Operations
    
    /**
     * Get the row value for accessing an underlying Column instance,
     * corresponding to the given table cell. For basic tables this just
     * returns the input row value. However, for tables that inherit
     * data columns from a parent table and present a filtered view on
     * this data, a mapping between the row numbers of the table and
     * the row numbers of the backing data column is needed. In those cases,
     * this method returns the result of that mapping. The method
     * {@link #getTableRow(int, int)} accesses this map in the reverse
     * direction.
     * @param row the table row to lookup
     * @param col the table column to lookup
     * @return the column row number for accessing the desired table cell
     */
    public int getColumnRow(int row, int col) {
        return m_rows.getColumnRow(row, col);
    }
    
    /**
     * Get the row number for this table given a row number for a backing
     * data column and the column number for the data column. For basic
     * tables this just returns the column row value. However, for tables that
     * inherit data columns from a parent table and present a filtered view on
     * this data, a mapping between the row numbers of the table and
     * the row numbers of the backing data column is needed. In those cases,
     * this method returns the result of this mapping, in the direction of
     * the backing column rows to the table rows of the cascaded table. The
     * method {@link #getColumnRow(int, int)} accesses this map in the reverse
     * direction.
     * @param colrow the row of the backing data column
     * @param col the table column to lookup.
     * @return the table row number for accessing the desired table cell
     */
    public int getTableRow(int colrow, int col) {
        return m_rows.getTableRow(colrow, col);
    }
    
    /**
     * Add a row to this table. All data columns will be notified and will
     * take on the appropriate default values for the added row.
     * @return the row number of the newly added row
     */
    public int addRow() {
        int r = m_rows.addRow();
        updateRowCount();
        
        fireTableEvent(r, r, TableModelEvent.ALL_COLUMNS,
                       TableModelEvent.INSERT);        
        return r;
    }
    
    /**
     * Add a given number of rows to this table. All data columns will be
     * notified and will take on the appropriate default values for the
     * added rows.
     * @param nrows the number of rows to add.
     */
    public void addRows(int nrows) {
        for ( int i=0; i<nrows; ++i ) {
            addRow();
        }
    }
    
    /**
     * Internal method that updates the row counts for local data columns.
     */
    protected void updateRowCount() {
        int maxrow = m_rows.getMaximumRow() + 1;
        
        // update columns
        Iterator cols = getColumns();
        while ( cols.hasNext() ) {
            Column c = (Column)cols.next();
            c.setMaximumRow(maxrow);
        }
    }
    
    /**
     * Removes a row from this table.
     * @param row the row to delete
     * @return true if the row was successfully deleted, false if the
     * row was already invalid
     */
    public boolean removeRow(int row) {
        if ( m_rows.isValidRow(row) ) {
            // the order of operations here is extremely important
            // otherwise listeners may end up with corrupted state.
            // fire update *BEFORE* clearing values
            // allow listeners (e.g., indices) to perform clean-up
            fireTableEvent(row, row, TableModelEvent.ALL_COLUMNS, 
                           TableModelEvent.DELETE);
            // invalidate the tuple
            m_tuples.invalidate(row);
            // release row with row manager
            // do this before clearing column values, so that any
            // listeners can determine that the row is invalid
            m_rows.releaseRow(row);
            // now clear column values
            for ( Iterator cols = getColumns(); cols.hasNext(); ) {
                Column c = (Column)cols.next();
                c.revertToDefault(row);
            }
            return true;
        }
        return false;
    }
    
    /**
     * Clear this table, removing all rows.
     * @see prefuse.data.tuple.TupleSet#clear()
     */
    public void clear() {
        IntIterator rows = rows(true);
        while ( rows.hasNext() ) {
            removeRow(rows.nextInt());
        }
    }
    
    /**
     * Indicates if the given row number corresponds to a valid table row.
     * @param row the row number to check for validity
     * @return true if the row is valid, false if it is not
     */
    public boolean isValidRow(int row) {
        return m_rows.isValidRow(row);
    }
    
    // ------------------------------------------------------------------------
    // Column Operations
    
    /**
     * Internal method indicating if the given data field is included as a
     * data column.
     */
    protected boolean hasColumn(String name) {
        return getColumnNumber(name) != -1;
    }
    
    /**
     * Get the data field name of the column at the given column number.
     * @param col the column number
     * @return the data field name of the column
     */
    public String getColumnName(int col) {
        return (String)m_names.get(col);
    }

    /**
     * Get the column number for a given data field name.
     * @param field the name of the column to lookup
     * @return the column number of the column, or -1 if the name is not found
     */
    public int getColumnNumber(String field) {
        ColumnEntry e = (ColumnEntry)m_entries.get(field);
        return ( e==null ? -1 : e.colnum );
    }

    /**
     * Get the column number for the given Column instance.
     * @param col the Column instance to lookup
     * @return the column number of the column, or -1 if the name is not found
     */
    public int getColumnNumber(Column col) {
        return m_columns.indexOf(col);
    }
    
    /**
     * Get the column at the given column number.
     * @param col the column number
     * @return the Column instance
     */
    public Column getColumn(int col) {
        m_lastCol = col;
        return (Column)m_columns.get(col);
    }
    
    /**
     * Get the column with the given data field name
     * @param field the data field name of the column
     * @return the Column instance
     */
    public Column getColumn(String field) {
        ColumnEntry e = (ColumnEntry)m_entries.get(field);
        return ( e != null ? e.column : null );
    }
    
    /**
     * Add a column with the given name and data type to this table.
     * @param name the data field name for the column
     * @param type the data type, as a Java Class, for the column
     * @see prefuse.data.tuple.TupleSet#addColumn(java.lang.String, java.lang.Class)
     */
    public void addColumn(String name, Class type) {
        addColumn(name, type, null);
    }

    /**
     * Add a column with the given name and data type to this table.
     * @param name the data field name for the column
     * @param type the data type, as a Java Class, for the column
     * @param defaultValue the default value for column data values
     * @see prefuse.data.tuple.TupleSet#addColumn(java.lang.String, java.lang.Class, java.lang.Object)
     */
    public void addColumn(String name, Class type, Object defaultValue) {
        
        if( type.isEnum() ) {
            type = String.class;
            defaultValue = ((Enum)defaultValue).name();
        }
        
        Column col = ColumnFactory.getColumn(type, 
                        m_rows.getMaximumRow()+1, defaultValue);
        addColumn(name, col);
    }
    
    /**
     * Add a derived column to this table, using an Expression instance to
     * dynamically calculate the column data values.
     * @param name the data field name for the column
     * @param expr a String expression in the prefuse expression language, to
     * be parsed into an {@link prefuse.data.expression.Expression} instance.
     * The string is parsed by the
     * {@link prefuse.data.expression.parser.ExpressionParser}. If an error
     * occurs during parsing, an exception will be thrown. 
     * @see prefuse.data.tuple.TupleSet#addColumn(java.lang.String, java.lang.String)
     */
    public void addColumn(String name, String expr) {
        Expression ex = ExpressionParser.parse(expr);
        Throwable t = ExpressionParser.getError();
        if ( t != null ) {
            throw new RuntimeException(t);
        } else {
            addColumn(name, ex);
        }
    }
    
    /**
     * Add a derived column to this table, using an Expression instance to
     * dynamically calculate the column data values.
     * @param name the data field name for the column
     * @param expr the Expression that will determine the column values
     * @see prefuse.data.tuple.TupleSet#addColumn(java.lang.String, prefuse.data.expression.Expression)
     */
    public void addColumn(String name, Expression expr) {
        addColumn(name, ColumnFactory.getColumn(this, expr));
    }
    
    /**
     * Add a constant column to this table, which returns one constant value
     * for all column rows.
     * @param name the data field name for the column
     * @param type the data type, as a Java Class, for the column
     * @param dflt the default value for column data values
     */
    public void addConstantColumn(String name, Class type, Object dflt) {
        addColumn(name, ColumnFactory.getConstantColumn(type, dflt));
    }
    
    /**
     * Internal method for adding a column.
     * @param name the name of the column
     * @param col the actual Column instance
     */
    protected void addColumn(String name, Column col) {
        int idx = getColumnNumber(name);
        if ( idx >= 0 && idx < m_columns.size() ) {
            throw new IllegalArgumentException(
                "Table already has column with name \""+name+"\"");
        }
        
        // add the column
        m_columns.add(col);
        m_names.add(name);
        m_lastCol = m_columns.size()-1;
        ColumnEntry entry = new ColumnEntry(m_lastCol, col, 
                new ColumnMetadata(this, name));
        
        // add entry, dispose of an overridden entry if needed
        ColumnEntry oldEntry = (ColumnEntry)m_entries.put(name, entry);
        if ( oldEntry != null ) oldEntry.dispose();
        
        invalidateSchema();
        
        // listen to what the column has to say
        col.addColumnListener(this);
        
        // fire notification
        fireTableEvent(m_rows.getMinimumRow(), m_rows.getMaximumRow(), 
                m_lastCol, TableModelEvent.INSERT);
    }

    /**
     * Internal method for removing a column.
     * @param idx the column number of the column to remove
     * @return the removed Column instance
     */
    protected Column removeColumn(int idx) {
        // make sure index is legal
        if ( idx < 0 || idx >= m_columns.size() ) {
            throw new IllegalArgumentException("Column index is not legal.");
        }
        
        String name = (String)m_names.get(idx);
        ((ColumnEntry)m_entries.get(name)).dispose();
        Column col = (Column)m_columns.remove(idx);
        m_entries.remove(name);
        m_names.remove(idx);
        renumberColumns();
        
        m_lastCol = -1;
        invalidateSchema();
        
        // ignore what the old column has to say
        col.removeColumnListener(this);
        
        // fire notification
        fireTableEvent(m_rows.getMinimumRow(), m_rows.getMaximumRow(), 
                       idx, TableModelEvent.DELETE);
        
        return col;
    }
    
    /**
     * Remove a data field from this table
     * @param field the name of the data field / column to remove
     * @return the removed Column instance
     */
    public Column removeColumn(String field) {
        int idx = m_names.indexOf(field);
        if ( idx < 0 ) {
            throw new IllegalArgumentException("No such column.");
        }
        return removeColumn(idx);
    }
    
    /**
     * Remove a column from this table
     * @param c the column instance to remove
     */
    public void removeColumn(Column c) {
        int idx = m_columns.indexOf(c);
        if ( idx < 0 ) {
            throw new IllegalArgumentException("No such column.");
        }
        removeColumn(idx);
    }
    
    /**
     * Internal method that re-numbers columns upon column removal.
     */
    protected void renumberColumns() {
        Iterator iter = m_names.iterator();
        for ( int idx=0; iter.hasNext(); ++idx ) {
            String name = (String)iter.next();
            ColumnEntry e = (ColumnEntry)m_entries.get(name);
            e.colnum = idx;
        }
    }
    
    /**
     * Internal method that returns an iterator over columns
     * @return an iterator over columns
     */
    protected Iterator getColumns() {
        return m_columns.iterator();
    }

    /**
     * Internal method that returns an iterator over column names
     * @return an iterator over column name
     */
    protected Iterator getColumnNames() {
        return m_names.iterator();
    }
    
    // ------------------------------------------------------------------------
    // Column Metadata
    
    /**
     * Return a metadata instance providing summary information about a column.
     * @param field the data field name of the column
     * @return the columns' associated ColumnMetadata instance
     */
    public ColumnMetadata getMetadata(String field) {
        ColumnEntry e = (ColumnEntry)m_entries.get(field);
        if ( e == null ) {
            throw new IllegalArgumentException("Unknown column name: "+field);
        }
        return e.metadata;
    }
    
    // ------------------------------------------------------------------------
    // Index Methods
    
    /**
     * Create (if necessary) and return an index over the given data field.
     * The first call to this method with a given field name will cause the
     * index to be created and stored. Subsequent calls will simply return
     * the stored index. To attempt to retrieve an index without triggering
     * creation of a new index, use the {@link #getIndex(String)} method.
     * @param field the data field name of the column to index
     * @return the index over the specified data column
     */
    public Index index(String field) {
        ColumnEntry e = (ColumnEntry)m_entries.get(field);
        if ( e == null ) {
            throw new IllegalArgumentException("Unknown column name: "+field);
        } else if ( e.index != null ) {
            return e.index; // already indexed
        }
        
        Column col = e.column;
        try {
            e.index = new TreeIndex(this, m_rows, col, null);
        } catch ( IncompatibleComparatorException ice ) { /* can't happen */ }
        
        return e.index;
    }
    
    /**
     * Retrieve, without creating, an index for the given data field.
     * @param field the data field name of the column
     * @return the stored index for the column, or null if no index has
     * been created
     */
    public Index getIndex(String field) {
        ColumnEntry e = (ColumnEntry)m_entries.get(field);
        if ( e == null ) {
            throw new IllegalArgumentException("Unknown column name: "+field);
        }
        return e.index;
    }
    
    /**
     * Internal method for index creation and retrieval.
     * @param field the data field name of the column
     * @param expType the expected data type of the index
     * @param create indicates whether or not a new index should be created
     * if none currently exists for the given data field
     * @return the Index for the given data field
     */
    protected Index getIndex(String field, Class expType, boolean create) {
        if ( !expType.equals(getColumnType(field)) ) {
            // TODO: need more nuanced type checking here?
            throw new IllegalArgumentException("Column type does not match.");
        }
        if ( getIndex(field)==null && create) {
            index(field);
        }
        return getIndex(field);
    }
    
    /**
     * Remove the Index associated with the given data field / column name.
     * @param field the name of the column for which to remove the index
     * @return true if an index was successfully removed, false if no
     * such index was found
     */
    public boolean removeIndex(String field) {
        ColumnEntry e = (ColumnEntry)m_entries.get(field);
        if ( e == null ) {
            throw new IllegalArgumentException("Unknown column name: "+field);
        }
        if ( e.index == null ) {
            return false;
        } else {
            e.index.dispose();
            e.index = null;
            return true;
        }
    }
    
    // ------------------------------------------------------------------------
    // Tuple Methods
    
    /**
     * Get the Tuple instance providing object-oriented access to the given
     * table row.
     * @param row the table row
     * @return the Tuple for the given table row
     */
    public Tuple getTuple(int row) {
        return m_tuples.getTuple(row);
    }
    
    /**
     * Add a Tuple to this table. If the Tuple is already a member of this
     * table, nothing is done and null is returned. If the Tuple is not
     * a member of this Table but has a compatible data schema, as
     * determined by {@link Schema#isAssignableFrom(Schema)}, a new row
     * is created, the Tuple's values are copied, and the new Tuple that
     * is a member of this Table is returned. If the data schemas are not
     * compatible, nothing is done and null is returned.
     * @param t the Tuple to "add" to this table
     * @return the actual Tuple instance added to this table, or null if
     * no new Tuple has been added
     * @see prefuse.data.tuple.TupleSet#addTuple(prefuse.data.Tuple)
     */
    public Tuple addTuple(Tuple t) {
        if ( t.getTable() == this ) {
            return null;
        } else {
            Schema s = t.getSchema();
            if ( getSchema().isAssignableFrom(s) ) {
                int r = addRow();
                for ( int i=0; i<s.getColumnCount(); ++i ) {
                    String field = s.getColumnName(i);
                    this.set(r, field, t.get(i));
                }
                return getTuple(r);
            } else {
                return null;
            }
        }
    }
    
    /**
     * Clears the contents of this table and then attempts to add the given
     * Tuple instance.
     * @param t the Tuple to make the sole tuple in thie table
     * @return the actual Tuple instance added to this table, or null if
     * no new Tuple has been added
     * @see prefuse.data.tuple.TupleSet#setTuple(prefuse.data.Tuple)
     */
    public Tuple setTuple(Tuple t) {
        clear();
        return addTuple(t);
    }
    
    /**
     * Remove a tuple from this table. If the Tuple is a member of this table,
     * its row is deleted from the table. Otherwise, nothing is done.
     * @param t the Tuple to remove from the table
     * @return true if the Tuple row was successfully deleted, false if the
     * Tuple is invalid or not a member of this table
     * @see prefuse.data.tuple.TupleSet#removeTuple(prefuse.data.Tuple)
     */
    public boolean removeTuple(Tuple t) {
        if ( containsTuple(t) ) {
            removeRow(t.getRow());
            return true;
        } else {
            return false;
        }
    }
    
    /**
     * Indicates if this table contains the given Tuple instance.
     * @param t the Tuple to check for containment
     * @return true if the Tuple represents a row of this table, false if
     * it does not
     * @see prefuse.data.tuple.TupleSet#containsTuple(prefuse.data.Tuple)
     */
    public boolean containsTuple(Tuple t) {
        return (t.getTable()==this && isValidRow(t.getRow())); 
    }
    
    /**
     * Get the number of tuples in this table. This is the same as the
     * value returned by {@link #getRowCount()}.
     * @return the number of tuples, which is the same as the number of rows
     * @see prefuse.data.tuple.TupleSet#getTupleCount()
     */
    public int getTupleCount() {
        return getRowCount();
    }
    
    /**
     * Returns true, as this table supports the addition of new data fields.
     * @see prefuse.data.tuple.TupleSet#isAddColumnSupported()
     */
    public boolean isAddColumnSupported() {
        return true;
    }
    
    // ------------------------------------------------------------------------
    // Data Access Methods
    
    /**
     * Check if the <code>get</code> method for the given data field returns
     * values that are compatible with a given target type.
     * @param field the data field to check
     * @param type a Class instance to check for compatibility with the
     * data field values.
     * @return true if the data field is compatible with provided type,
     * false otherwise. If the value is true, objects returned by
     * the {@link #get(int, String)} can be cast to the given type.
     * @see #get(int, String)
     */
    public boolean canGet(String field, Class type) {
        Column c = getColumn(field);
        return ( c==null ? false : c.canGet(type) );
    }
    
    /**
     * Check if the <code>set</code> method for the given data field can
     * accept values of a given target type.
     * @param field the data field to check
     * @param type a Class instance to check for compatibility with the
     * data field values.
     * @return true if the data field is compatible with provided type,
     * false otherwise. If the value is true, objects of the given type
     * can be used as parameters of the {@link #set(int, String, Object)}
     * method.
     * @see #set(int, String, Object)
     */
    public boolean canSet(String field, Class type) {
        Column c = getColumn(field);
        return ( c==null ? false : c.canSet(type) );
    }
    
    /**
     * Get the data value at the given row and field as an Object.
     * @param row the table row to get
     * @param field the data field to retrieve
     * @return the data value as an Object. The concrete type of this
     * Object is dependent on the underlying data column used.
     * @see #canGet(String, Class)
     * @see #getColumnType(String)
     */
    public Object get(int row, String field) {
        int col = getColumnNumber(field);
        row = getColumnRow(row, col);
        return getColumn(col).get(row);
    }
    
    /**
     * Set the value of a given row and data field.
     * @param row the table row to set
     * @param field the data field to set
     * @param val the value for the field. If the concrete type of this
     * Object is not compatible with the underlying data model, an
     * Exception will be thrown. Use the {@link #canSet(String, Class)}
     * method to check the type-safety ahead of time.
     * @see #canSet(String, Class)
     * @see #getColumnType(String)
     */
    public void set(int row, String field, Object val) {
        int col = getColumnNumber(field);
        row = getColumnRow(row, col);
        getColumn(col).set(val, row);
        
        // we don't fire a notification here, as we catch the
        // notification from the column itself and then dispatch
    }
    
    /**
     * Get the data value at the given row and column numbers as an Object.
     * @param row the row number
     * @param col the column number
     * @return the data value as an Object. The concrete type of this
     * Object is dependent on the underlying data column used.
     * @see #canGet(String, Class)
     * @see #getColumnType(int)
     */
    public Object get(int row, int col) {
        row = getColumnRow(row, col);
        return getColumn(col).get(row);
    }

    /**
     * Set the value of at the given row and column numbers.
     * @param row the row number
     * @param col the column number
     * @param val the value for the field. If the concrete type of this
     * Object is not compatible with the underlying data model, an
     * Exception will be thrown. Use the {@link #canSet(String, Class)}
     * method to check the type-safety ahead of time.
     * @see #canSet(String, Class)
     * @see #getColumnType(String)
     */
    public void set(int row, int col, Object val) {
        row = getColumnRow(row, col);
        getColumn(col).set(val, row);
        
        // we don't fire a notification here, as we catch the
        // notification from the column itself and then dispatch
    }
    
    /**
     * Get the default value for the given data field.
     * @param field the data field
     * @return the default value, as an Object, used to populate rows
     * of the data field.
     */
    public Object getDefault(String field) {
        int col = getColumnNumber(field);
        return getColumn(col).getDefaultValue();
    }
    
    /**
     * Revert this tuple's value for the given field to the default value
     * for the field.
     * @param field the data field
     * @see #getDefault(String)
     */
    public void revertToDefault(int row, String field) {
        int col = getColumnNumber(field);
        row = getColumnRow(row, col);
        getColumn(col).revertToDefault(row);
    }

    // ------------------------------------------------------------------------
    // Convenience Data Access Methods
    
    /**
     * Check if the given data field can return primitive <code>int</code>
     * values.
     * @param field the data field to check
     * @return true if the data field can return primitive <code>int</code>
     * values, false otherwise. If true, the {@link #getInt(int, String)}
     * method can be used safely.
     */
    public final boolean canGetInt(String field) {
        Column col = getColumn(field);
        return ( col==null ? false : col.canGetInt() );
    }
    
    /**
     * Check if the <code>setInt</code> method can safely be used for the
     * given data field.
     * @param field the data field to check
     * @return true if the {@link #setInt(int, String, int)} method can safely
     * be used for the given field, false otherwise.
     */
    public final boolean canSetInt(String field) {
        Column col = getColumn(field);
        return ( col==null ? false : col.canSetInt() );
    }
    
    /**
     * Get the data value at the given row and field as an
     * <code>int</code>.
     * @param row the table row to retrieve
     * @param field the data field to retrieve
     * @see #canGetInt(String)
     */
    public final int getInt(int row, String field) {
        int col = getColumnNumber(field);
        row = getColumnRow(row, col);
        return getColumn(col).getInt(row);
    }
    
    /**
     * Set the data value of the given row and field as an
     * <code>int</code>.
     * @param row the table row to set
     * @param field the data field to set
     * @param val the value to set
     * @see #canSetInt(String)
     */
    public final void setInt(int row, String field, int val) {
        int col = getColumnNumber(field);
        row = getColumnRow(row, col);
        getColumn(col).setInt(val, row);
    }
    
    /**
     * Get the data value at the given row and field as an
     * <code>int</code>.
     * @param row the table row to retrieve
     * @param col the column number of the data field to retrieve
     * @see #canGetInt(String)
     */
    public final int getInt(int row, int col) {
        row = getColumnRow(row, col);
        return getColumn(col).getInt(row);
    }
    
    /**
     * Set the data value of the given row and field as an
     * <code>int</code>.
     * @param row the table row to set
     * @param col the column number of the data field to set
     * @param val the value to set
     * @see #canSetInt(String)
     */
    public final void setInt(int row, int col, int val) {
        row = getColumnRow(row, col);
        getColumn(col).setInt(val, row);
    }
    
    // --------------------------------------------------------------
    
    /**
     * Check if the given data field can return primitive <code>long</code>
     * values.
     * @param field the data field to check
     * @return true if the data field can return primitive <code>long</code>
     * values, false otherwise. If true, the {@link #getLong(int, String)}
     * method can be used safely.
     */
    public final boolean canGetLong(String field) {
        Column col = getColumn(field);
        return ( col==null ? false : col.canGetLong() );
    }
    
    /**
     * Check if the <code>setLong</code> method can safely be used for the
     * given data field.
     * @param field the data field to check
     * @return true if the {@link #setLong(int, String, long)} method can
     * safely be used for the given field, false otherwise.
     */
    public final boolean canSetLong(String field) {
        Column col = getColumn(field);
        return ( col==null ? false : col.canSetLong() );
    }
    
    /**
     * Get the data value at the given row and field as a
     * <code>long</code>.
     * @param row the table row to retrieve
     * @param field the data field to retrieve
     * @see #canGetLong(String)
     */
    public final long getLong(int row, String field) {
        int col = getColumnNumber(field);
        row = getColumnRow(row, col);
        return getColumn(col).getLong(row);
    }
    
    /**
     * Set the data value of the given row and field as a
     * <code>long</code>.
     * @param row the table row to set
     * @param field the data field to set
     * @param val the value to set
     * @see #canSetLong(String)
     */
    public final void setLong(int row, String field, long val) {
        int col = getColumnNumber(field);
        row = getColumnRow(row, col);
        getColumn(col).setLong(val, row);
    }

    /**
     * Get the data value at the given row and field as an
     * <code>long</code>.
     * @param row the table row to retrieve
     * @param col the column number of the data field to retrieve
     * @see #canGetLong(String)
     */
    public final long getLong(int row, int col)  {
        row = getColumnRow(row, col);
        return getColumn(col).getLong(row);
    }
    
    /**
     * Set the data value of the given row and field as an
     * <code>long</code>.
     * @param row the table row to set
     * @param col the column number of the data field to set
     * @param val the value to set
     * @see #canSetLong(String)
     */
    public final void setLong(int row, int col, long val) {
        row = getColumnRow(row, col);
        getColumn(col).setLong(val, row);
    }
    
    // --------------------------------------------------------------
    
    /**
     * Check if the given data field can return primitive <code>float</code>
     * values.
     * @param field the data field to check
     * @return true if the data field can return primitive <code>float</code>
     * values, false otherwise. If true, the {@link #getFloat(int, String)}
     * method can be used safely.
     */
    public final boolean canGetFloat(String field) {
        Column col = getColumn(field);
        return ( col==null ? false : col.canGetFloat() );
    }
    
    /**
     * Check if the <code>setFloat</code> method can safely be used for the
     * given data field.
     * @param field the data field to check
     * @return true if the {@link #setFloat(int, String, float)} method can
     * safely be used for the given field, false otherwise.
     */
    public final boolean canSetFloat(String field) {
        Column col = getColumn(field);
        return ( col==null ? false : col.canSetFloat() );
    }
    
    /**
     * Get the data value at the given row and field as a
     * <code>float</code>.
     * @param row the table row to retrieve
     * @param field the data field to retrieve
     * @see #canGetFloat(String)
     */
    public final float getFloat(int row, String field) {
        int col = getColumnNumber(field);
        row = getColumnRow(row, col);
        return getColumn(col).getFloat(row);
    }
    
    /**
     * Set the data value of the given row and field as a
     * <code>float</code>.
     * @param row the table row to set
     * @param field the data field to set
     * @param val the value to set
     * @see #canSetFloat(String)
     */
    public final void setFloat(int row, String field, float val) {
        int col = getColumnNumber(field);
        row = getColumnRow(row, col);
        getColumn(col).setFloat(val, row);   
    }
    
    /**
     * Get the data value at the given row and field as a
     * <code>float</code>.
     * @param row the table row to retrieve
     * @param col the column number of the data field to get
     * @see #canGetFloat(String)
     */
    public final float getFloat(int row, int col) {
        row = getColumnRow(row, col);
        return getColumn(col).getFloat(row);
    }
    
    /**
     * Set the data value of the given row and field as a
     * <code>float</code>.
     * @param row the table row to set
     * @param col the column number of the data field to set
     * @param val the value to set
     * @see #canSetFloat(String)
     */
    public final void setFloat(int row, int col, float val) {
        row = getColumnRow(row, col);
        getColumn(col).setFloat(val, row);   
    }
    
    // --------------------------------------------------------------
    
    /**
     * Check if the given data field can return primitive <code>double</code>
     * values.
     * @param field the data field to check
     * @return true if the data field can return primitive <code>double</code>
     * values, false otherwise. If true, the {@link #getDouble(int, String)}
     * method can be used safely.
     */
    public final boolean canGetDouble(String field) {
        Column col = getColumn(field);
        return ( col==null ? false : col.canGetDouble() );
    }
    
    /**
     * Check if the <code>setDouble</code> method can safely be used for the
     * given data field.
     * @param field the data field to check
     * @return true if the {@link #setDouble(int, String, double)} method can
     * safely be used for the given field, false otherwise.
     */
    public final boolean canSetDouble(String field) {
        Column col = getColumn(field);
        return ( col==null ? false : col.canSetDouble() );
    }
    
    /**
     * Get the data value at the given row and field as a
     * <code>double</code>.
     * @param row the table row to retrieve
     * @param field the data field to retrieve
     * @see #canGetDouble(String)
     */
    public final double getDouble(int row, String field) {
        int col = getColumnNumber(field);
        row = getColumnRow(row, col);
        return getColumn(col).getDouble(row);
    }
    
    /**
     * Set the data value of the given row and field as a
     * <code>double</code>.
     * @param row the table row to set
     * @param field the data field to set
     * @param val the value to set
     * @see #canSetDouble(String)
     */
    public final void setDouble(int row, String field, double val) {
        int col = getColumnNumber(field);
        row = getColumnRow(row, col);
        getColumn(col).setDouble(val, row);
    }
    
    /**
     * Get the data value at the given row and field as a
     * <code>double</code>.
     * @param row the table row to retrieve
     * @param col the column number of the data field to get
     * @see #canGetDouble(String)
     */
    public final double getDouble(int row, int col) {
        row = getColumnRow(row, col);
        return getColumn(col).getDouble(row);
    }
    
    /**
     * Set the data value of the given row and field as a
     * <code>double</code>.
     * @param row the table row to set
     * @param col the column number of the data field to set
     * @param val the value to set
     * @see #canSetDouble(String)
     */
    public final void setDouble(int row, int col, double val) {
        row = getColumnRow(row, col);
        getColumn(col).setDouble(val, row);
    }

    // --------------------------------------------------------------
    
    /**
     * Check if the given data field can return primitive <code>boolean</code>
     * values.
     * @param field the data field to check
     * @return true if the data field can return primitive <code>boolean</code>
     * values, false otherwise. If true, the {@link #getBoolean(int, String)}
     * method can be used safely.
     */
    public final boolean canGetBoolean(String field) {
        Column col = getColumn(field);
        return ( col==null ? false : col.canGetBoolean() );
    }
    
    /**
     * Check if the <code>setBoolean</code> method can safely be used for the
     * given data field.
     * @param field the data field to check
     * @return true if the {@link #setBoolean(int, String, boolean)} method can
     * safely be used for the given field, false otherwise.
     */
    public final boolean canSetBoolean(String field) {
        Column col = getColumn(field);
        return ( col==null ? false : col.canSetBoolean() );
    }
    
    /**
     * Get the data value at the given row and field as a
     * <code>boolean</code>.
     * @param row the table row to retrieve
     * @param field the data field to retrieve
     * @see #canGetBoolean(String)
     */
    public final boolean getBoolean(int row, String field) {
        int col = getColumnNumber(field);
        row = getColumnRow(row, col);
        return getColumn(col).getBoolean(row);
    }
    
    /**
     * Set the data value of the given row and field as a
     * <code>boolean</code>.
     * @param row the table row to set
     * @param field the data field to set
     * @param val the value to set
     * @see #canSetBoolean(String)
     */
    public final void setBoolean(int row, String field, boolean val) {
        int col = getColumnNumber(field);
        row = getColumnRow(row, col);
        getColumn(col).setBoolean(val, row);
    }
    
    /**
     * Get the data value at the given row and field as a
     * <code>boolean</code>.
     * @param row the table row to retrieve
     * @param col the column number of the data field to get
     * @see #canGetBoolean(String)
     */
    public final boolean getBoolean(int row, int col) {
        row = getColumnRow(row, col);
        return getColumn(col).getBoolean(row);
    }
    
    /**
     * Set the data value of the given row and field as a
     * <code>boolean</code>.
     * @param row the table row to set
     * @param col the column number of the data field to set
     * @param val the value to set
     * @see #canSetBoolean(String)
     */
    public final void setBoolean(int row, int col, boolean val) {
        row = getColumnRow(row, col);
        getColumn(col).setBoolean(val, row);
    }
    
    // --------------------------------------------------------------
    
    /**
     * Check if the given data field can return primitive <code>String</code>
     * values.
     * @param field the data field to check
     * @return true if the data field can return primitive <code>String</code>
     * values, false otherwise. If true, the {@link #getString(int, String)}
     * method can be used safely.
     */
    public final boolean canGetString(String field) {
        Column col = getColumn(field);
        return ( col==null ? false : col.canGetString() );
    }
    
    /**
     * Check if the <code>setString</code> method can safely be used for the
     * given data field.
     * @param field the data field to check
     * @return true if the {@link #setString(int, String, String)} method can
     * safely be used for the given field, false otherwise.
     */
    public final boolean canSetString(String field) {
        Column col = getColumn(field);
        return ( col==null ? false : col.canSetString() );
    }
    
    /**
     * Get the data value at the given row and field as a
     * <code>String</code>.
     * @param row the table row to retrieve
     * @param field the data field to retrieve
     * @see #canGetString(String)
     */
    public final String getString(int row, String field) {
        int col = getColumnNumber(field);
        row = getColumnRow(row, col);
        return getColumn(col).getString(row);
    }
    
    /**
     * Set the data value of the given row and field as a
     * <code>String</code>.
     * @param row the table row to set
     * @param field the data field to set
     * @param val the value to set
     * @see #canSetString(String)
     */
    public final void setString(int row, String field, String val) {
        int col = getColumnNumber(field);
        row = getColumnRow(row, col);
        getColumn(col).setString(val, row);
    }
    
    /**
     * Get the data value at the given row and field as a
     * <code>String</code>.
     * @param row the table row to retrieve
     * @param col the column number of the data field to retrieve
     * @see #canGetString(String)
     */
    public final String getString(int row, int col) {
        row = getColumnRow(row, col);
        return getColumn(col).getString(row);
    }
    
    /**
     * Set the data value of the given row and field as a
     * <code>String</code>.
     * @param row the table row to set
     * @param col the column number of the data field to set
     * @param val the value to set
     * @see #canSetString(String)
     */
    public final void setString(int row, int col, String val) {
        row = getColumnRow(row, col);
        getColumn(col).setString(val, row);
    }
    
    // --------------------------------------------------------------
    
    /**
     * Check if the given data field can return primitive <code>Date</code>
     * values.
     * @param field the data field to check
     * @return true if the data field can return primitive <code>Date</code>
     * values, false otherwise. If true, the {@link #getDate(int, String)}
     * method can be used safely.
     */
    public final boolean canGetDate(String field) {
        Column col = getColumn(field);
        return ( col==null ? false : col.canGetDate() );
    }
    
    /**
     * Check if the <code>setDate</code> method can safely be used for the
     * given data field.
     * @param field the data field to check
     * @return true if the {@link #setDate(int, String, Date)} method can
     * safely be used for the given field, false otherwise.
     */
    public final boolean canSetDate(String field) {
        Column col = getColumn(field);
        return ( col==null ? false : col.canSetDate() );
    }
    
    /**
     * Get the data value at the given row and field as a
     * <code>Date</code>.
     * @param row the table row to retrieve
     * @param field the data field to retrieve
     * @see #canGetDate(String)
     */
    public final Date getDate(int row, String field) {
        int col = getColumnNumber(field);
        row = getColumnRow(row, col);
        return getColumn(col).getDate(row);
    }
    
    /**
     * Set the data value of the given row and field as a
     * <code>Date</code>.
     * @param row the table row to set
     * @param field the data field to set
     * @param val the value to set
     * @see #canSetDate(String)
     */
    public final void setDate(int row, String field, Date val) {
        int col = getColumnNumber(field);
        row = getColumnRow(row, col);
        getColumn(col).setDate(val, row);
    }
    
    /**
     * Get the data value at the given row and field as a
     * <code>Date</code>.
     * @param row the table row to retrieve
     * @param col the column number of the data field to retrieve
     * @see #canGetDate(String)
     */
    public final Date getDate(int row, int col) {
        row = getColumnRow(row, col);
        return getColumn(col).getDate(row);
    }
    
    /**
     * Set the data value of the given row and field as a
     * <code>Date</code>.
     * @param row the table row to set
     * @param col the column number of the data field to set
     * @param val the value to set
     * @see #canSetDate(String)
     */
    public final void setDate(int row, int col, Date val) {
        row = getColumnRow(row, col);
        getColumn(col).setDate(val, row);
    }

    // ------------------------------------------------------------------------
    // Query Operations
    
    /**
     * Query this table for a filtered, sorted subset of this table. This
     * operation creates an entirely new table independent of this table.
     * If a filtered view of this same table is preferred, use the
     * {@link CascadedTable} class.
     * @param filter the predicate filter determining which rows to include
     * in the new table. If this value is null, all rows will be included.
     * @param sort the sorting criteria determining the order in which
     * rows are added to the new table. If this value is null, the rows
     * will not be sorted.
     * @return a new table meeting the query specification
     */
    public Table select(Predicate filter, Sort sort) {
        Table t = getSchema().instantiate();
        Iterator tuples = tuples(filter, sort);
        while ( tuples.hasNext() ) {
            t.addTuple((Tuple)tuples.next());
        }
        return t;
    }
    
    /**
     * Removes all table rows that meet the input predicate filter.
     * @param filter a predicate specifying which rows to remove from
     * the table.
     */
    public void remove(Predicate filter) {
        for ( IntIterator ii = rows(filter); ii.hasNext(); )
            removeRow(ii.nextInt());
    }
    
    // ------------------------------------------------------------------------
    // Iterators
    
    /**
     * Return a TableIterator over the rows of this table.
     * @return a TableIterator over this table
     */
    public TableIterator iterator() {
        return iterator(rows());
    }

    /**
     * Return a TableIterator over the given rows of this table.
     * @param rows an iterator over the table rows to visit
     * @return a TableIterator over this table
     */
    public TableIterator iterator(IntIterator rows) {
        return new TableIterator(this, rows);
    }
    
    /**
     * Get an iterator over the tuples in this table.
     * @return an iterator over the table tuples
     * @see prefuse.data.tuple.TupleSet#tuples()
     */
    public Iterator tuples() {
        return m_tuples.iterator(rows());
    }
    
    /**
     * Get an iterator over the tuples in this table in reverse order.
     * @return an iterator over the table tuples in reverse order
     */
    public Iterator tuplesReversed() {
        return m_tuples.iterator(rows(true));
    }
    
    /**
     * Get an iterator over the tuples for the given rows in this table.
     * @param rows an iterator over the table rows to visit
     * @return an iterator over the selected table tuples
     */
    public Iterator tuples(IntIterator rows) {
        return m_tuples.iterator(rows);
    }
    
    /**
     * Get an interator over the row numbers of this table.
     * @return an iterator over the rows of this table
     */
    public IntIterator rows() {
        return m_rows.rows();
    }
    
    /**
     * Get a filtered iterator over the row numbers of this table, returning
     * only the rows whose tuples match the given filter predicate.
     * @param filter the filter predicate to apply
     * @return a filtered iterator over the rows of this table
     */
    public IntIterator rows(Predicate filter) {
        return FilterIteratorFactory.rows(this, filter);
    }
    
    /**
     * Get an interator over the row numbers of this table.
     * @param reverse true to iterate in rever order, false for normal order
     * @return an iterator over the rows of this table
     */
    public IntIterator rows(boolean reverse) {
        return m_rows.rows(reverse);
    }
    
    /**
     * Get an iterator over the rows of this table, sorted by the given data
     * field. This method will create an index over the field if one does
     * not yet exist.
     * @param field the data field to sort by
     * @param ascend true if the iteration should proceed in an ascending
     * (lowest to highest) sort order, false for a descending order
     * @return the sorted iterator over rows of this table
     */
    public IntIterator rowsSortedBy(String field, boolean ascend) {
        Class type = getColumnType(field);
        Index index = getIndex(field, type, true);
        int t = ascend ? Index.TYPE_ASCENDING : Index.TYPE_DESCENDING;
        return index.allRows(t);
    }
    
    /**
     * Return an iterator over a range of rwos in this table, determined
     * by a bounded range for a given data field. A new index over the
     * data field will be created if it doesn't already exist.
     * @param field the data field for determining the bounded range
     * @param lo the minimum range value
     * @param hi the maximum range value
     * @param indexType indicate the sort order and inclusivity/exclusivity
     * of the range bounds, using the constants of the
     * {@link prefuse.data.util.Index} class.
     * @return an iterator over a range of table rows, determined by a 
     * sorted bounded range of a data field
     */
    public IntIterator rangeSortedBy(String field, int lo, int hi, int indexType) {
        Index index = getIndex(field, int.class, true);
        return index.rows(lo, hi, indexType);
    }
    
    /**
     * Return an iterator over a range of rwos in this table, determined
     * by a bounded range for a given data field. A new index over the
     * data field will be created if it doesn't already exist. 
     * @param field the data field for determining the bounded range
     * @param lo the minimum range value
     * @param hi the maximum range value
     * @param indexType indicate the sort order and inclusivity/exclusivity
     * of the range bounds, using the constants of the
     * {@link prefuse.data.util.Index} class.
     * @return an iterator over a range of table rows, determined by a 
     * sorted bounded range of a data field
     */
    public IntIterator rangeSortedBy(String field, long lo, long hi, int indexType) {
        Index index = getIndex(field, long.class, true);
        return index.rows(lo, hi, indexType);
    }
    
    /**
     * Return an iterator over a range of rwos in this table, determined
     * by a bounded range for a given data field. A new index over the
     * data field will be created if it doesn't already exist.
     * @param field the data field for determining the bounded range
     * @param lo the minimum range value
     * @param hi the maximum range value
     * @param indexType indicate the sort order and inclusivity/exclusivity
     * of the range bounds, using the constants of the
     * {@link prefuse.data.util.Index} class.
     * @return an iterator over a range of table rows, determined by a 
     * sorted bounded range of a data field
     */
    public IntIterator rangeSortedBy(String field, float lo, float hi, int indexType) {
        Index index = getIndex(field, float.class, true);
        return index.rows(lo, hi, indexType);
    }
    
    /**
     * Return an iterator over a range of rwos in this table, determined
     * by a bounded range for a given data field. A new index over the
     * data field will be created if it doesn't already exist.
     * @param field the data field for determining the bounded range
     * @param lo the minimum range value
     * @param hi the maximum range value
     * @param indexType indicate the sort order and inclusivity/exclusivity
     * of the range bounds, using the constants of the
     * {@link prefuse.data.util.Index} class.
     * @return an iterator over a range of table rows, determined by a 
     * sorted bounded range of a data field
     */
    public IntIterator rangeSortedBy(String field, double lo, double hi, int indexType) {
        Index index = getIndex(field, double.class, true);
        return index.rows(lo, hi, indexType);
    }
    
    /**
     * Return an iterator over a range of rwos in this table, determined
     * by a bounded range for a given data field. A new index over the
     * data field will be created if it doesn't already exist.
     * @param field the data field for determining the bounded range
     * @param lo the minimum range value
     * @param hi the maximum range value
     * @param indexType indicate the sort order and inclusivity/exclusivity
     * of the range bounds, using the constants of the
     * {@link prefuse.data.util.Index} class.
     * @return an iterator over a range of table rows, determined by a 
     * sorted bounded range of a data field
     */
    public IntIterator rangeSortedBy(String field, Object lo, Object hi, int indexType) {
        Class type = TypeLib.getSharedType(lo, hi);
        // TODO: check this for correctness
        if ( type == null )
            throw new IllegalArgumentException("Incompatible arguments");
        Index index = getIndex(field, type, true);
        return index.rows(lo, hi, indexType);
    }
    
    // ------------------------------------------------------------------------
    // Listener Methods
    
    // -- ColumnListeners -----------------------------------------------------
    
    /**
     * @see prefuse.data.event.ColumnListener#columnChanged(prefuse.data.column.Column, int, boolean)
     */
    public void columnChanged(Column src, int idx, boolean prev) {
        handleColumnChanged(src, idx, idx);
    }

    /**
     * @see prefuse.data.event.ColumnListener#columnChanged(prefuse.data.column.Column, int, double)
     */
    public void columnChanged(Column src, int idx, double prev) {
        handleColumnChanged(src, idx, idx);
    }

    /**
     * @see prefuse.data.event.ColumnListener#columnChanged(prefuse.data.column.Column, int, float)
     */
    public void columnChanged(Column src, int idx, float prev) {
        handleColumnChanged(src, idx, idx);
    }

    /**
     * @see prefuse.data.event.ColumnListener#columnChanged(prefuse.data.column.Column, int, int)
     */
    public void columnChanged(Column src, int idx, int prev) {
        handleColumnChanged(src, idx, idx);
    }

    /**
     * @see prefuse.data.event.ColumnListener#columnChanged(prefuse.data.column.Column, int, long)
     */
    public void columnChanged(Column src, int idx, long prev) {
        handleColumnChanged(src, idx, idx);
    }

    /**
     * @see prefuse.data.event.ColumnListener#columnChanged(prefuse.data.column.Column, int, java.lang.Object)
     */
    public void columnChanged(Column src, int idx, Object prev) {
        handleColumnChanged(src, idx, idx);
    }

    /**
     * @see prefuse.data.event.ColumnListener#columnChanged(prefuse.data.column.Column, int, int, int)
     */
    public void columnChanged(Column src, int type, int start, int end) {
        handleColumnChanged(src, start, end);
    }
    
    /**
     * Handle a column change event.
     * @param c the modified column
     * @param start the starting row of the modified range
     * @param end the ending row (inclusive) of the modified range
     */
    protected void handleColumnChanged(Column c, int start, int end) {
        for ( ; !isValidRow(start) && start <= end; ++start );
        if ( start > end ) return; // bail if no valid rows
        
        // determine the index of the updated column
        int idx;
        if ( m_lastCol != -1 && c == getColumn(m_lastCol) ) {
            // constant time
            idx = m_lastCol;
        } else {
            // linear time
            idx = getColumnNumber(c);
        }
        
        // if we have a valid index, fire a notification
        if ( idx >= 0 ) {
            fireTableEvent(start, end, idx, TableModelEvent.UPDATE);
        }
    }
    
    // -- TableListeners ------------------------------------------------------
    
    /**
     * Add a table listener to this table.
     * @param listnr the listener to add
     */
    public void addTableListener(TableListener listnr) {
        if ( !m_listeners.contains(listnr) )
            m_listeners.add(listnr);
    }

    /**
     * Remove a table listener from this table.
     * @param listnr the listener to remove
     */
    public void removeTableListener(TableListener listnr) {
        m_listeners.remove(listnr);
    }
    
    /**
     * Removes all table listeners from this table.
     */
    public void removeAllTableListeners() {
    	m_listeners.clear();
    }
    
    /**
     * Fire a table event to notify listeners.
     * @param row0 the starting row of the modified range
     * @param row1 the ending row (inclusive) of the modified range
     * @param col the number of the column modified, or
     * {@link prefuse.data.event.EventConstants#ALL_COLUMNS} for operations
     * effecting all columns.
     * @param type the table modification type, one of
     * {@link prefuse.data.event.EventConstants#INSERT},
     * {@link prefuse.data.event.EventConstants#DELETE}, or
     * {@link prefuse.data.event.EventConstants#UPDATE}.
     */
    protected void fireTableEvent(int row0, int row1, int col, int type) {
        // increment the modification count
        ++m_modCount;
        
        if ( type != EventConstants.UPDATE && 
             col == EventConstants.ALL_COLUMNS )
        {
            // fire event to all tuple set listeners
            fireTupleEvent(this, row0, row1, type);
        }
        
        if ( !m_listeners.isEmpty() ) {
            // fire event to all table listeners
            Object[] lstnrs = m_listeners.getArray();
            for ( int i=0; i<lstnrs.length; ++i ) {
                ((TableListener)lstnrs[i]).tableChanged(
                        this, row0, row1, col, type);
            }
        }
    }
    
    // ------------------------------------------------------------------------
    // String Methods
    
    /**
     * @see java.lang.Object#toString()
     */
    public String toString() {
        StringBuffer sbuf = new StringBuffer();
        sbuf.append("Table[");
        sbuf.append("rows=").append(getRowCount());
        sbuf.append(", cols=").append(getColumnCount());
        sbuf.append(", maxrow=").append(m_rows.getMaximumRow());
        sbuf.append("]");
        return sbuf.toString();
    }
    
    // ------------------------------------------------------------------------
    // ColumnEntry helper
    
    /**
     * Helper class that encapsulates a map entry for a column, including the
     * column itself and its metadata and index.
     * 
     * @author <a href="http://jheer.org">jeffrey heer</a>
     */
    protected static class ColumnEntry {

        /** The column number. */
        public int            colnum;
        /** The Column instance. */
        public Column         column;
        /** The column metadata instance. */
        public ColumnMetadata metadata;
        /** The column Index instance. */
        public Index          index;
        
        /**
         * Create a new ColumnEntry.
         * @param col the column number
         * @param column the Column instance
         * @param metadata the ColumnMetadata instance
         */
        public ColumnEntry(int col, Column column, ColumnMetadata metadata) {
            this.colnum = col;
            this.column = column;
            this.metadata = metadata;
            this.index = null;
        }
        
        /**
         * Dispose of this column entry, disposing of any allocated
         * metadata or index instances.
         */
        public void dispose() {
            if ( metadata != null )
                metadata.dispose();
            if ( index != null )
                index.dispose();
        }

    } // end of inner class ColumnEntry
    
} // end of class Table
