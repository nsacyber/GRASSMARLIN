package prefuse.data.column;

import java.util.Date;

import prefuse.data.DataTypeException;
import prefuse.data.event.ColumnListener;
import prefuse.data.parser.DataParseException;
import prefuse.data.parser.DataParser;
import prefuse.data.parser.ObjectParser;
import prefuse.data.parser.ParserFactory;
import prefuse.util.TypeLib;
import prefuse.util.collections.CopyOnWriteArrayList;

/**
 * Abstract base class for Column implementations. Provides listener support
 * and default implementations of column methods.
 * 
 * @author <a href="http://jheer.org">jeffrey heer</a>
 */
public abstract class AbstractColumn implements Column {

    protected final Class  m_columnType;
    protected DataParser   m_parser;
    protected Object       m_defaultValue;
    protected boolean      m_readOnly;
    
    protected CopyOnWriteArrayList m_listeners;
    
    /**
     * Create a new AbstractColumn of type Object.
     */
    public AbstractColumn() {
        this(Object.class, null);
    }

    /**
     * Create a new AbstractColumn of a given type.
     * @param columnType the data type stored by this column
     */
    public AbstractColumn(Class columnType) {
        this(columnType, null);
    }

    /**
     * Create a new AbstractColumn of a given type.
     * @param columnType the data type stored by this column
     * @param defaultValue the default data value to use
     */
    public AbstractColumn(Class columnType, Object defaultValue) {
        m_columnType = columnType;
        
        DataParser p = ParserFactory.getDefaultFactory().getParser(columnType);
        m_parser = ( p==null ? new ObjectParser() : p );
        
        setDefaultValue(defaultValue);
        m_readOnly = false;
        m_listeners = new CopyOnWriteArrayList();
    }
    
    // ------------------------------------------------------------------------
    // Column Metadata
    
    /**
     * Indicates if the values in this column are read-only.
     * @return true if the values can not be edited, false otherwise
     */
    public boolean isReadOnly() {
        return m_readOnly;
    }
    
    /**
     * Sets if the values in this column are read-only
     * @param readOnly true to ensure the values can not be edited, 
     *  false otherwise
     */
    public void setReadOnly(boolean readOnly) {
        m_readOnly = readOnly;
    } //
    
    /**
     * Indicates if the value at the given row can be edited.
     * @param row the row to check
     * @return true is the value can be modified, false otherwise
     */
    public boolean isCellEditable(int row) {
        return !m_readOnly;
    }
    
    /**
     * Returns the most specific superclass for the values in the column
     * @return the Class of the column's data values
     */
    public Class getColumnType() {
        return m_columnType;
    }
    
    /**
     * @see prefuse.data.column.Column#getParser()
     */
    public DataParser getParser() {
        return m_parser;
    }
    
    /**
     * @see prefuse.data.column.Column#setParser(prefuse.data.parser.DataParser)
     */
    public void setParser(DataParser parser) {
        if ( !m_columnType.isAssignableFrom(parser.getType()) ) {
            throw new IllegalArgumentException(
               "Parser type ("+parser.getType().getName()+") incompatible with"
               +" this column's data type ("+m_columnType.getName()+")");
        }
        m_parser = parser;
    }
    
    // ------------------------------------------------------------------------
    // Listener Methods
    
    /**
     * Adds a listener to be notified when this column changes
     * @param listener the ColumnListener to add
     */
    public void addColumnListener(ColumnListener listener) {
        m_listeners.add(listener);
    }

    /**
     * Removes a listener, causing it to no longer be notified of changes
     * @param listener the ColumnListener to remove
     */
    public void removeColumnListener(ColumnListener listener) {
        m_listeners.remove(listener);
    }
    
    /**
     * Notifies all registered listeners of a column UPDATE event
     */
    protected final void fireColumnEvent(int type, int start, int end) {
        Object[] lstnrs = m_listeners.getArray();
        for ( int i=0; i<lstnrs.length; ++i )
            ((ColumnListener)lstnrs[i]).columnChanged(this, type, start, end);
    }
    
    /**
     * Notifies all registered listeners of a column UPDATE event
     * @param idx the row index of the column that was updated
     * @param prev the previous value at the given index
     */
    protected final void fireColumnEvent(int idx, int prev) {
        Object[] lstnrs = m_listeners.getArray();
        for ( int i=0; i<lstnrs.length; ++i )
            ((ColumnListener)lstnrs[i]).columnChanged(this, idx, prev);
    }
    
