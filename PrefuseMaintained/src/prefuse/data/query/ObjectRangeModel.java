package prefuse.data.query;

import java.util.HashMap;
import java.util.Map;

import javax.swing.DefaultBoundedRangeModel;

import prefuse.util.ui.ValuedRangeModel;

/**
 * Supports an ordered range of arbitrary objects. Designed to support
 * range-based dynamic queries over ordered, but not necessarily numerical,
 * data.
 * @author <a href="http://jheer.org">jeffrey heer</a>
 */
public class ObjectRangeModel extends DefaultBoundedRangeModel
    implements ValuedRangeModel
{
    private Object[] m_objects;
    private Map m_ordinal;
    
    /**
     * Create a new ObjectRangeModel with the given objects. The objects are
     * assumed to sorted in ascending order.
     * @param objects the members of this ObjectRangeModel, sorted in ascending
     * order.
     */
    public ObjectRangeModel(Object[] objects) {
        setValueRange(objects);
    }
    
    /**
     * Sets the range model to the given objects. The objects are
     * assumed to sorted in ascending order.
     * @param objects the members of this ObjectRangeModel, sorted in ascending
     * order.
     */
    public void setValueRange(Object[] objects) {
        if ( m_objects != null && objects.length == m_objects.length ) {
            boolean equal = true;
            for ( int i=0; i<objects.length; ++i ) {
                if ( objects[i] != m_objects[i] ) {
                    equal = false; break;
                }
            }
            if ( equal ) return; // early exit, model hasn't changed
        }
        // build a new object array
        m_objects = new Object[objects.length];
        System.arraycopy(objects, 0, m_objects, 0, objects.length);
        
        // build the object->index map
        if ( m_ordinal == null ) {
            m_ordinal = new HashMap();
        } else {
            m_ordinal.clear();
        }
        for ( int i=0; i<objects.length; ++i ) {
            m_ordinal.put(objects[i], new Integer(i));
        }
        setRangeProperties(0, objects.length-1, 0, objects.length-1, false);
    }
    
    /**
     * Return the Object at the given index.
     * @param i the index of the Object
     * @return return the requested Object.
     */
    public Object getObject(int i) {
        return m_objects[i];
    }
    
    
    /**
     * Return the index for a given Object, indicating its order in the range.
     * @param o the Object to lookup.
     * @return the index of the Object in the range model, -1 if the Object is
     * not found in the model.
     */
    public int getIndex(Object o) {
        Integer idx = (Integer)m_ordinal.get(o);
        return (idx==null ? -1 : idx.intValue());
    }
    
    /**
     * @see prefuse.util.ui.ValuedRangeModel#getMinValue()
     */
    public Object getMinValue() {
        return m_objects[getMinimum()];
    }
    
    /**
     * @see prefuse.util.ui.ValuedRangeModel#getMaxValue()
     */
    public Object getMaxValue() {
        return m_objects[getMaximum()];
    }
    
    /**
     * @see prefuse.util.ui.ValuedRangeModel#getLowValue()
     */
    public Object getLowValue() {
        return m_objects[getValue()];
    }
    
    /**
     * @see prefuse.util.ui.ValuedRangeModel#getHighValue()
     */
    public Object getHighValue() {
        return m_objects[getValue()+getExtent()];
    }
    
} // end of class ObjectRangeModel
