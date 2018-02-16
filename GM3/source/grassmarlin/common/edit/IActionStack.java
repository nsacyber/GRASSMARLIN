package grassmarlin.common.edit;

import com.sun.istack.internal.NotNull;

public interface IActionStack {
    class ActionFailedException extends RuntimeException {
        private final IAction action;

        public ActionFailedException(final IAction action) {
            super("An action returned a failure condition.");

            this.action = action;
        }
        public ActionFailedException(final IAction action, final String message) {
            super(message);

            this.action = action;
        }
        public ActionFailedException(final IAction action, final Exception innerException) {
            super("An action failed to return.", innerException);

            this.action = action;
        }
    }

    void doAction(@NotNull IAction action) throws ActionFailedException;

    IActionUndoable undo() throws ActionFailedException;
    IActionUndoable redo() throws ActionFailedException;

    boolean canUndo();
    boolean canRedo();

    void clear();
}
