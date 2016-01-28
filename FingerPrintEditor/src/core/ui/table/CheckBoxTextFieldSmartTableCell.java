package core.ui.table;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

/**
 * A text field - check box combo cell for the SmartTable architecture
 * 
 * 2004.11.06 - New
 */
public class CheckBoxTextFieldSmartTableCell extends SmartTableCell {
    private JCheckBox box;
    private JCheckBox editBox;
    private JPanel panel;
    private JPanel editPanel;
    private JTextField textField;
    private JLabel textJL;
    String oldText = null;
    
    public CheckBoxTextFieldSmartTableCell(int flowLayoutOrientation){
        this(flowLayoutOrientation, false, null);
    }
    
    public CheckBoxTextFieldSmartTableCell(int flowLayoutOrientation, ActionListener listener) {
        this(flowLayoutOrientation, false, listener);
    }
    
    /** Creates a new instance of CheckBoxesSmartTableCell */
    public CheckBoxTextFieldSmartTableCell(int flowLayoutOrientation, boolean isSelected) {
        this(flowLayoutOrientation, isSelected, null);
    }
    
    /** Creates a new instance of CheckBoxesSmartTableCell */
    public CheckBoxTextFieldSmartTableCell(int flowLayoutOrientation, boolean isSelected, ActionListener listener) {
        //panel = new JPanel( new FlowLayout(flowLayoutOrientation,5,0));
        panel = new JPanel( new BorderLayout());
        //editPanel = new JPanel( new FlowLayout(flowLayoutOrientation,0,0));  //5,0
        //Why does this use a border layout rather than a flow?  A single textfield gets properly placed
        //5 pixels down.  If you have a chechbox and a textfield, both get placed 10 pixels down.  The
        //same goes for CheckBoxesSMartTableCell, but checkboxes are smaller so it doesn't mattter.  That extra
        //amount pushes the bottom of the text field off of the row.
        editPanel = new JPanel( new BorderLayout());  //5,0
        // for ( int k=0; k<options.length; k++){
        textField = new JTextField();
        textField.setColumns(15);
        textField.addFocusListener(fl);
        textField.setScrollOffset(2); 
        textJL = new JLabel();
        //JLabel boxJL = new JLabel("Default -");
        //JLabel editBoxJL = new JLabel("Default -");
        box = new JCheckBox( "Default", isSelected);
        box.setOpaque(false);
        if ( listener != null )
            box.addActionListener(listener);
        ActionListener editBoxAL = new ActionListener() {
            public void actionPerformed(ActionEvent ae) {
                /*if (editBox.isSelected()) {
                    textField.setEnabled(false);
                    textJL.setEnabled(false);
                    if (oldText == null)
                        oldText = textJL.getText();
                    textJL.setText("");
                    textField.setText("");
                    //box.setText("  ");
                    //editBox.setText("  ");
                    
                }
                else {
                    textField.setEnabled(true);
                    textJL.setEnabled(true);
                    textJL.setText(oldText);
                    textField.setText(oldText);
                    oldText = null;
                    //box.setText("or");
                    //editBox.setText("or");
                }*/
                updateSelection();
            }
        };
        editBox = new JCheckBox( "Default", isSelected);
        editBox.setOpaque(false);
        if ( listener != null )
            editBox.addActionListener(listener);
        editBox.addActionListener(editBoxAL);
        //panel.add(boxJL);
        panel.add(box, BorderLayout.WEST);
        //textField = new JTextField("");
        panel.add(textJL, BorderLayout.CENTER);
        //editPanel.add(editBoxJL);
        /*GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridx=0;
        gbc.gridy=0;
        gbc.anchor = GridBagConstraints.WEST;*/
        //gbc.weightx=1;
        editPanel.add(editBox, BorderLayout.WEST);
        //gbc.weightx=5;
        //gbc.fill = GridBagConstraints.HORIZONTAL;
        //gbc.ipadx=15*6;
        //++gbc.gridx;
        editPanel.add(textField, BorderLayout.CENTER);
        //editPanel;
        //pack();
        //checkBoxHash.put(options[k], box);
        // }        
    }

    
    public java.awt.Component getTableCellEditorComponent(javax.swing.JTable table, Object value, boolean isSelected, int row, int column) {
        //return panel;
        return editPanel;
    }
    
    public java.awt.Component getTableCellRendererComponent(javax.swing.JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
        textJL.setText(textField.getText());
        box.setSelected(editBox.isSelected());
        return panel;
    }
    
    private void updateSelection() {
        if (editBox.isSelected()) {
            textField.setEnabled(false);
            textJL.setEnabled(false);
            if (oldText == null)
                oldText = textJL.getText();
            textJL.setText("");
            textField.setText("");
            //box.setText("  ");
            //editBox.setText("  ");
            
        }
        else {
            textField.setEnabled(true);
            textJL.setEnabled(true);
            textJL.setText(oldText);
            textField.setText(oldText);
            oldText = null;
            //box.setText("or");
            //editBox.setText("or");
        }
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
    
    public void requestFocus() {
    }
    
    public void setSelected(boolean bv){
        box.setSelected(bv);
        editBox.setSelected(bv);
        updateSelection();
    }
    
    public boolean isSelected(){        
        return editBox.isSelected(); 
    }
    
    public String getValue() {
        if (!isSelected())
            return textField.getText();
        else return "";
    }
    
    public void setValue(String text) {
        textField.setText(text);
        textJL.setText(text);
    }
    
    public void setValue(){
        textJL.setText(textField.getText());
    }
    
    public JTextField getTextField(){
        return textField;
    }
    
    /*private JCheckBox getCheckBox(String key){
        return (JCheckBox)checkBoxHash.get(key);
    }*/
    
    public void setEnabled(boolean bv) {        
    }
    
}
