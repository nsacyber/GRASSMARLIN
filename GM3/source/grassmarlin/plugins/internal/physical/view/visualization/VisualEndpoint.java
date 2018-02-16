package grassmarlin.plugins.internal.physical.view.visualization;

import grassmarlin.Event;
import grassmarlin.plugins.internal.logicalview.ICanHasContextMenu;
import grassmarlin.plugins.internal.physical.view.IPhysicalViewApi;
import grassmarlin.plugins.internal.physical.view.data.PhysicalDevice;
import grassmarlin.plugins.internal.physical.view.data.PhysicalEndpoint;
import grassmarlin.session.HardwareVertex;
import grassmarlin.session.PhysicalConnection;
import grassmarlin.session.PropertyContainer;
import grassmarlin.ui.common.IAltClickable;
import grassmarlin.ui.common.IDraggable;
import grassmarlin.ui.common.NodeOffsetBinding;
import javafx.beans.binding.NumberBinding;
import javafx.beans.binding.When;
import javafx.beans.property.*;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class VisualEndpoint extends HBox implements IDraggable, IPhysicalElement, IAltClickable, ICanHasContextMenu {
    public static final String PROPERTY_NAME = "Name";

    private final static Background BACKGROUND_ENDPOINT = new Background(new BackgroundFill(Color.LIGHTGRAY, CornerRadii.EMPTY, Insets.EMPTY));
    private final static Border BORDER_ENDPOINT = new Border(new BorderStroke(Color.DARKGRAY, BorderStrokeStyle.SOLID, CornerRadii.EMPTY, new BorderWidths(1.0)));
    private final static Insets PADDING_ENDPOINT = new Insets(4.0);

    private final static Background BACKGROUND_PORT = Background.EMPTY;
    private final static Border BORDER_PORT = Border.EMPTY;
    private final static Insets PADDING_PORT = Insets.EMPTY;

    private final static Background BACKGROUND_CLOUD = Background.EMPTY;
    private final static Border BORDER_CLOUD = Border.EMPTY;
    private final static Insets PADDING_CLOUD = Insets.EMPTY;

    private final PhysicalVisualization visualization;
    private final PhysicalEndpoint endpoint;

    private final SimpleObjectProperty<PhysicalConnection> owningDeviceAssociation;
    private final SimpleStringProperty titleProperty;

    private final SimpleDoubleProperty terminalXProperty;
    private final SimpleDoubleProperty terminalYProperty;
    private final SimpleDoubleProperty controlXProperty;
    private final SimpleDoubleProperty controlYProperty;

    private final SimpleDoubleProperty scaleProperty;

    private final Label labelTitle;
    private final PhysicalInterfaceImage physicalInterfaceImage;

    private final Tooltip tooltip;

    private final CheckMenuItem ckApplyLayout;

    public VisualEndpoint(final PhysicalVisualization visualization, final PhysicalEndpoint endpoint) {
        this.visualization = visualization;
        this.endpoint = endpoint;

        this.scaleProperty = new SimpleDoubleProperty(0.0);

        this.ckApplyLayout = new CheckMenuItem("Apply Layout");
        this.ckApplyLayout.setSelected(true);

        this.owningDeviceAssociation = new SimpleObjectProperty<>(null);
        this.terminalXProperty = new SimpleDoubleProperty(0.0);
        this.terminalYProperty = new SimpleDoubleProperty(0.0);
        this.controlXProperty = new SimpleDoubleProperty(0.0);
        this.controlYProperty = new SimpleDoubleProperty(0.0);

        this.physicalInterfaceImage = new PhysicalInterfaceImage(visualization.getPlugin(), endpoint);
        this.labelTitle = new Label();

        this.getChildren().add(this.physicalInterfaceImage);
        if(endpoint == null) {
            //Tether
            this.titleProperty = new ReadOnlyStringWrapper("");

            this.setBackground(BACKGROUND_CLOUD);
            this.setBorder(BORDER_CLOUD);
            this.setOpaqueInsets(PADDING_CLOUD);

            this.controlXProperty.set(0.0);
            this.controlYProperty.set(0.0);
        } else if(endpoint.getVertex() != null) {
            //Host
            this.titleProperty = new SimpleStringProperty(endpoint.getVertex().getAddress().toString());

            this.setBackground(BACKGROUND_ENDPOINT);
            this.setBorder(BORDER_ENDPOINT);
            this.setOpaqueInsets(PADDING_ENDPOINT);

            this.controlXProperty.set(-1.0);
            this.controlYProperty.set(0.0);
            this.getChildren().add(this.labelTitle);
        } else {
            //Cloud
            this.titleProperty = new ReadOnlyStringWrapper("Unknown Topology");

            this.setBackground(BACKGROUND_CLOUD);
            this.setBorder(BORDER_CLOUD);
            this.setOpaqueInsets(PADDING_CLOUD);

            this.controlXProperty.set(0.0);
            this.controlYProperty.set(0.0);
        }
        this.labelTitle.textProperty().bind(this.titleProperty());

        this.tooltip = new Tooltip();
        this.tooltip.textProperty().bind(this.titleProperty);
        this.labelTitle.setTooltip(this.tooltip);
        Tooltip.install(this.physicalInterfaceImage.getView(), this.tooltip);

        // When the scaling is set, then the title shouldn't be visible.  When the scaling isn't set, the image should scale to the height of the text.
        //TODO: Verify this way of setting the height of the image in the .otherwise is correct--it might be part of a feedback loop.
        //TODO: Nothing exposes or sets, the scaling property--it was just added and the image, etc need to be retrofitted to use it.

        final NumberBinding imageSizeBinding = new When(this.scaleProperty.greaterThan(0.0)).then(this.scaleProperty).otherwise(this.labelTitle.heightProperty());
        this.physicalInterfaceImage.getView().fitHeightProperty().bind(imageSizeBinding);
        this.physicalInterfaceImage.getView().fitWidthProperty().bind(imageSizeBinding);

        this.makeDraggable(true);

        final NodeOffsetBinding offsetImage = new NodeOffsetBinding(this.physicalInterfaceImage, this);
        this.terminalXProperty.bind(
                new When(this.physicalInterfaceImage.getView().imageProperty().isNull())
                        .then(0.0)
                        .otherwise(offsetImage.getX().add(this.physicalInterfaceImage.getView().fitWidthProperty().divide(2.0)))
        );
        this.terminalYProperty.bind(
                new When(this.physicalInterfaceImage.getView().imageProperty().isNull())
                        .then(this.heightProperty().divide(2.0))
                        .otherwise(offsetImage.getY().add(this.physicalInterfaceImage.getView().fitHeightProperty().divide(2.0)))
        );

        if(this.endpoint != null && this.endpoint.getVertex() != null) {
            this.endpoint.getVertex().onPropertyChanged.addHandler(this.handlerPropertyChanged);
        }
        this.owningDeviceAssociation.addListener(this::handleOwnerChanged);
        this.boundsInParentProperty().addListener(observable -> {
            VisualEndpoint.this.recalculateOwnerBounds();
        });
    }

    public void detachHandlers() {
        this.physicalInterfaceImage.detachHandlers();
    }

    @Override
    public List<Object> getRespondingNodes(Point2D point) {
        return Collections.singletonList(this);
    }

    @Override
    public List<MenuItem> getContextMenuItems() {
        if(this.endpoint.isPort()) {
            return null;
        } else {
            return Collections.singletonList(this.ckApplyLayout);
        }
    }

    @Override
    public BooleanProperty isSubjectToLayoutProperty() {
        return this.ckApplyLayout.selectedProperty();
    }

    private void recalculateOwnerBounds() {
        final PhysicalConnection connectionOwner = this.owningDeviceAssociation.get();
        if(connectionOwner != null) {
            this.setBackground(BACKGROUND_PORT);
            this.setBorder(BORDER_PORT);
            this.setOpaqueInsets(PADDING_PORT);

            final HardwareVertex vertexDevice = connectionOwner.other(this.getVertex());
            final PhysicalDevice physicalDevice = this.visualization.getGraph().getDevices().stream().filter(device -> device.getVertex() == vertexDevice).findAny().orElse(null);
            if(physicalDevice != null) {
                final VisualDevice visual = this.visualization.visualDeviceFor(physicalDevice);
                if(visual != null) {
                    visual.recomputePortBackgroundPaneLocation();
                    visual.requestLayout();
                }
            }
        } else {
            if(this.endpoint == null) {
                //We don't have to do anything here because this.endpoint is final and whatever we did in the constructor will still be correct.
            } else if(this.endpoint.getVertex() != null) {
                this.setBackground(BACKGROUND_ENDPOINT);
                this.setBorder(BORDER_ENDPOINT);
                this.setOpaqueInsets(PADDING_ENDPOINT);
            } else {
                this.setBackground(BACKGROUND_CLOUD);
                this.setBorder(BORDER_CLOUD);
                this.setOpaqueInsets(PADDING_CLOUD);

                this.controlXProperty.unbind();
                this.controlXProperty.set(0.0);
                this.controlYProperty.unbind();
                this.controlYProperty.set(0.0);
            }
        }
    }

    public DoubleProperty scaleProperty() {
        return this.scaleProperty;
    }

    public PhysicalInterfaceImage getPhysicalInterfaceImage() {
        return this.physicalInterfaceImage;
    }

    private final DragContext dragContext = new DragContext();
    @Override
    public DragContext getDragContext() {
        return this.dragContext;
    }

    protected void handleOwnerChanged(final ObservableValue<? extends PhysicalConnection> observable, final PhysicalConnection oldValue, final PhysicalConnection newValue) {
        this.makeDraggable(newValue == null);
        if(newValue != null) {
            this.titleProperty.set(newValue.getProperties().getOrDefault(PROPERTY_NAME, Collections.emptySet()).stream().map(value -> value.getValue().toString()).collect(Collectors.joining()));

            this.setBackground(BACKGROUND_PORT);
            this.setBorder(BORDER_PORT);
            this.setOpaqueInsets(PADDING_PORT);

            final HardwareVertex vertexDevice = newValue.other(this.getVertex());
            final PhysicalDevice physicalDevice = this.visualization.getGraph().getDevices().stream().filter(device -> device.getVertex() == vertexDevice).findAny().orElse(null);
            if(physicalDevice != null) {
                final VisualDevice visual = this.visualization.visualDeviceFor(physicalDevice);
                if(visual != null) {
                    visual.recomputePortBackgroundPaneLocation();
                    visual.requestLayout();
                }
            }
        } else {
            this.titleProperty.set(endpoint.getVertex().getAddress().toString());

            if(this.endpoint.getVertex() != null) {
                this.setBackground(BACKGROUND_ENDPOINT);
                this.setBorder(BORDER_ENDPOINT);
                this.setOpaqueInsets(PADDING_ENDPOINT);

                this.controlXProperty.unbind();
                this.controlXProperty.set(-1.0);
                this.controlYProperty.unbind();
                this.controlYProperty.set(0.0);
            } else {
                this.setBackground(BACKGROUND_CLOUD);
                this.setBorder(BORDER_CLOUD);
                this.setOpaqueInsets(PADDING_CLOUD);

                this.controlXProperty.unbind();
                this.controlXProperty.set(0.0);
                this.controlYProperty.unbind();
                this.controlYProperty.set(0.0);
            }
        }

        recalculateOwnerBounds();
    }

    private final Event.EventListener<PropertyContainer.PropertyEventArgs> handlerPropertyChanged = this::handlePropertyChanged;
    protected void handlePropertyChanged(final Event<PropertyContainer.PropertyEventArgs> event, final PropertyContainer.PropertyEventArgs args) {
        if(args.getName().equals(IPhysicalViewApi.PROPERTY_PORT)) {
            //TODO: Set image, control vector, etc.
            //TODO: Ensure the physical interface property is set on the vertex and not the edge.
            //Orientation--this is part of the icon.
            final Set<grassmarlin.session.Property<?>> ports = args.getContainer().getProperties().get(IPhysicalViewApi.PROPERTY_PORT);
            if (ports != null && !ports.isEmpty()) {
                boolean matched = false;
                for (final grassmarlin.session.Property<?> value : ports) {
                    final IPhysicalViewApi.ImageProperties properties = this.visualization.getPlugin().getPortImagePropertiesFor(value.getValue());
                    if (properties != null) {
                        matched = true;
                        properties.apply(this.physicalInterfaceImage.getView());
                        break;
                    }
                }
            }
        }
    }

    public PhysicalEndpoint getEndpoint() {
        return this.endpoint;
    }

    public ObjectProperty<PhysicalConnection> owningDeviceAssociationProperty() {
        return this.owningDeviceAssociation;
    }

    public StringProperty titleProperty() {
        return this.titleProperty;
    }

    //Endpoints and Control Points
    //The terminal X/Y should indicate a location within the VisualEndpoint to terminate an edge.
    public DoubleProperty terminalXProperty() {
        return this.terminalXProperty;
    }
    public DoubleProperty terminalYProperty() {
        return this.terminalYProperty;
    }
    //The control X/Y should create a unit vector
    public DoubleProperty controlXProperty() {
        return this.controlXProperty;
    }
    public DoubleProperty controlYProperty() {
        return this.controlYProperty;
    }

    public HardwareVertex getVertex() {
        return this.endpoint.getVertex();
    }

    @Override
    public int hashCode() {
        if(this.endpoint == null) {
            return super.hashCode();
        } else {
            return this.endpoint.hashCode();
        }
    }

    @Override
    public String toString() {
        if(this.endpoint.getVertex() == null) {
            return "[Endpoint:null]";
        } else {
            return String.format("[Endpoint:%s]", this.endpoint.getVertex().getAddress());
        }
    }
}
