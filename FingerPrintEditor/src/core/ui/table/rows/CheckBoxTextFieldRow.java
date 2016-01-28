package core.ui.table.rows;


import core.ui.table.CheckBoxTextFieldSmartTableCell;
import core.ui.table.NamedSmartTableCell;
import core.ui.table.Row;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;


/**
 * Row with a label, a check box, and a text field for use in the SmartTable architecture
 * 
 * 2004.11.11 - New
 */
public class CheckBoxTextFieldRow implements Row {
    private NamedSmartTableCell name;
    private CheckBoxTextFieldSmartTableCell checkBoxesTextFieldCell; 
    public CheckBoxTextFieldRow(String title){
        this(title, null);
    }
    public CheckBoxTextFieldRow(String title, ActionListener listener){
        name = new NamedSmartTableCell(title, FlowLayout.LEFT);
        checkBoxesTextFieldCell = new CheckBoxTextFieldSmartTableCell(FlowLayout.LEFT, listener);         
    }
    public String getName(){
        return name.getText(); 
    }
    public void setSelected(boolean bv){
        checkBoxesTextFieldCell.setSelected(bv);
    }
    public boolean isSelected(){
        return checkBoxesTextFieldCell.isSelected();
    }
    public Object getValueAt(int index){
        if ( index == 0 )
            return name;
        if ( index == 1 )
            return checkBoxesTextFieldCell;
        else 
            return new JLabel("Unknown");
    }
    public void setValueAt(int col, Object value){
        checkBoxesTextFieldCell.setValue(value.toString());
    }
    
    public void setValue(String value){
        if (value != null) {
            checkBoxesTextFieldCell.setSelected(false);
            checkBoxesTextFieldCell.setValue(value.toString());
        }
        //If the value is null, that means use the default, so set the checkbox to selected
        else checkBoxesTextFieldCell.setSelected(true);
    }
    
    public String getValue(){
        //if (!checkBoxesTextFieldCell.isSelected())
            return checkBoxesTextFieldCell.getValue();
        //If the checkbox is selected, we want to use the default value, which means we
        //need to return null
        //return "";
    }
    
    public void setEnabled(boolean bv){
        name.setEnabled(bv);
        checkBoxesTextFieldCell.setEnabled(bv);
    }
    
    public boolean isEnabled() { 
         return true;
    }
}

