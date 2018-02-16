package grassmarlin.ui.common;

import com.sun.istack.internal.NotNull;
import grassmarlin.common.edit.IAction;
import grassmarlin.common.edit.IActionStack;
import grassmarlin.common.edit.IActionUndoable;
import javafx.beans.binding.BooleanExpression;
import javafx.beans.property.ReadOnlyBooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;

public class ActionStackPropertyWrapper extends SimpleObjectProperty<IActionStack> implements IActionStack {
    private final SimpleBooleanProperty canUndoProperty;
    private final SimpleBooleanProperty canRedoProperty;

    public ActionStackPropertyWrapper() {
        this(null);
    }
    public ActionStackPropertyWrapper(final IActionStack stack) {
        super(null);

        this.canUndoProperty = new SimpleBooleanProperty(this, "Can Undo", false);
        this.canRedoProperty = new SimpleBooleanProperty(this, "Can Redo", false);

        if(stack != null) {
            this.set(stack);
        }
    }

    @Override
    public void doAction(@NotNull IAction action) {
        final IActionStack value = this.get();
        if(value != null) {
            value.doAction(action);
            this.updateBindings(value);
        }
    }

    @Override
    public IActionUndoable undo() {
        final IActionStack value = this.get();
        if(value != null) {
            final IActionUndoable result = value.undo();
            this.updateBindings(value);
            return result;
        } else {
            return null;
        }
    }
    @Override
    public IActionUndoable redo() {
        final IActionStack value = this.get();
        if(value != null) {
            final IActionUndoable result = value.redo();
            this.updateBindings(value);
            return result;
        } else {
            return null;
        }
    }

    @Override
    public boolean canUndo() {
        final IActionStack value = this.get();
        if(value != null) {
            return value.canUndo();
        } else {
            return false;
        }
    }

    @Override
    public boolean canRedo() {
        final IActionStack value = this.get();
        if(value != null) {
            return value.canRedo();
        } else {
            return false;
        }
    }

    @Override
    public void clear() {
        final IActionStack value = this.get();
        if(value != null) {
            value.clear();
            this.updateBindings(value);
        }
    }


    @Override
    protected void invalidated() {
        final IActionStack value = this.get();

        if(this.get() == null) {
            this.canUndoProperty.set(false);
            this.canRedoProperty.set(false);
        } else {
            this.canUndoProperty.set(value.canUndo());
            this.canRedoProperty.set(value.canRedo());
        }
    }

    @NotNull
    public BooleanExpression isUndoAvailableProperty() {
        return ReadOnlyBooleanProperty.readOnlyBooleanProperty(canUndoProperty);
    }

    @NotNull
    public BooleanExpression isRedoAvailableProperty() {
        return ReadOnlyBooleanProperty.readOnlyBooleanProperty(canRedoProperty);
    }

    protected void updateBindings(final IActionStack source) {
        if(this.get() == source) {
            this.canUndoProperty.set(source.canUndo());
            this.canRedoProperty.set(source.canRedo());
        }
    }

    public IActionStack wrapActionStack(final IActionStack stackRoot) {
        return new IActionStack() {

            @Override
            public void doAction(@NotNull IAction action) throws ActionFailedException {
                stackRoot.doAction(action);
                ActionStackPropertyWrapper.this.updateBindings(this);
            }

            @Override
            public IActionUndoable undo() throws ActionFailedException {
                final IActionUndoable result = stackRoot.undo();
                ActionStackPropertyWrapper.this.updateBindings(this);
                return result;
            }

            @Override
            public IActionUndoable redo() throws ActionFailedException {
                final IActionUndoable result = stackRoot.redo();
                ActionStackPropertyWrapper.this.updateBindings(this);
                return result;
            }

            @Override
            public boolean canUndo() {
                return stackRoot.canUndo();
            }

            @Override
            public boolean canRedo() {
                return stackRoot.canRedo();
            }

            @Override
            public void clear() {
                stackRoot.clear();
                ActionStackPropertyWrapper.this.updateBindings(this);
            }
        };
    }
}
