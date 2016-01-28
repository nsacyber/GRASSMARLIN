package core.ui;


import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;


/**
 *  ListItem for display on a SmartJList.  Renders itself and handles 
 *  its own mouse events
 *  
 *  2005.12.22 - Transitioned from Xware Legacy
 */
public interface SmartJListItem {

    /** 
     * Returns a fully rendered Component for display on the SmartJList 
     */
    public Component getRenderedComponent(JList list,
                                          Object value,
                                          int index,
                                          boolean isSelected,
                                          boolean cellHasFocus); 

    /**
     * Returns the MouseAdapter that will handle mouse events related to this
     * item on the SmartJList
     */
    public MouseAdapter getMouseAdapter();
    
}
