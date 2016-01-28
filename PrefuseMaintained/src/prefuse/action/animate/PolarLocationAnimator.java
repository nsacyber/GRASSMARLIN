package prefuse.action.animate;

import java.awt.geom.Point2D;

import prefuse.Display;
import prefuse.action.ItemAction;
import prefuse.util.MathLib;
import prefuse.visual.VisualItem;


/**
 * Animator that interpolates between starting and ending display locations
 * by linearly interpolating between polar coordinates.
 * 
 * @author <a href="http://jheer.org">jeffrey heer</a>
 */
public class PolarLocationAnimator extends ItemAction {
    
    private Point2D m_anchor = new Point2D.Double();
    private String  m_linear = null;
    
    // temp variables
    private double ax, ay, sx, sy, ex, ey, x, y;
    private double dt1, dt2, sr, st, er, et, r, t, stt, ett;
    
    /**
     * Creates a PolarLocationAnimator that operates on all VisualItems
     * within a Visualization.
     */
    public PolarLocationAnimator() {
        super();
    }
    
    /**
     * Creates a PolarLocationAnimator that operates on VisualItems
     * within the specified group.
     * @param group the data group to process
     */
    public PolarLocationAnimator(String group) {
        super(group);
    }
    
    /**
     * Creates a PolarLocationAnimator that operates on VisualItems
     * within the specified group, while using regular linear interpolation
     * (in Cartesian (x,y) coordinates rather than polar coordinates) for
     * items also contained within the specified linearGroup. 
     * @param group the data group to process
     * @param linearGroup the group of items that should be interpolated
     * in Cartesian (standard x,y) coordinates rather than polar coordinates.
     * Note that this animator will not process any items in 
     * <code>linearGroup</code> that are not also in <code>group</code>.
     */
    public PolarLocationAnimator(String group, String linearGroup) {
        super(group);
        m_linear = linearGroup;
    }

    private void setAnchor() {
        Display d = getVisualization().getDisplay(0);
        m_anchor.setLocation(d.getWidth()/2,d.getHeight()/2);
        d.getAbsoluteCoordinate(m_anchor, m_anchor);
        ax = m_anchor.getX();
        ay = m_anchor.getY();
    }

    /**
     * @see prefuse.action.Action#run(double)
     */
    public void run(double frac) {
        setAnchor();
        super.run(frac);
    }
    
    /**
     * @see prefuse.action.ItemAction#process(prefuse.visual.VisualItem, double)
     */
    public void process(VisualItem item, double frac) {
        if ( m_linear != null && item.isInGroup(m_linear) ) {
            // perform linear interpolation instead
            double s = item.getStartX();
            item.setX(s + frac*(item.getEndX()-s));
            s = item.getStartY();
            item.setY(s + frac*(item.getEndY()-s));
            return;
        }
        
        // otherwise, interpolate in polar coordinates
        sx = item.getStartX() - ax;
        sy = item.getStartY() - ay;
        ex = item.getEndX() - ax;
        ey = item.getEndY() - ay;
            
        sr = Math.sqrt(sx*sx + sy*sy);
        st = Math.atan2(sy,sx);
            
        er = Math.sqrt(ex*ex + ey*ey);
        et = Math.atan2(ey,ex);
        
        stt = st < 0 ? st+MathLib.TWO_PI : st;
        ett = et < 0 ? et+MathLib.TWO_PI : et;
            
        dt1 = et - st;
        dt2 = ett - stt;
            
        if ( Math.abs(dt1) < Math.abs(dt2) ) {
            t = st + frac * dt1;
        } else {
            t = stt + frac * dt2;
        }
        r = sr + frac * (er - sr);
                        
        x = Math.round(ax + r*Math.cos(t));
        y = Math.round(ay + r*Math.sin(t));
            
        item.setX(x);
        item.setY(y);
    }

} // end of class PolarLocationAnimator
