/**
 * Copyright (c) 2004-2006 Regents of the University of California.
 * See "license-prefuse.txt" for licensing terms.
 */
package prefuse.util;

import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;

import prefuse.data.Table;
import prefuse.data.Tuple;
import prefuse.data.column.ColumnMetadata;
import prefuse.data.tuple.TupleSet;
import prefuse.util.collections.DefaultLiteralComparator;

/**
 * Functions for processing an iterator of tuples, including the creation
 * of arrays of particular tuple data values and summary
 * statistics (min, max, median, mean, standard deviation).
 * 
 * @author <a href="http://jheer.org">jeffrey heer</a>
 */
public class DataLib {

    /**
     * Get an array containing all data values for a given tuple iteration
     * and field.
     * @param tuples an iterator over tuples
     * @param field the column / data field name
     * @return an array containing the data values
     */
    public static Object[] toArray(Iterator tuples, String field) {
        Object[] array = new Object[100];
        int i=0;
        for ( ; tuples.hasNext(); ++i ) {
            if ( i >= array.length )
                array = ArrayLib.resize(array, 3*array.length/2);
            array[i] = ((Tuple)tuples.next()).get(field);
        }
        return ArrayLib.trim(array, i);
    }

    /**
     * Get an array of doubles containing all column values for a given table
     * and field. The {@link Table#canGetDouble(String)} method must return
     * true for the given column name, otherwise an exception will be thrown.
     * @param tuples an iterator over tuples
     * @param field the column / data field name
     * @return an array of doubles containing the column values
     */
    public static double[] toDoubleArray(Iterator tuples, String field) {
        double[] array = new double[100];
        int i=0;
        for ( ; tuples.hasNext(); ++i ) {
            if ( i >= array.length )
                array = ArrayLib.resize(array, 3*array.length/2);
            array[i] = ((Tuple)tuples.next()).getDouble(field);
        }
        return ArrayLib.trim(array, i);
    }

    // ------------------------------------------------------------------------
    
    /**
     * Get a sorted array containing all column values for a given tuple
     * iterator and field.
     * @param tuples an iterator over tuples
     * @param field the column / data field name
     * @return an array containing the column values sorted
     */
    public static Object[] ordinalArray(Iterator tuples, String field) {
        return DataLib.ordinalArray(tuples, field,
                            DefaultLiteralComparator.getInstance());
    }

    /**
     * Get a sorted array containing all column values for a given table and
     * field.
     * @param tuples an iterator over tuples
     * @param field the column / data field name
     * @param cmp a comparator for sorting the column contents
     * @return an array containing the column values sorted
     */
    public static Object[] ordinalArray(Iterator tuples, String field,
                                        Comparator cmp)
    {
        // get set of all unique values
        HashSet set = new HashSet();
        while ( tuples.hasNext() )
            set.add(((Tuple)tuples.next()).get(field));
        
        // sort the unique values
        Object[] o = set.toArray();
        Arrays.sort(o, cmp);
        return o;
    }
    
    /**
     * Get a sorted array containing all column values for a given tuple
     * iterator and field.
     * @param tuples a TupleSet
     * @param field the column / data field name
     * @return an array containing the column values sorted
     */
    public static Object[] ordinalArray(TupleSet tuples, String field) {
        return ordinalArray(tuples, field,
                            DefaultLiteralComparator.getInstance());
    }

    /**
     * Get a sorted array containing all column values for a given table and
     * field.
     * @param tuples a TupleSet
     * @param field the column / data field name
     * @param cmp a comparator for sorting the column contents
     * @return an array containing the column values sorted
     */
    public static Object[] ordinalArray(TupleSet tuples, String field,
                                        Comparator cmp)
    {
        if ( tuples instanceof Table ) {
            ColumnMetadata md = ((Table)tuples).getMetadata(field);
            return md.getOrdinalArray();
        } else {
            return ordinalArray(tuples.tuples(), field, cmp);
        }
    }

    // ------------------------------------------------------------------------
    
