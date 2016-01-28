package prefuse.data.event;

import java.util.EventListener;

import prefuse.data.expression.Expression;

/**
 * Listener interface for monitoring changes to an Expression instance.
 * 
 * @author <a href="http://jheer.org">jeffrey heer</a>
 */
public interface ExpressionListener extends EventListener {

    /**
     * Notification that an Expression instance has been modified in some way.
     * @param expr the modified expression
     */
    public void expressionChanged(Expression expr);
    
} // end of interface ExpressionListener
