package prefuse.render;

import java.awt.BasicStroke;
import java.awt.Graphics2D;
import java.awt.Shape;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;

import prefuse.util.GraphicsLib;
import prefuse.visual.VisualItem;


/**
 * <p>Abstract base class implementation of the Renderer interface for
 * supporting the drawing of basic shapes. Subclasses should override the
 * {@link #getRawShape(VisualItem) getRawShape} method,
 * which returns the shape to draw. Optionally, subclasses can also override the
 * {@link #getTransform(VisualItem) getTransform} method to apply a desired
 * <code>AffineTransform</code> to the shape.</p>
 * 
 * <p><b>NOTE:</b> For more efficient rendering, subclasses should use a
 * single shape instance in memory, and update its parameters on each call
 * to getRawShape, rather than allocating a new Shape object each time.
 * Otherwise, a new object will be allocated every time something needs to
 * be drawn, and then subsequently be arbage collected. This can significantly
 * reduce performance, especially when there are many things to draw.
 * </p>
 * 
 * @version 1.0
 * @author alan newberger
 * @author <a href="http://jheer.org">jeffrey heer</a>
 */
public abstract class AbstractShapeRenderer implements Renderer {
    
    public static final int RENDER_TYPE_NONE = 0;
    public static final int RENDER_TYPE_DRAW = 1;
    public static final int RENDER_TYPE_FILL = 2;
    public static final int RENDER_TYPE_DRAW_AND_FILL = 3;

    private int m_renderType = RENDER_TYPE_DRAW_AND_FILL;
    protected AffineTransform m_transform = new AffineTransform();
    protected boolean m_manageBounds = true;
    
    public void setManageBounds(boolean b) {
        m_manageBounds = b;
    }
    
    /**
     * @see prefuse.render.Renderer#render(java.awt.Graphics2D, prefuse.visual.VisualItem)
     */
    public void render(Graphics2D g, VisualItem item) {
        Shape shape = getShape(item);
        if (shape != null)
            drawShape(g, item, shape);
    }
    
    /**
     * Draws the specified shape into the provided Graphics context, using
     * stroke and fill color values from the specified VisualItem. This method
     * can be called by subclasses in custom rendering routines. 
     */
    protected void drawShape(Graphics2D g, VisualItem item, Shape shape) {
        GraphicsLib.paint(g, item, shape, getStroke(item), getRenderType(item));
    }

    /**
     * Returns the shape describing the boundary of an item. The shape's
     * coordinates should be in abolute (item-space) coordinates.
     * @param item the item for which to get the Shape
     */
    public Shape getShape(VisualItem item) {
        AffineTransform at = getTransform(item);
        Shape rawShape = getRawShape(item);
        return (at==null || rawShape==null ? rawShape 
                 : at.createTransformedShape(rawShape));
    }

    /**
     * Retursn the stroke to use for drawing lines and shape outlines. By
     * default returns the value of {@link VisualItem#getStroke()}.
     * Subclasses can override this method to implement custom stroke
     * assignment, though changing the <code>VisualItem</code>'s stroke
     * value is preferred.
     * @param item the VisualItem
     * @return the strok to use for drawing lines and shape outlines
     */
    protected BasicStroke getStroke(VisualItem item) {
        return item.getStroke();
    }
    
    /**
     * Return a non-transformed shape for the visual representation of the
     * item. Subclasses must implement this method.
     * @param item the VisualItem being drawn
     * @return the "raw", untransformed shape.
     */
    protected abstract Shape getRawShape(VisualItem item);
    
    /**
     * Return the graphics space transform applied to this item's shape, if any.
     * Subclasses can implement this method, otherwise it will return null 
     * to indicate no transformation is needed.
     * @param item the VisualItem
     * @return the graphics space transform, or null if none
     */
    protected AffineTransform getTransform(VisualItem item) {
        return null;
    }

    /**
     * Returns a value indicating if a shape is drawn by its outline, by a 
     * fill, or both. The default is to draw both.
     * @return the rendering type
     */
    public int getRenderType(VisualItem item) {
        return m_renderType;
    }
    
    /**
     * Sets a value indicating if a shape is drawn by its outline, by a fill, 
     * or both. The default is to draw both.
     * @param type the new rendering type. Should be one of
     *  {@link #RENDER_TYPE_NONE}, {@link #RENDER_TYPE_DRAW},
     *  {@link #RENDER_TYPE_FILL}, or {@link #RENDER_TYPE_DRAW_AND_FILL}.
     */
    public void setRenderType(int type) {
        if ( type < RENDER_TYPE_NONE || type > RENDER_TYPE_DRAW_AND_FILL ) {
            throw new IllegalArgumentException("Unrecognized render type.");
        }
        m_renderType = type;
    }
    
    /**
     * @see prefuse.render.Renderer#locatePoint(java.awt.geom.Point2D, prefuse.visual.VisualItem)
     */
    public boolean locatePoint(Point2D p, VisualItem item) {
        if ( item.getBounds().contains(p) ) {
            // if within bounds, check within shape outline
            Shape s = getShape(item);
            return (s != null ? s.contains(p) : false);
        } else {
            return false;
        }
    }

    /**
     * @see prefuse.render.Renderer#setBounds(prefuse.visual.VisualItem)
     */
    public void setBounds(VisualItem item) {
        if ( !m_manageBounds ) return;
        Shape shape = getShape(item);
        if ( shape == null ) {
            item.setBounds(item.getX(), item.getY(), 0, 0);
        } else {
            GraphicsLib.setBounds(item, shape, getStroke(item));
        }
    }

} // end of abstract class AbstractShapeRenderer
