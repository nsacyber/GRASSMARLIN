package prefuse.controls;

import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;

import prefuse.Display;
import prefuse.action.layout.Layout;
import prefuse.visual.VisualItem;


/**
 * Follows the mouse cursor, updating the anchor parameter for any number
 * of layout instances to match the current cursor position. Will also
 * run a given activity in response to cursor updates.
 * 
 * @author <a href="http://jheer.org">jeffrey heer</a>
 */
public class AnchorUpdateControl extends ControlAdapter {
    
    private boolean m_anchorOverItem;
    private Layout[] m_layouts;
    private String m_action;
    private Point2D  m_tmp = new Point2D.Double();
    
    /**
     * Create a new AnchorUpdateControl.
     * @param layout the layout for which to update the anchor point
     */
    public AnchorUpdateControl(Layout layout) {
        this(layout,null);
    }

    /**
     * Create a new AnchorUpdateControl.
     * @param layout the layout for which to update the anchor point
     * @param action the name of an action to run upon anchor updates
     */
    public AnchorUpdateControl(Layout layout, String action) {
        this(new Layout[] {layout}, action);
    }

    /**
     * Create a new AnchorUpdateControl.
     * @param layout the layout for which to update the anchor point
     * @param action the name of an action to run upon anchor updates
     * @param overItem indicates if anchor update events should be processed
     * while the mouse cursor is hovered over a VisualItem.
     */
    public AnchorUpdateControl(Layout layout, String action, boolean overItem)
    {
        this(new Layout[] {layout}, action, overItem);
    }
    
    /**
     * Create a new AnchorUpdateControl.
     * @param layout the layouts for which to update the anchor point
     * @param action the name of an action to run upon anchor updates
     */
    public AnchorUpdateControl(Layout[] layout, String action) {
        this(layout, action, true);
    }
    
    /**
     * Create a new AnchorUpdateControl.
     * @param layout the layouts for which to update the anchor point
     * @param action the name of an action to run upon anchor updates
     * @param overItem indicates if anchor update events should be processed
     * while the mouse cursor is hovered over a VisualItem.
     */
    public AnchorUpdateControl(Layout[] layout, String action, boolean overItem)
    {
        m_layouts = (Layout[])layout.clone();
        m_action = action;
        m_anchorOverItem = overItem;
    }
    
    // ------------------------------------------------------------------------

    /**
     * @see java.awt.event.MouseListener#mouseExited(java.awt.event.MouseEvent)
     */
    public void mouseExited(MouseEvent e) {
        for ( int i=0; i<m_layouts.length; i++ ) 
            m_layouts[i].setLayoutAnchor(null);
        runAction(e);
    }
    
    /**
     * @see java.awt.event.MouseMotionListener#mouseMoved(java.awt.event.MouseEvent)
     */
    public void mouseMoved(MouseEvent e) {
        moveEvent(e);
    }
    
    /**
     * @see java.awt.event.MouseMotionListener#mouseDragged(java.awt.event.MouseEvent)
     */
    public void mouseDragged(MouseEvent e) {
        moveEvent(e);
    }
    
    /**
     * @see prefuse.controls.Control#itemDragged(prefuse.visual.VisualItem, java.awt.event.MouseEvent)
     */
    public void itemDragged(VisualItem item, MouseEvent e) {
        if ( m_anchorOverItem ) moveEvent(e);
    }

    /**
     * @see prefuse.controls.Control#itemMoved(prefuse.visual.VisualItem, java.awt.event.MouseEvent)
     */
    public void itemMoved(VisualItem item, MouseEvent e) {
        if ( m_anchorOverItem ) moveEvent(e);
    }
    
    /**
     * Registers a mouse move event, updating the anchor point for all
     * registered layout instances.
     * @param e the MouseEvent
     */
    public void moveEvent(MouseEvent e) {
        Display d = (Display)e.getSource();
        d.getAbsoluteCoordinate(e.getPoint(), m_tmp);
        for ( int i=0; i<m_layouts.length; i++ ) 
            m_layouts[i].setLayoutAnchor(m_tmp);
        runAction(e);
    }

    /**
     * Runs an optional action upon anchor update.
     * @param e MouseEvent
     */
    private void runAction(MouseEvent e) {
        if ( m_action != null ) {
            Display d = (Display)e.getSource();
            d.getVisualization().run(m_action);
        }
    }
        
} // end of class AnchorUpdateControl
