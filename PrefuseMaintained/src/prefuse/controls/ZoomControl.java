package prefuse.controls;

import java.awt.Cursor;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;

import prefuse.Display;
import prefuse.util.ui.UILib;
import prefuse.visual.VisualItem;


/**
 * Zooms the display, changing the scale of the viewable region. By default,
 * zooming is achieved by pressing the right mouse button on the background
 * of the visualization and dragging the mouse up or down. Moving the mouse up
 * zooms out the display around the spot the mouse was originally pressed.
 * Moving the mouse down similarly zooms in the display, making items
 * larger.
 *
 * @author <a href="http://jheer.org">jeffrey heer</a>
 */
public class ZoomControl extends AbstractZoomControl {
    
    private int yLast;
    private Point2D down = new Point2D.Float();
    private int button = RIGHT_MOUSE_BUTTON; 
    
    /**
     * Create a new zoom control.
     */
    public ZoomControl() {
        // do nothing
    }
    
    /**
     * Create a new zoom control.
     * @param mouseButton the mouse button that should initiate a zoom. One of
     * {@link Control#LEFT_MOUSE_BUTTON}, {@link Control#MIDDLE_MOUSE_BUTTON},
     * or {@link Control#RIGHT_MOUSE_BUTTON}.
     */
    public ZoomControl(int mouseButton) {
        button = mouseButton;
    }
    
    /**
     * @see java.awt.event.MouseListener#mousePressed(java.awt.event.MouseEvent)
     */
    public void mousePressed(MouseEvent e) {
        if ( UILib.isButtonPressed(e, button) ) {
            Display display = (Display)e.getComponent();
            if (display.isTranformInProgress()) {
                yLast = -1;
                System.err.println("can't move");
                return;
            }
            display.setCursor(
                Cursor.getPredefinedCursor(Cursor.N_RESIZE_CURSOR));
            display.getAbsoluteCoordinate(e.getPoint(), down);
            yLast = e.getY();
        }
    }
    
    /**
     * @see java.awt.event.MouseMotionListener#mouseDragged(java.awt.event.MouseEvent)
     */
    public void mouseDragged(MouseEvent e) {
        if ( UILib.isButtonPressed(e, button) ) {
            Display display = (Display)e.getComponent();
            if (display.isTranformInProgress() || yLast == -1) {
                yLast = -1;
                return;
            }
            
            int y = e.getY();
            int dy = y-yLast;
            double zoom = 1 + ((double)dy) / 100;
            
            int status = zoom(display, down, zoom, true);
            int cursor = Cursor.N_RESIZE_CURSOR;
            if ( status == NO_ZOOM )
                cursor = Cursor.WAIT_CURSOR;
            display.setCursor(Cursor.getPredefinedCursor(cursor));
            
            yLast = y;
        }
    }

    /**
     * @see java.awt.event.MouseListener#mouseReleased(java.awt.event.MouseEvent)
     */
    public void mouseReleased(MouseEvent e) {
        if ( UILib.isButtonPressed(e, button) ) {
            e.getComponent().setCursor(Cursor.getDefaultCursor());
        }
    }
    
    /**
     * @see prefuse.controls.Control#itemPressed(prefuse.visual.VisualItem, java.awt.event.MouseEvent)
     */
    public void itemPressed(VisualItem item, MouseEvent e) {
        if ( m_zoomOverItem )
            mousePressed(e);
    }

    /**
     * @see prefuse.controls.Control#itemDragged(prefuse.visual.VisualItem, java.awt.event.MouseEvent)
     */
    public void itemDragged(VisualItem item, MouseEvent e) {
        if ( m_zoomOverItem )
            mouseDragged(e);
    }
    
    /**
     * @see prefuse.controls.Control#itemReleased(prefuse.visual.VisualItem, java.awt.event.MouseEvent)
     */
    public void itemReleased(VisualItem item, MouseEvent e) {
        if ( m_zoomOverItem )
            mouseReleased(e);
    }
    
} // end of class ZoomControl
