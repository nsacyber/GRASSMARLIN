package grassmarlin.plugins.internal.physical.view.visualization;

import grassmarlin.ui.common.IDraggable;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.shape.CubicCurve;

public class VisualWire extends Group implements IDraggable {
    private final CubicCurve curve;
    private final DoubleProperty controlStrength;

    private final VisualEndpoint source;
    private final VisualEndpoint destination;

    public VisualWire(final VisualEndpoint endpointSource, final VisualEndpoint endpointDestination) {
        this.source = endpointSource;
        this.destination = endpointDestination;

        this.curve = new CubicCurve();
        this.curve.setFill(Color.TRANSPARENT);
        this.curve.setStroke(Color.BLUE);
        this.curve.setStrokeWidth(2.0);

        this.curve.startXProperty().bind(endpointSource.terminalXProperty().add(endpointSource.layoutXProperty()).add(endpointSource.translateXProperty()));
        this.curve.startYProperty().bind(endpointSource.terminalYProperty().add(endpointSource.layoutYProperty()).add(endpointSource.translateYProperty()));

        this.curve.endXProperty().bind(endpointDestination.terminalXProperty().add(endpointDestination.layoutXProperty()).add(endpointDestination.translateXProperty()));
        this.curve.endYProperty().bind(endpointDestination.terminalYProperty().add(endpointDestination.layoutYProperty()).add(endpointDestination.translateYProperty()));

        this.controlStrength = new SimpleDoubleProperty(32.0);

        this.curve.controlX1Property().bind(this.curve.startXProperty().add(endpointSource.controlXProperty().multiply(this.controlStrength)));
        this.curve.controlY1Property().bind(this.curve.startYProperty().add(endpointSource.controlYProperty().multiply(this.controlStrength)));
        this.curve.controlX2Property().bind(this.curve.endXProperty().add(endpointDestination.controlXProperty().multiply(this.controlStrength)));
        this.curve.controlY2Property().bind(this.curve.endYProperty().add(endpointDestination.controlYProperty().multiply(this.controlStrength)));

        this.getChildren().add(this.curve);

        this.makeDraggable(false);
    }

    public VisualEndpoint getSource() {
        return this.source;
    }
    public VisualEndpoint getDestination() {
        return this.destination;
    }

    private final DragContext dragContext = new DragContext();
    @Override
    public DragContext getDragContext() {
        return this.dragContext;
    }

    public DoubleProperty controlStrengthProperty() {
        return this.controlStrength;
    }

    @Override
    public String toString() {
        return String.format("[Wire:{%s, %s}]", this.source.getVertex().getAddress(), this.destination.getVertex().getAddress());
    }
}
