/**
 * 
 */
package prefuse.data.util;

import java.util.Arrays;
import java.util.Comparator;

import prefuse.data.Schema;
import prefuse.data.Table;
import prefuse.data.Tuple;
import prefuse.data.expression.Predicate;
import prefuse.data.tuple.TupleSet;
import prefuse.util.collections.CompositeComparator;
import prefuse.util.collections.NullComparator;

/**
 * <p>Utility class representing sorting criteria, this can be given as
 * input to the {@link TupleSet#tuples(Predicate, Sort)} method to
 * get a sorted iteration of tuples.</p>
 * 
 * <p>Sort criteria consists of an ordered list of data field names to
 * sort by, along with an indication to sort tuples in either ascending
 * or descending order. These criteria can be passed in to the
 * constructor or added incrementally using the
 * {@link #add(String, boolean)} method.</p>
 * 
 * <p>Alternatively, one can also specify the sorting criteria using a
 * single string, which is parsed using the {@link #parse(String)} method.
 * This string should consist
 * of a comma-delimited list of field names, which optional "ASC" or
 * "DESC" modifiers to specify ascending or descending sorts. If no
 * modifier is given, ascending order is assumed. Field
 * names which include spaces or other non-standard characters should
 * be written in brackets ([]), just as is done in 
 * {@link prefuse.data.expression.parser.ExpressionParser expression
 * language statements}. For example, the
 * following string</p>
 * 
 * <pre>"Profit DESC, [Product Type]"</pre>   
 * 
 * <p>sorts first by the data field "Profit" in descending order,
 * additionally sorting in ascending order by the data field
 * "Product Type" for tuples which have identical values in the
 * "Profit" field.</p>
 * 
 * @author <a href="http://jheer.org">jeffrey heer</a>
 */
public class Sort {

    private static final String ASC  = " ASC";
    private static final String DESC = " DESC";
    private static final String asc  = ASC.toLowerCase();
    private static final String desc = DESC.toLowerCase();
    
    private String[]  m_fields;
    private boolean[] m_ascend;
    
    /**
     * Creates a new, empty Sort specification.
     */
    public Sort() {
    	this(new String[0], new boolean[0]);
    }
    
    /**
     * Creates a new Sort specification that sorts on the
     * given fields, all in ascending order.
     * @param fields the fields to sort on, in order of precedence
     */
    public Sort(String[] fields) {
    	this(fields, new boolean[fields.length]);
    	Arrays.fill(m_ascend, true);
    }
    
    /**
     * Creates a new Sort specification that sorts on the
     * given fields in the given orders.
     * @param fields the fields to sort on, in order of precedence
     * @param ascend for each field, indicates if the field should
     * be sorted in ascending (true) or descending (false) order
     */
    public Sort(String[] fields, boolean[] ascend) {
        m_fields = fields;
        m_ascend = ascend;
    }
    
    /**
     * Adds a new field to this Sort specification.
     * @param field the additional field to sort on
     * @param ascend indicates if the field should
     * be sorted in ascending (true) or descending (false) order
     */
    public void add(String field, boolean ascend) {
        String[] f = new String[m_fields.length+1];
        System.arraycopy(m_fields, 0, f, 0, m_fields.length);
        f[m_fields.length] = field;
        m_fields = f;
        
        boolean[] b = new boolean[m_fields.length+1];
        System.arraycopy(m_ascend, 0, b, 0, m_ascend.length);
        b[m_ascend.length] = ascend;
        m_ascend = b;
    }
    
    /**
     * Returns the number of fields in this Sort specification.
     * @return the number of fields to sort on
     */
    public int size() {
        return m_fields.length;
    }
    
    /**
     * Returns the sort field at the given index.
     * @param i the index to look up
     * @return the sort field at the given index
     */
    public String getField(int i) {
        return m_fields[i];
    }
    
    /**
     * Returns the ascending modifier as the given index.
     * @param i the index to look up
     * @return true if the field at the given index is to be sorted
     * in ascending order, false for descending order
     */
    public boolean isAscending(int i) {
        return m_ascend[i];
    }
    