    /**
     * Get map mapping from column values (as Object instances) to their
     * ordinal index in a sorted array.
     * @param tuples an iterator over tuples
     * @param field the column / data field name
     * @return a map mapping column values to their position in a sorted
     * order of values
     */
    public static Map ordinalMap(Iterator tuples, String field) {
        return ordinalMap(tuples, field,
                DefaultLiteralComparator.getInstance());
    }

    /**
     * Get map mapping from column values (as Object instances) to their
     * ordinal index in a sorted array.
     * @param tuples an iterator over tuples
     * @param field the column / data field name
     * @param cmp a comparator for sorting the column contents
     * @return a map mapping column values to their position in a sorted
     * order of values
     */
    public static Map ordinalMap(Iterator tuples, String field, Comparator cmp)
    {
        Object[] o = ordinalArray(tuples, field, cmp);
        
        // map the values to the non-negative numbers
        HashMap map = new HashMap();
        for ( int i=0; i<o.length; ++i )
            map.put(o[i], new Integer(i));
        return map;
    }
    
    /**
     * Get map mapping from column values (as Object instances) to their
     * ordinal index in a sorted array.
     * @param tuples a TupleSet
     * @param field the column / data field name
     * @return a map mapping column values to their position in a sorted
     * order of values
     */
    public static Map ordinalMap(TupleSet tuples, String field) {
        return ordinalMap(tuples, field,
                          DefaultLiteralComparator.getInstance());
    }

    /**
     * Get map mapping from column values (as Object instances) to their
     * ordinal index in a sorted array.
     * @param tuples a TupleSet
     * @param field the column / data field name
     * @param cmp a comparator for sorting the column contents
     * @return a map mapping column values to their position in a sorted
     * order of values
     */
    public static Map ordinalMap(TupleSet tuples, String field, Comparator cmp)
    {
        if ( tuples instanceof Table ) {
            ColumnMetadata md = ((Table)tuples).getMetadata(field);
            return md.getOrdinalMap();
        } else {
            return ordinalMap(tuples.tuples(), field, cmp);
        }
    }

    // ------------------------------------------------------------------------    
    
    /**
     * Get the number of values in a data column. Duplicates will be counted.
     * @param tuples an iterator over tuples
     * @param field the column / data field name
     * @return the number of values
     */
    public static int count(Iterator tuples, String field) {
        int i = 0;
        for ( ; tuples.hasNext(); ++i, tuples.next() );
        return i;
    }

    /**
     * Get the number of distinct values in a data column.
     * @param tuples an iterator over tuples
     * @param field the column / data field name
     * @return the number of distinct values
     */
    public static int uniqueCount(Iterator tuples, String field) {
        HashSet set = new HashSet();
        while ( tuples.hasNext() )
            set.add(((Tuple)tuples.next()).get(field));
        return set.size();
    }

    // ------------------------------------------------------------------------
    
    /**
     * Get the Tuple with the minimum data field value.
     * @param tuples an iterator over tuples
     * @param field the column / data field name
     * @return the Tuple with the minimum data field value
     */
    public static Tuple min(Iterator tuples, String field) {
        return min(tuples, field, DefaultLiteralComparator.getInstance());
    }

    /**
     * Get the Tuple with the minimum data field value.
     * @param tuples an iterator over tuples
     * @param field the column / data field name
     * @param cmp a comparator for sorting the column contents
     * @return the Tuple with the minimum data field value
     */
    public static Tuple min(Iterator tuples, String field, Comparator cmp) {
        Tuple t = null, tmp;
        Object min = null;
        if ( tuples.hasNext() ) {
            t = (Tuple)tuples.next();
            min = t.get(field);
        }
        while ( tuples.hasNext() ) {
            tmp = (Tuple)tuples.next();
            Object obj = tmp.get(field);
            if ( cmp.compare(obj,min) < 0 ) {
                t = tmp;
                min = obj;
            }
        }
        return t;
    }

    /**
     * Get the Tuple with the minimum data field value.
     * @param tuples a TupleSet
     * @param field the column / data field name
     * @return the Tuple with the minimum data field value
     */
    public static Tuple min(TupleSet tuples, String field, Comparator cmp) {
        if ( tuples instanceof Table ) {
            Table table = (Table)tuples;
            ColumnMetadata md = table.getMetadata(field);
            return table.getTuple(md.getMinimumRow());
        } else {
            return min(tuples.tuples(), field, cmp);
        }
    }
    
