/*
 *  Copyright (C) 2011, 2012
 *  This file is part of GRASSMARLIN.
 */
package core.importmodule;

import core.Core.ALERT;
import core.types.InvokeObservable;
import core.types.LogEmitter;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Observable;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 */
public class ImportItem extends File implements Runnable {

    /**
     * Import this item is routed to.
     */
    Import type;
    /**
     * Value between 0 - 100 to indicate work done on this item.
     */
    Integer progress;
    /**
     * Indicates this item is a candidate to be imported.
     */
    boolean included;
    /**
     * Will auto log to the System.out if true, else false and silent
     */
    boolean logToConsol;
    /**
     * values uses to determine the amount of data worked.
     */
    long bytesRead, bytesThreashold, tasks;
    /**
     * Internal Observers used to provoke external events
     */
    InvokeObservable progressObserver, loggerObserver;
    /**
     * List of logs - usually for users
     */
    List<LogEmitter.Log> logs;
    /**
     * Reference to the importer that will run this command object.
     */
    Importer importer;
    
    final String canonicalPath;
    private boolean failed;

    protected ImportItem(String path, Import type, boolean initializeWithTrueCanonicalPath) {
        super(path);
        this.type = type;
        failed = false;
        included = false;
        logToConsol = false;
        progress = 0;
        bytesRead = 0L;
        if (exists()) {
            bytesThreashold = length() / 100;
        }
        progressObserver = new InvokeObservable(this);
        loggerObserver = new InvokeObservable(this);
        logs = Collections.synchronizedList(new ArrayList<LogEmitter.Log>());
        if( initializeWithTrueCanonicalPath ) {
            try {
                this.canonicalPath = this.getCanonicalPath();
            } catch (IOException ex) {
                Logger.getLogger(ImportItem.class.getName()).log(Level.SEVERE, null, ex);
                throw new java.lang.ExceptionInInitializerError("The import file does not seem to be valid");
            }
        } else {
            this.canonicalPath = path;
        }
    }
    
    public ImportItem(String path, Import type) {
        this(path, type, true);
    }

    public ImportItem(String path) {
        this(path, Import.None);
    }

    public Importer getImporter() {
        return importer;
    }
    
    /**
     * @return True if this import has an updated progress indicator, else false.
     */
    public boolean allowsProgress() {
        return true;
    }

    public Integer getProgress() {
        return progress;
    }

    /**
     * Import.NONE items cannot be processed and will always return false.
     * @return True if item's type is not NONE and is marked as included.
     */
    public Boolean isIncluded() {
        return !Import.None.equals(type) && included;
    }

    /**
     * Mark this item as included in the current import.
     *
     * @param b True to include this item and process it, else false to skip it.
     */
    public void setInclude(boolean b) {
        included = b;
    }

    public void setType(Import t) {
        type = t;
    }

    public Boolean hasTasks() {
        return tasks > 0;
    }

    public void notifyTaskCreation() {
        tasks++;
    }

    public void notifyTaskCompletion() {
        tasks--;
        if (isComplete()) {
            updateProgress();
        }
    }

    public long getTaskCount() {
        return tasks;
    }

    public Boolean isComplete() {
        return progress >= 100 && !hasTasks();
    }

    public Import getType() {
        return type;
    }

    /**
     * @return True if this item exists and can be read from disk.
     */
    public boolean isGood() {
        return exists() && canRead();
    }

    /**
     * Called when this item is fully read. Since time in processing could be
     * indeterminate we jump to half done. Tasks still report the rest.
     *
     * @return this, instance is returned so that methods may be chained.
     */
    public ImportItem readComplete() {
        progress = 100;
        updateProgress();
        return this;
    }

    public Observable getProgressObserver() {
        return progressObserver;
    }

    public Observable getLogObserver() {
        return loggerObserver;
    }

    public void setLogToConsole(boolean logToConsol) {
        this.logToConsol = logToConsol;
    }

    public void log(Object invoker, ALERT level, Object msg) {
        logs.add(new LogEmitter.Log(invoker, level, msg));
        loggerObserver.setChanged();
        if (this.logToConsol) {
            System.out.println(msg);
        }
    }

    public void pushLog(Object invoker, ALERT level, Object msg) {
        log(invoker, level, msg);
        loggerObserver.notifyObservers(this);
    }

    public List<LogEmitter.Log> getLog() {
        return logs;
    }

    /**
     * Default method to update the progress of an ImportItem is to tell it how
     * many bytes where just read. Will notify observers on each 1% tick.
     *
     * @param bytes Bytes read so far.
     * @param taskDone will reduce the amount of tasks if true, else tasks will
     * not be changed.
     */
    public void updateProgress(int bytes, boolean taskDone) {
        if (bytes == 0) {
            return;
        }
        bytesRead += bytes;
        if (taskDone) {
            tasks--;
        }
        updateProgress();
    }

    /**
     * Notifies observers when A) there is significant progress, B) import is
     * complete
     */
    public void updateProgress() {
        if (bytesRead >= bytesThreashold) {
            bytesRead -= bytesThreashold;
            if (progress < 100) {
                progress++;
            }
            forceUpdateProgress();
        } else if (isComplete()) {
            forceUpdateProgress();
        }
    }

    @Override
    public String toString() {
        return String.format("name:%s, selected:%b, method:%s", getName(), included, type.name());
    }

    public String getSafeCanonicalPath() {
        return this.canonicalPath;
    }
    
    /**
     * Resets progress and notifies observers.
     */
    public void reset() {
        progress = 0;
        bytesRead = 0;
        forceUpdateProgress();
    }

    void forceUpdateProgress() {
        progressObserver.setChanged();
        progressObserver.notifyObservers(this);
    }

    public void setImporter(Importer importer) {
        this.importer = importer;
    }
    
    public boolean failed() {
        return failed;
    }
    
    
    public void fail() {
        this.failed = true;
        reset();
    }
    
    public void fail(String msg) {
        fail();
        LogEmitter.factory.get().emit(this, ALERT.DANGER, this.getName() + ": " + msg);
        Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, msg);
    }
    
    /**
     * Override to force cancellation of tasks.
     */
    public void cancel() {}
    
    @Override
    public void run() {};

}
