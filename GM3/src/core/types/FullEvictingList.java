/*
 *  Copyright (C) 2011, 2012
 *  This file is part of GRASSMARLIN.
 */
package core.types;

import java.util.ArrayList;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

/**
 * A list with a limit, and a List-Consumer, when the limit of entries is hit the 
 * consumer accepts(this) and the list then clears itself.
 * @param <E> List entry type.
 */
public class FullEvictingList<E> extends ArrayList<E> {
    /**
     * called when {@link #limit} is hit.
     */
    final Consumer<FullEvictingList<E>> consumer;
    /**
     * Called on each new {@link #getLifetimeEntries() } value.
     */
    final IntConsumer eachEntry;
    /**
     * causes the {@link #consumer} to accept {@link #this}.
     */
    final int limit;
    /**
     * Count of all entries ever added.
     */
    int lifetimeEntries;
    
    
    
    public FullEvictingList(int limit, Consumer<FullEvictingList<E>> consumer, IntConsumer eachEntry) {
        this.eachEntry = eachEntry;
        this.consumer = consumer;
        this.limit = limit;
        lifetimeEntries = 0;
    }
    
    /**
     * @return How many times this list has ever been added to, does not reset when limit is hit.
     */
    public int getLifetimeEntries() {
        return lifetimeEntries;
    }
    
    @Override
    public boolean add(E e) {
        boolean b = super.add(e);
        
        eachEntry.accept(++lifetimeEntries);
        
        if( this.size() > limit ) {
            consumer.accept(this);
            this.clear();
        }
        
        return b;
    }

}
