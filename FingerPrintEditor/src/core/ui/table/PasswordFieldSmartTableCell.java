package core.ui.table;

import javax.swing.*;
import java.awt.*;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

/**
 * 
 * 2004.10.31 - New
 * 2007.09.04 - Imported into repository from Xware
 */
public class PasswordFieldSmartTableCell extends SmartTableCell {
    private JPasswordField passwordField;
    private JLabel textJL;
    private JPanel panel;
    
    /** 
     * Creates a new instance of PasswordFieldSmartTableCell 
     */
    public PasswordFieldSmartTableCell(String text, int textPosition) {
        this.passwordField = new JPasswordField(text);
        this.passwordField.addFocusListener(this.fl);
        this.passwordField.setScrollOffset(2);
        this.textJL = new JLabel(text);
        this.panel = new JPanel(new FlowLayout(textPosition));
        this.panel.add(this.textJL);
    }
    
    /**
     * Will select all text on a JTextField when the focus is gained.
     */
    private FocusListener fl = new FocusListener() {
        public void focusGained(FocusEvent fe) {
            if (fe.getSource() instanceof JTextField) { 
                ((JTextField)fe.getSource()).selectAll();
            }
        }
        public void focusLost(FocusEvent fe) {            
        }
    };

    
    public java.awt.Component getTableCellEditorComponent(javax.swing.JTable table, Object value, boolean isSelected, int row, int column) {
        return this.passwordField;
    }
    
    public java.awt.Component getTableCellRendererComponent(javax.swing.JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {        
        this.textJL.setText( getMaskedText() );
        return this.panel;
    }
    
    private String getMaskedText(){
        char[] mask = new char[this.passwordField.getPassword().length];
        for (int k=0; k<mask.length; k++) { 
            mask[k] = '*';
        }
        return new String(mask);
    }
    
    public String getValue(){
        return new String(this.passwordField.getPassword());
    }
    
    public void setValue(String text) {
        this.passwordField.setText(text);
        this.textJL.setText(getMaskedText());
    }
    
    public void setValue() {
        this.textJL.setText(getMaskedText());
    }
    
    public void requestFocus() {
        passwordField.requestFocus();
    }
    
    public void setEnabled(boolean bv) {
        this.passwordField.setEnabled(bv);
        this.textJL.setEnabled(bv);
    }
    
}
