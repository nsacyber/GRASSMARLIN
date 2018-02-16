package grassmarlin.ui.common;

import javafx.geometry.Point2D;
import javafx.scene.Node;

import java.util.function.Consumer;

/**
 * This interface adds draggable functionality to a JavaFX Node. If this interface is implemented by a non-node it will
 * have no effect.
 */
public interface IDraggable {
    class DragContext {
        public Consumer<Point2D> dragStartHandler = null;
        public Consumer<Point2D> dragHandler = null;
        public Point2D ptOrigin = null;
        public Point2D ptPrevious = null;

        public DragContext() {

        }
    }

    DragContext getDragContext();

    default void makeDraggable(final boolean draggable) {
        if (draggable) {
            this.getDragContext().dragStartHandler = this::startDrag;
            this.getDragContext().dragHandler = this::dragTo;
        } else {
            this.getDragContext().dragStartHandler = null;
            this.getDragContext().dragHandler = null;
        }
    }

    default void startDrag(final Point2D worldCursor) {
        this.getDragContext().ptOrigin = worldCursor;
        this.getDragContext().ptPrevious = new Point2D(0, 0);
    }

    default void dragTo(final Point2D worldDestination) {
        if (this instanceof Node) {
            final Node node = ((Node) this);
            final Point2D ptTranslated = worldDestination.subtract(this.getDragContext().ptOrigin);

            //Only update position if not bound
            if(!node.translateXProperty().isBound()) {
                node.translateXProperty().set(node.getTranslateX() - this.getDragContext().ptPrevious.getX() + ptTranslated.getX());
            }
            if(!node.translateYProperty().isBound()) {
                node.translateYProperty().set(node.getTranslateY() - this.getDragContext().ptPrevious.getY() + ptTranslated.getY());
            }


            // To avoid floating point rounding errors we will reverse this translation and apply a new absolute translation next time.
            this.getDragContext().ptPrevious = ptTranslated;
        }
    }

    default boolean isDraggable() {
        return this.getDragContext().dragHandler != null;
    }
}
