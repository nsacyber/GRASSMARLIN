package core.ui;

import javax.swing.*;
import javax.swing.tree.DefaultMutableTreeNode;
import java.awt.*;
import java.awt.event.MouseAdapter;


/**
 *  TreeNode for display on a SmartJTree.  Renders itself and handles 
 *  its own mouse events.  Use of this approach should be carefully 
 *  considered as creatting a new renderer for each TreeNode could be 
 *  expensive for large trees.
 *
 * 2006.07.19 - Transitioned to Repository
 */
public abstract class SmartTreeNode extends DefaultMutableTreeNode{
    
    /** 
     * Creates a new instance of SmartTreeNode 
     **/
    public SmartTreeNode(Object userObject){
        super(userObject);
    }
    
    /**
     * Returns the fully rendered Component for display on the Tree
     */
    public abstract Component getRenderedComponent(JTree tree, Object value, boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus);
    
    /**
     * Returns the MouseAdapter to handle MouseEvents for this TreeNode
     */
    public abstract MouseAdapter getMouseAdapter();
    
}
