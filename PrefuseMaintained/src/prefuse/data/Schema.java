package prefuse.data;

import java.util.HashMap;

import prefuse.util.PrefuseLib;

/**
 * <p>The Schema class represents a description of a Table's columns, including
 * column names, data types, and default values. New Table
 * instances can be created directly from Schema objects through the use of
 * the {@link #instantiate()} method. If a schema is subsequently changed,
 * instantiated table instances are not affected, keeping their original
 * schema.</p>
 * 
 * <p>Schema instances can be locked to prevent further changes. Any attempt
 * to alter a locked schema will result in a runtime exception being thrown.
 * If a schema is not locked, clients are free to add new columns and
 * edit default values.</p>
 * 
 * @author <a href="http://jheer.org">jeffrey heer</a>
 */
public class Schema implements Cloneable {

    private String[] m_names;
    private Class[]  m_types;
    private Object[] m_dflts;
    private HashMap  m_lookup;
    private int      m_size;
    private boolean  m_locked;
    
    // ------------------------------------------------------------------------
    // Constructors
    
    /**
     * Creates a new empty schema.
     */
    public Schema() {
        this(10);
    }
    
    /**
     * Creates a new empty schema with a starting capacity for a given number
     * of columns.
     * @param ncols the number of columns in this schema
     */
    public Schema(int ncols) {
        m_names = new String[ncols];
        m_types = new Class[ncols];
        m_dflts = new Object[ncols];
        m_size = 0;
        m_locked = false;
    }
    
    /**
     * Create a new schema consisting of the given column names and types.
     * @param names the column names
     * @param types the column types (as Class instances)
     */
    public Schema(String[] names, Class[] types) {
        this(names.length);
        
        // check the schema validity
        if ( names.length != types.length ) {
            throw new IllegalArgumentException(
                "Input arrays should be the same length");
        }
        for ( int i=0; i<names.length; ++i ) {
            addColumn(names[i], types[i], null);
        }
    }
    
    /**
     * Create a new schema consisting of the given column names, types, and
     * default column values.
     * @param names the column names
     * @param types the column types (as Class instances)
     * @param defaults the default values for each column
     */
    public Schema(String[] names, Class[] types, Object[] defaults) {
        this(names.length);
        
        // check the schema validity
        if ( names.length != types.length || 
             types.length != defaults.length )
        {
            throw new IllegalArgumentException(
                "Input arrays should be the same length");
        }
        for ( int i=0; i<names.length; ++i ) {
            addColumn(names[i], types[i], defaults[i]);
        }
    }
    
    /**
     * Creates a copy of this Schema. This might be useful for creating
     * extended schemas from a shared base schema. Cloned copies
     * of a locked Schema will not inherit the locked status.
     * @see java.lang.Object#clone()
     */
    public Object clone() {
        Schema s = new Schema(m_size);
        for ( int i=0; i<m_size; ++i ) {
            s.addColumn(m_names[i], m_types[i], m_dflts[i]);
        }
        return s;
    }
    
    /**
     * Lazily construct the lookup table for this schema. Used to
     * accelerate name-based lookups of schema information.
     */
    protected void initLookup() {
        m_lookup = new HashMap();
        for ( int i=0; i<m_names.length; ++i ) {
            m_lookup.put(m_names[i], new Integer(i));
        }
    }
    
    // ------------------------------------------------------------------------
    // Accessors / Mutators

    /**
     * Locks the schema, preventing any additional changes. Locked schemas
     * can not be unlocked! Cloned copies of a locked schema will not inherit
     * this locked status.
     * @return a pointer to this schema
     */
    public Schema lockSchema() {
        m_locked = true;
        return this;
    }
    
    /**
     * Indicates if this schema is locked. Locked schemas can not be edited.
     * @return true if this schema is locked, false otherwise
     */
    public boolean isLocked() {
        return m_locked;
    }    
    
