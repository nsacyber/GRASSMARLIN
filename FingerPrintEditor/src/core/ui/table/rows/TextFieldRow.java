package core.ui.table.rows;

import core.ui.table.NamedSmartTableCell;
import core.ui.table.Row;
import core.ui.table.TextFieldSmartTableCell;
import core.ui.table.TextFieldSmartTableCell.FireAction;

import java.awt.*;


/**
 * A row with a label and a TextField for use in the SmartTable architecture
 * 
 * 2004.11.11 - New
 */
public class TextFieldRow implements Row {
    NamedSmartTableCell name;
    TextFieldSmartTableCell value;
    
    public TextFieldRow(String title, String initialValue){
        name = new NamedSmartTableCell(title, FlowLayout.LEFT);
        value = new TextFieldSmartTableCell(initialValue, FlowLayout.LEFT);
    }
    
    public TextFieldRow(String title, String initialValue, FireAction fa){
        name = new NamedSmartTableCell(title, FlowLayout.LEFT);
        value = new TextFieldSmartTableCell(initialValue, FlowLayout.LEFT, fa);
    }
    
    public Object getValueAt(int column){
        if ( column == 0 )
            return name;
        else
            return value;
    }
    public void setValueAt(int column, Object newValue){
        if ( column == 1 )
            value.setValue();
    }
    public void setValue(String newValue){
        value.setValue(newValue);
    }
    public String getValue(){
        return value.getValue();
    }
    public String getTitle() { 
        return this.name.getText();
    }

    public void setEnabled(boolean bv) {
        name.setEnabled(bv);
        value.setEnabled(bv);
    }
    
    public boolean isEnabled() { 
        return true;
   }
    
}
