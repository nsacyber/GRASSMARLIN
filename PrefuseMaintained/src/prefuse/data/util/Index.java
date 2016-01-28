package prefuse.data.util;

import java.util.Comparator;

import prefuse.util.collections.IntIterator;


/**
 * Represents an index over a column of data, allowing quick lookups by
 * data value and providing iterators over sorted ranges of data. For
 * convenience, there are index lookup methods for a variety of data
 * types; which ones to use depend on the data type of the column
 * being indexed and calling a lookup method for an incompatible
 * data type could lead to an exception being thrown.
 * 
 * @author <a href="http://jheer.org">jeffrey heer</a>
 */
public interface Index {

    /** Flag for an ascending sort order. */
    public static final int TYPE_ASCENDING       = 1<<5;
    /** Flag for a descending sort order. */
    public static final int TYPE_DESCENDING      = 1<<4;
    /** Flag for including the lowest value of a range. */
    public static final int TYPE_LEFT_INCLUSIVE  = 1<<3;
    /** Flag for excluding the lowest value of a range. */
    public static final int TYPE_LEFT_EXCLUSIVE  = 1<<2;
    /** Flag for including the highest value of a range. */
    public static final int TYPE_RIGHT_INCLUSIVE = 1<<1;
    /** Flag for excluding the highest value of a range. */
    public static final int TYPE_RIGHT_EXCLUSIVE = 1;
    
    /** Composite flag for an ascending, left and right inclusive range. */
    public static final int TYPE_AII = 
        TYPE_ASCENDING  | TYPE_LEFT_INCLUSIVE | TYPE_RIGHT_INCLUSIVE;
    /** Composite flag for a descending, left and right inclusive range. */
    public static final int TYPE_DII = 
        TYPE_DESCENDING | TYPE_LEFT_INCLUSIVE | TYPE_RIGHT_INCLUSIVE;
    /** Composite flag for an ascending, left exclusive, right inclusive
     * range. */
    public static final int TYPE_AEI = 
        TYPE_ASCENDING  | TYPE_LEFT_EXCLUSIVE | TYPE_RIGHT_INCLUSIVE;
    /** Composite flag for a descending, left exclusive, right inclusive
    * range. */
    public static final int TYPE_DEI = 
        TYPE_DESCENDING | TYPE_LEFT_EXCLUSIVE | TYPE_RIGHT_INCLUSIVE;
    /** Composite flag for an ascending, left inclusive, right exclusive
     * range. */
    public static final int TYPE_AIE = 
        TYPE_ASCENDING  | TYPE_LEFT_INCLUSIVE | TYPE_RIGHT_EXCLUSIVE;
    /** Composite flag for a descending, left inclusive, right exclusive
     * range. */
    public static final int TYPE_DIE = 
        TYPE_DESCENDING | TYPE_LEFT_INCLUSIVE | TYPE_RIGHT_EXCLUSIVE;
    /** Composite flag for an ascending, left and right exclusive range. */
    public static final int TYPE_AEE = 
        TYPE_ASCENDING  | TYPE_LEFT_EXCLUSIVE | TYPE_RIGHT_EXCLUSIVE;
    /** Composite flag for a descending, left and right exclusive range. */
    public static final int TYPE_DEE = 
        TYPE_DESCENDING | TYPE_LEFT_EXCLUSIVE | TYPE_RIGHT_EXCLUSIVE;
    
    /**
     * Perform an initial indexing of a data column.
     */
    public void index();
    
    /**
     * Dispose of an index, deregistering all listeners.
     */
    public void dispose();
    
    /**
     * Get the comparator used to compare column data values.
     * @return the sort comparator
     */
    public Comparator getComparator();
    
    /**
     * Get the row (or one of the rows) with the minimum data value.
     * @return a row with a minimum data value
     */
    public int minimum();
    
    /**
     * Get the row (or one of the rows) with the maximum data value.
     * @return a row with a maximum data value
     */
    public int maximum();
    
    /**
     * Get the row (or one of the rows) with the median data value.
     * @return a row with a median data value
     */
    public int median();
    
