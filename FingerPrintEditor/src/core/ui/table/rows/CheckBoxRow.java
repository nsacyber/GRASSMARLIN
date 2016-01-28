package core.ui.table.rows;


import core.ui.table.CheckBoxSmartTableCell;
import core.ui.table.NamedSmartTableCell;
import core.ui.table.Row;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;


/**
 * A row with a label and a checkbox for use in the SmartTable architecture
 * 
 * 2004.11.11 - New
 */
public class CheckBoxRow implements Row {
    private NamedSmartTableCell name;    private CheckBoxSmartTableCell checkBoxCell; 
    public CheckBoxRow(String booleanName){
        this(booleanName, emptyAction, false);
    }
    public CheckBoxRow(String booleanName, AbstractAction action, boolean selected){
        name = new NamedSmartTableCell(booleanName, FlowLayout.LEFT);
        checkBoxCell = new CheckBoxSmartTableCell(action, selected);
    }
    public String getName(){
        return name.getText(); 
    }
    public void setSelected(boolean bv){
        checkBoxCell.setSelected(bv);
    }
    public boolean isSelected(){
        return checkBoxCell.isSelected();
    }
    public Object getValueAt(int index){
        if ( index == 0 )
            return name;
        if ( index == 1 )
            return checkBoxCell;
        else 
            return new JLabel("Unknown");
    }
    public void setValueAt(int col, Object value){
    }
    
    public void setEnabled(boolean bv) {
        name.setEnabled(bv);
        checkBoxCell.setEnabled(bv);
    }
    
    public boolean isEnabled() { 
        return true;
    }
    
    private static AbstractAction emptyAction = new AbstractAction(""){
        public void actionPerformed(ActionEvent ae){
        }
    };
    
}
