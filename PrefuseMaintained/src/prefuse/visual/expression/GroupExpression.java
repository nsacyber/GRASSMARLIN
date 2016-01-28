package prefuse.visual.expression;

import java.util.logging.Logger;

import prefuse.data.Tuple;
import prefuse.data.expression.AbstractExpression;
import prefuse.data.expression.Expression;
import prefuse.data.expression.Function;
import prefuse.data.expression.ObjectLiteral;

/**
 * Abstract base class for Expression instances dealing with data groups
 * within a Visualization. Maintains an Expression that serves as the
 * paremter to this Function; this Expression should return a valid
 * group name when evaluated on a given Tuple.
 * 
 * @author <a href="http://jheer.org">jeffrey heer</a>
 */
public abstract class GroupExpression extends AbstractExpression
    implements Function
{
    private static final Logger s_logger
        = Logger.getLogger(GroupExpression.class.getName());

    protected Expression m_group;
    
    /**
     * Create a new GroupExpression.
     */
    protected GroupExpression() {
        m_group = null;
    }
    
    /**
     * Create a new GroupExpression over the given group name.
     * @param group the data group name
     */
    protected GroupExpression(String group) {
        m_group = new ObjectLiteral(group);
    }
    
    /**
     * Evaluate the group name expression for the given Tuple
     * @param t the input Tuple to the group name expression
     * @return the String result of the expression
     */
    protected String getGroup(Tuple t) {
        String group = (String)m_group.get(t);
        if ( group == null ) {
            s_logger.warning("Null group lookup");
        }
        return group;
    }
    
    
    /**
     * Attempts to add the given expression as the group expression.
     * @see prefuse.data.expression.Function#addParameter(prefuse.data.expression.Expression)
     */
    public void addParameter(Expression e) {
        if ( m_group == null )
            m_group = e;
        else
            throw new IllegalStateException(
               "This function takes only 1 parameter.");
    }

    /**
     * @see prefuse.data.expression.Function#getParameterCount()
     */
    public int getParameterCount() {
        return 1;
    }
    
    /**
     * @see java.lang.Object#toString()
     */
    public String toString() {
        return getName()+"("+m_group+")";
    }
    
} // end of class GroupExpression
