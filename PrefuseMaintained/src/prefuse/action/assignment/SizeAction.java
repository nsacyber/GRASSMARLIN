package prefuse.action.assignment;

import java.util.logging.Logger;

import prefuse.action.EncoderAction;
import prefuse.data.expression.Predicate;
import prefuse.data.expression.parser.ExpressionParser;
import prefuse.visual.VisualItem;


/**
 * <p>Assignment Action that assigns size values to VisualItems.</p>
 * 
 * <p>
 * By default, a SizeAction simply assigns a single default size value to
 * all items (the initial default size is 1.0). Clients can change this 
 * default value to achieve uniform size assignment, or can add any number 
 * of additional rules for size assignment. Rules are specified by a Predicate
 * instance which, if returning true, will trigger that rule, causing either the
 * provided size value or the result of a delegate SizeAction to be
 * applied. Rules are evaluated in the order in which they are added to the
 * SizeAction, so earlier rules will have precedence over rules added later.
 * </p>
 * 
 * <p>In addition, subclasses can simply override {@link #getSize(VisualItem)}
 * to achieve custom size assignment. In some cases, this may be the simplest
 * or most flexible approach.</p>
 * 
 * <p>To automatically assign size values based on varying values of a
 * particular data field, consider using the {@link DataSizeAction}.</p>
 * 
 * @author <a href="http://jheer.org">jeffrey heer</a>
 */
public class SizeAction extends EncoderAction {

    protected double m_defaultSize = 1.0;
    
    /**
     * Constructor. A default size value of 1.0 will be used.
     */
    public SizeAction() {
        super();
    }
    
    /**
     * Constructor. A default size value of 1.0 will be used.
     * @param group the data group processed by this Action.
     */
    public SizeAction(String group) {
        super(group);
    }

    /**
     * Constructor which specified a default size value.
     * @param group the data group processed by this Action.
     * @param size the default size to use
     */
    public SizeAction(String group, double size) {
        super(group);
        m_defaultSize = size;
    }
    
    /**
     * Returns the default size value assigned to items.
     * @return the default size value
     */
    public double getDefaultSize() {
        return m_defaultSize;
    }
    
    /**
     * Sets the default size value assigned to items. Items will be assigned
     * the default size if they do not match any registered rules.
     * @param defaultSize the new default size value
     */
    public void setDefaultSize(double defaultSize) {
        m_defaultSize = defaultSize;
    }
    
    /**
     * Add a size mapping rule to this SizeAction. VisualItems that match
     * the provided predicate will be assigned the given size value (assuming
     * they do not match an earlier rule).
     * @param p the rule Predicate 
     * @param size the size value
     */
    public void add(Predicate p, double size) {
        super.add(p, new Double(size));
    }

    /**
     * Add a size mapping rule to this SizeAction. VisualItems that match
     * the provided expression will be assigned the given size value (assuming
     * they do not match an earlier rule). The provided expression String will
     * be parsed to generate the needed rule Predicate.
     * @param expr the expression String, should parse to a Predicate. 
     * @param size the size value
     * @throws RuntimeException if the expression does not parse correctly or
     * does not result in a Predicate instance.
     */
    public void add(String expr, double size) {
        Predicate p = (Predicate)ExpressionParser.parse(expr);
        add(p, size);       
    }
    
    /**
     * Add a size mapping rule to this SizeAction. VisualItems that match
     * the provided predicate will be assigned the size value returned by
     * the given SizeAction's getSize() method.
     * @param p the rule Predicate 
     * @param f the delegate SizeAction to use
     */
    public void add(Predicate p, SizeAction f) {
        super.add(p, f);
    }

    /**
     * Add a size mapping rule to this SizeAction. VisualItems that match
     * the provided expression will be assigned the given size value (assuming
     * they do not match an earlier rule). The provided expression String will
     * be parsed to generate the needed rule Predicate.
     * @param expr the expression String, should parse to a Predicate. 
     * @param f the delegate SizeAction to use
     * @throws RuntimeException if the expression does not parse correctly or
     * does not result in a Predicate instance.
     */
    public void add(String expr, SizeAction f) {
        Predicate p = (Predicate)ExpressionParser.parse(expr);
        super.add(p, f);
    }
    
    // ------------------------------------------------------------------------
    
    /**
     * @see prefuse.action.ItemAction#process(prefuse.visual.VisualItem, double)
     */
    public void process(VisualItem item, double frac) {
        double size = getSize(item);
        double old = item.getSize();
        item.setStartSize(old);
        item.setEndSize(size);
        item.setSize(size);
    }
    
    /**
     * Returns a size value for the given item.
     * @param item the item for which to get the size value
     * @return the size value for the item
     */
    public double getSize(VisualItem item) {
        Object o = lookup(item);
        if ( o != null ) {
            if ( o instanceof SizeAction ) {
                return ((SizeAction)o).getSize(item);
            } else if ( o instanceof Number ) {
                return ((Number)o).doubleValue();
            } else {
                Logger.getLogger(this.getClass().getName())
                    .warning("Unrecognized Object from predicate chain.");
            }
        }
        return m_defaultSize;   
    }

} // end of class SizeAction
