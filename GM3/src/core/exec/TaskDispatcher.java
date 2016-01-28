package core.exec;

// core
import core.Pipeline;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Executor service must be sub-classes anyway. java.util.concurrent.Executors'
 * implementations are best.
 *
 * We use this as an opportunity to add the pipeline to a task.
 *
 * The pipeline should be the access to the end point storage and the
 * fingerprinting framework.
 *
 * TaskDispatcher will calculate sample runtime statistics and sleep most of the
 * time.
 */
public class TaskDispatcher implements Runnable {

    public static final String DEFAULT_MONITOR_NAME = "Thread Monitor";
    public static final int DEFAULT_TIMEOUT_MS = 10000;

    final ExecutorService wexec;
    final Pipeline pipeline;

    long timeout;

    /**
     * @param pipeline Reference to the pipeline provided to all Task object
     * that will run through this TaskDispatcher.
     */
    public TaskDispatcher(Pipeline pipeline) {
        this.pipeline = pipeline;
        this.timeout = DEFAULT_TIMEOUT_MS;
        this.wexec = Executors.newCachedThreadPool();
    }

    /**
     * @return The timeout used if and when shutdown is called.
     */
    public long getTimeout() {
        return timeout;
    }

    /**
     * @param timeout Timeout to be used if and when shutdown is called.
     */
    public void setTimeout(long timeout) {
        this.timeout = timeout;
    }

    /**
     * @param tasks Each WILL be given a reference to the Pipeline and run with ExecutorService::execute.
     */
    public void accept(List<Task> tasks) {
        if (tasks.isEmpty()) {
            return;
        }
        tasks.forEach(task -> wexec.execute(applyBundle(task)) );
    }

    /**
     * @param task WILL be given a reference to the Pipeline and run with ExecutorService::execute.
     */
    public void accept(Task task) {
        wexec.execute(applyBundle(task));
    }

    /**
     * Attaches the internal Pipeline to the provided task and returns it.
     * @param task Task to attach the Pipeline to.
     * @return Reference to the provided Task.
     */
    public Task applyBundle(Task task) {
        return task.setPipeline(pipeline);
    }
    
    /**
     * @return ExecutorService used to execute Tasks.
     */
    public ExecutorService getWexec() {
        return wexec;
    }

    /**
     * Shuts down the ExecutorService for the set timeout in ms. 
     * @throws InterruptedException If interrupted while waiting. 
     */
    public void shutDown() throws InterruptedException {
        wexec.awaitTermination(timeout, TimeUnit.MILLISECONDS);
    }
    
    @Override
    public void run() {

    }
}