    /**
     * Generates a Comparator to be used for sorting tuples drawn from
     * the given tuple set.
     * @param ts the TupleSet whose Tuples are to be sorted
     * @return a Comparator instance for sorting tuples from the given
     * set using the sorting criteria given in this specification
     */
    public Comparator getComparator(TupleSet ts) {
        // get the schema, so we can lookup column value types        
        Schema s = null;
        if ( ts instanceof Table ) {
            // for Tables, we can get this directly
        	s = ((Table)ts).getSchema();
        } else {
        	// if non-table tuple set is empty, we punt
        	if ( ts.getTupleCount() == 0 )
        		return new NullComparator();
        	// otherwise, use the schema of the first tuple in the set
            s = ((Tuple)ts.tuples().next()).getSchema();
        }
        // create the comparator
        CompositeComparator cc = new CompositeComparator(m_fields.length);
        for ( int i=0; i<m_fields.length; ++i ) {
            cc.add(new TupleComparator(m_fields[i],
                       s.getColumnType(m_fields[i]), m_ascend[i]));
        }
        return cc;
    }
    
    // ------------------------------------------------------------------------
    
    private static void subparse(String s, Object[] res) {
        s = s.trim();
        
        // extract ascending modifier first
        res[1] = Boolean.TRUE;
        if ( s.endsWith(DESC) || s.endsWith(desc) ) {
            res[1] = Boolean.FALSE;
            s = s.substring(0, s.length()-DESC.length()).trim();
        } else if ( s.endsWith(ASC) || s.endsWith(asc) ) {
            s = s.substring(0, s.length()-ASC.length()).trim();
        }
        
        if ( s.startsWith("[") ) {
            if ( s.lastIndexOf("[") == 0 && 
                 s.endsWith("]") && s.indexOf("]") == s.length() ) {
                res[0] = s.substring(1, s.length()-1);
            } else {
                throw new RuntimeException();
            }
        } else {
            if ( s.indexOf(" ") < 0 && s.indexOf("\t") < 0 ) {
                res[0] = s;
            } else {
                throw new RuntimeException();
            }
        }
    }
    
    /**
     * Parse a comma-delimited String of data fields to sort on, along
     * with optional ASC or DESC modifiers, to generate a new Sort
     * specification. This string should consist
	 * of a comma-delimited list of field names, which optional "ASC" or
	 * "DESC" modifiers to specify ascending or descending sorts. If no
	 * modifier is given, ascending order is assumed. Field
	 * names which include spaces or other non-standard characters should
	 * be written in brackets ([]), just as is done in 
	 * {@link prefuse.data.expression.parser.ExpressionParser expression
	 * language statements}. For example, the
	 * following string</p>
	 * 
	 * <pre>"Profit DESC, [Product Type]"</pre>   
	 * 
	 * <p>sorts first by the data field "Profit" in descending order,
	 * additionally sorting in ascending order by the data field
	 * "Product Type" for tuples which have identical values in the
	 * "Profit" field.</p>
     * @param s the sort specification String
     * @return a new Sort specification
     */
    public static Sort parse(String s) {
        Sort sort = new Sort();
        Object[] res = new Object[2];
        int idx = 0, len = s.length();
        int comma = s.indexOf(',');
        int quote = s.indexOf('[');
        while ( idx < len ) {
            if ( comma < 0 ) {
                subparse(s.substring(idx), res);
                sort.add((String)res[0], ((Boolean)res[1]).booleanValue());
                break;
            } else if ( quote < 0 || comma < quote ) {
                subparse(s.substring(idx, comma), res);
                sort.add((String)res[0], ((Boolean)res[1]).booleanValue());
                idx = comma + 1;
                comma = s.indexOf(idx, ',');
            } else {
                int q2 = s.indexOf(quote, ']');
                if ( q2 < 0 ) {
                    throw new RuntimeException();
                } else {
                    comma = s.indexOf(q2, ',');
                    subparse(s.substring(idx, comma), res);
                    sort.add((String)res[0], ((Boolean)res[1]).booleanValue());
                    idx = comma + 1;
                    comma = s.indexOf(idx, ',');
                }
            }
        }
        return sort;
    }
    
    /**
     * @see java.lang.Object#toString()
     */
    public String toString() {
    	StringBuffer sbuf = new StringBuffer();
    	for ( int i=0; i<m_fields.length; ++i ) {
    		if ( i > 0 ) sbuf.append(", ");
    		sbuf.append('[').append(m_fields[i]).append(']');
    		sbuf.append((m_ascend[i]) ? ASC : DESC);
    	}
    	return sbuf.toString();
    }
    
} // end of class Sort