    /**
     * Get the number of unique data values in the index.
     * @return the number of unique data values
     */
    public int uniqueCount();
    
    /**
     * Get the size of this index, the number of data value / row
     * pairs included.
     * @return the size of the index
     */
    public int size();
    
    /**
     * Get an iterator over all rows in the index, in sorted order.
     * @param type the sort type, one of {@link #TYPE_ASCENDING} or
     * {@link #TYPE_DESCENDING}.
     * @return an iterator over all rows in the index
     */
    public IntIterator allRows(int type);
    
    /**
     * Get an iterator over a sorted range of rows.
     * @param lo the minimum data value
     * @param hi the maximum data value
     * @param type the iteration type, one of the composite flags
     * involving both a sort order, and whether each bound of
     * the range should inclusive or exclusive
     * @return an iterator over a sorted range of rows
     */
    public IntIterator rows(Object lo, Object hi, int type);
    
    /**
     * Get an iterator over a sorted range of rows.
     * @param lo the minimum data value
     * @param hi the maximum data value
     * @param type the iteration type, one of the composite flags
     * involving both a sort order, and whether each bound of
     * the range should inclusive or exclusive
     * @return an iterator over a sorted range of rows
     */
    public IntIterator rows(int lo, int hi, int type);
    
    /**
     * Get an iterator over a sorted range of rows.
     * @param lo the minimum data value
     * @param hi the maximum data value
     * @param type the iteration type, one of the composite flags
     * involving both a sort order, and whether each bound of
     * the range should inclusive or exclusive
     * @return an iterator over a sorted range of rows
     */
    public IntIterator rows(long lo, long hi, int type);
    
    /**
     * Get an iterator over a sorted range of rows.
     * @param lo the minimum data value
     * @param hi the maximum data value
     * @param type the iteration type, one of the composite flags
     * involving both a sort order, and whether each bound of
     * the range should inclusive or exclusive
     * @return an iterator over a sorted range of rows
     */
    public IntIterator rows(float lo, float hi, int type);
    
    /**
     * Get an iterator over a sorted range of rows.
     * @param lo the minimum data value
     * @param hi the maximum data value
     * @param type the iteration type, one of the composite flags
     * involving both a sort order, and whether each bound of
     * the range should inclusive or exclusive
     * @return an iterator over a sorted range of rows
     */
    public IntIterator rows(double lo, double hi, int type);

    /**
     * Get an iterator over all rows with the given data value.
     * @param val the data value
     * @return an iterator over all rows matching the data value
     */
    public IntIterator rows(Object val);

    /**
     * Get an iterator over all rows with the given data value.
     * @param val the data value
     * @return an iterator over all rows matching the data value
     */
    public IntIterator rows(int val);
    
    /**
     * Get an iterator over all rows with the given data value.
     * @param val the data value
     * @return an iterator over all rows matching the data value
     */
    public IntIterator rows(long val);
    
    /**
     * Get an iterator over all rows with the given data value.
     * @param val the data value
     * @return an iterator over all rows matching the data value
     */
    public IntIterator rows(float val);
    
    /**
     * Get an iterator over all rows with the given data value.
     * @param val the data value
     * @return an iterator over all rows matching the data value
     */
    public IntIterator rows(double val);
    
    /**
     * Get an iterator over all rows with the given data value.
     * @param val the data value
     * @return an iterator over all rows matching the data value
     */
    public IntIterator rows(boolean val);
    
    /**
     * Get the first row found with the given data value.
     * @param x the data value
     * @return the first row matching the data value
     */
    public int get(Object x);
    
    /**
     * Get the first row found with the given data value.
     * @param x the data value
     * @return the first row matching the data value
     */
    public int get(int x);
    
    /**
     * Get the first row found with the given data value.
     * @param x the data value
     * @return the first row matching the data value
     */
    public int get(long x);
    
    /**
     * Get the first row found with the given data value.
     * @param x the data value
     * @return the first row matching the data value
     */
    public int get(float x);
    
    /**
     * Get the first row found with the given data value.
     * @param x the data value
     * @return the first row matching the data value
     */
    public int get(double x);
    
} // end of interface Index
