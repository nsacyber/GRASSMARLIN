package prefuse.action;

import prefuse.Visualization;
import prefuse.activity.Activity;
import prefuse.util.collections.CopyOnWriteArrayList;

/**
 * Abstract base class for Action implementations that hold a collection
 * of subclasses.
 *  
 * @author <a href="http://jheer.org">jeffrey heer</a>
 */
public abstract class CompositeAction extends Action {

    protected CopyOnWriteArrayList m_actions = new CopyOnWriteArrayList();
    
    /**
     * Creates a new run-once CompositeAction.
     */
    public CompositeAction() {
        super(null, 0);
    }

    /**
     * Creates a new run-once CompositeAction that processes the given
     * Visualization.
     * @param vis the {@link prefuse.Visualization} processed by this Action
     */
    public CompositeAction(Visualization vis) {
        super(vis, 0);
    }
    
    /**
     * Creates a new CompositeAction of specified duration and default
     * step time of 20 milliseconds.
     * @param duration the duration of this Activity, in milliseconds
     */
    public CompositeAction(long duration) {
        super(null, duration, Activity.DEFAULT_STEP_TIME);
    }
    
    /**
     * Creates a new CompositeAction of specified duration and default
     * step time of 20 milliseconds that processes the given
     * Visualization.
     * @param vis the {@link prefuse.Visualization} processed by this Action
     * @param duration the duration of this Activity, in milliseconds
     */
    public CompositeAction(Visualization vis, long duration) {
        super(vis, duration, Activity.DEFAULT_STEP_TIME);
    }
    
    /**
     * Creates a new CompositeAction of specified duration and step time.
     * @param duration the duration of this Activity, in milliseconds
     * @param stepTime the time to wait in milliseconds between executions
     *  of the action list
     */
    public CompositeAction(long duration, long stepTime) {
        super(null, duration, stepTime);
    }
    
    // ------------------------------------------------------------------------
    
    /**
     * Set the Visualization processed by this Action. This also calls
     * {@link Action#setVisualization(Visualization)} on all Action instances
     * contained within this composite.
     * @param vis the {@link prefuse.Visualization} to process
     */
    public void setVisualization(Visualization vis) {
        super.setVisualization(vis);
        for ( int i=0; i<m_actions.size(); ++i ) {
            get(i).setVisualization(vis);
        }
    }
    
    /**
     * Returns the number of Actions in the composite.
     * @return the size of this composite
     */
    public int size() {
        return m_actions.size();
    }
    
    /**
     * Adds an Action to the end of the composite list.
     * @param a the Action instance to add
     * @return This reference is returned so that method may be chained.
     */
    public CompositeAction add(Action a) {
        m_actions.add(a);
        return this;
    }
    
    /**
     * Adds an Action at the given index.
     * @param i the index at which to add the Action
     * @param a the Action instance to add
     */
    public void add(int i, Action a) {
        m_actions.add(i, a);
    }
    
    /**
     * Returns the Action at the specified index.
     * @param i the index
     * @return the requested Action
     */
    public Action get(int i) {
        return (Action)m_actions.get(i);
    }
    
    /**
     * Removes a given Action from the composite.
     * @param a the Action to remove
     * @return true if the Action was found and removed, false otherwise
     */
    public boolean remove(Action a) {
        return m_actions.remove(a);
    }
    
    /**
     * Removes the Action at the specified index.
     * @param i the index
     * @return the removed Action
     */
    public Action remove(int i) {
        return (Action)m_actions.remove(i);
    }
    
} // end of class CompositeAction
