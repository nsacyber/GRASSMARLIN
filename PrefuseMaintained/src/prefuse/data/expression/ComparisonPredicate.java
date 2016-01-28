package prefuse.data.expression;

import java.util.Comparator;

import prefuse.data.Schema;
import prefuse.data.Tuple;
import prefuse.util.TypeLib;
import prefuse.util.collections.DefaultLiteralComparator;
import prefuse.util.collections.LiteralComparator;

/**
 * Predicate implementation that computes a comparison operation. Supported
 * operations are equals, not equals, less than, greater than, less than or
 * equal to, and greater than or equal to.
 * 
 * @author <a href="http://jheer.org">jeffrey heer</a>
 */
public class ComparisonPredicate extends BinaryExpression implements Predicate {

    /** Indicates a less-than comparison. */
    public static final int LT   = 0;
    /** Indicates a greater-than comparison. */
    public static final int GT   = 1;
    /** Indicates a equals comparison. */
    public static final int EQ   = 2;
    /** Indicates a not-equals comparison. */
    public static final int NEQ  = 3;
    /** Indicates a less-than-or-equals comparison. */
    public static final int LTEQ = 4;
    /** Indicates a greater-than-or-equals comparison. */
    public static final int GTEQ = 5;
    
    private Comparator m_cmp;
    
    /**
     * Create a new ComparisonPredicate. Uses a default comparator instance.
     * @param operation the comparison operation to compute
     * @param left the left sub-expression
     * @param right the right sub-expression
     */
    public ComparisonPredicate(int operation, 
            Expression left, Expression right)
    {
        this(operation, left, right, DefaultLiteralComparator.getInstance());
    }

    /**
     * Create a new ComparisonPredicate.
     * @param operation the comparison operation to compute
     * @param left the left sub-expression
     * @param right the right sub-expression
     * @param cmp the comparator to use to compare values
     */
    public ComparisonPredicate(int operation, 
            Expression left, Expression right, Comparator cmp)
    {
        super(operation, LT, GTEQ, left, right);
        this.m_cmp = cmp;
    }
    
    
    /**
     * Get the comparator used to compare instances.
     * @return the comparator instance
     */
    public Comparator getComparator() {
        return m_cmp;
    }
    
    // ------------------------------------------------------------------------
    
    /**
     * @see prefuse.data.expression.Expression#getType(prefuse.data.Schema)
     */
    public Class getType(Schema s) {
        return boolean.class;
    }
    
    /**
     * @see prefuse.data.expression.Expression#getBoolean(prefuse.data.Tuple)
     */
    public boolean getBoolean(Tuple t) {
        Class lType = m_left.getType(t.getSchema());
        Class rType = m_right.getType(t.getSchema());
        
        int c = 0;
        
        if( lType.isEnum() ) {
            Enum x = (Enum) m_left.get(t);
            String y = m_right.toString();
            return x.name().equals(y);
        }
        
        if ( TypeLib.isNumericType(lType) && TypeLib.isNumericType(rType) ) {
            Class type = TypeLib.getNumericType(lType, rType);
            if ( type == int.class || type == byte.class ) {
                int x = m_left.getInt(t);
                int y = m_right.getInt(t);
                c = ((LiteralComparator)m_cmp).compare(x,y);
            } else if ( type == long.class ) {
                long x = m_left.getLong(t);
                long y = m_right.getLong(t);
                c = ((LiteralComparator)m_cmp).compare(x,y);
            } else if ( type == float.class ) {
                float x = m_left.getFloat(t);
                float y = m_right.getFloat(t);
                c = ((LiteralComparator)m_cmp).compare(x,y);
            } else if ( type == double.class ) {
                double x = m_left.getDouble(t);
                double y = m_right.getDouble(t);
                c = ((LiteralComparator)m_cmp).compare(x,y);
            } else {
                throw new IllegalStateException();
            }
        } else {
            try {
                c = m_cmp.compare(m_left.get(t), m_right.get(t));
            } catch( Exception ex ) {
                System.out.println("prefuse error 0x33" + ex);
            }
        }
       
        switch ( m_op ) {
        case LT:
            return ( c == -1 );
        case GT:
            return ( c == 1 );
        case EQ:
            return ( c == 0 );
        case NEQ:
            return ( c != 0 );
        case LTEQ:
            return ( c <= 0 );
        case GTEQ:
            return ( c >= 0 );
        default:
            throw new IllegalStateException("Unknown operation.");
        }
    }

    /**
     * @see prefuse.data.expression.Expression#get(prefuse.data.Tuple)
     */
    public Object get(Tuple t) {
        return ( getBoolean(t) ? Boolean.TRUE : Boolean.FALSE );
    }
    
    /**
     * @see java.lang.Object#toString()
     */
    public String toString() {
        String op = "?";
        switch ( m_op ) {
        case LT:
            op = "<";
            break;
        case GT:
            op = ">";
            break;
        case EQ:
            op = "=";
            break;
        case NEQ:
            op = "!=";
            break;
        case LTEQ:
            op = "<=";
            break;
        case GTEQ:
            op = ">=";
            break;
        }
        return m_left.toString()+' '+op+' '+m_right.toString();
    }
    
} // end of class BinaryPredicate
