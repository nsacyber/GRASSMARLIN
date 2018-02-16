package grassmarlin.plugins.internal.logicalview.swing;

import grassmarlin.plugins.internal.logicalview.GraphLogicalVertex;
import grassmarlin.plugins.internal.logicalview.IVisualLogicalVertex;
import grassmarlin.plugins.internal.logicalview.visual.VertexColorAssignment;
import grassmarlin.session.LogicalVertex;
import grassmarlin.session.pipeline.LogicalAddressMapping;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;

public class VisualLogicalVertex implements ZoomableScrollPane.IContent, ZoomableScrollPane.IDraggable, IVisualLogicalVertex {
    private final GraphLogicalVertex vertex;
    private Rectangle2D bounds;
    private double locationX;
    private double locationY;

    private boolean showChildElements;

    private final Map<LogicalAddressMapping, Collection<Point>> edgePoints;
    private final Collection<Point> pointsRootRow;

    public VisualLogicalVertex(final GraphLogicalVertex vertex) {
        this.vertex = vertex;

        this.edgePoints = new HashMap<>();
        this.pointsRootRow = new CopyOnWriteArrayList<>();

        this.showChildElements = true;
    }

    // == Accessors / Accessor-like methods
    public boolean getChildElementVisibility() {
        return this.showChildElements;
    }
    public boolean setChildElementVisibility(final boolean value) {
        if(this.showChildElements != value) {
            this.showChildElements = value;
            return true;
        } else {
            return false;
        }
    }

    public boolean isSubjectToLayout() {
        //TODO: allow changing of isSubjectToLayout flag
        return true;
    }

    @Override
    public GraphLogicalVertex getVertex() {
        return this.vertex;
    }


    protected Collection<Point> edgePointsFor(final LogicalAddressMapping mapping) {
        synchronized(this.edgePoints) {
            final Collection<Point> existing = this.edgePoints.get(mapping);
            if(existing != null) {
                return existing;
            }
            return new ArrayList<>(this.pointsRootRow);
        }
    }

