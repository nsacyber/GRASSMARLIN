package core.ui.graph;

import core.ui.graph.DefaultNode.NodePoint;

import java.awt.*;


/**
 * A connection point to a Node.  Allows an object, such as a Link, to
 * always be able to get the most current location of the connection point
 * on the Node as the Node moves around.
 * 
 * 2007.06.13 - New
 */
public class ConnectionPoint {
    private Node node;
    private NodePoint nodePoint;
    private Point cachedPointValue;
    
    /**
     * Creates a new ConnectionPoint for node
     * @param node
     * @param nodePoint
     */
    public ConnectionPoint(Node node, NodePoint nodePoint) { 
        this.node = node;
        this.nodePoint = nodePoint;
        this.cachedPointValue = new Point(-1,-1);
    }
    
    /**
     * Returns the current connection point
     * @return
     */
    public Point getPoint() { 
        return this.node.getNodePointValue(this.nodePoint);
    }
    
    /**
     * Returns true if the point has changed since the last check (or construction)
     */
    public boolean hasMoved() { 
        boolean moved = ! this.cachedPointValue.equals(this.getPoint());
        this.cachedPointValue = this.getPoint();
        return moved;
    }

    /**
     * @return Returns the node.
     */
    public Node getNode () {
        return node;
    }

    /**
     * @return Returns the nodePoint.
     */
    public NodePoint getNodePoint () {
        return nodePoint;
    }
    
}
