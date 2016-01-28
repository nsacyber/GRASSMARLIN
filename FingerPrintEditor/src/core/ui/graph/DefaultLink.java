package core.ui.graph;

import java.awt.*;
import java.util.Observable;


/**
 * Default link is just a plain arrow drawn from origin to target
 * 2007.05 - New...
 * 2007.06.13 - Changed getRect()
 */
public class DefaultLink extends Observable implements Link {
    //points of the arrow
    protected ConnectionPoint origin;
    protected ConnectionPoint target;
    //properties
    protected Color color;
    protected double angle;
    protected double length;
    protected boolean useBestEdge = false;
    
    /**
     * constructs new genius link given origin and target
     * @param origin
     * @param target
     */
    public DefaultLink(ConnectionPoint origin, ConnectionPoint target) {
        this.origin = origin;
        this.target = target;
        this.color = new Color(60,149,230);
        this.angle = findAngle(this.origin.getPoint(), this.target.getPoint());
        this.length = findLength(this.origin.getPoint(), this.target.getPoint());
    }

    /**
     * uses trig to find legth of the hypotenuse
     * @return
     */
    private double findLength (Point originPoint, Point targetPoint) {
        int x1 = originPoint.x;
        int y1 = originPoint.y;
        int x2 = targetPoint.x;
        int y2 = targetPoint.y;
        
        double sideA = x2 - x1;
        double sideB = (y1 > y2 ? (y1 - y2) : (y2 - y1));
        if(sideB == 0) {
            return sideA;
        }
        
        return Math.sqrt(Math.pow(sideA, 2) + Math.pow(sideB, 2));
        
    }

    /**
     * uses trig to find angle of the arrow
     * @return
     */
    private double findAngle (Point originPoint, Point targetPoint) {
        int x1 = originPoint.x;
        int y1 = originPoint.y;
        int x2 = targetPoint.x;
        int y2 = targetPoint.y;
        
        double angle = 0.0;
        double offset = 0.0;

        double sideA = x2 - x1;
        double sideB = 0.0;
        
        if(y1 >= y2) {
            sideB = y1 - y2; 
            offset = Math.toRadians(-90);
        }
        else {
            sideB = -(y2 - y1);
            offset = Math.toRadians(90);
        }
        if(sideB == 0) {
            return angle;
        }
        
        angle = Math.atan(sideA / sideB);
        return angle + offset;
    }
    
    public void setColor(Color color) {
        this.color = color;
    }
    
