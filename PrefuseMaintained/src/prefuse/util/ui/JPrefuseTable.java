package prefuse.util.ui;

import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;

import prefuse.data.Table;
import prefuse.visual.VisualTable;

/**
 * Swing component that displays a prefuse Table instance in a Swing
 * JTable component.
 * 
 * @author <a href="http://jheer.org">jeffrey heer</a>
 */
public class JPrefuseTable extends JTable {

    private Table m_table;
    private TableCellRenderer m_tcr = new DefaultTableCellRenderer();
    
    /**
     * Create a new JPrefuseTable.
     * @param t the Table to display.
     */
    public JPrefuseTable(Table t) {
        super();
        m_table = t;
        
        PrefuseTableModel model = new PrefuseTableModel(m_table);
        super.setModel(model);
        m_table.addTableListener(model);
    }
    
    /**
     * Get the table backing this component.
     * @return a prefuse Table instance
     */
    public Table getTable() {
        return m_table;
    }
    
    /**
     * Get the cell renderer to use for drawing table cells.
     * @see javax.swing.JTable#getCellRenderer(int, int)
     */
    public TableCellRenderer getCellRenderer(int r, int c) {
        return m_tcr;
    }
    
    // ------------------------------------------------------------------------
    
    /**
     * Create a new window displaying the contents of the input Table as
     * a Swing JTable.
     * @param t the Table instance to display
     * @return a reference to the JFrame holding the table view
     */
    public static JFrame showTableWindow(Table t) {
        JPrefuseTable table = new JPrefuseTable(t);
        String title = t.toString();
        if ( t instanceof VisualTable ) {
            title = ((VisualTable)t).getGroup() + " " + title;
        }
        JFrame frame = new JFrame(title);
        frame.getContentPane().add(new JScrollPane(table));
        frame.pack();
        frame.setVisible(true);
        return frame;
    }
    
} // end of class JPrefuseTable
