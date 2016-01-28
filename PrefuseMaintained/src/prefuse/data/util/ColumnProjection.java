package prefuse.data.util;

import prefuse.data.column.Column;
import prefuse.data.event.ProjectionListener;

/**
 * Interface for filtering only a subset of a Table columns, computing
 * a projection of the available data fields. Used in conjunction with
 * CascadedTable instances to control what fields are inherited from
 * a parent table.
 * 
 * @author <a href="http://jheer.org">jeffrey heer</a>
 */
public interface ColumnProjection {

    /**
     * Determine if the given Column should be included.
     * @param col the Column to test
     * @param name the name of the column
     * @return true if the column passes the projection, false otherwise
     */
    public boolean include(Column col, String name);
    
    /**
     * Add a listener to this column projection
     * @param lstnr the listener to add
     */
    public void addProjectionListener(ProjectionListener lstnr);
    
    /**
     * Remove a listener from this column projection
     * @param lstnr the listener to remove
     */
    public void removeProjectionListener(ProjectionListener lstnr);
    
} // end of interface ColumnProjection
