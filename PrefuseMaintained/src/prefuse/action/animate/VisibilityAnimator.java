package prefuse.action.animate;

import java.util.Iterator;

import prefuse.action.GroupAction;
import prefuse.util.ColorLib;
import prefuse.visual.VisualItem;
import prefuse.visual.expression.StartVisiblePredicate;

/**
 * Animator that interpolates the visibility status of VisualItems. Items
 * not currently visible but with end visibility true are faded in, while
 * items currently visible but with end visibility false are faded out and
 * finally set to not visible.
 * 
 * @author <a href="http://jheer.org">jeffrey heer</a>
 */
public class VisibilityAnimator extends GroupAction {
    
    /**
     * Create a new VisibilityAnimator that processes all data groups.
     */
    public VisibilityAnimator() {
        super();
    }
    
    /**
     * Create a new VisibilityAnimator that processes the specified group.
     * @param group the data group to process.
     */
    public VisibilityAnimator(String group) {
        super(group);
    }
    
    /**
     * @see prefuse.action.GroupAction#run(double)
     */
    public void run(double frac) {
        if ( frac == 0.0 ) {
            setup();
        } else if ( frac == 1.0 ) {
            finish();
        }
    }
    
    private void setup() {
        // handle fade-in nodes
        Iterator items = m_vis.visibleItems(m_group);
        while ( items.hasNext() ) {
            VisualItem item = (VisualItem) items.next();
            if ( !item.isStartVisible() ) {
                item.setStartFillColor(
                        ColorLib.setAlpha(item.getEndFillColor(),0));
                item.setStartStrokeColor(
                        ColorLib.setAlpha(item.getEndStrokeColor(),0));
                item.setStartTextColor(
                        ColorLib.setAlpha(item.getEndTextColor(),0));
            }
        }
        
        // handle fade-out nodes
        items = m_vis.items(m_group, StartVisiblePredicate.TRUE);
        while ( items.hasNext() ) {
            VisualItem item = (VisualItem) items.next();
            if ( !item.isEndVisible() ) {
                // fade-out case
                item.setVisible(true);
                item.setEndFillColor(
                        ColorLib.setAlpha(item.getStartFillColor(),0));
                item.setEndStrokeColor(
                        ColorLib.setAlpha(item.getStartStrokeColor(),0));
                item.setEndTextColor(
                        ColorLib.setAlpha(item.getStartTextColor(),0));
            }
        }
    }
    
    private void finish() {
        // set faded-out nodes to permanently invisible
        Iterator items = m_vis.items(m_group, StartVisiblePredicate.TRUE);
        while ( items.hasNext() ) {
            VisualItem item = (VisualItem) items.next();
            if ( !item.isEndVisible() ) {
                item.setVisible(false);
                item.setStartVisible(false);
            }
        }
        
        // set faded-in nodes to permanently visible
        items = m_vis.visibleItems(m_group);
        while ( items.hasNext() ) {
            VisualItem item = (VisualItem) items.next();
            if ( !item.isStartVisible() ) {
                item.setStartVisible(true);
                item.setStartFillColor(item.getEndFillColor());
                item.setStartTextColor(item.getEndTextColor());
                item.setStartStrokeColor(item.getEndStrokeColor());
            }
        }
    }

} // end of class VisibilityAnimator
