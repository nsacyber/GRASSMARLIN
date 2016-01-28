package prefuse.action.distortion;

import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;


/**
 * <p>
 * Computes a graphical fisheye distortion of a graph view. This distortion 
 * allocates more space to items near the layout anchor and less space to 
 * items further away, magnifying space near the anchor and demagnifying 
 * distant space in a continuous fashion.
 * </p>
 * 
 * <p>
 * For more details on this form of transformation, see Manojit Sarkar and 
 * Marc H. Brown, "Graphical Fisheye Views of Graphs", in Proceedings of 
 * CHI'92, Human Factors in Computing Systems, p. 83-91, 1992. Available
 * online at <a href="http://citeseer.ist.psu.edu/sarkar92graphical.html">
 * http://citeseer.ist.psu.edu/sarkar92graphical.html</a>. 
 * </p>
 *
 * @author <a href="http://jheer.org">jeffrey heer</a>
 */
public class FisheyeDistortion extends Distortion {

    private double  dx, dy;   // distortion factors
    private double  sz = 3.0; // size factor
    
    /**
     * Create a new FisheyeDistortion with default distortion factor.
     */
    public FisheyeDistortion() {
        this(4);
    }
    
    /**
     * Create a new FisheyeDistortion with the given distortion factor
     * for use along both the x and y directions.
     * @param dfactor the distortion factor (same for both axes)
     */
    public FisheyeDistortion(double dfactor) {
        this(dfactor, dfactor);
    }
    
    /**
     * Create a new FisheyeDistortion with the given distortion factors
     * along the x and y directions.
     * @param xfactor the distortion factor along the x axis
     * @param yfactor the distortion factor along the y axis
     */
    public FisheyeDistortion(double xfactor, double yfactor) {
        super();
        dx = xfactor;
        dy = yfactor;
        m_distortX = dx > 0;
        m_distortY = dy > 0;
    }
    
    /**
     * Returns the distortion factor for the x-axis.
     * @return returns the distortion factor for the x-axis.
     */
    public double getXDistortionFactor() {
        return dx;
    }

    /**
     * Sets the distortion factor for the x-axis.
     * @param d The distortion factor to set.
     */
    public void setXDistortionFactor(double d) {
        dx = d;
        m_distortX = dx > 0;
    }
    
    /**
     * Returns the distortion factor for the y-axis.
     * @return returns the distortion factor for the y-axis.
     */
    public double getYDistortionFactor() {
        return dy;
    }

    /**
     * Sets the distortion factor for the y-axis.
     * @param d The distortion factor to set.
     */
    public void setYDistortionFactor(double d) {
        dy = d;
        m_distortY = dy > 0;
    }
    
    /**
     * @see prefuse.action.distortion.Distortion#distortX(double, java.awt.geom.Point2D, java.awt.geom.Rectangle2D)
     */
    protected double distortX(double x, Point2D anchor, Rectangle2D bounds) {
        return fisheye(x,anchor.getX(),dx,bounds.getMinX(),bounds.getMaxX());
    }
    
    /**
     * @see prefuse.action.distortion.Distortion#distortY(double, java.awt.geom.Point2D, java.awt.geom.Rectangle2D)
     */
    protected double distortY(double y, Point2D anchor, Rectangle2D bounds) {
        return fisheye(y,anchor.getY(),dy,bounds.getMinY(),bounds.getMaxY());
    }
    
    /**
     * @see prefuse.action.distortion.Distortion#distortSize(java.awt.geom.Rectangle2D, double, double, java.awt.geom.Point2D, java.awt.geom.Rectangle2D)
     */
    protected double distortSize(Rectangle2D bbox, double x, double y, 
            Point2D anchor, Rectangle2D bounds)
    { 
        if ( !m_distortX && !m_distortY ) return 1.;
        double fx=1, fy=1;

        if ( m_distortX ) {
            double ax = anchor.getX();
            double minX = bbox.getMinX(), maxX = bbox.getMaxX();
            double xx = (Math.abs(minX-ax) > Math.abs(maxX-ax) ? minX : maxX);
            if ( xx < bounds.getMinX() || xx > bounds.getMaxX() )
                xx = (xx==minX ? maxX : minX);
            fx = fisheye(xx,ax,dx,bounds.getMinX(),bounds.getMaxX());
            fx = Math.abs(x-fx)/bbox.getWidth();
        }

        if ( m_distortY ) {
            double ay = anchor.getY();
            double minY = bbox.getMinY(), maxY = bbox.getMaxY();
            double yy = (Math.abs(minY-ay) > Math.abs(maxY-ay) ? minY : maxY);
            if ( yy < bounds.getMinY() || yy > bounds.getMaxY() )
                yy = (yy==minY ? maxY : minY);
            fy = fisheye(yy,ay,dy,bounds.getMinY(),bounds.getMaxY());
            fy = Math.abs(y-fy)/bbox.getHeight();
        }
        
        double sf = (!m_distortY ? fx : (!m_distortX ? fy : Math.min(fx,fy)));
        if (Double.isInfinite(sf) || Double.isNaN(sf)) {
            return 1.;
        } else {
            return sz*sf;
        }
    }
    
    private double fisheye(double x, double a, double d, double min, double max) {
        if ( d != 0 ) {
            boolean left = x<a;
            double v, m = (left ? a-min : max-a);
            if ( m == 0 ) m = max-min;
            v = Math.abs(x - a) / m;
            v = (d+1)/(d+(1/v));
            return (left?-1:1)*m*v + a;
        } else {
            return x;
        }
    }
    
} // end of class FisheyeDistortion
