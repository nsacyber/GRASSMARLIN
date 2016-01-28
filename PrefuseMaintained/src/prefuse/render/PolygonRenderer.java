package prefuse.render;

import java.awt.Shape;
import java.awt.geom.GeneralPath;

import prefuse.Constants;
import prefuse.data.Schema;
import prefuse.util.GraphicsLib;
import prefuse.visual.VisualItem;


/**
 * <p>Renderer for drawing a polygon, either as a closed shape, or as a
 * series of potentially unclosed curves. VisualItems must have a data field
 * containing an array of floats that tores the polyon. A {@link Float#NaN}
 * value can be used to mark the end point of the polygon for float arrays
 * larger than their contained points. By default, this renderer will
 * create closed paths, joining the first and last points in the point
 * array if necessary. The {@link #setClosePath(boolean)} method can be
 * used to render open paths, such as poly-lines or poly-curves.</p>
 * 
 * <p>A polygon edge type parameter (one of 
 * {@link prefuse.Constants#POLY_TYPE_LINE},
 * {@link prefuse.Constants#POLY_TYPE_CURVE}, or
 * {@link prefuse.Constants#POLY_TYPE_STACK}) determines how the
 * edges of the polygon are drawn. The LINE type result in a standard polygon,
 * with straight lines drawn between each sequential point. The CURVE type
 * causes the edges of the polygon to be interpolated as a cardinal spline,
 * giving a smooth blob-like appearance to the shape. The STACK type is similar
 * to the curve type except that straight line segments (not curves) are used
 * when the slope of the line between two adjacent points is zero or infinity.
 * This is useful for drawing stacks of data with otherwise curved edges.</p>
 *
 * @author <a href="http://jheer.org">jeffrey heer</a>
 */
public class PolygonRenderer extends AbstractShapeRenderer {

    /**
     * Default data field for storing polygon (float array) values.
     */
    public static final String POLYGON = "_polygon";
    /**
     * A Schema describing the polygon specification.
     */
    public static final Schema POLYGON_SCHEMA = new Schema();
    static {
        POLYGON_SCHEMA.addColumn(POLYGON, float[].class);
    }
    
    private int     m_polyType = Constants.POLY_TYPE_LINE;
    private float   m_slack = 0.08f;
    private float   m_epsilon = 0.1f;
    private boolean m_closed = true;
    private String  m_polyfield = POLYGON;
    
    private GeneralPath m_path = new GeneralPath();
    
    /**
     * Create a new PolygonRenderer supporting straight lines.
     */
    public PolygonRenderer() {
        this(Constants.EDGE_TYPE_LINE);
    }
    
    /**
     * Create a new PolygonRenderer.
     * @param polyType the polygon edge type, one of
     * {@link prefuse.Constants#POLY_TYPE_LINE},
     * {@link prefuse.Constants#POLY_TYPE_CURVE}, or
     * {@link prefuse.Constants#POLY_TYPE_STACK}).
     */
    public PolygonRenderer(int polyType) {
        m_polyType = polyType;
    }

    /**
     * Get the polygon line type.
     * @return the polygon edge type, one of
     * {@link prefuse.Constants#POLY_TYPE_LINE},
     * {@link prefuse.Constants#POLY_TYPE_CURVE}, or
     * {@link prefuse.Constants#POLY_TYPE_STACK}).
     */
    public int getPolyType() {
        return m_polyType;
    }
    
    /**
     * Set the polygon line type.
     * @param polyType the polygon edge type, one of
     * {@link prefuse.Constants#POLY_TYPE_LINE},
     * {@link prefuse.Constants#POLY_TYPE_CURVE}, or
     * {@link prefuse.Constants#POLY_TYPE_STACK}).
     */
    public void setPolyType(int polyType) {
        if ( polyType < 0 || polyType >= Constants.POLY_TYPE_COUNT ) {
            throw new IllegalArgumentException("Unknown edge type: "+polyType);
        }
        m_polyType = polyType;
    }
    
    /**
     * Indicates if this renderer uses a closed or open path. If true,
     * the renderer will draw closed polygons, if false, the renderer will
     * draw poly-lines or poly-curves.
     * @return true if paths are closed, false otherwise.
     */
    public boolean isClosePath() {
        return m_closed;
    }

    /**
     * Sets if this renderer uses a closed or open path. If true,
     * the renderer will draw closed polygons, if false, the renderer will
     * @param closePath true to close paths, false otherwise.
     */
    public void setClosePath(boolean closePath) {
        m_closed = closePath;
    }

    /**
     * Gets the slack parameter for curved lines. The slack parameter
     * determines how tightly the curves are string between the points
     * of the polygon. A value of zero results in straight lines. Values
     * near 0.1 (0.08 is the default) typically result in visible curvature
     * that still follows the polygon boundary nicely.
     * @return the curve slack parameter
     */
    public float getCurveSlack() {
        return m_slack;
    }

    /**
     * Sets the slack parameter for curved lines. The slack parameter
     * determines how tightly the curves are string between the points
     * of the polygon. A value of zero results in straight lines. Values
     * near 0.1 (0.08 is the default) typically results in visible curvature
     * that still follows the polygon boundary nicely.
     * @param slack the curve slack parameter to use
     */
    public void setCurveSlack(float slack) {
        m_slack = slack;
    }

    /**
     * @see prefuse.render.AbstractShapeRenderer#getRawShape(prefuse.visual.VisualItem)
     */
    protected Shape getRawShape(VisualItem item) {
        float[] poly = (float[])item.get(m_polyfield);
        if ( poly == null ) { return null; }
        
        float x = (float)item.getX();
        float y = (float)item.getY();
        
        // initialize the path
        m_path.reset();
        m_path.moveTo(x+poly[0],y+poly[1]);
        
        if ( m_polyType == Constants.POLY_TYPE_LINE )
        {
            // create a polygon
            for ( int i=2; i<poly.length; i+=2 ) {
                if ( Float.isNaN(poly[i]) ) break;
                m_path.lineTo(x+poly[i],y+poly[i+1]);
            }
        }
        else if ( m_polyType == Constants.POLY_TYPE_CURVE )
        {
            // interpolate the polygon points with a cardinal spline
            return GraphicsLib.cardinalSpline(m_path, poly, 
                    m_slack, m_closed, x, y);
        }
        else if ( m_polyType == Constants.POLY_TYPE_STACK )
        {
            // used curved lines, except for non-sloping segments
            return GraphicsLib.stackSpline(m_path, poly, 
                    m_epsilon, m_slack, m_closed, x, y);
        }
        if ( m_closed ) m_path.closePath();
        return m_path;
    }

} // end of class PolygonRenderer
