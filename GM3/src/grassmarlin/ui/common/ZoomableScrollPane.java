package grassmarlin.ui.common;

import com.sun.istack.internal.Nullable;
import grassmarlin.plugins.internal.logicalview.ICanHasContextMenu;
import grassmarlin.plugins.internal.logicalview.visual.VisualLogicalVertex;
import grassmarlin.session.serialization.XmlSerializable;
import grassmarlin.ui.common.menu.ActiveMenuItem;
import javafx.geometry.BoundingBox;
import javafx.geometry.Bounds;
import javafx.geometry.Point2D;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.MenuItem;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.ScrollEvent;
import javafx.scene.layout.Pane;
import javafx.scene.transform.NonInvertibleTransformException;
import javafx.scene.transform.Scale;
import javafx.scene.transform.Translate;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class ZoomableScrollPane extends Pane implements IAltClickable, ICanHasContextMenu, XmlSerializable {
    public static class AltClickableWrapper extends Group implements IAltClickable {
        private final Object owner;

        public AltClickableWrapper(final Node element, final Object owner) {
            this.getChildren().add(element);
            this.owner = owner;
        }

        @Override
        public List<Object> getRespondingNodes(Point2D point) {
            return Arrays.asList(this.owner);
        }
    }

    @FunctionalInterface
    public interface IMultiLayeredNode {
        Node nodeForLayer(final String layer);
    }
    private static class DragContext {
        double x;
        double y;
    }

    private Scale scaleTransform;
    private Translate translateTransform;

    private final static double SCALE_FACTOR = 1.1;
    //Track the current scale as an integer to avoid precision-related errors when zooming in and out.
    //The scaling is computed as getScaleValue() := Math.pow(scaleFactor, scaleLevel)
    private int scaleLevel;
    private final static int SCALE_MIN = -48;   // 1.1^-48 ~= 0.01x
    private final static int SCALE_MAX = 48;    // 1.1^48 ~= 100x

    private final DragContext dragContext;
    private final BiConsumer<List<Object>, Point2D> altClickConsumer;
    private final Consumer<Point2D> clickConsumer;
    protected final Group zoomGroup;
    private final LinkedHashMap<String, Group> layers;

    private boolean isBeingDragged;

    public ZoomableScrollPane(@Nullable final BiConsumer<List<Object>, Point2D> altClickConsumer, @Nullable final Consumer<Point2D> clickConsumer, String... layerNames) {
        this(altClickConsumer, clickConsumer);

        for(final String name : layerNames) {
            final Group group = new Group();
            this.layers.put(name, group);
            this.zoomGroup.getChildren().add(group);
        }
    }
    public ZoomableScrollPane(@Nullable final BiConsumer<List<Object>, Point2D> altClickConsumer, @Nullable final Consumer<Point2D> clickConsumer) {
        this.scaleTransform = new Scale(getScaleValue(), getScaleValue(), 0, 0);
        this.translateTransform = new Translate();
        this.altClickConsumer = altClickConsumer;
        this.clickConsumer = clickConsumer;
        this.zoomGroup = new Group();
        this.layers = new LinkedHashMap<>();
        zoomGroup.getTransforms().addAll(this.translateTransform, this.scaleTransform);

        //This object is responsible for handling panning operations, and does so by manipulation translateTransform
        this.setOnMousePressed(this::handleMousePressed);
        this.setOnMouseDragged(this::handleMouseDragged);
        this.setOnScroll(this::handleMouseWheelZoom);

        this.scaleLevel = 0;
        this.dragContext = new DragContext();

        this.getChildren().add(this.zoomGroup);
    }

    public boolean addLayeredChild(final IMultiLayeredNode root) {
        boolean result = false;
        for(Map.Entry<String, Group> layer : layers.entrySet()) {
            final Node content = root.nodeForLayer(layer.getKey());
            if(content != null) {
                if(!layer.getValue().getChildren().contains(content)) {
                    result |= layer.getValue().getChildren().add(content);
                }
            }
        }
        return result;
    }

    public void clear() {
        this.zoomGroup.getChildren().clear();
        for (Group group : layers.values()) {
            group.getChildren().clear();
        }
        this.zoomGroup.getChildren().addAll(layers.values());
    }

    protected Group getLayer(String layer) {
        return this.layers.get(layer);
    }

    /**
     * Add a child node
     *
     * @param child The node to add
     * @return <code>true</code> if added, <code>false</code> otherwise
     */
    public void addChild(final Node child) {
        this.zoomGroup.getChildren().add(child);
    }

    public void addChild(final Node child, final String layer) {
        this.layers.get(layer).getChildren().add(child);
    }

    public boolean removeLayeredChild(final IMultiLayeredNode root) {
        boolean result = false;
        for(Map.Entry<String, Group> layer : layers.entrySet()) {
            final Node content = root.nodeForLayer(layer.getKey());
            if(content != null) {
                result |= layer.getValue().getChildren().remove(content);
            }
        }
        return result;
    }

    public boolean removeChild(final Node child) {
        return this.zoomGroup.getChildren().remove(child);
    }

    public boolean removeChild(final Node child, final String layer) {
        return this.layers.get(layer).getChildren().remove(child);
    }

    public boolean removeAll(final Node... children) {
        final Group group = this.zoomGroup;
        boolean result = false;
        for(final Node node : children) {
            if(node instanceof IMultiLayeredNode) {
                for(Map.Entry<String, Group> entry : this.layers.entrySet()) {
                    final Node nodeForLayer = ((IMultiLayeredNode)node).nodeForLayer(entry.getKey());
                    if(nodeForLayer != null) {
                        result |= entry.getValue().getChildren().remove(nodeForLayer);
                    }
                }
            } else {
                result |= zoomGroup.getChildren().remove(node);
            }
        }
        return result;
    }

    public boolean removeAll(final String layer, Node... children) {
        boolean result = false;
        for(final Node node : children) {
            if(node instanceof IMultiLayeredNode) {
                for(Map.Entry<String, Group> entry : this.layers.entrySet()) {
                    final Node nodeForLayer = ((IMultiLayeredNode)node).nodeForLayer(entry.getKey());
                    if(nodeForLayer != null) {
                        result |= entry.getValue().getChildren().remove(nodeForLayer);
                    }
                }
            } else {
                final Group group = layers.get(layer);
                result |= group.getChildren().remove(node);
            }
        }
        return result;
    }

    public double getScaleValue(int level) {
        return Math.pow(SCALE_FACTOR, level);
    }

    public double getScaleValue() {
        return Math.pow(SCALE_FACTOR, this.scaleLevel);
    }

    public int getScaleLevel(double value) {
        //Floor and cast_to_int are not identical, as cast-to-int rounds closer to 0, which matters when dealing with negative values
        return (int) Math.floor(Math.log(value) / Math.log(SCALE_FACTOR));
    }

    private void handleMousePressed(MouseEvent event) {
        isBeingDragged = false;
        if(event.isPrimaryButtonDown()) {
            event.consume(); //Tasted like delicious, imaginary, deceitful cake.
            dragContext.x = translateTransform.getX() - event.getScreenX();
            dragContext.y = translateTransform.getY() - event.getScreenY();
            isBeingDragged = true;
            if (clickConsumer != null) {
                clickConsumer.accept(new Point2D(event.getScreenX(), event.getScreenY()));
            }
        } else if(event.isSecondaryButtonDown()) {
            event.consume();
            // create list of objects below cursor and pass to consumer
            List<Object> nodeList = new ArrayList<>();

            // Add menu items for zoomable scroll pane controls
            nodeList.add(this);

            Point2D ptEvent = this.zoomGroup.screenToLocal(event.getScreenX(), event.getScreenY());
            for (Node child : this.zoomGroup.getChildrenUnmodifiable()) {
                if (child instanceof IAltClickable && child.getBoundsInParent().contains(ptEvent)) {
                    nodeList.addAll(((IAltClickable) child).getRespondingNodes(child.parentToLocal(ptEvent)));
                }
            }
            for(Group layer : layers.values()) {
                for (Node child : layer.getChildrenUnmodifiable()) {
                    if (child instanceof IAltClickable && child.getBoundsInParent().contains(ptEvent)) {
                        nodeList.addAll(((IAltClickable) child).getRespondingNodes(child.parentToLocal(ptEvent)));
                    }
                }
            }

            if (altClickConsumer != null) {
                altClickConsumer.accept(nodeList, new Point2D(event.getScreenX(), event.getScreenY()));
            }
        }
    }

    private void handleMouseDragged(MouseEvent event) {
        if (event.isPrimaryButtonDown() && !event.isShiftDown() && isBeingDragged) {
            event.consume();
            // X,Y += The screen delta between the click and current
            translateTransform.setX(event.getScreenX() + dragContext.x);
            translateTransform.setY(event.getScreenY() + dragContext.y);
        } else {
            isBeingDragged = false;
        }
    }

    private void handleMouseWheelZoom(ScrollEvent event) {
        int scaleNew = scaleLevel;
        double delta = event.getDeltaY();
        // Shift+scroll scrolls X instead of Y
        if (event.isShiftDown()) {
            delta = event.getDeltaX();
        }

        if (delta < 0) {
            scaleNew--;
        } else if (delta > 0) {
            scaleNew++;
        }
        final Point2D ptEvent = this.screenToLocal(event.getScreenX(), event.getScreenY());
        this.zoomTo(scaleNew, ptEvent);
        event.consume();
    }

    public void centerOn(double x, double y) {
        // Translate to inverse of point on which it should focus
        // Since translate defines top-left, we want to take (point - half_screen), the additive inverse of which is (half_screen - point)
        translateTransform.setX((getWidth() / 2.0) - (x * getScaleValue()));
        translateTransform.setY((getHeight() / 2.0) - (y * getScaleValue()));
    }

    public void zoomReset() {
        // Zoom to 1:1 and put 0,0 in the center (this isn't the default (which is 0,0) in the top left), but it makes sense)
        scaleLevel = 0;
        scaleTransform.setX(1.0);
        scaleTransform.setY(1.0);
        centerOn(0.0, 0.0);
    }

    public void zoomToVertex(final VisualLogicalVertex vertex) {
        // Zoom to 1:1 and put the center of the vertex in the center of the display.
        scaleLevel = 0;
        scaleTransform.setX(1.0);
        scaleTransform.setY(1.0);
        if(vertex == null) {
            this.centerOn(0.0, 0.0);
        } else {
            this.centerOn(vertex.getTranslateX() + vertex.getWidth() / 2.0, vertex.getTranslateY() + vertex.getHeight() / 2.0);
        }
    }

    public void zoomTo(int scaleLevel, Point2D ptViewport) {
        this.zoomTo(scaleLevel, ptViewport.getX(), ptViewport.getY());
    }

    public void zoomTo(int scaleLevel) {
        // Zoom around the center of the display
        Bounds bounds = getBoundsInLocal();
        this.zoomTo(scaleLevel, bounds.getWidth() / 2.0, bounds.getHeight() / 2.0);
    }

    public void zoomTo(int scaleLevel, double viewportX, double viewportY) {
        try {
            // Clamp the scaleLevel to the permitted range
            if (scaleLevel < SCALE_MIN) {
                scaleLevel = SCALE_MIN;
            } else if (scaleLevel > SCALE_MAX) {
                scaleLevel = SCALE_MAX;
            }

            /*
                Viewport coordinates are screen coordinates relative to the top left of the ZoomableScrollPane
                Each viewport coordinate can be translated to a world coordinate, which is the coordinate system used by the
                visual elements; this is done by performing the transforms that are applied to zoomPane to the given viewport
                point
             */
            Point2D worldCursorPre = scaleTransform.inverseTransform(translateTransform.inverseTransform(viewportX, viewportY));

            this.scaleLevel = scaleLevel;
            scaleTransform.setX(getScaleValue());
            scaleTransform.setY(getScaleValue());

            // Since we want the viewportX,Y point to remain unchanged, find the point under that location and update the
            // translate transformation by the difference.
            Point2D worldCursorPost = scaleTransform.inverseTransform(translateTransform.inverseTransform(viewportX, viewportY));

            translateTransform.setX(translateTransform.getX() + getScaleValue() * (worldCursorPost.getX() - worldCursorPre.getX()));
            translateTransform.setY(translateTransform.getY() + getScaleValue() * (worldCursorPost.getY() - worldCursorPre.getY()));
        } catch (NonInvertibleTransformException wontHappen) {
            // scaleTransform.inverseTransform might throw a NonInvertibleTransformException, but only does so if you
            // apply an inverse 3d transform to a 2d point.
        }
    }

    /**
     * Like zoomToFit, this fills the screen with the content, but does so by repositioning the content rather than
     * changing the zoom and translation transforms. This results in nodes remaining unscaled and moving closer or
     * farther from each other permitting, for example, more room for details to be displayed without overlapping.
     *
     * This will distort the graph as the X and Y axes are scaled independently.
     *
     * There is some error but the result is close enough that attention is being spent elsewhere
     */
    public void scaleToWindow() {
        Bounds boundsWorld = calculateNodeBounds();

        if (boundsWorld == null || boundsWorld.getWidth() == 0.0 && boundsWorld.getHeight() == 0.0) {
            zoomReset();
        } else {
            // Get the (translated and scaled) coordinates for the top left and width/height
            Point2D ptTopLeft = new Point2D(
                    -translateTransform.getX() / scaleTransform.getX(),
                    -translateTransform.getY() / scaleTransform.getY()
            );
            double width = getWidth() / scaleTransform.getX();
            double height = getHeight() / scaleTransform.getY();

            for (Node node : this.getChildrenUnmodifiable()) {
                this.setNodeBounds(node, boundsWorld, ptTopLeft, width, height);
            }
        }
    }

    private void setNodeBounds(Node node, Bounds boundsWorld, Point2D topLeft, double scaledWidth, double scaledHeight) {
        // Start from the top left, normalized against the bounds
        if (node instanceof IDraggable && !node.translateXProperty().isBound() && !node.translateYProperty().isBound()) {
            double x = node.getTranslateX() - boundsWorld.getMinX();
            double y = node.getTranslateY() - boundsWorld.getMinY();
            x /= boundsWorld.getWidth();
            y /= boundsWorld.getHeight();
            // Rescale to viewport
            x *= scaledWidth;
            y *= scaledHeight;
            // Set scaled value shifted by viewport location
            node.setTranslateX(x + topLeft.getX());
            node.setTranslateY(y + topLeft.getY());
        }

        if (node instanceof Parent && !((Parent)node).getChildrenUnmodifiable().isEmpty()) {
            for (Node child : ((Parent) node).getChildrenUnmodifiable()) {
                setNodeBounds(child, boundsWorld, topLeft, scaledWidth, scaledHeight);
            }
        }
    }

    protected Bounds calculateNodeBounds() {
        List<IDraggable> draggableChildren = this.zoomGroup.getChildrenUnmodifiable().stream()
                .filter(child -> child instanceof IDraggable)
                .map(child -> (IDraggable)child)
                .collect(Collectors.toList());
        for (Group layer : this.layers.values()) {
            draggableChildren.addAll(layer.getChildrenUnmodifiable().stream()
            .filter(child -> child instanceof IDraggable)
            .map(child -> (IDraggable)child)
            .collect(Collectors.toList()));
        }
        return calculateNodeBounds(draggableChildren);
    }

    protected static class Point {
        public double x;
        public double y;

        public Point(double val) {
            x = y = val;
        }
    }

    protected Bounds calculateNodeBounds(List<IDraggable> nodes) {
        if (nodes.isEmpty()) {
            return null;
        } else {
            Point ptTopLeft = new Point(Double.MAX_VALUE);
            Point ptBottomRight = new Point(-Double.MAX_VALUE);

            for (IDraggable draggable : nodes) {
                if (draggable instanceof Parent && !((Parent) draggable).getChildrenUnmodifiable().isEmpty()) {
                    List<IDraggable> draggableChildren = ((Parent) draggable).getChildrenUnmodifiable().stream()
                            .filter(child -> child instanceof IDraggable)
                            .map(child -> (IDraggable)child)
                            .collect(Collectors.toList());
                    if (!draggableChildren.isEmpty()) {
                        Bounds bounds = calculateNodeBounds(draggableChildren);
                        if (!Double.isNaN(bounds.getMinX())) {
                            ptTopLeft.x = Double.min(ptTopLeft.x, bounds.getMinX());
                            if (!Double.isNaN(bounds.getMaxX())) {
                                ptBottomRight.x = Double.max(ptBottomRight.x, bounds.getMaxX());
                            }
                        }
                        if (!Double.isNaN(bounds.getMinY())) {
                            ptTopLeft.y = Double.min(ptTopLeft.y, bounds.getMinY());
                            if (!Double.isNaN(bounds.getMaxY())) {
                                ptBottomRight.y = Double.max(ptBottomRight.y, bounds.getMaxY());
                            }
                        }
                    }
                }
                if (draggable instanceof Node) {
                    Node node = (Node)draggable;
                    Bounds bounds = node.getBoundsInParent();
                    if (bounds.getWidth() > 0 && bounds.getHeight() > 0) {
                        if (!Double.isNaN(node.getTranslateX())) {
                            ptTopLeft.x = Double.min(ptTopLeft.x, node.getTranslateX());
                            if (!Double.isNaN(bounds.getWidth())) {
                                ptBottomRight.x = Double.max(ptBottomRight.x, node.getTranslateX() + bounds.getWidth());
                            }
                        }
                        if (!Double.isNaN(node.getTranslateY())) {
                            ptTopLeft.y = Double.min(ptTopLeft.y, node.getTranslateY());
                            if (!Double.isNaN(bounds.getHeight())) {
                                ptBottomRight.y = Double.max(ptBottomRight.y, node.getTranslateY() + bounds.getHeight());
                            }
                        }
                    }
                }
            }

            return new BoundingBox(ptTopLeft.x, ptTopLeft.y, ptBottomRight.x - ptTopLeft.x, ptBottomRight.y - ptTopLeft.y);
        }
    }

    public void zoomToFit() {
        final Bounds boundsNodes = calculateNodeBounds();

        if(boundsNodes == null || boundsNodes.getWidth() == 0.0 || boundsNodes.getHeight() == 0.0) {
            //If either dimension occupies no space, then just reset the zoom--there is probably no content, and, if there is, something is horribly wrong with it.
            zoomReset();
        } else {
            final double scaleX = getWidth() / boundsNodes.getWidth();
            final double scaleY = getHeight() / boundsNodes.getHeight();

            final int scaleNew = getScaleLevel(Math.min(scaleX, scaleY));

            zoomTo(scaleNew);

            //Translate the newly-scaled graph so that it is (roughly) centered.
            translateTransform.setX(-getScaleValue() * boundsNodes.getMinX() + (getWidth() - (boundsNodes.getWidth() * getScaleValue())) / 2.0);
            translateTransform.setY(-getScaleValue() * boundsNodes.getMinY() + (getHeight() - (boundsNodes.getHeight() * getScaleValue())) / 2.0);
        }
    }

    protected void handleFitToWindow() {
        this.scaleToWindow();
    }

    protected void handleReset() {
        this.zoomReset();
    }

    @Override
    public List<MenuItem> getContextMenuItems() {
        List<MenuItem> items = new ArrayList<>();
        MenuItem zoomToFitItem = new ActiveMenuItem("Zoom To Fit", event -> this.zoomToFit());
        MenuItem scaleToWindow = new ActiveMenuItem("Fit To Window", event -> this.handleFitToWindow());
        MenuItem reset = new ActiveMenuItem("Reset Zoom", event -> this.handleReset());

        items.addAll(Arrays.asList(zoomToFitItem, scaleToWindow, reset));
        return items;
    }

    @Override
    public List<Object> getRespondingNodes(Point2D point) {
        return Arrays.asList(this);
    }

    @Override
    public void readFromXml(final XMLStreamReader reader) throws XMLStreamException {
        this.scaleLevel = Integer.parseInt(reader.getAttributeValue(null, "scale"));
        this.scaleTransform.setX(getScaleValue());
        this.scaleTransform.setY(getScaleValue());
        this.translateTransform.setX(Double.parseDouble(reader.getAttributeValue(null, "x")));
        this.translateTransform.setY(Double.parseDouble(reader.getAttributeValue(null, "y")));
        reader.next();
    }

    @Override
    public void writeToXml(final XMLStreamWriter writer) throws XMLStreamException {
        writer.writeAttribute("scale", Integer.toString(this.scaleLevel));
        writer.writeAttribute("x", Double.toString(this.translateTransform.getX()));
        writer.writeAttribute("y", Double.toString(this.translateTransform.getY()));
        writer.writeEndElement();
    }
}
