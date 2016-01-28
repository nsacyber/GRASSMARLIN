package prefuse.data.column;

import java.util.Arrays;
import java.util.Date;

import prefuse.data.DataReadOnlyException;
import prefuse.data.DataTypeException;
import prefuse.util.TimeLib;

/**
 * Column implementation for storing Date values.
 * 
 * @author <a href="http://jheer.org">jeffrey heer</a>
 */
public class DateColumn extends AbstractColumn {

    private long[] m_values;
    private int    m_size;
    
    /**
     * Create a new empty DateColumn. 
     */
    public DateColumn() {
        this(Date.class, 0, 10, 0L);
    }

    /**
     * Create a new DateColumn. 
     * @param nrows the initial size of the column
     */
    public DateColumn(int nrows) {
        this(Date.class, nrows, nrows, 0L);
    }
    
    /**
     * Create a new DateColumn.
     * @param type the exact data type (must be an instance or 
     * subclass of java.util.Date) 
     * @param nrows the initial size of the column
     */
    public DateColumn(Class type, int nrows) {
        this(type, nrows, nrows, 0L);
    }
    
    /**
     * Create a new DateColumn.
     * @param type the exact data type (must be an instance or 
     * subclass of java.util.Date)
     * @param nrows the initial size of the column
     * @param capacity the initial capacity of the column
     * @param defaultValue the default value for the column
     */
    public DateColumn(Class type, int nrows, int capacity, long defaultValue) {
        super(type, TimeLib.getDate(type, defaultValue));
        if ( !Date.class.isAssignableFrom(type) ) {
            throw new IllegalArgumentException("Column type must be an "
                + "instance or subclass of java.util.Date.");
        }
        if ( capacity < nrows ) {
            throw new IllegalArgumentException(
                "Capacity value can not be less than the row count.");
        }
        m_values = new long[capacity];
        Arrays.fill(m_values, defaultValue);
        m_size = nrows;
    }
    
    // ------------------------------------------------------------------------
    // Column Metadata
    
    /**
     * @see prefuse.data.column.Column#getRowCount()
     */
    public int getRowCount() {
        return m_size;
    }
    
    /**
     * @see prefuse.data.column.Column#setMaximumRow(int)
     */
    public void setMaximumRow(int nrows) {
        if ( nrows > m_values.length ) {
            int capacity = Math.max((3*m_values.length)/2 + 1, nrows);
            long[] values = new long[capacity];
            System.arraycopy(m_values, 0, values, 0, m_size);
            Arrays.fill(values, m_size, capacity,
                    ((Date)m_defaultValue).getTime());
            m_values = values;
        }
        m_size = nrows;
    }
    
    /**
     * Indicates if the set method can be called without
     * an exception being thrown for the given type.
     * @param type the Class of the data type to check
     * @return true if the type is supported by this column, false otherwise
     */
    public boolean canSet(Class type) {
        if ( type == null ) return false;
        
        if ( Number.class.isAssignableFrom(type) ||
             String.class.isAssignableFrom(type) )
        {
            return true;
        } else {
            return m_columnType.isAssignableFrom(type);
        }
    }
    
    // ------------------------------------------------------------------------
    // Data Access Methods
    
    /**
     * @see prefuse.data.column.Column#get(int)
     */
    public Object get(int row) {
        return TimeLib.getDate(m_columnType, getLong(row));
    }

    /**
     * @see prefuse.data.column.Column#set(java.lang.Object, int)
     */
    public void set(Object val, int row) throws DataTypeException {
        if ( m_readOnly ) {
            throw new DataReadOnlyException();
        } else if ( val != null ) {
            if ( val instanceof Date ) {
                setLong(((Date)val).getTime(), row);
            } else if ( val instanceof Number ) {
                setLong(((Number)val).longValue(), row);
            } else if ( val instanceof String ) {
                setString((String)val, row);
            } else {
                throw new DataTypeException(val.getClass());
            }
        } else {
            throw new DataTypeException("Column does not accept null values");
        }
    }

    // ------------------------------------------------------------------------
    // Data Type Convenience Methods
    
    /**
     * @see prefuse.data.column.AbstractColumn#getLong(int)
     */
    public long getLong(int row) throws DataTypeException {
        if ( row < 0 || row > m_size ) {
            throw new IllegalArgumentException("Row index out of bounds: "+row);
        }
        return m_values[row];
    }

    /**
     * @see prefuse.data.column.AbstractColumn#setLong(long, int)
     */
    public void setLong(long val, int row) throws DataTypeException {
        if ( m_readOnly ) {
            throw new DataReadOnlyException();
        } else if ( row < 0 || row >= m_size ) {
            throw new IllegalArgumentException("Row index out of bounds: "+row);
        }
        // get the previous value
        long prev = m_values[row];
        
        // exit early if no change
        if ( prev == val ) return;
        
        // set the new value
        m_values[row] = val;
        
        // fire a change event
        fireColumnEvent(row, prev);
    }

    // ------------------------------------------------------------------------
    
    /**
     * @see prefuse.data.column.Column#getDouble(int)
     */
    public double getDouble(int row) throws DataTypeException {
        return getLong(row);
    }
    
} // end of class DateColumn
