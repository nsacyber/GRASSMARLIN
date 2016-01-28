package prefuse.data.expression;

import java.util.Comparator;

import prefuse.data.Schema;
import prefuse.data.Tuple;
import prefuse.util.TypeLib;
import prefuse.util.collections.DefaultLiteralComparator;
import prefuse.util.collections.LiteralComparator;

/**
 * Predicate instance that evaluates if a value is contained within
 * a bounded range.
 * 
 * @author <a href="http://jheer.org">jeffrey heer</a>
 */
public class RangePredicate extends BinaryExpression implements Predicate {

    /** Indicates the both the left and right bounds are inclusive */
    public static final int IN_IN = 0;
    /** Indicates an inclusive left bound and exclusive right bound */
    public static final int IN_EX = 1;
    /** Indicates an exclusive left bound and inclusive right bound */
    public static final int EX_IN = 2;
    /** Indicates the both the left and right bounds are exclusive */
    public static final int EX_EX = 3;
    
    private Expression m_middle;
    private Comparator m_cmp;
    
    // ------------------------------------------------------------------------
    // Constructors
    
    /**
     * Create a new RangePredicate. Both bounds are assumed to be inclusive.
     * @param middle the value to test for membership in the range
     * @param left the lower range bound
     * @param right the upper range bound
     */
    public RangePredicate(Expression middle, 
            Expression left, Expression right)
    {
        this(IN_IN, middle, left, right,
             DefaultLiteralComparator.getInstance());
    }
    
    /**
     * Create a new RangePredicate. Both bounds are assumed to be inclusive.
     * @param middle the value to test for membership in the range
     * @param left the lower range bound
     * @param right the upper range bound
     * @param cmp the comparator to use for comparing data values
     */
    public RangePredicate(Expression middle, 
            Expression left, Expression right, Comparator cmp)
    {
        this(IN_IN, middle, left, right, cmp);
    }
    
    /**
     * Create a new RangePredicate.
     * @param operation operation code indicating the inclusiveness /
     * exclusiveness of the bounds
     * @param middle the value to test for membership in the range
     * @param left the lower range bound
     * @param right the upper range bound
     */
    public RangePredicate(int operation, Expression middle, 
            Expression left, Expression right)
    {
        this(operation, middle, left, right,
             DefaultLiteralComparator.getInstance());
    }
    
    /**
     * Create a new RangePredicate.
     * @param operation operation code indicating the inclusiveness /
     * exclusiveness of the bounds
     * @param middle the value to test for membership in the range
     * @param left the lower range bound
     * @param right the upper range bound
     * @param cmp the comparator to use for comparing data values
     */
    public RangePredicate(int operation, Expression middle, 
            Expression left, Expression right, Comparator cmp)
    {
        super(operation, IN_IN, EX_EX, left, right);
        
        this.m_middle = middle;
        this.m_cmp = cmp;
    }
    
    // ------------------------------------------------------------------------
    // Accessors
    
    /**
     * Get the middle expression being tested for inclusion in the range
     * @return the middle expression
     */
    public Expression getMiddleExpression() {
        return m_middle;
    }
    
    /**
     * Get the comparator used to compare data values.
     * @return the comparator used to compare data values
     */
    public Comparator getComparator() {
        return m_cmp;
    }
    
    // ------------------------------------------------------------------------
    // Expression Interface
    
