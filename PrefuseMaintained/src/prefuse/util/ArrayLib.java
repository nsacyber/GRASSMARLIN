package prefuse.util;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.Comparator;
import java.util.Random;
import java.util.StringTokenizer;

/**
 * Library of supplementary array routines not 
 *  supported by the java.util.Arrays class.
 *
 * @author jeffrey heer
 */
public abstract class ArrayLib {
    
    /**
     * Arrays with lengths beneath this value will be insertion sorted.
     */
    public static final int SORT_THRESHOLD = 30;
    
    // ------------------------------------------------------------------------
    // Shuffle
    
    /**
     * Randomly permute the contents of an array.
     * @param a the array to shuffle
     * @param r the source of randomness to use
     */
    public static final void shuffle(int[] a, Random r) {
        shuffle(a, 0, a.length, r);
    }
    
    /**
     * Randomly permute the contents of a range an array.
     * @param a the array to shuffle
     * @param start the starting index of the range to shuffle
     * @param len then length of the range to shuffle
     * @param r the source of randomness to use
     */
    public static final void shuffle(int[] a, int start, int len, Random r) {
        for ( int i=start+len; --i>0; ) {
            int t = a[i], j = r.nextInt(i);
            a[i] = a[j];
            a[j] = t;
        }
    }
    
    /**
     * Randomly permute the contents of an array.
     * @param a the array to shuffle
     * @param r the source of randomness to use
     */
    public static final void shuffle(long[] a, Random r) {
        shuffle(a, 0, a.length, r);
    }
    
    /**
     * Randomly permute the contents of a range an array.
     * @param a the array to shuffle
     * @param start the starting index of the range to shuffle
     * @param len then length of the range to shuffle
     * @param r the source of randomness to use
     */
    public static final void shuffle(long[] a, int start, int len, Random r) {
        for ( int i=start+len; i>1; --i ) {
            long t = a[i];
            int j = r.nextInt(i);
            a[i] = a[j];
            a[j] = t;
        }
    }
    
    /**
     * Randomly permute the contents of an array.
     * @param a the array to shuffle
     * @param r the source of randomness to use
     */
    public static final void shuffle(float[] a, Random r) {
        shuffle(a, 0, a.length, r);
    }
    
    /**
     * Randomly permute the contents of a range an array.
     * @param a the array to shuffle
     * @param start the starting index of the range to shuffle
     * @param len then length of the range to shuffle
     * @param r the source of randomness to use
     */
    public static final void shuffle(float[] a, int start, int len, Random r) {
        for ( int i=start+len; i>1; --i ) {
            float t = a[i];
            int j = r.nextInt(i);
            a[i] = a[j];
            a[j] = t;
        }
    }
    
    /**
     * Randomly permute the contents of an array.
     * @param a the array to shuffle
     * @param r the source of randomness to use
     */
    public static final void shuffle(double[] a, Random r) {
        shuffle(a, 0, a.length, r);
    }
    
    /**
     * Randomly permute the contents of a range an array.
     * @param a the array to shuffle
     * @param start the starting index of the range to shuffle
     * @param len then length of the range to shuffle
     * @param r the source of randomness to use
     */
    public static final void shuffle(double[] a, int start, int len, Random r) {
        for ( int i=start+len; i>1; --i ) {
            double t = a[i];
            int j = r.nextInt(i);
            a[i] = a[j];
            a[j] = t;
        }
    }
    
    /**
     * Randomly permute the contents of an array.
     * @param a the array to shuffle
     * @param r the source of randomness to use
     */
    public static final void shuffle(Object[] a, Random r) {
        shuffle(a, 0, a.length, r);
    }
    
    /**
     * Randomly permute the contents of a range an array.
     * @param a the array to shuffle
     * @param start the starting index of the range to shuffle
     * @param len then length of the range to shuffle
     * @param r the source of randomness to use
     */
    public static final void shuffle(Object[] a, int start, int len, Random r) {
        for ( int i=start+len; i>1; --i ) {
            Object t = a[i];
            int j = r.nextInt(i);
            a[i] = a[j];
            a[j] = t;
        }
    }
    
    // ------------------------------------------------------------------------
    // Max / Min / Sum
    
    /**
     * Find the maximum value in an array.
     * @param a the array
     * @return the maximum value in the array
     */
    public static final double max(double[] a) {
        double max = Double.NEGATIVE_INFINITY;
        for ( int i=0; i<a.length; ++i ) {
            if ( a[i] > max )
                max = a[i];
        }
        return max;
    }

    /**
     * Find the minimum value in an array.
     * @param a the array
     * @return the minimum value in the array
     */
    public static final double min(double[] a) {
        double min = Double.POSITIVE_INFINITY;
        for ( int i=0; i<a.length; ++i ) {
            if ( a[i] < min )
                min = a[i];
        }
        return min;
    }
    
