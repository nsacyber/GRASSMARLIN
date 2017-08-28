package grassmarlin.ui.common.tasks;

import grassmarlin.Logger;
import grassmarlin.RuntimeConfiguration;
import grassmarlin.session.Session;
import grassmarlin.session.serialization.Writer;
import grassmarlin.ui.sdi.DocumentState;
import javafx.application.Platform;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.atomic.AtomicBoolean;

public class SavingTask extends AsyncUiTask<SavingTask> {
    private final Session session;
    private final Path path;
    private final RuntimeConfiguration config;

    private final DocumentState state;

    public SavingTask(final RuntimeConfiguration config, final DocumentState state, final AsyncUiTask.TaskCallback<SavingTask> onSuccess, final AsyncUiTask.TaskCallback<SavingTask> onFailure) {
        this(config, state, Paths.get(state.currentSessionPathProperty().get()), onSuccess, onFailure);
    }

    public SavingTask(final RuntimeConfiguration config, final DocumentState state, final Path path, final AsyncUiTask.TaskCallback<SavingTask> onSuccess, final AsyncUiTask.TaskCallback<SavingTask> onFailure) {
        super("Saving", true, onSuccess, onFailure, null);

        this.config = config;
        this.state = state;
        this.session = state.getSession();
        this.path = path;
    }

    @Override
    protected void handleSuccess(SavingTask self) {
        Platform.runLater(() -> this.state.dirtyProperty().set(false));
        super.handleSuccess(self);
    }

    @Override
    public boolean task(AtomicBoolean shouldStop) {
        boolean result = Writer.writeSession(path, session, config, this);
        if(result) {
            Logger.log(Logger.Severity.COMPLETION, "The session has been saved.");
        } else {
            Logger.log(Logger.Severity.WARNING, "The session could not be saved.");
        }
        return result;
    }
}
