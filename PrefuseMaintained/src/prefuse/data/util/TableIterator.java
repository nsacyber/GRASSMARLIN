package prefuse.data.util;

import java.util.ConcurrentModificationException;
import java.util.Date;

import prefuse.data.Table;
import prefuse.util.collections.IntIterator;

/**
 * An iterator over table rows, providing convenience methods for accessing and
 * manipulating table data.
 * 
 * @author <a href="http://jheer.org">jeffrey heer</a>
 */
public class TableIterator extends IntIterator {

    private Table       m_table;
    private IntIterator m_rows;
    private int         m_modCount;
    
    protected int m_cur = -1;

    /**
     * Create a new TableIterator using a given iterator over table rows.
     * @param table the Table to iterate over
     * @param rows the iteration over individual table rows
     */
    public TableIterator(Table table, IntIterator rows) {
        m_table = table;
        m_rows = rows;
        m_modCount = table.getModificationCount();
    }

    // ------------------------------------------------------------------------
    // Iterator Methods
    
    /**
     * Returns the next table row.
     * @see prefuse.util.collections.LiteralIterator#nextInt()
     */
    public int nextInt() {
        if ( m_modCount != m_table.getModificationCount() )
            throw new ConcurrentModificationException();
        m_cur = m_rows.nextInt();
        return m_cur;
    }

    /**
     * @see java.util.Iterator#hasNext()
     */
    public boolean hasNext() {
        return m_rows.hasNext();
    }

    /**
     * Remove the current row, deleting it from the table.
     * @see java.util.Iterator#remove()
     */
    public void remove() {
        if ( m_table.removeRow(m_cur) )
            modify();
    }
    
