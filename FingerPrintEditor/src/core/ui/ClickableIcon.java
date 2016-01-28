/**
 * 
 */
package core.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseListener;

/**
 *
 */
public class ClickableIcon implements Icon{
    private ImageIcon xIcon;// = new ImageIcon(SlickTabbedPane.class.getResource("/resource/images/error.png"));
    private Rectangle iconPosition;
    private MouseListener mouseListener;
    private Component component;
    boolean firstPaint = true;
    
    public Component getComponent () {
        return component;
    }

    public void setComponent ( Component component ) {
        this.component = component;
    }

    public ClickableIcon(ImageIcon icon, Component component, MouseListener listener){
        this.setComponent(component);
        this.xIcon = icon;
        this.setMouseListener(listener);
    }
    
    public MouseListener getMouseListener () {
        return mouseListener;
    }

    public void setMouseListener ( MouseListener mouseListener ) {
        this.mouseListener = mouseListener;
    }
    
    public int getIconHeight () {
        return this.xIcon.getIconHeight();
    }

    public int getIconWidth () {
        return this.xIcon.getIconWidth();
    }
    
    public void paintIcon ( Component c, Graphics g, int x, int y ) {
        if(firstPaint && this.mouseListener != null){ 
            this.component.addMouseListener( this.mouseListener);
        }
        this.iconPosition = new Rectangle(x,y,getIconWidth(), getIconHeight() );
        this.xIcon.paintIcon(c, g, x, y);
        firstPaint = false;
    }

    public Rectangle getIconPosition () {
        return iconPosition;
    }

    public void setIconPosition ( Rectangle iconPosition ) {
        this.iconPosition = iconPosition;
    }        

}
