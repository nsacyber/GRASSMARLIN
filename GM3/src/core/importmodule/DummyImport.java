/*
 *  Copyright (C) 2011, 2012
 *  This file is part of GRASSMARLIN.
 */
package core.importmodule;

/**
 * @author BESTDOG An ImportItem which SHOULD be used with {@link Import#None}.
 */
public class DummyImport extends ImportItem {

    /**
     * Import that can never be run or complete.
     * @param path Use like {@link java.io.File}.
     */
    public DummyImport(String path) {
        super(path);
    }
    
    @Override
    public boolean isGood() {
        return false;
    }
    
    @Override
    public Boolean isComplete() {
        return false;
    }
    
}
