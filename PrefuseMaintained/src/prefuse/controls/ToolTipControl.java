package prefuse.controls;

import java.awt.event.MouseEvent;

import prefuse.Display;
import prefuse.visual.VisualItem;


/**
 * Control that enables a tooltip display for items based on mouse hover.
 * 
 * @author <a href="http://jheer.org">jeffrey heer</a>
 */
public class ToolTipControl extends ControlAdapter {

    private String[] label;
    private StringBuffer sbuf;
    
    /**
     * Create a new ToolTipControl.
     * @param field the field name to use for the tooltip text
     */
    public ToolTipControl(String field) {
        this(new String[] {field});
    }

    /**
     * Create a new ToolTipControl.
     * @param fields the field names to use for the tooltip text. The
     * values of each field will be concatenated to form the tooltip.
     */
    public ToolTipControl(String[] fields) {
        label = fields;
        if ( fields.length > 1 )
            sbuf = new StringBuffer();
    }
    
    /**
     * @see prefuse.controls.Control#itemEntered(prefuse.visual.VisualItem, java.awt.event.MouseEvent)
     */
    public void itemEntered(VisualItem item, MouseEvent e) {
        Display d = (Display)e.getSource();
        if ( label.length == 1 ) {
            // optimize the simple case
            if ( item.canGetString(label[0]) ) {
                d.setToolTipText(item.getString(label[0]));
            }
        } else {
            sbuf.delete(0, sbuf.length());
            for ( int i=0; i<label.length; ++i ) {
                if ( item.canGetString(label[i]) ) {
                    if ( sbuf.length() > 0 )
                        sbuf.append("; ");
                    sbuf.append(item.getString(label[i]));
                }
            }
            // show tool tip only, if at least one field is available
            if (sbuf.length() > 0) {
            	d.setToolTipText(sbuf.toString());
            }
        }
    }
    
    /**
     * @see prefuse.controls.Control#itemExited(prefuse.visual.VisualItem, java.awt.event.MouseEvent)
     */
    public void itemExited(VisualItem item, MouseEvent e) {
        Display d = (Display)e.getSource();
        d.setToolTipText(null);
    }
    
} // end of class ToolTipControl
