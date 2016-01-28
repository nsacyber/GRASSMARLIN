package core.ui.table;

import javax.accessibility.AccessibleAction;
import javax.accessibility.AccessibleContext;
import javax.swing.*;

/**
 * Cell for use in the SmartTable architecture with a CheckBox object
 * 
 * 2004.11.06 - New
 */
public class CheckBoxSmartTableCell extends SmartTableCell {
    private JCheckBox checkBox;
    
    /** Creates a new instance of CheckBoxSmartTableCell */
    public CheckBoxSmartTableCell(AbstractAction action, boolean selected) {
        checkBox = new JCheckBox(action);
        checkBox.setSelected(selected);
    }

    @Override
    public java.awt.Component getTableCellEditorComponent(javax.swing.JTable table, Object value, boolean isSelected, int row, int column) {
        return checkBox;
    }
    
    @Override
    public java.awt.Component getTableCellRendererComponent(javax.swing.JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        return checkBox;
    }
    
    @Override
    public void requestFocus() {
       //checkBox.requestFocus();
    }
    
    /** 
     * Selects the check box in the cell if true
     */
    public void setSelected(boolean bv){
        // we want to fire the internal action to ensure the appropriate actions are 
        // taken.  We must make sure that the current selection is the opposite 
        // of the desired selection because the accessible action will invert the
        // current selection when fired
        checkBox.setSelected(!bv);
        AccessibleContext ac = checkBox.getAccessibleContext();
        ((AccessibleAction)ac.getAccessibleAction()).doAccessibleAction(0);        
    }

    /**
     * Returns true if the check box is selected
     */
    public boolean isSelected(){
        return checkBox.isSelected();
    }

    /**
     * Enables or disables this component
     */
    public void setEnabled(boolean bv) {
        checkBox.setEnabled(bv);
    }
    
}