    /**
     * Get the Tuple with the minimum data field value.
     * @param tuples a TupleSet
     * @param field the column / data field name
     * @return the Tuple with the minimum data field value
     */
    public static Tuple min(TupleSet tuples, String field) {
        return min(tuples, field, DefaultLiteralComparator.getInstance());
    }
    
    // ------------------------------------------------------------------------
    
    /**
     * Get the Tuple with the maximum data field value.
     * @param tuples an iterator over tuples
     * @param field the column / data field name
     * @return the Tuple with the maximum data field value
     */
    public static Tuple max(Iterator tuples, String field) {
        return max(tuples, field, DefaultLiteralComparator.getInstance());
    }

    /**
     * Get the Tuple with the maximum data field value.
     * @param tuples an iterator over tuples
     * @param field the column / data field name
     * @param cmp a comparator for sorting the column contents
     * @return the Tuple with the maximum data field value
     */
    public static Tuple max(Iterator tuples, String field, Comparator cmp) {
        Tuple t = null, tmp;
        Object min = null;
        if ( tuples.hasNext() ) {
            t = (Tuple)tuples.next();
            min = t.get(field);
        }
        while ( tuples.hasNext() ) {
            tmp = (Tuple)tuples.next();
            Object obj = tmp.get(field);
            if ( cmp.compare(obj,min) > 0 ) {
                t = tmp;
                min = obj;
            }
        }
        return t;
    }

    /**
     * Get the Tuple with the maximum data field value.
     * @param tuples a TupleSet
     * @param field the column / data field name
     * @return the Tuple with the maximum data field value
     */
    public static Tuple max(TupleSet tuples, String field, Comparator cmp) {
        if ( tuples instanceof Table ) {
            Table table = (Table)tuples;
            ColumnMetadata md = table.getMetadata(field);
            return table.getTuple(md.getMaximumRow());
        } else {
            return max(tuples.tuples(), field, cmp);
        }
    }
    
    /**
     * Get the Tuple with the maximum data field value.
     * @param tuples a TupleSet
     * @param field the column / data field name
     * @return the Tuple with the maximum data field value
     */
    public static Tuple max(TupleSet tuples, String field) {
        return max(tuples, field, DefaultLiteralComparator.getInstance());
    }
    
    // ------------------------------------------------------------------------
    
    /**
     * Get the Tuple with the median data field value.
     * @param tuples an iterator over tuples
     * @param field the column / data field name
     * @return the Tuple with the median data field value
     */
    public static Tuple median(Iterator tuples, String field) {
        return median(tuples, field, DefaultLiteralComparator.getInstance());
    }

    /**
     * Get the Tuple with the median data field value.
     * @param tuples an iterator over tuples
     * @param field the column / data field name
     * @param cmp a comparator for sorting the column contents
     * @return the Tuple with the median data field value
     */
    public static Tuple median(Iterator tuples, String field, Comparator cmp) {
        Object[] t = new Tuple[100];
        int i=0;
        for ( ; tuples.hasNext(); ++i ) {
            if ( i >= t.length )
                t = ArrayLib.resize(t, 3*t.length/2);
            t[i] = (Tuple)tuples.next();
        }
        ArrayLib.trim(t, i);
        
        Object[] v = new Object[t.length];
        int[] idx = new int[t.length];
        for ( i=0; i<t.length; ++i ) {
            idx[i] = i;
            v[i] = ((Tuple)t[i]).get(field);
        }
    
        ArrayLib.sort(v, idx, cmp);
        return (Tuple)t[idx[idx.length/2]];
    }

    /**
     * Get the Tuple with the median data field value.
     * @param tuples a TupleSet
     * @param field the column / data field name
     * @return the Tuple with the median data field value
     */
    public static Tuple median(TupleSet tuples, String field, Comparator cmp) {
        if ( tuples instanceof Table ) {
            Table table = (Table)tuples;
            ColumnMetadata md = table.getMetadata(field);
            return table.getTuple(md.getMedianRow());
        } else {
            return median(tuples.tuples(), field, cmp);
        }
    }
    
