/*
 *  Copyright (C) 2011, 2012
 *  This file is part of GRASSMARLIN.
 */
package core.exportmodule;

import core.Pipeline;
import core.exec.Task;
import java.io.File;

/**
 *
 */
public abstract class ExportItem extends Task {

    boolean async, isOnQueue;
    
    public ExportItem() {
        super();
        async = true;
        isOnQueue = false;
    }
    
    public abstract File getExportFile();

    public abstract void export( Pipeline pipeline, File file );
    
    public boolean isAsync() {
        return async;
    }
    
    public void setAsync(boolean async) {
        this.async = async;
    }
    
    @Override
    public void run() {
        if( isAsync() && !isOnQueue ) {
            isOnQueue = true;
            getPipeline().taskDispatcher().accept(this);
        } else {
            export( getPipeline(), getExportFile() );
        }
    }
    
    @Override
    protected void complete() {
        isOnQueue = false;
        super.complete();
    }
    
}
