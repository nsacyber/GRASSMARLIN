/*
 *  Copyright (C) 2011, 2012
 *  This file is part of GRASSMARLIN.
 */
package ui.views.graph.logical.watch;

import core.types.InvokeObservable;
import ui.views.tree.visualnode.VisualNode;

/**
 *
 * @author BESTDOG
 * @param <T>
 */
public interface Watch<T extends Watch> {
    void update(VisualNode root);
    int getId();
    void setId(int id);
    
    default InvokeObservable getWatchRequest() {
        return null;
    }
    
    default InvokeObservable getSubnetChangeRequest() {
        return null;
    }
    
    default InvokeObservable getFocusRequest() {
        return null;
    }
    
    default Watch<T> setWatchRequest(InvokeObservable invokeObservable) {
        return this;
    }
    
    default Watch<T> setSubnetChangeRequest(InvokeObservable invokeObservable) {
        return this;
    }
    
    default Watch<T> setFocusRequest(InvokeObservable invokeObservable) {
        return this;
    }

    /***
     * Removes the Watch from the originating data.
     */
    default Watch<T> close() {
        return this;
    }

}
