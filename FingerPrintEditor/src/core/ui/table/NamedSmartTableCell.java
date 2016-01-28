package core.ui.table;

import javax.swing.*;
import java.awt.*;

/**
 * A cell in a SmartTable with a simple label
 * 
 * 2004.10.31 - New
 */
public class NamedSmartTableCell extends SmartTableCell {
    private JLabel nameJL;
    private JPanel panel;
    
    /** Creates a new instance of NamedSmartTableCell */
    public NamedSmartTableCell(String name, int textPosition) {
        nameJL = new JLabel(name);
        panel = new JPanel(new FlowLayout(textPosition));
        panel.add(nameJL);
    }
    
    public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column){
        return panel;
    }    
    
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column){
        return panel;
    }

    public void requestFocus() {
    }
    
    public void setEnabled(boolean bv){
        nameJL.setEnabled(bv);
    }
    
    public String getText(){
        return nameJL.getText();
    }
    
    public void setText(String text){
        nameJL.setText(text);
    }
    
}
