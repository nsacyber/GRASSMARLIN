package prefuse.controls;

import java.awt.Cursor;
import java.awt.Point;
import java.awt.event.MouseEvent;

import prefuse.Display;
import prefuse.util.ui.UILib;


/**
 * Control that can be used to rotate the display. This results in a
 * transformation of the display itself, such that all aspects are
 * rotated. For example, after a rotation of 180 degrees, upright
 * text strings will subsequently upside down. To rotate item positions
 * but leave other aspects such as orientation intact, you can
 * instead create a new {@link prefuse.action.Action} module that rotates
 * just the item co-ordinates.
 *
 * @author <a href="http://jheer.org">jeffrey heer</a>
 */
public class RotationControl extends ControlAdapter {

    private Point down = new Point();
    private double baseAngle = 0; // the baseline angle of the rotation
    private int m_button;         // the mouse button to use
    
    /**
     * Create a new RotateControl. Rotations will be initiated by dragging
     * the mouse with the left mouse button pressed.
     */
    public RotationControl() {
        this(Control.LEFT_MOUSE_BUTTON);
    }
    
    /**
     * Create a new RotateControl
     * @param mouseButton the mouse button that should initiate a rotation. One
     * of {@link Control#LEFT_MOUSE_BUTTON},
     * {@link Control#MIDDLE_MOUSE_BUTTON}, or 
     * {@link Control#RIGHT_MOUSE_BUTTON}.
     */
    public RotationControl(int mouseButton) {
        m_button = mouseButton;
    }
    
    /**
     * @see java.awt.event.MouseListener#mousePressed(java.awt.event.MouseEvent)
     */
    public void mousePressed(MouseEvent e) {
        if ( UILib.isButtonPressed(e, m_button) ) {
            Display display = (Display)e.getComponent();
            display.setCursor(
                Cursor.getPredefinedCursor(Cursor.E_RESIZE_CURSOR));
            down.setLocation(e.getPoint());
            baseAngle = Double.NaN;
        }
    }
    
    /**
     * @see java.awt.event.MouseMotionListener#mouseDragged(java.awt.event.MouseEvent)
     */
    public void mouseDragged(MouseEvent e) {
        if ( UILib.isButtonPressed(e, m_button) ) {
            int dy = e.getY() - down.y;
            int dx = e.getX() - down.x;
            double angle = Math.atan2(dy, dx);
            
            // only rotate once the base angle has been established
            if ( !Double.isNaN(baseAngle) ) {
                Display display = (Display)e.getComponent();
                display.rotate(down, angle-baseAngle);
            }
            baseAngle = angle;
        }
    }
    
    /**
     * @see java.awt.event.MouseListener#mouseReleased(java.awt.event.MouseEvent)
     */
    public void mouseReleased(MouseEvent e) {
        if ( UILib.isButtonPressed(e, m_button) ) {
            e.getComponent().setCursor(Cursor.getDefaultCursor());
        }
    }
    
} // end of class RotationControl
