package prefuse.render;

import prefuse.visual.VisualItem;

/**
 * The RendererFactory is responsible for providing the proper Renderer
 * instance for drawing a given VisualItem.
 * 
 * @author <a href="http://jheer.org">jeffrey heer</a>
 */
public interface RendererFactory {

    /**
     * Return a Renderer instance to draw the given VisualItem.
     * @param item the item for which to retrieve the renderer
     * @return the Renderer for the given VisualItem
     */
    public Renderer getRenderer(VisualItem item);

} // end of interface RendererFactory
