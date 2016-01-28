package prefuse.util.ui;

import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.event.TreeModelEvent;
import javax.swing.event.TreeModelListener;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;

import prefuse.data.Graph;
import prefuse.data.Node;
import prefuse.data.Tree;
import prefuse.data.event.EventConstants;
import prefuse.data.event.GraphListener;
import prefuse.util.StringLib;
import prefuse.util.collections.CopyOnWriteArrayList;
import prefuse.visual.VisualTree;

/**
 * Swing component that displays a prefuse Tree instance in a Swing
 * JTree component. Graph instances can also be displayed by first
 * getting a Tree instance with the
 * {@link prefuse.data.Graph#getSpanningTree()} method.
 * 
 * @author <a href="http://jheer.org">jeffrey heer</a>
 * @see javax.swing.JTree
 */
public class JPrefuseTree extends JTree {

    private Tree m_tree;
    private String m_field;
    
    /**
     * Create a new JPrefuseTree.
     * @param t the Tree to display
     * @param labelField the data field used to privde labels
     */
    public JPrefuseTree(Tree t, String labelField) {
        super();
        m_tree = t;
        m_field = labelField;
        
        PrefuseTreeModel model = new PrefuseTreeModel();
        super.setModel(model);
        m_tree.addGraphModelListener(model);
    }
    
    /**
     * Return the backing Tree instance.
     * @return the backing Tree
     */
    public Tree getTree() {
        return m_tree;
    }
    
    /**
     * Returns a String label for Node instances by looking up the
     * label data field specified in the constructor of this class.
     * @see javax.swing.JTree#convertValueToText(java.lang.Object, boolean, boolean, boolean, int, boolean)
     */
    public String convertValueToText(Object value, boolean selected,
            boolean expanded, boolean leaf, int row, boolean hasFocus)
    {
        if ( value == null )
            return "";
        
        if ( value instanceof Node ) {
            Object o = ((Node)value).get(m_field);
            if ( o.getClass().isArray() ) {
                return StringLib.getArrayString(o);
            } else {
                return o.toString();
            }
        } else {
            return value.toString();
        }
    }
    
    // ------------------------------------------------------------------------
    
    /**
     * TreeModel implementation that provides an adapter to a backing prefuse
     * Tree instance.
     */
    public class PrefuseTreeModel implements TreeModel, GraphListener {

        private CopyOnWriteArrayList m_listeners = new CopyOnWriteArrayList();
        
        /**
         * @see javax.swing.tree.TreeModel#getRoot()
         */
        public Object getRoot() {
            return m_tree.getRoot();
        }

        /**
         * @see javax.swing.tree.TreeModel#getChild(java.lang.Object, int)
         */
        public Object getChild(Object node, int idx) {
            Node c = ((Node)node).getChild(idx);
            if ( c == null ) {
                throw new IllegalArgumentException("Index out of range: "+idx);
            }
            return c;
        }

        /**
         * @see javax.swing.tree.TreeModel#getChildCount(java.lang.Object)
         */
        public int getChildCount(Object node) {
            return ((Node)node).getChildCount();
        }

        /**
         * @see javax.swing.tree.TreeModel#isLeaf(java.lang.Object)
         */
        public boolean isLeaf(Object node) {
            return ((Node)node).getChildCount() == 0;
        }

        /**
         * @see javax.swing.tree.TreeModel#valueForPathChanged(javax.swing.tree.TreePath, java.lang.Object)
         */
        public void valueForPathChanged(TreePath path, Object newValue) {
            // do nothing?
        }

        /**
         * @see javax.swing.tree.TreeModel#getIndexOfChild(java.lang.Object, java.lang.Object)
         */
        public int getIndexOfChild(Object parent, Object child) {
            return ((Node)parent).getChildIndex(((Node)child));
        }

        /**
         * @see javax.swing.tree.TreeModel#addTreeModelListener(javax.swing.event.TreeModelListener)
         */
        public void addTreeModelListener(TreeModelListener tml) {
            if ( !m_listeners.contains(tml) )
                m_listeners.add(tml);
        }

        /**
         * @see javax.swing.tree.TreeModel#removeTreeModelListener(javax.swing.event.TreeModelListener)
         */
        public void removeTreeModelListener(TreeModelListener tml) {
            m_listeners.remove(tml);
        }

        /**
         * @see prefuse.data.event.GraphListener#graphChanged(prefuse.data.Graph, java.lang.String, int, int, int, int)
         */
        public void graphChanged(Graph g, String table, int start, int end,
                                 int col, int type)
        {
            if ( m_listeners == null || m_listeners.size() == 0 )
                return; // nothing to do
            
            boolean nodeTable = table.equals(Graph.NODES);
            if ( type != EventConstants.UPDATE && nodeTable )
                return;
            else if ( type == EventConstants.UPDATE && !nodeTable )
                return;
            
            for ( int row = start; row <= end; ++row ) {
                // create the event
                Node n = null;
                if ( nodeTable )
                    n = g.getNode(row);
                else
                    n = g.getEdge(row).getTargetNode();
                Object[] path = new Object[n.getDepth()+1];
                for ( int i=path.length; --i>=0; n=n.getParent() ) {
                    path[i] = n;
                }
                TreeModelEvent e = new TreeModelEvent(this, path);
                
                // fire it
                Object[] lstnrs = m_listeners.getArray();
                for ( int i=0; i<lstnrs.length; ++i ) {
                    TreeModelListener tml = (TreeModelListener)lstnrs[i];
                    
                    switch ( type ) {
                    case EventConstants.INSERT:
                        tml.treeNodesInserted(e);
                        break;
                    case EventConstants.DELETE:
                        tml.treeNodesRemoved(e);
                        break;
                    case EventConstants.UPDATE:
                        tml.treeNodesChanged(e);
                    }
                }
            }
        }
                
    } // end of inner class PrefuseTreeModel
    
    // ------------------------------------------------------------------------
    
    /**
     * Create a new window displaying the contents of the input Tree as
     * a Swing JTree.
     * @param t the Tree instance to display
     * @param labelField the data field to use for labeling nodes
     * @return a reference to the JFrame holding the tree view
     */
    public static JFrame showTreeWindow(Tree t, String labelField) {
        JPrefuseTree tree = new JPrefuseTree(t, labelField);
        String title = t.toString();
        if ( t instanceof VisualTree ) {
            title = ((VisualTree)t).getGroup() + " " + title;
        }
        JFrame frame = new JFrame(title);
        frame.getContentPane().add(new JScrollPane(tree));
        frame.pack();
        frame.setVisible(true);
        return frame;
    }
    
} // end of class JPrefuseTree
