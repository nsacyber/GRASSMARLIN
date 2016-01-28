package core.ui.table.rows;


import core.ui.table.JComboBoxSmartTableCell;
import core.ui.table.NamedSmartTableCell;
import core.ui.table.Row;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;


/**
 * A row with a label and a combobox for use in the SmartTable architecture
 * 
 * 2004.11.11 - New
 */
public class ComboBoxRow implements Row {
    private NamedSmartTableCell name;
    private JComboBoxSmartTableCell comboBoxCell;
    
    public ComboBoxRow(String title, Object[] items){
        this(title, items, null);
    }
    public ComboBoxRow(String title, Object[] items, ActionListener listener){
        name = new NamedSmartTableCell(title, FlowLayout.LEFT);
        comboBoxCell = new JComboBoxSmartTableCell(items, listener);
    }
    public String getName(){
        return name.getText(); 
    }
    public Object getValueAt(int index){
        if ( index == 0 )
            return name;
        if ( index == 1 )
            return comboBoxCell;
        else 
            return new JLabel("Unknown");
    }
    
    public int getSelectedIndex(){
        return comboBoxCell.getSelectedIndex();
    }
    
    public Object getSelectedItem(){
        return comboBoxCell.getSelectedItem();        
    }    
    
    public void setValueAt(int col, Object value){
    }
    
    public void ifFoundSetSelectedItem(Object item) {
        for (int i = 0; i < comboBoxCell.getItemCount(); ++i) {
            if (item.equals(comboBoxCell.getItem(i))) {
                comboBoxCell.setSelectedIndex(i);
                return;
            }
        }
    }
    
    public void setSelectedItem(Object item){
        comboBoxCell.setSelectedItem(item);
    }
    
    public void setSelectedIndex(int index){
        comboBoxCell.setSelectedIndex(index);
    }
    
    public void addItem(String objectToAdd) {
        comboBoxCell.addItem(objectToAdd);
    }
    
    public void removeItem(Object objectToRemove) {
        comboBoxCell.removeItem(objectToRemove);
    }
    
    public void setEnabled(boolean bv) {
        name.setEnabled(bv);
        comboBoxCell.setEnabled(bv);
    }    
    
    public boolean isEnabled() { 
        return true;
    }
    
}
