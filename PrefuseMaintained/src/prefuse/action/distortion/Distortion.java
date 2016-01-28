package prefuse.action.distortion;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.Iterator;

import prefuse.action.layout.Layout;
import prefuse.visual.VisualItem;


/**
 * Abstract base class providing a structure for space-distortion techniques.
 *
 * @version 1.0
 * @author <a href="http://jheer.org">jeffrey heer</a>
 */
public abstract class Distortion extends Layout {

    private Point2D m_tmp = new Point2D.Double();
    protected boolean m_distortSize = true;
    protected boolean m_distortX = true;
    protected boolean m_distortY = true;
    
    // ------------------------------------------------------------------------
    
    /**
     * Create a new Distortion instance.
     */
    public Distortion() {
        super();
    }

    /**
     * Create a new Distortion instance that processes the given data group.
     * @param group the data group processed by this Distortion instance
     */
    public Distortion(String group) {
        super(group);
    }
    
    // ------------------------------------------------------------------------

    /**
     * Controls whether item sizes are distorted along with the item locations.
     * @param s true to distort size, false to distort positions only
     */
    public void setSizeDistorted(boolean s) {
        m_distortSize = s;
    }
    
    /**
     * Indicates whether the item sizes are distorted along with the item
     * locations.
     * @return true if item sizes are distorted by this action, false otherwise
     */
    public boolean isSizeDistorted() {
        return m_distortSize;
    }
    
    // ------------------------------------------------------------------------
    
    /**
     * @see prefuse.action.Action#run(double)
     */
    public void run(double frac) {
        Rectangle2D bounds = getLayoutBounds();
        Point2D anchor = correct(m_anchor, bounds);
        
        final Iterator iter = getVisualization().visibleItems(m_group);
        
        while ( iter.hasNext() ) {
            VisualItem item = (VisualItem)iter.next();
            if ( item.isFixed() ) continue;
            
            // reset distorted values
            // TODO - make this play nice with animation?
            item.setX(item.getEndX());
            item.setY(item.getEndY());
            item.setSize(item.getEndSize());
            
            // compute distortion if we have a distortion focus
            if ( anchor != null ) {
                Rectangle2D bbox = item.getBounds();
                double x = item.getX();
                double y = item.getY();
                
                // position distortion
                if ( m_distortX )
                    item.setX(x=distortX(x, anchor, bounds));
                if ( m_distortY )
                    item.setY(y=distortY(y, anchor, bounds));
                
                // size distortion
                if ( m_distortSize ) {
                    double sz = distortSize(bbox, x, y, anchor, bounds);
                    item.setSize(sz*item.getSize());
                }
            }
        }
    }
    
    /**
     * Corrects the anchor position, such that if the anchor is outside the
     * layout bounds, the anchor is adjusted to be the nearest point on the
     * edge of the bounds.
     * @param anchor the un-corrected anchor point
     * @param bounds the layout bounds
     * @return the corrected anchor point
     */
    protected Point2D correct(Point2D anchor, Rectangle2D bounds) {
        if ( anchor == null ) return anchor;
        double x = anchor.getX(), y = anchor.getY();
        double x1 = bounds.getMinX(), y1 = bounds.getMinY();
        double x2 = bounds.getMaxX(), y2 = bounds.getMaxY();
        x = (x < x1 ? x1 : (x > x2 ? x2 : x));
        y = (y < y1 ? y1 : (y > y2 ? y2 : y));
        
        m_tmp.setLocation(x,y);
        return m_tmp;
    }
    
    /**
     * Distorts an item's x-coordinate.
     * @param x the undistorted x coordinate
     * @param anchor the anchor or focus point of the display
     * @param bounds the layout bounds
     * @return the distorted x-coordinate
     */
    protected abstract double distortX(double x, Point2D anchor, Rectangle2D bounds);

    /**
     * Distorts an item's y-coordinate.
     * @param y the undistorted y coordinate
     * @param anchor the anchor or focus point of the display
     * @param bounds the layout bounds
     * @return the distorted y-coordinate
     */
    protected abstract double distortY(double y, Point2D anchor, Rectangle2D bounds);
    
    /**
     * Returns the scaling factor by which to transform the size of an item.
     * @param bbox the bounding box of the undistorted item
     * @param x the x-coordinate of the distorted item
     * @param y the y-coordinate of the distorted item
     * @param anchor the anchor or focus point of the display
     * @param bounds the layout bounds
     * @return the scaling factor by which to change the size
     */
    protected abstract double distortSize(Rectangle2D bbox, double x, double y, 
            Point2D anchor, Rectangle2D bounds);

} // end of abstract class Distortion
