package core.ui.table;

import javax.swing.*;
import java.awt.*;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

/**
 * Text field cell for use in the SmartTable architecture
 * 
 * 2004.10.31 - New
 */
public class TextFieldSmartTableCell extends SmartTableCell {
    private JTextField textField;
    private JLabel textJL;
    private JPanel panel;
    private FireAction fireAction;
    
    /** Creates a new instance of TextFieldSmartTableCell */
    public TextFieldSmartTableCell(String text, int textPosition) {
        textField = new JTextField(text);
        textField.addFocusListener(fl);
        textField.setScrollOffset(2);
        textJL = new JLabel(text);
        panel = new JPanel(new FlowLayout(textPosition));
        panel.add(textJL);
    }
    
    /** Creates a new instance of TextFieldSmartTableCell */
    public TextFieldSmartTableCell(String text, int textPosition, FireAction fa) {
        textField = new JTextField(text);
        textField.addFocusListener(fl);
        textField.setScrollOffset(2);
        this.fireAction = fa;
        textJL = new JLabel(text);
        panel = new JPanel(new FlowLayout(textPosition));
        panel.add(textJL);
    }
    
    /**
     * Will select all text on a JTextField when the focus is gained.
     */
    private FocusListener fl = new FocusListener() {
        public void focusGained(FocusEvent fe) {
            if (fe.getSource() instanceof JTextField)
                ((JTextField)fe.getSource()).selectAll();
        }
        public void focusLost(FocusEvent fe) {            
        }
    };

    
    public java.awt.Component getTableCellEditorComponent(javax.swing.JTable table, Object value, boolean isSelected, int row, int column) {
        return textField;
    }
    
    public java.awt.Component getTableCellRendererComponent(javax.swing.JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        textJL.setText(textField.getText());
        return panel;
    }
    
    public String getValue(){
        return textField.getText();
    }
    
    public void setValue(String text){
        textField.setText(text);
        textJL.setText(text);
    }
    
    public void setValue(){
        textJL.setText(textField.getText());
        if (this.fireAction != null)
            this.fireAction.fireAction(textField.getText());
    }
    
    public JTextField getTextField(){
        return textField;
    }
    
    public void requestFocus() {
        textField.requestFocus();
    }
    
    public void setEnabled(boolean bv){
        textField.setEnabled(bv);
        textJL.setEnabled(bv);
    }
    
    //A simple class that allows an action to be fired as a specified time
    public static class FireAction {
        
        public void fireAction(String actionString) {
        }
    }
    
}
