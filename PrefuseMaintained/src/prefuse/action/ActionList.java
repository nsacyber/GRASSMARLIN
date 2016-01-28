package prefuse.action;

import java.util.logging.Logger;

import prefuse.Visualization;
import prefuse.activity.Activity;
import prefuse.activity.Pacer;
import prefuse.util.StringLib;


/**
 * <p>The ActionList represents a chain of Actions that process VisualItems.
 * ActionList also implements the Action interface, so ActionLists can be placed
 * within other ActionList or {@link ActionSwitch} instances,
 * allowing recursive composition of different sets of Actions.</p>
 * 
 * @author <a href="http://jheer.org">jeffrey heer</a>
 * @see prefuse.activity.Activity
 * @see prefuse.action.Action
 */
public class ActionList extends CompositeAction {

    private static final Logger s_logger = 
        Logger.getLogger(ActionList.class.getName());
    
    /**
     * Creates a new run-once ActionList.
     */
    public ActionList() {
        super(0);
    }
    
    /**
     * Creates a new run-once ActionList that processes the given
     * Visualization.
     * @param vis the {@link prefuse.Visualization} to process.
     */
    public ActionList(Visualization vis) {
        super(vis);
    }
    
    /**
     * Creates a new ActionList of specified duration and default
     * step time of 20 milliseconds.
     * @param duration the duration of this Activity, in milliseconds
     */
    public ActionList(long duration) {
        super(duration, Activity.DEFAULT_STEP_TIME);
    }
    

    @Override
    public ActionList add(Action a) {
        super.add(a);
        return this;
    }
    
    public synchronized ActionList setPacerFunction(Pacer pfunc) {
        super.setPacingFunction(pfunc);
        return this;
    }
    
    /**
     * Creates a new ActionList which processes the given Visualization
     * and has the specified duration and a default step time of 20
     * milliseconds.
     * @param vis the {@link prefuse.Visualization} to process.
     * @param duration the duration of this Activity, in milliseconds
     */
    public ActionList(Visualization vis, long duration) {
        super(vis, duration);
    }
    
    /**
     * Creates a new ActionList of specified duration and step time.
     * @param duration the duration of this Activity, in milliseconds
     * @param stepTime the time to wait in milliseconds between executions
     *  of the action list
     */
    public ActionList(long duration, long stepTime) {
        super(duration, stepTime);
    }

    /**
     * @see prefuse.action.Action#run(double)
     */
    public void run(double frac) {
        Object[] actions = m_actions.getArray();
        for ( int i=0; i<actions.length; ++i ) {
            Action a = (Action)actions[i];
            try {
                if ( a.isEnabled() ) a.run(frac);
            } catch ( Exception e ) {
                s_logger.warning(e.getMessage() + '\n'
                        + StringLib.getStackTrace(e));
            }
        }
    }

} // end of class ActionList
