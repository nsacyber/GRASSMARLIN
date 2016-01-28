package prefuse.data.util;

import prefuse.data.event.ProjectionListener;
import prefuse.util.collections.CopyOnWriteArrayList;

/**
 * Abstract base class for column projection instances. Implements the
 * listener functionality.
 * 
 * @author <a href="http://jheer.org">jeffrey heer</a>
 */
public abstract class AbstractColumnProjection implements ColumnProjection {

    // ------------------------------------------------------------------------
    // Listener Methods
    
    private CopyOnWriteArrayList m_listeners;
    
    /**
     * @see prefuse.data.util.ColumnProjection#addProjectionListener(prefuse.data.event.ProjectionListener)
     */
    public void addProjectionListener(ProjectionListener lstnr) {
        if ( m_listeners == null )
            m_listeners = new CopyOnWriteArrayList();
        if ( !m_listeners.contains(lstnr) )
            m_listeners.add(lstnr);
    }

    /**
     * @see prefuse.data.util.ColumnProjection#removeProjectionListener(prefuse.data.event.ProjectionListener)
     */
    public void removeProjectionListener(ProjectionListener lstnr) {
        if ( m_listeners != null )
            m_listeners.remove(lstnr);
        if ( m_listeners.size() == 0 )
            m_listeners = null;
    }
    
    public void fireUpdate() {
        if ( m_listeners == null )
            return;
        Object[] lstnrs = m_listeners.getArray();
        for ( int i=0; i<lstnrs.length; ++i ) {
            ((ProjectionListener)lstnrs[i]).projectionChanged(this);
        }
    }
    
} // end of abstract class AbstractColumnProjection
