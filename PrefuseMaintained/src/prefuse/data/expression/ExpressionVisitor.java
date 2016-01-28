package prefuse.data.expression;

/**
 * Visitor interface for objects that visit each sub-expression of an
 * Expression instance, performing some computation or data collection.
 * 
 * @author <a href="http://jheer.org">jeffrey heer</a>
 */
public interface ExpressionVisitor {

    /**
     * Callback made when visiting a particular expression.
     * @param expr the expression currently being visited.
     */
    public void visitExpression(Expression expr);
    
    /**
     * Callback to signal that the visitor has just descended
     * a level in the Expression tree.
     */
    public void down();
    
    /**
     * Callback to signal that the visitor has just ascended
     * a level in the Expression tree.
     */
    public void up();
    
} // end of interface ExpressionVisitor
