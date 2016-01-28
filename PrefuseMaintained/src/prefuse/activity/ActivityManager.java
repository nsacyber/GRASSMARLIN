package prefuse.activity;

import java.util.ArrayList;

import prefuse.util.PrefuseConfig;


/**
 * <p>The ActivityManager is responsible for scheduling and running timed 
 * activities that perform data processing and animation.</p>
 * 
 * <p>The AcivityManager runs in its own separate thread of execution, and
 * one instance is used to schedule activities from any number of currently
 * active visualizations. The class is implemented as a singleton; the single
 * instance of this class is interacted with through static methods. These
 * methods are called by an Activity's run methods, and so are made only
 * package visible here.</p>
 * 
 * <p>Activity instances can be scheduled by using their  
 * {@link prefuse.activity.Activity#run()},
 * {@link prefuse.activity.Activity#runAt(long)}, and 
 * {@link prefuse.activity.Activity#runAfter(Activity)}
 * methods. These will automatically call the
 * appropriate methods with the ActivityManager.</p>
 * 
 * <p>For {@link prefuse.action.Action} instances, one can also register
 * the actions with a {@link prefuse.Visualization} and use the
 * visualizations provided run methods to launch Actions in a
 * convenient fashion. The interface, which is backed by an {@link ActivityMap}
 * instance, also provides a useful level of indirection, allowing actions
 * to be changed dynamically without changes to code in other locations.
 * </p>
 *
 * @author <a href="http://jheer.org">jeffrey heer</a>
 * @see Activity
 * @see prefuse.action.Action
 */
public class ActivityManager extends Thread {
    
    private static ActivityManager s_instance;
    
    private ArrayList m_activities;
    private ArrayList m_tmp;
    private long      m_nextTime;
    private boolean   m_run;
    
    /**
     * Returns the active ActivityManager instance.
     * @return the ActivityManager
     */
    private synchronized static ActivityManager getInstance() {
        if ( s_instance == null || !s_instance.isAlive() ) {
            s_instance = new ActivityManager();
        }
        return s_instance;
    }
    
    /**
     * Create a new ActivityManger.
     */
    private ActivityManager() {
        super("prefuse_ActivityManager");
        m_activities = new ArrayList();
        m_tmp = new ArrayList();
        m_nextTime = Long.MAX_VALUE;
        
        int priority = PrefuseConfig.getInt("activity.threadPriority");
        if ( priority >= Thread.MIN_PRIORITY && 
             priority <= Thread.MAX_PRIORITY )
        {
            this.setPriority(priority);
        }
        this.setDaemon(true);
        this.start();
    }
    
    /**
     * Stops the activity manager thread. All scheduled actvities are
     * canceled, and then the thread is then notified to stop running.
     */
    public static void stopThread() {
        ActivityManager am;
        synchronized ( ActivityManager.class ) {
            am = s_instance;
        }
        if ( am != null )
            am._stop();
    }
    
    /**
     * Schedules an Activity with the manager.
     * @param a the Activity to schedule
     */
    static void schedule(Activity a) {
        getInstance()._schedule(a, a.getStartTime());
    }
    
    /**
     * Schedules an Activity to start immediately, overwriting the
     * Activity's currently set startTime.
     * @param a the Activity to schedule
     */
    static void scheduleNow(Activity a) {
        getInstance()._schedule(a, System.currentTimeMillis());
    }
    
    /**
     * Schedules an Activity at the specified startTime, overwriting the
     * Activity's currently set startTime.
     * @param a the Activity to schedule
     * @param startTime the time at which the activity should run
     */
    static void scheduleAt(Activity a, long startTime) {
        getInstance()._schedule(a, startTime);
    }
    
    /**
     * Schedules an Activity to start immediately after another Activity.
     * The second Activity will be scheduled to start immediately after the
     * first one finishes, overwriting any previously set startTime. If the
     * first Activity is cancelled, the second one will not run.
     * 
     * This functionality is provided by using an ActivityListener to monitor
     * the first Activity. The listener is removed upon completion or
     * cancellation of the first Activity.
     * 
     * This method does not effect the scheduling of the first Activity.
     * @param before the first Activity to run
     * @param after the Activity to run immediately after the first
     */
    static void scheduleAfter(Activity before, Activity after) {
        getInstance()._scheduleAfter(before, after);
    }
    
