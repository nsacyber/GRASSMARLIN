package prefuse.data.column;

import java.util.Date;

import prefuse.data.DataTypeException;
import prefuse.data.event.ColumnListener;
import prefuse.data.parser.DataParser;


/**
 * Interface for a data column in a table.
 * 
 * @author <a href="http://jheer.org">jeffrey heer</a>
 */
public interface Column {
    
    // ------------------------------------------------------------------------
    // Column Metadata
    
    /**
     * Returns the number of rows in this data column
     * @return the number of rows
     */
    public int getRowCount();
    
    /**
     * Sets the number of rows in this data column
     * @param nrows the number of rows
     */
    public void setMaximumRow(int nrows);
    
    /**
     * Indicates if the values in this column are read-only.
     * @return true if the values can not be edited, false otherwise
     */
    public boolean isReadOnly();
    
    /**
     * Sets if the values in this column are read-only
     * @param readOnly true to ensure the values can not be edited, 
     *  false otherwise
     */
    public void setReadOnly(boolean readOnly);
    
    /**
     * Indicates if the value at the given row can be edited.
     * @param row the row to check
     * @return true is the value can be modified, false otherwise
     */
    public boolean isCellEditable(int row);
    
    /**
     * Returns the most specific superclass for the values in the column
     * @return the Class of the column's data values
     */
    public Class getColumnType();
    
    /**
     * Get the data parser used to map String values to and from the values
     * stored by this Column.
     * @return the DataParser used by this Column
     */
    public DataParser getParser();

    /**
     * Set the data parser used to map String values to and from the values
     * stored by this Column.
     * @param parser the DataParser to use
     */
    public void setParser(DataParser parser);
    
    // ------------------------------------------------------------------------
    // Listener Methods
    
    /**
     * Adds a listener to be notified when this column changes
     * @param listener the ColumnListener to add
     */
    public void addColumnListener(ColumnListener listener);

    /**
     * Removes a listener, causing it to no longer be notified of changes
     * @param listener the ColumnListener to remove
     */
    public void removeColumnListener(ColumnListener listener);
    
    
    // ------------------------------------------------------------------------
    // Data Access Methods
    
    /**
     * Returns the default value for rows that have not been set explicitly. 
     */
    public Object getDefaultValue();
    
    /**
     * Reverts the specified row back to the column's default value.
     * @param row
     */
    public void revertToDefault(int row);
    
    /**
     * Indicates if the get method can be called without
     * an exception being thrown for the given type.
     * @param type the Class of the data type to check
     * @return true if the type is supported by this column, false otherwise
     */
    public boolean canGet(Class type);
    
    /**
     * Indicates if the set method can be called without
     * an exception being thrown for the given type.
     * @param type the Class of the data type to check
     * @return true if the type is supported by this column, false otherwise
     */
    public boolean canSet(Class type);
    
    /**
     * Get the data value at the specified row
     * @param row the row from which to retrieve the value
     * @return the data value
     */
    public Object get(int row);
        
    /**
     * Set the data value at the specified row
     * @param val the value to set
     * @param row the row at which to set the value
     */
    public void set(Object val, int row) throws DataTypeException;
    
    
    // ------------------------------------------------------------------------
    // Data Type Convenience Methods
    
    // because java's type system can be tedious at times...
    
    // -- int -----------------------------------------------------------------
    
    /**
     * Indicates if convenience get method can be called without
     * an exception being thrown for the int type.
     * @return true if getInt is supported, false otherwise
     */
    public boolean canGetInt();
    
    /**
     * Indicates if convenience set method can be called without
     * an exception being thrown for the int type.
     * @return true if setInt is supported, false otherwise
     */
    public boolean canSetInt();
    
    /**
     * Get the data value at the specified row as an integer
     * @param row the row from which to retrieve the value
     * @return the data value as an integer
     * @throws DataTypeException if this column does not 
     *  support the integer type
     */
    public int getInt(int row) throws DataTypeException;
    
    /**
     * Set the data value at the specified row as an integer
     * @param val the value to set
     * @param row the row at which to set the value
     * @throws DataTypeException if this column does not 
     *  support the integer type
     */
    public void setInt(int val, int row) throws DataTypeException;

    // -- long ----------------------------------------------------------------
    
    /**
     * Indicates if convenience get method can be called without
     * an exception being thrown for the long type.
     * @return true if getLong is supported, false otherwise
     */
    public boolean canGetLong();
    
    /**
     * Indicates if convenience set method can be called without
     * an exception being thrown for the long type.
     * @return true if setLong is supported, false otherwise
     */
    public boolean canSetLong();
    
    /**
     * Get the data value at the specified row as a long
     * @param row the row from which to retrieve the value
     * @return the data value as a long
     * @throws DataTypeException if this column does not 
     *  support the long type
     */
    public long getLong(int row) throws DataTypeException;
    
