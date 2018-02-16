package grassmarlin.plugins.internal.physical.view.visualization;

import grassmarlin.Event;
import grassmarlin.plugins.internal.logicalview.ICanHasContextMenu;
import grassmarlin.plugins.internal.physical.view.IPhysicalViewApi;
import grassmarlin.plugins.internal.physical.view.data.PhysicalDevice;
import grassmarlin.plugins.internal.physical.view.data.PhysicalEndpoint;
import grassmarlin.session.HardwareVertex;
import grassmarlin.session.PhysicalConnection;
import grassmarlin.session.Property;
import grassmarlin.session.PropertyCloud;
import grassmarlin.ui.common.IAltClickable;
import grassmarlin.ui.common.IDraggable;
import grassmarlin.ui.common.ZoomableScrollPane;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.geometry.Bounds;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;

import java.util.*;
import java.util.stream.Collectors;

public class VisualDevice extends VBox implements IDraggable, ICanHasContextMenu, IAltClickable, ZoomableScrollPane.IMultiLayeredNode, IPhysicalElement {
    protected final static double PADDING_AMOUNT = 2.0;

    private final PhysicalVisualization visualization;
    private final PhysicalDevice device;

    private final Map<HardwareVertex, VisualEndpoint> lookupPorts;
    private final Map<VisualEndpoint, VisualTether> lookupTethers;
    private final Pane panePorts;
    private final Group tethers;

    private final SimpleDoubleProperty portScaleProperty;

    private final CheckMenuItem ckApplyLayout;

    public VisualDevice(final PhysicalVisualization visualization, final PhysicalDevice device) {
        this.visualization = visualization;
        this.device = device;

        this.lookupPorts = new HashMap<>();
        this.lookupTethers = new HashMap<>();

        this.panePorts = new Pane();
        this.tethers = new Group();

        this.ckApplyLayout = new CheckMenuItem("Apply Layout");
        this.ckApplyLayout.setSelected(true);

        //TODO: This should be modifiable through a property on the device's vertex.
        this.portScaleProperty = new SimpleDoubleProperty(32.0);

        final HBox layoutTitle = new HBox();
        final Label lblNameplate = new Label();
        lblNameplate.setText(device.getName());
        lblNameplate.setAlignment(Pos.BASELINE_CENTER);

        layoutTitle.getChildren().add(lblNameplate);
        this.getChildren().addAll(layoutTitle, panePorts);

        HBox.setHgrow(lblNameplate, Priority.ALWAYS);

        final Background bkg = new Background(new BackgroundFill(Color.TAN, CornerRadii.EMPTY, Insets.EMPTY));
        layoutTitle.setBackground(bkg);
        this.panePorts.setBackground(bkg);

        this.makeDraggable(true);

        this.device.onPortAdded.addHandler(this.handlerPortAdded);
        this.device.onPortRemoved.addHandler(this.handlerPortRemoved);

        // Process any ports that already exist in the device; we might process ports twice, so be prepared to ignore duplicates.
        for(final PhysicalEndpoint port : this.device.getPorts()) {
            this.addPort(port);
        }
    }

    public PhysicalDevice getDevice() {
        return this.device;
    }

    @Override
    public List<Object> getRespondingNodes(Point2D point) {
        return Collections.singletonList(this);
    }
    @Override
    public BooleanProperty isSubjectToLayoutProperty() {
        return this.ckApplyLayout.selectedProperty();
    }

    @Override
    public List<MenuItem> getContextMenuItems() {
        return Collections.singletonList(this.ckApplyLayout);
    }

    private final DragContext dragContext = new DragContext();
    public DragContext getDragContext() {
        return this.dragContext;
    }

    private final Event.EventListener<PhysicalEndpoint> handlerPortAdded = this::handlePortAdded;
    protected void handlePortAdded(final Event<PhysicalEndpoint> event, final PhysicalEndpoint port) {
        //The event can fire from an arbitrary worker thread, but we must process it in the UI thread (that ensure the visualization.visualEndpointFor(port) will work.)
        Platform.runLater(() -> {
            VisualDevice.this.visualization.getGraph().waitForValidState();
            VisualDevice.this.addPort(port);
        });
    }

    private void addPort(final PhysicalEndpoint port) {
        if(lookupPorts.putIfAbsent(port.getVertex(), VisualDevice.this.visualization.visualEndpointFor(port)) == null) {
            final VisualEndpoint visual = lookupPorts.get(port.getVertex());
            final VisualTether tether = new VisualTether(this, visual);
            lookupTethers.put(visual, tether);
            this.tethers.getChildren().add(tether);
            //Whenever a layout pass is needed by the endpoint, we need to rebuild the bounding rectangle for the ports.
            visual.needsLayoutProperty().addListener(observable -> this.requestLayout());
            visual.scaleProperty().bind(this.portScaleProperty);

            final PhysicalConnection properties = VisualDevice.this.device.getPortDetails(port);
            properties.onPropertyChanged.addHandler(this.handlerPortPropertiesChanged);
            visual.owningDeviceAssociationProperty().set(properties);
            //By forcing evaluation of the properties, we will rebuild the bounding rectangle for the ports.
            this.handlerPortPropertiesChanged.handle(null, properties.new PropertyEventArgs(null, null, true));
        }
    }
    private final Event.EventListener<PhysicalEndpoint> handlerPortRemoved = this::handlePortRemoved;
    protected void handlePortRemoved(final Event<PhysicalEndpoint> event, final PhysicalEndpoint port) {
        final VisualEndpoint visual = lookupPorts.get(port.getVertex());

        visual.getPhysicalInterfaceImage().getView().fitWidthProperty().unbind();
        visual.getPhysicalInterfaceImage().getView().fitHeightProperty().unbind();
        visual.getPhysicalInterfaceImage().getView().setFitWidth(0.0);
        visual.getPhysicalInterfaceImage().getView().setFitHeight(0.0);
        this.recomputePortBackgroundPaneLocation();
    }

