package prefuse.util.ui;

import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableModel;

import prefuse.data.Table;
import prefuse.data.event.EventConstants;
import prefuse.data.event.TableListener;
import prefuse.util.StringLib;
import prefuse.util.collections.CopyOnWriteArrayList;
import prefuse.util.collections.IntIterator;

/**
 * TableModel implementation that serves as an adapter between a prefuse
 * Table instance and a JTable component.
 */
public class PrefuseTableModel implements TableModel, TableListener {
    
    private CopyOnWriteArrayList m_listeners = new CopyOnWriteArrayList();
    private int[] m_rowmap;
    private Table m_table;
    
    /**
     * Creates a new PrefuseTableModel
     * @param table the underlying prefuse table
     */
    public PrefuseTableModel(Table table) {
    	m_table = table;
    }
    
    /**
     * Initialize mapping between prefuse table rows and the rows reported
     * by this model.
     */
    private void initRowMap() {
        m_rowmap = new int[m_table.getRowCount()];
        IntIterator rows = m_table.rows();
        for ( int i=0; rows.hasNext(); ++i ) {
            m_rowmap[i] = rows.nextInt();
        }
    }
    
    /**
     * Get the prefuse table row for a row index into this table model.
     * @param rowIndex the row index in this table model
     * @return the corresponding prefuse table row
     */
    private int getRow(int rowIndex) {
        if ( m_rowmap == null )
            initRowMap();
        return m_rowmap[rowIndex];
    }
    
    // --------------------------------------------------------------------
    
    /**
     * @see javax.swing.table.TableModel#getColumnCount()
     */
    public int getColumnCount() {
        return m_table.getColumnCount();
    }
    /**
     * @see javax.swing.table.TableModel#getRowCount()
     */
    public int getRowCount() {
        return m_table.getRowCount();
    }
    /**
     * @see javax.swing.table.TableModel#isCellEditable(int, int)
     */
    public boolean isCellEditable(int rowIndex, int columnIndex) {
        return m_table.isCellEditable(rowIndex, columnIndex);
    }
    /**
     * @see javax.swing.table.TableModel#getColumnClass(int)
     */
    public Class getColumnClass(int columnIndex) {
        return m_table.getColumnType(columnIndex);
    }
    /**
     * @see javax.swing.table.TableModel#getValueAt(int, int)
     */
    public Object getValueAt(int rowIndex, int columnIndex) {
        Object o = m_table.get(getRow(rowIndex), columnIndex);
        if ( o != null && o.getClass().isArray() ) {
            return StringLib.getArrayString(o);
        } else {
            return o;
        }
    }
    /**
     * @see javax.swing.table.TableModel#setValueAt(java.lang.Object, int, int)
     */
    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
        m_table.set(getRow(rowIndex), columnIndex, aValue);
    }
    /**
     * @see javax.swing.table.TableModel#getColumnName(int)
     */
    public String getColumnName(int columnIndex) {
        return m_table.getColumnName(columnIndex);
    }
    
    // --------------------------------------------------------------------
    
    /**
     * @see javax.swing.table.TableModel#addTableModelListener(javax.swing.event.TableModelListener)
     */
    public void addTableModelListener(TableModelListener l) {
        m_listeners.add(l);
    }
    /**
     * @see javax.swing.table.TableModel#removeTableModelListener(javax.swing.event.TableModelListener)
     */
    public void removeTableModelListener(TableModelListener l) {
        m_listeners.remove(l);
    }

    /**
     * @see prefuse.data.event.TableListener#tableChanged(prefuse.data.Table, int, int, int, int)
     */
    public void tableChanged(Table t, int start, int end, int col, int type) {
        if ( type == EventConstants.INSERT || type == EventConstants.DELETE )
            m_rowmap = null; // invalidate row map
        
        Object[] lstnrs = m_listeners.getArray();
        if ( lstnrs.length == 0 )
            return;
        
        TableModelEvent evt 
            = new TableModelEvent(this, start, end, col, type);
        for ( int i=0; i<lstnrs.length; ++i ) {
            ((TableModelListener)lstnrs[i]).tableChanged(evt);
        }
    }
    
} // end of class PrefuseTableModel
