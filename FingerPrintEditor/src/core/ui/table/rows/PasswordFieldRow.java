package core.ui.table.rows;

import core.ui.table.NamedSmartTableCell;
import core.ui.table.PasswordFieldSmartTableCell;
import core.ui.table.Row;

import java.awt.*;


/**
 * 2004.11.11 - New
 * 2007.05.11 - Added ability to set the tool tip for the name cell in this row
 * 2007.09.04 - Imported into repository from Xware
 */
public class PasswordFieldRow implements Row {
    NamedSmartTableCell name;
    PasswordFieldSmartTableCell value; 
    
    public PasswordFieldRow(String title, String initialValue){
        this.name = new NamedSmartTableCell(title, FlowLayout.LEFT);
        this.value = new PasswordFieldSmartTableCell(initialValue, FlowLayout.LEFT);
    }
    
//    /**
//     * Sets the tool tip for the label on the title cell for this Row
//     */
//    public void setToolTipText(String text) { 
//        this.name.setToolTipText(text);
//    }
    
    public Object getValueAt(int column){
        if (column == 0) { 
            return this.name;
        }
        else { 
            return this.value;
        }
    }
    public void setValueAt(int column, Object newValue) {
        if (column == 1) { 
            value.setValue();
        }
    }
    public void setValue(String newValue) {
        value.setValue(newValue);
    }
    public String getValue() {
        return value.getValue();
    }                    

    public void setEnabled(boolean bv) {
        this.name.setEnabled(bv);
        this.value.setEnabled(bv);
    }    
    
    public boolean isEnabled() { 
        return true;
    }
}
