package prefuse.controls;

import java.awt.geom.Point2D;

import prefuse.Display;

/**
 * Abstract base class for zoom controls.
 * 
 * @author <a href="http://jheer.org">jeffrey heer</a>
 */
public class AbstractZoomControl extends ControlAdapter {

    public static final double DEFAULT_MIN_SCALE = 1E-3;
    public static final double DEFAULT_MAX_SCALE = 75;
    
    /** Indicates a zoom operation completed successfully. */
    protected static final int ZOOM = 0;
    /** Indicates the minimum allowed zoom level has been reached. */
    protected static final int MIN_ZOOM = 1;
    /** Indicates the maximum allowed zoom level has been reached. */
    protected static final int MAX_ZOOM = 2;
    /** Indicates no zooming can be performed. This is often due to a
     *  transformation activity in progress. */
    protected static final int NO_ZOOM = 3;
    
    protected double m_minScale = DEFAULT_MIN_SCALE;
    protected double m_maxScale = DEFAULT_MAX_SCALE;
    protected boolean m_zoomOverItem = true;
    
    /**
     * Zoom the given display at the given point by the zoom factor,
     * in either absolute (item-space) or screen co-ordinates.
     * @param display the Display to zoom
     * @param p the point to center the zoom upon
     * @param zoom the scale factor by which to zoom
     * @param abs if true, the point p should be assumed to be in absolute
     * coordinates, otherwise it will be treated as scree (pixel) coordinates
     * @return a return code indicating the status of the zoom operation.
     * One of {@link #ZOOM}, {@link #NO_ZOOM}, {@link #MIN_ZOOM},
     * {@link #MAX_ZOOM}.
     */
    protected int zoom(Display display, Point2D p, double zoom, boolean abs) {
        if ( display.isTranformInProgress() )
            return NO_ZOOM;
        
        double scale = display.getScale();
        double result = scale * zoom;
        int status = ZOOM;

        if ( result < m_minScale ) {
            zoom = m_minScale/scale;
            status = MIN_ZOOM;
        } else if ( result > m_maxScale ) {
            zoom = m_maxScale/scale;
            status = MAX_ZOOM;
        }       
        
        if ( abs )
            display.zoomAbs(p,zoom);
        else
            display.zoom(p,zoom);
        display.repaint();
        return status;
    }
    
    /**
     * Gets the maximum scale value allowed by this zoom control
     * @return the maximum scale value 
     */
    public double getMaxScale() {
        return m_maxScale;
    }
    
    /**
     * Sets the maximum scale value allowed by this zoom control
     * @return the maximum scale value 
     */
    public void setMaxScale(double maxScale) {
        this.m_maxScale = maxScale;
    }
    
    /**
     * Gets the minimum scale value allowed by this zoom control
     * @return the minimum scale value 
     */
    public double getMinScale() {
        return m_minScale;
    }
    
    /**
     * Sets the minimum scale value allowed by this zoom control
     * @return the minimum scale value 
     */
    public void setMinScale(double minScale) {
        this.m_minScale = minScale;
    }

    /**
     * Indicates if the zoom control will work while the mouse is
     * over a VisualItem.
     * @return true if the control still operates over a VisualItem
     */
    public boolean isZoomOverItem() {
        return m_zoomOverItem;
    }

    /**
     * Determines if the zoom control will work while the mouse is
     * over a VisualItem
     * @param zoomOverItem true to indicate the control operates
     * over VisualItems, false otherwise
     */
    public void setZoomOverItem(boolean zoomOverItem) {
        this.m_zoomOverItem = zoomOverItem;
    }
    
} // end of class AbstractZoomControl
