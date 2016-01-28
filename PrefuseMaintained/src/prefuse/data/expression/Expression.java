package prefuse.data.expression;

import prefuse.data.Schema;
import prefuse.data.Tuple;
import prefuse.data.event.ExpressionListener;

/**
 * <p>An Expression is an arbitrary function that takes a single Tuple as an
 * argument. Expressions support both Object-valued and primitive-valued
 * (int, long, float, double, boolean) evaluation methods. The appropriate
 * method to call depends on the particular Expression implementation.
 * A {@link #getType(Schema)} method provides mechanism for determining the
 * return type of a given Expression instance. A {@link Predicate} is an
 * Expression which is guaranteed to support the {@link #getBoolean(Tuple)}
 * method, is often used to filter tuples.</p>
 * 
 * <p>Expressions also support a listener interface, allowing clients to
 * monitor changes to expressions, namely rearrangements or modification
 * of contained sub-expressions. The Expression interface also supports
 * visitors, which can be used to visit every sub-expression in an expression
 * tree.</p>
 * 
 * <p>Using the various Expression implementations in the
 * {@link prefuse.data.expression.Expression} package, clients can
 * programatically construct a tree of expressions for use as complex
 * query predicates or as functions for computing a derived data column
 * (see {@link prefuse.data.Table#addColumn(String, Expression)}. Often it is
 * more convenient to write expressions in the prefuse expression language,
 * a SQL-like data manipulation language, and compile the Expression tree
 * using the {@link prefuse.data.expression.parser.ExpressionParser}. The
 * documentation for the ExpressionParser class includes a full reference
 * for the textual expression language.</p>
 * 
 * @author <a href="http://jheer.org">jeffrey heer</a>
 */
public interface Expression {

    /**
     * Returns the type that this expression evaluates to when tuples
     * with the given Schema are provided as input.
     */
    public Class getType(Schema s);
    
    /**
     * Passes the visitor through this expression and any sub expressions
     * @param v the ExpressionVisitor
     */
    public void visit(ExpressionVisitor v);
    
    /**
     * Evaluate the Expression on the given input Tuple.
     * @param t the input Tuple
     * @return the Expression return value, as an Object
     */
    public Object get(Tuple t);
    
    /**
     * Evaluate the Expression on the given input Tuple.
     * @param t the input Tuple
     * @return the Expression return value, as an int
     */
    public int getInt(Tuple t);
    
    /**
     * Evaluate the Expression on the given input Tuple.
     * @param t the input Tuple
     * @return the Expression return value, as a long
     */
    public long getLong(Tuple t);
    
    /**
     * Evaluate the Expression on the given input Tuple.
     * @param t the input Tuple
     * @return the Expression return value, as a float
     */
    public float getFloat(Tuple t);

    /**
     * Evaluate the Expression on the given input Tuple.
     * @param t the input Tuple
     * @return the Expression return value, as a double
     */
    public double getDouble(Tuple t);

    /**
     * Evaluate the Expression on the given input Tuple.
     * @param t the input Tuple
     * @return the Expression return value, as a boolean
     */
    public boolean getBoolean(Tuple t);
    
    /**
     * Add a listener to this Expression.
     * @param lstnr the expression listener to add
     */
    public void addExpressionListener(ExpressionListener lstnr);
    
    /**
     * Remove a listener to this Expression.
     * @param lstnr the expression listener to remove
     */
    public void removeExpressionListener(ExpressionListener lstnr);
    
} // end of interface Expression
