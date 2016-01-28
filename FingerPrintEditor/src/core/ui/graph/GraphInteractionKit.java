/**
 * 
 */
package core.ui.graph;

import core.ui.graph.DefaultNode.NodePoint;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.util.ArrayList;
import java.util.Collection;


/**
 * The GraphInteractionKit is a series of inner classes that extend various adaptors and listeners
 * the goal is to provide a generic means of interacting with any GraphPanel and prevent the need to dumplicate code
 * between different graph classes
 * 
 * 2007.06.21 - New...
 *
 */
public abstract class GraphInteractionKit {
    
    /**
     * 
     * The <code>SelectClicked</code> class defines a Mouse adapter for selecting a clicked node
     * and highlighting it.
     * 
     * 2007.06.21 - New...
     *
     */
    public static class SelectClicked extends MouseAdapter {
    	/** The collection of all nodes on the active GraphPanel */
        private Collection<DefaultNode> paintedNodes = new ArrayList<DefaultNode>();
        /** The GraphPanel where this mouse adapter listens */
        private GraphPanel component;
        /** Desired color for the indicator */
        private Color color;
        
        /**
         * Constructs a new instance of SelectClicked
         * @param paintedNodes
         * @param component
         * @param color
         */
        public SelectClicked(Collection<DefaultNode> paintedNodes, GraphPanel component, Color color) {
            this.paintedNodes = paintedNodes;
            this.component = component;
            this.color = color;
        }
        
        /**
         * the method called by the listener when a mouse button is pressed.  This method determines
         * which node the mouse is over and creates and indicator node around the clicked node.
         */
        public void mousePressed(MouseEvent me) {
        	//Because we have a shift click listener we need to make sure this listener doesnt 
        	//accedently get invoked when a shift is held down.
            if(MouseEvent.getMouseModifiersText(me.getModifiers()).equals("Shift+Button1")) {
                return;
            }
            //assume mouse is not over a node
            boolean overNode = false;
            //loop though all available nodes
            for (DefaultNode node : this.paintedNodes) { 
            	//ask if mouse pointer is over this node
                if (node.isPointOverNode(me.getPoint())) { 
                    overNode = true;
                    //if this node is already and indicator node then nothing needs to be done (its already selected)
                    //(as a side note,  indicator nodes contain the node they indicate, only the indicator node needs
                    //to be asked to paint the indicated node will be automatically painted on top.)
                    if(!(node instanceof IndicatorNode)) {
                    	//if any node is already selected
                        if(this.component.getSelectedNode() != null) {
                            //add selectedNode back to paint collection
                            this.paintedNodes.add((DefaultNode)this.component.getSelectedNode().getNode());
                            //remove this reference of the indicator node from paint collection
                            this.paintedNodes.remove(this.component.getSelectedNode());
                        }
                        //remove the clicked node from the paint collection
                        this.paintedNodes.remove(node);
                        //create new selector node and add in clicked node
                        this.component.setSelectedNode(new IndicatorNode(node, this.color, 5));
                        //add the selectednode to the paint collection
                        this.paintedNodes.add(this.component.getSelectedNode());
                        break;
                    }
                }
            }
            if(!overNode) {
                if(this.component.getSelectedNode() != null) {
                    //add selectedNode back to paint collection
                    this.paintedNodes.add((DefaultNode)this.component.getSelectedNode().getNode());
                    //remove this reference of the selected node from paint collection
                    this.paintedNodes.remove(this.component.getSelectedNode());
                    //point selected to null
                    this.component.setSelectedNode(null);
                }
            }
            //ask the component to repaint.
            this.component.repaint();
        }
    }
    
    /**
     * 
     * The <code>SelectShiftClicked</code> class defines a Mouse adapter for selecting a shift clicked node
     * and highlighting it.
     * 
     * 2007.06.21 - New...
     *
     */
    public static class SelectShiftClicked extends MouseAdapter {
    	/** The collection of all nodes on the active GraphPanel */
        private Collection<DefaultNode> paintedNodes = new ArrayList<DefaultNode>();
        /** The GraphPanel where this mouse adapter listens */
        private GraphPanel component;
        /** Desired color for the indicator */
        private Color color;
        
        /**
         * Constructs a new instance of SelectShiftClicked
         * @param paintedNodes
         * @param component
         * @param color
         */
        public SelectShiftClicked(Collection<DefaultNode> paintedNodes, GraphPanel component, Color color) {
            this.paintedNodes = paintedNodes;
            this.component = component;
            this.color = color;
        }
        