    /**
     * Notifies all registered listeners of a column UPDATE event
     * @param idx the row index of the column that was updated
     * @param prev the previous value at the given index
     */
    protected final void fireColumnEvent(int idx, long prev) {
        Object[] lstnrs = m_listeners.getArray();
        for ( int i=0; i<lstnrs.length; ++i )
            ((ColumnListener)lstnrs[i]).columnChanged(this, idx, prev);
    }
    
    /**
     * Notifies all registered listeners of a column UPDATE event
     * @param idx the row index of the column that was updated
     * @param prev the previous value at the given index
     */
    protected final void fireColumnEvent(int idx, float prev) {
        Object[] lstnrs = m_listeners.getArray();
        for ( int i=0; i<lstnrs.length; ++i )
            ((ColumnListener)lstnrs[i]).columnChanged(this, idx, prev);
    }
    
    /**
     * Notifies all registered listeners of a column UPDATE event
     * @param idx the row index of the column that was updated
     * @param prev the previous value at the given index
     */
    protected final void fireColumnEvent(int idx, double prev) {
        Object[] lstnrs = m_listeners.getArray();
        for ( int i=0; i<lstnrs.length; ++i )
            ((ColumnListener)lstnrs[i]).columnChanged(this, idx, prev);
    }
    
    /**
     * Notifies all registered listeners of a column UPDATE event
     * @param idx the row index of the column that was updated
     * @param prev the previous value at the given index
     */
    protected final void fireColumnEvent(int idx, boolean prev) {
        Object[] lstnrs = m_listeners.getArray();
        for ( int i=0; i<lstnrs.length; ++i )
            ((ColumnListener)lstnrs[i]).columnChanged(this, idx, prev);
    }
    
    /**
     * Notifies all registered listeners of a column UPDATE event
     * @param idx the row index of the column that was updated
     * @param prev the previous value at the given index
     */
    protected final void fireColumnEvent(int idx, Object prev) {
        Object[] lstnrs = m_listeners.getArray();
        for ( int i=0; i<lstnrs.length; ++i )
            ((ColumnListener)lstnrs[i]).columnChanged(this, idx, prev);
    }
    
    // ------------------------------------------------------------------------
    // Data Access Methods
    
    /**
     * Returns the default value for rows that have not been set explicitly. 
     */
    public Object getDefaultValue() {
        return m_defaultValue;
    }
 
    /**
     * Sets the default value for this column. Rows previously added
     * under a different default value will not be changed as a result
     * of this method; the new default will apply to newly added rows
     * only.
     * @param dflt
     */
    public void setDefaultValue(Object dflt) {
        boolean prim = m_columnType.isPrimitive();
        if ( dflt != null &&
            ((!prim && !m_columnType.isInstance(dflt)) ||
             (prim && !TypeLib.isWrapperInstance(m_columnType, dflt))) )
        {
            throw new IllegalArgumentException(
                "Default value is not of type " + m_columnType.getName());
        }
        m_defaultValue = dflt;
    }
    
    /**
     * Reverts the specified row back to the column's default value.
     * @param row
     */
    public void revertToDefault(int row) {
        set(m_defaultValue, row);
    }
    
    /**
     * Indicates if the get method can be called without
     * an exception being thrown for the given type.
     * @param type the Class of the data type to check
     * @return true if the type is supported by this column, false otherwise
     */
    public boolean canGet(Class type) {
        if ( type == null ) return false;
        
        if ( m_columnType.isPrimitive() ) {
            boolean primTypes = type.isAssignableFrom(m_columnType) || 
                    (TypeLib.isNumericType(m_columnType) 
                     && TypeLib.isNumericType(type));
             
            return primTypes
                || type.isAssignableFrom(TypeLib.getWrapperType(m_columnType))
                || type.isAssignableFrom(String.class);
        } else {
            return type.isAssignableFrom(m_columnType);
        }
    }
    
    /**
     * Indicates if the set method can be called without
     * an exception being thrown for the given type.
     * @param type the Class of the data type to check
     * @return true if the type is supported by this column, false otherwise
     */
    public boolean canSet(Class type) {
        if ( type == null ) return false;
        
        if ( m_columnType.isPrimitive() ) {
            return m_columnType.isAssignableFrom(type)
                || TypeLib.getWrapperType(m_columnType).isAssignableFrom(type)
                || String.class.isAssignableFrom(type);
        } else {
            return m_columnType.isAssignableFrom(type);
        }
    }
    
    // ------------------------------------------------------------------------
    // Data Type Convenience Methods
    
    // because java's type system can be tedious at times...
    
