package grassmarlin.common.edit;

import com.sun.istack.internal.NotNull;

import java.util.ArrayDeque;
import java.util.Deque;

public class ActionStack implements IActionStack {
    public static final int DEFAULT_CAPACITY = 50;

    protected final Deque<IActionUndoable> undoBuffer;
    protected final Deque<IActionUndoable> redoBuffer;

    public ActionStack(final Deque<IActionUndoable> undoBuffer, final Deque<IActionUndoable> redoBuffer) {
        this.undoBuffer = undoBuffer;
        this.redoBuffer = redoBuffer;
    }

    public ActionStack(final int bufferSize) {
        this(new ArrayDeque<>(bufferSize), new ArrayDeque<>(bufferSize));
    }

    public ActionStack() {
        this(DEFAULT_CAPACITY);
    }

    @Override
    public synchronized void doAction(@NotNull IAction action) throws ActionFailedException {
        final boolean success;
        try {
            success = action.doAction();
        } catch(Exception ex) {
            this.clear();
            throw new ActionFailedException(action, ex);
        }
        if(!success) {
            throw new ActionFailedException(action);
        }
        this.redoBuffer.clear();

        if(action instanceof IActionUndoable) {
            while(!undoBuffer.offerFirst((IActionUndoable)action)) {
                undoBuffer.pollLast();
            }
        } else {
            this.undoBuffer.clear();
        }
    }

    @Override
    public synchronized IActionUndoable undo() throws ActionFailedException {
        IActionUndoable action = undoBuffer.pollFirst();
        if (action != null) {
            final boolean success;
            try {
                success = action.undoAction();
            } catch(Exception ex) {
                this.clear();
                throw new ActionFailedException(action, ex);
            }
            if(!success) {
                undoBuffer.offerFirst(action);
                throw new ActionFailedException(action);
            }

            while(!redoBuffer.offerFirst(action)) {
                redoBuffer.pollLast();
            }
        }
        return action;
    }

    @Override
    public synchronized IActionUndoable redo() throws ActionFailedException {
        IActionUndoable action = redoBuffer.pollFirst();
        if (action != null) {
            final boolean success;
            try {
                success = action.doAction();
            } catch(Exception ex) {
                this.clear();
                throw new ActionFailedException(action, ex);
            }
            if(!success) {
                this.redoBuffer.offerFirst(action);
                throw new ActionFailedException(action);
            }

            while(!undoBuffer.offerFirst(action)) {
                undoBuffer.pollLast();
            }
        }
        return action;
    }

    @Override
    public synchronized void clear() {
        this.undoBuffer.clear();
        this.redoBuffer.clear();
    }

    public boolean canUndo() {
        return !undoBuffer.isEmpty();
    }

    public boolean canRedo() {
        return !redoBuffer.isEmpty();
    }
}
