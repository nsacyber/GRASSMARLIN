package prefuse.util.display;

import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.util.logging.Logger;

/**
 * Represents a clipping rectangle in a prefuse <code>Display</code>.
 *
 * @author <a href="http://jheer.org">jeffrey heer</a>
 */
public class Clip {
    
    private static final byte EMPTY   = 0;
    private static final byte INUSE   = 1;
    private static final byte INVALID = 2;
    
    private double[] clip = new double[8];
    private byte status = INVALID;
    
    /**
     * Reset the clip to an empty status.
     */
    public void reset() {
        status = EMPTY;
    }
    
    /**
     * Invalidate the clip. In this state, the clip contents have no meaning.
     */
    public void invalidate() {
        status = INVALID;
    }
       
    /**
     * Set the clip contents, and set the status to valid and in use.
     * @param c the clip whose contents should be copied
     */
    public void setClip(Clip c) {
        status = INUSE;
        System.arraycopy(c.clip, 0, clip, 0, clip.length);
    }
    
    /**
     * Set the clip contents, and set the status to valid and in use.
     * @param r the clip contents to copy
     */
    public void setClip(Rectangle2D r) {
        setClip(r.getX(),r.getY(),r.getWidth(),r.getHeight());
    }
    
    /**
     * Set the clip contents, and set the status to valid and in use.
     * @param x the minimum x-coordinate
     * @param y the minimum y-coorindate
     * @param w the clip width
     * @param h the clip height
     */
    public void setClip(double x, double y, double w, double h) {
        status = INUSE;
        clip[0] = x;
        clip[1] = y;
        clip[6] = x+w;
        clip[7] = y+h;
    }
    
    /**
     * Transform the clip contents. A new clip region will be created
     * which is the bounding box of the transformed region.
     * @param at the affine transform
     */
    public void transform(AffineTransform at) {
        // make the extra corner points valid
        clip[2] = clip[0]; clip[3] = clip[7];
        clip[4] = clip[6]; clip[5] = clip[1];
        
        // transform the points
        at.transform(clip,0,clip,0,4);
        
        // make safe against rotation
        double xmin = clip[0], ymin = clip[1];
        double xmax = clip[6], ymax = clip[7];
        for ( int i=0; i<7; i+=2 ) {
            if ( clip[i] < xmin )
                xmin = clip[i];
            if ( clip[i] > xmax )
                xmax = clip[i];
            if ( clip[i+1] < ymin )
                ymin = clip[i+1];
            if ( clip[i+1] > ymax )
                ymax = clip[i+1];
        }
        clip[0] = xmin; clip[1] = ymin;
        clip[6] = xmax; clip[7] = ymax;
    }
    
    /**
     * Limit the clip such that it fits within the specified region.
     * @param x1 the minimum x-coordinate
     * @param y1 the minimum y-coorindate
     * @param x2 the maximum x-coordinate
     * @param y2 the maximum y-coorindate
     */
    public void limit(double x1, double y1, double x2, double y2) {
        clip[0] = Math.max(clip[0],x1);
        clip[1] = Math.max(clip[1],y1);
        clip[6] = Math.min(clip[6],x2);
        clip[7] = Math.min(clip[7],y2);
    }
    
    /**
     * Indicates if this Clip intersects the given rectangle expanded
     * by the additional margin pace.
     * @param r the rectangle to test for intersect
     * @param margin additional margin "bleed" to include in the intersection
     * @return true if the clip intersects the expanded region, false otherwise
     */
    public boolean intersects(Rectangle2D r, double margin) {
        double tw = clip[6]-clip[0];
        double th = clip[7]-clip[1];
        double rw = r.getWidth();
        double rh = r.getHeight();
        if (rw < 0 || rh < 0 || tw < 0 || th < 0) {
            return false;
        }
        double tx = clip[0];
        double ty = clip[1];
        double rx = r.getX()-margin;
        double ry = r.getY()-margin;
        rw += rx+2*margin;
        rh += ry+2*margin;
        tw += tx;
        th += ty;
        //      overflow || intersect
        return ((rw < rx || rw > tx) &&
                (rh < ry || rh > ty) &&
                (tw < tx || tw > rx) &&
                (th < ty || th > ry));
    }
      
