package grassmarlin.ui.pipeline;

import javafx.beans.binding.ObjectExpression;
import javafx.beans.binding.When;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Bounds;
import javafx.scene.input.MouseEvent;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.Polygon;
import javafx.scene.transform.Scale;

public class LinkingTriangle extends Polygon implements Linker {

    private Linkable container;
    private BooleanProperty isSelected;
    private ObjectProperty<Paint> selectedColorProperty;

    private final Scale transformScale;

    public LinkingTriangle(final ObjectProperty<Paint> fillColorProperty, final Linkable parent, final ObjectExpression<? extends Bounds> heightProvider) {
        super(0.1, 0.1, 0.1, 0.9, 0.65, 0.5);
        this.selectedColorProperty = new SimpleObjectProperty<>(Color.YELLOW);

        this.container = parent;
        this.isSelected = new SimpleBooleanProperty();
        this.isSelected.bind(this.container.getSelectedProperty());

        this.fillProperty().bind(new When(this.isSelected).then(this.selectedColorProperty).otherwise(fillColorProperty));

        this.setOnMousePressed(this::handleMouseClicked);

        this.transformScale = new Scale(heightProvider.get().getHeight(), heightProvider.get().getHeight());
        this.getTransforms().addAll(this.transformScale);

        heightProvider.addListener(observable -> {
            this.transformScale.setX(heightProvider.get().getHeight());
            this.transformScale.setY(heightProvider.get().getHeight());
        });
    }

    @Override
    public double prefWidth(final double height) {
        return this.transformScale.deltaTransform(0.75, 0.0).getX();
    }

    private void handleMouseClicked(MouseEvent event) {
        this.container.getLinkingPane().handleLinkerSelected(this);
    }

    @Override
    public void setLinkable(Linkable linkable) {
        this.container = linkable;
    }

    @Override
    public Linkable getLinkable() {
        return this.container;
    }
}
