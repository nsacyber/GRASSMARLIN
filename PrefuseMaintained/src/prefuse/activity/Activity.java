package prefuse.activity;

import prefuse.util.collections.CopyOnWriteArrayList;


/**
 * Represents an activity that can be scheduled and run. This could include
 * data processing, animation, and time-sensitive operations.
 *
 * @author <a href="http://jheer.org">jeffrey heer</a>
 * @see prefuse.activity.ActivityManager
 * @see prefuse.action.Action
 */
public abstract class Activity {

    public static final long INFINITY = -1L; // specifies infinite duration
    public static final long DEFAULT_STEP_TIME = 15L;
    
    private boolean m_enabled = true;
    private Pacer m_pacer;
    
    private long m_startTime = -1L;
    private long m_duration  = -1L;
    private long m_stepTime  = -1L;
    private long m_nextTime  = -1L;
    private boolean m_isRunning = false;
    private boolean m_isScheduled = false;
    
    private CopyOnWriteArrayList m_listeners;
    
    /**
     * Creates a new Activity.
     * @param duration the length of this activity.
     *  A value of {@link #INFINITY} indicates an infinite running time.
     * @see prefuse.activity.Activity#Activity(long, long, long)     
     */
    public Activity(long duration) {
        this(duration, DEFAULT_STEP_TIME);
    }
    
    /**
     * Creates a new Activity.
     * @param duration the length of this activity.
     *  A value of {@link #INFINITY} indicates an infinite running time.
     * @param stepTime the delay time between steps of this activity
     * @see prefuse.activity.Activity#Activity(long, long, long)
     */
    public Activity(long duration, long stepTime) {
        this(duration, stepTime, System.currentTimeMillis());
    }
    
    /**
     * Creates a new Activity.
     * @param duration the length of this activity. 
     *  A value of {@link #INFINITY} indicates an infinite running time.
     * @param stepTime the delay time between steps of this activity
     * @param startTime the time at which this activity should begin
     */
    public Activity(long duration, long stepTime, long startTime) {
        m_startTime = startTime;
        m_nextTime  = startTime;
        m_duration  = duration;
        m_stepTime  = stepTime;
    }
       
    /**
     * Schedules this Activity to start immediately.
     */
    public void run() {
        ActivityManager.scheduleNow(this);
    }
    
    /**
     * Schedules this Activity for the specified startTime, overwriting the
     * Activity's currently set startTime.
     * @param startTime the time at which the activity should run
     */
    public void runAt(long startTime) {
        ActivityManager.scheduleAt(this,startTime);
    }
    
    /**
     * Schedules this Activity to start immediately after another Activity.
     * This Activity will be scheduled to start immediately after the
     * first one finishes, overwriting any previously set startTime. If the
     * first Activity is cancelled, this one will not run.
     * 
     * This functionality is provided by using an ActivityListener to monitor
     * the first Activity. The listener is removed upon completion or
     * cancellation of the first Activity.
     * 
     * This method does not in any way affect the scheduling of the first 
     * Activity. If the first Activity is never scheduled, this Activity
     * will correspondingly never be run unless scheduled by a separate
     * scheduling call.
     * @param before the Activity that must finish before this one starts
     */
    public void runAfter(Activity before) {
        ActivityManager.scheduleAfter(before, this);
    }
    
    /**
     * Schedules this Activity to start immediately after another Activity.
     * This Activity will be scheduled to start immediately after the
     * first one finishes, overwriting any previously set startTime. If the
     * first Activity is cancelled, this one will not run.
     * 
     * This functionality is provided by using an ActivityListener to monitor
     * the first Activity. The listener will persist across mulitple runs,
     * meaning the second Activity will always be evoked upon a successful
     * finish of the first.
     * 
     * This method does not in any way affect the scheduling of the first 
     * Activity. If the first Activity is never scheduled, this Activity
     * will correspondingly never be run unless scheduled by a separate
     * scheduling call.
     * @param before the Activity that must finish before this one starts
     */
    public void alwaysRunAfter(Activity before) {
        ActivityManager.alwaysScheduleAfter(before, this);
    }
    
