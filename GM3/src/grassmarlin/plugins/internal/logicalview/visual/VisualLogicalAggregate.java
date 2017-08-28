package grassmarlin.plugins.internal.logicalview.visual;

import com.sun.istack.internal.NotNull;
import com.sun.javafx.collections.ObservableSetWrapper;
import grassmarlin.plugins.internal.logicalview.FilteredLogicalGraph;
import grassmarlin.plugins.internal.logicalview.ICanHasContextMenu;
import grassmarlin.session.serialization.XmlSerializable;
import grassmarlin.ui.common.IDraggable;
import grassmarlin.ui.common.TranslucentColorExpression;
import grassmarlin.ui.common.ZoomableScrollPane;
import grassmarlin.ui.common.menu.ColorSelectionMenuItem;
import javafx.application.Platform;
import javafx.beans.binding.BooleanExpression;
import javafx.beans.binding.DoubleExpression;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableSet;
import javafx.collections.SetChangeListener;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.control.MenuItem;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Polygon;
import javafx.scene.transform.NonInvertibleTransformException;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import java.util.*;
import java.util.stream.Collectors;

public class VisualLogicalAggregate implements ZoomableScrollPane.IMultiLayeredNode, ICanHasContextMenu, XmlSerializable {
    private class AggregateHull extends Polygon implements IDraggable {
        private final DoubleExpression ZERO = new SimpleDoubleProperty(0.0);

        public AggregateHull() {
            super();

            this.makeDraggable(true);

            this.translateXProperty().bind(ZERO);
            this.translateYProperty().bind(ZERO);
        }

        @Override
        public void handleMousePressed(MouseEvent event) {
            if (event.isPrimaryButtonDown()) {
                try {
                    this.dragContext.ptOrigin = this.getParent().getLocalToSceneTransform().inverseTransform(event.getSceneX(), event.getSceneY());
                } catch (NonInvertibleTransformException ex) {
                    ex.printStackTrace();
                    this.dragContext.ptOrigin = new Point2D(event.getSceneX(), event.getSceneY());
                }
                this.dragContext.ptPrevious = new Point2D(0, 0);
            }
        }

        @Override
        public void handleMouseDragged(MouseEvent event) {
            // The primary button has to be down
            // The drag target only has to match if we are going to process the drag; if dragging is disabled then that check will be handled elsewhere
            if (event.isPrimaryButtonDown() && event.getSource() == this) {
                event.consume();

                Point2D ptTemp = new Point2D(event.getSceneX(), event.getSceneY());
                try {
                    ptTemp = this.getParent().getLocalToSceneTransform().inverseTransform(ptTemp);
                } catch (NonInvertibleTransformException ex) {
                    ex.printStackTrace();
                    // We just won't be able to account for the translation. There may be some distortion, but it will still work.
                }

                final Point2D ptTranslated = ptTemp.subtract(dragContext.ptOrigin);

                for(final VisualLogicalVertex member : VisualLogicalAggregate.this.members) {
                    member.setTranslateX(member.getTranslateX() - dragContext.ptPrevious.getX() + ptTranslated.getX());
                    member.setTranslateY(member.getTranslateY() - dragContext.ptPrevious.getY() + ptTranslated.getY());
                }

                // To avoid floating point rounding errors we will reverse this translation and apply a new absolute translation next time.
                dragContext.ptPrevious = ptTranslated;
            }
        }

    }

    private final LogicalVisualization visualization;
    private final FilteredLogicalGraph graph;
    private final String property;
    private final Object value;

    private final ObjectProperty<Color> colorFill;
    private final ObjectProperty<Color> colorBorder;

    private final ObservableSet<VisualLogicalVertex> members;

    private final AggregateHull polyAggregate;
    private final Pane label;
    private final ZoomableScrollPane.AltClickableWrapper wrapperAggregate;
    private final ZoomableScrollPane.AltClickableWrapper wrapperLabel;

    private final BooleanExpression isLayerActive;

    private final MenuItem miChangeBackgroundColor;
    private final MenuItem miChangeBorderColor;

