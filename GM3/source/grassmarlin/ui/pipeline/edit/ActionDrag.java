package grassmarlin.ui.pipeline.edit;

import grassmarlin.common.edit.IActionUndoable;
import grassmarlin.ui.common.IDraggable;
import javafx.geometry.Point2D;
import javafx.scene.Node;

public class ActionDrag implements IActionUndoable {
    private Point2D ptCurrent;
    private final Point2D ptDestination;
    private final IDraggable toDrag;

    public ActionDrag(IDraggable toDrag, Point2D ptDestination) {
        this.ptCurrent = new Point2D(((Node)toDrag).getTranslateX(), ((Node)toDrag).getTranslateY());
        this.ptDestination = ptDestination;
        this.toDrag = toDrag;
    }

    @Override
    public boolean doAction() {
        final Point2D ptTranslated = ptDestination.subtract(toDrag.getDragContext().ptOrigin);

        //Only update position if not bound
        Node node = (Node)toDrag;
        if(!node.translateXProperty().isBound()) {
            node.translateXProperty().set(node.getTranslateX() - toDrag.getDragContext().ptPrevious.getX() + ptTranslated.getX());
        }
        if(!node.translateYProperty().isBound()) {
            node.translateYProperty().set(node.getTranslateY() - toDrag.getDragContext().ptPrevious.getY() + ptTranslated.getY());
        }


        // To avoid floating point rounding errors we will reverse this translation and apply a new absolute translation next time.
        toDrag.getDragContext().ptPrevious = ptTranslated;

        return true;
    }

    @Override
    public boolean undoAction() {
        ((Node)this.toDrag).translateXProperty().set(ptCurrent.getX());
        ((Node)this.toDrag).translateYProperty().set(ptCurrent.getY());

        return true;
    }

    public void setPtCurrent(Point2D ptCurrent) {
        this.ptCurrent = ptCurrent;
    }

    public Point2D getPtCurrent() {
        return this.ptCurrent;
    }

    public IDraggable getToDrag() {
        return this.toDrag;
    }
}