package prefuse;

/**
 * Constants used throughout the prefuse toolkit.
 * 
 * @author <a href="http://jheer.org">jeffrey heer</a>
 */
public interface Constants {

    /** A left-to-right layout orientation */
    public static final int ORIENT_LEFT_RIGHT = 0;
    /** A right-to-left layout orientation */
    public static final int ORIENT_RIGHT_LEFT = 1;
    /** A top-to-bottom layout orientation */
    public static final int ORIENT_TOP_BOTTOM = 2;
    /** A bottom-to-top layout orientation */
    public static final int ORIENT_BOTTOM_TOP = 3;
    /** A centered layout orientation */
    public static final int ORIENT_CENTER     = 4;
    /** The total number of orientation values */
    public static final int ORIENTATION_COUNT = 5;
    
    /** A left alignment */
    public static final int LEFT   = 0;
    /** A right alignment */
    public static final int RIGHT  = 1;
    /** A center alignment */
    public static final int CENTER = 2;
    /** A bottom alignment */
    public static final int BOTTOM = 3;
    /** A top alignment */
    public static final int TOP    = 4;
    
    /** A left alignment, outside of bounds */
    public static final int FAR_LEFT = 5;
    /** A right alignment, outside of bounds */
    public static final int FAR_RIGHT = 6;
    /** A bottom alignment, outside of bounds */
    public static final int FAR_BOTTOM = 7;
    /** A top alignment, outside of bounds */
    public static final int FAR_TOP = 8;
    
    /** A straight-line edge type */
    public static final int EDGE_TYPE_LINE  = 0;
    /** A curved-line edge type */
    public static final int EDGE_TYPE_CURVE = 1;
    /** The total number of edge type values */
    public static final int EDGE_TYPE_COUNT = 2;
    
    /** No arrows on edges */
    public static final int EDGE_ARROW_NONE = 0;
    /** Arrows on edges pointing from source to target */
    public static final int EDGE_ARROW_FORWARD = 1;
    /** Arrows on edges pointing from target to source */
    public static final int EDGE_ARROW_REVERSE = 2;
    /** The total number of edge arrow type values */
    public static final int EDGE_ARROW_COUNT = 3;
    
    /** Use straight-lines for polygon edges */
    public static final int POLY_TYPE_LINE  = EDGE_TYPE_LINE;
    /** Use curved-lines for polygon edges */
    public static final int POLY_TYPE_CURVE = EDGE_TYPE_CURVE;
    /** Use curved-lines for polygon edges, 
     * but use straight lines for zero-slope edges */
    public static final int POLY_TYPE_STACK = 2;
    /** The total number of polygon type values */
    public static final int POLY_TYPE_COUNT = 3;
    
    /** A linear scale */
    public static final int LINEAR_SCALE   = 0;
    /** A logarithmic (base 10) scale */
    public static final int LOG_SCALE      = 1;
    /** A square root scale */
    public static final int SQRT_SCALE     = 2;
    /** A quantile scale, based on the underlying distribution */
    public static final int QUANTILE_SCALE = 3;
    /** The total number of scale type values */
    public static final int SCALE_COUNT    = 4;
    
    /** An unknown data type */
    public static final int UNKNOWN = -1;
    /** A nominal (categorical) data type */
    public static final int NOMINAL = 0;
    /** An ordinal (ordered) data type */
    public static final int ORDINAL = 1;
    /** A numerical (quantitative) data type */
    public static final int NUMERICAL = 2;
    /** The total number of data type values */
    public static final int DATATYPE_COUNT = 3;
    
    /** Indicates the horizontal (X) axis */
    public static final int X_AXIS = 0;
    /** Indicates the vertical (Y) axis */
    public static final int Y_AXIS = 1;
    /** The total number of axis type values */
    public static final int AXIS_COUNT = 2;
    
    public static final int NODE_TRAVERSAL = 0;
    public static final int EDGE_TRAVERSAL = 1;
    public static final int NODE_AND_EDGE_TRAVERSAL = 2;
    /** The total number of traversal type values */
    public static final int TRAVERSAL_COUNT = 3;
    
    /** Indicates a continuous (non-discrete) spectrum */
    public static final int CONTINUOUS = -1;
    
    /** The absolute minimum degree-of-interest (DOI) value */
    public static final double MINIMUM_DOI = -Double.MAX_VALUE;
    
    /** No shape. Draw nothing. */
    public static final int SHAPE_NONE           = -1;
    /** Rectangle/Square shape */
    public static final int SHAPE_RECTANGLE      = 0;
    /** Ellipse/Circle shape */
    public static final int SHAPE_ELLIPSE        = 1;
    /** Diamond shape */
    public static final int SHAPE_DIAMOND        = 2;
    /** Cross shape */
    public static final int SHAPE_CROSS          = 3;
    /** Star shape */
    public static final int SHAPE_STAR           = 4;
    /** Up-pointing triangle shape */
    public static final int SHAPE_TRIANGLE_UP    = 5;
    /** Down-pointing triangle shape */
    public static final int SHAPE_TRIANGLE_DOWN  = 6;
    /** Left-pointing triangle shape */
    public static final int SHAPE_TRIANGLE_LEFT  = 7;
    /** Right-pointing triangle shape */
    public static final int SHAPE_TRIANGLE_RIGHT = 8;
    /** Hexagon shape */
    public static final int SHAPE_HEXAGON        = 9;
    /** The number of recognized shape types */
    public static final int SHAPE_COUNT          = 10;
    
} // end of interface Constants
