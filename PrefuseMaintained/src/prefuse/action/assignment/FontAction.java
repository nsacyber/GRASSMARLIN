package prefuse.action.assignment;

import java.awt.Font;
import java.util.logging.Logger;

import prefuse.action.EncoderAction;
import prefuse.data.expression.Predicate;
import prefuse.data.expression.parser.ExpressionParser;
import prefuse.util.FontLib;
import prefuse.visual.VisualItem;


/**
 * <p>Assignment Action that assigns font values to VisualItems.
 * By default, a FontAction simply sets each VisualItem to use a default 
 * 10 point sans-serif font (10 point sans-serif). Clients can change this
 * default value to achieve uniform font assignment, or can add any number
 * of additional rules for font assignment.
 * Rules are specified by a Predicate instance which, if returning true, will
 * trigger that rule, causing either the provided font value or the result of
 * a delegate FontAction to be applied. Rules are evaluated in the order in
 * which they are added to the FontAction, so earlier rules will have
 * precedence over rules added later.
 * </p>
 * 
 * <p>In addition, subclasses can simply override {@link #getFont(VisualItem)}
 * to achieve custom font assignment. In some cases, this may be the simplest
 * or most flexible approach.</p>
 * 
 * @author <a href="http://jheer.org">jeffrey heer</a>
 */
public class FontAction extends EncoderAction {

    protected Font defaultFont = FontLib.getFont("SansSerif",Font.PLAIN,10);
    
    /**
     * Create a new FontAction that processes all data groups.
     */
    public FontAction() {
        super();
    }
    
    /**
     * Create a new FontAction that processes the specified group.
     * @param group the data group to process
     */
    public FontAction(String group) {
        super(group);
    }
    
    /**
     * Create a new FontAction that processes the specified group.
     * @param group the data group to process
     * @param defaultFont the default Font to assign
     */
    public FontAction(String group, Font defaultFont) {
        super(group);
        this.defaultFont = defaultFont;
    }
    
    // ------------------------------------------------------------------------
    
    /**
     * Set the default font to be assigned to items. Items will be assigned
     * the default font if they do not match any registered rules.
     * @param f the default font to use
     */
    public void setDefaultFont(Font f) {
        defaultFont = f;
    }
    
    /**
     * Get the default font assigned to items.
     * @return the default font
     */
    public Font getDefaultFont() {
        return defaultFont;
    }
    
    /**
     * Add a font mapping rule to this FontAction. VisualItems that match
     * the provided predicate will be assigned the given font value (assuming
     * they do not match an earlier rule).
     * @param p the rule Predicate 
     * @param font the font
     */
    public void add(Predicate p, Font font) {
        super.add(p, font);
    }

    /**
     * Add a font mapping rule to this FontAction. VisualItems that match
     * the provided expression will be assigned the given font value (assuming
     * they do not match an earlier rule). The provided expression String will
     * be parsed to generate the needed rule Predicate.
     * @param expr the expression String, should parse to a Predicate. 
     * @param font the font
     * @throws RuntimeException if the expression does not parse correctly or
     * does not result in a Predicate instance.
     */
    public void add(String expr, Font font) {
        Predicate p = (Predicate)ExpressionParser.parse(expr);
        super.add(p, font);       
    }
    
    /**
     * Add a font mapping rule to this FontAction. VisualItems that match
     * the provided predicate will be assigned the font value returned by
     * the given FontAction's getFont() method.
     * @param p the rule Predicate 
     * @param f the delegate FontAction to use
     */
    public void add(Predicate p, FontAction f) {
        super.add(p, f);
    }

    /**
     * Add a font mapping rule to this FontAction. VisualItems that match
     * the provided expression will be assigned the given font value (assuming
     * they do not match an earlier rule). The provided expression String will
     * be parsed to generate the needed rule Predicate.
     * @param expr the expression String, should parse to a Predicate. 
     * @param f the delegate FontAction to use
     * @throws RuntimeException if the expression does not parse correctly or
     * does not result in a Predicate instance.
     */
    public void add(String expr, FontAction f) {
        Predicate p = (Predicate)ExpressionParser.parse(expr);
        super.add(p, f);
    }
    
    // ------------------------------------------------------------------------
    
    /**
     * @see prefuse.action.ItemAction#process(prefuse.visual.VisualItem, double)
     */
    public void process(VisualItem item, double frac) {
        Font f = getFont(item);
        Font o = item.getFont();
        item.setStartFont(o);
        item.setEndFont(f);
        item.setFont(f);
    }
    
    /**
     * Returns the Font to use for a given VisualItem. Subclasses should
     * override this method to perform customized font assignment.
     * @param item the VisualItem for which to get the Font
     * @return the Font for the given item
     */
    public Font getFont(VisualItem item) {
        Object o = lookup(item);
        if ( o != null ) {
            if ( o instanceof FontAction ) {
                return ((FontAction)o).getFont(item);
            } else if ( o instanceof Font ) {
                return (Font)o;
            } else {
                Logger.getLogger(this.getClass().getName())
                    .warning("Unrecognized Object from predicate chain.");
            }
        }
        return defaultFont;   
    }

} // end of class FontAction
