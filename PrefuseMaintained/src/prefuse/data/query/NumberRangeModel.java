package prefuse.data.query;

import javax.swing.DefaultBoundedRangeModel;

import prefuse.util.TypeLib;
import prefuse.util.ui.ValuedRangeModel;

/**
 * Range model for numerical data. Designed to support range-based dynamic
 * queries.
 * 
 * @author <a href="http://jheer.org">jeffrey heer</a>
 */
public class NumberRangeModel extends DefaultBoundedRangeModel
    implements ValuedRangeModel
{
    protected Class m_type;
    protected Number m_min, m_max, m_lo, m_hi;
    
    // ------------------------------------------------------------------------
    
    /**
     * Create a new NumberRangeModel for the given range.
     * @param lo the low value of the selected range
     * @param hi the high value of the selected range
     * @param min the minimum value allowed for ranges
     * @param max the maximum value allowed for ranges
     */
    public NumberRangeModel(int lo, int hi, int min, int max) {
        this(new Integer(lo), new Integer(hi), 
             new Integer(min),  new Integer(hi));
    }
    
    /**
     * Create a new NumberRangeModel for the given range.
     * @param lo the low value of the selected range
     * @param hi the high value of the selected range
     * @param min the minimum value allowed for ranges
     * @param max the maximum value allowed for ranges
     */
    public NumberRangeModel(long lo, long hi, long min, long max) {
        this(new Long(lo), new Long(hi), new Long(min), new Long(max));
    }
    
    /**
     * Create a new NumberRangeModel for the given range.
     * @param lo the low value of the selected range
     * @param hi the high value of the selected range
     * @param min the minimum value allowed for ranges
     * @param max the maximum value allowed for ranges
     */
    public NumberRangeModel(float lo, float hi, float min, float max) {
        this(new Float(lo), new Float(hi), new Float(min), new Float(max));
    }
    
    /**
     * Create a new NumberRangeModel for the given range.
     * @param lo the low value of the selected range
     * @param hi the high value of the selected range
     * @param min the minimum value allowed for ranges
     * @param max the maximum value allowed for ranges
     */
    public NumberRangeModel(double lo, double hi, double min, double max) {
        this(new Double(lo), new Double(hi), new Double(min), new Double(max));
    }
    
    /**
     * Create a new NumberRangeModel for the given range.
     * @param lo the low value of the selected range
     * @param hi the high value of the selected range
     * @param min the minimum value allowed for ranges
     * @param max the maximum value allowed for ranges
     */
    public NumberRangeModel(Number lo, Number hi, Number min, Number max) {
        m_type = TypeLib.getPrimitiveType(min.getClass());
        setValueRange(lo, hi, min, max);
    }

    // ------------------------------------------------------------------------
    
    /**
     * Update the range settings based on current values.
     */
    protected void updateRange() {
        if ( m_type == int.class )
        {
            setRange(m_lo.intValue(), m_hi.intValue()-m_lo.intValue(),
                    m_min.intValue(), m_max.intValue());
        }
        else if ( m_type == long.class )
        {
        	long range = m_max.longValue() - m_min.longValue();
        	if (range == 0) {
        		setRange(0, 0, 0, 0);
        	} else {
        		long lo = m_lo.longValue() - m_min.longValue();
        		long hi = m_hi.longValue() - m_min.longValue();
        	
        		int v = (int)(10000 *  lo / range );
        		int e = (int)(10000 *  hi / range ) - v;
        		setRange(v, e, 0, 10000);
        	}
        }
        else
        {
        	double range = m_max.doubleValue() - m_min.doubleValue();
        	if (range == 0) {
        		setRange(0, 0, 0, 0);
        	} else {
        		double lo = m_lo.doubleValue() - m_min.doubleValue();
        		double hi = m_hi.doubleValue() - m_min.doubleValue();

        		int v = (int)(10000.0 * lo / range );
        		int e = (int)(10000.0 * hi / range ) - v;
        		setRange(v, e, 0, 10000);
        	}
        }
    }
    
    /**
     * Set the range settings in the pixel-space coordinates.
     */
    protected void setRange(int val, int ext, int min, int max) {
        super.setRangeProperties(val, ext, min, max, false);
    }
    
    /**
     * @see javax.swing.BoundedRangeModel#setRangeProperties(int, int, int, int, boolean)
     */
    public void setRangeProperties(int val, int extent, int min, int max,
                                   boolean adj)
    {
        if ( min!=getMinimum() || max!= getMaximum() ) {
            throw new IllegalArgumentException("Can not change min or max.");
        }
        m_lo = null;
        m_hi = null;
        super.setRangeProperties(val, extent, min, max, adj);
    }
    
    /**
     * Set the range model's backing values.
     * @param lo the low value of the selected range
     * @param hi the high value of the selected range
     * @param min the minimum value allowed for ranges
     * @param max the maximum value allowed for ranges
     */
    public void setValueRange(Number lo, Number hi, Number min, Number max) {
        m_lo = lo; m_hi = hi; m_min = min; m_max = max;
        updateRange();
    }
    
    /**
     * Set the range model's backing values.
     * @param lo the low value of the selected range
     * @param hi the high value of the selected range
     * @param min the minimum value allowed for ranges
     * @param max the maximum value allowed for ranges
     */
    public void setValueRange(double lo, double hi, double min, double max) {
        m_lo = new Double(lo);   m_hi = new Double(hi);
        m_min = new Double(min); m_max = new Double(max);
        updateRange();
    }
    
    /**
     * Set the range model's backing values.
     * @param lo the low value of the selected range
     * @param hi the high value of the selected range
     * @param min the minimum value allowed for ranges
     * @param max the maximum value allowed for ranges
     */
    public void setValueRange(int lo, int hi, int min, int max) {
        m_lo = new Integer(lo);   m_hi = new Integer(hi);
        m_min = new Integer(min); m_max = new Integer(max);
        updateRange();
    }
    
    /**
     * Set the range model's backing values.
     * @param lo the low value of the selected range
     * @param hi the high value of the selected range
     * @param min the minimum value allowed for ranges
     * @param max the maximum value allowed for ranges
     */
    public void setValueRange(long lo, long hi, long min, long max) {
        m_lo = new Long(lo);   m_hi = new Long(hi);
        m_min = new Long(min); m_max = new Long(max);
        updateRange();
    }
    
    /**
     * @see prefuse.util.ui.ValuedRangeModel#getMinValue()
     */
    public Object getMinValue() {
        return m_min;
    }

    /**
     * Set the minimum range value.
     * @param n the minimum range value.
     */
    public void setMinValue(Number n) {
        setValueRange((Number)getLowValue(), (Number)getHighValue(), n, m_max);
    }
    
    /**
     * @see prefuse.util.ui.ValuedRangeModel#getMaxValue()
     */
    public Object getMaxValue() {
        return m_max;
    }

    /**
     * Set the maximum range value.
     * @param n the maximum range value.
     */
    public void setMaxValue(Number n) {
        setValueRange((Number)getLowValue(), (Number)getHighValue(), m_min, n);
    }
    
    /**
     * @see prefuse.util.ui.ValuedRangeModel#getLowValue()
     */
    public Object getLowValue() {
        if ( m_lo == null )
            m_lo = (Number)value(getValue());
        return m_lo;
    }

    /**
     * Set the lowest selected range value.
     * @param n the low value of the selected range.
     */
    public void setLowValue(Number n) {
        setValueRange(n, (Number)getHighValue(), m_min, m_max);
    }
    
    /**
     * @see prefuse.util.ui.ValuedRangeModel#getHighValue()
     */
    public Object getHighValue() {
        if ( m_hi == null )
            m_hi = (Number)value(getValue()+getExtent());
        return m_hi;
    }
    
    /**
     * Set the highest selected range value.
     * @param n the high value of the selected range.
     */
    public void setHighValue(Number n) {
        setValueRange((Number)getLowValue(), n, m_min, m_max);
    }
    
    protected Object value(int val) {
        int min = getMinimum();
        int max = getMaximum();
        if ( m_type == double.class || m_type == float.class ) {
            double f = (val-min)/(double)(max-min);
            double m = m_min.doubleValue();
            double v = m + f*(m_max.doubleValue()-m);
            return (m_type==float.class ? (Number)new Float((float)v) 
                                        : new Double(v));
        } else if ( m_type == long.class ) {
            long m = m_min.longValue();
            long v = m + (val-min)*(m_max.longValue()-m)/(max-min);
            return new Long(v);
        } else {
            return new Integer(val);
        }
    }
    
    /**
     * Not supported, throws an exception.
     * @throws UnsupportedOperationException
     * @see javax.swing.BoundedRangeModel#setMinimum(int)
     */
    public void setMinimum(int min) {
        throw new UnsupportedOperationException();
    }
    
    /**
     * Not supported, throws an exception.
     * @throws UnsupportedOperationException
     * @see javax.swing.BoundedRangeModel#setMaximum(int)
     */
    public void setMaximum(int max) {
        throw new UnsupportedOperationException();
    }
    
    /**
     * @see javax.swing.BoundedRangeModel#setValue(int)
     */
    public void setValue(int val) {
        m_lo = null;
        super.setValue(val);
    }
    
    /**
     * @see javax.swing.BoundedRangeModel#setExtent(int)
     */
    public void setExtent(int extent) {
        m_hi = null;
        super.setExtent(extent);
    }
    
} // end of class NumberRangeModel
