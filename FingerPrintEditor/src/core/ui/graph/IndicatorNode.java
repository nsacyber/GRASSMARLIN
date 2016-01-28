package core.ui.graph;

import java.awt.*;

public class IndicatorNode extends DefaultNode {
    
    private Node indicatedNode;
    private int intensity;
    
    public IndicatorNode (Node indicatedNode, Color color, int intensity) {
        this.indicatedNode = indicatedNode;
        this.boarderColor = color;
        this.setBackgroundColor(color);
        this.intensity = intensity;
        initializeIndicatorNode();
    }

    private void initializeIndicatorNode () {
        this.width = this.indicatedNode.getWidth() + this.intensity;
        this.height = this.indicatedNode.getHeight() + this.intensity;
        this.setBorder(this.intensity);
        this.setMovable(this.indicatedNode.isMovable());
        Point topLeft = new Point(this.indicatedNode.getNodePointValue(NodePoint.topLeft));
        topLeft.setLocation(topLeft.x - this.intensity/2, topLeft.y - this.intensity/2);
        this.generateAllPointsFromTopLeft(topLeft);
    }
    
    public void paintNode(Graphics2D g2d) {
        super.paintNode(g2d);
        Point nodeTopLeft = new Point(0,0);
        nodeTopLeft = new Point(this.nodePoints.get(NodePoint.topLeft).x + this.intensity/2,
                this.nodePoints.get(NodePoint.topLeft).y + this.intensity/2);
        this.indicatedNode.moveNode(nodeTopLeft);
        this.indicatedNode.paintNode(g2d);
    }
    
    public Node getNode() {
        return this.indicatedNode;
    }

}
