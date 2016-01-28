package core.ui;

import javax.swing.*;
import javax.swing.tree.DefaultTreeCellRenderer;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Convenience class for adding simple named nodes to SmartJTrees
 * 
 * 2006.07.19 - Transitioned to repository
 */
public class NamedSmartTreeNode extends SmartTreeNode {
    public static final long serialVersionUID = 10001;
    private static final DefaultTreeCellRenderer renderer = new DefaultTreeCellRenderer(){};
    protected String name;    
    private JPopupMenu popupMenu = null;
    

    /**
     * Creates a new instance of NamedSmartTreeNode
     */
    public NamedSmartTreeNode(String name){ 
        super(name);
        this.name = name; 
    }
    
    /**
     * @inheritDoc
     */
    public MouseAdapter getMouseAdapter() { 
        return mouseAdapter; 
    }
    
    /**
     * Sets the name of this node
     */
    public void setName(String name) { 
        super.setUserObject(name);
        this.name = name;
    }
        
    /**
     * @inheritDoc
     */
    public Component getRenderedComponent(javax.swing.JTree tree, Object value, boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus)  {
        NamedSmartTreeNode.renderer.setText(this.name);
        return NamedSmartTreeNode.renderer;
    }
    
    /**
     * Sets an optional JPopupMenu to be activated by MouseEvents on this Node
     */
    public void setPopupMenu(JPopupMenu menu){
        this.popupMenu = menu;
    }    
    
    /**
     * Shows the Optional JPopupMenu if it has been set
     */
    private MouseAdapter mouseAdapter = new MouseAdapter() {
        public void mousePressed(MouseEvent me) {
            this.maybeShowPopup(me);
        }
        public void mouseReleased(MouseEvent me) {
            this.maybeShowPopup(me);
        }
        private void maybeShowPopup(MouseEvent e) {
            if (e.isPopupTrigger() && popupMenu != null) {
                NamedSmartTreeNode.this.popupMenu.show(e.getComponent(), e.getX(), e.getY());
            }
        }
    };
    
}
