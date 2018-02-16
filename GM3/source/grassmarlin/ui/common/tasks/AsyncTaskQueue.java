package grassmarlin.ui.common.tasks;

import grassmarlin.Event;
import grassmarlin.Logger;
import javafx.beans.binding.When;
import javafx.beans.property.*;

import java.util.ArrayDeque;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicBoolean;

public class AsyncTaskQueue {
    private final Queue<AsyncUiTask> tasks;
    private final AtomicBoolean shouldStop;

    private final SimpleBooleanProperty hasTask;
    private final SimpleStringProperty currentTaskDescription;
    private final SimpleStringProperty currentSubtaskDescription;
    private final DoubleProperty currentProgress;

    private final Event.IAsyncExecutionProvider uiProvider;

    public AsyncTaskQueue(final Event.IAsyncExecutionProvider uiProvider) {
        this.tasks = new ArrayDeque<>(10);
        this.shouldStop = new AtomicBoolean(false);
        this.uiProvider = uiProvider;

        this.hasTask = new SimpleBooleanProperty(false);
        this.currentTaskDescription = new SimpleStringProperty();
        this.currentSubtaskDescription = new SimpleStringProperty();
        this.currentProgress = new SimpleDoubleProperty(0.0);

        final Thread threadTasks;
        threadTasks = new Thread(this::Thread_processQueue);
        threadTasks.setName("AsyncTaskQueue");
        threadTasks.setDaemon(true);
        threadTasks.start();
    }

    public void enqueue(AsyncUiTask task) {
        tasks.offer(task);
    }

    public void cancelCurrentTask() {
        shouldStop.set(true);
    }

    private void Thread_processQueue() {
        while(true) {
            try {
                final AsyncUiTask taskNext = tasks.poll();
                if(taskNext == null) {
                    Thread.sleep(1L);
                } else {
                    shouldStop.set(false);
                    this.uiProvider.runLater(() -> {
                        currentProgress.bind(new When(taskNext.hasProgressBarProperty()).then(taskNext.progressProperty()).otherwise(new ReadOnlyDoubleWrapper(-1.0)));
                        currentTaskDescription.bind(taskNext.taskDescriptionProperty());
                        currentSubtaskDescription.bind(taskNext.subtaskDescriptionProperty());
                        hasTask.set(true);
                    });
                    try {
                        taskNext.execute(shouldStop);
                    } finally {
                        this.uiProvider.runLater(() -> {
                            hasTask.set(false);
                            currentTaskDescription.unbind();
                            currentSubtaskDescription.unbind();
                            currentProgress.unbind();
                        });
                    }
                }
            } catch(InterruptedException ex) {
                Logger.log(Logger.Severity.WARNING, "Terminating AsyncTaskQueue.");
                return;
            }
        }
    }

    public StringProperty taskProperty() {
        return this.currentTaskDescription;
    }
    public StringProperty subtaskProperty() {
        return this.currentSubtaskDescription;
    }
    public DoubleProperty progressProperty() {
        return this.currentProgress;
    }
    public BooleanProperty hasTaskProperty() {
        return this.hasTask;
    }
}