    /**
     * Compute the sum of the values in an array.
     * @param a the array
     * @return the sum of the values in the array
     */
    public static final double sum(double[] a) {
        double sum = 0;
        for ( int i=0; i<a.length; ++i ) {
            sum += a[i];
        }
        return sum;
    }
    
    //// -----------------------------------------------
    //// -- Searching Functions ------------------------

    /**
     * Perform a binary search over a sorted array for the given key.
     * @param a the array to search
     * @param key the key to search for
     * @return the index of the given key if it exists in the array,
     * otherwise -1 times the index value at the insertion point that
     * would be used if the key were added to the array. 
     */
    public static final int binarySearch(int[] a, int key) {
        int x1 = 0;
        int x2 = a.length;
        int i = x2 / 2;
        while (x1 < x2) {
            if (a[i] == key) {
                return i;
            } else if (a[i] < key) {
                x1 = i + 1;
            } else {
                x2 = i;
            }
            i = x1 + (x2 - x1) / 2;
        }
        return -1*(i+1);
    }

    /**
     * Perform a binary search over a sorted range of an array for the given
     * key. The range is assumed to start at index 0.
     * @param a the array to search
     * @param key the key to search for
     * @param length the the length of the range to search over.
     * @return the index of the given key if it exists in the array,
     * otherwise -1 times the index value at the insertion point that
     * would be used if the key were added to the array. 
     */
    public static final int binarySearch(int[] a, int key, int length) {
        int x1 = 0;
        int x2 = length;
        int i = x2 / 2;

        while (x1 < x2) {
            if (a[i] == key) {
                return i;
            } else if (a[i] < key) {
                x1 = i + 1;
            } else {
                x2 = i;
            }
            i = x1 + (x2 - x1) / 2;
        }
        return -1*(i+1);
    }

    /**
     * Perform a binary search over a sorted range of an array for the given
     * key.
     * @param a the array to search
     * @param key the key to search for
     * @param begin the starting index of the range
     * @param end the ending index of the range, exclusive
     * @return the index of the given key if it exists in the array,
     * otherwise -1 times the index value at the insertion point that
     * would be used if the key were added to the array. 
     */
    public static final int binarySearch(int[] a, int key, int begin, int end) {
        int x1 = begin;
        int x2 = end;
        int i = x1 + (x2 - x1) / 2;

        while (x1 < x2) {
            if (a[i] == key) {
                return i;
            } else if (a[i] < key) {
                x1 = i + 1;
            } else {
                x2 = i;
            }
            i = x1 + (x2 - x1) / 2;
        }

        return -1*(i+1);
    }

    /**
     * Perform a binary search over a sorted array for the given key.
     * @param a the array to search
     * @param key the key to search for
     * @return the index of the given key if it exists in the array,
     * otherwise -1 times the index value at the insertion point that
     * would be used if the key were added to the array. 
     */
    public static final int binarySearch(Object[] a, Object key) {
        int x1 = 0;
        int x2 = a.length;
        int i = x2 / 2, c;
        while (x1 < x2) {
            c = ((Comparable)a[i]).compareTo(key);
            if (c == 0) {
                return i;
            } else if (c < 0) {
                x1 = i + 1;
            } else {
                x2 = i;
            }
            i = x1 + (x2 - x1) / 2;
        }
        return -1*(i+1);
    }

    /**
     * Perform a binary search over a sorted range of an array for the given
     * key. The range is assumed to start at index 0.
     * @param a the array to search
     * @param key the key to search for
     * @param length the the length of the range to search over.
     * @return the index of the given key if it exists in the array,
     * otherwise -1 times the index value at the insertion point that
     * would be used if the key were added to the array. 
     */
    public static final int binarySearch(Object[] a, Object key, int length) {
        int x1 = 0;
        int x2 = length;
        int i = x2 / 2, c;

        while (x1 < x2) {
            c = ((Comparable)a[i]).compareTo(key);
            if (c == 0) {
                return i;
            } else if (c < 0) {
                x1 = i + 1;
            } else {
                x2 = i;
            }
            i = x1 + (x2 - x1) / 2;
        }
        return -1*(i+1);
    }

    /**
     * Perform a binary search over a sorted range of an array for the given
     * key.
     * @param a the array to search
     * @param key the key to search for
     * @param begin the starting index of the range
     * @param end the ending index of the range, exclusive
     * @return the index of the given key if it exists in the array,
     * otherwise -1 times the index value at the insertion point that
     * would be used if the key were added to the array. 
     */
    public static final int binarySearch(Object[] a, Object key, int begin, int end) {
        int x1 = begin;
        int x2 = end;
        int i = x1 + (x2 - x1) / 2, c;

        while (x1 < x2) {
            c = ((Comparable)a[i]).compareTo(key);
            if (c == 0) {
                return i;
            } else if (c < 0) {
                x1 = i + 1;
            } else {
                x2 = i;
            }
            i = x1 + (x2 - x1) / 2;
        }

        return -1*(i+1);
    }

