package prefuse.action.assignment;

import java.util.Map;

import prefuse.Constants;
import prefuse.data.tuple.TupleSet;
import prefuse.util.DataLib;
import prefuse.visual.VisualItem;

/**
 * <p>
 * Assignment Action that assigns shape values for a group of items based upon
 * a data field. Shape values are simple integer codes that indicate to
 * appropriate renderer instances what shape should be drawn. The default
 * list of shape values is included in the {@link prefuse.Constants} class,
 * all beginning with the prefix <code>SHAPE</code>. Of course, clients can
 * always create their own shape codes that are handled by a custom Renderer. 
 * </p>
 * 
 * <p>The data field will be assumed to be nominal, and shapes will
 * be assigned to unique values in the order they are encountered. Note that
 * if the number of unique values is greater than
 * {@link prefuse.Constants#SHAPE_COUNT} (when no palette is given) or
 * the length of a specified palette, then duplicate shapes will start
 * being assigned.</p>
 * 
 * <p>This Action only sets the shape field of the VisualItem. For this value
 * to have an effect, a renderer instance that takes this shape value
 * into account must be used (e.g., {@link prefuse.render.ShapeRenderer}).
 * </p>
 * 
 * @author <a href="http://jheer.org">jeffrey heer</a>
 */
public class DataShapeAction extends ShapeAction {
    
    protected static final int NO_SHAPE = Integer.MIN_VALUE;
    
    protected String m_dataField;
    protected int[]  m_palette;
    
    protected Map    m_ordinalMap;
    
    
    /**
     * Create a new DataShapeAction.
     * @param group the data group to process
     * @param field the data field to base shape assignments on
     */
    public DataShapeAction(String group, String field) {
        super(group, NO_SHAPE);
        m_dataField = field;
    }
    
    /**
     * Create a new DataShapeAction.
     * @param group the data group to process
     * @param field the data field to base shape assignments on
     * @param palette a palette of shape values to use for the encoding.
     * By default, shape values are assumed to be one of the integer SHAPE
     * codes included in the {@link prefuse.Constants} class.
     */
    public DataShapeAction(String group, String field, int[] palette) {
        super(group, NO_SHAPE);
        m_dataField = field;
        m_palette = palette;
    }
    
    // ------------------------------------------------------------------------
    
    /**
     * Returns the data field used to encode shape values.
     * @return the data field that is mapped to shape values
     */
    public String getDataField() {
        return m_dataField;
    }
    
    /**
     * Set the data field used to encode shape values.
     * @param field the data field to map to shape values
     */
    public void setDataField(String field) {
        m_dataField = field;
    }
    
    /**
     * This operation is not supported by the DataShapeAction type.
     * Calling this method will result in a thrown exception.
     * @see prefuse.action.assignment.ShapeAction#setDefaultShape(int)
     * @throws UnsupportedOperationException
     */
    public void setDefaultShape(int defaultShape) {
        throw new UnsupportedOperationException();
    }
    
    // ------------------------------------------------------------------------
    
    /**
     * @see prefuse.action.EncoderAction#setup()
     */
    protected void setup() {
        TupleSet ts = m_vis.getGroup(m_group);
        m_ordinalMap = DataLib.ordinalMap(ts, m_dataField);
    }
    
    /**
     * @see prefuse.action.assignment.ShapeAction#getShape(prefuse.visual.VisualItem)
     */
    public int getShape(VisualItem item) {
        // check for any cascaded rules first
        int shape = super.getShape(item);
        if ( shape != NO_SHAPE ) {
            return shape;
        }
        
        // otherwise perform data-driven assignment
        Object v = item.get(m_dataField);
        int idx = ((Integer)m_ordinalMap.get(v)).intValue();

        if ( m_palette == null ) {
            return idx % Constants.SHAPE_COUNT;
        } else {
            return m_palette[idx % m_palette.length];
        }
    }
    
} // end of class DataShapeAction
