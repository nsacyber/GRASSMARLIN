package core.ui.table;

import javax.swing.*;
import javax.swing.border.Border;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.ArrayList;

/**
 *
 * Provides a simple table that supports adding and deleting rows
 *
 * 2004.11.11 - New
 * 2007.04.23 - Added Cursor change function to mouse listener; corrected bug in removeAllRows method; integrated ObjectFactory
 */
public class SimpleAddDeleteTable<T extends AddDeleteRow> extends JPanel {
    public static final long serialVersionUID = 10001;
    private ArrayList<T> rows = new ArrayList<T>();    
    private SmartJTable table = null;
    private JScrollPane jsp = null;
    private JLabel titleLabel;
    private JLabel addLabel;
    private JLabel deleteLabel;
    private String addText = "<html><nobr><font color=blue><u>Add</u></font></html>";
    private String deleteText = "<html><nobr><font color=blue><u>Delete</u></font></html>";
    private String inactiveText = "<html></html>";
    private JPopupMenu popupMenu = null;
    private JPopupMenu addOnlyPopupMenu = null;
    private ObjectFactory<T> objectFactory;
    private String tableTitle;
    private Dimension tablePreferredSize;
    private String[] tableColumnNames;
    
    private boolean enabled = true;
    
    /**
     * Creates a new instance of SimpleAddDeleteTable 
     *
     * @param rowType The type of Row used in this table
     * @param title The tile of the table
     * @param columnNames The names of the columns - also used to set number of columns in TableModel
     */
    public SimpleAddDeleteTable(ObjectFactory<T> objectFactory, String title, Dimension preferredSize, String[] columnNames) {
        this.objectFactory = objectFactory;
        this.tableTitle = title;
        this.tablePreferredSize = preferredSize;
        this.tableColumnNames = columnNames;
        this.buildInterface();
    }        
    /**
     * Return the table for this component
     */
	public SmartJTable getTable(){
		return table;
	}

	/**
     * Builds the interface  
	 */
    public void buildInterface(){        
        this.initPopups();
        TableModel model = new AbstractTableModel() {
            public static final long serialVersionUID = 10001;
            public int getColumnCount(){
                return tableColumnNames.length;
            }
            public int getRowCount(){
                return rows.size();
            }
            public Object getValueAt(int row, int col){
                return ((Row)rows.get(row)).getValueAt(col);
            }
            public String getColumnName(int column){
                return tableColumnNames[column];
            }
            public void setValueAt(Object value, int row, int col) {
                ((Row)rows.get(row)).setValueAt(col, value);
                fireTableCellUpdated(row, col);
            }
            public boolean isCellEditable(int row, int col) {
                return true;
            }
        };
        this.table = new SmartJTable(model);
        this.table.addMouseListener(this.mouseAdapter);
        this.table.getTableHeader().addMouseListener(this.addOnlyMouseAdapter);
        this.jsp = new JScrollPane(this.table);        
        Border outside = BorderFactory.createEmptyBorder(15,15,15,15);
        this.jsp.setPreferredSize(this.tablePreferredSize);
        setLayout(new BorderLayout());
        JPanel titlePanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        this.titleLabel = new JLabel(this.tableTitle);
        titlePanel.add(this.titleLabel);
        this.addLabel = new JLabel(this.addText);        
        this.addLabel.addMouseListener(this.addMouseAdapter);
        this.deleteLabel = new JLabel(this.deleteText);
        this.deleteLabel.addMouseListener(this.deleteMouseAdapter);
        titlePanel.add(this.addLabel);
        titlePanel.add(this.deleteLabel);
        super.add(titlePanel, BorderLayout.NORTH);
        super.add(this.jsp, BorderLayout.CENTER);
        super.setBorder(outside);
    }


    /** 
     * Initializes the Popup Menus
     */
    private void initPopups(){ 
        this.popupMenu = new JPopupMenu();
        this.addOnlyPopupMenu = new JPopupMenu();
        AbstractAction addAction = new AbstractAction("Add"){
            public void actionPerformed(ActionEvent ae){
                SimpleAddDeleteTable.this.addRow();
            }
        };
        AbstractAction deleteAction = new AbstractAction("Delete"){
            public void actionPerformed(ActionEvent ae){
                SimpleAddDeleteTable.this.deleteCurrentRow();
            }
        };
        this.popupMenu.add(addAction);
        this.popupMenu.add(deleteAction);
        this.addOnlyPopupMenu.add(addAction);
    }
    
