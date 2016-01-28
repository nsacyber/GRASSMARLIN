package prefuse.activity;

import java.util.HashMap;

/**
 * <p>Maps between Activity instances and user-defined keys. Can be used to
 * maintain and schedule Activity instances through a layer of indirection.</p>
 * 
 * <p>
 * For example, an Activity could be stored in the map using the method
 * call put("activity", activityRef). The Activity pointed to by activityRef
 * could then be subsequently scheduled using the method call 
 * run("activity"). Furthermore, the Activity referred to by the
 * key "activity" could be changed later by another call to put(), changing
 * a visualization's behavior without modifying any other application code.
 * </p>
 *
 * @author <a href="http://jheer.org">jeffrey heer</a>
 */
public class ActivityMap {

    private HashMap     m_map;
    private ActivityMap m_parent;
    
    /**
     * Creates a new ActivityMap instance.
     */
    public ActivityMap() {
        this(null);
    }
    
    /**
     * Creates a new ActivityMap instance with the specified parent map.
     * @param parent The parent map to use. This map is referred to to resolve
     * keys that are not found within this, the child map.
     */
    public ActivityMap(ActivityMap parent) {
        m_map = new HashMap();
        m_parent = parent;
    }
    
    /**
     * Clears the contents of this ActivityMap. Does not affect the parent map.
     */
    public void clear() {
        m_map.clear();
    }
    
    /**
     * Returns the number of mappings in this ActivityMap. Does not include
     * mappings stored in the parent map.
     * @return the number of mappings in this ActivityMap
     */
    public int size() {
        return m_map.size();
    }
    
    /**
     * Returns the Activity associated with the given key. If the key is not
     * found in this map, the parent map is consulted. If no result is found,
     * null is returned.
     * @param key the key corresponding to a requested Activity instance
     * @return the requested Activity instance, or null if not found by this map
     * or the parent map.
     */
    public Activity get(String key) {
        Activity a = (Activity)m_map.get(key);
        return (a==null && m_parent!=null ? m_parent.get(key) : a);
    }

    /**
     * Runs the Activity corresponding to the given key with the
     * ActivityManager to begin at the specified time.
     * @param key the key corresponding to the Activity to run
     * @param time the start time at which to begin the Activity
     * @return the scheduled Activity, or null if not found
     */
    public Activity runAt(String key, long time) {
        Activity a = get(key);
        if ( a != null )
            ActivityManager.scheduleAt(a,time);
        return a;
    }    
    
    /**
     * Schedules the Activity corresponding to the given key to be run
     * immediately by the ActivityManager.
     * @param key the key corresponding to the Activity to run
     * @return the scheduled Activity, or null if not found
     */
    public Activity run(String key) {
        Activity a = get(key);
        if ( a != null )
            ActivityManager.scheduleNow(a);
        return a;
    }
    
    /**
     * Schedules the Activity corresponding to the afterKey to be run
     * immediately after the completion of the Activity corresponding to 
     * the beforeKey. This method has no scheduling effect on the Activity
     * corresponding to the before key.
     * @param beforeKey the key corresponding to the first Activity
     * @param afterKey the key corresponding to the Activity to be scheduled
     *  after the completion of the first.
     * @return the second, newly scheduled Activity, or null if either of the
     * keys are not found
     */
    public Activity runAfter(String beforeKey, String afterKey) {
        Activity before = get(beforeKey);
        Activity after  = get(afterKey);
        if ( before != null && after != null )
            ActivityManager.scheduleAfter(before, after);
        return after;
    }
    
    /**
     * Schedules the Activity corresponding to the afterKey to always be run
     * immediately after the completion of the Activity corresponding to 
     * the beforeKey. This method has no scheduling effect on the Activity
     * corresponding to the before key.
     * @param beforeKey the key corresponding to the first Activity
     * @param afterKey the key corresponding to the Activity to be scheduled
     *  after the completion of the first.
     * @return the second, newly scheduled Activity, or null if either of the
     * keys are not found
     */
    public Activity alwaysRunAfter(String beforeKey, String afterKey) {
        Activity before = get(beforeKey);
        Activity after  = get(afterKey);
        if ( before != null && after != null )
            ActivityManager.alwaysScheduleAfter(before, after);
        return after;
    }
    
    /**
     * Cancels the Activity corresponding to the given key.
     * @param key the lookup key for the Activity to cancel
     * @return the cancelled Activity, or null if no Activity
     *  was found for the given key.
     */
    public Activity cancel(String key) {
        Activity a = get(key);
        if ( a != null )
            a.cancel();
        return a;
    }
    
    /**
     * Associates the given key with the given Activity
     * @param key the key to associate with the Activity
     * @param activity an Activity instance
     * @return the Activity previously mapped to by the key, or null if none
     */
    public Activity put(String key, Activity activity) {
        return (Activity)m_map.put(key, activity);
    }
    
    /**
     * Removes a mapping from this ActivityMap. The parent map, if any,
     * is not effected by this method.
     * @param key the key of the mapping to remove
     */
    public void remove(Object key) {
        m_map.remove(key);
    }
    
    /**
     * Returns an array consisting of all the keys associated with this
     * map. This does not include any mappings in the parent map.
     * @return an array of all keys in this ActivityMap
     */
    public Object[] keys() {
        return m_map.keySet().toArray();
    }
    
    /**
     * Returns all keys in this ActivityMap, and in the parent map, and the
     * parent's parent, etc.
     * @return an array of all keys in this ActivityMap and its parents
     */
    public Object[] allKeys() {
        Object[] a1 = m_map.keySet().toArray();
        if ( m_parent != null ) {
            Object[] a2 = m_parent.allKeys();
            if ( a2 != null && a2.length > 0 ) {
                Object[] o = new Object[a1.length+a2.length];
                System.arraycopy(a1,0,o,0,a1.length);
                System.arraycopy(a2,0,o,a1.length,a2.length);
                return o;
            }
        }
        return a1;
    }
    
    /**
     * Sets this ActivityMap's parent. null values are legal, and
     * indicate this map has no parent.
     * @param parent the new parent for this map, or null for no parent
     */
    public void setParent(ActivityMap parent) {
        m_parent = parent;
    }
    
    /**
     * Returns this ActivityMap's parent map. This method return null if
     * this map has no parent.
     * @return this map's parent map
     */
    public ActivityMap getParent() {
        return m_parent;
    }
    
} // end of class ActivityMap
