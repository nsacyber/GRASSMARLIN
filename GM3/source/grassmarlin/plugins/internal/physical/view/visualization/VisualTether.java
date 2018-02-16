package grassmarlin.plugins.internal.physical.view.visualization;

import javafx.scene.Group;
import javafx.scene.shape.Line;

public class VisualTether extends Group {
    private final Line connector;

    public VisualTether(final VisualDevice device, final VisualEndpoint port) {
        this.connector = new Line();
        //TODO: This should bind to some definable point within the device's bounding region.
        this.connector.startXProperty().bind(device.translateXProperty());
        this.connector.startYProperty().bind(device.translateYProperty());

        this.connector.endXProperty().bind(port.terminalXProperty().add(port.translateXProperty()));
        this.connector.endYProperty().bind(port.terminalYProperty().add(port.translateYProperty()));

        this.getChildren().add(this.connector);
    }
}
