package core.ui.table;

import javax.swing.*;
import java.awt.event.ActionListener;

/**
 * A combobox cell for use in the SmartTable architecture
 * 
 * 2004.11.11 - New
 */
public class JComboBoxSmartTableCell extends SmartTableCell {    
    private Object options;
    private JComboBox comboBox;
    
    /** 
     * Creates a new instance of JComboBoxSmartTableCell 
     *
     */
    public JComboBoxSmartTableCell(Object[] options) {
        this(options, null);
    }
    
    public JComboBoxSmartTableCell(Object[] options, ActionListener listener){
        this.options = options;
        comboBox = new JComboBox(options);
        if ( listener != null )
            comboBox.addActionListener(listener);
    }
    
    public java.awt.Component getTableCellEditorComponent(javax.swing.JTable table, Object value, boolean isSelected, int row, int column) {
        return comboBox;
    }
    
    public java.awt.Component getTableCellRendererComponent(javax.swing.JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        return comboBox;
    }
    
    public void requestFocus() {
        comboBox.requestFocus();
    }
    
    public int getSelectedIndex(){
        return comboBox.getSelectedIndex();
    }
    
    public Object getSelectedItem(){
        return comboBox.getSelectedItem();
    }
    
    public void setSelectedItem(Object item){
        comboBox.setSelectedItem(item);
    }
    
    public void setSelectedIndex(int index){
        comboBox.setSelectedIndex(index);
    }
    
    public void setEnabled(boolean bv) {        
        comboBox.setEnabled(bv);
    }
    
    public Object getItem(int index) {
        return comboBox.getItemAt(index);
    }
    
    public int getItemCount() {
        return comboBox.getItemCount();
    }
    
    public void addItem(String objectToAdd) {
        //We want to keep the list in alphabetical order
        int i = 0;
        for (i = 0; i < comboBox.getItemCount(); ++i) {
            if (objectToAdd.compareTo((String)comboBox.getItemAt(i)) < 0) {
                comboBox.insertItemAt(objectToAdd,i);
                comboBox.setSelectedIndex(i);
                return;
            }
        }
        //TODO: Verify this won't overflow
        comboBox.insertItemAt(objectToAdd,i);
        comboBox.setSelectedIndex(i);
    }
    
    public void removeItem(Object objectToRemove) {
        comboBox.removeItem(objectToRemove);
    }
    
}
