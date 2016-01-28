package prefuse.action.animate;

import prefuse.action.ItemAction;
import prefuse.util.ColorLib;
import prefuse.util.PrefuseLib;
import prefuse.util.collections.CopyOnWriteArrayList;
import prefuse.visual.VisualItem;


/**
 * Animator that linearly interpolates between starting and ending colors
 * for VisualItems during an animation. By default, interpolates the three
 * primary color fields: {@link VisualItem#STROKECOLOR stroke color},
 * {@link VisualItem#FILLCOLOR fill color}, and
 * {@link VisualItem#TEXTCOLOR text color}.
 * 
 * @author <a href="http://jheer.org">jeffrey heer</a>
 */
public class ColorAnimator extends ItemAction {

    private static final String[] DEFAULTS = new String[] { 
        VisualItem.STROKECOLOR, VisualItem.FILLCOLOR, 
        VisualItem.TEXTCOLOR };
    
    private CopyOnWriteArrayList m_colorFields;
    
    /**
     * Create a new ColorAnimator that processes all data groups.
     */
    public ColorAnimator() {
        super();
        setColorFields(DEFAULTS);
    }

    /**
     * Create a new ColorAnimator that processes the specified group.
     * @param group the data group to process
     */
    public ColorAnimator(String group) {
        super(group);
        setColorFields(DEFAULTS);
    }

    /**
     * Create a new ColorAnimator that processes the specified group and
     * color field.
     * @param group the data group to process
     * @param field the color field to interpolate
     */
    public ColorAnimator(String group, String field) {
        super(group);
        setColorFields(new String[] {field});
    }
    
    /**
     * Create a new ColorAnimator that processes the specified group and
     * color fields.
     * @param group the data group to process
     * @param fields the color fields to interpolate
     */
    public ColorAnimator(String group, String[] fields) {
        super(group);
        setColorFields(fields);
    }
    
    /**
     * Sets the color fields to interpolate.
     * @param fields the color fields to interpolate
     */
    public void setColorFields(String[] fields) {
        if ( fields == null ) {
            throw new IllegalArgumentException();
        }
        
        if ( m_colorFields == null )
            m_colorFields = new CopyOnWriteArrayList();
        else
            m_colorFields.clear();
        
        for ( int i=0; i<fields.length; ++i ) {
            m_colorFields.add(fields[i]);
            m_colorFields.add(PrefuseLib.getStartField(fields[i]));
            m_colorFields.add(PrefuseLib.getEndField(fields[i]));
        }
    }
    
    /**
     * @see prefuse.action.ItemAction#process(prefuse.visual.VisualItem, double)
     */
    public void process(VisualItem item, double frac) {
        if ( m_colorFields == null ) return;
        
        Object[] fields = m_colorFields.getArray();
        for ( int i=0; i<fields.length; i += 3 ) {
            String f  = (String)fields[i];
            String sf = (String)fields[i+1];
            String ef = (String)fields[i+2];
            
            int sc = item.getInt(sf), ec = item.getInt(ef);
            int cc = ColorLib.interp(sc, ec, frac);
            item.setInt(f, cc);
        }
    }

} // end of class ColorAnimator