    /**
     * Run this activity one step. Subclasses should override this method to
     * specify the actions this activity should perform.
     * @param elapsedTime the time elapsed since the start of the activity.
     */
    protected abstract void run(long elapsedTime);
    
    /**
     * Run this activity for a single step. This method is called by the
     * ActivityManager -- outside code should have no need to call or
     * override this method. To implement custom activities, override the
     * run() method instead.
     * @param currentTime the time at which this step is being run.
     * @return the time (in milliseconds) when this activity should be
     *  run again. A return value of -1 indicates this activity is finished.
     */
    long runActivity(long currentTime) {
        if (currentTime < m_startTime) {
            return m_startTime - currentTime;  
        }
        
        long elapsedTime = currentTime - m_startTime;
        
        if ( m_duration == 0 || currentTime >= getStopTime() )
        {
            if ( !setRunning(true) ) {
                fireActivityStarted();
            }
            if ( m_enabled ) {               
                run(elapsedTime);
                fireActivityStepped();
            }
            setRunning(false);
            
            fireActivityFinished();
            return -1;
        }
        else if ( currentTime >= m_nextTime )
        {
            if ( !setRunning(true) )
                fireActivityStarted();
            if ( m_enabled ) {
                run(elapsedTime);
                fireActivityStepped();
            }
            m_nextTime = currentTime + m_stepTime;
        }
        
        return (m_nextTime-currentTime);
    }
    
    /**
     * Cancels this activity, if scheduled. This will stop a
     * running activity, and will remove the activity from
     * the ActivityManager's schedule.
     */
    public void cancel() {
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
    	ActivityManager.cancelActivity(this);
    }
    
    /**
     * Indicates if this activity is currently scheduled 
     *  with the ActivityManager
     * @return true if scheduled, false otherwise
     */
    public synchronized boolean isScheduled() {
        return m_isScheduled;
    }
    
    /**
     * Sets whether or not this Activity has been scheduled. This method should
     * only be called by the ActivityManager.
     * @param s the scheduling state of this Activity
     */
    void setScheduled(boolean s) {
        boolean fire;
        synchronized ( this ) {
            fire = (s && !m_isScheduled);
            m_isScheduled = s;
        }
        if ( fire )
            fireActivityScheduled();
    }
    
    /**
     * Sets a flag indicating whether or not this activity is currently running
     * @param s the new running state of this activity
     */
    synchronized boolean setRunning(boolean s) {
        boolean b = m_isRunning;
        m_isRunning = s;
        return b;
    }
    
    /**
     * Indicates if this activity is currently running.
     * @return true if running, false otherwise
     */
    public synchronized boolean isRunning() {
        return m_isRunning;
    }
    
    // ------------------------------------------------------------------------
    // Activity Listeners
    
    /**
     * Add an ActivityListener to monitor this activity's events.
     * @param l the ActivityListener to add
     */
    public void addActivityListener(ActivityListener l) {
        if ( m_listeners == null )
            m_listeners = new CopyOnWriteArrayList();
        if ( !m_listeners.contains(l) )
            m_listeners.add(l);
    }
    
    /**
     * Remove a registered ActivityListener
     * @param l the ActivityListener to remove
     */
    public void removeActivityListener(ActivityListener l) {
        if ( m_listeners == null )
            return;
        m_listeners.remove(l);
        if ( m_listeners.size() == 0 )
            m_listeners = null;
    }

    protected void fireActivityScheduled() {
        if ( m_listeners == null ) return;
        Object[] a = m_listeners.getArray();
        for ( int i=0; i<a.length; ++i ) {
            ((ActivityListener)a[i]).activityScheduled(this);
        }
    }
    
    protected void fireActivityStarted() {
        if ( m_listeners == null ) return;
        Object[] a = m_listeners.getArray();
        for ( int i=0; i<a.length; ++i ) {
            ((ActivityListener)a[i]).activityStarted(this);
        }
    }
    
    protected void fireActivityStepped() {
        if ( m_listeners == null ) return;
        Object[] a = m_listeners.getArray();
        for ( int i=0; i<a.length; ++i ) {
            ((ActivityListener)a[i]).activityStepped(this);
        }
    }
    
