package prefuse.data.util;

import prefuse.data.CascadedTable;
import prefuse.data.Table;

/**
 * RowManager instance that additionally takes into account tables which
 * inherit from a parent table but can also have their own, dedicated
 * columns.
 * 
 * @author <a href="http://jheer.org">jeffrey heer</a>
 */
public class CascadedRowManager extends FilteredRowManager {
    
    /**
     * Create a new CascadedRowManager.
     * @param table the table to manage
     */
    public CascadedRowManager(Table table) {
        super(table);
    }
    
    /**
     * @see prefuse.data.util.RowManager#getColumnRow(int, int)
     */
    public int getColumnRow(int row, int col) {
        if ( !isValidRow(row) )
            return -1;
        else if ( col >= ((CascadedTable)getTable()).getLocalColumnCount() )
            return ((CascadedTable)m_table).getParentTable()
                        .getColumnRow(getParentRow(row), col);
        else
            return row;
    }
    
    /**
     * @see prefuse.data.util.RowManager#getTableRow(int, int)
     */
    public int getTableRow(int columnRow, int col) {
        int row;
        if ( col < ((CascadedTable)getTable()).getLocalColumnCount() ) {
            row = columnRow;
        } else {
            row = getChildRow(columnRow);
        }
        return isValidRow(row) ? row : -1;
    }
    
} // end of class CascadedRowManager
