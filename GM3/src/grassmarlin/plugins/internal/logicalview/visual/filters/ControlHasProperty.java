package grassmarlin.plugins.internal.logicalview.visual.filters;

import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;

public class ControlHasProperty extends HBox {
    private final TextField tbPropertyName;

    public ControlHasProperty() {
        this.tbPropertyName = new TextField();

        this.getChildren().addAll(
                new Label("Property: "),
                tbPropertyName
        );
    }

    public PropertyEdgeStyleRule getEdgeStyleRule() {
        return new PropertyEdgeStyleRule(tbPropertyName.getText(), null);
    }

    public void setEdgeStyleRule(PropertyEdgeStyleRule rule) {
        this.tbPropertyName.setText(rule.getPropertyName());
    }
}
