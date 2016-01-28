package prefuse.util.ui;

import javax.swing.BoundedRangeModel;

/**
 * BoundedRangeModel that additionally supports a mapping between the integer
 * range used by interface components and a richer range of values, such
 * as numbers or arbitrary objects.
 * 
 * @author <a href="http://jheer.org">jeffrey heer</a>
 * @see javax.swing.BoundedRangeModel
 */
public interface ValuedRangeModel extends BoundedRangeModel {

    /**
     * Get the minimum value backing the range model. This is
     * the absolute minimum value possible for the range span.
     * @return the minimum value
     */
    public Object getMinValue();

    /**
     * Get the maximum value backing the range model. This is
     * the absolute maximum value possible for the range span.
     * @return the maximum value
     */
    public Object getMaxValue();
    
    /**
     * Get the value at the low point of the range span.
     * @return the lowest value of the current range
     */
    public Object getLowValue();

    /**
     * Get the value at the high point of the range span.
     * @return the highest value of the current range
     */
    public Object getHighValue();
    
} // end of interface ValuedRangeModel
