/*
 *  Copyright (C) 2011, 2012
 *  This file is part of GRASSMARLIN.
 */
package ui.views.graph;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import prefuse.action.Action;

/**
 * Swings threading policy and Prefuse's beta released "action pipeline" don't work well together.
 * Use this queue for any processing that effects the visualization and add this 
 * action to the beginning of any rendering or layout action list.
 */
public class QueueAction extends Action {

    final List<Runnable> actions, buffer;
    AtomicBoolean lock;
    
    public QueueAction() {
        actions = new CopyOnWriteArrayList<>();
        buffer = new CopyOnWriteArrayList<>();
        lock = new AtomicBoolean(false);
    }
    
    public void add( Runnable action ) {
        if( lock.get() ) {
            buffer.add(action);
        } else {
            actions.add(action);
        }
    }
    
    public void clear() {
        this.buffer.clear();
        this.actions.clear();
    }
    
    @Override
    public void run(double frac) {
        if( lock.compareAndSet(false, true) ) {
            actions.forEach(Runnable::run);
            actions.clear();
            lock.set(false);
            actions.addAll(buffer);
            buffer.clear();
        }
    }
    
}
