package grassmarlin.plugins.internal.physicalview.visual;

import grassmarlin.plugins.internal.physicalview.graph.Switch;
import grassmarlin.ui.common.IDraggable;
import javafx.geometry.Point2D;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.transform.NonInvertibleTransformException;

import java.util.*;

public class VisualSwitch extends HBox implements IDraggable {
    protected final Switch device;
    private final Map<Switch.PortGroup, Pane> lookupPortGroups;
    private final Map<Switch.Port, VisualSwitchPort> lookupVisualPorts;
    private final List<VisualEndpoint> connectedEndpoints;

    public VisualSwitch(final Switch device) {
        this.device = device;
        this.lookupPortGroups = new HashMap<>();
        this.lookupVisualPorts = new HashMap<>();
        this.connectedEndpoints = new ArrayList<>();

        this.setBorder(new Border(new BorderStroke(Color.BLACK, BorderStrokeStyle.SOLID, null, null, null)));

        for(final Switch.PortGroup group : this.device.getPortGroups()) {
            final GridPane visualGroup = new GridPane();
            this.lookupPortGroups.put(group, visualGroup);

            this.getChildren().add(visualGroup);

            for(final Map.Entry<Switch.Port, Switch.PortVisualSettings> entry : group.getPorts().entrySet()) {
                final VisualSwitchPort visualPort = new VisualSwitchPort(this, entry.getKey(), entry.getValue());
                visualGroup.add(visualPort, entry.getValue().getX(), entry.getValue().getY());
                lookupVisualPorts.put(entry.getKey(), visualPort);
            }
        }

        this.makeDraggable(true);
    }

    public Map<Switch.Port, VisualSwitchPort> getPortMapping() {
        return lookupVisualPorts;
    }

    public Switch getSwitch() {
        return this.device;
    }

    void setConnectedEndpoints(final Collection<VisualEndpoint> endpoints) {
        this.connectedEndpoints.clear();
        this.connectedEndpoints.addAll(endpoints);
    }

    @Override
    public void handleMouseDragged(MouseEvent event) {
        // The primary button has to be down
        // The drag target only has to match if we are going to process the drag; if dragging is disabled then that check will be handled elsewhere
        if (event.isPrimaryButtonDown() && event.getSource() == this) {
            event.consume();

            Point2D ptTemp = new Point2D(event.getSceneX(), event.getSceneY());
            try {
                ptTemp = this.getParent().getLocalToSceneTransform().inverseTransform(ptTemp);
            } catch (NonInvertibleTransformException ex) {
                ex.printStackTrace();
                // We just won't be able to account for the translation. There may be some distortion, but it will still work.
            }

            final Point2D ptTranslated = ptTemp.subtract(dragContext.ptOrigin);

            this.dragTo(new Point2D(this.getLayoutX() - dragContext.ptPrevious.getX() + ptTranslated.getX(), this.getLayoutY() - dragContext.ptPrevious.getY() + ptTranslated.getY()));

            //Deviation from default:  If shift is down, find VisualEndpoints that are connected to the switch and apply the same translation to them.
            if(event.isShiftDown()) {
                for(final VisualEndpoint endpoint : connectedEndpoints) {
                    endpoint.dragTo(new Point2D(endpoint.getLayoutX() - dragContext.ptPrevious.getX() + ptTranslated.getX(), endpoint.getLayoutY() - dragContext.ptPrevious.getY() + ptTranslated.getY()));
                }
            }

            // To avoid floating point rounding errors we will reverse this translation and apply a new absolute translation next time.
            dragContext.ptPrevious = ptTranslated;
        }
    }
}
