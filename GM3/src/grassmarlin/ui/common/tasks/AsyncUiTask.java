package grassmarlin.ui.common.tasks;

import grassmarlin.common.fxobservables.FxBooleanProperty;
import grassmarlin.common.fxobservables.FxStringProperty;
import javafx.application.Platform;
import javafx.beans.property.*;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 *
 * @param <T> The class to be passed to the events; this must be a superclass of the instantiated type.
 */
@SuppressWarnings("unchecked")
public abstract class AsyncUiTask<T extends AsyncUiTask> {
    @FunctionalInterface
    public interface TaskCallback<T extends AsyncUiTask> {
        void run(T self);
    }

    protected final DoubleProperty progress;
    protected final FxStringProperty descriptionSubtask;
    private final FxBooleanProperty hasProgressBar;
    private final StringProperty descriptionTask;

    private final TaskCallback<T> fnOnSuccess;
    private final TaskCallback<T> fnOnFailure;
    private final TaskCallback<T> fnOnCancel;

    protected AsyncUiTask(final String description, final boolean hasProgressBar, final TaskCallback<T> fnOnSuccess, final TaskCallback<T> fnOnFailure, final TaskCallback<T> fnOnCancel) {
        this.progress = new SimpleDoubleProperty(0.0);
        this.hasProgressBar = new FxBooleanProperty(hasProgressBar);
        this.descriptionSubtask = new FxStringProperty((String)null);
        this.descriptionTask = new ReadOnlyStringWrapper(description);

        this.fnOnSuccess = fnOnSuccess;
        this.fnOnFailure = fnOnFailure;
        this.fnOnCancel = fnOnCancel;
    }

    protected abstract boolean task(final AtomicBoolean shouldStop);

    public void execute(final AtomicBoolean shouldStop) {
        final boolean result = task(shouldStop);

        if (shouldStop.get()) {
            handleCancel((T)this);
        } else {
            if(result) {
                handleSuccess((T)this);
            } else {
                handleFailure((T)this);
            }
        }
    }

    protected void handleCancel(T self) {
        if(this.fnOnCancel != null) {
            Platform.runLater(() -> AsyncUiTask.this.fnOnCancel.run((T)self));
        }
    }
    protected void handleSuccess(T self) {
        if(this.fnOnSuccess != null) {
            Platform.runLater(() -> AsyncUiTask.this.fnOnSuccess.run((T)self));
        }
    }
    protected void handleFailure(T self) {
        if(this.fnOnFailure != null) {
            Platform.runLater(() -> AsyncUiTask.this.fnOnFailure.run((T)self));
        }
    }


    public BooleanProperty hasProgressBarProperty() {
        return this.hasProgressBar;
    }
    public DoubleProperty progressProperty() {
        return this.progress;
    }
    public StringProperty subtaskDescriptionProperty() {
        return this.descriptionSubtask;
    }
    public StringProperty taskDescriptionProperty() {
        return this.descriptionTask;
    }
}
