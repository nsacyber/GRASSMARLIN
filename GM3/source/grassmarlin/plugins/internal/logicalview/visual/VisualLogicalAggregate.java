package grassmarlin.plugins.internal.logicalview.visual;

import com.sun.istack.internal.NotNull;
import grassmarlin.plugins.internal.logicalview.ICanHasContextMenu;
import grassmarlin.session.Property;
import grassmarlin.session.serialization.XmlSerializable;
import grassmarlin.ui.common.IAltClickable;
import grassmarlin.ui.common.IDraggable;
import grassmarlin.ui.common.TranslucentColorExpression;
import grassmarlin.ui.common.ZoomableScrollPane;
import grassmarlin.ui.common.menu.ColorSelectionMenuItem;
import javafx.application.Platform;
import javafx.beans.binding.DoubleExpression;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.control.MenuItem;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Polygon;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import java.util.*;
import java.util.stream.Collectors;

public class VisualLogicalAggregate implements ZoomableScrollPane.IMultiLayeredNode, ICanHasContextMenu, XmlSerializable {
    private class AggregateHull extends Polygon implements IDraggable, IAltClickable {
        private final DoubleExpression ZERO = new SimpleDoubleProperty(0.0);
        private final IDraggable.DragContext context = new DragContext();

        public AggregateHull() {
            super();

            this.makeDraggable(true);

            this.translateXProperty().bind(ZERO);
            this.translateYProperty().bind(ZERO);

            this.setOnMouseReleased(event -> {
                if(event.isShiftDown()) {
                    for(final VisualLogicalVertex vertex : VisualLogicalAggregate.this.findMembers()) {
                        vertex.setSubjectToLayout(true);
                    }
                }
            });
        }

        @Override
        public DragContext getDragContext() {
            return this.context;
        }

        @Override
        public void startDrag(final Point2D worldLocation) {
            for(final VisualLogicalVertex vertex : VisualLogicalAggregate.this.findMembers()) {
                vertex.setSubjectToLayout(false);
            }
            IDraggable.super.startDrag(worldLocation);
        }

        @Override
        public void dragTo(final Point2D worldTarget) {
            final Point2D ptTranslated = worldTarget.subtract(this.getDragContext().ptOrigin);

            for(final VisualLogicalVertex member : VisualLogicalAggregate.this.findMembers()) {
                member.setTranslateX(member.getTranslateX() - this.getDragContext().ptPrevious.getX() + ptTranslated.getX());
                member.setTranslateY(member.getTranslateY() - this.getDragContext().ptPrevious.getY() + ptTranslated.getY());
            }

            // To avoid floating point rounding errors we will reverse this translation and apply a new absolute translation next time.
            this.getDragContext().ptPrevious = ptTranslated;
        }

        @Override
        public List<Object> getRespondingNodes(Point2D point) {
            return Collections.singletonList(VisualLogicalAggregate.this);
        }
    }

    private final LogicalVisualization visualization;
    private final String property;
    private final Property<?> value;

    private final ObjectProperty<Color> colorFill;
    private final ObjectProperty<Color> colorBorder;

    private final AggregateHull polyAggregate;
    private final Pane label;

    private final MenuItem miChangeBackgroundColor;
    private final MenuItem miChangeBorderColor;

    public VisualLogicalAggregate(final LogicalVisualization visualization, final String property, final Property<?> value) {
        this.visualization = visualization;
        this.property = property;
        this.value = value;

        this.polyAggregate = new AggregateHull();
        this.label = new Pane();
        //TODO: Add content to label.

        this.colorBorder = new SimpleObjectProperty<>(visualization.getColorFactory().getBorderColor(value));
        this.colorFill = new SimpleObjectProperty<>(visualization.getColorFactory().getBackgroundColor(value));

        this.polyAggregate.fillProperty().bind(new TranslucentColorExpression(this.colorFill, new SimpleDoubleProperty(0.6)));
        this.polyAggregate.strokeProperty().bind(this.colorBorder);
        this.polyAggregate.setStrokeWidth(2.0);

        this.miChangeBackgroundColor = new ColorSelectionMenuItem(this.colorFill, "Change Background Color of " + this.value.toString(), () -> {
            this.visualization.markAsModified();
        });
        this.miChangeBorderColor = new ColorSelectionMenuItem(this.colorBorder, "Change Border Color of " + this.value.toString(), () -> {
            this.visualization.markAsModified();
        });
    }

    public void requestLayout() {
        this.rebuildHull();
    }

    protected List<VisualLogicalVertex> findMembers() {
        return this.visualization.getVertices().stream()
                .filter(vertex -> {
                    final Set<Property<?>> set = vertex.getVertex().getProperties().get(VisualLogicalAggregate.this.property);
                    if(set != null) {
                        return set.contains(VisualLogicalAggregate.this.value);
                    } else {
                        return false;
                    }
                })
                .collect(Collectors.toList());
    }