    /**
     * Adds a new row to the table
     */
    public void addRow(){
        this.addRow(this.objectFactory.createNewObject(""));
    }
    
    /**
     * Adds the row to the table
     * @param row
     */
    public void addRow(T row){
        row.addMouseListener(mouseAdapter);
        this.rows.add(row);        
        if (table != null) {
            ((AbstractTableModel)table.getModel()).fireTableRowsInserted(rows.size()-1, rows.size()-1);
        }
    }        
    
    /**
     * fires TableDataChanged on the TableModel
     */
    public void refresh(){
        ((AbstractTableModel)table.getModel()).fireTableDataChanged();
    }
    
    /**
     * Returns the Rows in the table
     * @return
     */
    public ArrayList<T> getRows(){
        return rows;
    }    

    /**
     * Returns the row for index
     * @param index
     * @return
     */
    public T getRowAtIndex(int index) { 
        return this.rows.get(index);
    }
    
    /**
     * Removes all rows from the table
     */
    public void deleteAllRows() {
        while (rows.size() > 0) { 
            rows.remove(0);
        }
        ((AbstractTableModel)table.getModel()).fireTableStructureChanged();
    }

    /**
     * Deletes the currently selected Row from the table
     */
    public void deleteCurrentRow(){
        if (table.getSelectedRow() != -1) {
            rows.remove( table.getSelectedRow() );
            ((AbstractTableModel)table.getModel()).fireTableStructureChanged();
        }
    }
    
    /**
     * Popup MouseAdapter - add and delete
     */
    private MouseAdapter mouseAdapter = new MouseAdapter(){
        public void mousePressed(MouseEvent me){ 
            maybePopup(me); 
        }
        public void mouseReleased(MouseEvent me){ 
            maybePopup(me); 
        }
        private void maybePopup(MouseEvent me){
            if ( !enabled ) 
                return;
            if ( me.isPopupTrigger() ){
                table.changeSelection(table.rowAtPoint(me.getPoint()), table.columnAtPoint(me.getPoint()),false,false);
                popupMenu.show(me.getComponent(), me.getX(), me.getY());
            }
        }
     };
     
     /**
      * Popup MouseAdapter - add only
      */
     private MouseAdapter addOnlyMouseAdapter = new MouseAdapter(){
        public void mousePressed(MouseEvent me){ 
            maybePopup(me); 
        }
        public void mouseReleased(MouseEvent me){ 
            maybePopup(me); 
        }
        private void maybePopup(MouseEvent me){
            if ( !enabled ) 
                return;
            if ( me.isPopupTrigger() ){
                addOnlyPopupMenu.show(me.getComponent(), me.getX(), me.getY());
            }
        }
     };
     
     /**
      * Listens to the add label
      */
     private MouseAdapter addMouseAdapter = new MouseAdapter(){
         public void mouseReleased(MouseEvent me){ 
            if ( !enabled ) 
                return;
            addRow();
        }
        @Override
        public void mouseEntered(MouseEvent e) {
            SimpleAddDeleteTable.this.addLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }
        @Override
        public void mouseExited(MouseEvent e) {
            SimpleAddDeleteTable.this.addLabel.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        }
     };

     
     /** 
      * Listens to the delete label
      */
     private MouseAdapter deleteMouseAdapter = new MouseAdapter(){
         public void mouseReleased(MouseEvent me){ 
            if ( !enabled ) {  
                return;
            }
            SimpleAddDeleteTable.this.deleteCurrentRow();
        }
        @Override
        public void mouseEntered(MouseEvent e) {
            SimpleAddDeleteTable.this.deleteLabel.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        }
        @Override
        public void mouseExited(MouseEvent e) {
            SimpleAddDeleteTable.this.deleteLabel.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
        }
     };
     

     /**
      * @inheritDoc
      */
     public void setEnabled(boolean bv){
        enabled = bv;
        for ( int k=0; k<rows.size(); k++){
            ((Row)rows.get(k)).setEnabled(bv);
        }
        addLabel.setEnabled(bv);
        addLabel.setText(bv?addText:inactiveText);
        deleteLabel.setEnabled(bv);
        deleteLabel.setText(bv?deleteText:inactiveText);
        titleLabel.setEnabled(bv);        
        refresh();
     }
     
}
