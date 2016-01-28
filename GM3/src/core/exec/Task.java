/*
 *  Copyright (C) 2011, 2012
 *  This file is part of GRASSMARLIN.
 */

package core.exec;

import core.Pipeline;

/**
 * <pre>
 * Task is used to create a modular and sequenctial operation model for PCAP processing.
 * Depending on the type of traffic and whether or not fingerprinting is available 
 * a task may or may not be necessary to run at all.
 * 
 * Tasks have an internal {@code  nextTask} which is run on completion of the current task 
 * at some point in the future after it is put on the dispatch work queue and removed to run.
 * 
 * Note that {@code Task::complete} must be called in each {@code Task::run} method.
 * </pre>
 */
public abstract class Task implements Runnable {
    /** flag to indicate completion */
    protected boolean isComplete;
    /** access to important application components */
    protected Pipeline pipeline;
    /** next task to run */
    protected Task nextTask;

    /**
     * constructs a new task object with default values
     */
    protected Task() {
        this.isComplete = false;
        this.nextTask = null;
        this.pipeline = null;
    }
    
    /**
     * Must be explicitely called in each Tasks run method.
     */
    protected void complete() {
        this.isComplete = true;
        if( hasNext() ) {
            this.pipeline.taskDispatcher().accept(this.nextTask);
        }
    }

    /**
     * Indicates this task has another task to run.
     * @return True if there is a pending task, else false.
     */
    public boolean hasNext() {
        return this.nextTask != null;
    }

    /**
     * Sets the Next Task in for this Task.
     * @param task Task to be run on completion of this Task.
     */
    public final void setNext(Task task) {
        nextTask = task;
    }

    /**
     * Retrieve the next task to run.
     * @return Task to run next.
     */
    public Task getNext() {
        return this.nextTask;
    }

    /**
     * Indicates this task is complete and ready to be reaped or reused.
     * @return True if complete, else false.
     */
    public boolean isComplete() {
        return this.isComplete;
    }
 
    /**
     * @param pipeline Reference to the pipeline provides near global access to important application components.
     * @return This reference is returned so that methods may be chained.
     */
    public Task setPipeline(Pipeline pipeline) {
        this.pipeline = pipeline;
        return this;
    }
    
    /**
     * All tasks are provided a reference to the pipeline so a task can manipulate/store data.
     * @return Reference to the pipeline WILL never be null.
     */
    public Pipeline getPipeline() {
        return this.pipeline;
    }
    
}