        /**
         * the method called by the listener when a mouse button is pressed.  This method determines
         * which node the mouse is over and creates and indicator node around the clicked 
         * node only if shift is also pressed.
         * 
         */
        public void mousePressed(MouseEvent me) {
        	//do nothing if shift isnt pressed
            if(!MouseEvent.getMouseModifiersText(me.getModifiers()).equals("Shift+Button1")) {
                return;
            }
            //asume mouse isnt over a node
            boolean overNode = false;
            //loop though all available nodes
            for (DefaultNode node : this.paintedNodes) { 
            	//ask if mouse pointer is over this node
                if (node.isPointOverNode(me.getPoint())) { 
                    overNode = true;
                    //if this node is already and indicator node then nothing needs to be done (its already selected)
                    //(as a side note,  indicator nodes contain the node they indicate, only the indicator node needs
                    //to be asked to paint the indicated node will be automatically painted on top.)
                    if(!(node instanceof IndicatorNode)) {
                        if(this.component.getShiftSelectedNode() != null) {
                            //add selectedNode back to paint collection
                            this.paintedNodes.add((DefaultNode)this.component.getShiftSelectedNode().getNode());
                            //remove this reference of the selected node from paint collection
                            this.paintedNodes.remove(this.component.getShiftSelectedNode());
                        }
                        //remove the clicked node from the paint collection
                        this.paintedNodes.remove(node);
                        //create new selector node and add in clicked node
                        this.component.setShiftSelectedNode(new IndicatorNode(node, this.color, 5));
                        //add the selectednode to the paint collection
                        this.paintedNodes.add(this.component.getShiftSelectedNode());
                        break;
                    }
                }
            }
            if(!overNode) {
                if(this.component.getShiftSelectedNode() != null) {
                    //add selectedNode back to paint collection
                    this.paintedNodes.add((DefaultNode)this.component.getShiftSelectedNode().getNode());
                    //remove this reference of the selected node from paint collection
                    this.paintedNodes.remove(this.component.getShiftSelectedNode());
                    //point selected to null
                    this.component.setShiftSelectedNode(null);
                }
            }
            this.component.repaint();
        }
    }
    
    /**
     * 
     * The <code>PrepareClickedForDrag</code> class defines a Mouse adapter for preparing a clicked node
     * For <code>DragClicked</code>.
     *  
     * 2007.06.21 - New...
     *
     */
    public static class PrepareClickedForDrag extends MouseAdapter {
        
    	/** The collection of all nodes on the active GraphPanel */
        private Collection<DefaultNode> nodes = new ArrayList<DefaultNode>();
        /** The GraphPanel where this mouse adapter listens */
        private GraphPanel component;

        /**
         * constructs a new instance of PrepareClickedForDrag
         * @param nodes
         * @param component
         */
        public PrepareClickedForDrag(Collection<DefaultNode> nodes, GraphPanel component) {
            this.nodes = nodes;
            this.component = component;
        }
        
        /**
         * is called when a mouse button is pressed
         */
        public void mousePressed(MouseEvent me) {
        	//set the clicked node to null
            this.component.setClickedNode(null);
            //loop through all the nodes in the component
            for (DefaultNode node : this.nodes) { 
                if (node.isMovable() && node.isPointOverNode(me.getPoint())) { 
                	//set the clicked node to this node
                    this.component.setClickedNode(node);
                    // set the drag offset. this will be the relitive coodinates for the mouse pointer and the top left
                    // of the node. (prevents the node from snapping to the top left corner on a drag)
                    this.component.getClickedNode().setDragOffset(me.getX() - this.component.getClickedNode().getNodePointValue(NodePoint.topLeft).x,
                            me.getY() - this.component.getClickedNode().getNodePointValue(NodePoint.topLeft).y);
                }
            }
            this.component.repaint();
        }
    }
    
    /**
     * 
     * The <code>DragClicked</code> class defines a MouseMotionListener for dragging a clicked node
     * Must add <code>PrepareClickedForDrag</code> to same componenet in most cases
     * 
     * 2007.06.21 - New...
     *
     */
    public static class DragClicked implements MouseMotionListener {
    	/** The GraphPanel where this mouse adapter listens */
        private GraphPanel component;

        /**
         * creates a new instance of the DragClicked listener
         * @param component
         */
        public DragClicked(GraphPanel component) {
            this.component = component;
        }
        
        /**
         * called when the mouse button is held down and thepointer moves. this moves the node to the new location
         */
        public void mouseDragged (MouseEvent me) {
            if (this.component.getClickedNode() != null) {
                this.component.getClickedNode().moveNodeWithOffset(me.getPoint());
            }
            this.component.repaint();
            
        }
        /**
         * needed to saticify interface, not used here.
         */
        public void mouseMoved (MouseEvent e) {}
    }

}