    /**
     * Add a column to this schema.
     * @param name the column name
     * @param type the column type (as a Class instance)
     * @throws IllegalArgumentException is either name or type are null or
     * the name already exists in this schema.
     */
    public void addColumn(String name, Class type) {
        addColumn(name, type, null);
    }
    
    /**
     * Add a column to this schema.
     * @param name the column name
     * @param type the column type (as a Class instance)
     * @throws IllegalArgumentException is either name or type are null or
     * the name already exists in this schema.
     */
    public void addColumn(String name, Class type, Object defaultValue) {
        // check lock status
        if ( m_locked ) {
            throw new IllegalStateException(
                "Can not add column to a locked Schema.");
        }
        // check for validity
        if ( name == null ) {
            throw new IllegalArgumentException(
                "Null column names are not allowed.");
        }
        if ( type == null ) {
            throw new IllegalArgumentException(
                "Null column types are not allowed.");
        }
        for ( int i=0; i<m_size; ++i ) {
            if ( m_names[i].equals(name) ) {
                throw new IllegalArgumentException(
                    "Duplicate column names are not allowed: "+m_names[i]);
            }
        }
        
        // resize if necessary
        // TODO put resizing functionality into library routines?
        if ( m_names.length == m_size ) {
            int capacity = (3*m_names.length)/2 + 1;
            String[] names = new String[capacity];
            Class[]  types = new Class[capacity];
            Object[] dflts = new Object[capacity];
            System.arraycopy(m_names, 0, names, 0, m_size);
            System.arraycopy(m_types, 0, types, 0, m_size);
            System.arraycopy(m_dflts, 0, dflts, 0, m_size);
            m_names = names;
            m_types = types;
            m_dflts = dflts;
        }
        
        m_names[m_size] = name;
        m_types[m_size] = type;
        m_dflts[m_size] = defaultValue;
        
        if ( m_lookup != null )
            m_lookup.put(name, new Integer(m_size));
        
        ++m_size;
    }
    
    /**
     * <p>Add a new interpolated column to this data schema. This actually adds
     * three columns to the schema: a column for the current value of the
     * field, and columns for starting and ending values. During animation or
     * operations spread over a time span, the current value can be
     * interpolated between the start and end values.</p>
     * 
     * <p>The name for the current value column is the name parameter provided
     * to the method. The name for the start and end columns will be determined
     * by the return value of {@link PrefuseLib#getStartField(String)} and
     * {@link PrefuseLib#getEndField(String)}. The default behavior for these
     * methods is to append ":start" to the name of a stating value column
     * and append ":end" to the the name of an ending value column.</p>
     * 
     * @param name the name of the interpolated column to add
     * @param type the data type the columns will contain
     * @param dflt the default value for each of the columns
     */
    public void addInterpolatedColumn(String name, Class type, Object dflt) {
        addColumn(name, type, dflt);
        addColumn(PrefuseLib.getStartField(name), type, dflt);
        addColumn(PrefuseLib.getEndField(name), type, dflt);
    }
    
    /**
     * Add an interpolated column with a null default value.
     * @see #addInterpolatedColumn(String, Class, Object)
     * @param name the name of the interpolated column to add
     * @param type the data type the columns will contain
     */
    public void addInterpolatedColumn(String name, Class type) {
        addInterpolatedColumn(name, type, null);
    }
    
    /**
     * Get the number of columns in this schema.
     * @return the number of columns
     */
    public int getColumnCount() {
        return m_size;
    }
    
    /**
     * The name of the column at the given position.
     * @param col the column index
     * @return the column name
     */
    public String getColumnName(int col) {
        return m_names[col];
    }
    
    /**
     * The column index for the column with the given name.
     * @param field the column name
     * @return the column index
     */
    public int getColumnIndex(String field) {
        if ( m_lookup == null )
            initLookup();
        
        Integer idx = (Integer)m_lookup.get(field);
        return ( idx==null ? -1 : idx.intValue() );
    }
    
