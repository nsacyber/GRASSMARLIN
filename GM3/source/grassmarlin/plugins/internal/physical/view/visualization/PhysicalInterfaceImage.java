package grassmarlin.plugins.internal.physical.view.visualization;

import grassmarlin.Event;
import grassmarlin.plugins.internal.physical.view.IPhysicalViewApi;
import grassmarlin.plugins.internal.physical.view.Plugin;
import grassmarlin.plugins.internal.physical.view.data.PhysicalEndpoint;
import grassmarlin.session.Property;
import grassmarlin.session.PropertyContainer;
import javafx.application.Platform;
import javafx.beans.binding.ObjectExpression;
import javafx.scene.Group;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

public class PhysicalInterfaceImage extends Group {
    private final Plugin plugin;
    private final ImageView view;
    private final AtomicBoolean isValid;

    private final PhysicalEndpoint endpoint;

    public PhysicalInterfaceImage(final Plugin plugin, final PhysicalEndpoint endpoint) {
        this.plugin = plugin;
        this.view = new ImageView();

        this.view.setPreserveRatio(true);
        this.view.setSmooth(false);

        this.isValid = new AtomicBoolean(false);

        this.endpoint = endpoint;

        this.getChildren().add(this.view);

        if(endpoint != null && endpoint.getVertex() != null) {
            endpoint.getVertex().onPropertyChanged.addHandler(this.handlerPropertyChanged);
        }
    }

    public void detachHandlers() {
        if(endpoint != null && endpoint.getVertex() != null) {
            endpoint.getVertex().onPropertyChanged.removeHandler(this.handlerPropertyChanged);
        }
    }

    public ObjectExpression<Image> imageProperty() {
        return this.view.imageProperty();
    }

    private final Event.EventListener<PropertyContainer.PropertyEventArgs> handlerPropertyChanged = this::handlePropertyChanged;
    protected void handlePropertyChanged(final Event<PropertyContainer.PropertyEventArgs> event, final PropertyContainer.PropertyEventArgs args) {
        if(args.getName().equals(IPhysicalViewApi.PROPERTY_PORT)) {
            if(isValid.getAndSet(false)) {
                Platform.runLater(PhysicalInterfaceImage.this::requestLayout);
            }
        }
    }

    @Override
    public void layoutChildren() {
        if(!isValid.getAndSet(true)) {
            if(this.endpoint == null) {
                this.view.setImage(null);
            } else if(this.endpoint.getVertex() == null) {
                this.plugin.getCloudImageProperties().apply(this.view);
            } else {
                final Set<Property<?>> ports = this.endpoint.getVertex().getProperties().get(IPhysicalViewApi.PROPERTY_PORT);
                if (ports == null || ports.isEmpty()) {
                    this.view.setImage(null);
                    this.view.setViewport(null);
                } else {
                    boolean matched = false;
                    for (final Property<?> value : ports) {
                        final IPhysicalViewApi.ImageProperties properties = this.plugin.getPortImagePropertiesFor(value.getValue());
                        if (properties != null) {
                            matched = true;
                            properties.apply(this.view);
                            break;
                        }
                    }
                    if (!matched) {
                        this.view.setImage(null);
                        this.view.setViewport(null);
                    }
                }
            }
        }

        super.layoutChildren();
    }

    public ImageView getView() {
        return this.view;
    }
}
