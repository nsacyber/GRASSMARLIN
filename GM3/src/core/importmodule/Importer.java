package core.importmodule;

// gm
import core.Core.ALERT;
import core.Preferences;
import core.Pipeline;
import core.exec.Task;
import core.exec.TaskDispatcher;
import core.types.LogEmitter;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Stream;

/**
 * Importer runs each ImportItem and gives them a reference to itself (the importer)
 * Importer is meant to track the completion of importItems.
 */
public class Importer {
    /* display text for states of the import session */
    public static final String IMPORT_COMPLETE_TAG = "Import Complete.";
    public static final String IMPORT_CANCEL_TAG = "Import Cancelled.";
    public static final String IMPORT_START_TAG = "Import Started.";

    protected LogEmitter logEmitter;
    protected Pipeline pipeline;
    protected TaskDispatcher dispatch;
    protected Thread executor;

    public ArrayList<ImportItem> jobs;

    private boolean verbose;
    private Preferences preferences;

    Importer() {
        super();
        jobs = new ArrayList<>();
        verbose = true;
        executor = null;
        logEmitter = LogEmitter.factory.get();
    }

    public Importer(TaskDispatcher dispatch, Pipeline pipeline, Preferences preferences) {
        this();
        this.dispatch = dispatch;
        this.pipeline = pipeline;
        this.preferences = preferences;
    }

    /**
     * Controls the level of error reporting for debugging.
     * @param verbose True will log more errors, false logs less.
     */
    public void setVerbose(boolean verbose) {
        this.verbose = verbose;
    }

    /**
     * Sets the {@link core.exec.TaskDispatcher} used to run the {@link core.exec.Task}s created by {@link core.importmodule.ImportItem}s.
     * @param dispatch TaskDispatcher to run all Tasks on.
     */
    public void setDispatch(TaskDispatcher dispatch) {
        this.dispatch = dispatch;
    }

    public LogEmitter getLogEmitter() {
        return logEmitter;
    }

    public synchronized Importer importCancel() {
        try {
            jobs.forEach(ImportItem::cancel);
            jobs.clear();
            if (executor != null) {
                executor.join(200);
                if (verbose) {
                    logEmitter.emit(this, ALERT.WARNING, IMPORT_CANCEL_TAG);
                }
            }
        } catch (InterruptedException ex) {
            if (verbose) {
                logEmitter.emit(this, ALERT.WARNING, ex.getMessage());
            }
        }
        return this;
    }

    public synchronized Importer importStartAsynch(List<ImportItem> newJobs) {
        if (executor != null) {
            if (verbose) {
                logEmitter.emit(this, ALERT.WARNING, "An Import is already in progress.");
            }
            return this;
        }

        executor = new Thread(() -> {
            try {
                jobs.clear();
                jobs.addAll(newJobs);
                importStart(jobs.stream());
            } catch (InterruptedException ex) {
                /* this exception is expected when import is cancelled */
                Logger.getLogger(Importer.class.getName()).log(Level.FINE, "Importer interrupted.", ex);
//                logEmitter.emit(this,ALERT.DANGER, ex);
            } finally {
                executor = null;
            }
        });
        executor.start();
        return this;
    }
    
    public void importSerial(List<ImportItem> items) {
        items.forEach(this::runImport);
    }

    public void importStart(Stream<ImportItem> stream) throws InterruptedException {
        logEmitter.emit(this, ALERT.MESSAGE, IMPORT_START_TAG);
        stream
                .filter(ImportItem::isIncluded)
                .filter(i -> !i.isComplete())
                .filter(i -> {
                    if (i.isGood()) {
                        return true;
                    }
                    if (verbose) {
                        i.log(this, ALERT.DANGER, "Import cannot be processed, file does not exist or does not have proper access rights.");
                    }
                    return false;
                })
                .sorted((a, b) -> {
                    return a.getType().ordinal() - b.getType().ordinal();
                })
                .forEach(this::checkAndRunItem);
        logEmitter.emit(this, ALERT.MESSAGE, IMPORT_COMPLETE_TAG);
    }

    /**
     * Checks the item before running it.
     * @param item Item to run.
     */
    private void checkAndRunItem(ImportItem item) {
        /* if spliterator is thread safe and this method is called in stream we need to check this to cancel */
        if (jobs.isEmpty()) {
            return;
        }
        if (!item.isIncluded()) {
            return;
        }
        if (verbose) {
            item.pushLog(this, ALERT.MESSAGE, "Import started for " + item.getName());
        }
        runImport(item);
        if (verbose) {
            item.pushLog(this, ALERT.MESSAGE, "Import completed for " + item.getName());
        }
        if (jobs.isEmpty()) {
            return; // don't signal that it's read if we need to cancel the import
        }
        item.readComplete();
    }
    
    /**
     * Sets the importer reference and runs the ImportItem.
     * @param item Item to run.
     */
    private void runImport(ImportItem item) {
        item.setImporter(this);
        item.run();
    }
    
    /**
     * Supplies the Task to the TaskDispatcher.
     * @param t Task to run in the future.
     */
    public void run( Task t ) {
        dispatch.accept(t);
    }
    
    /**
     * @return True if the "Importer thread" is running.
     */
    public boolean importInProgress() {
        return executor != null;
    }
    
    /**
     * @return The value of {@link Pipeline#filtersAvailable() }.
     */
    public boolean fingerprintsAvailable() {
        return pipeline.filtersAvailable();
    }
    
    /**
     * @return The global {@link core.Preferences} object.
     */
    public Preferences getPreferences() {
        return preferences;
    }

    public List<ImportItem> getActiveItems() {
        return new ArrayList<>( this.jobs );
    }

}
