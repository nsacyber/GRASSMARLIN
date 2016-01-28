package prefuse.data.event;

import java.util.EventListener;

import prefuse.data.column.Column;

/**
 * Listener interface for monitoring changes to a data column.
 * 
 * @author <a href="http://jheer.org">jeffrey heer</a>
 */
public interface ColumnListener extends EventListener {

    /**
     * Notification that a data column has changed.
     * @param src the column that has changed
     * @param type One of {@link EventConstants#INSERT},
     * {@link EventConstants#DELETE}, or {@link EventConstants#UPDATE}.
     * @param start the first column index that has been changed
     * @param end the last column index that has been changed
     */
    public void columnChanged(Column src, int type, int start, int end);
    
    /**
     * Notification that a data column has changed.
     * @param src the column that has changed
     * @param idx the column row index that has changed
     * @param prev the previous value at the given location
     */
    public void columnChanged(Column src, int idx, int prev);
    
    /**
     * Notification that a data column has changed.
     * @param src the column that has changed
     * @param idx the column row index that has changed
     * @param prev the previous value at the given location
     */
    public void columnChanged(Column src, int idx, long prev);
    
    /**
     * Notification that a data column has changed.
     * @param src the column that has changed
     * @param idx the column row index that has changed
     * @param prev the previous value at the given location
     */
    public void columnChanged(Column src, int idx, float prev);
    
    /**
     * Notification that a data column has changed.
     * @param src the column that has changed
     * @param idx the column row index that has changed
     * @param prev the previous value at the given location
     */
    public void columnChanged(Column src, int idx, double prev);
    
    /**
     * Notification that a data column has changed.
     * @param src the column that has changed
     * @param idx the column row index that has changed
     * @param prev the previous value at the given location
     */
    public void columnChanged(Column src, int idx, boolean prev);
    
    /**
     * Notification that a data column has changed.
     * @param src the column that has changed
     * @param idx the column row index that has changed
     * @param prev the previous value at the given location
     */
    public void columnChanged(Column src, int idx, Object prev);
    
} // end of interface ColumnListener
