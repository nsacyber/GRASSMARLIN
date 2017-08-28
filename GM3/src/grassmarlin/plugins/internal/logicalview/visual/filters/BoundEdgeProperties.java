package grassmarlin.plugins.internal.logicalview.visual.filters;

import grassmarlin.Event;
import grassmarlin.plugins.internal.logicalview.visual.VisualLogicalEdge;
import grassmarlin.session.PropertyCloud;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.binding.DoubleExpression;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.collections.ObservableList;

public class BoundEdgeProperties {
    private final VisualLogicalEdge edge;
    private final ObservableList<EdgeStyleRule> rules;

    private SimpleDoubleProperty weight;
    private SimpleDoubleProperty opacity;

    public BoundEdgeProperties(final VisualLogicalEdge edge, final ObservableList<EdgeStyleRule> rules) {
        this.edge = edge;
        this.rules = rules;

        this.weight = new SimpleDoubleProperty(1.0);
        this.opacity = new SimpleDoubleProperty(1.0);

        this.rules.addListener(this.handlerChange);
        this.edge.getEdgeData().onPropertyChanged.addHandler(this.handlerEdgeDataPropertyChanged);

        this.handleChange(null);
    }

    private Event.EventListener<PropertyCloud.PropertyEventArgs> handlerEdgeDataPropertyChanged = this::handleEdgeDataPropertyChanged;
    private void handleEdgeDataPropertyChanged(Event<PropertyCloud.PropertyEventArgs> event, PropertyCloud.PropertyEventArgs args) {
        this.handleChange(null);
    }

    private final InvalidationListener handlerChange = this::handleChange;
    private void handleChange(final Observable o) {
        for(final EdgeStyleRule rule : this.rules) {
            if(rule.applies(this.edge)) {
                this.weight.bind(rule.weight);
                this.opacity.bind(rule.opacity);
                return;
            }
        }
        this.weight.unbind();
        this.weight.set(1.0);
        this.opacity.unbind();
        this.opacity.set(1.0);
    }

    public DoubleExpression opacityProperty() {
        return this.opacity;
    }
    public DoubleExpression weightProperty() {
        return this.weight;
    }
}
