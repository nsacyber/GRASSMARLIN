package prefuse.data.column;

import java.util.BitSet;
import java.util.Iterator;
import java.util.Set;

import prefuse.data.DataTypeException;
import prefuse.data.Table;
import prefuse.data.event.ColumnListener;
import prefuse.data.event.EventConstants;
import prefuse.data.event.ExpressionListener;
import prefuse.data.expression.Expression;
import prefuse.data.expression.ExpressionAnalyzer;

/**
 * <p>Column instance that stores values provided by an Expression
 * instance. These expressions can reference other column values within the
 * same table. Values are evaluated when first requested and then cached to
 * increase performance. This column maintains listeners for all referenced
 * columns discovered in the expression and for the expression itself,
 * invalidating all cached entries when an update to either occurs.</p>
 * 
 * <p>
 * WARNING: Infinite recursion, eventually resulting in a StackOverflowError,
 * could occur if an expression refers to its own column, or if two
 * ExpressionColumns have expressions referring to each other. The 
 * responsibility for avoiding such situations is left with client programmers.
 * Note that it is fine for one ExpressionColumn to reference another;
 * however, the graph induced by such references must not contain any cycles.
 * </p>
 * 
 * @author <a href="http://jheer.org">jeffrey heer</a>
 * @see prefuse.data.expression
 */
public class ExpressionColumn extends AbstractColumn {
    
    private Expression m_expr;
    private Table m_table;
    private Set m_columns;
    
    private BitSet m_valid;
    private Column m_cache;
    private Listener m_lstnr;
    
    /**
     * Create a new ExpressionColumn.
     * @param table the table this column is a member of
     * @param expr the expression used to provide the column values
     */
    public ExpressionColumn(Table table, Expression expr) {
        super(expr.getType(table.getSchema()));
        m_table = table;
        m_expr = expr;
        m_lstnr = new Listener();
        
        init();
        
        int nrows = m_table.getRowCount();
        m_cache = ColumnFactory.getColumn(getColumnType(), nrows);
        m_valid = new BitSet(nrows);
        m_expr.addExpressionListener(m_lstnr);
    }
    
    protected void init() {
        // first remove listeners on any current columns
        if ( m_columns != null && m_columns.size() > 0 ) {
            Iterator iter = m_columns.iterator();
            while ( iter.hasNext() ) {
                String field = (String)iter.next();
                Column col = m_table.getColumn(field);
                col.removeColumnListener(m_lstnr);
            }
        }
        // now get the current set of columns
        m_columns = ExpressionAnalyzer.getReferencedColumns(m_expr);
        
        // sanity check table and expression
        Iterator iter = m_columns.iterator();
        while ( iter.hasNext() ) {
            String name = (String)iter.next();
            if ( m_table.getColumn(name) == null )
                throw new IllegalArgumentException("Table must contain all "
                        + "columns referenced by the expression."
                        + " Bad column name: "+name);
            
        }
        
        // passed check, so now listen to columns
        iter = m_columns.iterator();
        while ( iter.hasNext() ) {
            String field = (String)iter.next();
            Column col = m_table.getColumn(field);
            col.addColumnListener(m_lstnr);
        }
    }
    
    // ------------------------------------------------------------------------
    // Column Metadata
    
    /**
     * @see prefuse.data.column.Column#getRowCount()
     */
    public int getRowCount() {
        return m_cache.getRowCount();
    }

    /**
     * @see prefuse.data.column.Column#setMaximumRow(int)
     */
    public void setMaximumRow(int nrows) {
        m_cache.setMaximumRow(nrows);
    }

    // ------------------------------------------------------------------------
    // Cache Management
    
    /**
     * Check if this ExpressionColumn has a valid cached value at the given
     * row.
     * @param row the row to check for a valid cache entry
     * @return true if the cache row is valid, false otherwise
     */
    public boolean isCacheValid(int row) {
        return m_valid.get(row);
    }
    
    /**
     * Invalidate a range of the cache.
     * @param start the start of the range to invalidate
     * @param end the end of the range to invalidate, inclusive
     */
    public void invalidateCache(int start, int end ) {
        m_valid.clear(start, end+1);
    }
    
    // ------------------------------------------------------------------------
    // Data Access Methods    

    /**
     * Has no effect, as all values in this column are derived.
     * @param row the row to revert
     */
    public void revertToDefault(int row) {
        // do nothing, as we don't have default values.
    }
    
    /**
     * @see prefuse.data.column.AbstractColumn#canSet(java.lang.Class)
     */
    public boolean canSet(Class type) {
        return false;
    }
    
    /**
     * @see prefuse.data.column.Column#get(int)
     */
    public Object get(int row) {
        rangeCheck(row);
        if ( isCacheValid(row) ) {
            return m_cache.get(row);
        }
        Object val = m_expr.get(m_table.getTuple(row));
        Class type = val==null ? Object.class : val.getClass();
        if ( m_cache.canSet(type) ) {
            m_cache.set(val, row);
            m_valid.set(row);
        }
        return val;
    }

