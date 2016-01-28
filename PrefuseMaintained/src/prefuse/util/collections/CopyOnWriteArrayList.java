package prefuse.util.collections;
/*
 * Written by Doug Lea with assistance from members of JCP JSR-166
 * Expert Group.  Adapted and released, under explicit permission,
 * from JDK ArrayList.java which carries the following copyright:
 *
 * Copyright 1997 by Sun Microsystems, Inc.,
 * 901 San Antonio Road, Palo Alto, California, 94303, U.S.A.
 * All rights reserved.
 *
 * This software is the confidential and proprietary information
 * of Sun Microsystems, Inc. ("Confidential Information").  You
 * shall not disclose such Confidential Information and shall use
 * it only in accordance with the terms of the license agreement
 * you entered into with Sun.
 */

import java.util.AbstractList;
import java.util.Collection;
import java.util.ConcurrentModificationException;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.NoSuchElementException;
import java.util.RandomAccess;


/**
 * A thread-safe variant of {@link java.util.ArrayList} in which all mutative
 * operations (<tt>add</tt>, <tt>set</tt>, and so on) are implemented by
 * making a fresh copy of the underlying array.
 *
 * <p> This is ordinarily too costly, but may be <em>more</em> efficient
 * than alternatives when traversal operations vastly outnumber
 * mutations, and is useful when you cannot or don't want to
 * synchronize traversals, yet need to preclude interference among
 * concurrent threads.  The "snapshot" style iterator method uses a
 * reference to the state of the array at the point that the iterator
 * was created. This array never changes during the lifetime of the
 * iterator, so interference is impossible and the iterator is
 * guaranteed not to throw <tt>ConcurrentModificationException</tt>.
 * The iterator will not reflect additions, removals, or changes to
 * the list since the iterator was created.  Element-changing
 * operations on iterators themselves (<tt>remove</tt>, <tt>set</tt>, and
 * <tt>add</tt>) are not supported. These methods throw
 * <tt>UnsupportedOperationException</tt>.
 *
 * <p>All elements are permitted, including <tt>null</tt>.
 *
 * <p>This class is a member of the
 * <a href="{@docRoot}/../guide/collections/index.html">
 * Java Collections Framework</a>.
 *
 * @since 1.5
 * @author Doug Lea
 */
