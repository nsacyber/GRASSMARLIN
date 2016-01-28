/*
 Copyright 1999 CERN - European Organization for Nuclear Research.
 Permission to use, copy, modify, distribute and sell this software and its documentation for any purpose 
 is hereby granted without fee, provided that the above copyright notice appear in all copies and 
 that both that copyright notice and this permission notice appear in supporting documentation. 
 CERN makes no representations about the suitability of this software for any purpose. 
 It is provided "as is" without expressed or implied warranty.
 */
package prefuse.util.collections;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * Hash map holding (key,value) associations of type <tt>(int-->Object)</tt>;
 * Automatically grows and shrinks as needed; Implemented using open addressing
 * with double hashing. First see the <a href="package-summary.html">package
 * summary</a> and javadoc <a href="package-tree.html">tree view</a> to get
 * the broad picture.
 * 
 * This class has been adapted from the corresponding class in the COLT
 * library for scientfic computing.
 * 
 * @author wolfgang.hoschek@cern.ch
 * @version 1.0, 09/24/99
 * @see java.util.HashMap
 */
public class IntObjectHashMap extends AbstractHashMap implements Cloneable {
    
    protected static final int defaultCapacity = 277;
    protected static final double defaultMinLoadFactor = 0.2;
    protected static final double defaultMaxLoadFactor = 0.5;
    
    protected static final byte FREE = 0;
    protected static final byte FULL = 1;
    protected static final byte REMOVED = 2;
    
    /**
     * The hash table keys.
     */
    protected int table[];

    /**
     * The hash table values.
     */
    protected Object values[];

    /**
     * The state of each hash table entry (FREE, FULL, REMOVED).
     */
    protected byte state[];

    /**
     * The number of table entries in state==FREE.
     */
    protected int freeEntries;
    
    /**
     * Constructs an empty map with default capacity and default load factors.
     */
    public IntObjectHashMap() {
        this(defaultCapacity);
    }

    /**
     * Constructs an empty map with the specified initial capacity and default
     * load factors.
     * 
     * @param initialCapacity
     *            the initial capacity of the map.
     * @throws IllegalArgumentException
     *             if the initial capacity is less than zero.
     */
    public IntObjectHashMap(int initialCapacity) {
        this(initialCapacity, defaultMinLoadFactor, defaultMaxLoadFactor);
    }

    /**
     * Constructs an empty map with the specified initial capacity and the
     * specified minimum and maximum load factor.
     * 
     * @param initialCapacity
     *            the initial capacity.
     * @param minLoadFactor
     *            the minimum load factor.
     * @param maxLoadFactor
     *            the maximum load factor.
     * @throws IllegalArgumentException
     *             if
     *             <tt>initialCapacity < 0 || (minLoadFactor < 0.0 || minLoadFactor >= 1.0) || (maxLoadFactor <= 0.0 || maxLoadFactor >= 1.0) || (minLoadFactor >= maxLoadFactor)</tt>.
     */
    public IntObjectHashMap(int initialCapacity, double minLoadFactor,
            double maxLoadFactor) {
        setUp(initialCapacity, minLoadFactor, maxLoadFactor);
    }

    /**
     * Removes all (key,value) associations from the receiver. Implicitly calls
     * <tt>trimToSize()</tt>.
     */
    public void clear() {
        Arrays.fill(state, FREE);
        Arrays.fill(values, null);
        
        this.distinct = 0;
        this.freeEntries = table.length; // delta
        trimToSize();
    }

    /**
     * Returns a deep copy of the receiver.
     * @return a deep copy of the receiver.
     */
    public Object clone() {
        try {
            IntObjectHashMap copy = (IntObjectHashMap) super.clone();
            copy.table = (int[]) copy.table.clone();
            copy.values = (Object[]) copy.values.clone();
            copy.state = (byte[]) copy.state.clone();
            return copy;
        } catch (CloneNotSupportedException e) {
            // won't happen
            return null;
        }
    }

