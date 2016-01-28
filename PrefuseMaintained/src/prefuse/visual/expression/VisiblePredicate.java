package prefuse.visual.expression;

import prefuse.data.expression.ColumnExpression;
import prefuse.data.expression.Expression;
import prefuse.data.expression.Function;
import prefuse.data.expression.NotPredicate;
import prefuse.data.expression.Predicate;
import prefuse.visual.VisualItem;

/**
 * Expression that indicates if an item's visible flag is set.
 * 
 * @author <a href="http://jheer.org">jeffrey heer</a>
 */
public class VisiblePredicate extends ColumnExpression
    implements Predicate, Function
{

    /** Convenience instance for the visible == true case. */
    public static final Predicate TRUE = new VisiblePredicate();
    /** Convenience instance for the visible == false case. */
    public static final Predicate FALSE = new NotPredicate(TRUE);
    
    /**
     * Create a new VisiblePredicate.
     */
    public VisiblePredicate() {
        super(VisualItem.VISIBLE);
    }

    /**
     * @see prefuse.data.expression.Function#getName()
     */
    public String getName() {
        return "VISIBLE";
    }

    /**
     * @see prefuse.data.expression.Function#addParameter(prefuse.data.expression.Expression)
     */
    public void addParameter(Expression e) {
        throw new IllegalStateException("This function takes 0 parameters");
    }

    /**
     * @see prefuse.data.expression.Function#getParameterCount()
     */
    public int getParameterCount() {
        return 0;
    }
    
    /**
     * @see java.lang.Object#toString()
     */
    public String toString() {
        return getName()+"()";
    }

} // end of class VisiblePredicate
