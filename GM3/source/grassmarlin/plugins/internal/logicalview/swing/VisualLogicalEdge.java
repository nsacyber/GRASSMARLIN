package grassmarlin.plugins.internal.logicalview.swing;

import grassmarlin.plugins.internal.logicalview.GraphLogicalEdge;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.util.Collection;

public class VisualLogicalEdge implements ZoomableScrollPane.IContent {
    private Rectangle2D bounds;

    private final GraphLogicalEdge edge;
    private final VisualLogicalVertex vertexSource;
    private final VisualLogicalVertex vertexDestination;

    public VisualLogicalEdge(final GraphLogicalEdge edge, final VisualLogicalVertex vertexSource, final VisualLogicalVertex vertexDestination) {
        this.edge = edge;
        this.vertexSource = vertexSource;
        this.vertexDestination = vertexDestination;
    }

    @Override
    public void paint(Graphics2D g) {
        double xMin = -Double.MAX_VALUE;
        double xMax = Double.MAX_VALUE;
        double yMin = -Double.MAX_VALUE;
        double yMax = Double.MAX_VALUE;

        for(final GraphLogicalEdge.PacketList packetList : edge.getPacketLists()) {
            //TODO: this needs to check to determine which vertex contains the source and which contains the destination.
            final VisualLogicalVertex source = this.vertexSource;
            final VisualLogicalVertex destination = this.vertexDestination;

            final Collection<Point> pointsSource = source.edgePointsFor(packetList.getSourceAddress());
            final Collection<Point> pointsDestination = destination.edgePointsFor(packetList.getDestinationAddress());

            double distance = Double.MAX_VALUE;
            Point ptSource = null;
            Point ptDestination = null;

            for(final Point pt0 : pointsSource) {
                for(final Point pt1 : pointsDestination) {
                    final double dX = pt1.getX() - pt0.getX();
                    final double dY = pt1.getY() - pt0.getY();
                    final double distanceSquared = dX * dX + dY * dY;

                    if(distanceSquared < distance) {
                        distance = distanceSquared;
                        ptSource = pt0;
                        ptDestination = pt1;
                    }
                }
            }

            if(ptSource != null) {
                //TODO: Check for curved lines
                //TODO: Draw endpoint arrows, adjust the endpoint offsets accordingly
                //TODO: Edge styles.
                g.setColor(Color.BLACK);
                g.setStroke(new BasicStroke());
                g.drawLine((int)ptSource.getX(), (int)ptSource.getY(), (int)ptDestination.getX(), (int)ptDestination.getY());

                xMin = Math.min(xMin, Math.min(ptSource.getX(), ptDestination.getX()));
                xMax = Math.max(xMax, Math.max(ptSource.getX(), ptDestination.getX()));
                yMin = Math.min(yMin, Math.min(ptSource.getY(), ptDestination.getY()));
                yMax = Math.max(yMax, Math.max(ptSource.getY(), ptDestination.getY()));
            }
        }

        this.bounds = new Rectangle2D.Double(xMin, yMin, xMax - xMin, yMax - yMin);
    }

    @Override
    public Rectangle2D getBounds() {
        return this.bounds;
    }
}