    /**
     * Returns <tt>true</tt> if the receiver contains the specified key.
     * @return <tt>true</tt> if the receiver contains the specified key.
     */
    public boolean containsKey(int key) {
        return indexOfKey(key) >= 0;
    }

    /**
     * Returns <tt>true</tt> if the receiver contains the specified value.
     * @return <tt>true</tt> if the receiver contains the specified value.
     */
    public boolean containsValue(Object value) {
        return indexOfValue(value) >= 0;
    }

    /**
     * Ensures that the receiver can hold at least the specified number of
     * associations without needing to allocate new internal memory. If
     * necessary, allocates new internal memory and increases the capacity of
     * the receiver.
     * <p>
     * This method never need be called; it is for performance tuning only.
     * Calling this method before <tt>put()</tt>ing a large number of
     * associations boosts performance, because the receiver will grow only once
     * instead of potentially many times and hash collisions get less probable.
     * 
     * @param minCapacity
     *            the desired minimum capacity.
     */
    public void ensureCapacity(int minCapacity) {
        if (table.length < minCapacity) {
            int newCapacity = nextPrime(minCapacity);
            rehash(newCapacity);
        }
    }

    /**
     * Returns the value associated with the specified key. It is often a good
     * idea to first check with {@link #containsKey(int)} whether the given key
     * has a value associated or not, i.e. whether there exists an association
     * for the given key or not.
     * 
     * @param key
     *            the key to be searched for.
     * @return the value associated with the specified key; <tt>null</tt> if
     *         no such key is present.
     */
    public Object get(int key) {
        int i = indexOfKey(key);
        if (i < 0)
            return null; // not contained
        return values[i];
    }

    /**
     * @param key
     *            the key to be added to the receiver.
     * @return the index where the key would need to be inserted, if it is not
     *         already contained. Returns -index-1 if the key is already
     *         contained at slot index. Therefore, if the returned index < 0,
     *         then it is already contained at slot -index-1. If the returned
     *         index >= 0, then it is NOT already contained and should be
     *         inserted at slot index.
     */
    protected int indexOfInsertion(int key) {
        final int tab[] = table;
        final byte stat[] = state;
        final int length = tab.length;

        final int hash = key & 0x7FFFFFFF;
        int i = hash % length;
        // double hashing, see http://www.eece.unm.edu/faculty/heileman/hash/node4.html
        int decrement = hash % (length - 2);
        // int decrement = (hash / length) % length;
        if (decrement == 0)
            decrement = 1;

        // stop if we find a removed or free slot, or if we find the key itself
        // do NOT skip over removed slots (yes, open addressing is like that...)
        while (stat[i] == FULL && tab[i] != key) {
            i -= decrement;
            // hashCollisions++;
            if (i < 0)
                i += length;
        }

        if (stat[i] == REMOVED) {
            // stop if we find a free slot, or if we find the key itself.
            // do skip over removed slots (yes, open addressing is like that...)
            // assertion: there is at least one FREE slot.
            int j = i;
            while (stat[i] != FREE && (stat[i] == REMOVED || tab[i] != key)) {
                i -= decrement;
                // hashCollisions++;
                if (i < 0)
                    i += length;
            }
            if (stat[i] == FREE)
                i = j;
        }

        if (stat[i] == FULL) {
            // key already contained at slot i.
            // return a negative number identifying the slot.
            return -i - 1;
        }
        // not already contained, should be inserted at slot i.
        // return a number >= 0 identifying the slot.
        return i;
    }

    /**
     * @param key
     *            the key to be searched in the receiver.
     * @return the index where the key is contained in the receiver, returns -1
     *         if the key was not found.
     */
    protected int indexOfKey(int key) {
        final int tab[] = table;
        final byte stat[] = state;
        final int length = tab.length;

        final int hash = key & 0x7FFFFFFF;
        int i = hash % length;
        // double hashing, see http://www.eece.unm.edu/faculty/heileman/hash/node4.html
        int decrement = hash % (length - 2);
        // int decrement = (hash / length) % length;
        if (decrement == 0)
            decrement = 1;

        // stop if we find a free slot, or if we find the key itself.
        // do skip over removed slots (yes, open addressing is like that...)
        while (stat[i] != FREE && (stat[i] == REMOVED || tab[i] != key)) {
            i -= decrement;
            // hashCollisions++;
            if (i < 0)
                i += length;
        }

        if (stat[i] == FREE)
            return -1; // not found
        return i; // found, return index where key is contained
    }

