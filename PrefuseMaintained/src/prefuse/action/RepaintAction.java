package prefuse.action;

import prefuse.Visualization;

/**
 * Issues a repaint request to a Visualization.
 *
 * @version 1.0
 * @author <a href="http://jheer.org">jeffrey heer</a>
 */
public class RepaintAction extends Action {

    /**
     * Create a new RepaintAction.
     */
    public RepaintAction() {
        super();
    }
    
    /**
     * Create a new RepaintAction.
     * @param vis the Visualization to repaint
     */
    public RepaintAction(Visualization vis) {
        super(vis);
    }
    
    /**
     * Calls the {@link prefuse.Visualization#repaint()} method on
     * this Action's associated Visualization.
     */
    public void run(double frac) {
        getVisualization().repaint();
    }

} // end of class RepaintAction
