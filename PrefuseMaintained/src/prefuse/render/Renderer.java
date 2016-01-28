package prefuse.render;

import java.awt.Graphics2D;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;

import prefuse.visual.VisualItem;


/**
 * Interface for rendering VisualItems, providing drawing as well as location
 * checking and bounding box routines.
 * 
 * @author <a href="http://jheer.org">jeffrey heer</a>
 * @author alan newberger
 */
public interface Renderer {

    /**
     * Provides a default graphics context for renderers to do useful
     * things like compute string widths when an external graphics context
     * has not yet been provided.
     */
    public static final Graphics2D DEFAULT_GRAPHICS = (Graphics2D)
        new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB).getGraphics();

    /**
     * Render item into a Graphics2D context.
     * @param g the Graphics2D context
     * @param item the visual item to draw
     */
    public void render(Graphics2D g, VisualItem item);

    /**
     * Returns true if the Point is located inside the extents of the item.
     * This calculation matches against the exaxt item shape, and so is more
     * sensitive than just checking within a bounding box.
     * @param p the point to test for containment
     * @param item the item to test containment against
     * @return true if the point is contained within the the item, else false
     */
    public boolean locatePoint(Point2D p, VisualItem item);

    /**
     * Calculates and sets the bounding rectangle for an item. This is called
     * by a VisualItem when it validates its bounds.
     * @param item the item to compute the bounding box for
     */
    public void setBounds(VisualItem item);

} // end of interface Renderer
