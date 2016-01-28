package core.ui.graph;

import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Rectangle2D;
import java.util.HashMap;
import java.util.Optional;


/**
 * Default implementation of Node 
 * 
 * 2007.06.05 - (CC) New...
 * 2007.06.14 - (CC) syncronized all methods touching nodePoints
 */
public class DefaultNode implements Node {
    public boolean isCloseable;
    private Shape closeButton = null;
    private GraphNodeContainer nodeContainer;

    public void updateContainerRef(GraphNodeContainer nodeContainer) {
        this.nodeContainer = nodeContainer;
    }

    public GraphNodeContainer getNodeContainer() {
        return nodeContainer;
    }

    public Shape getCloseButton() {
        return closeButton;
    }

    /**
     * References to points on the Node
     */
    public enum NodePoint {
        topLeft,
        topRight,
        topCenter,
        bottomLeft,
        bottomRight,
        bottomCenter,
        leftCenter,
        rightCenter,
    }
    protected int width;
    protected int height;
    private String name = "";
    /**
     * Actual point values for connection points on the Node
     */
    protected HashMap<NodePoint, Point> nodePoints = new HashMap<NodePoint, Point>();
    
    /** Background color for this node **/
    protected Color color;
    protected Color gradColor;
    protected boolean useGradient;
    protected boolean useBoarder;
    protected int boarderSize;
    protected Color boarderColor;
    protected int dragOffsetX;
    protected int dragOffsetY;
    
    /**
     * This node can be moved by the user when true.  Default if false.
     */
    private boolean movable = false;

    /**
     * Default constructor - locates node at the origin - 0,0
     */
    public DefaultNode () {
        this(0,0);
    }

    /**
     * 
     */
    public DefaultNode (int width, int height) {
        this(width,height,new Point(0,0));
    }
    
    /**
     * 
     */
    public DefaultNode (int width, int height, Point topLeft) {
        this.width = width;
        this.height = height;
        generateAllPointsFromTopLeft(topLeft);
        this.useGradient = false;
        this.color = Color.WHITE;
        this.useBoarder = true;
        this.boarderSize = 1;
        this.boarderColor = Color.BLACK;
    }

    /**
     * Calculates all node connection points
     */
    protected synchronized void generateAllPointsFromTopLeft(Point topLeftPoint) {
        this.nodePoints.clear();
        this.nodePoints.put(NodePoint.topLeft, topLeftPoint);
        
        Point point = new Point();
        
        point.setLocation(this.nodePoints.get(NodePoint.topLeft).x + width, this.nodePoints.get(NodePoint.topLeft).y);
        this.nodePoints.put(NodePoint.topRight,  (Point)point.clone());
        
        point.setLocation(this.nodePoints.get(NodePoint.topLeft).x + (width / 2), this.nodePoints.get(NodePoint.topLeft).y);
        this.nodePoints.put(NodePoint.topCenter,  (Point)point.clone());
        
        point.setLocation(this.nodePoints.get(NodePoint.topLeft).x, this.nodePoints.get(NodePoint.topLeft).y + this.height);
        this.nodePoints.put(NodePoint.bottomLeft,  (Point)point.clone());
        
        point.setLocation(this.nodePoints.get(NodePoint.bottomLeft).x + this.width, this.nodePoints.get(NodePoint.bottomLeft).y);
        this.nodePoints.put(NodePoint.bottomRight,  (Point)point.clone());
        
        point.setLocation(this.nodePoints.get(NodePoint.bottomLeft).x + (this.width / 2), this.nodePoints.get(NodePoint.bottomLeft).y);
        this.nodePoints.put(NodePoint.bottomCenter,  (Point)point.clone());
        
        point.setLocation(this.nodePoints.get(NodePoint.topLeft).x, this.nodePoints.get(NodePoint.topLeft).y + (this.height/2));
        this.nodePoints.put(NodePoint.leftCenter,  (Point)point.clone());
        
        point.setLocation(this.nodePoints.get(NodePoint.topRight).x, this.nodePoints.get(NodePoint.topLeft).y + (this.height/2));
        this.nodePoints.put(NodePoint.rightCenter,  (Point)point.clone());
    }
    
    public synchronized Point getNodePointValue(NodePoint nPoint) {
        return this.nodePoints.get(nPoint);
    }
    
    public void setDragOffset(int dragOffsetX, int dragOffsetY) {
        this.dragOffsetX = dragOffsetX;
        this.dragOffsetY = dragOffsetY;
    }
    