    protected void fireActivityFinished() {
        if ( m_listeners == null ) return;
        Object[] a = m_listeners.getArray();
        for ( int i=0; i<a.length; ++i ) {
            ((ActivityListener)a[i]).activityFinished(this);
        }
    }
    
    protected void fireActivityCancelled() {
        if ( m_listeners == null ) return;
        Object[] a = m_listeners.getArray();
        for ( int i=0; i<a.length; ++i ) {
            ((ActivityListener)a[i]).activityCancelled(this);
        }
    }
    
    // ------------------------------------------------------------------------
    // Accessor / Mutator Methods
    
    /**
     * Returns a value between 0 and 1 inclusive, indicating the current
     * position in an animation or other similarly parameterized activity.
     * The returned value is determined by consulting this Activity's
     * pacing function.
     * @param elapsedTime the time in milliseconds since the start of this
     *  Activity.
     * @return a value between 0 and 1 indicating the current position in
     *  an animation.
     * @see prefuse.activity.Activity#getPacingFunction()
     */
    public double getPace(long elapsedTime) {
        long duration = getDuration();
        double frac = (duration == 0L ? 0.0 : ((double)elapsedTime)/duration);
        frac = Math.min(1, Math.max(0, frac));
        return m_pacer!=null ? m_pacer.pace(frac) : frac;
    }
    
    /**
     * Returns the pacing function associated with this Activity. Pacing
     * functions are used to control the pace of animations.
     * @return this Activity's pacing function. A value of null indicates a
     *  basic, linear pace is used, moving from 0 to 1 uniformly over time.
     */
    public synchronized Pacer getPacingFunction() {
        return m_pacer;
    }
    
    /**
     * Sets the pacing function associated with this Activity. Pacing
     * functions are used to control the pace of animations.
     * @param pfunc this Activity's new pacing function, or null to
     *  indicate a basic, linear pace moving from 0 to 1 uniformly
     *  over time.
     */
    public synchronized void setPacingFunction(Pacer pfunc) {
        m_pacer = pfunc;
    }
    
    /**
     * Get the time at which this activity should complete.
     * @return the stopping time for this activity, or Long.MAX_VALUE
     *  if this activity should run indefinitely.
     */
    public long getStopTime() {
        if (m_duration == -1) {
            return Long.MAX_VALUE;
        }
        return m_startTime + m_duration;
    }
    
    /**
     * Get the time at which this activity should be run next.
     * @return the time this activity should run next
     */
    public long getNextTime() {
        return m_nextTime;
    }
    
    /**
     * Returns the duration of this activity
     * @return the duration of this activity, in milliseconds
     */
    public long getDuration() {
        return m_duration;
    }

    /**
     * Set the duration of this activity
     * @param duration The new duration, in milliseconds, for this activity.
     *  A value of {@link #INFINITY} indicates that this activity should run
     *  indefinitely.
     */
    public void setDuration(long duration) {
        this.m_duration = duration;
    }

    /**
     * Returns this activity's start time
     * @return the starting time for this activity
     */
    public long getStartTime() {
        return m_startTime;
    }

    /**
     * Sets this activity's start time
     * @param time the new starting time for this activity
     */
    public void setStartTime(long time) {
        m_startTime = time;
    }

    /**
     * Returns the delay between runs for this activity
     * @return the step time between runs of this activity
     */
    public long getStepTime() {
        return m_stepTime;
    }

    /**
     * Sets the delay between runs for this activity
     * @param time the new step time between runs of this activity
     */
    public void setStepTime(long time) {
        m_stepTime = time;
    }
    
    /**
     * Indicates whether or not this activity is currently enabled.
     * @return true if enabled, false otherwise
     */
    public boolean isEnabled() {
        return m_enabled;
    }
    
    /**
     * Sets whether this component is enabled.
     * @param s true to enable component, false to disable it
     */
    public void setEnabled(boolean s) {
        m_enabled = s;
    }
    
} // end of class Activity
