/*
 *  Copyright (C) 2011, 2012
 *  This file is part of GRASSMARLIN.
 */

package core.types;

import java.util.LinkedList;

/**
 *
 * @param <E> Object.class this List will hold
 */
public class EvictingList<E> extends LinkedList<E> {
    
    int limit;
    
    public EvictingList(int limit) {
        this.limit = limit;
    }
    
    @Override
    public boolean add(E e) {
        boolean b = super.add(e);
        if( this.size() > limit )
            while( size() > limit )
                remove();
        return b;
    }
    
}