    /**
     * Perform a binary search over a sorted array for the given key.
     * @param a the array to search
     * @param key the key to search for
     * @param cp the comparator to use to compare key values
     * @return the index of the given key if it exists in the array,
     * otherwise -1 times the index value at the insertion point that
     * would be used if the key were added to the array. 
     */
    public static final int binarySearch(Object[] a, Object key, Comparator cp) {
        int x1 = 0;
        int x2 = a.length;
        int i = x2 / 2, c;
        while (x1 < x2) {
            c = cp.compare(a[i], key);
            if (c == 0) {
                return i;
            } else if (c < 0) {
                x1 = i + 1;
            } else {
                x2 = i;
            }
            i = x1 + (x2 - x1) / 2;
        }
        return -1*(i+1);
    }

    /**
     * Perform a binary search over a sorted range of an array for the given
     * key. The range is assumed to start at index 0.
     * @param a the array to search
     * @param key the key to search for
     * @param cp the comparator to use to compare key values
     * @param length the the length of the range to search over.
     * @return the index of the given key if it exists in the array,
     * otherwise -1 times the index value at the insertion point that
     * would be used if the key were added to the array. 
     */
    public static final int binarySearch(Object[] a, Object key, Comparator cp, int length) {
        int x1 = 0;
        int x2 = length;
        int i = x2 / 2, c;

        while (x1 < x2) {
            c = cp.compare(a[i], key);
            if (c == 0) {
                return i;
            } else if (c < 0) {
                x1 = i + 1;
            } else {
                x2 = i;
            }
            i = x1 + (x2 - x1) / 2;
        }
        return -1*(i+1);
    }

    /**
     * Perform a binary search over a sorted range of an array for the given
     * key.
     * @param a the array to search
     * @param key the key to search for
     * @param cp the comparator to use to compare key values
     * @param begin the starting index of the range
     * @param end the ending index of the range, exclusive
     * @return the index of the given key if it exists in the array,
     * otherwise -1 times the index value at the insertion point that
     * would be used if the key were added to the array. 
     */
    public static final int binarySearch(Object[] a, Object key, Comparator cp, int begin, int end) {
        int x1 = begin;
        int x2 = end;
        int i = x1 + (x2 - x1) / 2, c;

        while (x1 < x2) {
            c = cp.compare(a[i], key);
            if (c == 0) {
                return i;
            } else if (c < 0) {
                x1 = i + 1;
            } else {
                x2 = i;
            }
            i = x1 + (x2 - x1) / 2;
        }

        return -1*(i+1);
    }

    //// -----------------------------------------------
    //// -- Finding Functions --------------------------

