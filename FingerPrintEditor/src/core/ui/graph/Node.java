package core.ui.graph;

import core.ui.graph.DefaultNode.NodePoint;

import java.awt.*;


/**
 * Node on the Graph
 * 
 * 2007.06.13 - 
 */
public interface Node {
    
    public void setBorder(int size);
    
    public void paintNode(Graphics2D g2d);
    
    public void setGradColors(Color color, Color gradColor);
    
    public void setBackgroundColor(Color color);
    
    public void moveNode(Point topLeft);
    
    public boolean isPointOverNode(Point point);
    
    /**
     * Returns the Point specified by NodePoint (topLeft, topRight, etc..) for this Node
     * @param nPoint Point on the node to return
     */
    public Point getNodePointValue(NodePoint nPoint);
    
    public int getHeight();
    
    public int getWidth();
    
    public void setDragOffset(int dragOffsetX, int dragOffsetY);
    
    public int getDragOffsetX();
    
    public int getDragOffsetY();
    
    /**
     * Moves the node to a new location, based on an offset from
     * the mouseLocation.
     * 
     * @param mouseLocation Location of the mouse pointer
     */
    public void moveNodeWithOffset(Point mouseLocation);
    
    public void setHeight(int height);
    
    public void setWidth(int width);
    
    /** 
     * Returns true when this node has a tool tip configured 
     */
    public boolean hasToolTip();
    
    /**
     * Returns the tool tip text if there is any
     */
    public String getToolTipText();
    
    /**
     * Returns true if this Node can be moved independantly
     * TODO: There may be a better way to do this
     */    
    public boolean isMovable();

}
