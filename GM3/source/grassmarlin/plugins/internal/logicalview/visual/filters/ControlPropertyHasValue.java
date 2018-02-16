package grassmarlin.plugins.internal.logicalview.visual.filters;

import grassmarlin.RuntimeConfiguration;
import grassmarlin.ui.common.controls.ObjectField;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;

import java.io.Serializable;

public class ControlPropertyHasValue extends HBox {
    private final TextField tbPropertyName;
    private final ObjectField<Serializable> ofValue;

    public ControlPropertyHasValue(final RuntimeConfiguration config) {
        this.tbPropertyName = new TextField();
        this.ofValue = new ObjectField<>(config, Object.class);

        this.getChildren().addAll(
                new Label("Property: "),
                tbPropertyName,
                new Label("Value: "),
                ofValue
        );
    }

    public PropertyEdgeStyleRule getEdgeStyleRule() {
        return new PropertyEdgeStyleRule(tbPropertyName.getText(), ofValue.createInstanceFromText());
    }

    public void setEdgeStyleRule(PropertyEdgeStyleRule rule) {
        this.tbPropertyName.setText(rule.getPropertyName());
    }
}
