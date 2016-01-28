package prefuse.action.layout;

import java.awt.Insets;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

import prefuse.Display;
import prefuse.action.GroupAction;
import prefuse.util.PrefuseLib;
import prefuse.visual.VisualItem;

/**
 * Abstract base class providing convenience methods for layout algorithms.
 *
 * @author <a href="http://jheer.org">jeffrey heer</a>
 */
public abstract class Layout extends GroupAction {

    /** The explicitly set layout bounds. May be null. */
    protected Rectangle2D m_bounds = null;
    /** The explicitly set anchor point at which the layout can
     *  be centered or rooted. May be null. */
    protected Point2D     m_anchor = null;
    
    protected boolean     m_margin = false;
    protected Insets      m_insets = new Insets(0,0,0,0);
    protected double[]    m_bpts   = new double[4];
    protected Rectangle2D m_tmpb   = new Rectangle2D.Double();
    protected Point2D     m_tmpa   = new Point2D.Double();
    
    // ------------------------------------------------------------------------
    
    /**
     * Create a new Layout.
     */
    public Layout() {
        super();
    }
    
    /**
     * Create a new Layout.
     * @param group the data group to layout.
     */
    public Layout(String group) {
        super(group);
    }

    public Layout(String group, long duration) {
        super(group, duration);
    }
    
    // ------------------------------------------------------------------------
    
    /**
     * Set the margins the layout should observe within its layout bounds.
     * @param top the top margin, in pixels
     * @param left the left margin, in pixels
     * @param bottom the bottom margin, in pixels
     * @param right the right margin, in pixels
     */
    public void setMargin(int top, int left, int bottom, int right) {
        m_insets.top = top;
        m_insets.left = left;
        m_insets.bottom = bottom;
        m_insets.right = right;
        m_margin = true;
    }
    
    /**
     * Returns the bounds in which the layout should be computed. If the
     * bounds have been explicitly set, that value is used. Otherwise,
     * an attempt is made to compute the bounds based upon the display
     * region of the first display found in this action's associated
     * Visualization.
     * @return the layout bounds within which to constrain the layout.
     */
    public Rectangle2D getLayoutBounds() {
        if ( m_bounds != null )
            return m_bounds;
        
        if ( m_vis != null && m_vis.getDisplayCount() > 0 )
        {
            Display d = m_vis.getDisplay(0);
            Insets i = m_margin ? m_insets : d.getInsets(m_insets);
            m_bpts[0] = i.left; 
            m_bpts[1] = i.top;
            m_bpts[2] = d.getWidth()-i.right;
            m_bpts[3] = d.getHeight()-i.bottom;
            d.getInverseTransform().transform(m_bpts,0,m_bpts,0,2);
            m_tmpb.setRect(m_bpts[0],m_bpts[1],
                          m_bpts[2]-m_bpts[0],
                          m_bpts[3]-m_bpts[1]);
            return m_tmpb;
        } else {
            return null;
        }
    }
    
    /**
     * Explicitly set the layout bounds. A reference to the input rectangle
     * instance is maintained, not a copy, and so any subsequent changes to
     * the rectangle object will also change the layout bounds.
     * @param b a rectangle specifying the layout bounds. A reference to this
     * same instance is kept.
     */
    public void setLayoutBounds(Rectangle2D b) {
        m_bounds = b;
    }
     
    /**
     * Return the layout anchor at which to center or root the layout. How this
     * point is used (if it is used at all) is dependent on the particular
     * Layout implementation. If no anchor point has been explicitly set, the
     * center coordinate for the first display found in this action's
     * associated Visualization is used, if available.
     * @return the layout anchor point.
     */
    public Point2D getLayoutAnchor() {
        if ( m_anchor != null )
            return m_anchor;
        
        m_tmpa.setLocation(0,0);
        if ( m_vis != null ) {
            Display d = m_vis.getDisplay(0);
            m_tmpa.setLocation(d.getWidth()/2.0,d.getHeight()/2.0);
            d.getInverseTransform().transform(m_tmpa, m_tmpa);
        }
        return m_tmpa;
    }
    
    /**
     * Explicitly set the layout anchor point. The provided object will be
     * used directly (rather than copying its values), so subsequent
     * changes to that point object will change the layout anchor.
     * @param a the layout anchor point to use
     */
    public void setLayoutAnchor(Point2D a) {
        m_anchor = a;
    }
    
    /**
     * Convenience method for setting an x-coordinate. The start value of the
     * x-coordinate will be set to the current value, and the current and end
     * values will be set to the provided x-coordinate. If the current value
     * is not a number (NaN), the x-coordinate of the provided referrer
     * item (if non null) will be used to set the start coordinate.
     * @param item the item to set
     * @param referrer the referrer item to use for the start location if
     * the current value is not a number (NaN)
     * @param x the x-coordinate value to set. This will be set for both
     * the current and end values.
     * @see prefuse.util.PrefuseLib#setX(VisualItem, VisualItem, double)
     */
    public void setX(VisualItem item, VisualItem referrer, double x) {
        PrefuseLib.setX(item, referrer, x);
    }
    
    /**
     * Convenience method for setting an y-coordinate. The start value of the
     * y-coordinate will be set to the current value, and the current and end
     * values will be set to the provided y-coordinate. If the current value
     * is not a number (NaN), the y-coordinate of the provided referrer
     * item (if non null) will be used to set the start coordinate.
     * @param item the item to set
     * @param referrer the referrer item to use for the start location if
     * the current value is not a number (NaN)
     * @param y the y-coordinate value to set. This will be set for both
     * the current and end values.
     * @see prefuse.util.PrefuseLib#setY(VisualItem, VisualItem, double)
     */
    public void setY(VisualItem item, VisualItem referrer, double y) {
        PrefuseLib.setY(item, referrer, y);
    }

} // end of abstract class Layout
