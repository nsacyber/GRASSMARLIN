/*
 *  Copyright (C) 2011, 2012
 *  This file is part of GRASSMARLIN.
 */
package core.importmodule;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * HashMap used to construct abstract data vectors that contain the useable data
 * found in all flat file ImportItems.
 */
public class TraitMap extends HashMap<Trait, Object> {

    Trait identity;

    public TraitMap(Trait identity) {
        this.identity = identity;
    }

    public TraitMap(TraitMap other) {
        super(other);
        this.identity = other.identity;
    }

    public Trait getIdentity() {
        return identity;
    }
    
    /**
     * Creates a new TraitMap as a branch of the vector.
     *
     * @param key Trait to branch on.
     * @return New branch.
     */
    public TraitMap newBranch(Trait key) {
        TraitMap newMap = new TraitMap(key);
        if (this.containsKey(key)) {
            throw new java.lang.IllegalArgumentException("Cannot branch on existing vector.");
        } else {
            put(key, newMap);
        }
        return newMap;
    }

    /**
     * Gets an existing branch.
     *
     * @param key Trait key for the branch.
     * @return Existing branch, will throw if null.
     */
    public TraitMap getBranch(Trait key) {
        return (TraitMap) this.get(key);
    }

    /**
     * Creates a new List of TraitMaps for the given key, entering the list as
     * an entry.
     *
     * @param key Key t
     * @return new List of TraitMaps.
     */
    public List<TraitMap> newList(Trait key) {
        List<TraitMap> list = new ArrayList<>();
        put(key, list);
        return list;
    }

    /**
     * Gets an existing list.
     *
     * @param key Trait key for the list.
     * @return Existing list, will throw if null.
     */
    public List<TraitMap> getList(Trait key) {
        return (List<TraitMap>) this.get(key);
    }

    public List<TraitMap> getOrCreateList(Trait key) {
        List<TraitMap> list;
        if (this.containsKey(key)) {
            list = getList(key);
        } else {
            list = newList(key);
        }
        return list;
    }

    public TraitMap getOrCreateBranch(Trait key) {
        TraitMap map;
        if (this.containsKey(key)) {
            map = getBranch(key);
        } else {
            map = newBranch(key);
        }
        return map;
    }

    public String getString(Trait key) {
        return (String) this.get(key);
    }

    /**
     * Returns the Integer value of the Trait key.
     * @param key Key of an stored Integer value.
     * @return Integer on success -1 on failure.
     */
    public Integer getInteger(Trait key) {
        return getInteger(key, -1);
    }
    
    public Integer getInteger(Trait key, Integer defaultValue) {
        Integer returnVal;
        Object o = this.get(key);
        if( o != null && o instanceof Integer ) {
            returnVal = (Integer) o;
        } else {
            returnVal = defaultValue;
        }
        return returnVal;
    }
    
    public Long getLong(Trait key) {
        Object o = this.get(key);
        return o == null ? null : o instanceof Long ? (Long) o : null;
    }
    
    public String prettyPrint() {
        StringBuilder sb = new StringBuilder();
        prettyPrint(this, sb, 0);
        return sb.toString();
    }

    private void prettyPrint(TraitMap map, StringBuilder sb, int depth) {
        for (Map.Entry<Trait, Object> ent : map.entrySet()) {
            Trait key = ent.getKey();
            Object val = ent.getValue();

            for (int i = depth; i > -1; --i) {
                sb.append(" ");
            }
            sb.append(key.toString()).append("\n");

            if (key.type == List.class) {
                List l = (List) val;
                l.forEach(v -> {
                    for (int i = depth + 1; i > -1; --i) {
                        sb.append(" ");
                    }
                    sb.append(v.toString()).append("\n");
                });
            } else if (val.getClass().equals(TraitMap.class)) {
                prettyPrint((TraitMap) val, sb, depth + 1);
            } else {
                for (int i = depth + 1; i > -1; --i) {
                    sb.append(" ");
                }
                sb.append(val.toString()).append("\n");
            }

        }
    }

    @Override
    public Object put(Trait key, Object value) {
        if (!key.type.isAssignableFrom(value.getClass())) {
            throw new IllegalArgumentException(String.format("Trait %s expects %s, found %s", key.name(), key.type, value.getClass()));
        }
        return super.put(key, value);
    }

    public Object parseThenPut(Trait key, String value) {
        if (!key.hasParseMethod()) {
            throw new IllegalArgumentException(String.format("Trait %s has no parse method.", key.name()));
        }
        return put(key, key.parse(value));
    }

    public boolean is(Trait identity) {
        return this.identity.equals(identity);
    }

    public void convertTo(Trait newIdentity) {
        this.identity = newIdentity;
    }

    /**
     * Calls {@link String#trim() } before calling {@link #put(core.importmodule.Trait, java.lang.Object)
     * }.
     *
     * @param key Trait key.
     * @param value String value.
     * @return The last value for this key or NULL if no value existed.
     */
    public Object putString(Trait key, String value) {
        return this.put(key, value.trim());
    }

    public void check() {
        if (this.isEmpty()) {
            System.err.println(String.format("Encoutnered empty map \"%s\"", this.identity.name()));
        } else {
            checkSelf();
            forEach(this::check);
        }
    }

    private void checkSelf() {
        if( this.identity.canValidate() ) {
            List<Class> classes = values().stream().map(Object::getClass).collect(Collectors.toList());
            boolean result =  this.identity.requiredClasses.stream().allMatch( c -> {
                boolean res = classes.contains( c );
                if( !res ) {
                    System.err.printf("\"%s\" missing class \"%s\".\n", identity, c.getName());
                }
                return res;
            });
            if( result ) {
                System.err.printf("\"%s\" has missing required classes.\n", identity);
            }
        }
    }
    
    private void check(Trait t, Object v) {
        if (v instanceof TraitMap) {
            ((TraitMap) v).check();
        } else if (v instanceof List) {
            List l = (List) v;
            if (l.isEmpty()) {
                System.err.println(String.format("Encoutnered empty list \"%s\"", t.name()));
            } else {
                l.forEach(item -> {
                    check(t, item);
                });
            }
        } else {
            /* good */
        }
    }

}
