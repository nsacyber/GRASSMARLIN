/*
 *  Copyright (C) 2011, 2012
 *  This file is part of GRASSMARLIN.
 */
package ui.views.graph;

import java.util.concurrent.atomic.AtomicBoolean;
import prefuse.action.ActionList;

/**
 * A multipurpose optional Action which only runs when a shared AtomicBoolean is unlocked (false).
 */
public class NoOverlapActionList extends ActionList {
    
    private final static AtomicBoolean PERMA_UNLOCKED = new AtomicBoolean(false);
    final AtomicBoolean lock;
    
    public NoOverlapActionList(AtomicBoolean lock) {
        this.lock = lock == null ? PERMA_UNLOCKED : lock;
    }
    
    @Override
    public void run(double frac) {
        if( lock.compareAndSet(false, true)) {
            super.run(frac); // for list
            runIfSafe(frac); // for individual action
            lock.lazySet(false);
        } /*else {
        System.out.println("Action avoided.");
        }*/
    }
    
    protected void runIfSafe(double frac) {
        
    }
    
}
