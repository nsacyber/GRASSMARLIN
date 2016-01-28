package prefuse.data.expression;

/**
 * A Predicate is a special type of Expression that carries the guarantee
 * that the {@link prefuse.data.expression.Expression#getBoolean(Tuple)}
 * method is supported. Predicates are particularly useful for issuing
 * queries to prefuse data structures. To create a Predicate, one can
 * either instantiate the desired Predicate instances directly, or
 * write a parseable textual expression. The documentation for the
 * {@link prefuse.data.expression.parser.ExpressionParser} class includes
 * a full reference for prefuse's textual expression language.
 * 
 * @author <a href="http://jheer.org">jeffrey heer</a>
 */
public interface Predicate extends Expression {
    
} // end of interface Predicate
