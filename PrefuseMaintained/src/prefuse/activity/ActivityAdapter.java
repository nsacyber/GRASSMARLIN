package prefuse.activity;

/**
 * Adapter class for ActivityListeners. Provides empty implementations of
 * ActivityListener routines.
 * 
 * @version 1.0
 * @author <a href="http://jheer.org">jeffrey heer</a>
 */
public class ActivityAdapter implements ActivityListener {

    /**
     * @see prefuse.activity.ActivityListener#activityScheduled(prefuse.activity.Activity)
     */
    public void activityScheduled(Activity a) {
    }

    /**
     * @see prefuse.activity.ActivityListener#activityStarted(prefuse.activity.Activity)
     */
    public void activityStarted(Activity a) {
    }

    /**
     * @see prefuse.activity.ActivityListener#activityStepped(prefuse.activity.Activity)
     */
    public void activityStepped(Activity a) {
    }

    /**
     * @see prefuse.activity.ActivityListener#activityFinished(prefuse.activity.Activity)
     */
    public void activityFinished(Activity a) {
    }

    /**
     * @see prefuse.activity.ActivityListener#activityCancelled(prefuse.activity.Activity)
     */
    public void activityCancelled(Activity a) {
    }

} // end of class ActivityAdapter
