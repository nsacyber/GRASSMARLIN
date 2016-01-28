package prefuse.controls;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.util.EventListener;

import prefuse.visual.VisualItem;


/**
 * Listener interface for processing user interface events on a Display.
 * 
 * @author alan newberger
 * @author <a href="http://jheer.org">jeffrey heer</a>
 */
public interface Control extends EventListener, 
    MouseListener, MouseMotionListener, MouseWheelListener, KeyListener
{
    /** Represents the use of the left mouse button */
    public static final int LEFT_MOUSE_BUTTON   = MouseEvent.BUTTON1_MASK;
    /** Represents the use of the middle mouse button */
    public static final int MIDDLE_MOUSE_BUTTON = MouseEvent.BUTTON2_MASK;
    /** Represents the use of the right mouse button */
    public static final int RIGHT_MOUSE_BUTTON  = MouseEvent.BUTTON3_MASK;
    
    /**
     * Indicates if this Control is currently enabled.
     * @return true if the control is enabled, false if disabled
     */
    public boolean isEnabled();
    
    /**
     * Sets the enabled status of this control.
     * @param enabled true to enable the control, false to disable it
     */
    public void setEnabled(boolean enabled);
    
    // -- Actions performed on VisualItems ------------------------------------

    /**
     * Invoked when a mouse button is pressed on a VisualItem and then dragged.
     */
    public void itemDragged(VisualItem item, MouseEvent e);
    
    /**
     * Invoked when the mouse cursor has been moved onto a VisualItem but
     *  no buttons have been pushed.
     */
    public void itemMoved(VisualItem item, MouseEvent e);
    
    /**
     * Invoked when the mouse wheel is rotated while the mouse is over a
     *  VisualItem.
     */
    public void itemWheelMoved(VisualItem item, MouseWheelEvent e);
    
    /**
     * Invoked when the mouse button has been clicked (pressed and released) on
     *  a VisualItem.
     */
    public void itemClicked(VisualItem item, MouseEvent e);
    
    /**
     * Invoked when a mouse button has been pressed on a VisualItem.
     */
    public void itemPressed(VisualItem item, MouseEvent e);
    
    /**
     * Invoked when a mouse button has been released on a VisualItem.
     */
    public void itemReleased(VisualItem item, MouseEvent e);
    
    /**
     * Invoked when the mouse enters a VisualItem.
     */
    public void itemEntered(VisualItem item, MouseEvent e);
    
    /**
     * Invoked when the mouse exits a VisualItem.
     */
    public void itemExited(VisualItem item, MouseEvent e);
    
    /**
     * Invoked when a key has been pressed, while the mouse is over
     *  a VisualItem.
     */
    public void itemKeyPressed(VisualItem item, KeyEvent e);
    
    /**
     * Invoked when a key has been released, while the mouse is over
     *  a VisualItem.
     */
    public void itemKeyReleased(VisualItem item, KeyEvent e);
    
    /**
     * Invoked when a key has been typed, while the mouse is over
     *  a VisualItem.
     */
    public void itemKeyTyped(VisualItem item, KeyEvent e);
    
    
    // -- Actions performed on the Display ------------------------------------
    
    /**
     * Invoked when the mouse enters the Display.
     */
    public void mouseEntered(MouseEvent e);
    
    /**
     * Invoked when the mouse exits the Display.
     */
    public void mouseExited(MouseEvent e);
    
    /**
     * Invoked when a mouse button has been pressed on the Display but NOT
     *  on a VisualItem.
     */
    public void mousePressed(MouseEvent e);
    
    /**
     * Invoked when a mouse button has been released on the Display but NOT
     *  on a VisualItem.
     */
    public void mouseReleased(MouseEvent e);
    
    /**
     * Invoked when the mouse button has been clicked (pressed and released) on
     *  the Display, but NOT on a VisualItem.
     */
    public void mouseClicked(MouseEvent e);
    
    /**
     * Invoked when a mouse button is pressed on the Display (but NOT a 
     *  VisualItem) and then dragged.
     */
    public void mouseDragged(MouseEvent e);
    
    /**
     * Invoked when the mouse cursor has been moved on the Display (but NOT a
     * VisualItem) and no buttons have been pushed.
     */
    public void mouseMoved(MouseEvent e);
    
    /**
     * Invoked when the mouse wheel is rotated while the mouse is over the
     *  Display (but NOT a VisualItem).
     */
    public void mouseWheelMoved(MouseWheelEvent e);
    
    /**
     * Invoked when a key has been pressed, while the mouse is NOT 
     *  over a VisualItem.
     */
    public void keyPressed(KeyEvent e);
    
    /**
     * Invoked when a key has been released, while the mouse is NOT
     *  over a VisualItem.
     */
    public void keyReleased(KeyEvent e);
    
    /**
     * Invoked when a key has been typed, while the mouse is NOT
     *  over a VisualItem.
     */
    public void keyTyped(KeyEvent e);

} // end of inteface ControlListener