public class CopyOnWriteArrayList
    implements List, RandomAccess, Cloneable, java.io.Serializable {
    private static final long serialVersionUID = 8673264195747942595L;

    /** The array, accessed only via getArray/setArray. */
    private volatile transient Object[] array;

    /**
     * This has been made public to support more efficient iteration.
     * <strong>DO NOT MODIFY this array upon getting it</strong>.
     * Otherwise you risk wreaking havoc on your list. In fact, if you are
     * not the author of this comment, you probably shouldn't use it at all.
     * @return this lists internal array
     */
    public Object[]  getArray()    { return array; }
    
    void      setArray(Object[] a) { array = a; }

    /**
     * Creates an empty list.
     */
    public CopyOnWriteArrayList() {
        setArray(new Object[0]);
    }

    /**
     * Creates a list containing the elements of the specified
     * collection, in the order they are returned by the collection's
     * iterator.
     *
     * @param c the collection of initially held elements
     * @throws NullPointerException if the specified collection is null
     */
    public CopyOnWriteArrayList(Collection c) {
        Object[] elements = new Object[c.size()];
        int size = 0;
        for (Iterator itr = c.iterator(); itr.hasNext(); ) {
            Object e = itr.next();
            elements[size++] = e;
        }
        setArray(elements);
    }

    /**
     * Creates a list holding a copy of the given array.
     *
     * @param toCopyIn the array (a copy of this array is used as the
     *        internal array)
     * @throws NullPointerException if the specified array is null
     */
    public CopyOnWriteArrayList(Object[] toCopyIn) {
        copyIn(toCopyIn, 0, toCopyIn.length);
    }

    /**
     * Replaces the held array with a copy of the <tt>n</tt> elements
     * of the provided array, starting at position <tt>first</tt>.  To
     * copy an entire array, call with arguments (array, 0,
     * array.length).
     * @param toCopyIn the array. A copy of the indicated elements of
     * this array is used as the internal array.
     * @param first The index of first position of the array to
     * start copying from.
     * @param n the number of elements to copy. This will be the new size of
     * the list.
     */
    private void copyIn(Object[] toCopyIn, int first, int n) {
        int limit = first + n;
        if (limit > toCopyIn.length)
            throw new IndexOutOfBoundsException();
        Object[] newElements = copyOfRange(toCopyIn, first, limit,
                                          Object[].class);
        synchronized (this) { setArray(newElements); }
    }

    /**
     * Returns the number of elements in this list.
     *
     * @return the number of elements in this list
     */
    public int size() {
        return getArray().length;
    }

    /**
     * Returns <tt>true</tt> if this list contains no elements.
     *
     * @return <tt>true</tt> if this list contains no elements
     */
    public boolean isEmpty() {
        return size() == 0;
    }

    /**
     * Test for equality, coping with nulls.
     */
    private static boolean eq(Object o1, Object o2) {
        return (o1 == null ? o2 == null : o1.equals(o2));
    }

    /**
     * static version of indexOf, to allow repeated calls without
     * needing to re-acquire array each time.
     * @param o element to search for
     * @param elements the array
     * @param index first index to search
     * @param fence one past last index to search
     * @return index of element, or -1 if absent
     */
    private static int indexOf(Object o, Object[] elements,
                               int index, int fence) {
        if (o == null) {
            for (int i = index; i < fence; i++)
                if (elements[i] == null)
                    return i;
        } else {
            for (int i = index; i < fence; i++)
                if (o.equals(elements[i]))
                    return i;
        }
        return -1;
    }

    /**
     * static version of lastIndexOf.
     * @param o element to search for
     * @param elements the array
     * @param index first index to search
     * @return index of element, or -1 if absent
     */
    private static int lastIndexOf(Object o, Object[] elements, int index) {
        if (o == null) {
            for (int i = index; i >= 0; i--)
                if (elements[i] == null)
                    return i;
        } else {
            for (int i = index; i >= 0; i--)
                if (o.equals(elements[i]))
                    return i;
        }
        return -1;
    }

    /**
     * Returns <tt>true</tt> if this list contains the specified element.
     * More formally, returns <tt>true</tt> if and only if this list contains
     * at least one element <tt>e</tt> such that
     * <tt>(o==null&nbsp;?&nbsp;e==null&nbsp;:&nbsp;o.equals(e))</tt>.
     *
     * @param o element whose presence in this list is to be tested
     * @return <tt>true</tt> if this list contains the specified element
     */
    public boolean contains(Object o) {
        Object[] elements = getArray();
        return indexOf(o, elements, 0, elements.length) >= 0;
    }

    /**
     * {@inheritDoc}
     */
    public int indexOf(Object o) {
        Object[] elements = getArray();
        return indexOf(o, elements, 0, elements.length);
    }


    /**
     * Returns the index of the first occurrence of the specified element in
     * this list, searching forwards from <tt>index</tt>, or returns -1 if
     * the element is not found.
     * More formally, returns the lowest index <tt>i</tt> such that
     * <tt>(i&nbsp;&gt;=&nbsp;index&nbsp;&amp;&amp;&nbsp;(e==null&nbsp;?&nbsp;get(i)==null&nbsp;:&nbsp;e.equals(get(i))))</tt>,
     * or -1 if there is no such index.
     *
     * @param e element to search for
     * @param index index to start searching from
     * @return the index of the first occurrence of the element in
     *         this list at position <tt>index</tt> or later in the list;
     *         <tt>-1</tt> if the element is not found.
     * @throws IndexOutOfBoundsException if the specified index is negative
     */
    public int indexOf(Object e, int index) {
        Object[] elements = getArray();
        return indexOf(e, elements, index, elements.length);
    }

    /**
     * {@inheritDoc}
     */
    public int lastIndexOf(Object o) {
        Object[] elements = getArray();
        return lastIndexOf(o, elements, elements.length - 1);
    }

    /**
     * Returns the index of the last occurrence of the specified element in
     * this list, searching backwards from <tt>index</tt>, or returns -1 if
     * the element is not found.
     * More formally, returns the highest index <tt>i</tt> such that
     * <tt>(i&nbsp;&lt;=&nbsp;index&nbsp;&amp;&amp;&nbsp;(e==null&nbsp;?&nbsp;get(i)==null&nbsp;:&nbsp;e.equals(get(i))))</tt>,
     * or -1 if there is no such index.
     *
     * @param e element to search for
     * @param index index to start searching backwards from
     * @return the index of the last occurrence of the element at position
     *         less than or equal to <tt>index</tt> in this list;
     *         -1 if the element is not found.
     * @throws IndexOutOfBoundsException if the specified index is greater
     *         than or equal to the current size of this list
     */
    public int lastIndexOf(Object e, int index) {
        Object[] elements = getArray();
        return lastIndexOf(e, elements, index);
    }

    /**
     * Returns a shallow copy of this list.  (The elements themselves
     * are not copied.)
     *
     * @return a clone of this list
     */
    public Object clone() {
        try {
            return super.clone();
        } catch (CloneNotSupportedException e) {
            // this shouldn't happen, since we are Cloneable
            throw new InternalError();
        }
    }

    /**
     * Returns an array containing all of the elements in this list
     * in proper sequence (from first to last element).
     *
     * <p>The returned array will be "safe" in that no references to it are
     * maintained by this list.  (In other words, this method must allocate
     * a new array).  The caller is thus free to modify the returned array.
     *
     * <p>This method acts as bridge between array-based and collection-based
     * APIs.
     *
     * @return an array containing all the elements in this list
     */
    public Object[] toArray() {
        Object[] elements = getArray();
        return copyOf(elements, elements.length);
    }

    /**
     * Returns an array containing all of the elements in this list in
     * proper sequence (from first to last element); the runtime type of
     * the returned array is that of the specified array.  If the list fits
     * in the specified array, it is returned therein.  Otherwise, a new
     * array is allocated with the runtime type of the specified array and
     * the size of this list.
     *
     * <p>If this list fits in the specified array with room to spare
     * (i.e., the array has more elements than this list), the element in
     * the array immediately following the end of the list is set to
     * <tt>null</tt>.  (This is useful in determining the length of this
     * list <i>only</i> if the caller knows that this list does not contain
     * any null elements.)
     *
     * <p>Like the {@link #toArray()} method, this method acts as bridge between
     * array-based and collection-based APIs.  Further, this method allows
     * precise control over the runtime type of the output array, and may,
     * under certain circumstances, be used to save allocation costs.
     *
     * <p>Suppose <tt>x</tt> is a list known to contain only strings.
     * The following code can be used to dump the list into a newly
     * allocated array of <tt>String</tt>:
     *
     * <pre>
     *     String[] y = x.toArray(new String[0]);</pre>
     *
     * Note that <tt>toArray(new Object[0])</tt> is identical in function to
     * <tt>toArray()</tt>.
     *
     * @param a the array into which the elements of the list are to
     *          be stored, if it is big enough; otherwise, a new array of the
     *          same runtime type is allocated for this purpose.
     * @return an array containing all the elements in this list
     * @throws ArrayStoreException if the runtime type of the specified array
     *         is not a supertype of the runtime type of every element in
     *         this list
     * @throws NullPointerException if the specified array is null
     */
    public Object[] toArray(Object a[]) {
        Object[] elements = getArray();
        int len = elements.length;
        if (a.length < len)
            return copyOf(elements, len, a.getClass());
        else {
            System.arraycopy(elements, 0, a, 0, len);
            if (a.length > len)
                a[len] = null;
            return a;
        }
    }

    // Positional Access Operations

    /**
     * {@inheritDoc}
     *
     * @throws IndexOutOfBoundsException {@inheritDoc}
     */
    public Object get(int index) {
        return (getArray()[index]);
    }

    /**
     * Replaces the element at the specified position in this list with the
     * specified element.
     *
     * @throws IndexOutOfBoundsException {@inheritDoc}
     */
    public synchronized Object set(int index, Object element) {
        Object[] elements = getArray();
        int len = elements.length;
        Object oldValue = elements[index];

        if (oldValue != element) {
            Object[] newElements = copyOf(elements, len);
            newElements[index] = element;
            setArray(newElements);
        }
        return oldValue;
    }

    /**
     * Appends the specified element to the end of this list.
     *
     * @param e element to be appended to this list
     * @return <tt>true</tt> (as per the spec for {@link Collection#add})
     */
    public boolean add(Object e) {
        synchronized (this) {
            Object[] elements = getArray();
            int len = elements.length;
            Object[] newElements = copyOf(elements, len + 1);
            newElements[len] = e;
            setArray(newElements);
        }
        return true;
    }

    /**
     * Inserts the specified element at the specified position in this
     * list. Shifts the element currently at that position (if any) and
     * any subsequent elements to the right (adds one to their indices).
     *
     * @throws IndexOutOfBoundsException {@inheritDoc}
     */
    public synchronized void add(int index, Object element) {
        Object[] elements = getArray();
        int len = elements.length;
        if (index > len || index < 0)
            throw new IndexOutOfBoundsException("Index: " + index+
                                                ", Size: " + len);
        Object[] newElements;
        int numMoved = len - index;
        if (numMoved == 0)
            newElements = copyOf(elements, len + 1);
        else {
            newElements = new Object[len + 1];
            System.arraycopy(elements, 0, newElements, 0, index);
            System.arraycopy(elements, index, newElements, index + 1,
                             numMoved);
        }
        newElements[index] = element;
        setArray(newElements);
    }

    /**
     * Removes the element at the specified position in this list.
     * Shifts any subsequent elements to the left (subtracts one from their
     * indices).  Returns the element that was removed from the list.
     *
     * @throws IndexOutOfBoundsException {@inheritDoc}
     */
    public synchronized Object remove(int index) {
        Object[] elements = getArray();
        int len = elements.length;
        Object oldValue = elements[index];
        int numMoved = len - index - 1;
        if (numMoved == 0)
            setArray(copyOf(elements, len - 1));
        else {
            Object[] newElements = new Object[len - 1];
            System.arraycopy(elements, 0, newElements, 0, index);
            System.arraycopy(elements, index + 1, newElements, index,
                             numMoved);
            setArray(newElements);
        }
        return oldValue;
    }

    /**
     * Removes the first occurrence of the specified element from this list,
     * if it is present.  If this list does not contain the element, it is
     * unchanged.  More formally, removes the element with the lowest index
     * <tt>i</tt> such that
     * <tt>(o==null&nbsp;?&nbsp;get(i)==null&nbsp;:&nbsp;o.equals(get(i)))</tt>
     * (if such an element exists).  Returns <tt>true</tt> if this list
     * contained the specified element (or equivalently, if this list
     * changed as a result of the call).
     *
     * @param o element to be removed from this list, if present
     * @return <tt>true</tt> if this list contained the specified element
     */
    public synchronized boolean remove(Object o) {
        Object[] elements = getArray();
        int len = elements.length;
        if (len != 0) {
            // Copy while searching for element to remove
            // This wins in the normal case of element being present
            int newlen = len - 1;
            Object[] newElements = new Object[newlen];

            for (int i = 0; i < newlen; ++i) {
                if (eq(o, elements[i])) {
                    // found one;  copy remaining and exit
                    for (int k = i + 1; k < len; ++k)
                        newElements[k-1] = elements[k];
                    setArray(newElements);
                    return true;
                } else
                    newElements[i] = elements[i];
            }

            // special handling for last cell
            if (eq(o, elements[newlen])) {
                setArray(newElements);
                return true;
            }
        }
        return false;
    }

    /**
     * Removes from this list all of the elements whose index is between
     * <tt>fromIndex</tt>, inclusive, and <tt>toIndex</tt>, exclusive.
     * Shifts any succeeding elements to the left (reduces their index).
     * This call shortens the list by <tt>(toIndex - fromIndex)</tt> elements.
     * (If <tt>toIndex==fromIndex</tt>, this operation has no effect.)
     *
     * @param fromIndex index of first element to be removed
     * @param toIndex index after last element to be removed
     * @throws IndexOutOfBoundsException if fromIndex or toIndex out of
     *              range (fromIndex &lt; 0 || fromIndex &gt;= size() || toIndex
     *              &gt; size() || toIndex &lt; fromIndex)
     */
    private synchronized void removeRange(int fromIndex, int toIndex) {
        Object[] elements = getArray();
        int len = elements.length;

        if (fromIndex < 0 || fromIndex >= len ||
            toIndex > len || toIndex < fromIndex)
            throw new IndexOutOfBoundsException();
        int newlen = len - (toIndex - fromIndex);
        int numMoved = len - toIndex;
        if (numMoved == 0)
            setArray(copyOf(elements, newlen));
        else {
            Object[] newElements = new Object[newlen];
            System.arraycopy(elements, 0, newElements, 0, fromIndex);
            System.arraycopy(elements, toIndex, newElements,
                             fromIndex, numMoved);
            setArray(newElements);
        }
    }

    /**
     * Append the element if not present.
     *
     * @param e element to be added to this list, if absent
     * @return <tt>true</tt> if the element was added
     */
    public synchronized boolean addIfAbsent(Object e) {
        // Copy while checking if already present.
        // This wins in the most common case where it is not present
        Object[] elements = getArray();
        int len = elements.length;
        Object[] newElements = new Object[len + 1];
        for (int i = 0; i < len; ++i) {
            if (eq(e, elements[i]))
                return false; // exit, throwing away copy
            else
                newElements[i] = elements[i];
        }
        newElements[len] = e;
        setArray(newElements);
        return true;
    }

    /**
     * Returns <tt>true</tt> if this list contains all of the elements of the
     * specified collection.
     *
     * @param c collection to be checked for containment in this list
     * @return <tt>true</tt> if this list contains all of the elements of the
     *         specified collection
     * @throws NullPointerException if the specified collection is null
     * @see #contains(Object)
     */
    public boolean containsAll(Collection c) {
        Object[] elements = getArray();
        int len = elements.length;
        for (Iterator itr = c.iterator(); itr.hasNext(); ) {
            Object e = itr.next();
            if (indexOf(e, elements, 0, len) < 0)
                return false;
        }
        return true;
    }

    /**
     * Removes from this list all of its elements that are contained in
     * the specified collection. This is a particularly expensive operation
     * in this class because of the need for an internal temporary array.
     *
     * @param c collection containing elements to be removed from this list
     * @return <tt>true</tt> if this list changed as a result of the call
     * @throws ClassCastException if the class of an element of this list
     *         is incompatible with the specified collection (optional)
     * @throws NullPointerException if this list contains a null element and the
     *         specified collection does not permit null elements (optional),
     *         or if the specified collection is null
     * @see #remove(Object)
     */
    public synchronized boolean removeAll(Collection c) {
        Object[] elements = getArray();
        int len = elements.length;
        if (len != 0) {
            // temp array holds those elements we know we want to keep
            int newlen = 0;
            Object[] temp = new Object[len];
            for (int i = 0; i < len; ++i) {
                Object element = elements[i];
                if (!c.contains(element))
                    temp[newlen++] = element;
            }
            if (newlen != len) {
                setArray(copyOfRange(temp, 0, newlen, Object[].class));
                return true;
            }
        }
        return false;
    }

    /**
     * Retains only the elements in this list that are contained in the
     * specified collection.  In other words, removes from this list all of
     * its elements that are not contained in the specified collection.
     *
     * @param c collection containing elements to be retained in this list
     * @return <tt>true</tt> if this list changed as a result of the call
     * @throws ClassCastException if the class of an element of this list
     *         is incompatible with the specified collection (optional)
     * @throws NullPointerException if this list contains a null element and the
     *         specified collection does not permit null elements (optional),
     *         or if the specified collection is null
     * @see #remove(Object)
     */
    public synchronized boolean retainAll(Collection c) {
        Object[] elements = getArray();
        int len = elements.length;
        if (len != 0) {
            // temp array holds those elements we know we want to keep
            int newlen = 0;
            Object[] temp = new Object[len];
            for (int i = 0; i < len; ++i) {
                Object element = elements[i];
                if (c.contains(element))
                    temp[newlen++] = element;
            }
            if (newlen != len) {
                setArray(copyOfRange(temp, 0, newlen, Object[].class));
                return true;
            }
        }
        return false;
    }

    /**
     * Appends all of the elements in the specified collection that
     * are not already contained in this list, to the end of
     * this list, in the order that they are returned by the
     * specified collection's iterator.
     *
     * @param c collection containing elements to be added to this list
     * @return the number of elements added
     * @throws NullPointerException if the specified collection is null
     * @see #addIfAbsent(Object)
     */
    public int addAllAbsent(Collection c) {
        int numNew = c.size();
        if (numNew == 0)
            return 0;
        synchronized (this) {
            Object[] elements = getArray();
            int len = elements.length;

            Object[] temp = new Object[numNew];
            int added = 0;
            for (Iterator itr = c.iterator(); itr.hasNext(); ) {
                Object e = itr.next();
                if (indexOf(e, elements, 0, len) < 0 &&
                    indexOf(e, temp, 0, added) < 0)
                    temp[added++] = e;
            }
            if (added != 0) {
                Object[] newElements = new Object[len + added];
                System.arraycopy(elements, 0, newElements, 0, len);
                System.arraycopy(temp, 0, newElements, len, added);
                setArray(newElements);
            }
            return added;
        }
    }

    /**
     * Removes all of the elements from this list.
     * The list will be empty after this call returns.
     */
    public synchronized void clear() {
        setArray(new Object[0]);
    }

    /**
     * Appends all of the elements in the specified collection to the end
     * of this list, in the order that they are returned by the specified
     * collection's iterator.
     *
     * @param c collection containing elements to be added to this list
     * @return <tt>true</tt> if this list changed as a result of the call
     * @throws NullPointerException if the specified collection is null
     * @see #add(Object)
     */
    public boolean addAll(Collection c) {
        int numNew = c.size();
        if (numNew == 0)
            return false;
        synchronized (this) {
            Object[] elements = getArray();
            int len = elements.length;
            Object[] newElements = new Object[len + numNew];
            System.arraycopy(elements, 0, newElements, 0, len);
            for (Iterator itr = c.iterator(); itr.hasNext(); ) {
                Object e = itr.next();
                newElements[len++] = e;
            }
            setArray(newElements);
            return true;
        }
    }

    /**
     * Inserts all of the elements in the specified collection into this
     * list, starting at the specified position.  Shifts the element
     * currently at that position (if any) and any subsequent elements to
     * the right (increases their indices).  The new elements will appear
     * in this list in the order that they are returned by the
     * specified collection's iterator.
     *
     * @param index index at which to insert the first element
     *        from the specified collection
     * @param c collection containing elements to be added to this list
     * @return <tt>true</tt> if this list changed as a result of the call
     * @throws IndexOutOfBoundsException {@inheritDoc}
     * @throws NullPointerException if the specified collection is null
     * @see #add(int,Object)
     */
    public boolean addAll(int index, Collection c) {
        int numNew = c.size();
        synchronized (this) {
            Object[] elements = getArray();
            int len = elements.length;
            if (index > len || index < 0)
                throw new IndexOutOfBoundsException("Index: " + index +
                                                    ", Size: "+ len);
            if (numNew == 0)
                return false;
            int numMoved = len - index;
            Object[] newElements;
            if (numMoved == 0)
                newElements = copyOf(elements, len + numNew);
            else {
                newElements = new Object[len + numNew];
                System.arraycopy(elements, 0, newElements, 0, index);
                System.arraycopy(elements, index,
                                 newElements, index + numNew,
                                 numMoved);
            }
            for (Iterator itr = c.iterator(); itr.hasNext(); ) {
                Object e = itr.next();
                newElements[index++] = e;
            }
            setArray(newElements);
            return true;
        }
    }

    /**
     * Save the state of the list to a stream (i.e., serialize it).
     *
     * @serialData The length of the array backing the list is emitted
     *               (int), followed by all of its elements (each an Object)
     *               in the proper order.
     * @param s the stream
     */
    private void writeObject(java.io.ObjectOutputStream s)
        throws java.io.IOException{

        // Write out element count, and any hidden stuff
        s.defaultWriteObject();

        Object[] elements = getArray();
        int len = elements.length;
        // Write out array length
        s.writeInt(len);

        // Write out all elements in the proper order.
        for (int i = 0; i < len; i++)
            s.writeObject(elements[i]);
    }

    /**
     * Reconstitute the list from a stream (i.e., deserialize it).
     * @param s the stream
     */
    private void readObject(java.io.ObjectInputStream s)
        throws java.io.IOException, ClassNotFoundException {

        // Read in size, and any hidden stuff
        s.defaultReadObject();

        // Read in array length and allocate array
        int len = s.readInt();
        Object[] elements = new Object[len];

        // Read in all elements in the proper order.
        for (int i = 0; i < len; i++)
            elements[i] = s.readObject();
        setArray(elements);

    }

    /**
     * Returns a string representation of this list, containing
     * the String representation of each element.
     */
    public String toString() {
        Object[] elements = getArray();
        int maxIndex = elements.length - 1;
        StringBuffer buf = new StringBuffer();
        buf.append("[");
        for (int i = 0; i <= maxIndex; i++) {
            buf.append(String.valueOf(elements[i]));
            if (i < maxIndex)
                buf.append(", ");
        }
        buf.append("]");
        return buf.toString();
    }

    /**
     * Compares the specified object with this list for equality.
     * Returns true if and only if the specified object is also a {@link
     * List}, both lists have the same size, and all corresponding pairs
     * of elements in the two lists are <em>equal</em>.  (Two elements
     * <tt>e1</tt> and <tt>e2</tt> are <em>equal</em> if <tt>(e1==null ?
     * e2==null : e1.equals(e2))</tt>.)  In other words, two lists are
     * defined to be equal if they contain the same elements in the same
     * order.
     *
     * @param o the object to be compared for equality with this list
     * @return <tt>true</tt> if the specified object is equal to this list
     */
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof List))
            return false;

        List l2 = (List)(o);
        if (size() != l2.size())
            return false;

        ListIterator e1 = listIterator();
        ListIterator e2 = l2.listIterator();
        while (e1.hasNext()) {
            if (!eq(e1.next(), e2.next()))
                return false;
        }
        return true;
    }

    /**
     * Returns the hash code value for this list.
     *
     * <p>This implementation uses the definition in {@link List#hashCode}.
     *
     * @return the hash code value for this list
     */
    public int hashCode() {
        int hashCode = 1;
        Object[] elements = getArray();
        int len = elements.length;
        for (int i = 0; i < len; ++i) {
            Object obj = elements[i];
            hashCode = 31*hashCode + (obj==null ? 0 : obj.hashCode());
        }
        return hashCode;
    }

    /**
     * Returns an iterator over the elements in this list in proper sequence.
     *
     * <p>The returned iterator provides a snapshot of the state of the list
     * when the iterator was constructed. No synchronization is needed while
     * traversing the iterator. The iterator does <em>NOT</em> support the
     * <tt>remove</tt> method.
     *
     * @return an iterator over the elements in this list in proper sequence
     */
    public Iterator iterator() {
        return new COWIterator(getArray(), 0);
    }

    /**
     * {@inheritDoc}
     *
     * <p>The returned iterator provides a snapshot of the state of the list
     * when the iterator was constructed. No synchronization is needed while
     * traversing the iterator. The iterator does <em>NOT</em> support the
     * <tt>remove</tt>, <tt>set</tt> or <tt>add</tt> methods.
     */
    public ListIterator listIterator() {
        return new COWIterator(getArray(), 0);
    }

    /**
     * {@inheritDoc}
     *
     * <p>The list iterator returned by this implementation will throw an
     * <tt>UnsupportedOperationException</tt> in its <tt>remove</tt>,
     * <tt>set</tt> and <tt>add</tt> methods.
     *
     * @throws IndexOutOfBoundsException {@inheritDoc}
     */
    public ListIterator listIterator(final int index) {
        Object[] elements = getArray();
        int len = elements.length;
        if (index < 0 || index > len)
            throw new IndexOutOfBoundsException("Index: " + index);

        return new COWIterator(getArray(), index);
    }

    private static class COWIterator implements ListIterator {
        /** Snapshot of the array **/
        private final Object[] snapshot;
        /** Index of element to be returned by subsequent call to next.  */
        private int cursor;

        private COWIterator(Object[] elements, int initialCursor) {
            cursor = initialCursor;
            snapshot = elements;
        }

        public boolean hasNext() {
            return cursor < snapshot.length;
        }

        public boolean hasPrevious() {
            return cursor > 0;
        }

        public Object next() {
            try {
                return (snapshot[cursor++]);
            } catch (IndexOutOfBoundsException ex) {
                throw new NoSuchElementException();
            }
        }

        public Object previous() {
            try {
                return (snapshot[--cursor]);
            } catch (IndexOutOfBoundsException e) {
                throw new NoSuchElementException();
            }
        }

        public int nextIndex() {
            return cursor;
        }

        public int previousIndex() {
            return cursor - 1;
        }

        /**
         * Not supported. Always throws UnsupportedOperationException.
         * @throws UnsupportedOperationException always; <tt>remove</tt>
         *         is not supported by this iterator.
         */
        public void remove() {
            throw new UnsupportedOperationException();
        }

        /**
         * Not supported. Always throws UnsupportedOperationException.
         * @throws UnsupportedOperationException always; <tt>set</tt>
         *         is not supported by this iterator.
         */
        public void set(Object e) {
            throw new UnsupportedOperationException();
        }

        /**
         * Not supported. Always throws UnsupportedOperationException.
         * @throws UnsupportedOperationException always; <tt>add</tt>
         *         is not supported by this iterator.
         */
        public void add(Object e) {
            throw new UnsupportedOperationException();
        }
    }

    /**
     * Returns a view of the portion of this list between
     * <tt>fromIndex</tt>, inclusive, and <tt>toIndex</tt>, exclusive.
     * The returned list is backed by this list, so changes in the
     * returned list are reflected in this list, and vice-versa.
     * While mutative operations are supported, they are probably not
     * very useful for CopyOnWriteArrayLists.
     *
     * <p>The semantics of the list returned by this method become
     * undefined if the backing list (i.e., this list) is
     * <i>structurally modified</i> in any way other than via the
     * returned list.  (Structural modifications are those that change
     * the size of the list, or otherwise perturb it in such a fashion
     * that iterations in progress may yield incorrect results.)
     *
     * @param fromIndex low endpoint (inclusive) of the subList
     * @param toIndex high endpoint (exclusive) of the subList
     * @return a view of the specified range within this list
     * @throws IndexOutOfBoundsException {@inheritDoc}
     */
    public synchronized List subList(int fromIndex, int toIndex) {
        Object[] elements = getArray();
        int len = elements.length;
        if (fromIndex < 0 || toIndex > len  || fromIndex > toIndex)
            throw new IndexOutOfBoundsException();
        return new COWSubList(this, fromIndex, toIndex);
    }


    /**
     * Sublist for CopyOnWriteArrayList.
     * This class extends AbstractList merely for convenience, to
     * avoid having to define addAll, etc. This doesn't hurt, but
     * is wasteful.  This class does not need or use modCount
     * mechanics in AbstractList, but does need to check for
     * concurrent modification using similar mechanics.  On each
     * operation, the array that we expect the backing list to use
     * is checked and updated.  Since we do this for all of the
     * base operations invoked by those defined in AbstractList,
     * all is well.  While inefficient, this is not worth
     * improving.  The kinds of list operations inherited from
     * AbstractList are already so slow on COW sublists that
     * adding a bit more space/time doesn't seem even noticeable.
     */
    private static class COWSubList extends AbstractList {
        private final CopyOnWriteArrayList l;
        private final int offset;
        private int size;
        private Object[] expectedArray;

        // only call this holding l's lock
        private COWSubList(CopyOnWriteArrayList list,
                           int fromIndex, int toIndex) {
            l = list;
            expectedArray = l.getArray();
            offset = fromIndex;
            size = toIndex - fromIndex;
        }

        // only call this holding l's lock
        private void checkForComodification() {
            if (l.getArray() != expectedArray)
                throw new ConcurrentModificationException();
        }

        // only call this holding l's lock
        private void rangeCheck(int index) {
            if (index < 0 || index >= size)
                throw new IndexOutOfBoundsException("Index: " + index +
                                                    ",Size: " + size);
        }

        public Object set(int index, Object element) {
            synchronized (l) {
                rangeCheck(index);
                checkForComodification();
                Object x = l.set(index + offset, element);
                expectedArray = l.getArray();
                return x;
            }
        }

        public Object get(int index) {
            synchronized (l) {
                rangeCheck(index);
                checkForComodification();
                return l.get(index + offset);
            }
        }

        public int size() {
            synchronized (l) {
                checkForComodification();
                return size;
            }
        }

        public void add(int index, Object element) {
            synchronized (l) {
                checkForComodification();
                if (index<0 || index>size)
                    throw new IndexOutOfBoundsException();
                l.add(index + offset, element);
                expectedArray = l.getArray();
                size++;
            }
        }

        public void clear() {
            synchronized (l) {
                checkForComodification();
                l.removeRange(offset, offset+size);
                expectedArray = l.getArray();
                size = 0;
            }
        }

        public Object remove(int index) {
            synchronized (l) {
                rangeCheck(index);
                checkForComodification();
                Object result = l.remove(index + offset);
                expectedArray = l.getArray();
                size--;
                return result;
            }
        }

        public Iterator iterator() {
            synchronized (l) {
                checkForComodification();
                return new COWSubListIterator(l, 0, offset, size);
            }
        }

        public ListIterator listIterator(final int index) {
            synchronized (l) {
                checkForComodification();
                if (index<0 || index>size)
                    throw new IndexOutOfBoundsException("Index: "+index+
                                                        ", Size: "+size);
                return new COWSubListIterator(l, index, offset, size);
            }
        }

        public List subList(int fromIndex, int toIndex) {
            synchronized (l) {
                checkForComodification();
                if (fromIndex<0 || toIndex>size)
                    throw new IndexOutOfBoundsException();
                return new COWSubList(l, fromIndex + offset,
                                         toIndex + offset);
            }
        }

    }


    private static class COWSubListIterator implements ListIterator {
        private final ListIterator i;
        private final int offset;
        private final int size;
        private COWSubListIterator(List l, int index, int offset,
                                   int size) {
            this.offset = offset;
            this.size = size;
            i = l.listIterator(index + offset);
        }

        public boolean hasNext() {
            return nextIndex() < size;
        }

        public Object next() {
            if (hasNext())
                return i.next();
            else
                throw new NoSuchElementException();
        }

        public boolean hasPrevious() {
            return previousIndex() >= 0;
        }

        public Object previous() {
            if (hasPrevious())
                return i.previous();
            else
                throw new NoSuchElementException();
        }

        public int nextIndex() {
            return i.nextIndex() - offset;
        }

        public int previousIndex() {
            return i.previousIndex() - offset;
        }

        public void remove() {
            throw new UnsupportedOperationException();
        }

        public void set(Object e) {
            throw new UnsupportedOperationException();
        }

        public void add(Object e) {
            throw new UnsupportedOperationException();
        }
    }