    /**
     * Union this clip with another clip. As a result, this clip
     * will become a bounding box around the two original clips.
     * @param c the clip to union with
     */
    public void union(Clip c) {
        if ( status == INVALID )
            return;
        if ( status == EMPTY ) {
            setClip(c);
            status = INUSE;
            return;
        }
        clip[0] = Math.min(clip[0], c.clip[0]);
        clip[1] = Math.min(clip[1], c.clip[1]);
        clip[6] = Math.max(clip[6], c.clip[6]);
        clip[7] = Math.max(clip[7], c.clip[7]);
    }
    
    /**
     * Union this clip with another region. As a result, this clip
     * will become a bounding box around the two original regions.
     * @param r the rectangle to union with
     */
    public void union(Rectangle2D r) {
        if ( status == INVALID )
            return;
        
        double minx = r.getMinX();
        double miny = r.getMinY();
        double maxx = r.getMaxX();
        double maxy = r.getMaxY();
        
        if ( Double.isNaN(minx) || Double.isNaN(miny) ||
             Double.isNaN(maxx) || Double.isNaN(maxy) ) {
            Logger.getLogger(getClass().getName()).warning(
                "Union with invalid clip region: "+r);
            return;
        }
        
        if ( status == EMPTY ) {
            setClip(r);
            status = INUSE;
            return;
        }
        clip[0] = Math.min(clip[0], minx);
        clip[1] = Math.min(clip[1], miny);
        clip[6] = Math.max(clip[6], maxx);
        clip[7] = Math.max(clip[7], maxy);
    }
    
    /**
     * Union this clip with another region. As a result, this clip
     * will become a bounding box around the two original regions.
     * @param x the x-coordinate of the region to union with
     * @param y the y-coordinate of the region to union with
     * @param w the width of the region to union with
     * @param h the height of the region to union with
     */
    public void union(double x, double y, double w, double h) {
        if ( status == INVALID )
            return;
        if ( status == EMPTY ) {
            setClip(x,y,w,h);
            status = INUSE;
            return;
        }
        clip[0] = Math.min(clip[0], x);
        clip[1] = Math.min(clip[1], y);
        clip[6] = Math.max(clip[6], x+w);
        clip[7] = Math.max(clip[7], y+h);
    }
    
    /**
     * Intersect this clip with another region. As a result, this
     * clip will become the intersecting area of the two regions.
     * @param c the clip to intersect with
     */
    public void intersection(Clip c) {
        if ( status == INVALID )
            return;
        if ( status == EMPTY ) {
            setClip(c);
            status = INUSE;
            return;
        }
        clip[0] = Math.max(clip[0], c.clip[0]);
        clip[1] = Math.max(clip[1], c.clip[1]);
        clip[6] = Math.min(clip[6], c.clip[6]);
        clip[7] = Math.min(clip[7], c.clip[7]);
    }
    
    /**
     * Intersect this clip with another region. As a result, this
     * clip will become the intersecting area of the two regions.
     * @param r the rectangle to intersect with
     */
    public void intersection(Rectangle2D r) {
        if ( status == INVALID )
            return;
        if ( status == EMPTY ) {
            setClip(r);
            status = INUSE;
            return;
        }
        clip[0] = Math.max(clip[0], r.getMinX());
        clip[1] = Math.max(clip[1], r.getMinY());
        clip[6] = Math.min(clip[6], r.getMaxX());
        clip[7] = Math.min(clip[7], r.getMaxY());
    }
    
