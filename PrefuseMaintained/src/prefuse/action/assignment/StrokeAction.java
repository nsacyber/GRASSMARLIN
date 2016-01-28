package prefuse.action.assignment;

import java.awt.BasicStroke;
import java.awt.Stroke;
import java.util.logging.Logger;

import prefuse.action.EncoderAction;
import prefuse.data.expression.Predicate;
import prefuse.data.expression.parser.ExpressionParser;
import prefuse.util.StrokeLib;
import prefuse.visual.VisualItem;


/**
 * <p>Assignment Action that assigns <code>Stroke</code> values to VisualItems.
 * The Stroke instance determines how lines and shape outlines are drawn,
 * including the base size of the line, the line endings and line join types,
 * and whether the line is solid or dashed. By default, a StrokeAction simply
 * sets each VisualItem to use a default 1-pixel wide solid line. Clients can
 * change this default value to achieve uniform Stroke assignment, or can add
 * any number of additional rules for Stroke assignment.
 * Rules are specified by a Predicate instance which, if returning true, will
 * trigger that rule, causing either the provided Stroke value or the result of
 * a delegate StrokeAction to be applied. Rules are evaluated in the order in
 * which they are added to the StrokeAction, so earlier rules will have
 * precedence over rules added later.
 * </p>
 * 
 * <p>In addition, subclasses can simply override
 * {@link #getStroke(VisualItem)} to achieve custom Stroke assignment. In some
 * cases, this may be the simplest or most flexible approach.</p>
 * 
 * @author <a href="http://jheer.org">jeffrey heer</a>
 */
public class StrokeAction extends EncoderAction {

    protected BasicStroke defaultStroke = StrokeLib.getStroke(1.0f);
    
    /**
     * Create a new StrokeAction that processes all data groups.
     */
    public StrokeAction() {
        super();
    }
    
    /**
     * Create a new StrokeAction that processes the specified group.
     * @param group the data group to process
     */
    public StrokeAction(String group) {
        super(group);
    }
    
    /**
     * Create a new StrokeAction that processes the specified group.
     * @param group the data group to process
     * @param defaultStroke the default Stroke to assign
     */
    public StrokeAction(String group, BasicStroke defaultStroke) {
        super(group);
        this.defaultStroke = defaultStroke;
    }
    
    // ------------------------------------------------------------------------
    
    /**
     * Set the default BasicStroke to be assigned to items. Items will be
     * assigned the default Stroke if they do not match any registered rules.
     * @param f the default BasicStroke to use
     */
    public void setDefaultStroke(BasicStroke f) {
        defaultStroke = f;
    }
    
    /**
     * Get the default BasicStroke assigned to items.
     * @return the default BasicStroke
     */
    public BasicStroke getDefaultStroke() {
        return defaultStroke;
    }
    
    /**
     * Add a mapping rule to this StrokeAction. VisualItems that match
     * the provided predicate will be assigned the given BasicStroke value
     * (assuming they do not match an earlier rule).
     * @param p the rule Predicate 
     * @param stroke the BasicStroke
     */
    public void add(Predicate p, BasicStroke stroke) {
        super.add(p, stroke);
    }

    /**
     * Add a mapping rule to this StrokeAction. VisualItems that match
     * the provided expression will be assigned the given BasicStroke value
     * (assuming they do not match an earlier rule). The provided expression
     * String will be parsed to generate the needed rule Predicate.
     * @param expr the expression String, should parse to a Predicate. 
     * @param stroke the BasicStroke
     * @throws RuntimeException if the expression does not parse correctly or
     * does not result in a Predicate instance.
     */
    public void add(String expr, BasicStroke stroke) {
        Predicate p = (Predicate)ExpressionParser.parse(expr);
        add(p, stroke);       
    }
    
    /**
     * Add a mapping rule to this StrokeAction. VisualItems that match
     * the provided predicate will be assigned the BasicStroke value returned
     * by the given StrokeAction's getStroke() method.
     * @param p the rule Predicate 
     * @param f the delegate StrokeAction to use
     */
    public void add(Predicate p, StrokeAction f) {
        super.add(p, f);
    }

    /**
     * Add a mapping rule to this StrokeAction. VisualItems that match
     * the provided expression will be assigned the given BasicStroke value
     * (assuming they do not match an earlier rule). The provided expression
     * String will be parsed to generate the needed rule Predicate.
     * @param expr the expression String, should parse to a Predicate. 
     * @param f the delegate StrokeAction to use
     * @throws RuntimeException if the expression does not parse correctly or
     * does not result in a Predicate instance.
     */
    public void add(String expr, StrokeAction f) {
        Predicate p = (Predicate)ExpressionParser.parse(expr);
        super.add(p, f);
    }
    
    // ------------------------------------------------------------------------
    
    /**
     * @see prefuse.action.ItemAction#process(prefuse.visual.VisualItem, double)
     */
    public void process(VisualItem item, double frac) {
        item.setStroke(getStroke(item));
    }
    
    /**
     * Returns the stroke to use for a given VisualItem. Subclasses should
     * override this method to perform customized Stroke assignment.
     * @param item the VisualItem for which to get the Stroke
     * @return the BasicStroke for the given item
     */
    public BasicStroke getStroke(VisualItem item) {
        Object o = lookup(item);
        if ( o != null ) {
            if ( o instanceof StrokeAction ) {
                return ((StrokeAction)o).getStroke(item);
            } else if ( o instanceof Stroke ) {
                return (BasicStroke)o;
            } else {
                Logger.getLogger(this.getClass().getName())
                    .warning("Unrecognized Object from predicate chain.");
            }
        }
        return defaultStroke;   
    }

} // end of class StrokeAction