    /**
     * The type of the column at the given position.
     * @param col the column index
     * @return the column type
     */
    public Class getColumnType(int col) {
        return m_types[col];
    }

    /**
     * The type of the column with the given name.
     * @param field the column name
     * @return the column type
     */
    public Class getColumnType(String field) {
        int idx = getColumnIndex(field);
        return ( idx<0 ? null : m_types[idx] );
    }
    
    /**
     * The default value of the column at the given position.
     * @param col the column index
     * @return the column's default value
     */
    public Object getDefault(int col) {
        return m_dflts[col];
    }
    
    /**
     * The default value of the column with the given name.
     * @param field the column name
     * @return the column's default value
     */
    public Object getDefault(String field) {
        int idx = getColumnIndex(field);
        return ( idx<0 ? null : m_dflts[idx] );
    }
    
    /**
     * Set the default value for the given field.
     * @param col the column index of the field to set the default for
     * @param val the new default value
     */
    public void setDefault(int col, Object val) {
        // check lock status
        if ( m_locked ) {
            throw new IllegalStateException(
                "Can not update default values of a locked Schema.");
        }
        m_dflts[col] = val;
    }
    
    /**
     * Set the default value for the given field.
     * @param field the name of column to set the default for
     * @param val the new default value
     */
    public Schema setDefault(String field, Object val) {
        // check lock status
        if ( m_locked ) {
            throw new IllegalStateException(
                "Can not update default values of a locked Schema.");
        }
        int idx = getColumnIndex(field);
        m_dflts[idx] = val;
        return this;
    }
    
    /**
     * Set the default value for the given field as an int.
     * @param field the name of column to set the default for
     * @param val the new default value
     */
    public Schema setDefault(String field, int val) {
        setDefault(field, new Integer(val));
        return this;
    }
    
    /**
     * Set the default value for the given field as a long.
     * @param field the name of column to set the default for
     * @param val the new default value
     */
    public void setDefault(String field, long val) {
        setDefault(field, new Long(val));
    }
    
    /**
     * Set the default value for the given field as a float.
     * @param field the name of column to set the default for
     * @param val the new default value
     */
    public void setDefault(String field, float val) {
        setDefault(field, new Float(val));
    }
    
    /**
     * Set the default value for the given field as a double.
     * @param field the name of column to set the default for
     * @param val the new default value
     */
    public void setDefault(String field, double val) {
        setDefault(field, new Double(val));
    }
    
    /**
     * Set the default value for the given field as a boolean.
     * @param field the name of column to set the default for
     * @param val the new default value
     */
    public Schema setDefault(String field, boolean val) {
        setDefault(field, val ? Boolean.TRUE : Boolean.FALSE);
        return this;
    }
    
    /**
     * Set default values for the current, start, and end columns of an
     * interpolated column.
     * @param field the field name of the interpolated column
     * @param val the new default value for all three implicated columns
     */
    public void setInterpolatedDefault(String field, Object val) {
        setDefault(field, val);
        setDefault(PrefuseLib.getStartField(field), val);
        setDefault(PrefuseLib.getEndField(field), val);
    }
    
    /**
     * Set default values for the current, start, and end columns of an
     * interpolated column as an int.
     * @param field the field name of the interpolated column
     * @param val the new default value for all three implicated columns
     */
    public void setInterpolatedDefault(String field, int val) {
        setInterpolatedDefault(field, new Integer(val));
    }
    
    /**
     * Set default values for the current, start, and end columns of an
     * interpolated column as a long.
     * @param field the field name of the interpolated column
     * @param val the new default value for all three implicated columns
     */
    public void setInterpolatedDefault(String field, long val) {
        setInterpolatedDefault(field, new Long(val));
    }
    
    /**
     * Set default values for the current, start, and end columns of an
     * interpolated column as a float.
     * @param field the field name of the interpolated column
     * @param val the new default value for all three implicated columns
     */
    public void setInterpolatedDefault(String field, float val) {
        setInterpolatedDefault(field, new Float(val));
    }
    