    protected void rebuildHull() {
        final List<VisualLogicalVertex> members = findMembers();
        final List<Point2D> points = new ArrayList<>(members.size() * 4);
        for(VisualLogicalVertex vertex : members) {
            if(!vertex.isVisible()) {
                continue;
            }
            final Bounds bounds = vertex.getLayoutBounds();
            final double offsetX = vertex.getLayoutX() + vertex.getTranslateX();
            final double offsetY = vertex.getLayoutY() + vertex.getTranslateY();

            points.add(new Point2D(offsetX, offsetY));
            points.add(new Point2D(offsetX, offsetY + bounds.getHeight()));
            points.add(new Point2D(offsetX + bounds.getWidth(), offsetY));
            points.add(new Point2D(offsetX + bounds.getWidth(), offsetY + bounds.getHeight()));
        }

        final List<Double> ptsHull = VisualLogicalAggregate.hullFor(points);
        polyAggregate.getPoints().clear();
        if(ptsHull != null) {
            polyAggregate.getPoints().addAll(ptsHull);
        }
    }

    protected static List<Double> hullFor(@NotNull List<Point2D> points) {
        final Stack<Point2D> ptsPolygon = new Stack<>();

        //Remove invalid points, remove duplicates.
        points = points.stream().filter(pt -> !Double.isNaN(pt.getX()) && !Double.isNaN(pt.getY())).distinct().collect(Collectors.toList());
        //Without at least 3 distinct points, we will not be able to draw anything meaningful.
        if(points.size() < 3) {
            return null;
        }
        //Y check is inverted relative to X because the screen uses non-cartesian coordinates and the algorithm expects cartesian coordinates.
        points.sort((o1, o2) ->
                (o1.getX() == o2.getX()) ?
                        (o1.getY() == o2.getY()) ?
                                0 :
                                (o1.getY() > o2.getY() ? -1 : 1) :
                        (o1.getX() < o2.getX() ? -1 : 1)
        );

        //Add the points, in order, to the polygon.
        //For each point, remove all immediately-preceding points that are either "inside" or colinear
        for(int idxPoint = 0; idxPoint < points.size(); idxPoint++) {
            while(ptsPolygon.size() >= 2 &&
                    ptsPolygon.get(ptsPolygon.size() - 1).subtract(ptsPolygon.get(ptsPolygon.size() - 2))
                            .crossProduct(points.get(idxPoint).subtract(ptsPolygon.get(ptsPolygon.size() - 2)))
                            .getZ() <= 0) {
                ptsPolygon.pop();
            }
            ptsPolygon.push(points.get(idxPoint));
        }
        // And now make a return trip.  Same logic, just reverse order.  Also, we skip the last point, since it would be redundant.
        for(int idxPoint = points.size() - 2; idxPoint >= 0; idxPoint--) {
            while(ptsPolygon.size() >= 2 &&
                    ptsPolygon.get(ptsPolygon.size() - 1).subtract(ptsPolygon.get(ptsPolygon.size() - 2))
                            .crossProduct(points.get(idxPoint).subtract(ptsPolygon.get(ptsPolygon.size() - 2)))
                            .getZ() <= 0) {
                ptsPolygon.pop();
            }
            ptsPolygon.push(points.get(idxPoint));
        }
        ptsPolygon.pop();   //The last point will be a duplicate of the first.

        final List<Double> result = new ArrayList<>(ptsPolygon.size() * 2);
        for(Point2D pt : ptsPolygon) {
            result.add(pt.getX());
            result.add(pt.getY());
        }
        return result;
    }

    @Override
    public Node nodeForLayer(String layer) {
        switch(layer) {
            case LogicalVisualization.LAYER_AGGREGATES:
                return this.polyAggregate;
            case LogicalVisualization.LAYER_NODES:
                return this.label;
            default:
                return null;
        }
    }


    @Override
    public List<MenuItem> getContextMenuItems() {
        return Arrays.asList(this.miChangeBackgroundColor, this.miChangeBorderColor);
    }

    @Override
    public void readFromXml(final XMLStreamReader source) throws XMLStreamException {
        final String strColorBorder = source.getAttributeValue(null, "ColorBorder");
        final String strColorFill = source.getAttributeValue(null, "ColorFill");
        //HACK: This assumes colors, but there are valid alternatives.  Also, it ignores the alpha component that is expected to be present for the fill.
        final Color colorBorder = Color.web(strColorBorder.substring(2, 8));
        final Color colorFill = Color.web(strColorFill.substring(2, 8));

        Platform.runLater(() -> {
            VisualLogicalAggregate.this.colorBorder.set(colorBorder);
            VisualLogicalAggregate.this.colorFill.set(colorFill);
        });
    }

    @Override
    public void writeToXml(final XMLStreamWriter writer) throws XMLStreamException {
        writer.writeAttribute("ColorBorder", colorBorder.get().toString());
        writer.writeAttribute("ColorFill", colorFill.get().toString());
    }

    @Override
    public String toString() {
        return String.format("[VisualLogicalAggregate: Points:(%s) Fill:(%s) Border:(%s)]", this.polyAggregate.getPoints(), this.colorFill.get(), this.colorBorder.get());
    }
}