    /**
     * @see prefuse.data.column.Column#set(java.lang.Object, int)
     */
    public void set(Object val, int row) throws DataTypeException {
        throw new UnsupportedOperationException();
    }
    
    private void rangeCheck(int row) {
        if ( row < 0 || row >= getRowCount() )
            throw new IndexOutOfBoundsException();
    }
    
    // ------------------------------------------------------------------------
    
    /**
     * @see prefuse.data.column.Column#getBoolean(int)
     */
    public boolean getBoolean(int row) throws DataTypeException {
        if ( !canGetBoolean() )
            throw new DataTypeException(boolean.class);
        rangeCheck(row);
        
        if ( isCacheValid(row) ) {
            return m_cache.getBoolean(row);
        } else {
            boolean value = m_expr.getBoolean(m_table.getTuple(row));
            m_cache.setBoolean(value, row);
            m_valid.set(row);
            return value;
        }
    }

    private void computeNumber(int row) {
        if ( m_columnType == int.class || m_columnType == byte.class ) {
            m_cache.setInt(m_expr.getInt(m_table.getTuple(row)), row);
        } else if ( m_columnType == long.class ) {
            m_cache.setLong(m_expr.getLong(m_table.getTuple(row)), row);
        } else if ( m_columnType == float.class ) {
            m_cache.setFloat(m_expr.getFloat(m_table.getTuple(row)), row);
        } else {
            m_cache.setDouble(m_expr.getDouble(m_table.getTuple(row)), row);
        }
        m_valid.set(row);
    }
    
    /**
     * @see prefuse.data.column.Column#getInt(int)
     */
    public int getInt(int row) throws DataTypeException {
        if ( !canGetInt() )
            throw new DataTypeException(int.class);
        rangeCheck(row);
        
        if ( !isCacheValid(row) )
            computeNumber(row);
        return m_cache.getInt(row);
    }

    /**
     * @see prefuse.data.column.Column#getDouble(int)
     */
    public double getDouble(int row) throws DataTypeException {
        if ( !canGetDouble() )
            throw new DataTypeException(double.class);
        rangeCheck(row);
        
        if ( !isCacheValid(row) )
            computeNumber(row);
        return m_cache.getDouble(row);
    }

    /**
     * @see prefuse.data.column.Column#getFloat(int)
     */
    public float getFloat(int row) throws DataTypeException {
        if ( !canGetFloat() )
            throw new DataTypeException(float.class);
        rangeCheck(row);
        
        if ( !isCacheValid(row) )
            computeNumber(row);
        return m_cache.getFloat(row);
    }

    /**
     * @see prefuse.data.column.Column#getLong(int)
     */
    public long getLong(int row) throws DataTypeException {
        if ( !canGetLong() )
            throw new DataTypeException(long.class);
        rangeCheck(row);
        
        if ( !isCacheValid(row) )
            computeNumber(row);
        return m_cache.getLong(row);
    }
    
    // ------------------------------------------------------------------------
    // Listener Methods

    private class Listener implements ColumnListener, ExpressionListener {
    
        public void columnChanged(int start, int end) {
            // for a single index change with a valid cache value,
            // propagate a change event with the previous value
            if ( start == end && isCacheValid(start) ) {
                if ( !m_table.isValidRow(start) ) return;
                
                // invalidate the cache index
                invalidateCache(start, end);
                // fire change event including previous value
                Class type = getColumnType();
                if ( int.class == type ) {
                    fireColumnEvent(start, m_cache.getInt(start));
                } else if ( long.class == type ) {
                    fireColumnEvent(start, m_cache.getLong(start));
                } else if ( float.class == type ) {
                    fireColumnEvent(start, m_cache.getFloat(start));
                } else if ( double.class == type ) {
                    fireColumnEvent(start, m_cache.getDouble(start));
                } else if ( boolean.class == type ) {
                    fireColumnEvent(start, m_cache.getBoolean(start));
                } else {
                    fireColumnEvent(start, m_cache.get(start));
                }
                
            // otherwise send a generic update
            } else {
                // invalidate cache indices
                invalidateCache(start, end);
                // fire change event
                fireColumnEvent(EventConstants.UPDATE, start, end);
            }
        }
        
        public void columnChanged(Column src, int idx, boolean prev) {
            columnChanged(idx, idx);
        }
    
        public void columnChanged(Column src, int idx, double prev) {
            columnChanged(idx, idx);
        }
    
        public void columnChanged(Column src, int idx, float prev) {
            columnChanged(idx, idx);
        }
    
        public void columnChanged(Column src, int type, int start, int end) {
            columnChanged(start, end);
        }
    
        public void columnChanged(Column src, int idx, int prev) {
            columnChanged(idx, idx);
        }
    
        public void columnChanged(Column src, int idx, long prev) {
            columnChanged(idx, idx);
        }
    
        public void columnChanged(Column src, int idx, Object prev) {
            columnChanged(idx, idx);
        }
    
        public void expressionChanged(Expression expr) {
            // mark everything as changed
            columnChanged(0, m_cache.getRowCount()-1);
            // re-initialize our setup
            init();
        }
    }
    
} // end of class DerivedColumn
