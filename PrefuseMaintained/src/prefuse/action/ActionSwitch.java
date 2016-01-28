package prefuse.action;

/**
 * The ActionSwitch selects between a set of Actions, allowing only one
 * of a group of Actions to be executed at a time.
 *
 * @version 1.0
 * @author <a href="http://jheer.org">jeffrey heer</a>
 */
public class ActionSwitch extends CompositeAction {

    private int m_switchVal;
    
    /**
     * Creates an empty action switch.
     */
    public ActionSwitch() {
        m_switchVal = 0;
    }
    
    /**
     * Creates a new ActionSwitch with the given actions and switch value.
     * @param acts the Actions to include in this switch
     * @param switchVal the switch value indicating which Action to run
     */
    public ActionSwitch(Action[] acts, int switchVal) {
        for ( int i=0; i<acts.length; i++ )
            m_actions.add(acts[i]);
        setSwitchValue(switchVal);
    }
    
    /**
     * @see prefuse.action.Action#run(double)
     */
    public void run(double frac) {
        if ( m_actions.size() > 0 ) {
            get(getSwitchValue()).run(frac);
        }
    }
    
    /**
     * Returns the current switch value, indicating the index of the Action
     * that will be executed in reponse to run() invocations.
     * @return the switch value
     */
    public int getSwitchValue() {
        return m_switchVal;
    }
    
    /**
     * Set the switch value. This is the index of the Action that will be
     * executed in response to run() invocations.
     * @param s the new switch value
     */
    public void setSwitchValue(int s) {
        if ( s < 0 || s >= size() )
            throw new IllegalArgumentException(
                    "Switch value out of legal range");
        m_switchVal = s;
    }

} // end of class ActionSwitch