    /**
     * Schedules an Activity to start immediately after another Activity.
     * The second Activity will be scheduled to start immediately after the
     * first one finishes, overwriting any previously set startTime. If the
     * first Activity is cancelled, the second one will not run.
     * 
     * This functionality is provided by using an ActivityListener to monitor
     * the first Activity. The listener will persist across mulitple runs,
     * meaning the second Activity will always be evoked upon a successful
     * finish of the first.
     * 
     * This method does not otherwise effect the scheduling of the first Activity.
     * @param before the first Activity to run
     * @param after the Activity to run immediately after the first
     */
    static void alwaysScheduleAfter(Activity before, Activity after) {
        getInstance()._alwaysScheduleAfter(before, after);
    }
    
    /**
     * Cancels an Activity and removes it from this manager, called by
     * an Activity when the activity needs to be cancelled. 
     * @param a The activity to cancel.
     */
    static void cancelActivity(Activity a){
    	getInstance()._cancelActivity(a);
    }
    
    /**
     * Returns the number of scheduled activities
     * @return the number of scheduled activities
     */
    public static int activityCount() {
        return getInstance()._activityCount();
    }
    
    /**
     * Stops the activity manager thread. All scheduled actvities are
     * canceled, and then the thread is then notified to stop running.
     */
    private synchronized void _stop() {
        while ( m_activities.size() > 0 ) {
            Activity a = (Activity)m_activities.get(m_activities.size()-1);
            a.cancel();
        }
        _setRunning(false);
        notify();
    }
    
    /**
     * Schedules an Activity with the manager.
     * @param a the Activity to schedule
     */
    private void _schedule(Activity a, long startTime) {
        if ( a.isScheduled() ) {
        	try { notifyAll(); } catch ( Exception e ) {}
            return; // already scheduled, do nothing
        }
        a.setStartTime(startTime);
        synchronized ( this ) {
            m_activities.add(a);
            a.setScheduled(true);
            if ( startTime < m_nextTime ) { 
               m_nextTime = startTime;
               notify();
            }
        }
    }
    
    /**
     * Schedules an Activity to start immediately after another Activity.
     * The second Activity will be scheduled to start immediately after the
     * first one finishes, overwriting any previously set startTime. If the
     * first Activity is cancelled, the second one will not run.
     * 
     * This functionality is provided by using an ActivityListener to monitor
     * the first Activity. The listener is removed upon completion or
     * cancellation of the first Activity.
     * 
     * This method does not effect the scheduling of the first Activity.
     * @param before the first Activity to run
     * @param after the Activity to run immediately after the first
     */
    private void _scheduleAfter(Activity before, Activity after) {
        before.addActivityListener(new ScheduleAfterActivity(after,true));
    }
    
    /**
     * Schedules an Activity to start immediately after another Activity.
     * The second Activity will be scheduled to start immediately after the
     * first one finishes, overwriting any previously set startTime. If the
     * first Activity is cancelled, the second one will not run.
     * 
     * This functionality is provided by using an ActivityListener to monitor
     * the first Activity. The listener will persist across mulitple runs,
     * meaning the second Activity will always be evoked upon a successful
     * finish of the first.
     * 
     * This method does not otherwise effect the scheduling of the first Activity.
     * @param before the first Activity to run
     * @param after the Activity to run immediately after the first
     */
    private void _alwaysScheduleAfter(Activity before, Activity after) {
        before.addActivityListener(new ScheduleAfterActivity(after,false));
    }
    
    /**
     * Cancels an action, called by an Activity when it is cancelled. 
     * Application code should not call this method! Instead, use 
     * Activity.cancel() to stop a sheduled or running Activity.
     * @param a The Activity to cancel
     */
    private void _cancelActivity(Activity a){
    	/*
    	 * Prefuse Bug ID #1708926
         * The fix ("Contribution") has not been tested and/or validated for release as or in products,
         * combinations with products or other commercial use.
         * Any use of the Contribution is entirely made at the user's own responsibility and the user can
         * not rely on any features, functionalities or performances Alcatel-Lucent has attributed to the Contribution.
         * THE CONTRIBUTION BY ALCATEL-LUCENT (...) IS PROVIDED AS IS, WITHOUT WARRANTY OF ANY KIND,
         * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
         * FITNESS FOR A PARTICULAR PURPOSE, COMPLIANCE, NON-INTERFERENCE  AND/OR INTERWORKING WITH THE SOFTWARE
         * TO WHICH THE CONTRIBUTION HAS BEEN MADE, TITLE AND NON-INFRINGEMENT.
         * IN NO EVENT SHALL ALCATEL-LUCENT (...) BE LIABLE FOR ANY DAMAGES OR OTHER LIABLITY,
         * WHETHER IN CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE CONTRIBUTION
         * OR THE USE OR OTHER DEALINGS IN THE CONTRIBUTION,
         * WHETHER TOGETHER WITH THE SOFTWARE TO WHICH THE CONTRIBUTION RELATES OR ON A STAND ALONE BASIS.
    	 */
        boolean fire = false;
        //removeActivity synchronizes on this, we need to lock this
        //before we lock the activity to avoid deadlock
        synchronized ( this ) {
        	synchronized(a)
        	{
        		if ( a.isScheduled() ) {
        			// attempt to remove this activity, if the remove fails,
        			// this activity is not currently scheduled with the manager
        			_removeActivity(a);
        			fire = true;
        		}
        		a.setRunning(false);
        	}
        }
        if ( fire )
            a.fireActivityCancelled();
    }
    
