
package core.ui.table;

import javax.swing.*;
import java.awt.*;

/**
 * The basic cell value used in the SmartTable  architecture
 * 
 * 2004.10.30 - New
 */
public abstract class SmartTableCell {
    
    /** Creates a new instance of SmartTableCell */
    public SmartTableCell() {
    }
    
    /**
     * Returns the editor for this component in the row
     */
    public abstract Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column);

    /**
     * Returns the renderer for this component in the row
     */
    public abstract Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column);
    
    /**
     * Request focus for the row
     */
    public abstract void requestFocus();

    /** 
     * Enable or disable this row
     */
    public abstract void setEnabled(boolean bv);
    
}
