package grassmarlin.plugins.internal.logicalview.visual.filters;

import grassmarlin.Event;
import grassmarlin.plugins.internal.logicalview.visual.VisualLogicalEdge;
import grassmarlin.session.PropertyCloud;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.binding.BooleanExpression;
import javafx.beans.binding.DoubleExpression;
import javafx.beans.binding.ObjectExpression;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.ObservableList;
import javafx.scene.paint.Color;

public class BoundEdgeProperties {
    private final VisualLogicalEdge edge;
    private final ObservableList<EdgeStyleRule> rules;
    private final BooleanExpression useRules;

    private SimpleDoubleProperty weight;
    private SimpleDoubleProperty opacity;
    private SimpleObjectProperty<Color> color;

    public BoundEdgeProperties(final VisualLogicalEdge edge, final ObservableList<EdgeStyleRule> rules, final BooleanExpression useRules) {
        this.edge = edge;
        this.rules = rules;
        this.useRules = useRules;

        this.weight = new SimpleDoubleProperty(1.0);
        this.opacity = new SimpleDoubleProperty(1.0);
        this.color = new SimpleObjectProperty<>(Color.BLACK);

        this.rules.addListener(this.handlerChange);
        this.useRules.addListener(this.handlerChange);
        this.edge.getEdgeData().onPropertyChanged.addHandler(this.handlerEdgeDataPropertyChanged);

        this.handleChange(null);
    }

    private Event.EventListener<PropertyCloud.PropertyEventArgs> handlerEdgeDataPropertyChanged = this::handleEdgeDataPropertyChanged;
    private void handleEdgeDataPropertyChanged(Event<PropertyCloud.PropertyEventArgs> event, PropertyCloud.PropertyEventArgs args) {
        this.handleChange(null);
    }

    private final InvalidationListener handlerChange = this::handleChange;
    private void handleChange(final Observable o) {
        if(this.useRules.get()) {
            for (final EdgeStyleRule rule : this.rules) {
                if (rule.applies(this.edge)) {
                    this.weight.bind(rule.weight);
                    this.opacity.bind(rule.opacity);
                    this.color.bind(rule.color);
                    return;
                }
            }
        }
        this.weight.unbind();
        this.weight.set(1.0);
        this.opacity.unbind();
        this.opacity.set(1.0);
        this.color.unbind();
        this.color.set(null);
    }

    public DoubleExpression opacityProperty() {
        return this.opacity;
    }
    public DoubleExpression weightProperty() {
        return this.weight;
    }
    public ObjectExpression<Color> colorProperty() {
        return this.color;
    }
}
