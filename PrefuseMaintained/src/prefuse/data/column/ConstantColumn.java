package prefuse.data.column;

import prefuse.data.DataTypeException;
import prefuse.data.event.ColumnListener;

/**
 * Column implementation holding a single, constant value for all rows.
 * 
 * @author <a href="http://jheer.org">jeffrey heer</a>
 */
public class ConstantColumn extends AbstractColumn {

    private int m_size;

    /**
     * Create a new ConstantColumn.
     * @param type the data type of this column
     * @param defaultValue the default value used for all rows
     */
    public ConstantColumn(Class type, Object defaultValue) {
        super(type, defaultValue);
    }
    
    /**
     * @see prefuse.data.column.Column#getRowCount()
     */
    public int getRowCount() {
        return m_size+1;
    }

    /**
     * @see prefuse.data.column.Column#setMaximumRow(int)
     */
    public void setMaximumRow(int nrows) {
        m_size = nrows;
    }

    /**
     * @see prefuse.data.column.Column#get(int)
     */
    public Object get(int row) {
        if ( row < 0 || row > m_size ) {
            throw new IllegalArgumentException("Row index out of bounds: "+row);
        }
        return super.m_defaultValue;
    }

    /**
     * Unsupported operation.
     * @see prefuse.data.column.Column#set(java.lang.Object, int)
     */
    public void set(Object val, int row) throws DataTypeException {
        throw new UnsupportedOperationException(
                "Can't set values on a ConstantColumn");
    }

    /**
     * Returns false.
     * @see prefuse.data.column.Column#canSet(java.lang.Class)
     */
    public boolean canSet(Class type) {
        return false;
    }    
    
    /**
     * Does nothing.
     * @see prefuse.data.column.Column#addColumnListener(prefuse.data.event.ColumnListener)
     */
    public void addColumnListener(ColumnListener listener) {
        return; // column can't change, so nothing to listen to
    }

    /**
     * Does nothing.
     * @see prefuse.data.column.Column#removeColumnListener(prefuse.data.event.ColumnListener)
     */
    public void removeColumnListener(ColumnListener listener) {
        return; // column can't change, so nothing to listen to
    }
    
} // end of class Constant Column