    @Override
    public void paint(Graphics2D g) {
        final String title = vertex.toString();

        double height;
        double width;

        final Rectangle2D boundsTitle = g.getFontMetrics().getStringBounds(title, g);
        height = boundsTitle.getHeight();
        //TODO: Adjust baseline width for images
        width = boundsTitle.getWidth();

        //Hack for calculating row height
        final int heightRow = (int)height;

        if(this.showChildElements) {
            for(final LogicalVertex child : this.vertex.getChildAddresses()) {
                final Rectangle2D boundsChild = g.getFontMetrics().getStringBounds(child.getLogicalAddress().toString(), g);
                height += boundsChild.getHeight();
                width = Math.max(width, boundsChild.getWidth());
            }
        }

        //We need to cache the bounds when we draw so that operations that need the bounds can get it.
        this.bounds = new Rectangle2D.Double(width / -2.0, height / -2.0, width, height);

        //Recalculate the root row bind locations
        //TODO: updating the existing list is inadequate--the collection reference needs to change.
        synchronized(this.edgePoints) {
            this.pointsRootRow.clear();
            //TODO: Don't need to construct new point objects--awt points are not immutable.
            this.pointsRootRow.addAll(Arrays.asList(
                    new Point((int) (this.bounds.getMaxX() - boundsTitle.getWidth() + this.locationX), (int) (boundsTitle.getHeight() / 2.0 + bounds.getY() + this.locationY)),
                    new Point((int) (this.bounds.getMaxX() + this.locationX), (int) (boundsTitle.getHeight() / 2.0 + bounds.getY() + this.locationY))
            ));
            //top and bottom are only available if the children are hidden--top connection through children looks odd, bottom is just wrong at that point.
            if (!this.showChildElements) {
                this.pointsRootRow.add(new Point((int) (this.bounds.getMaxX() - boundsTitle.getWidth() / 2.0 + this.locationX), (int) (bounds.getMaxY() + this.locationY)));
                this.pointsRootRow.add(new Point((int) (this.bounds.getMaxX() - boundsTitle.getWidth() / 2.0 + this.locationX), (int) (bounds.getY() + this.locationY)));
            }
        }

        double topRow = 0.0;

        final AffineTransform transformOriginal = g.getTransform();
        g.translate(this.bounds.getX() + this.locationX, this.bounds.getY() + this.locationY);
        g.setStroke(new BasicStroke(0.0f));

        {
            //TODO: Identify images to draw
            final javafx.scene.paint.Color fxBkg = VertexColorAssignment.backgroundColorFor(vertex.getRootLogicalAddressMapping().getClass()).getValue();
            final javafx.scene.paint.Color fxText = VertexColorAssignment.textColorFor(vertex.getRootLogicalAddressMapping().getClass()).getValue();
            topRow += drawRow(width, topRow, vertex.toString(), new Color((float) fxBkg.getRed(), (float) fxBkg.getGreen(), (float) fxBkg.getBlue()), new Color((float) fxText.getRed(), (float) fxText.getGreen(), (float) fxText.getBlue()), g).getHeight();
        }
        synchronized(this.edgePoints) {
            this.edgePoints.clear();
            if (this.showChildElements) {
                for (final LogicalVertex child : this.vertex.getChildAddresses()) {
                    //TODO: Set background/font colors based on coloring rules
                    final javafx.scene.paint.Color fxBkg = VertexColorAssignment.backgroundColorFor(child.getLogicalAddress().getClass()).getValue();
                    final javafx.scene.paint.Color fxText = VertexColorAssignment.textColorFor(child.getLogicalAddress().getClass()).getValue();
                    final Rectangle2D boundsChild = drawRow(width, topRow, child.getLogicalAddress().toString(), new Color((float) fxBkg.getRed(), (float) fxBkg.getGreen(), (float) fxBkg.getBlue()), new Color((float) fxText.getRed(), (float) fxText.getGreen(), (float) fxText.getBlue()), g);
                    topRow += boundsChild.getHeight();

                    final ArrayList<Point> points = new ArrayList<>(2);
                    points.add(new Point((int)(this.bounds.getMaxX() - boundsChild.getWidth() + this.locationX), (int)(boundsChild.getHeight() / 2.0 + bounds.getY() + this.locationY)));
                    points.add(new Point((int)(this.bounds.getMaxX() + this.locationX), (int)(boundsChild.getHeight() / 2.0 + bounds.getY() + this.locationY)));
                    this.edgePoints.put(child.getLogicalAddressMapping(), points);
                }
            }
        }

        g.setTransform(transformOriginal);
    }

    protected static Rectangle2D drawRow(final double width, final double top, final String text, final Color colorBkg, final Color colorText, final Graphics2D g, final Image... icons) {
        //TODO: Support image drawing
        final Rectangle2D boundsText = g.getFontMetrics().getStringBounds(text, g);
        //The rectangle is right-aligned
        g.setColor(colorBkg);
        g.fillRoundRect((int)(width - boundsText.getWidth()), (int)top, (int)boundsText.getWidth(), (int)boundsText.getHeight(), 5, 5);
        g.setColor(colorText);
        g.drawString(text, (int)(width - boundsText.getWidth()), (int)(top - boundsText.getY()));

        return boundsText;
    }

    @Override
    public Rectangle2D getBounds() {
        return this.bounds;
    }

    @Override
    public void moveTo(final double x, final double y) {
        this.locationX = x;
        this.locationY = y;
    }
    @Override
    public double getTranslateX() {
        return this.locationX;
    }
    @Override
    public double getTranslateY() {
        return this.locationY;
    }
    @Override
    public void setTranslateX(final double x) {
        this.locationX = x;
    }
    @Override
    public void setTranslateY(final double y) {
        this.locationY = y;
    }
}