    /**
     * Tracks table modifications.
     */
    protected void modify() {
        ++m_modCount;
        m_cur = -1;
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
     * the {@link #get(String)} can be cast to the given type.
     * @see #get(String)
     */
    public final boolean canGet(String field, Class type) {
        return m_table.canGet(field, type);
    }
    
    /**
     * Check if the <code>set</code> method for the given data field can
     * accept values of a given target type.
     * @param field the data field to check
     * @param type a Class instance to check for compatibility with the
     * data field values.
     * @return true if the data field is compatible with provided type,
     * false otherwise. If the value is true, objects of the given type
     * can be used as parameters of the {@link #set(String, Object)} method.
     * @see #set(String, Object)
     */
    public final boolean canSet(String field, Class type) {
        return m_table.canSet(field, type);
    }
    
    /**
     * Get the data value at the given field as an Object.
     * @param field the data field to retrieve
     * @return the data value as an Object. The concrete type of this
     * Object is dependent on the underlying data column used.
     * @see #canGet(String, Class)
     * @see prefuse.data.Table#getColumnType(String)
     */
    public final Object get(String field) {
        return m_table.get(m_cur, field);
    }
    
    /**
     * Set the value of a given data field.
     * @param field the data field to set
     * @param val the value for the field. If the concrete type of this
     * Object is not compatible with the underlying data model, an
     * Exception will be thrown. Use the {@link #canSet(String, Class)}
     * method to check the type-safety ahead of time.
     * @see #canSet(String, Class)
     * @see prefuse.data.Table#getColumnType(String)
     */
    public final void set(String field, Object val) {
        ++m_modCount;
        m_table.set(m_cur, field, val);
    }
    
    // ------------------------------------------------------------------------
    // Convenience Data Access Methods
    
    /**
     * Check if the given data field can return primitive <code>int</code>
     * values.
     * @param field the data field to check
     * @return true if the data field can return primitive <code>int</code>
     * values, false otherwise. If true, the {@link #getInt(String)} method
     * can be used safely.
     */
    public final boolean canGetInt(String field) {
        return m_table.canGetInt(field);
    }
    
    /**
     * Check if the <code>setInt</code> method can safely be used for the
     * given data field.
     * @param field the data field to check
     * @return true if the {@link #setInt(String, int)} method can safely
     * be used for the given field, false otherwise.
     */
    public final boolean canSetInt(String field) {
        return m_table.canSetInt(field);
    }
    
    /**
     * Get the data value at the given field as an <code>int</code>.
     * @param field the data field to retrieve
     * @see #canGetInt(String)
     */
    public final int getInt(String field) {
        return m_table.getInt(m_cur, field);
    }
    
    /**
     * Set the data value of the given field with an <code>int</code>.
     * @param field the data field to set
     * @param val the value to set
     * @see #canSetInt(String)
     */
    public final void setInt(String field, int val) {
        ++m_modCount;
        m_table.setInt(m_cur, field, val);
    }
    
    // --------------------------------------------------------------
    
    /**
     * Check if the given data field can return primitive <code>long</code>
     * values.
     * @param field the data field to check
     * @return true if the data field can return primitive <code>long</code>
     * values, false otherwise. If true, the {@link #getLong(String)} method
     * can be used safely.
     */
    public final boolean canGetLong(String field) {
        return m_table.canGetLong(field);
    }
    
    /**
     * Check if the <code>setLong</code> method can safely be used for the
     * given data field.
     * @param field the data field to check
     * @return true if the {@link #setLong(String, long)} method can safely
     * be used for the given field, false otherwise.
     */
    public final boolean canSetLong(String field) {
        return m_table.canSetLong(field);
    }
    
    /**
     * Get the data value at the given field as a <code>long</code>.
     * @param field the data field to retrieve
     * @see #canGetLong(String)
     */
    public final long getLong(String field) {
        return m_table.getLong(m_cur, field);
    }
    
    /**
     * Set the data value of the given field with a <code>long</code>.
     * @param field the data field to set
     * @param val the value to set
     * @see #canSetLong(String)
     */
    public final void setLong(String field, long val) {
        ++m_modCount;
        m_table.setLong(m_cur, field, val);
    }

    // --------------------------------------------------------------
    
    /**
     * Check if the given data field can return primitive <code>float</code>
     * values.
     * @param field the data field to check
     * @return true if the data field can return primitive <code>float</code>
     * values, false otherwise. If true, the {@link #getFloat(String)} method
     * can be used safely.
     */
    public final boolean canGetFloat(String field) {
        return m_table.canGetFloat(field);
    }
    
    /**
     * Check if the <code>setFloat</code> method can safely be used for the
     * given data field.
     * @param field the data field to check
     * @return true if the {@link #setFloat(String, float)} method can safely
     * be used for the given field, false otherwise.
     */
    public final boolean canSetFloat(String field) {
        return m_table.canSetFloat(field);
    }
    
    /**
     * Get the data value at the given field as a <code>float</code>.
     * @param field the data field to retrieve
     * @see #canGetFloat(String)
     */
    public final float getFloat(String field) {
        return m_table.getFloat(m_cur, field);
    }
    
    /**
     * Set the data value of the given field with a <code>float</code>.
     * @param field the data field to set
     * @param val the value to set
     * @see #canSetFloat(String)
     */
    public final void setFloat(String field, float val) {
        ++m_modCount;
        m_table.setFloat(m_cur, field, val);
    }
    
    // --------------------------------------------------------------
    
    /**
     * Check if the given data field can return primitive <code>double</code>
     * values.
     * @param field the data field to check
     * @return true if the data field can return primitive <code>double</code>
     * values, false otherwise. If true, the {@link #getDouble(String)} method
     * can be used safely.
     */
    public final boolean canGetDouble(String field) {
        return m_table.canGetDouble(field);
    }
    
    /**
     * Check if the <code>setDouble</code> method can safely be used for the
     * given data field.
     * @param field the data field to check
     * @return true if the {@link #setDouble(String, double)} method can safely
     * be used for the given field, false otherwise.
     */
    public final boolean canSetDouble(String field) {
        return m_table.canSetDouble(field);
    }
    
    /**
     * Get the data value at the given field as a <code>double</code>.
     * @param field the data field to retrieve
     * @see #canGetDouble(String)
     */
    public final double getDouble(String field) {
        return m_table.getDouble(m_cur, field);
    }
    
    /**
     * Set the data value of the given field with a <code>double</code>.
     * @param field the data field to set
     * @param val the value to set
     * @see #canSetDouble(String)
     */
    public final void setDouble(String field, double val) {
        ++m_modCount;
        m_table.setDouble(m_cur, field, val);
    }
    
    // --------------------------------------------------------------
    
    /**
     * Check if the given data field can return primitive <code>boolean</code>
     * values.
     * @param field the data field to check
     * @return true if the data field can return primitive <code>boolean</code>
     * values, false otherwise. If true, the {@link #getBoolean(String)} method
     * can be used safely.
     */
    public final boolean canGetBoolean(String field) {
        return m_table.canGetBoolean(field);
    }
    
    /**
     * Check if the <code>setBoolean</code> method can safely be used for the
     * given data field.
     * @param field the data field to check
     * @return true if the {@link #setBoolean(String, boolean)} method can
     * safely be used for the given field, false otherwise.
     */
    public final boolean canSetBoolean(String field) {
        return m_table.canSetBoolean(field);
    }
    
    /**
     * Get the data value at the given field as a <code>boolean</code>.
     * @param field the data field to retrieve
     * @see #canGetBoolean(String)
     */
    public final boolean getBoolean(String field) {
        return m_table.getBoolean(m_cur, field);
    }
    
    /**
     * Set the data value of the given field with a <code>boolean</code>.
     * @param field the data field to set
     * @param val the value to set
     * @see #canSetBoolean(String)
     */
    public final void setBoolean(String field, boolean val) {
        ++m_modCount;
        m_table.setBoolean(m_cur, field, val);
    }
    
    // --------------------------------------------------------------
    
    /**
     * Check if the given data field can return <code>String</code>
     * values.
     * @param field the data field to check
     * @return true if the data field can return <code>String</code>
     * values, false otherwise. If true, the {@link #getString(String)} method
     * can be used safely.
     */
    public final boolean canGetString(String field) {
        return m_table.canGetString(field);
    }
    
    /**
     * Check if the <code>setString</code> method can safely be used for the
     * given data field.
     * @param field the data field to check
     * @return true if the {@link #setString(String, String)} method can safely
     * be used for the given field, false otherwise.
     */
    public final boolean canSetString(String field) {
        return m_table.canSetString(field);
    }
    
    /**
     * Get the data value at the given field as a <code>String</code>.
     * @param field the data field to retrieve
     * @see #canGetString(String)
     */
    public final String getString(String field) {
        return m_table.getString(m_cur, field);
    }
    
    /**
     * Set the data value of the given field with a <code>String</code>.
     * @param field the data field to set
     * @param val the value to set
     * @see #canSetString(String)
     */
    public final void setString(String field, String val) {
        ++m_modCount;
        m_table.setString(m_cur, field, val);
    }
    
    // --------------------------------------------------------------
    
    /**
     * Check if the given data field can return <code>Date</code>
     * values.
     * @param field the data field to check
     * @return true if the data field can return <code>Date</code>
     * values, false otherwise. If true, the {@link #getDate(String)} method
     * can be used safely.
     */
    public final boolean canGetDate(String field) {
        return m_table.canGetDate(field);
    }
    
    /**
     * Check if the <code>setDate</code> method can safely be used for the
     * given data field.
     * @param field the data field to check
     * @return true if the {@link #setDate(String, Date)} method can safely
     * be used for the given field, false otherwise.
     */
    public final boolean canSetDate(String field) {
        return m_table.canSetDate(field);
    }
    
    /**
     * Get the data value at the given field as a <code>Date</code>.
     * @param field the data field to retrieve
     * @see #canGetDate(String)
     */
    public final Date getDate(String field) {
        return m_table.getDate(m_cur, field);
    }
    
    /**
     * Set the data value of the given field with a <code>Date</code>.
     * @param field the data field to set
     * @param val the value to set
     * @see #canSetDate(String)
     */
    public final void setDate(String field, Date val) {
        ++m_modCount;
        m_table.setDate(m_cur, field, val);
    }
    
} // end of class TableIterator