    /**
     * Set the data value at the specified row as a long
     * @param val the value to set
     * @param row the row at which to set the value
     * @throws DataTypeException if this column does not 
     *  support the long type
     */
    public void setLong(long val, int row) throws DataTypeException;
    
    // -- float ---------------------------------------------------------------    
    
    /**
     * Indicates if convenience get method can be called without
     * an exception being thrown for the float type.
     * @return true if getFloat is supported, false otherwise
     */
    public boolean canGetFloat();
    
    /**
     * Indicates if convenience set method can be called without
     * an exception being thrown for the float type.
     * @return true if setFloat is supported, false otherwise
     */
    public boolean canSetFloat();
    
    /**
     * Get the data value at the specified row as a float
     * @param row the row from which to retrieve the value
     * @return the data value as a float
     * @throws DataTypeException if this column does not 
     *  support the float type
     */
    public float getFloat(int row) throws DataTypeException;
    
    /**
     * Set the data value at the specified row as a float
     * @param val the value to set
     * @param row the row at which to set the value
     * @throws DataTypeException if this column does not 
     *  support the float type
     */
    public void setFloat(float val, int row) throws DataTypeException;
    
    // -- double --------------------------------------------------------------
    
    /**
     * Indicates if convenience get method can be called without
     * an exception being thrown for the double type.
     * @return true if getDouble is supported, false otherwise
     */
    public boolean canGetDouble();
    
    /**
     * Indicates if convenience set method can be called without
     * an exception being thrown for the double type.
     * @return true if setDouble is supported, false otherwise
     */
    public boolean canSetDouble();
    
    /**
     * Get the data value at the specified row as a double
     * @param row the row from which to retrieve the value
     * @return the data value as a double
     * @throws DataTypeException if this column does not 
     *  support the double type
     */
    public double getDouble(int row) throws DataTypeException;
    
    /**
     * Set the data value at the specified row as a double
     * @param val the value to set
     * @param row the row at which to set the value
     * @throws DataTypeException if this column does not 
     *  support the double type
     */
    public void setDouble(double val, int row) throws DataTypeException;
    
    // -- boolean -------------------------------------------------------------
    
    /**
     * Indicates if convenience get method can be called without
     * an exception being thrown for the boolean type.
     * @return true if getBoolean is supported, false otherwise
     */
    public boolean canGetBoolean();
    
    /**
     * Indicates if convenience set method can be called without
     * an exception being thrown for the boolean type.
     * @return true if setBoolean is supported, false otherwise
     */
    public boolean canSetBoolean();
    
    /**
     * Get the data value at the specified row as a boolean
     * @param row the row from which to retrieve the value
     * @return the data value as a boolean
     * @throws DataTypeException if this column does not 
     *  support the boolean type
     */
    public boolean getBoolean(int row) throws DataTypeException;
    
    /**
     * Set the data value at the specified row as a boolean
     * @param val the value to set
     * @param row the row at which to set the value
     * @throws DataTypeException if this column does not 
     *  support the boolean type
     */
    public void setBoolean(boolean val, int row) throws DataTypeException;
    
    // -- String --------------------------------------------------------------
    
    /**
     * Indicates if convenience get method can be called without
     * an exception being thrown for the String type.
     * @return true if getString is supported, false otherwise
     */
    public boolean canGetString();
    
    /**
     * Indicates if convenience set method can be called without
     * an exception being thrown for the String type.
     * @return true if setString is supported, false otherwise
     */
    public boolean canSetString();
    
    /**
     * Get the data value at the specified row as a String
     * @param row the row from which to retrieve the value
     * @return the data value as a String
     * @throws DataTypeException if this column does not 
     *  support the String type
     */
    public String getString(int row) throws DataTypeException;
    
    /**
     * Set the data value at the specified row as a String
     * @param val the value to set
     * @param row the row at which to set the value
     * @throws DataTypeException if this column does not 
     *  support the String type
     */
    public void setString(String val, int row) throws DataTypeException;
    
    // -- Date ----------------------------------------------------------------
    
    /**
     * Indicates if convenience get method can be called without
     * an exception being thrown for the Date type.
     * @return true if getDate is supported, false otherwise
     */
    public boolean canGetDate();
    
    /**
     * Indicates if convenience set method can be called without
     * an exception being thrown for the Date type.
     * @return true if setDate is supported, false otherwise
     */
    public boolean canSetDate();
    
    /**
     * Get the data value at the specified row as a Date
     * @param row the row from which to retrieve the value
     * @return the data value as a Date
     * @throws DataTypeException if this column does not 
     *  support the Date type
     */
    public Date getDate(int row) throws DataTypeException;
    
    /**
     * Set the data value at the specified row as a Date
     * @param val the value to set
     * @param row the row at which to set the value
     * @throws DataTypeException if this column does not 
     *  support the Date type
     */
    public void setDate(Date val, int row) throws DataTypeException;
    
} // end of interface Column
