package prefuse.visual.expression;

import prefuse.data.Schema;
import prefuse.data.Tuple;
import prefuse.data.expression.Predicate;
import prefuse.visual.VisualItem;

/**
 * Expression that indicates if an item is currently a member of a particular
 * data group. The data group name is provided by a String-valued
 * sub-expression.
 * 
 * @author <a href="http://jheer.org">jeffrey heer</a>
 */
public class InGroupPredicate extends GroupExpression implements Predicate {
    
    /**
     * Create a new InGroupPredicate.
     */
    public InGroupPredicate() {
    }
    
    /**
     * Create a new InGroupPredicate.
     * @param group @param group the data group name to use as a parameter
     */
    public InGroupPredicate(String group) {
        super(group);
    }
    
    /**
     * @see prefuse.data.expression.Expression#get(prefuse.data.Tuple)
     */
    public Object get(Tuple t) {
        return getBoolean(t) ? Boolean.TRUE : Boolean.FALSE;
    }
    
    /**
     * @see prefuse.data.expression.Expression#getBoolean(prefuse.data.Tuple)
     */
    public boolean getBoolean(Tuple t) {
        if ( !(t instanceof VisualItem) )
            return false;
        
        String group = getGroup(t);
        if ( group == null ) {
            return false;
        }
        VisualItem item = (VisualItem)t;
        return item.getVisualization().isInGroup(item, group);
    }

    /**
     * @see prefuse.data.expression.Function#getName()
     */
    public String getName() {
        return "INGROUP";
    }

    /**
     * @see prefuse.data.expression.Expression#getType(prefuse.data.Schema)
     */
    public Class getType(Schema s) {
        return boolean.class;
    }
    
} // end of class InGroupPredicate
