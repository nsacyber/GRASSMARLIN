package prefuse.util.collections;

import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;

/**
 * Sorted map implementation using a red-black tree to map from Object keys to
 * int values.
 * 
 * @author <a href="http://jheer.org">jeffrey heer</a>
 */
public class ObjectIntTreeMap extends AbstractTreeMap
    implements ObjectIntSortedMap
{
    
    // dummy entry used as wrapper for queries
    private ObjectEntry dummy = new ObjectEntry(null, Integer.MIN_VALUE, NIL, 0);
    private Comparator cmp = null;
    
    // ------------------------------------------------------------------------
    // Constructors
    
    public ObjectIntTreeMap() {
        this(null, false);
    }
    
    public ObjectIntTreeMap(boolean allowDuplicates) {
        this(null, allowDuplicates);
    }
    
    public ObjectIntTreeMap(Comparator comparator) {
        this(comparator, false);
    }
    
    public ObjectIntTreeMap(Comparator comparator, boolean allowDuplicates) {
        super(null, allowDuplicates);
        this.cmp = (comparator == null ? super.comparator() : comparator);
    }
    
    /**
     * @see java.util.SortedMap#comparator()
     */
    public Comparator comparator() {
        return cmp;
    }
    
    // ------------------------------------------------------------------------
    // SortedMap Methods

    /**
     * @see java.util.Map#containsKey(java.lang.Object)
     */
    public boolean containsKey(Object key) {
        return find(key, 0) != NIL;
    }

    /**
     * @see java.util.Map#get(java.lang.Object)
     */
    public int get(Object key) {
        Entry ret = find(key, 0);
        return ( ret == NIL ? Integer.MIN_VALUE : ret.val );
    }

    /**
     * @see java.util.Map#put(java.lang.Object, java.lang.Object)
     */
    public int put(Object key, int value) {
        Entry t = root;
        lastOrder = 0;
        
        if (t == NIL) {
            incrementSize(true);
            root = new ObjectEntry(key, value, NIL, lastOrder);
            return Integer.MIN_VALUE;
        }

        dummy.key = key;
        dummy.order = Integer.MAX_VALUE;
        
        while (true) {
            int cmp = compare(dummy, t);
            if (cmp == 0) {
                return t.setValue(value);
            } else if (cmp < 0) {
                if (t.left != NIL) {
                    t = t.left;
                } else {
                    incrementSize(lastOrder==0);
                    t.left = new ObjectEntry(key, value, t, lastOrder);
                    fixUpInsert(t.left);
                    return Integer.MIN_VALUE;
                }
            } else { // cmp > 0
                if (t.right != NIL) {
                    t = t.right;
                } else {
                    incrementSize(lastOrder==0);
                    t.right = new ObjectEntry(key, value, t, lastOrder);
                    fixUpInsert(t.right);
                    return Integer.MIN_VALUE;
                }
            }
        }
    }

    /**
     * @see java.util.Map#remove(java.lang.Object)
     */
    public int remove(Object key) {
        // remove the last instance with the given key
        Entry x;
        if ( allowDuplicates )
            x = findPredecessor(key, Integer.MAX_VALUE);
        else
            x = find(key, 0);
        
        if (x == NIL)
            return Integer.MIN_VALUE;

        int val = x.val;
        remove(x);
        return val;
    }

    public int remove(Object key, int val) {
        // remove the last instance with the given key
        Entry x = findCeiling(key, 0);
        if ( x!=NIL && ((key==null && x.getKey()!=null) || 
                        (key!=null && !x.getKey().equals(key))) )
            x = successor(x);
        if (x==NIL || ((key==null && x.getKey()!=null)
                   ||  (key!=null && !x.getKey().equals(key))) )
            return Integer.MIN_VALUE;

        for ( ; x.val != val && x != NIL; x = successor(x) );
        if (x == NIL) return Integer.MIN_VALUE;
        
        remove(x);
        return val;
    }
    
    /**
     * @see java.util.SortedMap#firstKey()
     */
    public Object firstKey() {
        return minimum(root).getKey();
    }
    
    /**
     * @see java.util.SortedMap#lastKey()
     */
    public Object lastKey() {
        return maximum(root).getKey();
    }
    
    // -- Collection view methods ---------------------------------------------

    public Iterator keyIterator() {
        return new KeyIterator();
    }
    
    public Iterator keyRangeIterator(Object fromKey, boolean fromInc,
                                     Object toKey,   boolean toInc)
    {
        Entry start, end;
        
        if ( fromKey == toKey && (fromKey == MIN_KEY || fromKey == MAX_KEY) )
            return Collections.EMPTY_LIST.iterator();
        
        boolean bmin = (fromKey == MIN_KEY || toKey == MAX_KEY);
        boolean bmax = (fromKey == MAX_KEY || toKey == MIN_KEY);
        
        if ( !bmax && (bmin || cmp.compare(fromKey, toKey) <= 0) ) {
            start = findCeiling(fromKey, (fromInc ? 0 : Integer.MAX_VALUE));
            end = findCeiling(toKey, (toInc? Integer.MAX_VALUE : 0));
        } else {
            start = findCeiling(fromKey, (fromInc ? Integer.MAX_VALUE : 0));
            start = predecessor(start);
            end = findCeiling(toKey, (toInc ? 0 : Integer.MAX_VALUE));
            end = predecessor(end);
        }
        return new KeyIterator(start, end);
    }
    
    public IntIterator valueRangeIterator(Object fromKey, boolean fromInc, 
                                          Object toKey,   boolean toInc)
    {
        return new ValueIterator(
           (EntryIterator)keyRangeIterator(fromKey,fromInc,toKey,toInc));
    }
    
    // ------------------------------------------------------------------------
    // Internal Binary Search Tree / Red-Black Tree methods
    // Adapted from Cormen, Leiserson, and Rivest's Introduction to Algorithms
    
    protected int compare(Entry e1, Entry e2) {
        Object k1 = e1.getKey(), k2 = e2.getKey();
        
        if ( k1 == k2 && (k1 == MIN_KEY || k1 == MAX_KEY) ) {
            return 0;
        } else if ( k1 == MIN_KEY || k2 == MAX_KEY ) {
            return -1;
        } else if ( k1 == MAX_KEY || k2 == MIN_KEY ) {
            return 1;
        }
        
        int c = cmp.compare(e1.getKey(), e2.getKey());
        if ( allowDuplicates ) {
            if ( c == 0 ) {
                c = (e1.order < e2.order ? -1 : (e1.order > e2.order ? 1 : 0));
                lastOrder = 1 + (c < 0 ? e1.order : e2.order);
            }
        }
        return c;
    }
    
    private Entry find(Object key, int order) {
        dummy.key = key;
        dummy.order = order;
        Entry e = find(dummy);
        dummy.key = null;
        return e;
    }
    
    private Entry findPredecessor(Object key, int order) {
        dummy.key = key;
        dummy.order = order;
        Entry e = findPredecessor(dummy);
        dummy.key = null;
        return e;
    }
    
    private Entry findCeiling(Object key, int order) {
        dummy.key = key;
        dummy.order = order;
        Entry e = findCeiling(dummy);
        dummy.key = null;
        return e;
    }
    
    // ========================================================================
    // Inner classes
    
    // ------------------------------------------------------------------------
    // Entry class - represents a Red-Black Tree Node
    
    static class ObjectEntry extends AbstractTreeMap.Entry {
        Object key;
        
        public ObjectEntry(Object key, int val) {
            super(val);
            this.key = key;
        }
        
        public ObjectEntry(Object key, int val, Entry parent, int order) {
            super(val, parent, order);
            this.key = key;
        }
        
        public Object getKey() {
            return key;
        }
        
        public void copyFields(Entry x) {
            super.copyFields(x);
            this.key = x.getKey();
        }
        
    }
    
} // end of class DuplicateTreeMap