    /**
     * Removes an Activity from this manager, called by an
     * Activity when it finishes or is cancelled. Application 
     * code should not call this method! Instead, use 
     * Activity.cancel() to stop a sheduled or running Activity.
     * @param a
     * @return true if the activity was found and removed, false
     *  if the activity is not scheduled with this manager.
     */
    private boolean _removeActivity(Activity a) {
        boolean r;
        synchronized ( this ) {
            r = m_activities.remove(a);
            if ( r ) {
                if ( m_activities.size() == 0 ) {
                    m_nextTime = Long.MAX_VALUE;
                }
            }
        }
        if ( r ) {
            a.setScheduled(false);
        }
        return r;
    }
    
    /**
     * Returns the number of scheduled activities
     * @return the number of scheduled activities
     */
    private synchronized int _activityCount() {
        return m_activities.size();
    }
    
    /**
     * Sets the running flag for the ActivityManager instance.
     */
    private synchronized void _setRunning(boolean b) {
        m_run = b;
    }
    
    /**
     * Used by the activity loop to determine if the ActivityManager
     * thread should keep running or exit.
     */
    private synchronized boolean _keepRunning() {
        return m_run;
    }
    
    /**
     * Main scheduling thread loop. This is automatically started upon
     * initialization of the ActivityManager.
     */
    public void run() {
        _setRunning(true);
        while ( _keepRunning() ) {
            if ( _activityCount() > 0 ) {
                long currentTime = System.currentTimeMillis();
                long t = -1;
                
                synchronized (this) {
                    // copy content of activities, as new activities might
                    // be added while we process the current ones
                    for ( int i=0; i<m_activities.size(); i++ ) {
                        Activity a = (Activity)m_activities.get(i);
                        m_tmp.add(a);
                        
                        // remove activities that won't be run again
                        if ( currentTime >= a.getStopTime() )
                        {
                            m_activities.remove(i--);
                            a.setScheduled(false);
                        }
                    }
                    // if no activities left, reflect that in the next time
                    if ( m_activities.size() == 0 ) {
                        m_nextTime = Long.MAX_VALUE;
                    }
                }
                
                for ( int i=0; i<m_tmp.size(); i++ ) {
                    // run the activity - the activity will check for
                    // itself if it should perform any action or not
                    Activity a = (Activity)m_tmp.get(i);
                    long s = a.runActivity(currentTime);
                    // compute minimum time for next activity cycle
                    t = (s<0 ? t : t<0 ? s : Math.min(t,s));
                }

                // clear the temporary list
                m_tmp.clear();
                
                if ( t == -1 ) continue;
                
                // determine the next time we should run
                try {
                    synchronized (this) { wait(t); }
                } catch (InterruptedException e) { }
                
            } else {
                // nothing to do, chill out until notified
                try {
                    synchronized (this) { wait(); }
                } catch (InterruptedException e) { }
            }
        }
    }
    
    public class ScheduleAfterActivity extends ActivityAdapter {
        Activity after;
        boolean remove;
        public ScheduleAfterActivity(Activity after, boolean remove) {
            this.after = after;
            this.remove = remove;
        }
        public void activityFinished(Activity a) {
            if ( remove ) a.removeActivityListener(this);
            scheduleNow(after);
        }
        public void activityCancelled(Activity a) {
            if ( remove ) a.removeActivityListener(this);
        }
    } // end of inner class ScheduleAfterActivity
    
} // end of class ActivityManager