//    // Support for resetting lock while deserializing
//    private static final Unsafe unsafe =  Unsafe.getUnsafe();
//    private static final long lockOffset;
//    static {
//        try {
//            lockOffset = unsafe.objectFieldOffset
//                (CopyOnWriteArrayList.class.getDeclaredField("lock"));
//            } catch (Exception ex) { throw new Error(ex); }
//    }
//    private void resetLock() {
//        unsafe.putObjectVolatile(this, lockOffset, new ReentrantLock());
//    }
//

    // Temporary emulations of anticipated new j.u.Arrays functions

    private static Object[] copyOfRange(Object[] original, int from, int to,
                                        Class newType) {
        int newLength = to - from;
        if (newLength < 0)
            throw new IllegalArgumentException(from + " > " + to);
        Object[] copy = (Object[]) java.lang.reflect.Array.newInstance
            (newType.getComponentType(), newLength);
        System.arraycopy(original, from, copy, 0,
                         Math.min(original.length - from, newLength));
        return copy;
    }

    private static Object[] copyOf(Object[] original, int newLength,
                                   Class newType) {
        Object[] copy = (Object[]) java.lang.reflect.Array.newInstance
            (newType.getComponentType(), newLength);
        System.arraycopy(original, 0, copy, 0,
                         Math.min(original.length, newLength));
        return copy;
    }

    private static Object[] copyOf(Object[] original, int newLength) {
        return copyOf(original, newLength, original.getClass());
    }
}