    // -- int -----------------------------------------------------------------
    
    /**
     * Indicates if convenience get method can be called without
     * an exception being thrown for the int type.
     * @return true if getInt is supported, false otherwise
     */
    public boolean canGetInt() {
        return canGet(int.class);
    }
    
    /**
     * Indicates if convenience set method can be called without
     * an exception being thrown for the int type.
     * @return true if setInt is supported, false otherwise
     */
    public boolean canSetInt() {
        return canSet(int.class);
    }
    
    /**
     * Get the data value at the specified row as an integer
     * @param row the row from which to retrieve the value
     * @return the data value as an integer
     * @throws DataTypeException if this column does not 
     *  support the integer type
     */
    public int getInt(int row) throws DataTypeException {
        if ( canGetInt() ) {
            return ((Integer)get(row)).intValue();
        } else {
            throw new DataTypeException(int.class);
        }
    }
    
    /**
     * Set the data value at the specified row as an integer
     * @param val the value to set
     * @param row the row at which to set the value
     * @throws DataTypeException if this column does not 
     *  support the integer type
     */
    public void setInt(int val, int row) throws DataTypeException {
        if ( canSetInt() ) {
            set(new Integer(val), row);
        } else {
            throw new DataTypeException(int.class);
        }
    }

    // -- long ----------------------------------------------------------------
    
    /**
     * Indicates if convenience get method can be called without
     * an exception being thrown for the long type.
     * @return true if getLong is supported, false otherwise
     */
    public boolean canGetLong() {
        return canGet(long.class);
    }
    
    /**
     * Indicates if convenience set method can be called without
     * an exception being thrown for the long type.
     * @return true if setLong is supported, false otherwise
     */
    public boolean canSetLong() {
        return canSet(long.class);
    }
    
    /**
     * Get the data value at the specified row as a long
     * @param row the row from which to retrieve the value
     * @return the data value as a long
     * @throws DataTypeException if this column does not 
     *  support the long type
     */
    public long getLong(int row) throws DataTypeException {
        if ( canGetLong() ) {
            return ((Long)get(row)).longValue();
        } else {
            throw new DataTypeException(long.class);
        }
    }
    
    /**
     * Set the data value at the specified row as a long
     * @param val the value to set
     * @param row the row at which to set the value
     * @throws DataTypeException if this column does not 
     *  support the long type
     */
    public void setLong(long val, int row) throws DataTypeException {
        if ( canSetLong() ) {
            set(new Long(val), row);
        } else {
            throw new DataTypeException(long.class);
        }
    }
    
    // -- float ---------------------------------------------------------------    
    
    /**
     * Indicates if convenience get method can be called without
     * an exception being thrown for the float type.
     * @return true if getFloat is supported, false otherwise
     */
    public boolean canGetFloat() {
        return canGet(float.class);
    }
    
    /**
     * Indicates if convenience set method can be called without
     * an exception being thrown for the float type.
     * @return true if setFloat is supported, false otherwise
     */
    public boolean canSetFloat() {
        return canSet(float.class);
    }
    
    /**
     * Get the data value at the specified row as a float
     * @param row the row from which to retrieve the value
     * @return the data value as a float
     * @throws DataTypeException if this column does not 
     *  support the float type
     */
    public float getFloat(int row) throws DataTypeException {
        if ( canGetFloat() ) {
            return ((Float)get(row)).floatValue();
        } else {
            throw new DataTypeException(float.class);
        }
    }
    
    /**
     * Set the data value at the specified row as a float
     * @param val the value to set
     * @param row the row at which to set the value
     * @throws DataTypeException if this column does not 
     *  support the float type
     */
    public void setFloat(float val, int row) throws DataTypeException {
        if ( canSetFloat() ) {
            set(new Float(val), row);
        } else {
            throw new DataTypeException(float.class);
        }
    }
    
    // -- double --------------------------------------------------------------
    
    /**
     * Indicates if convenience get method can be called without
     * an exception being thrown for the double type.
     * @return true if getDouble is supported, false otherwise
     */
    public boolean canGetDouble() {
        return canGet(double.class);
    }
    
    /**
     * Indicates if convenience set method can be called without
     * an exception being thrown for the double type.
     * @return true if setDouble is supported, false otherwise
     */
    public boolean canSetDouble() {
        return canSet(double.class);
    }
    