    /**
     * Intersect this clip with another region. As a result, this
     * clip will become the intersecting area of the two regions.
     * @param x the x-coordinate of the region to intersect with
     * @param y the y-coordinate of the region to intersect with
     * @param w the width of the region to intersect with
     * @param h the height of the region to intersect with
     */
    public void intersection(double x, double y, double w, double h) {
        if ( status == INVALID )
            return;
        if ( status == EMPTY ) {
            setClip(x,y,w,h);
            status = INUSE;
            return;
        }
        clip[0] = Math.max(clip[0], x);
        clip[1] = Math.max(clip[1], y);
        clip[6] = Math.min(clip[6], x+w);
        clip[7] = Math.min(clip[7], y+h);
    }
    
    /**
     * Minimally expand the clip such that each coordinate is an integer.
     */
    public void expandToIntegerLimits() {
        clip[0] = Math.floor(clip[0]);
        clip[1] = Math.floor(clip[1]);
        clip[6] = Math.ceil(clip[6]);
        clip[7] = Math.ceil(clip[7]);
    }
    
    /**
     * Expand the clip in all directions by the given value.
     * @param b the value to expand by
     */
    public void expand(double b) {
        clip[0] -= b; clip[1] -= b;
        clip[6] += b; clip[7] += b;
    }

    /**
     * Grow the clip width and height by the given value. The minimum
     * coordinates will be unchanged.
     * @param b the value to grow the width and height by
     */
    public void grow(double b) {
        clip[6] += b; clip[7] += b;
    }
    
    /**
     * Get the minimum x-coordinate.
     * @return the minimum x-coordinate
     */
    public double getMinX() {
        return clip[0];
    }
    
    /**
     * Get the minimum y-coordinate.
     * @return the minimum y-coordinate
     */
    public double getMinY() {
        return clip[1];
    }
    
    /**
     * Get the maximum x-coordinate.
     * @return the maximum x-coordinate
     */
    public double getMaxX() {
        return clip[6];
    }
    
    /**
     * Get the maximum y-coordinate.
     * @return the maximum y-coordinate
     */
    public double getMaxY() {
        return clip[7];
    }
    
    /**
     * Get the clip's width
     * @return the clip width
     */
    public double getWidth() {
        return clip[6]-clip[0];
    }

    /**
     * Get the clip's height
     * @return the clip height
     */
    public double getHeight() {
        return clip[7]-clip[1];
    }
    
    /**
     * Indicates if the clip is set to an empty status.
     * @return true if the clip is set to empty, false otherwise
     */
    public boolean isEmpty() {
        return status==EMPTY;
    }
    
    /**
     * Indicates if the clip is set to an invalid status.
     * @return true if the clip is set to invalid, false otherwise
     */
    public boolean isInvalid() {
        return status==INVALID;
    }
    
    // ------------------------------------------------------------------------
    
    /**
     * @see java.lang.Object#equals(java.lang.Object)
     */
    public boolean equals(Object o) {
        if ( o instanceof Rectangle2D ) {
            Rectangle2D r = (Rectangle2D)o;
            return ( r.getMinX()==clip[0] && r.getMinY()==clip[1] &&
                     r.getMaxX()==clip[6] && r.getMaxY()==clip[7] );
        } else if ( o instanceof Clip ) {
            Clip r = (Clip)o;
            if ( r.status == status ) {
                if ( status == Clip.INUSE )
                    return ( r.clip[0]==clip[0] && r.clip[1]==clip[1] &&
                            r.clip[6]==clip[6] && r.clip[7]==clip[7] );
                else
                    return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }
    
    /**
     * @see java.lang.Object#toString()
     */
    public String toString() {
        StringBuffer sb = new StringBuffer(20);
        sb.append("Clip[");
        switch (status) {
        case INVALID:
            sb.append("invalid");
            break;
        case EMPTY:
            sb.append("empty");
            break;
        default:
            sb.append(clip[0]).append(",");
            sb.append(clip[1]).append(",");
            sb.append(clip[6]).append(",");
            sb.append(clip[7]);
        }
        sb.append("]");
        return sb.toString();
    }
    
} // end of class Clip