    public int getDragOffsetX() {
        return this.dragOffsetX;
    }
    
    public int getDragOffsetY() {
        return this.dragOffsetY;
    }
    
    public void moveNode(Point topLeft) {
        generateAllPointsFromTopLeft(topLeft);
    }
    
    public void moveNodeWithOffset(Point mouseLocation) {
        mouseLocation.setLocation((mouseLocation.x - this.dragOffsetX), (mouseLocation.y - this.dragOffsetY));
        this.generateAllPointsFromTopLeft(mouseLocation);
    }
    
    public void setBackgroundColor(Color color) {
        this.color = color;
        this.useGradient = false;
    }
    
    public void setGradColors(Color color, Color gradColor) {
        this.color = color;
        this.gradColor = gradColor;
        this.useGradient = true;
    }
    
    public void setBorder(int size) {
        if(size == 0) {
            this.useBoarder = false;
        }
        else {
            this.boarderSize = size;
        }
    }
    
    public synchronized void paintNode(Graphics2D g2d) {
        Shape r = new Rectangle2D.Float( this.nodePoints.get(NodePoint.topLeft).x, this.nodePoints.get(NodePoint.topLeft).y, this.width, this.height);
        if(this.useBoarder) {
            g2d.setStroke(new BasicStroke(this.boarderSize));
            g2d.setPaint(this.boarderColor);
            g2d.draw(r);
        }
        if(this.useGradient) {
            g2d.setPaint(new GradientPaint(this.nodePoints.get(NodePoint.topLeft).x,0,this.color, this.nodePoints.get(NodePoint.topRight).x, 0, this.gradColor, false));
        }
        else {
            g2d.setPaint(this.color);
        }
        
        g2d.fill(r);
        maybePaintCloseButton(g2d);
    }

    public synchronized void maybePaintCloseButton(Graphics2D g2d) {
        generateAllPointsFromTopLeft(this.nodePoints.get(NodePoint.topLeft));
        if(this.isCloseable) {
            this.closeButton = new Ellipse2D.Float(this.nodePoints.get(NodePoint.topRight).x-13 , this.nodePoints.get(NodePoint.topRight).y+5, 7, 7);
            g2d.setStroke(new BasicStroke(1));
            g2d.setPaint(Color.red);
            g2d.fill(closeButton);
            g2d.setPaint(Color.black);
            g2d.draw(closeButton);
        }
    }

    public synchronized void maybeClose(Point point) {
        if(isPointOverCloseButton(point) && this.nodeContainer != null) {
            this.nodeContainer.removeNode(this);
        }
    }
    
    public synchronized boolean isPointOverNode(Point point) {
        return (point.x > this.nodePoints.get(NodePoint.topLeft).x && point.x < this.nodePoints.get(NodePoint.topRight).x) &&
                (point.y > this.nodePoints.get(NodePoint.topLeft).y && point.y < this.nodePoints.get(NodePoint.bottomLeft).y);
    }

    private synchronized boolean isPointOverCloseButton(Point point) {
        if(this.closeButton == null) {
            return false;
        }
        else {
            return this.closeButton.contains(point.x,point.y);
        }
    }
    
    public int getHeight() {
        return this.height;
    }
    
    public int getWidth() {
        return this.width;
    }
    
    public void setHeight(int height) {
        this.height = height;
    }
    
    public void setWidth(int width) {
        this.width = width;
    }
    
    /**
     * Returns false by default
     */
    public boolean hasToolTip() { 
        return false;
    }
    
    /**
     * Returns an empty String
     */
    public String getToolTipText() { 
        return "";
    }
    
    /**
     * Toggles the movable indicator for the Node
     */
    public void setMovable(boolean bv) { 
        this.movable = bv;
    }
    
    /**
     * Returns true if this Node can be moved by the user
     */
    public boolean isMovable() { 
        return this.movable;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isCloseable() {
        return isCloseable;
    }

    public void setIsCloseable(boolean isCloseable) {
        this.isCloseable = isCloseable;
    }

    public boolean mouseDragged(MouseEvent me, Optional<DefaultNode> nodeBeingDragged) { return false; }

    public boolean mouseReleased(MouseEvent me, Optional<DefaultNode> nodeBeingReleased) { return false;}

    public boolean mouseRightClicked(MouseEvent me) { return false; }

    public void setGrowForMouseOver(boolean mouseOverGrow ) {}
}




