    /**
     * Get the data value at the specified row as a double
     * @param row the row from which to retrieve the value
     * @return the data value as a double
     * @throws DataTypeException if this column does not 
     *  support the double type
     */
    public double getDouble(int row) throws DataTypeException {
        if ( canGetDouble() ) {
            return ((Double)get(row)).doubleValue();
        } else {
            throw new DataTypeException(double.class);
        }
    }
    
    /**
     * Set the data value at the specified row as a double
     * @param val the value to set
     * @param row the row at which to set the value
     * @throws DataTypeException if this column does not 
     *  support the double type
     */
    public void setDouble(double val, int row) throws DataTypeException {
        if ( canSetDouble() ) {
            set(new Double(val), row);
        } else {
            throw new DataTypeException(double.class);
        }
    }
    
    // -- boolean -------------------------------------------------------------
    
    /**
     * Indicates if convenience get method can be called without
     * an exception being thrown for the boolean type.
     * @return true if getBoolean is supported, false otherwise
     */
    public boolean canGetBoolean() {
        return canGet(boolean.class);
    }
    
    /**
     * Indicates if convenience set method can be called without
     * an exception being thrown for the boolean type.
     * @return true if setBoolean is supported, false otherwise
     */
    public boolean canSetBoolean() {
        return canSet(boolean.class);
    }
    
    /**
     * Get the data value at the specified row as a boolean
     * @param row the row from which to retrieve the value
     * @return the data value as a boolean
     * @throws DataTypeException if this column does not 
     *  support the boolean type
     */
    public boolean getBoolean(int row) throws DataTypeException {
        if ( canGetBoolean() ) {
            return ((Boolean)get(row)).booleanValue();
        } else {
            throw new DataTypeException(boolean.class);
        }
    }
    
    /**
     * Set the data value at the specified row as a boolean
     * @param val the value to set
     * @param row the row at which to set the value
     * @throws DataTypeException if this column does not 
     *  support the boolean type
     */
    public void setBoolean(boolean val, int row) throws DataTypeException {
        if ( canSetBoolean() ) {
            set(new Boolean(val), row);
        } else {
            throw new DataTypeException(boolean.class);
        }
    }
    
    // -- String --------------------------------------------------------------
    
    /**
     * Indicates if convenience get method can be called without
     * an exception being thrown for the String type.
     * @return true if getString is supported, false otherwise
     */
    public boolean canGetString() {
        return true;
        //return canGet(String.class);
    }
    
    /**
     * Indicates if convenience set method can be called without
     * an exception being thrown for the String type.
     * @return true if setString is supported, false otherwise
     */
    public boolean canSetString() {
        return m_parser != null && !(m_parser instanceof ObjectParser);
        //return canSet(String.class);
    }
    
    /**
     * Get the data value at the specified row as a String
     * @param row the row from which to retrieve the value
     * @return the data value as a String
     * @throws DataTypeException if this column does not 
     *  support the String type
     */
    public String getString(int row) throws DataTypeException {
        if ( canGetString() ) {
            return m_parser.format(get(row));
        } else {
            throw new DataTypeException(String.class);
        }
    }
    
    /**
     * Set the data value at the specified row as a String
     * @param val the value to set
     * @param row the row at which to set the value
     * @throws DataTypeException if this column does not 
     *  support the String type
     */
    public void setString(String val, int row) throws DataTypeException {
        try {
            set(m_parser.parse(val), row);
        } catch (DataParseException e) {
            throw new DataTypeException(e);
        }
    }
    
    // -- Date ----------------------------------------------------------------
    
    /**
     * Indicates if convenience get method can be called without
     * an exception being thrown for the Date type.
     * @return true if getDate is supported, false otherwise
     */
    public boolean canGetDate() {
        return canGet(Date.class);
    }
    
    /**
     * Indicates if convenience set method can be called without
     * an exception being thrown for the Date type.
     * @return true if setDate is supported, false otherwise
     */
    public boolean canSetDate() {
        return canSet(Date.class);
    }
    
    /**
     * Get the data value at the specified row as a Date
     * @param row the row from which to retrieve the value
     * @return the data value as a Date
     * @throws DataTypeException if this column does not 
     *  support the Date type
     */
    public Date getDate(int row) throws DataTypeException {
        if ( canGetDate() ) {
            return (Date)get(row);
        } else {
            throw new DataTypeException(Date.class);
        }
    }
    
    /**
     * Set the data value at the specified row as a Date
     * @param val the value to set
     * @param row the row at which to set the value
     * @throws DataTypeException if this column does not 
     *  support the Date type
     */
    public void setDate(Date val, int row) throws DataTypeException {
        set(val, row);
    }
    
} // end of abstract class AbstractColumn
