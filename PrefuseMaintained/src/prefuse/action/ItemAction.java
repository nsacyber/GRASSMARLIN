package prefuse.action;

import java.util.Iterator;

import prefuse.Visualization;
import prefuse.data.expression.Predicate;
import prefuse.visual.VisualItem;
import prefuse.visual.expression.VisiblePredicate;

/**
 * An Action that processes VisualItems one item at a time. By default,
 * it only processes items that are visible. Use the
 * {@link #setFilterPredicate(Predicate)} method
 * to change the filtering criteria.
 * 
 * @author <a href="http://jheer.org">jeffrey heer</a>
 */
public abstract class ItemAction extends GroupAction {
    
    /** A reference to filtering predicate for this Action */
    protected Predicate m_predicate;
    
    /**
     * Create a new ItemAction that processes all groups.
     * @see Visualization#ALL_ITEMS
     */
    public ItemAction() {
        this((Visualization)null);
    }
    
    /**
     * Create a new ItemAction that processes all groups.
     * @param vis the {@link prefuse.Visualization} to process
     * @see Visualization#ALL_ITEMS
     */
    public ItemAction(Visualization vis) {
        this(vis, Visualization.ALL_ITEMS);
    }
    
    /**
     * Create a new ItemAction that processes the specified group.
     * @param group the name of the group to process
     */
    public ItemAction(String group) {
        this(null, group);
    }
    
    /**
     * Create a new ItemAction that processes the specified group.
     * @param group the name of the group to process
     * @param filter the filtering {@link prefuse.data.expression.Predicate}
     */
    public ItemAction(String group, Predicate filter) {
        this(null, group, filter);
    }
    
    /**
     * Create a new ItemAction that processes the specified group.
     * @param vis the {@link prefuse.Visualization} to process
     * @param group the name of the group to process
     */
    public ItemAction(Visualization vis, String group) {
        this(vis, group, VisiblePredicate.TRUE);
    }

    /**
     * Create a new ItemAction that processes the specified group.
     * @param vis the {@link prefuse.Visualization} to process
     * @param group the name of the group to process
     * @param filter the filtering {@link prefuse.data.expression.Predicate}
     */
    public ItemAction(Visualization vis, String group, Predicate filter) {
        super(vis, group);
        m_predicate = filter;
    }
    
    // ------------------------------------------------------------------------
    
    /**
     * Returns the filtering predicate used by this Action.
     * @return the filtering {@link prefuse.data.expression.Predicate}
     */
    public Predicate getFilterPredicate() {
        return m_predicate;
    }

    /**
     * Sets the filtering predicate used by this Action.
     * @param filter the filtering {@link prefuse.data.expression.Predicate}
     * to use
     */
    public void setFilterPredicate(Predicate filter) {
        m_predicate = filter;
    }
    
    /**
     * @see prefuse.action.Action#run(double)
     */
    public void run(double frac) {
        Iterator items = getVisualization().items(m_group, m_predicate);
        while ( items.hasNext() ) {
            VisualItem item = (VisualItem)items.next();
            if( item != null && item.getRow() != -1 ) {
                process(item, frac);
            }
        }
    }
    
    /**
     * Processes an individual item.
     * @param item the VisualItem to process
     * @param frac the fraction of elapsed duration time
     */
    public abstract void process(VisualItem item, double frac);

} // end of class ItemAction
