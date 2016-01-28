package prefuse.data.expression;

import prefuse.data.Schema;
import prefuse.data.Tuple;
import prefuse.util.TypeLib;

/**
 * Literal expression of a numeric value. 
 * 
 * @author <a href="http://jheer.org">jeffrey heer</a>
 */
public class NumericLiteral extends Literal {

    private final Number m_number;
    private final Class  m_type;
    
    // ------------------------------------------------------------------------
    // Constructors
    
    /**
     * Create a new integer NumericLiteral.
     * @param x the literal numeric value
     */
    public NumericLiteral(int x) {
        m_number = new Integer(x);
        m_type = int.class;
    }

    /**
     * Create a new long NumericLiteral.
     * @param x the literal numeric value
     */
    public NumericLiteral(long x) {
        m_number = new Long(x);
        m_type = long.class;
    }
    
    /**
     * Create a new float NumericLiteral.
     * @param x the literal numeric value
     */
    public NumericLiteral(float x) {
        m_number = new Float(x);
        m_type = float.class;
    }
    
    /**
     * Create a new double NumericLiteral.
     * @param x the literal numeric value
     */
    public NumericLiteral(double x) {
        m_number = new Double(x);
        m_type = double.class;
    }
    
    /**
     * Create a new NumericLiteral.
     * @param x the literal numeric value, must be an instance of 
     * {@link java.lang.Number}, otherwise an exception will be thrown.
     */
    public NumericLiteral(Object x) {
        if ( x instanceof Number ) {
            m_number = (Number)x;
            m_type = TypeLib.getPrimitiveType(m_number.getClass());
        } else {
            throw new IllegalArgumentException("Invalid type!");
        }
    }

    // ------------------------------------------------------------------------
    // Expression Interface    
    
    /**
     * @see prefuse.data.expression.Expression#getType(prefuse.data.Schema)
     */
    public Class getType(Schema s) {
        return m_type;
    }

    /**
     * @see prefuse.data.expression.Expression#get(prefuse.data.Tuple)
     */
    public Object get(Tuple t) {
        return m_number;
    }

    /**
     * @see prefuse.data.expression.Expression#getInt(prefuse.data.Tuple)
     */
    public int getInt(Tuple t) {
        return m_number.intValue();
    }

    /**
     * @see prefuse.data.expression.Expression#getLong(prefuse.data.Tuple)
     */
    public long getLong(Tuple t) {
        return m_number.longValue();
    }

    /**
     * @see prefuse.data.expression.Expression#getFloat(prefuse.data.Tuple)
     */
    public float getFloat(Tuple t) {
        return m_number.floatValue();
    }

    /**
     * @see prefuse.data.expression.Expression#getDouble(prefuse.data.Tuple)
     */
    public double getDouble(Tuple t) {
        return m_number.doubleValue();
    }

    /**
     * @see java.lang.Object#toString()
     */
    public String toString() {
        return m_number.toString();
    }
    
} // end of class NumericLiteral
