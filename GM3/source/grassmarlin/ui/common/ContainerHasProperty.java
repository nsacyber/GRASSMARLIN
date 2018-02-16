package grassmarlin.ui.common;

import grassmarlin.Event;
import grassmarlin.common.Confidence;
import grassmarlin.session.Property;
import grassmarlin.session.PropertyContainer;
import javafx.beans.binding.BooleanBinding;

import java.io.Serializable;
import java.util.Set;

public class ContainerHasProperty extends BooleanBinding {
    private final PropertyContainer container;
    private final String nameProperty;
    private final Serializable valueProperty;
    private final Confidence minConfidence;

    public ContainerHasProperty(final PropertyContainer container, final String nameProperty) {
        this(container, nameProperty, null, null);
    }
    public ContainerHasProperty(final PropertyContainer container, final String nameProperty, final Serializable valueProperty) {
        this(container, nameProperty, valueProperty, null);
    }
    public ContainerHasProperty(final PropertyContainer container, final String nameProperty, final Serializable valueProperty, final Confidence minConfidence) {
        this.container = container;
        this.nameProperty = nameProperty;
        this.valueProperty = valueProperty;
        this.minConfidence = minConfidence;

        container.onPropertyChanged.addHandler(this.handlerPropertyChanged);
    }

    private final Event.EventListener<PropertyContainer.PropertyEventArgs> handlerPropertyChanged = this::handlePropertyChanged;
    private void handlePropertyChanged(final Event<PropertyContainer.PropertyEventArgs> event, final PropertyContainer.PropertyEventArgs args) {
        //TODO: Consider a more robust validation check.
        if(args.getName().equals(this.nameProperty)) {
            this.invalidate();
        }
    }

    @Override
    protected boolean computeValue() {
        final Set<Property<?>> properties = this.container.getProperties().get(nameProperty);
        if(properties == null) {
            return false;
        } else {
            if(valueProperty != null) {
                final Property<?> property = properties.stream().filter(existing -> existing.getValue().equals(valueProperty)).findAny().orElse(null);
                if(property == null) {
                    return false;
                } else {
                    return property.getConfidence().compareTo(this.minConfidence) >= 0;
                }
            } else {
                //We're only testing for the presence of the property, not a value.
                return true;
            }
        }
    }
}
