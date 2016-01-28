package prefuse.data.event;

import java.util.EventListener;

import prefuse.data.Graph;

/**
 * Listner interface for monitoring changes to a graph or tree structure.
 * 
 * @author <a href="http://jheer.org">jeffrey heer</a>
 */
public interface GraphListener extends EventListener {
    
    /**
     * Notification that a graph has changed.
     * @param g the graph that has changed
     * @param table the particular table within the graph that has changed
     * @param start the starting row index of the changed table region
     * @param end the ending row index of the changed table region
     * @param col the column that has changed, or
     * {@link EventConstants#ALL_COLUMNS} if the operation affects all
     * columns
     * @param type the type of modification, one of
     * {@link EventConstants#INSERT}, {@link EventConstants#DELETE}, or
     * {@link EventConstants#UPDATE}.
     */
    public void graphChanged(Graph g, String table, 
            int start, int end, int col, int type);
    
} // end of interface GraphListener
