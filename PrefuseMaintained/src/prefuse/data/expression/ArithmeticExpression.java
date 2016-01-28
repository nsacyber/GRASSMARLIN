package prefuse.data.expression;

import prefuse.data.Schema;
import prefuse.data.Tuple;
import prefuse.util.TypeLib;

/**
 * Expression supporting basic arithmetic: add, subtract, multiply,
 * divide, exponentiate (pow), and modulo (%).
 * 
 * @author <a href="http://jheer.org">jeffrey heer</a>
 */
public class ArithmeticExpression extends BinaryExpression {

    /** Indicates an addition operation. */
    public static final int ADD = 0;
    /** Indicates a subtraction operation. */
    public static final int SUB = 1;
    /** Indicates a multiplication operation. */
    public static final int MUL = 2;
    /** Indicates a division operation. */
    public static final int DIV = 3;
    /** Indicates an exponentiation (pow) operation. */
    public static final int POW = 4;
    /** Indicates a modulo operation. */
    public static final int MOD = 5;

    private Class m_type;    
    
    /**
     * Create a new ArithmeticExpression.
     * @param operation the operation to perform
     * @param left the left sub-expression
     * @param right the right sub-expression
     */
    public ArithmeticExpression(int operation, 
            Expression left, Expression right)
    {
        super(operation, ADD, MOD, left, right);
        m_type = null;
    }
    
    /**
     * @see prefuse.data.expression.Expression#getType(prefuse.data.Schema)
     */
    public Class getType(Schema s) {
        if ( m_type == null ) {
            Class lType = m_left.getType(s);
            Class rType = m_right.getType(s);
        
            // determine this class's type
            m_type = TypeLib.getNumericType(lType, rType);
        }
        return m_type;
    }

    /**
     * @see prefuse.data.expression.Expression#get(prefuse.data.Tuple)
     */
    public Object get(Tuple t) {
        Class type = getType(t.getSchema());
        if ( int.class == type || byte.class == type ) {
            return new Integer(getInt(t));
        } else if ( long.class == type ) {
            return new Long(getInt(t));
        } else if ( float.class == type ) {
            return new Float(getFloat(t));
        } else if ( double.class == type ) {
            return new Double(getDouble(t));
        } else {
            throw new IllegalStateException();
        }
        
    }

    /**
     * @see prefuse.data.expression.Expression#getInt(prefuse.data.Tuple)
     */
    public int getInt(Tuple t) {
        int x = m_left.getInt(t);
        int y = m_right.getInt(t);
        
        // compute return value
        switch ( m_op ) {
        case ADD:
            return x+y;
        case SUB:
            return x-y;
        case MUL:
            return x*y;
        case DIV:
            return x/y;
        case POW:
            return (int)Math.pow(x,y);
        case MOD:
            return x%y;
        }
        throw new IllegalStateException("Unknown operation type.");
    }

    /**
     * @see prefuse.data.expression.Expression#getLong(prefuse.data.Tuple)
     */
    public long getLong(Tuple t) {
        long x = m_left.getLong(t);
        long y = m_right.getLong(t);
        
        // compute return value
        switch ( m_op ) {
        case ADD:
            return x+y;
        case SUB:
            return x-y;
        case MUL:
            return x*y;
        case DIV:
            return x/y;
        case POW:
            return (long)Math.pow(x,y);
        case MOD:
            return x%y;
        }
        throw new IllegalStateException("Unknown operation type.");
    }

    /**
     * @see prefuse.data.expression.Expression#getFloat(prefuse.data.Tuple)
     */
    public float getFloat(Tuple t) {
        float x = m_left.getFloat(t);
        float y = m_right.getFloat(t);
        
        // compute return value
        switch ( m_op ) {
        case ADD:
            return x+y;
        case SUB:
            return x-y;
        case MUL:
            return x*y;
        case DIV:
            return x/y;
        case POW:
            return (float)Math.pow(x,y);
        case MOD:
            return (float)Math.IEEEremainder(x,y);
        }
        throw new IllegalStateException("Unknown operation type.");
    }

    /**
     * @see prefuse.data.expression.Expression#getDouble(prefuse.data.Tuple)
     */
    public double getDouble(Tuple t) {
        double x = m_left.getDouble(t);
        double y = m_right.getDouble(t);
        
        // compute return value
        switch ( m_op ) {
        case ADD:
            return x+y;
        case SUB:
            return x-y;
        case MUL:
            return x*y;
        case DIV:
            return x/y;
        case POW:
            return Math.pow(x,y);
        case MOD:
            return Math.IEEEremainder(x,y);
        }
        throw new IllegalStateException("Unknown operation type.");
    }

    /**
     * @see java.lang.Object#toString()
     */
    public String toString() {
        char op = '?';
        switch ( m_op ) {
        case ADD:
            op = '+';
            break;
        case SUB:
            op = '-';
            break;
        case MUL:
            op = '*';
            break;
        case DIV:
            op = '/';
            break;
        case POW:
            op = '^';
            break;
        case MOD:
            op = '%';
            break;
        }
        return '('+m_left.toString()+' '+op+' '+m_right.toString()+')';
    }
    
} // end of class ArithmeticExpression
