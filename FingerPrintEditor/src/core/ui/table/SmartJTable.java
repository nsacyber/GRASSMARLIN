package core.ui.table;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableModel;
import java.awt.*;


/**
 * A table that delegates the rendering and editing of its cells
 * to the cells themselves - or to the rows.  This design is contrary
 * to purpose of the existing Java design and is only appropriate for
 * trivial tables - such as those used in configuration Wizards.
 * 
 * This model would perform very poorly in a large table structure 
 * that would be better suited to have a single or limited renderers
 * and editors.  Use with caution. 
 *
 * 
 * 2004.10.30 - New
 * 2007.04.10 - Reformatted 
 * 2007.09.06 - Added handling for null values when rendering
 */
public class SmartJTable extends JTable {
    public static final long serialVersionUID = 1001;
    
    /** Creates a new instance of SmartJTable */
    public SmartJTable(TableModel model) {
        super(model);
        super.setDefaultEditor(Object.class, cellEditor);
        super.setDefaultRenderer(Object.class, cellRenderer); 
        super.setSelectionForeground(Color.red);
        super.setIntercellSpacing(new Dimension(5,5));
        super.setRowHeight(25);
    }
    
    class MyCellEditor extends AbstractCellEditor implements TableCellEditor {
        public static final long serialVersionUID = 10001;
        SmartTableCell cell = null;
        //TODO: Fix this hack
        //Yes this is a hack.  I didn't want to risk upsetting anything previously
        //coded so I added a hack only for what I specifically needed.
        public Object getCellEditorValue() {
            if (cell != null) {
                if (cell instanceof CheckBoxTextFieldSmartTableCell) {
                    return ((CheckBoxTextFieldSmartTableCell)cell).getValue();
                }
            }
            return "edited";
        }
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row, int column){
            if (! (value instanceof SmartTableCell )) { 
                return new JLabel(value.getClass().toString());
            }
            this.cell = (SmartTableCell)value;
            return this.cell.getTableCellEditorComponent(table, value, isSelected, row, column); 
        }
    }
        
    private TableCellEditor cellEditor = new MyCellEditor();    
    
    
    class MyTableCellRenderer extends DefaultTableCellRenderer {
        public static final long serialVersionUID = 10001;
        public MyTableCellRenderer(){
        }
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column){
            if (! (value instanceof SmartTableCell )) { 
                if (value == null) { 
                    return new JLabel("Null");
                } else { 
                    return new JLabel(value.getClass().toString());
                }
            }
            SmartTableCell cell = (SmartTableCell)value;
            Component c = cell.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            if ( hasFocus ){
                table.editCellAt(row,column);
                cell.requestFocus(); 
            }                
            if ( isSelected ){
                c.setForeground(table.getSelectionForeground());
                c.setBackground(table.getSelectionBackground());
            }
            else{
                c.setForeground(table.getForeground());
                c.setBackground(table.getBackground());
            }        
            return c;
        }
    }
    
    private DefaultTableCellRenderer cellRenderer = new MyTableCellRenderer();
    
}