    /**
     * @param value
     *            the value to be searched in the receiver.
     * @return the index where the value is contained in the receiver, returns
     *         -1 if the value was not found.
     */
    protected int indexOfValue(Object value) {
        final Object val[] = values;
        final byte stat[] = state;

        for (int i = stat.length; --i >= 0;) {
            if (stat[i] == FULL && val[i] == value)
                return i;
        }

        return -1; // not found
    }

    /**
     * Returns the first key the given value is associated with. It is often a
     * good idea to first check with {@link #containsValue(Object)} whether
     * there exists an association from a key to this value.
     * 
     * @param value the value to search for.
     * @return the first key for which holds <tt>get(key) == value</tt>;
     *         returns <tt>Integer.MIN_VALUE</tt> if no such key exists.
     */
    public int keyOf(Object value) {
        // returns the first key found; there may be more matching keys,
        // however.
        int i = indexOfValue(value);
        if (i < 0)
            return Integer.MIN_VALUE;
        return table[i];
    }

    /**
     * Fills all keys contained in the receiver into the specified list. Fills
     * the list, starting at index 0. After this call returns the specified list
     * has a new size that equals <tt>this.size()</tt>.
     * <p>
     * This method can be used to iterate over the keys of the receiver.
     * 
     * @param list
     *            the list to be filled 
     */
    public int keys(int[] list) {
        int[] tab = table;
        byte[] stat = state;

        if ( list.length < distinct )
            return -1;
        
        int j = 0;
        for (int i = tab.length; i-- > 0;) {
            if (stat[i] == FULL)
                list[j++] = tab[i];
        }
        return distinct;
    }

    /**
     * Associates the given key with the given value. Replaces any old
     * <tt>(key,someOtherValue)</tt> association, if existing.
     * 
     * @param key
     *            the key the value shall be associated with.
     * @param value
     *            the value to be associated.
     * @return <tt>true</tt> if the receiver did not already contain such a
     *         key; <tt>false</tt> if the receiver did already contain such a
     *         key - the new value has now replaced the formerly associated
     *         value.
     */
    public boolean put(int key, Object value) {
        int i = indexOfInsertion(key);
        if (i < 0) { // already contained
            i = -i - 1;
            this.values[i] = value;
            return false;
        }

        if (this.distinct > this.highWaterMark) {
            int newCapacity = chooseGrowCapacity(this.distinct + 1,
                    this.minLoadFactor, this.maxLoadFactor);
            rehash(newCapacity);
            return put(key, value);
        }

        this.table[i] = key;
        this.values[i] = value;
        if (this.state[i] == FREE)
            this.freeEntries--;
        this.state[i] = FULL;
        this.distinct++;

        if (this.freeEntries < 1) { // delta
            int newCapacity = chooseGrowCapacity(this.distinct + 1,
                    this.minLoadFactor, this.maxLoadFactor);
            rehash(newCapacity);
        }

        return true;
    }

    /**
     * Rehashes the contents of the receiver into a new table with a smaller or
     * larger capacity. This method is called automatically when the number of
     * keys in the receiver exceeds the high water mark or falls below the low
     * water mark.
     */
    protected void rehash(int newCapacity) {
        int oldCapacity = table.length;
        // if (oldCapacity == newCapacity) return;

        int oldTable[] = table;
        Object oldValues[] = values;
        byte oldState[] = state;

        int newTable[] = new int[newCapacity];
        Object newValues[] = new Object[newCapacity];
        byte newState[] = new byte[newCapacity];

        this.lowWaterMark = chooseLowWaterMark(newCapacity, this.minLoadFactor);
        this.highWaterMark = chooseHighWaterMark(newCapacity,
                this.maxLoadFactor);

        this.table = newTable;
        this.values = newValues;
        this.state = newState;
        this.freeEntries = newCapacity - this.distinct; // delta

        for (int i = oldCapacity; i-- > 0;) {
            if (oldState[i] == FULL) {
                int element = oldTable[i];
                int index = indexOfInsertion(element);
                newTable[index] = element;
                newValues[index] = oldValues[i];
                newState[index] = FULL;
            }
        }
    }

