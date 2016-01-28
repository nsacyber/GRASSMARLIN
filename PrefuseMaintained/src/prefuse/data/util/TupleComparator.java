/**
 * 
 */
package prefuse.data.util;

import java.util.Comparator;

import prefuse.data.Tuple;
import prefuse.util.collections.DefaultLiteralComparator;
import prefuse.util.collections.LiteralComparator;

/**
 * Comparator that compares Tuples based on the value of a single field.
 * 
 * @author <a href="http://jheer.org">jeffrey heer</a>
 */
public class TupleComparator implements Comparator {

    private String m_field;
    private int m_col;
    private Comparator m_cmp;
    private Class m_type;
    private int m_rev;
    
    /**
     * Creates a new TupleComparator.
     * @param field the data field to compare
     * @param type the expected type of the data field
     * @param ascend true to sort in ascending order, false for descending
     */
    public TupleComparator(String field, Class type, boolean ascend) {
        this(field, type, ascend, DefaultLiteralComparator.getInstance());
    }
    
    /**
     * Creates a new TupleComparator.
     * @param field the data field to compare
     * @param type the expected type of the data field
     * @param ascend true to sort in ascending order, false for descending
     * @param c the comparator to use. Note that for primitive types,
     * this should be an instance of LiteralComparator, otherwise
     * subequent errors will occur.
     */
    public TupleComparator(String field, Class type, 
                           boolean ascend, Comparator c)
    {
        m_field = field;
        m_col = -1;
        m_type = type;
        m_rev = ascend ? 1 : -1;
        m_cmp = c;
    }
    
    /**
     * Creates a new TupleComparator.
     * @param col the column number of the data field to compare
     * @param type the expected type of the data field
     * @param ascend true to sort in ascending order, false for descending
     */
    public TupleComparator(int col, Class type, boolean ascend) {
        this(col, type, ascend, DefaultLiteralComparator.getInstance());
    }
    
    /**
     * Creates a new TupleComparator.
     * @param col the column number of the data field to compare
     * @param type the expected type of the data field
     * @param ascend true to sort in ascending order, false for descending
     */
    public TupleComparator(int col, Class type, boolean ascend, Comparator c) {
        m_field = null;
        m_col = col;
        m_type = type;
        m_rev = ascend ? 1 : -1;
        m_cmp = c;
    }
    
    /**
     * Compares two tuples. If either input Object is not a Tuple,
     * a ClassCastException will be thrown.
     * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
     */
    public int compare(Object o1, Object o2) {
        Tuple t1 = (Tuple)o1, t2 = (Tuple)o2;
        int c = 0;
        
        if ( m_col == -1 ) {
            if ( m_type == int.class || m_type == byte.class ) {
                c = ((LiteralComparator)m_cmp).compare(
                        t1.getInt(m_field), t2.getInt(m_field));
            } else if ( m_type == double.class ) {
                c = ((LiteralComparator)m_cmp).compare(
                        t1.getDouble(m_field), t2.getDouble(m_field));
            } else if ( m_type == long.class ) {
                c = ((LiteralComparator)m_cmp).compare(
                        t1.getLong(m_field), t2.getLong(m_field));
            } else if ( m_type == float.class ) {
                c = ((LiteralComparator)m_cmp).compare(
                        t1.getFloat(m_field), t2.getFloat(m_field));
            } else if ( m_type == boolean.class ) {
                c = ((LiteralComparator)m_cmp).compare(
                        t1.getBoolean(m_field), t2.getBoolean(m_field));
            } else if ( !m_type.isPrimitive() ) {
                c = m_cmp.compare(t1.get(m_field), t2.get(m_field));
            } else {
                throw new IllegalStateException(
                        "Unsupported type: " + m_type.getName());
            }
        } else {
            if ( m_type == int.class || m_type == byte.class ) {
                c = ((LiteralComparator)m_cmp).compare(
                        t1.getInt(m_col), t2.getInt(m_col));
            } else if ( m_type == double.class ) {
                c = ((LiteralComparator)m_cmp).compare(
                        t1.getDouble(m_col), t2.getDouble(m_col));
            } else if ( m_type == long.class ) {
                c = ((LiteralComparator)m_cmp).compare(
                        t1.getLong(m_col), t2.getLong(m_col));
            } else if ( m_type == float.class ) {
                c = ((LiteralComparator)m_cmp).compare(
                        t1.getFloat(m_col), t2.getFloat(m_col));
            } else if ( m_type == boolean.class ) {
                c = ((LiteralComparator)m_cmp).compare(
                        t1.getBoolean(m_col), t2.getBoolean(m_col));
            } else if ( !m_type.isPrimitive() ) {
                c = m_cmp.compare(t1.get(m_col), t2.get(m_col));
            } else {
                throw new IllegalStateException(
                        "Unsupported type: " + m_type.getName());
            }
        }
        return m_rev * c;
    }

} // end of class TupleComparator
