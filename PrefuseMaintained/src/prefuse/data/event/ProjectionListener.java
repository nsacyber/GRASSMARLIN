package prefuse.data.event;

import java.util.EventListener;

import prefuse.data.util.ColumnProjection;

/**
 * Listener interface for monitoring changes to the state of a
 * column projection filter.
 * 
 * @author <a href="http://jheer.org">jeffrey heer</a>
 */
public interface ProjectionListener extends EventListener {

    /**
     * Notification that the internal state of a projection has been updated.
     * @param projection the source of the change notification
     */
    public void projectionChanged(ColumnProjection projection);
    
} // end of interface ProjectionListener
