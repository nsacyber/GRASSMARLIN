package prefuse.data.expression;

import prefuse.data.Tuple;
import prefuse.util.TypeLib;

/**
 * Abstarct base class for a Literal Expression that evaluates to a
 * constant value.
 * 
 * @author <a href="http://jheer.org">jeffrey heer</a>
 */
public abstract class Literal extends AbstractExpression {

    /**
     * Evaluate the given tuple and data field and return the
     * result as a new Literal instance.
     * @param t the Tuple
     * @param field the data field to lookup
     * @return a new Literal expression containing the
     * value of the Tuple's data field
     */
    public static Literal getLiteral(Tuple t, String field) {
        Class type = t.getColumnType(field);
        if ( type == int.class )
        {
            return new NumericLiteral(t.getInt(field));
        }
        else if ( type == long.class )
        {
            return new NumericLiteral(t.getLong(field));
        }
        else if ( type == float.class )
        {
            return new NumericLiteral(t.getFloat(field));
        }
        else if ( type == double.class )
        {
            return new NumericLiteral(t.getDouble(field));
        }
        else if ( type == boolean.class )
        {
            return new BooleanLiteral(t.getBoolean(field));
        }
        else
        {
            return new ObjectLiteral(t.get(field));
        }
    }
    
    /**
     * Return the given object as a new Literal instance.
     * @param val the object value
     * @return a new Literal expression containing the
     * object value. The type is assumed to be the
     * value's concrete runtime type.
     */
    public static Literal getLiteral(Object val) {
        return getLiteral(val, val.getClass());
    }
    
    /**
     * Return the given object as a new Literal instance.
     * @param val the object value
     * @param type the type the literal should take
     * @return a new Literal expression containing the
     * object value
     */
    public static Literal getLiteral(Object val, Class type) {
        if ( TypeLib.isNumericType(type) )
        {
            return new NumericLiteral(val);
        }
        else if ( type == boolean.class )
        {
            return new BooleanLiteral(((Boolean)val).booleanValue());
        }
        else
        {
            if ( type.isInstance(val) ) {
                return new ObjectLiteral(val);
            } else {
                throw new IllegalArgumentException("Object does "
                        + "not match the provided Class type.");
            }
        }
    }
    
} // end of abstarct class Literal
