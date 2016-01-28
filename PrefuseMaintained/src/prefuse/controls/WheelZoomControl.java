package prefuse.controls;

import java.awt.Point;
import java.awt.event.MouseWheelEvent;

import prefuse.Display;
import prefuse.visual.VisualItem;

/**
 * Zooms the display using the mouse scroll wheel, changing the scale of the
 * viewable region.
 *
 * @author bobruney
 * @author mathis ahrens
 * @author <a href="http://jheer.org">jeffrey heer</a>
 */
public class WheelZoomControl extends AbstractZoomControl {
    
    private Point m_point = new Point();
    
    /**
     * @see prefuse.controls.Control#itemWheelMoved(prefuse.visual.VisualItem, java.awt.event.MouseWheelEvent)
     */
    public void itemWheelMoved(VisualItem item, MouseWheelEvent e) {
        if ( m_zoomOverItem )
            mouseWheelMoved(e);
    }
    
    /**
     * @see java.awt.event.MouseWheelListener#mouseWheelMoved(java.awt.event.MouseWheelEvent)
     */
    public void mouseWheelMoved(MouseWheelEvent e) {
        Display display = (Display)e.getComponent();
        m_point.x = display.getWidth()/2;
        m_point.y = display.getHeight()/2;
        zoom(display, m_point,
             1 + 0.1f * e.getWheelRotation(), false);
    }
    
} // end of class WheelZoomControl
