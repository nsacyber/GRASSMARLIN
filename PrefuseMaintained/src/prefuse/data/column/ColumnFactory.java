package prefuse.data.column;

import java.util.Date;

import prefuse.data.DataTypeException;
import prefuse.data.Table;
import prefuse.data.expression.Expression;

/**
 * Factory class for generating appropriate column instances. Used by
 * Tables to generate their columns.
 * 
 * @author <a href="http://jheer.org">jeffrey heer</a>
 */
public class ColumnFactory {
    
    /**
     * Get a new column of the given type.
     * @param type the column data type
     * @return the new column
     */
    public static final Column getColumn(Class type) {
        return getColumn(type, 0, 0, null);
    }

    /**
     * Get a new column of the given type.
     * @param type the column data type
     * @param nrows the number of rows to include in the column
     * @return the new column
     */
    public static final Column getColumn(Class type, int nrows) {
        return getColumn(type, nrows, nrows, null);
    }

    /**
     * Get a new column of the given type.
     * @param type the column data type
     * @param nrows the number of rows to include in the column
     * @param defaultValue the default value for the column
     * @return the new column
     */
    public static final Column getColumn(Class type, int nrows, 
                                         Object defaultValue)
    {
        return getColumn(type, nrows, nrows, defaultValue);
    }
    
    /**
     * Get a new column of the given type.
     * @param type the column data type
     * @param nrows the number of rows to include in the column
     * @param nnz the number of expected non-zero entries (NOTE: currently
     * this value is not being used)
     * @param defaultValue the default value for the column
     * @return the new column
     */
    public static final Column getColumn(Class type, int nrows, int nnz,
                                         Object defaultValue)
    {
        if ( type == byte.class )
        {
            if ( defaultValue == null ) {
                return new ByteColumn(nrows);
            } else {
                byte def = ((Number)defaultValue).byteValue();
                return new ByteColumn(nrows, nrows, def);
            }
        }
        if ( type == int.class )
        {
            if ( defaultValue == null ) {
                return new IntColumn(nrows);
            } else {
                int def = ((Number)defaultValue).intValue();
                return new IntColumn(nrows, nrows, def);
            }
        }
        else if ( type == long.class )
        {
            if ( defaultValue == null ) {
                return new LongColumn(nrows);
            } else {
                long def = ((Number)defaultValue).longValue();
                return new LongColumn(nrows, nrows, def);
            }
        }
        else if ( type == float.class )
        {
            if ( defaultValue == null ) {
                return new FloatColumn(nrows);
            } else {
                float def = ((Number)defaultValue).floatValue();
                return new FloatColumn(nrows, nrows, def);
            }
        }
        else if ( type == double.class )
        {
            if ( defaultValue == null ) {
                return new DoubleColumn(nrows);
            } else {
                double def = ((Number)defaultValue).doubleValue();
                return new DoubleColumn(nrows, nrows, def);
            }
        }
        else if ( type == boolean.class )
        {
            if ( defaultValue == null ) {
                return new BooleanColumn(nrows);
            } else {
                boolean def = ((Boolean)defaultValue).booleanValue();
                return new BooleanColumn(nrows, nrows, def);
            }
        }
        else if ( Date.class.isAssignableFrom(type) )
        {
            if ( defaultValue == null ) {
                return new DateColumn(type, nrows);
            } else {
                Date d = ((Date)defaultValue);
                return new DateColumn(type, nrows, nrows, d.getTime());
            }
        }
        else if ( type == byte.class 
                    || type == short.class 
                    || type == char.class 
                    || type == void.class )
        {
            throw new DataTypeException(type);
        }
        else
        {
            return new ObjectColumn(type, nrows, nrows, defaultValue);
        }
    }
    
    /**
     * Get a new column based on the given expression.
     * @param t the table the column should be added to
     * @param expr the expression that should provide the column values
     * @return the new column
     */
    public static final Column getColumn(Table t, Expression expr) {
        return new ExpressionColumn(t, expr);
    }
    
    /**
     * Get a new column of a constant value.
     * @param type the column data type
     * @param dflt the default constant value for the column
     * @return the new column
     */
    public static final Column getConstantColumn(Class type, Object dflt) {
        return new ConstantColumn(type, dflt);
    }
    
} // end of class ColumnFactory
