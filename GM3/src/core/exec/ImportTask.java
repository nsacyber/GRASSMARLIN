/*
 *  Copyright (C) 2011, 2012
 *  This file is part of GRASSMARLIN.
 */
package core.exec;

import core.importmodule.ImportItem;

/**
 *
 * Task specifically tailored to updating progress on ImportItems
 *
 * @param <T> The type of ImportItems this Task will use.
 */
public abstract class ImportTask<T extends ImportItem> extends Task {

    /** The item being updated */
    protected final T importItem;

    /**
     * Constructs a new ImportTask
     * @param importItem
     */
    public ImportTask(T importItem) {
        super();
        if( importItem == null ) {
            throw new java.lang.ExceptionInInitializerError("Null ImportItem.");
        }
        this.importItem = importItem;
        notifyCreated();
    }

    /**
     * Notifies observers of creation
     */
    protected final void notifyCreated() {
        this.importItem.notifyTaskCreation();
    }

    /**
     * Notifies observers of task completion
     */
    protected final void notifyComplete() {
        this.importItem.notifyTaskCompletion();
    }

    /**
     * Flags item as complete, calls notify
     * Must be explicitly called in each Tasks run method
     */
    @Override
    protected void complete() {
        notifyComplete();
        super.isComplete = true;
        if (hasNext()) {
            getNext().setPipeline(this.getPipeline());
            super.getPipeline().taskDispatcher().accept(getNext());
        }
    }

    /**
     * Sets bytes read of import item
     * Must be explicitly called in each Tasks run method
     *
     * @param bytesRead Bytes of the import item that were just processed.
     */
    protected void complete(int bytesRead) {
        this.importItem.updateProgress(bytesRead, true);
        complete();
    }
}
