/*
 *  Copyright (C) 2011, 2012
 *  This file is part of GRASSMARLIN.
 */

package core.types;

import java.io.Serializable;
import java.util.Observable;

/**
 * <pre>
 * An Observer with a public acces to setChanged.
 * </pre>
 */
public class InvokeObservable extends Observable implements Serializable {

    final Object invoker;
    
    public InvokeObservable(Object o) {
        invoker = o;
    }
    
    public Object getInvoker() {
        return invoker;
    }
    
    public Class getInvokationClass() {
        return invoker.getClass();
    }
    
    @Override
    public void notifyObservers(Object arg) {
        this.setChanged();
        super.notifyObservers(arg);
    }
    
    @Override
    public void setChanged() {
        super.setChanged();
    }
    
}