    public Graphics2D paintLink(Graphics2D g2dd) {
        Point originPoint = this.origin.getPoint();
        Point targetPoint = this.target.getPoint();
        //if statement nightmare
        if (this.origin.hasMoved() || this.target.hasMoved()) {
            this.angle = this.findAngle(originPoint, targetPoint);
            this.length = this.findLength(originPoint, targetPoint);
        
            if(this.useBestEdge) {
                if(this.origin.getNodePoint() == DefaultNode.NodePoint.bottomCenter) {
                    if(this.angle < 0  && this.angle > -Math.PI/2) {
                        this.origin = new ConnectionPoint(this.origin.getNode(),DefaultNode.NodePoint.rightCenter);
                        this.target = new ConnectionPoint(this.target.getNode(),DefaultNode.NodePoint.leftCenter);
                    }
                    if(this.angle > -Math.PI  && this.angle < -Math.PI/2) {
                        this.origin = new ConnectionPoint(this.origin.getNode(),DefaultNode.NodePoint.leftCenter);
                        this.target = new ConnectionPoint(this.target.getNode(),DefaultNode.NodePoint.rightCenter);
                    }
                }
                else if (this.origin.getNodePoint() == DefaultNode.NodePoint.rightCenter) {
                    if(this.angle < -Math.PI/2 && this.angle > -Math.PI) {
                        this.origin = new ConnectionPoint(this.origin.getNode(),DefaultNode.NodePoint.topCenter);
                        this.target = new ConnectionPoint(this.target.getNode(),DefaultNode.NodePoint.bottomCenter);
                    }
                    if(this.angle > Math.PI/2 && this.angle < Math.PI) {
                        this.origin = new ConnectionPoint(this.origin.getNode(),DefaultNode.NodePoint.bottomCenter);
                        this.target = new ConnectionPoint(this.target.getNode(),DefaultNode.NodePoint.topCenter);
                    }
                }
                else if (this.origin.getNodePoint() == DefaultNode.NodePoint.leftCenter) {
                    if(this.angle < Math.PI/2 && this.angle > 0) {
                        this.origin = new ConnectionPoint(this.origin.getNode(),DefaultNode.NodePoint.bottomCenter);
                        this.target = new ConnectionPoint(this.target.getNode(),DefaultNode.NodePoint.topCenter);
                    }
                    if(this.angle > -Math.PI/2 && this.angle < 0) {
                        this.origin = new ConnectionPoint(this.origin.getNode(),DefaultNode.NodePoint.topCenter);
                        this.target = new ConnectionPoint(this.target.getNode(),DefaultNode.NodePoint.bottomCenter);
                    }
                }
                else if(this.origin.getNodePoint() == DefaultNode.NodePoint.topCenter) {
                    if(this.angle > 0  && this.angle < Math.PI/2) {
                        this.origin = new ConnectionPoint(this.origin.getNode(),DefaultNode.NodePoint.rightCenter);
                        this.target = new ConnectionPoint(this.target.getNode(),DefaultNode.NodePoint.leftCenter);
                    }
                    if(this.angle < Math.PI  && this.angle > Math.PI/2) {
                        this.origin = new ConnectionPoint(this.origin.getNode(),DefaultNode.NodePoint.leftCenter);
                        this.target = new ConnectionPoint(this.target.getNode(),DefaultNode.NodePoint.rightCenter);
                    }
                }
                originPoint = this.origin.getPoint();
                targetPoint = this.target.getPoint();
                this.angle = this.findAngle(originPoint, targetPoint);
                this.length = this.findLength(originPoint, targetPoint);
            }
        }
        Graphics2D g2d = (Graphics2D) g2dd.create();
        int ax = originPoint.x;
        int ay = originPoint.y;
        int endX = ax + (int)this.length;
        Shape arrow = new Polygon(new int[] {ax, endX-10, endX-10, endX, endX-10, endX-10, ax, ax}, 
                                  new int[] {ay, ay, ay-4, ay+2, ay+8, ay+4, ay+4, ay}, 8);
        g2d.setStroke(new BasicStroke(1));
        g2d.rotate(this.angle, ax, ay);
        g2d.setPaint(this.color);
        g2d.fill(arrow);
        g2d.setPaint(Color.BLACK);
        g2d.draw(arrow);
        return g2d;
    }
    
    /**
     * 
     */
    public Rectangle getRect() {
        int padding = 10;
        Point originPoint = this.origin.getPoint();
        Point targetPoint = this.target.getPoint();
        int leftest = originPoint.x;
        int rightest = originPoint.x;
        int topest = originPoint.y;
        int bottomest = originPoint.y;
        if (targetPoint.x < leftest) { 
            leftest = targetPoint.x;
        }
        if (targetPoint.x > rightest) { 
            rightest = targetPoint.x;
        }
        if (targetPoint.y < topest) { 
            topest = targetPoint.y;
        }
        if (targetPoint.y > bottomest) { 
            bottomest = targetPoint.y;
        }
        topest -= padding;
        leftest -= padding;
        bottomest += padding;
        rightest += padding;
        return new Rectangle(leftest, topest, rightest - leftest, bottomest - topest);
    }

    /**
     * @return Returns the origin.
     */
    public ConnectionPoint getOrigin () {
        return origin;
    }

    /**
     * @return Returns the target.
     */
    public ConnectionPoint getTarget () {
        return target;
    }

    /**
     * @param useBestEdge The useBestEdge to set.
     */
    public void setUseBestEdge (boolean useBestEdge) {
        this.useBestEdge = useBestEdge;
    }

}
