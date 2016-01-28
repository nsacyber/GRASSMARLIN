package prefuse.render;

import java.awt.Graphics2D;
import java.awt.geom.Point2D;

import prefuse.visual.VisualItem;


/**
 * Renderer that does nothing, causing an item to be rendered "into
 * the void". Possibly useful for items that must exist and have a spatial
 * location but should otherwise be invisible and non-interactive (e.g.,
 * invisible end-points for visible edges).
 *  
 * @author <a href="http://jheer.org">jeffrey heer</a>
 */
public class NullRenderer implements Renderer {

    /**
     * @see prefuse.render.Renderer#render(java.awt.Graphics2D, prefuse.visual.VisualItem)
     */
    public void render(Graphics2D g, VisualItem item) {
        // do nothing
    }
    
    /**
     * @see prefuse.render.Renderer#locatePoint(java.awt.geom.Point2D, prefuse.visual.VisualItem)
     */
    public boolean locatePoint(Point2D p, VisualItem item) {
        return false;
    }
    
    /**
     * @see prefuse.render.Renderer#setBounds(prefuse.visual.VisualItem)
     */
    public void setBounds(VisualItem item) {
        item.setBounds(item.getX(), item.getY(), 0, 0);
    }

} // end of class NullRenderer
