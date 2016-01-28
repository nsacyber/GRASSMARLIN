/*
 *  Copyright (C) 2011, 2012
 *  This file is part of GRASSMARLIN.
 */
package core.importmodule;

import core.exec.CiscoImportTask;
import core.importmodule.parser.cisco.CiscoReader;

/**
 * ImportItem for Cisco "Show" commands
 */
public class CiscoImport extends ImportItem {

    private static final CiscoReader reader = new CiscoReader();

    int maxEntry;
    int currentEntry;
    int currentProgress;

    public CiscoImport(String path) {
        super(path, Import.CiscoShow);

        maxEntry = 0;
        currentEntry = 0;
        currentProgress = 0;
    }

    @Override
    public void run() {
        this.getImporter().run(new CiscoImportTask(this, reader));
    }

    /**
     * Set the maximum amount of entries, when the amount of
     * {@link #currentEntry}s reaches this limit, progress will be reported as
     * finished.
     *
     * @param maxEntry The new value for maxEntry.
     */
    public void setMaxEntry(int maxEntry) {
        this.maxEntry = maxEntry;
    }
    
    public void reportEntryComplete() {
        this.currentEntry++;
        this.updateProgress();
    }

    @Override
    public void updateProgress() {
        int thisProgress = (int)( (currentEntry * 100.0f) / maxEntry );
        if( thisProgress != currentProgress ) {
            currentProgress = thisProgress < 99 ? thisProgress : 100; // rounding error
            super.forceUpdateProgress();
        }
    }
 
    @Override
    public Boolean isComplete() {
        return currentEntry != 0 && currentEntry == maxEntry;
    }
    
    @Override
    public Integer getProgress() {
        return currentProgress;
    }
    
    @Override
    public void reset() {
        super.reset();
        this.currentEntry = 0;
        this.currentProgress = 0;
    }
    
}