    public void clear() {
        //TODO: Remove all ports, clean up event hooks.
        this.recomputePortBackgroundPaneLocation();
    }

    private final Event.EventListener<PropertyCloud.PropertyEventArgs> handlerPortPropertiesChanged = this::handlePortPropertiesChanged;
    protected void handlePortPropertiesChanged(final Event<PropertyCloud.PropertyEventArgs> event, final PropertyCloud.PropertyEventArgs args) {
        final PhysicalConnection connection = (PhysicalConnection)args.getCloud();
        final HardwareVertex vertexPort = connection.other(this.device.getVertex());
        final VisualEndpoint visualPort = this.lookupPorts.get(vertexPort);

        if(args.getName() == null && !args.isAdded()) {
            //TODO: Cleanup?
        } else {
            //HACK: We're going to be lazy and just set everything every time something changes.  That doesn't happen often enough to be a performance concern, but it is bad form.
            if(args.getCloud().hasProperties(IPhysicalViewApi.PROPERTY_PORT_POSITION_X, IPhysicalViewApi.PROPERTY_PORT_POSITION_Y)) {
                this.lookupTethers.get(visualPort).setVisible(false);

                final double x = (Double)args.getCloud().getBestPropertyValue(IPhysicalViewApi.PROPERTY_PORT_POSITION_X);
                final double y = (Double)args.getCloud().getBestPropertyValue(IPhysicalViewApi.PROPERTY_PORT_POSITION_Y);

                visualPort.makeDraggable(false);
                visualPort.translateXProperty().bind(this.translateXProperty().add(this.layoutXProperty()).add(this.panePorts.layoutXProperty().add(PADDING_AMOUNT)).add(this.portScaleProperty.multiply(x)));
                visualPort.translateYProperty().bind(this.translateYProperty().add(this.layoutYProperty()).add(this.panePorts.layoutYProperty().add(PADDING_AMOUNT)).add(this.portScaleProperty.multiply(y)));
            } else {
                this.lookupTethers.get(visualPort).setVisible(true);

                visualPort.translateXProperty().unbind();
                visualPort.translateYProperty().unbind();
                visualPort.makeDraggable(true);
            }

            final double angleControl = args.getCloud().getProperties().getOrDefault(IPhysicalViewApi.PROPERTY_PORT_CONTROL_ANGLE, (Set<Property<?>>)Collections.EMPTY_SET).stream().filter(property -> property.getValue() instanceof Double).map(property -> (Double)property.getValue()).findAny().orElse(180.0) * (Math.PI / 180.0);
            //TODO: Control point strength
            visualPort.controlXProperty().set(Math.cos(angleControl));
            visualPort.controlYProperty().set(Math.sin(angleControl));

            //HACK: This is done as a runLater to resolve a race condition with the label sizing.  But it doesn't necessarily solve that problem, it just reduces the liklihood of it mattering.
            Platform.runLater(this::recomputePortBackgroundPaneLocation);
        }
    }

    protected void recomputePortBackgroundPaneLocation() {
        // Recompute the size of panePorts (it doesnt contain the ports as children, it just serves as a backdrop for them on a different layer)
        final Collection<VisualEndpoint> visuals = this.lookupPorts.values();
        if(visuals.isEmpty()) {
            this.panePorts.setVisible(false);
        } else {
            double xMin = Double.MAX_VALUE;
            double xMax = -Double.MAX_VALUE;
            double yMin = Double.MAX_VALUE;
            double yMax = -Double.MAX_VALUE;
            for (final VisualEndpoint visual : visuals) {
                final VisualTether tether = lookupTethers.get(visual);
                if(!tether.isVisible()) {
                    final Bounds bounds = visual.getBoundsInParent();
                    xMin = Math.min(xMin, bounds.getMinX());
                    xMax = Math.max(xMax, bounds.getMaxX());
                    yMin = Math.min(yMin, bounds.getMinY());
                    yMax = Math.max(yMax, bounds.getMaxY());
                }
            }

            if(xMin == Double.MAX_VALUE) {
                this.panePorts.setVisible(false);
            } else {
                this.panePorts.setPrefWidth(xMax - xMin + 2.0 * PADDING_AMOUNT);
                this.panePorts.setPrefHeight(yMax - yMin + 2.0 * PADDING_AMOUNT);
                this.panePorts.setVisible(true);
            }
        }
    }

    @Override
    public Node nodeForLayer(final String layer) {
        switch(layer) {
            case PhysicalVisualization.LAYER_DEVICES:
                return this;
            case PhysicalVisualization.LAYER_WIRES:
                return this.tethers;
            default:
                return null;
        }
    }

    @Override
    public String toString() {
        return String.format("[Device: %s (%s)]", this.device.getName(), this.lookupPorts.values().stream().map(endpoint -> endpoint.toString()).collect(Collectors.joining(", ")));
    }
}
