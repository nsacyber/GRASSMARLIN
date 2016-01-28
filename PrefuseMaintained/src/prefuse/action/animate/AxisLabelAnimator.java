package prefuse.action.animate;

import java.util.Iterator;

import prefuse.action.ItemAction;
import prefuse.action.layout.AxisLabelLayout;
import prefuse.data.tuple.TupleSet;
import prefuse.util.ColorLib;
import prefuse.visual.VisualItem;
import prefuse.visual.expression.StartVisiblePredicate;


/**
 * Animator that interpolates positions, colors, and visibility status for
 * metric axes.
 * 
 * @author <a href="http://jheer.org">jeffrey heer</a>
 * @see prefuse.action.layout.AxisLabelLayout
 */
public class AxisLabelAnimator extends ItemAction {

    /**
     * Create a new AxisLabelAnimator.
     */
    protected AxisLabelAnimator() {
        super();
    }    

    /**
     * Create a new AxisLabelAnimator for the given data group.
     * @param group the data group to process
     */
    public AxisLabelAnimator(String group) {
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
        } else {
            super.run(frac);
        }
        TupleSet ts = m_vis.getGroup(m_group);
        ts.putClientProperty(AxisLabelLayout.FRAC, new Double(frac));
    }
    
    private void setup() {
        // handle fade-in nodes
        Iterator items = m_vis.visibleItems(m_group);
        while ( items.hasNext() ) {
            VisualItem item = (VisualItem) items.next();
            if ( !item.isStartVisible() ) {
                int efc = item.getEndFillColor();
                int esc = item.getEndStrokeColor();
                int etc = item.getEndTextColor();
                item.setStartFillColor(ColorLib.setAlpha(efc,0));
                item.setStartStrokeColor(ColorLib.setAlpha(esc,0));
                item.setStartTextColor(ColorLib.setAlpha(etc,0));
                item.setStartVisible(true);
            }
            process(item, 0.0);
        }
        
        // handle fade-out nodes
        items = m_vis.items(m_group, StartVisiblePredicate.TRUE);
        while ( items.hasNext() ) {
            VisualItem item = (VisualItem) items.next();
            if ( !item.isEndVisible() ) {
                int sfc = item.getStartFillColor();
                int ssc = item.getStartStrokeColor();
                int stc = item.getStartTextColor();
                item.setEndFillColor(ColorLib.setAlpha(sfc,0));
                item.setEndStrokeColor(ColorLib.setAlpha(ssc,0));
                item.setEndTextColor(ColorLib.setAlpha(stc,0));
                item.setVisible(true);
                process(item, 0.0);
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
            process(item, 1.0);
            item.setStartFillColor(item.getEndFillColor());
            item.setStartTextColor(item.getEndTextColor());
            item.setStartStrokeColor(item.getEndStrokeColor());
        }
    }
    
    /**
     * @see prefuse.action.ItemAction#process(prefuse.visual.VisualItem, double)
     */
    public void process(VisualItem item, double frac) {
        double v = item.getStartX();
        item.setX(v + frac*(item.getEndX()-v));
        v = item.getStartY();
        item.setY(v + frac*(item.getEndY()-v));
        v = item.getDouble(VisualItem.STARTX2);
        v = v + frac*(item.getDouble(VisualItem.ENDX2)-v);
        item.setDouble(VisualItem.X2, v);
        v = item.getDouble(VisualItem.STARTY2);
        v = v + frac*(item.getDouble(VisualItem.ENDY2)-v);
        item.setDouble(VisualItem.Y2, v);
        
        int c = ColorLib.interp(item.getStartStrokeColor(),
                item.getEndStrokeColor(), frac);
        item.setStrokeColor(c);
        
        int tc = ColorLib.interp(item.getStartTextColor(),
                item.getEndTextColor(), frac);
        item.setTextColor(tc);
        
        int fc = ColorLib.interp(item.getStartFillColor(),
                item.getEndFillColor(), frac);
        item.setFillColor(fc);
    }

} // end of class AxisLabelAnimator
