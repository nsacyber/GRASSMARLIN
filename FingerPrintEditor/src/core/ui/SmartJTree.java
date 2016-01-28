package core.ui;

import javax.swing.*;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeCellRenderer;
import javax.swing.tree.TreePath;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * Standard JTree with a TreeCellRenderer that delegaes the job
 * of rendering to each SmartJTreeNode, and a MouseListener that 
 * delegates the job of handling MouseEvents to the SmartJTreeNode
 * that is at the point of the mouse event.  Use of this Object 
 * should be carefully considered as creating large numbers of 
 * unique renderes could be expensive for large trees.
 * 
 * 2006.07.19 - Transitioned to repository
 */
public class SmartJTree extends JTree {
    public static final long serialVersionUID = 100001;
    
    /**
     * Creates a new instance of SmartJTree
     */
    public SmartJTree(SmartTreeNode rootNode){ 
        super(rootNode); 
        super.addMouseListener(treeMouseAdapter);
        super.setCellRenderer(treeCellRenderer);
    }
    
    /**
     * Acts as a proxy for the MouseAdapters on each SmartTreeNode
     */
    private MouseAdapter treeMouseAdapter = new MouseAdapter() {
        public void mouseClicked(MouseEvent me){ 
            getMouseAdapter(me, true).mouseClicked(me); 
        }
        public void mouseEntered(MouseEvent me) { 
            getMouseAdapter(me, false).mouseEntered(me); 
        }
        public void mouseExited(MouseEvent me){
            getMouseAdapter(me, false).mouseExited(me);
        }
        public void mousePressed(MouseEvent me){
            getMouseAdapter(me, true).mousePressed(me);
        }
        public void mouseReleased(MouseEvent me){
            getMouseAdapter(me, false).mouseReleased(me);
        }
    };

    /**
     * Default MouseAdapter for non-SmartTreeNodes
     */
    private MouseAdapter emptyAdapter = new MouseAdapter(){
    };
    
    
    /**
     * Finds the TreeNode at the point of the MouseEvent and returns
     * the MouseAdapter for that Node 
     */
    private MouseAdapter getMouseAdapter(MouseEvent me, boolean select) {
        TreePath path = getPathForLocation(me.getX(), me.getY()); 
        if ( path == null ){
            return emptyAdapter;
        }else{
            if ( select ){ 
                this.setSelectionPath(path); 
            }
            SmartTreeNode node = (SmartTreeNode)path.getLastPathComponent();
            return node.getMouseAdapter();
        }
    }   
    
    
    /**
     * Asks each SmartTreeNode for its Rendered Component and displays it on the Tree
     */
    private TreeCellRenderer treeCellRenderer = new TreeCellRenderer() {
        public Component getTreeCellRendererComponent(JTree tree, Object value, boolean selected, boolean expanded, boolean leaf, int row, boolean hasFocus) {
            if ( value instanceof SmartTreeNode ){
                return ((SmartTreeNode)value).getRenderedComponent(tree, value, selected, expanded, leaf, row, hasFocus);
            } else { 
                return new JLabel( "value: " + value.getClass() ); 
            }
        }
    };

    /**
     * Returns the DefaultTreeModel for this SmartJTree
     */
    public DefaultTreeModel getSmartTreeModel() { 
        return (DefaultTreeModel)this.getModel(); 
    }
    
}
