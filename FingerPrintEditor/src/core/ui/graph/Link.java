package core.ui.graph;

import java.awt.*;

public interface Link {
//    /**
//     * Sets the connection points for this link
//     * @param origin
//     * @param target
//     */
//    public void setEndPoints(ConnectionPoint origin, ConnectionPoint target);
    public void setColor(Color color);
    public Graphics2D paintLink(Graphics2D g2dd);
    public Rectangle getRect();
    public ConnectionPoint getOrigin();
    public ConnectionPoint getTarget();
    public void setUseBestEdge(boolean useBestEdge);

}
