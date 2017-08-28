package grassmarlin.ui.common;

import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.input.MouseEvent;
import javafx.scene.transform.NonInvertibleTransformException;

/**
 * This interface adds draggable functionality to a JavaFX Node. If this interface is implemented by a non-node it will
 * have no effect.
 */
public interface IDraggable {
    class DragContext {
        public Point2D ptOrigin;
        public Point2D ptPrevious;
    }

    DragContext dragContext = new DragContext();

    default void makeDraggable(final boolean draggable) {
        if (this instanceof Node) {
            final Node node = (Node)this;
            if (draggable) {
                node.setOnMousePressed(this::handleMousePressed);
                node.setOnMouseDragged(this::handleMouseDragged);
            } else {
                node.setOnMousePressed(this::handleMouseEventRedirectToParent);
                node.setOnMouseDragged(this::handleMouseEventRedirectToParent);
            }
        }
    }

    default void handleMousePressed(MouseEvent event) {
        if (this instanceof Node) {
            if (event.isPrimaryButtonDown()) {
                if (event.getSource() instanceof IDraggable) {
                    event.consume();
                    try {
                        dragContext.ptOrigin = ((Node) this).getParent().getLocalToSceneTransform().inverseTransform(event.getSceneX(), event.getSceneY());
                    } catch (NonInvertibleTransformException ex) {
                        ex.printStackTrace();
                        dragContext.ptOrigin = new Point2D(event.getSceneX(), event.getSceneY());
                    }
                    dragContext.ptPrevious = new Point2D(0, 0);
                }
            }
        }
    }

    default void handleMouseDragged(MouseEvent event) {
        // The primary button has to be down
        // The drag target only has to match if we are going to process the drag; if dragging is disabled then that check will be handled elsewhere
        if (event.isPrimaryButtonDown() && event.getSource() == this) {
            event.consume();

            Point2D ptTemp = new Point2D(event.getSceneX(), event.getSceneY());
            if (this instanceof Node) {
                Node node = ((Node) this);

                try {
                    ptTemp = node.getParent().getLocalToSceneTransform().inverseTransform(ptTemp);
                } catch (NonInvertibleTransformException ex) {
                    ex.printStackTrace();
                    // We just won't be able to account for the translation. There may be some distortion, but it will still work.
                }

                final Point2D ptTranslated = ptTemp.subtract(dragContext.ptOrigin);

                this.dragTo(new Point2D(node.getTranslateX() - dragContext.ptPrevious.getX() + ptTranslated.getX(), node.getTranslateY() - dragContext.ptPrevious.getY() + ptTranslated.getY()));

                // To avoid floating point rounding errors we will reverse this translation and apply a new absolute translation next time.
                dragContext.ptPrevious = ptTranslated;
            }
        }
    }

    default void handleMouseEventRedirectToParent(MouseEvent event) {
        if (event.isPrimaryButtonDown()) {
            // Redirect to the containing group;
            if (event.getSource() instanceof Node && ((Node) event.getSource()).getParent() instanceof Node) {
                ((Node) event.getSource()).getParent().fireEvent(event);
                event.consume();
            }
        }
    }

    default void dragTo(final Point2D ptDestination) {
        final Node node = ((Node) this);

        node.translateXProperty().set(ptDestination.getX());
        node.translateYProperty().set(ptDestination.getY());
    }
}
