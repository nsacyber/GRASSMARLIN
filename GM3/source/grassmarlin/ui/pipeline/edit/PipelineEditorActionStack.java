package grassmarlin.ui.pipeline.edit;

import grassmarlin.common.edit.ActionStack;
import grassmarlin.common.edit.IAction;
import grassmarlin.common.edit.IActionUndoable;
import javafx.beans.binding.BooleanExpression;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;

public class PipelineEditorActionStack extends ActionStack {

    private int cleanPoint;
    private final BooleanProperty atCleanPoint;

    public PipelineEditorActionStack(int size) {
        super(size);
        this.atCleanPoint = new SimpleBooleanProperty();
        this.setCleanPoint();
    }

    @Override
    public void doAction(IAction action) {
        IAction previous = this.undoBuffer.peekFirst();
        if (action instanceof ActionDrag) {
            if (previous instanceof ActionDrag && ((ActionDrag) action).getToDrag() == ((ActionDrag) previous).getToDrag()) {
                ((ActionDrag) action).setPtCurrent(((ActionDrag) this.undoBuffer.pollFirst()).getPtCurrent());
            }
            if (this.atCleanPoint.get()) {
                super.doAction(action);
                this.setCleanPoint();
            } else {
                super.doAction(action);
                this.atCleanPoint.set(false);
            }
        } else {
            super.doAction(action);
            this.atCleanPoint.set(false);
        }
    }

    @Override
    public synchronized IActionUndoable undo() {
        IActionUndoable undone = super.undo();

        if (this.undoBuffer.size() == cleanPoint) {
            this.atCleanPoint.set(true);
        } else if (!(undone instanceof ActionDrag)) {
            this.atCleanPoint.set(false);
        } else if (this.atCleanPoint.get()){
            this.setCleanPoint();
        }

        return undone;
    }

    @Override
    public synchronized IActionUndoable redo() {
        IActionUndoable redone = super.redo();

        if (this.undoBuffer.size() == cleanPoint) {
            this.atCleanPoint.set(true);
        } else if (!(redone instanceof ActionDrag)){
            this.atCleanPoint.set(false);
        } else if (this.atCleanPoint.get()){
            this.setCleanPoint();
        }

        return redone;
    }

    @Override
    public synchronized  void clear() {
        super.clear();
        this.setCleanPoint();
    }

    public synchronized void setCleanPoint() {
        this.cleanPoint = this.undoBuffer.size();
        this.atCleanPoint.set(true);
    }

    public synchronized BooleanExpression isAtCleanPoint() {
        return ReadOnlyBooleanProperty.readOnlyBooleanProperty(this.atCleanPoint.asObject());
    }
}