    /**
     * @see prefuse.data.expression.Expression#getBoolean(prefuse.data.Tuple)
     */
    public boolean getBoolean(Tuple t) {
        Class lType = m_left.getType(t.getSchema());
        Class rType = m_right.getType(t.getSchema());
        Class mType = m_middle.getType(t.getSchema());
        Class sType = null;
        
        // see if we can match the end-points' type
        if ( lType.isAssignableFrom(rType) ) {
            sType = lType;
        } else if ( rType.isAssignableFrom(lType) ) {
            sType = rType;
        }
        
        int c1, c2 = 0;
        if ( sType != null && TypeLib.isNumericType(sType) && 
                TypeLib.isNumericType(mType) )
        {
            // the range is of numeric types
            Class type = TypeLib.getNumericType(sType, mType);
            if ( type == int.class ) {
                int lo = m_left.getInt(t);
                int hi = m_right.getInt(t);
                int x  = m_middle.getInt(t);
                c1 = ((LiteralComparator)m_cmp).compare(x,lo);
                c2 = ((LiteralComparator)m_cmp).compare(x,hi);
            } else if ( type == long.class ) {
                long lo = m_left.getLong(t);
                long hi = m_right.getLong(t);
                long x  = m_middle.getLong(t);
                c1 = ((LiteralComparator)m_cmp).compare(x,lo);
                c2 = ((LiteralComparator)m_cmp).compare(x,hi);
            } else if ( type == float.class ) {
                float lo = m_left.getFloat(t);
                float hi = m_right.getFloat(t);
                float x  = m_middle.getFloat(t);
                c1 = ((LiteralComparator)m_cmp).compare(x,lo);
                c2 = ((LiteralComparator)m_cmp).compare(x,hi);
            } else if ( type == double.class ) {
                double lo = m_left.getDouble(t);
                double hi = m_right.getDouble(t);
                double x  = m_middle.getDouble(t);
                c1 = ((LiteralComparator)m_cmp).compare(x,lo);
                c2 = ((LiteralComparator)m_cmp).compare(x,hi);
            } else {
                throw new IllegalStateException();
            }
        } else {
            Object lo = m_left.get(t);
            Object hi = m_right.get(t);
            Object x  = m_middle.get(t);
            c1 = m_cmp.compare(x, lo);
            c2 = m_cmp.compare(x, hi);
        }
       
        // check the comparison values to see if it is in-range
        switch ( m_op ) {
        case IN_IN:
            return ( c1 >= 0 && c2 <= 0 );
        case IN_EX:
            return ( c1 >= 0 && c2 < 0 );
        case EX_IN:
            return ( c1 > 0 && c2 <= 0 );
        case EX_EX:
            return ( c1 > 0 && c2 < 0 );
        default:
            throw new IllegalStateException("Unknown operation.");
        }
    }
    
    /**
     * @see prefuse.data.expression.Expression#getType(prefuse.data.Schema)
     */
    public Class getType(Schema s) {
        return boolean.class;
    }

    /**
     * @see prefuse.data.expression.Expression#get(prefuse.data.Tuple)
     */
    public Object get(Tuple t) {
        return ( getBoolean(t) ? Boolean.TRUE : Boolean.FALSE );
    }
    
    /**
     * @see prefuse.data.expression.Expression#visit(prefuse.data.expression.ExpressionVisitor)
     */
    public void visit(ExpressionVisitor v) {
        v.visitExpression(this);
        v.down(); m_left.visit(v);   v.up();
        v.down(); m_middle.visit(v); v.up();
        v.down(); m_right.visit(v);  v.up();
    }
    
    /**
     * @see prefuse.data.expression.AbstractExpression#addChildListeners()
     */
    protected void addChildListeners() {
        super.addChildListeners();
        m_middle.addExpressionListener(this);
    }
    
    /**
     * @see prefuse.data.expression.AbstractExpression#removeChildListeners()
     */
    protected void removeChildListeners() {
        super.removeChildListeners();
        m_middle.removeExpressionListener(this);
    }
    
    /**
     * @see java.lang.Object#toString()
     */
    public String toString() {
        String lop = "?", rop = "?";
        switch ( m_op ) {
        case IN_IN:
            lop = rop = "<=";
            break;
        case IN_EX:
            lop = "<="; rop = "<";
            break;
        case EX_IN:
            lop = "<"; rop = "<=";
            break;
        case EX_EX:
            lop = rop = "<";
            break;
        }
        return '('+m_left.toString()+' '+lop+' '+m_middle.toString()+" AND "+ 
                   m_middle.toString()+' '+rop+' '+m_right.toString()+')';
    }
    
} // end of class RangePredicate
