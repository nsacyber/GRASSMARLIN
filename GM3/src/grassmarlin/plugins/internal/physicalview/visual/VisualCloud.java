package grassmarlin.plugins.internal.physicalview.visual;

import grassmarlin.ui.common.IDraggable;
import javafx.beans.binding.DoubleExpression;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.transform.NonInvertibleTransformException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class VisualCloud extends StackPane implements IHasControlPoint, IDraggable {
    private final List<VisualEndpoint> connectedEndpoints;

    public VisualCloud() {
        this.connectedEndpoints = new ArrayList<>();

        final Label title = new Label("Unknown Topology");
        title.setPadding(new Insets(8.0, 24.0, 8.0, 24.0));
        this.setBorder(new Border(new BorderStroke(Color.LIGHTGRAY, BorderStrokeStyle.SOLID, new CornerRadii(4.0), BorderWidths.DEFAULT)));
        this.setBackground(new Background(new BackgroundFill(Color.rgb(208, 208, 208, 0.7), new CornerRadii(4.0), null)));

        this.getChildren().add(title);

        this.makeDraggable(true);
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

            this.dragTo(new Point2D(this.getTranslateX() - dragContext.ptPrevious.getX() + ptTranslated.getX(), this.getTranslateY() - dragContext.ptPrevious.getY() + ptTranslated.getY()));

            //Deviation from default:  If shift is down, find VisualEndpoints that are connected to the switch and apply the same translation to them.
            if(event.isShiftDown()) {
                for(final VisualEndpoint endpoint : connectedEndpoints) {
                    endpoint.dragTo(new Point2D(endpoint.getTranslateX() - dragContext.ptPrevious.getX() + ptTranslated.getX(), endpoint.getTranslateY() - dragContext.ptPrevious.getY() + ptTranslated.getY()));
                }
            }

            // To avoid floating point rounding errors we will reverse this translation and apply a new absolute translation next time.
            dragContext.ptPrevious = ptTranslated;
        }
    }

    @Override
    public DoubleExpression getTerminalX() {
        return this.translateXProperty().add(this.widthProperty().divide(2.0));
    }
    @Override
    public DoubleExpression getTerminalY() {
        return this.translateYProperty().add(this.heightProperty().divide(2.0));
    }
    @Override
    public DoubleExpression getControlX() {
        return this.translateXProperty().add(this.widthProperty().divide(2.0));
    }
    @Override
    public DoubleExpression getControlY() {
        return this.translateYProperty().add(this.heightProperty().divide(2.0));
    }

}
