/*
 *  Copyright (C) 2011, 2012
 *  This file is part of GRASSMARLIN.
 */
package core.importmodule;

import core.importmodule.parser.Bro2Reader;
import core.importmodule.parser.AbstractReader;
import core.Core;
import core.exec.Bro2ChunkProcessorTask;
import core.types.FullEvictingList;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * ImportItem that supports the type of progress counting used in Bro2.
 * 
 * It will read {@link #CHUNK_SIZE} entries, one line per entry, and create a task
 * for each chunk.
 */
public class Bro2Import extends ImportItem {
    /**
     * The reader required to import any Bro2.
     */
    private static final Bro2Reader reader = new Bro2Reader();
    /**
     * arbitrary chunk size to indicate when to break off into a new thread and
     * start processing mid-parse
     */
    private static final int CHUNK_SIZE = 650;
    
    private int maxEntry;
    private int currentEntry;
    private int newProgess;
    
    /**
     * Attempts a canonical path validating constructor of ImportItem
     * @param path Path of the import file. MUST RESOLVE TRUE OR EXCEPTION IS THROWN.
     */
    public Bro2Import(String path) {
        super(path, Import.Bro2, true);
        newProgess =
        maxEntry =
        currentEntry = 0;
    }
    
    public void setMaxEntry(int maxEntry) {
        this.maxEntry = maxEntry;
    }

    public void setCurrentEntry(int currentEntry) {
        this.currentEntry = currentEntry;
        updateProgress();
    }
    
    public int getCurrentEntry() {
        return currentEntry;
    }
    
    @Override
    public void run() {
        
        FullEvictingList<Map<Trait,Object>> evictingList = new FullEvictingList<>(
                Bro2Import.CHUNK_SIZE,
                this::submitNewChunkTask,
                this::setCurrentEntry
        );
        
        try {
            /* max entry is the max point for progress */
            maxEntry = AbstractReader.countLines(this);
            
            if( maxEntry == 0 ) {
                throw new FileNotFoundException("Bro log is empty.");
            }
            
            /* read the actual log file */
            reader.accept(this, evictingList);
            
            /* process any entries that left over less than the CHUNK_SIZE */
            if( !evictingList.isEmpty() ) {
                submitNewChunkTask(evictingList);
            }
            
            /* if current Entry never changed */
            if( currentEntry < 1 ) {
                log(this, Core.ALERT.DANGER, "Failed to parse any log entries.");
            } else {
                setCurrentEntry(maxEntry);
            }
            
        } catch (FileNotFoundException ex) {
            Logger.getLogger(Bro2Import.class.getName()).log(Level.SEVERE, null, ex);
        }
        
    }
    
    /**
     * Copy construct a new array list from the argument and immediately submits a new Bro2ChunkProcessorTask to run.
     * @param list List to copy into the chunk-task
     */
    private void submitNewChunkTask(List<Map<Trait, Object>> list) {
        List<Map<Trait, Object>> newList = new ArrayList<>(list);

        Bro2ChunkProcessorTask nextBroTask = new Bro2ChunkProcessorTask(this, newList);

        getImporter().run(nextBroTask);
    }
    
    
    @Override
    public void updateProgress() {
        int thisProgress = (int)( (currentEntry * 100.0f) / maxEntry );
        if( thisProgress != newProgess ) {
            newProgess = thisProgress < 99 ? thisProgress : 100; // rounding error
            super.forceUpdateProgress();
        }
    }
   
    @Override
    public Integer getProgress() {
        return newProgess;
    }
    
}