    public VisualLogicalAggregate(final LogicalVisualization visualization, final FilteredLogicalGraph graph, final String property, final Object value) {
        this.visualization = visualization;
        this.graph = graph;
        this.property = property;
        this.value = value;

        this.members = new ObservableSetWrapper<>(new HashSet<>());
        this.members.addListener(handler_membershipChanged);

        this.isLayerActive = graph.groupingProperty().isEqualTo(this.property);

        this.polyAggregate = new AggregateHull();
        this.label = new Pane();
        this.wrapperAggregate = new ZoomableScrollPane.AltClickableWrapper(this.polyAggregate, this);
        this.wrapperLabel = new ZoomableScrollPane.AltClickableWrapper(this.label, this);

        this.colorBorder = new SimpleObjectProperty<>(visualization.getColorFactory().getBorderColor(value));
        this.colorFill = new SimpleObjectProperty<>(visualization.getColorFactory().getBackgroundColor(value));

        initComponents();

        this.miChangeBackgroundColor = new ColorSelectionMenuItem(this.colorFill, "Change Background Color of " + this.value.toString(), () -> {
            this.visualization.markAsModified();
        });
        this.miChangeBorderColor = new ColorSelectionMenuItem(this.colorBorder, "Change Border Color of " + this.value.toString(), () -> {
            this.visualization.markAsModified();
        });
    }

    private void initComponents() {
        //TODO: Bind opacity to a configurable value
        this.polyAggregate.fillProperty().bind(new TranslucentColorExpression(this.colorFill, new SimpleDoubleProperty(0.6)));
        this.polyAggregate.strokeProperty().bind(this.colorBorder);
        this.polyAggregate.setStrokeWidth(2.0);

        //TODO: the visibility of the component objects for aggregates needs a little more depth, specifically to handle the optional display of the label and the collapse state.
        this.polyAggregate.visibleProperty().bind(this.isLayerActive);
        this.label.visibleProperty().bind(this.isLayerActive);
    }

    public void clearMembers() {
        this.members.stream().forEach(member -> member.layoutBoundsProperty().removeListener(this.handler_memberCoordinateChanged));
        this.members.stream().forEach(member -> member.boundsInParentProperty().removeListener(this.handler_memberCoordinateChanged));
        this.members.clear();
    }

    protected void addMember(final VisualLogicalVertex visual) {
        if(visual != null && members.add(visual)) {
            visual.layoutBoundsProperty().addListener(this.handler_memberCoordinateChanged);
            visual.boundsInParentProperty().addListener(this.handler_memberCoordinateChanged);
        }
    }
    protected void removeMember(final VisualLogicalVertex visual) {
        if(visual != null && members.remove(visual)) {
            visual.layoutBoundsProperty().removeListener(this.handler_memberCoordinateChanged);
            visual.boundsInParentProperty().removeListener(this.handler_memberCoordinateChanged);
        }
    }


    protected final SetChangeListener<VisualLogicalVertex> handler_membershipChanged = this::Handle_membershipChanged;
    protected final ChangeListener<Bounds> handler_memberCoordinateChanged = this::Handle_memberCoordinateChanged;
    private void Handle_membershipChanged(SetChangeListener.Change<? extends VisualLogicalVertex> change) {
        this.rebuildHull();
    }
    private void Handle_memberCoordinateChanged(ObservableValue<? extends Bounds> observable, Bounds oldValue, Bounds newValue) {
        this.rebuildHull();
    }

    protected void rebuildHull() {
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

        final List<Double> ptsHull = hullFor(points);
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
    public Node nodeForLayer(final String layer) {
        switch(layer) {
            case LogicalVisualization.LAYER_AGGREGATES:
                return this.wrapperAggregate;
            case LogicalVisualization.LAYER_NODES:
                return this.wrapperLabel;
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
        //TODO: Serialization/deserialization of VisualLogicalAggregate objects needs to handle non-color values and color-with-alpha values better.
        writer.writeAttribute("ColorBorder", colorBorder.get().toString());
        writer.writeAttribute("ColorFill", colorFill.get().toString());
    }
}
