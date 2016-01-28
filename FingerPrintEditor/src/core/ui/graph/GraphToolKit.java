package core.ui.graph;

import core.ui.graph.DefaultNode.NodePoint;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.util.Collection;


public class GraphToolKit {
    
    /**
     * Virtically aligns all the moveable nodes with the center of the given node
     * @param tallestNode
     */
    public static void virticallyAllignNodes (Node tallestNode, Collection<Node> nodesToAlign) {
        Point centerPoint = tallestNode.getNodePointValue(NodePoint.leftCenter);
        int vOffset = 0;
        for(Node node : nodesToAlign) {
            vOffset = centerPoint.y - node.getNodePointValue(NodePoint.leftCenter).y;
            node.moveNode(new Point(node.getNodePointValue(NodePoint.topLeft).x, node.getNodePointValue(NodePoint.topLeft).y + vOffset));
        }
    }
    
    /**
     * determines the tallest node of all the moveable nodes
     * @return
     */
    public static Node findTallestNode (Collection<Node> nodeList) {
        int height = 0;
        Node tallestNode = null;
        for(Node node : nodeList) {
            if(node.getHeight() > height) {
                height = node.getHeight();
                tallestNode = node;
            }
        }
        return tallestNode;
    }

    /**
     * Creates a white background
     * @param g2d
     */
    public static void fillBackgroundColor(Graphics2D g2d, Color color) {
        Shape r = new Rectangle2D.Float(g2d.getClip().getBounds().x,g2d.getClip().getBounds().y,g2d.getClip().getBounds().width,g2d.getClip().getBounds().height);
        g2d.setPaint(color);
        g2d.fill(r);
    }
}
