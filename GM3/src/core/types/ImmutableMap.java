/*
 *  Copyright (C) 2011, 2012
 *  This file is part of GRASSMARLIN.
 */
package core.types;

import java.util.Collection;
import java.util.HashMap;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * HashMap that auto creates entries on a failed call to get, auto created entries come from the supplier
 * provided by the constructor.
 * 
 * Key values are expected to always be the proper type.
 * 
 * There is also a pluggable toString function to make this map behave more like a proper class.
 * 
 * @param <K> Key type for the HashMap.
 * @param <V> Value type for the HashMap.
 */
public class ImmutableMap<K,V extends Collection> extends HashMap<K,V> {

    final Supplier<V> sup;
    final Function<ImmutableMap<K,V>,String> toString;
    
    public ImmutableMap(Supplier<V> sup) {
        this(sup, null);
    }
    
    public ImmutableMap(Supplier<V> sup, Function<ImmutableMap<K,V>,String> toString) {
        super();
        this.sup = sup;
        this.toString = toString;
    }
    
    public int valueSize() {
        return values().stream().mapToInt(c->c.size()).sum();
    }
    
    /* may throw class cast exception */
    @Override
    public V get(Object key) {
        V val = super.get(key);
        
        if( val == null ) {
            val = sup.get();
            put((K)key,val);
        }
            
        return val;
    }
    
    @Override
    public String toString() {
        if( toString == null ) {
            return super.toString();
        } else {
            return toString.apply(this);
        }
    }
}