    /**
     * Set default values for the current, start, and end columns of an
     * interpolated column as a double.
     * @param field the field name of the interpolated column
     * @param val the new default value for all three implicated columns
     */
    public void setInterpolatedDefault(String field, double val) {
        setInterpolatedDefault(field, new Double(val));
    }
    
    /**
     * Set default values for the current, start, and end columns of an
     * interpolated column as a boolean.
     * @param field the field name of the interpolated column
     * @param val the new default value for all three implicated columns
     */
    public void setInterpolatedDefault(String field, boolean val) {
        setInterpolatedDefault(field, val ? Boolean.TRUE : Boolean.FALSE);
    }
    
    
    // ------------------------------------------------------------------------
    // Comparison and Hashing
    
    /**
     * Compares this schema with another one for equality.
     */
    public boolean equals(Object o) {
        if ( !(o instanceof Schema) )
            return false;
        
        Schema s = (Schema)o;
        if ( m_size != s.getColumnCount() )
            return false;
        
        for ( int i=0; i<m_size; ++i ) {
            if ( !(m_names[i].equals(s.getColumnName(i)) &&
                   m_types[i].equals(s.getColumnType(i)) &&
                   m_dflts[i].equals(s.getDefault(i))) )
            {
                return false;
            }
        }
        return true;
    }
    
    /**
     * Indicates if values from a given Schema can be safely assigned to
     * data using this Schema. The input Schema must be less than or
     * equal in length to this Schema, and all contained columns in the
     * given Schema must have matching names and types to this Schema.
     * This method does not consider default values settings or the
     * locked status of the Schemas. For example, if the given Schema
     * has different default values than this one, this will have no
     * impact on the assignability of the two.
     * @param s the input Schema
     * @return true if data models using this Schema could be assigned values
     * directly from data using the input Schema, false otherwise.
     */
    public boolean isAssignableFrom(Schema s) {
        int ssize = s.getColumnCount();
        
        if ( ssize > m_size )
            return false;
        
        for ( int i=0; i<ssize; ++i ) {
            int idx = getColumnIndex(s.getColumnName(i));
            if ( idx < 0 )
                return false;
            
            if ( !m_types[idx].equals(s.getColumnType(i)) )
                return false;
        }
        return true;
    }
    
    /**
     * Computes a hashcode for this schema.
     */
    public int hashCode() {
        int hashcode = 0;
        for ( int i=0; i<m_size; ++i ) {
            int idx = i+1;
            int code = idx*m_names[i].hashCode();
            code ^= idx*m_types[i].hashCode();
            if ( m_dflts[i] != null )
                code ^= m_dflts[i].hashCode();
            hashcode ^= code;
        }
        return hashcode;
    }
    
    /**
     * Returns a descriptive String for this schema.
     */
    public String toString() {
        StringBuffer sbuf = new StringBuffer();
        sbuf.append("Schema[");
        for ( int i=0; i<m_size; ++i ) {
            if ( i > 0 ) sbuf.append(' ');
            sbuf.append('(').append(m_names[i]).append(", ");
            sbuf.append(m_types[i].getName()).append(", ");
            sbuf.append(m_dflts[i]).append(')');
        }
        sbuf.append(']');
        return sbuf.toString();
    }
    
    // ------------------------------------------------------------------------
    // Table Operations
       
    /**
     * Instantiate this schema as a new Table instance.
     * @return a new Table with this schema
     */
    public Table instantiate() {
        return instantiate(0);
    }
    
    /**
     * Instantiate this schema as a new Table instance.
     * @param nrows the number of starting rows in the table
     * @return a new Table with this schema
     */
    public Table instantiate(int nrows) {
        Table t = new Table(nrows, m_size);
        for ( int i=0; i<m_size; ++i ) {
            t.addColumn(m_names[i], m_types[i], m_dflts[i]);
        }
        return t;
    }
    
} // end of class Schema