    /**
     * Linearly search an array for a given key value.
     * @param a the array to search
     * @param key the key to search for
     * @return the index of the first occurrence of the key in the array,
     * of -1 if the key is not found.
     */
    public static final int find(int[] a, int key) {
        for (int i = 0; i < a.length; i++) {
            if (a[i] == key) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Linearly search an array range for a given key value. Assumes that
     * the range begins at index 0.
     * @param a the array to search
     * @param key the key to search for
     * @param length the length of the range to search over
     * @return the index of the first occurrence of the key in the array,
     * of -1 if the key is not found.
     */
    public static final int find(int[] a, int key, int length) {
        for (int i = 0; i < length; i++) {
            if (a[i] == key) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Linearly search an array range for a given key value.
     * @param a the array to search
     * @param key the key to search for
     * @param begin the starting index of the range
     * @param end the ending index of the range, exclusive
     * @return the index of the first occurrence of the key in the array,
     * of -1 if the key is not found.
     */
    public static final int find(int[] a, int key, int begin, int end) {
        for (int i = begin; i < end; i++) {
            if (a[i] == key) {
                return i;
            }
        }
        return -1;
    }

    //// -----------------------------------------------
    //// -- Resizing Functions -------------------------

    /**
     * Resize the given array as needed to meet a target size.
     * @param a the array to potentially resize
     * @param size the minimum size of the target array
     * @return the resized array, if the original array meets the size
     * requirement, it is simply return, otherwise a new array is
     * allocated and the contents of the original array are copied over.
     */
    public static final int[] resize(int[] a, int size) {
        if ( a.length >= size ) return a;
        int[] b = new int[size];
        System.arraycopy(a, 0, b, 0, a.length);
        return b;
    }
    
    /**
     * Resize the given array as needed to meet a target size.
     * @param a the array to potentially resize
     * @param size the minimum size of the target array
     * @return the resized array, if the original array meets the size
     * requirement, it is simply return, otherwise a new array is
     * allocated and the contents of the original array are copied over.
     */
    public static final float[] resize(float[] a, int size) {
        if ( a.length >= size ) return a;
        float[] b = new float[size];
        System.arraycopy(a, 0, b, 0, a.length);
        return b;
    }
    
    /**
     * Resize the given array as needed to meet a target size.
     * @param a the array to potentially resize
     * @param size the minimum size of the target array
     * @return the resized array, if the original array meets the size
     * requirement, it is simply return, otherwise a new array is
     * allocated and the contents of the original array are copied over.
     */
    public static final double[] resize(double[] a, int size) {
        if ( a.length >= size ) return a;
        double[] b = new double[size];
        System.arraycopy(a, 0, b, 0, a.length);
        return b;
    }
    
    /**
     * Resize the given array as needed to meet a target size.
     * @param a the array to potentially resize
     * @param size the minimum size of the target array
     * @return the resized array, if the original array meets the size
     * requirement, it is simply return, otherwise a new array is
     * allocated and the contents of the original array are copied over.
     */
    public static final Object[] resize(Object[] a, int size) {
        if ( a.length >= size ) return a;
        Object[] b = new Object[size];
        System.arraycopy(a, 0, b, 0, a.length);
        return b;
    }

    /**
     * Trims an array to be exactly the target a size.
     * @param a the array to trim
     * @param size the desired size of the array. This value must be lesser
     * than or equal to the size of the input array.
     * @return a trimmed array instance
     */
    public static final int[] trim(int[] a, int size) {
        //assert (size <= a.length);
        if ( a.length == size ) {
            return a;
        } else {
            int[] b = new int[size];
            System.arraycopy(a, 0, b, 0, size);
            return b;
        }
    }

    /**
     * Trims an array to be exactly the target a size.
     * @param a the array to trim
     * @param size the desired size of the array. This value must be lesser
     * than or equal to the size of the input array.
     * @return a trimmed array instance
     */
    public static final float[] trim(float[] a, int size) {
        //assert (size <= a.length);
        if ( a.length == size ) {
            return a;
        } else {
            float[] b = new float[size];
            System.arraycopy(a, 0, b, 0, size);
            return b;
        }
    }
    
    /**
     * Trims an array to be exactly the target a size.
     * @param a the array to trim
     * @param size the desired size of the array. This value must be lesser
     * than or equal to the size of the input array.
     * @return a trimmed array instance
     */
    public static final double[] trim(double[] a, int size) {
        //assert (size <= a.length);
        if ( a.length == size ) {
            return a;
        } else {
            double[] b = new double[size];
            System.arraycopy(a, 0, b, 0, size);
            return b;
        }
    }
    
    /**
     * Trims an array to be exactly the target a size.
     * @param a the array to trim
     * @param size the desired size of the array. This value must be lesser
     * than or equal to the size of the input array.
     * @return a trimmed array instance
     */
    public static final Object[] trim(Object[] a, int size) {
        //assert (size <= a.length);
        if ( a.length == size ) {
            return a;
        } else {
            Object[] b = new Object[size];
            System.arraycopy(a, 0, b, 0, size);
            return b;
        }
    }
    
    //// -----------------------------------------------
    //// -- Sorting Functions --------------------------

    // -- int / double sorting ------------------------------------------

    /**
     * Sort two arrays simultaneously, using the sort order of the values
     * in the first array to determine the sort order for both arrays.
     * @param a the array to sort by
     * @param b the array to re-arrange based on the sort order of the
     * first array.
     */
    public static final void sort(int[] a, double[] b) {
        mergesort(a, b, 0, a.length - 1);
    }

    /**
     * Sort two arrays simultaneously, using the sort order of the values
     * in the first array to determine the sort order for both arrays.
     * @param a the array to sort by
     * @param b the array to re-arrange based on the sort order of the
     * first array.
     * @param length the array range length to sort over
     */
    public static final void sort(int[] a, double[] b, int length) {
        mergesort(a, b, 0, length - 1);
    }

    /**
     * Sort two arrays simultaneously, using the sort order of the values
     * in the first array to determine the sort order for both arrays.
     * @param a the array to sort by
     * @param b the array to re-arrange based on the sort order of the
     * first array.
     * @param begin the start index of the range to sort
     * @param end the end index, exclusive, of the range to sort
     */
    public static final void sort(int[] a, double[] b, int begin, int end) {
        mergesort(a, b, begin, end - 1);
    }

    // -- Insertion Sort --

    protected static final void insertionsort(int[] a, double[] b, int p, int r) {
        for (int j = p + 1; j <= r; ++j) {
            int key = a[j];
            double val = b[j];
            int i = j - 1;
            while (i >= p && a[i] > key) {
                a[i + 1] = a[i];
                b[i + 1] = b[i];
                i--;
            }
            a[i + 1] = key;
            b[i + 1] = val;
        }
    }

    // -- Mergesort --

    protected static final void mergesort(int[] a, double[] b, int p, int r) {
        if (p >= r) {
            return;
        }
        if (r - p + 1 < SORT_THRESHOLD) {
            insertionsort(a, b, p, r);
        } else {
            int q = (p + r) / 2;
            mergesort(a, b, p, q);
            mergesort(a, b, q + 1, r);
            merge(a, b, p, q, r);
        }
    }

    protected static final void merge(int[] a, double[] b, int p, int q, int r) {
        int[] t = new int[r - p + 1];
        double[] v = new double[r - p + 1];
        int i, p1 = p, p2 = q + 1;
        for (i = 0; p1 <= q && p2 <= r; ++i) {
            if (a[p1] < a[p2]) {
                v[i] = b[p1];
                t[i] = a[p1++];
            } else {
                v[i] = b[p2];
                t[i] = a[p2++];
            }
        }
        for (; p1 <= q; ++p1, ++i) {
            v[i] = b[p1];
            t[i] = a[p1];
        }
        for (; p2 <= r; ++p2, ++i) {
            v[i] = b[p2];
            t[i] = a[p2];
        }
        for (i = 0, p1 = p; i < t.length; ++i, ++p1) {
            b[p1] = v[i];
            a[p1] = t[i];
        }
    }

    // -- int / int sorting ---------------------------------------------

    /**
     * Sort two arrays simultaneously, using the sort order of the values
     * in the first array to determine the sort order for both arrays.
     * @param a the array to sort by
     * @param b the array to re-arrange based on the sort order of the
     * first array.
     */
    public static final void sort(int[] a, int[] b) {
        mergesort(a, b, 0, a.length - 1);
    }

    /**
     * Sort two arrays simultaneously, using the sort order of the values
     * in the first array to determine the sort order for both arrays.
     * @param a the array to sort by
     * @param b the array to re-arrange based on the sort order of the
     * first array.
     * @param length the array range length to sort over
     */
    public static final void sort(int[] a, int[] b, int length) {
        mergesort(a, b, 0, length - 1);
    }

    /**
     * Sort two arrays simultaneously, using the sort order of the values
     * in the first array to determine the sort order for both arrays.
     * @param a the array to sort by
     * @param b the array to re-arrange based on the sort order of the
     * first array.
     * @param begin the start index of the range to sort
     * @param end the end index, exclusive, of the range to sort
     */
    public static final void sort(int[] a, int[] b, int begin, int end) {
        mergesort(a, b, begin, end - 1);
    }

    // -- Insertion Sort --

    protected static final void insertionsort(int[] a, int[] b, int p, int r) {
        for (int j = p + 1; j <= r; ++j) {
            int key = a[j];
            int val = b[j];
            int i = j - 1;
            while (i >= p && a[i] > key) {
                a[i + 1] = a[i];
                b[i + 1] = b[i];
                i--;
            }
            a[i + 1] = key;
            b[i + 1] = val;
        }
    }

    // -- Mergesort --

    protected static final void mergesort(int[] a, int[] b, int p, int r) {
        if (p >= r) {
            return;
        }
        if (r - p + 1 < SORT_THRESHOLD) {
            insertionsort(a, b, p, r);
        } else {
            int q = (p + r) / 2;
            mergesort(a, b, p, q);
            mergesort(a, b, q + 1, r);
            merge(a, b, p, q, r);
        }
    }

    protected static final void merge(int[] a, int[] b, int p, int q, int r) {
        int[] t = new int[r - p + 1];
        int[] v = new int[r - p + 1];
        int i, p1 = p, p2 = q + 1;
        for (i = 0; p1 <= q && p2 <= r; ++i) {
            if (a[p1] < a[p2]) {
                v[i] = b[p1];
                t[i] = a[p1++];
            } else {
                v[i] = b[p2];
                t[i] = a[p2++];
            }
        }
        for (; p1 <= q; ++p1, ++i) {
            v[i] = b[p1];
            t[i] = a[p1];
        }
        for (; p2 <= r; ++p2, ++i) {
            v[i] = b[p2];
            t[i] = a[p2];
        }
        for (i = 0, p1 = p; i < t.length; ++i, ++p1) {
            b[p1] = v[i];
            a[p1] = t[i];
        }
    }

    // -- int / Object sorting ---------------------------------------------

    /**
     * Sort two arrays simultaneously, using the sort order of the values
     * in the first array to determine the sort order for both arrays.
     * @param a the array to sort by
     * @param b the array to re-arrange based on the sort order of the
     * first array.
     * @param begin the start index of the range to sort
     * @param end the end index, exclusive, of the range to sort
     */
    public static final void sort(int[] a, Object[] b, int begin, int end) {
        int length = end-begin;
        
        if ( length < SORT_THRESHOLD ) {
            insertionsort(a, b, begin, end-1);
            return;
        }
        
        // allocate source arrays
        int[] ks = new int[length];
        Object[] vs = new Object[length];
        for ( int i=0, idx=begin; i<length; ++i, ++idx ) {
            ks[i] = a[idx];
            vs[i] = b[idx];
        }
        mergesort(ks, a, vs, b, begin, end, -begin);
    }
    
    /**
     * Sort two arrays simultaneously, using the sort order of the values
     * in the first array to determine the sort order for both arrays.
     * @param a the array to sort by
     * @param b the array to re-arrange based on the sort order of the
     * first array.
     * @param abuf a buffer array to perform the sorting without
     * allocating any additional memory
     * @param bbuf a buffer array to perform the sorting without
     * allocating any additional memory
     * @param begin the start index of the range to sort
     * @param end the end index, exclusive, of the range to sort
     */
    public static final void sort(int[] a, Object[] b, 
            int[] abuf, Object[] bbuf, int begin, int end)
    {
        int length = end-begin;
        
        if ( length < SORT_THRESHOLD ) {
            insertionsort(a, b, begin, end-1);
            return;
        }
        
        // allocate source arrays
        for ( int i=0, idx=begin; i<length; ++i, ++idx ) {
            abuf[i] = a[idx];
            bbuf[i] = b[idx];
        }
        mergesort(abuf, a, bbuf, b, begin, end, -begin);
    }

    // -- Insertion Sort --

    protected static final void insertionsort(int[] a, Object[] b, int p, int r) {
        int i, key;
        Object val;
        for (int j = p + 1; j <= r; ++j) {
            key = a[j];
            val = b[j];
            i = j - 1;
            while (i >= p && a[i] > key) {
                a[i + 1] = a[i];
                b[i + 1] = b[i];
                i--;
            }
            a[i + 1] = key;
            b[i + 1] = val;
        }
    }

    // -- Mergesort --
    
    protected static void mergesort(int ks[], int kd[], Object[] vs,
            Object[] vd, int lo, int hi, int off)
    {
        int length = hi-lo;

        if (length < SORT_THRESHOLD) {
            insertionsort(kd, vd, lo, hi-1);
            return;
        }

        // Recursively sort halves of dest into src
        int dlo = lo;
        int dhi = hi;
        lo += off;
        hi += off;
        int mid = (lo + hi) >> 1;
        mergesort(kd, ks, vd, vs, lo, mid, -off);
        mergesort(kd, ks, vd, vs, mid, hi, -off);

        // If list is already sorted, just copy from src to dest. This is an
        // optimization that results in faster sorts for nearly ordered lists.
        if ( ks[mid-1] <= ks[mid] ) {
            System.arraycopy(ks, lo, kd, dlo, length);
            System.arraycopy(vs, lo, vd, dlo, length);
            return;
        }

        // Merge sorted halves (now in src) into dest
        for (int i = dlo, p = lo, q = mid; i < dhi; i++) {
            if (q >= hi || p < mid && ks[p] <= ks[q] ) {
                vd[i] = vs[p];
                kd[i] = ks[p++];
            } else {
                vd[i] = vs[q];
                kd[i] = ks[q++];
            }
        }
    }
    
    protected static final void merge(int[] a, Object[] b, int p, int q, int r) {
        int[] t = new int[r - p + 1];
        Object[] v = new Object[r - p + 1];
        int i, p1 = p, p2 = q + 1;
        for (i = 0; p1 <= q && p2 <= r; ++i) {
            if (a[p1] < a[p2]) {
                v[i] = b[p1];
                t[i] = a[p1++];
            } else {
                v[i] = b[p2];
                t[i] = a[p2++];
            }
        }
        for (; p1 <= q; ++p1, ++i) {
            v[i] = b[p1];
            t[i] = a[p1];
        }
        for (; p2 <= r; ++p2, ++i) {
            v[i] = b[p2];
            t[i] = a[p2];
        }
        for (i = 0, p1 = p; i < t.length; ++i, ++p1) {
            b[p1] = v[i];
            a[p1] = t[i];
        }
    }

    // -- double / int sorting -------------------------------------------

    /**
     * Sort two arrays simultaneously, using the sort order of the values
     * in the first array to determine the sort order for both arrays.
     * @param a the array to sort by
     * @param b the array to re-arrange based on the sort order of the
     * first array.
     */
    public static final void sort(double[] a, int[] b) {
        mergesort(a, b, 0, a.length - 1);
    }

    /**
     * Sort two arrays simultaneously, using the sort order of the values
     * in the first array to determine the sort order for both arrays.
     * @param a the array to sort by
     * @param b the array to re-arrange based on the sort order of the
     * first array.
     * @param length the length of the range to be sorted
     */
    public static final void sort(double[] a, int[] b, int length) {
        mergesort(a, b, 0, length - 1);
    }

    /**
     * Sort two arrays simultaneously, using the sort order of the values
     * in the first array to determine the sort order for both arrays.
     * @param a the array to sort by
     * @param b the array to re-arrange based on the sort order of the
     * first array.
     * @param begin the start index of the range to sort
     * @param end the end index, exclusive, of the range to sort
     */
    public static final void sort(double[] a, int[] b, int begin, int end) {
        mergesort(a, b, begin, end - 1);
    }

    // -- Insertion Sort --

    protected static final void insertionsort(double[] a, int[] b, int p, int r) {
        for (int j = p + 1; j <= r; ++j) {
            double key = a[j];
            int val = b[j];
            int i = j - 1;
            while (i >= p && a[i] > key) {
                a[i + 1] = a[i];
                b[i + 1] = b[i];
                --i;
            }
            a[i + 1] = key;
            b[i + 1] = val;
        }
    }

    // -- Mergesort --

    protected static final void mergesort(double[] a, int[] b, int p, int r) {
        if (p >= r) {
            return;
        }
        if (r - p + 1 < SORT_THRESHOLD) {
            insertionsort(a, b, p, r);
        } else {
            int q = (p + r) / 2;
            mergesort(a, b, p, q);
            mergesort(a, b, q + 1, r);
            merge(a, b, p, q, r);
        }
    }

    protected static final void merge(double[] a, int[] b, int p, int q, int r) {
        double[] t = new double[r - p + 1];
        int[] v = new int[r - p + 1];
        int i, p1 = p, p2 = q + 1;
        for (i = 0; p1 <= q && p2 <= r; ++i) {
            if (a[p1] < a[p2]) {
                v[i] = b[p1];
                t[i] = a[p1++];
            } else {
                v[i] = b[p2];
                t[i] = a[p2++];
            }
        }
        for (; p1 <= q; ++p1, ++i) {
            v[i] = b[p1];
            t[i] = a[p1];
        }
        for (; p2 <= r; ++p2, ++i) {
            v[i] = b[p2];
            t[i] = a[p2];
        }
        for (i = 0, p1 = p; i < t.length; i++, p1++) {
            b[p1] = v[i];
            a[p1] = t[i];
        }
    }    
    
    // -- float / int sorting -------------------------------------------

    /**
     * Sort two arrays simultaneously, using the sort order of the values
     * in the first array to determine the sort order for both arrays.
     * @param a the array to sort by
     * @param b the array to re-arrange based on the sort order of the
     * first array.
     */
    public static final void sort(float[] a, int[] b) {
        mergesort(a, b, 0, a.length - 1);
    }

    /**
     * Sort two arrays simultaneously, using the sort order of the values
     * in the first array to determine the sort order for both arrays.
     * @param a the array to sort by
     * @param b the array to re-arrange based on the sort order of the
     * first array.
     * @param length the length of the range to be sorted
     */
    public static final void sort(float[] a, int[] b, int length) {
        mergesort(a, b, 0, length - 1);
    }

    /**
     * Sort two arrays simultaneously, using the sort order of the values
     * in the first array to determine the sort order for both arrays.
     * @param a the array to sort by
     * @param b the array to re-arrange based on the sort order of the
     * first array.
     * @param begin the start index of the range to sort
     * @param end the end index, exclusive, of the range to sort
     */
    public static final void sort(float[] a, int[] b, int begin, int end) {
        mergesort(a, b, begin, end - 1);
    }

    // -- Insertion Sort --

    protected static final void insertionsort(float[] a, int[] b, int p, int r) {
        for (int j = p + 1; j <= r; ++j) {
            float key = a[j];
            int val = b[j];
            int i = j - 1;
            while (i >= p && a[i] > key) {
                a[i + 1] = a[i];
                b[i + 1] = b[i];
                --i;
            }
            a[i + 1] = key;
            b[i + 1] = val;
        }
    }

    // -- Mergesort --

    protected static final void mergesort(float[] a, int[] b, int p, int r) {
        if (p >= r) {
            return;
        }
        if (r - p + 1 < SORT_THRESHOLD) {
            insertionsort(a, b, p, r);
        } else {
            int q = (p + r) / 2;
            mergesort(a, b, p, q);
            mergesort(a, b, q + 1, r);
            merge(a, b, p, q, r);
        }
    }

    protected static final void merge(float[] a, int[] b, int p, int q, int r) {
        float[] t = new float[r - p + 1];
        int[] v = new int[r - p + 1];
        int i, p1 = p, p2 = q + 1;
        for (i = 0; p1 <= q && p2 <= r; ++i) {
            if (a[p1] < a[p2]) {
                v[i] = b[p1];
                t[i] = a[p1++];
            } else {
                v[i] = b[p2];
                t[i] = a[p2++];
            }
        }
        for (; p1 <= q; ++p1, ++i) {
            v[i] = b[p1];
            t[i] = a[p1];
        }
        for (; p2 <= r; ++p2, ++i) {
            v[i] = b[p2];
            t[i] = a[p2];
        }
        for (i = 0, p1 = p; i < t.length; i++, p1++) {
            b[p1] = v[i];
            a[p1] = t[i];
        }
    }

    // -- Object / int sorting ------------------------------------------------
    
    /**
     * Sort two arrays simultaneously, using the sort order of the values
     * in the first array to determine the sort order for both arrays.
     * @param a the array to sort by
     * @param b the array to re-arrange based on the sort order of the
     * first array.
     * @param cmp the comparator to use to compare key values
     */
    public static final void sort(Object[] a, int[] b, Comparator cmp) {
        mergesort(a, b, 0, a.length - 1, cmp);
    }

    /**
     * Sort two arrays simultaneously, using the sort order of the values
     * in the first array to determine the sort order for both arrays.
     * @param a the array to sort by
     * @param b the array to re-arrange based on the sort order of the
     * first array.
     * @param length the length of the range to be sorted
     * @param cmp the comparator to use to compare key values
     */
    public static final void sort(Object[] a, int[] b, int length, 
            Comparator cmp)
    {
        mergesort(a, b, 0, length - 1, cmp);
    }

    /**
     * Sort two arrays simultaneously, using the sort order of the values
     * in the first array to determine the sort order for both arrays.
     * @param a the array to sort by
     * @param b the array to re-arrange based on the sort order of the
     * first array.
     * @param begin the start index of the range to sort
     * @param end the end index, exclusive, of the range to sort
     * @param cmp the comparator to use to compare key values
     */
    public static final void sort(Object[] a, int[] b, int begin, int end, 
            Comparator cmp)
    {
        mergesort(a, b, begin, end - 1, cmp);
    }

    // -- Insertion Sort --

    protected static final void insertionsort(Object[] a, int[] b, int p, int r,
            Comparator cmp)
    {
        for (int j = p + 1; j <= r; ++j) {
            Object key = a[j];
            int val = b[j];
            int i = j - 1;
            while (i >= p && cmp.compare(a[i],key) > 0 ) {
                a[i + 1] = a[i];
                b[i + 1] = b[i];
                --i;
            }
            a[i + 1] = key;
            b[i + 1] = val;
        }
    }

    // -- Mergesort --

    protected static final void mergesort(Object[] a, int[] b, int p, int r,
            Comparator cmp)
    {
        if (p >= r) {
            return;
        }
        if (r - p + 1 < SORT_THRESHOLD) {
            insertionsort(a, b, p, r, cmp);
        } else {
            int q = (p + r) / 2;
            mergesort(a, b, p, q, cmp);
            mergesort(a, b, q + 1, r, cmp);
            merge(a, b, p, q, r, cmp);
        }
    }

    protected static final void merge(Object[] a, int[] b, int p, int q, int r,
            Comparator cmp)
    {
        Object[] t = new Object[r - p + 1];
        int[] v = new int[r - p + 1];
        int i, p1 = p, p2 = q + 1;
        for (i = 0; p1 <= q && p2 <= r; ++i) {
            if ( cmp.compare(a[p1],a[p2]) < 0 ) {
                v[i] = b[p1];
                t[i] = a[p1++];
            } else {
                v[i] = b[p2];
                t[i] = a[p2++];
            }
        }
        for (; p1 <= q; ++p1, ++i) {
            v[i] = b[p1];
            t[i] = a[p1];
        }
        for (; p2 <= r; ++p2, ++i) {
            v[i] = b[p2];
            t[i] = a[p2];
        }
        for (i = 0, p1 = p; i < t.length; i++, p1++) {
            b[p1] = v[i];
            a[p1] = t[i];
        }
    }
    
    // ------------------------------------------------------------------------
    // Array File I/O
    
    /**
     * Read in a text file as an array of integers. Uses the default java
     * StringTokenizer to segment the text file. Additionally, tokens beginning
     * with the '#' character are ignored.
     * @param filename the name of the file to read in
     * @return an array of integers parsed from the file
     */
    public static int[] getIntArray(String filename) {
        int[] array = null;
        try {
            BufferedReader br = new BufferedReader(new FileReader(filename));
            String line = br.readLine();
            StringTokenizer st = new StringTokenizer(line);
            int maxlen = st.countTokens();
            int len = 0;
            array = new int[maxlen];            
            while ( st.hasMoreTokens() ) {
                String tok = st.nextToken();
                if ( tok.startsWith("#") ) // commented int
                    continue;
                array[len++] = Integer.parseInt(tok);
            }
            if ( len != maxlen )
                array = ArrayLib.trim(array, len);
            return array;
        } catch ( Exception e ) {
            e.printStackTrace();
            return null;
        }
    }

} // end of class ArrayLib
