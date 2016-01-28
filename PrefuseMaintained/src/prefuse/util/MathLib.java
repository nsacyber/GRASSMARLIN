package prefuse.util;

import java.util.Arrays;

import prefuse.Constants;

/**
 * Library of mathematical constants and methods not included in the
 * {@link java.lang.Math} class.
 * 
 * @author <a href="http://jheer.org">jeffrey heer</a>
 */
public class MathLib {

    /** The value 2 * PI */
    public static final double TWO_PI = 2*Math.PI;
    /** The natural logarithm of 10 */
    public static final double LOG10 = Math.log(10);
    /** The natural logarithm of 2 */
    public static final double LOG2 = Math.log(2);
    
    private MathLib() {
        // prevent instantiation
    }
    
    /**
     * The base 2 logarithm of the input value
     * @param x the input value
     * @return the base 2 logarithm
     */
    public static double log2(double x) {
        return Math.log(x)/LOG2;
    }

    /**
     * The base 10 logarithm of the input value
     * @param x the input value
     * @return the base 10 logarithm
     */
    public static double log10(double x) {
        return Math.log(x)/LOG10;
    }
    
    /**
     * The "safe" base 10 logarithm of the input value, handling
     * negative values by simply making them positive and then
     * negating the return value.
     * @param x the input value
     * @return the "negative-safe" base 10 logarithm
     */
    public static double safeLog10(double x) {
        boolean neg = (x < 0.0);
        if ( neg ) { x = -x; }
        if ( x < 10.0 ) { x += (10.0-x) / 10; }
        x = Math.log(x) / LOG10;
        
        return neg ? -x : x;
    }
    
    /**
     * The "safe" square root of the input value, handling
     * negative values by simply making them positive and then
     * negating the return value.
     * @param x the input value
     * @return the "negative-safe" square root
     */
    public static double safeSqrt(double x) {
        return ( x<0 ? -Math.sqrt(-x) : Math.sqrt(x) );
    }
    
    /**
     * Interpolates a value within a range using a specified scale,
     * returning the fractional position of the value within that scale.
     * @param scale The scale on which to perform the interpolation, one of
     * {@link prefuse.Constants#LINEAR_SCALE},
     * {@link prefuse.Constants#LOG_SCALE},
     * {@link prefuse.Constants#SQRT_SCALE}, or
     * {@link prefuse.Constants#QUANTILE_SCALE}.
     * @param val the interpolation value, a fraction between 0 and 1.0.
     * @param dist a double array describing the distribution of the data.
     * For the {@link prefuse.Constants#QUANTILE_SCALE} option, this should
     * be a collection of quantile boundaries, as determined by the
     * {@link #quantiles(int, double[])} method. For any other scale type,
     * the first value of the array must contain the minimum value of the
     * distribution and the last value of the array must contain the
     * maximum value of the distribution; all values in between will be
     * ignored.
     * @return the fractional position of the value within the scale,
     * a double between 0 and 1.
     */
    public static double interp(int scale, double val, double dist[]) {
        switch ( scale ) {
        case Constants.LINEAR_SCALE:
            return linearInterp(val, dist[0], dist[dist.length-1]);
        case Constants.LOG_SCALE:
            return logInterp(val, dist[0], dist[dist.length-1]);
        case Constants.SQRT_SCALE:
            return sqrtInterp(val, dist[0], dist[dist.length-1]);
        case Constants.QUANTILE_SCALE:
            return quantile(val, dist);
        }
        throw new IllegalArgumentException("Unrecognized scale value: "+scale);
    }
    
    /**
     * Interpolates a value between a given minimum and maximum value using
     * a linear scale.
     * @param val the interpolation value, a fraction between 0 and 1.0.
     * @param min the minimum value of the interpolation range
     * @param max the maximum value of the interpolation range
     * @return the resulting interpolated value
     */
    public static double linearInterp(double val, double min, double max) {
        double denominator = (max-min);
        if ( denominator == 0 )
            return 0;
        return (val-min)/denominator;
    }
    
    /**
     * Interpolates a value between a given minimum and maximum value using
     * a base-10 logarithmic scale.
     * @param val the interpolation value, a fraction between 0 and 1.0.
     * @param min the minimum value of the interpolation range
     * @param max the maximum value of the interpolation range
     * @return the resulting interpolated value
     */
    public static double logInterp(double val, double min, double max) {
        double logMin = safeLog10(min);
        double denominator = (safeLog10(max)-logMin);
        if ( denominator == 0 )
            return 0;
        return (safeLog10(val)-logMin) / denominator; 
    }
    
    /**
     * Interpolates a value between a given minimum and maximum value using
     * a square root scale.
     * @param val the interpolation value, a fraction between 0 and 1.0.
     * @param min the minimum value of the interpolation range
     * @param max the maximum value of the interpolation range
     * @return the resulting interpolated value
     */
    public static double sqrtInterp(double val, double min, double max) {
        double sqrtMin = safeSqrt(min);
        double denominator = (safeSqrt(max)-sqrtMin);
        if ( denominator == 0 )
            return 0;
        return (safeSqrt(val)-sqrtMin) / denominator;
    }
    
    /**
     * Compute the n-quantile boundaries for a set of values. The result is
     * an n+1 size array holding the minimum value in the first entry and
     * then n quantile boundaries in the subsequent entries.
     * @param n the number of quantile boundaries. For example, a value of 4
     * will break up the values into quartiles, while a value of 100 will break
     * up the values into percentiles.
     * @param values the array of double values to divide into quantiles
     * @return an n+1 array of doubles containing the minimum value and
     * the quantile boundary values, in that order
     */
    public static double[] quantiles(int n, double[] values) {
        values = (double[])values.clone();
        Arrays.sort(values);
        double[] qtls = new double[n+1];
        for ( int i=0; i<=n; ++i ) {
            qtls[i] = values[((values.length-1)*i)/n];
        }
        return qtls;
    }
    
    /**
     * Get the quantile measure, as a value between 0 and 1, for a given
     * value and set of quantile boundaries. For example, if the input value
     * is the median of the distribution described by the quantile boundaries,
     * this method will return 0.5. As another example, if the quantile
     * boundaries represent percentiles, this value will return the percentile
     * ranking of the input value according to the given boundaries.
     * @param val the value for which to return the quantile ranking
     * @param quantiles an array of quantile boundaries of a distribution
     * @return the quantile ranking, a value between 0 and 1
     * @see #quantiles(int, double[])
     */
    public static double quantile(double val, double[] quantiles) {
        int x1 = 1;
        int x2 = quantiles.length;
        int i = x2 / 2;
        while (x1 < x2) {
            if (quantiles[i] == val) {
                break;
            } else if (quantiles[i] < val) {
                x1 = i + 1;
            } else {
                x2 = i;
            }
            i = x1 + (x2 - x1) / 2;
        }
        return ((double)i)/(quantiles.length-1);
    }
    
} // end of class MathLib