    /**
     * Removes the given key with its associated element from the receiver, if
     * present.
     * 
     * @param key
     *            the key to be removed from the receiver.
     * @return <tt>true</tt> if the receiver contained the specified key,
     *         <tt>false</tt> otherwise.
     */
    public boolean removeKey(int key) {
        int i = indexOfKey(key);
        if (i < 0)
            return false; // key not contained

        this.state[i] = REMOVED;
        this.values[i] = null; // delta
        this.distinct--;

        if (this.distinct < this.lowWaterMark) {
            int newCapacity = chooseShrinkCapacity(this.distinct,
                    this.minLoadFactor, this.maxLoadFactor);
            rehash(newCapacity);
        }

        return true;
    }

    /**
     * Initializes the receiver.
     * 
     * @param initialCapacity
     *            the initial capacity of the receiver.
     * @param minLoadFactor
     *            the minLoadFactor of the receiver.
     * @param maxLoadFactor
     *            the maxLoadFactor of the receiver.
     * @throws IllegalArgumentException
     *             if
     *             <tt>initialCapacity < 0 || (minLoadFactor < 0.0 || minLoadFactor >= 1.0) || (maxLoadFactor <= 0.0 || maxLoadFactor >= 1.0) || (minLoadFactor >= maxLoadFactor)</tt>.
     */
    protected void setUp(int initialCapacity, double minLoadFactor,
            double maxLoadFactor) {
        int capacity = initialCapacity;
        super.setUp(capacity, minLoadFactor, maxLoadFactor);
        capacity = nextPrime(capacity);
        if (capacity == 0)
            capacity = 1; // open addressing needs at least one FREE slot at any time.

        this.table = new int[capacity];
        this.values = new Object[capacity];
        this.state = new byte[capacity];

        // memory will be exhausted long before this pathological case happens, anyway.
        this.minLoadFactor = minLoadFactor;
        if (capacity == PrimeFinder.largestPrime)
            this.maxLoadFactor = 1.0;
        else
            this.maxLoadFactor = maxLoadFactor;

        this.distinct = 0;
        this.freeEntries = capacity; // delta

        // lowWaterMark will be established upon first expansion.
        // establishing it now (upon instance construction) would immediately make the table shrink upon first put(...).
        // After all the idea of an "initialCapacity" implies violating lowWaterMarks when an object is young.
        // See ensureCapacity(...)
        this.lowWaterMark = 0;
        this.highWaterMark = chooseHighWaterMark(capacity, this.maxLoadFactor);
    }

    /**
     * Trims the capacity of the receiver to be the receiver's current 
     * size. Releases any superfluous internal memory. An application can use this operation to minimize the 
     * storage of the receiver.
     */
    public void trimToSize() {
        // * 1.2 because open addressing's performance exponentially degrades beyond that point
        // so that even rehashing the table can take very long
        int newCapacity = nextPrime((int) (1 + 1.2 * size()));
        if (table.length > newCapacity) {
            rehash(newCapacity);
        }
    }

    /**
     * Fills all values contained in the receiver into the specified list.
     * Fills the list, starting at index 0.
     * After this call returns the specified list has a new size that equals
     * <tt>this.size()</tt>.
     * <p>
     * This method can be used to iterate over the values of the receiver.
     *
     * @param list the list to be filled, can have any size.
     */
    public void values(ArrayList list) {
        Object[] val = values;
        byte[] stat = state;

        for (int i = stat.length; i-- > 0;) {
            if (stat[i] == FULL)
                list.add(val[i]);
        }
    }
    
} // end of class IntObjectHashMap