    /**
     * Get the Tuple with the median data field value.
     * @param tuples a TupleSet
     * @param field the column / data field name
     * @return the Tuple with the median data field value
     */
    public static Tuple median(TupleSet tuples, String field) {
        return median(tuples, field, DefaultLiteralComparator.getInstance());
    }
    
    // ------------------------------------------------------------------------
    
    /**
     * Get the mean value of a tuple data value. If any tuple does not have the
     * named field or the field is not a numeric data type, NaN will be returned.
     * @param tuples an iterator over tuples
     * @param field the column / data field name
     * @return the mean value, or NaN if a non-numeric data type is encountered
     */
    public static double mean(Iterator tuples, String field) {
        try {
            int count = 0;
            double sum = 0;
            
            while ( tuples.hasNext() ) {
                sum += ((Tuple)tuples.next()).getDouble(field);
                ++count;
            }
            return sum/count;
        } catch ( Exception e ) {
            return Double.NaN;
        }
    }
    
    /**
     * Get the standard deviation of a tuple data value. If any tuple does not
     * have the named field or the field is not a numeric data type, NaN will be
     * returned.
     * @param tuples an iterator over tuples
     * @param field the column / data field name
     * @return the standard deviation value, or NaN if a non-numeric data type
     * is encountered
     */
    public static double deviation(Iterator tuples, String field) {
        return deviation(tuples, field, DataLib.mean(tuples, field));
    }    
    
    /**
     * Get the standard deviation of a tuple data value. If any tuple does not
     * have the named field or the field is not a numeric data type, NaN will be
     * returned.
     * @param tuples an iterator over tuples
     * @param field the column / data field name
     * @param mean the mean of the column, used to speed up accurate
     * deviation calculation
     * @return the standard deviation value, or NaN if a non-numeric data type
     * is encountered
     */
    public static double deviation(Iterator tuples, String field, double mean) {
        try {
            int count = 0;
            double sumsq = 0;
            double x;
            
            while ( tuples.hasNext() ) {
                x = ((Tuple)tuples.next()).getDouble(field) - mean;
                sumsq += x*x;
                ++count;
            }
            return Math.sqrt(sumsq/count);
        } catch ( Exception e ) {
            return Double.NaN;
        }
    }

    /**
     * Get the sum of a tuple data value. If any tuple does not have the named
     * field or the field is not a numeric data type, NaN will be returned.
     * @param tuples an iterator over tuples
     * @param field the column / data field name
     * @return the sum, or NaN if a non-numeric data type is encountered
     */
    public static double sum(Iterator tuples, String field) {
        try {
            double sum = 0;
            
            while ( tuples.hasNext() ) {
                sum += ((Tuple)tuples.next()).getDouble(field);
            }
            return sum;
        } catch ( Exception e ) {
            return Double.NaN;
        }
    }

    // ------------------------------------------------------------------------
    
    /**
     * Infer the data field type across all tuples in a TupleSet.
     * @param tuples the TupleSet to analyze
     * @param field the data field to type check
     * @return the inferred data type
     * @throws IllegalArgumentException if incompatible types are used
     */
    public static Class inferType(TupleSet tuples, String field) {
        if ( tuples instanceof Table ) {
            return ((Table)tuples).getColumnType(field);
        } else {
            Class type = null, type2 = null;
            Iterator iter = tuples.tuples();
            while ( iter.hasNext() ) {
                Tuple t = (Tuple)iter.next();
                if ( type == null ) {
                    type = t.getColumnType(field);
                } else if ( !type.equals(type2=t.getColumnType(field)) ) {
                    if ( type2.isAssignableFrom(type) ) {
                        type = type2;
                    } else if ( !type.isAssignableFrom(type2) ) {
                        throw new IllegalArgumentException(
                           "The data field ["+field+"] does not have " +
                           "a consistent type across provided Tuples");    
                    }
                }
            }
            return type;
        }
    }
    
} // end of class DataLib
