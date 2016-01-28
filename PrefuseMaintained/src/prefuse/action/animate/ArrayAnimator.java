package prefuse.action.animate;

import java.util.logging.Logger;

import prefuse.action.ItemAction;
import prefuse.util.PrefuseLib;
import prefuse.visual.VisualItem;


/**
 * Animator that inerpolates an array of numerical values.
 * 
 * @author <a href="http://jheer.org">jeffrey heer</a>
 */
public class ArrayAnimator extends ItemAction {

    private static final Logger s_logger
        = Logger.getLogger(ArrayAnimator.class.getName());
    
    private String m_field; // the field
    private String m_start; // the start field
    private String m_end;   // the end field
    
    /**
     * Create a new ArrayAnimator that processes the given data group
     * and interpolates arrays in the given data field.
     * @param group the data group to process
     * @param field the data field to interpolate. This should be an
     * interpolated field (have start and end instances as well as
     * the field name itself).
     */
    public ArrayAnimator(String group, String field) {
        super(group);
        m_field = field;
        m_start = PrefuseLib.getStartField(field);
        m_end = PrefuseLib.getEndField(field);
    }
    
    /**
     * @see prefuse.action.ItemAction#process(prefuse.visual.VisualItem, double)
     */
    public void process(VisualItem item, double frac) {
        Object o = item.get(m_field);
        if ( o instanceof float[] ) {
            float[] a = (float[])o;
            float[] s = (float[])item.get(m_start);
            float[] e = (float[])item.get(m_end);
            
            float f = (float)frac;
            for ( int i=0; i<a.length; ++i ) {
                if ( Float.isNaN(a[i]) ) break;
                a[i] = s[i] + f*(e[i]-s[i]);
            }
            item.setValidated(false);
        } else if ( o instanceof double[] ) {
            double[] a = (double[])o;
            double[] s = (double[])item.get(m_start);
            double[] e = (double[])item.get(m_end);
            
            for ( int i=0; i<a.length; ++i ) {
                if ( Double.isNaN(a[i]) ) break;
                a[i] = s[i] + frac*(e[i]-s[i]);
            }
            item.setValidated(false);
        } else {
            s_logger.warning("Encountered non-double/non-float array type: "
                    + (o==null ? "null" : o.getClass().getName()));
        }
    }

} // end of class ArrayAnimator
