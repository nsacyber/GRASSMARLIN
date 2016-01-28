package prefuse.util;

import java.awt.BasicStroke;
import java.awt.Font;
import java.awt.Stroke;
import java.awt.geom.Rectangle2D;

import prefuse.Constants;
import prefuse.Display;
import prefuse.data.Schema;
import prefuse.visual.VisualItem;

/**
 * General library routines used by the prefuse toolkit.
 * 
 * @author <a href="http://jheer.org">jeffrey heer</a>
 */
public class PrefuseLib {

    /**
     * Group delimiter string used to demarcate the hierarchical structure
     * of nested data groups; the default value is a dot (.).
     */
    private static final String GROUP_DELIMITER 
        = PrefuseConfig.get("data.delimiter");
    /**
     * Scale factor for psychophysical scaling of area values, defaults to
     * 0.5, resulting in linear scaling.
     */
    private static final double SIZE_SCALE_FACTOR 
        = PrefuseConfig.getDouble("size.scale2D");
    /**
     * Prefix for visualization-specific data fields, such as those used by
     * VisualItem; the default is a single underscore (_).
     */
    public static final String FIELD_PREFIX
        = PrefuseConfig.get("data.visual.fieldPrefix");
    
    // ------------------------------------------------------------------------
    
    private PrefuseLib() {
        // prevent instantiation
    }
    
    // ------------------------------------------------------------------------
    // Memory Usage / Debugging Output
    
    /**
     * Get a String showing current JVM memory usage in kilobytes.
     * @return the memory usage String in KB
     */
    public static String getMemoryUsageInKB() {
        long total = Runtime.getRuntime().totalMemory() / (2<<10);
        long free  = Runtime.getRuntime().freeMemory() / (2<<10);
        long max   = Runtime.getRuntime().maxMemory() / (2<<10);
        return "Memory: "+(total-free)+"k / "+total+"k / "+max+"k";
    }

    /**
     * Get a String showing current JVM memory usage in megabytes.
     * @return the memory usage String in MB
     */
    public static String getMemoryUsageInMB() {
        long total = Runtime.getRuntime().totalMemory() / (2<<20);
        long free  = Runtime.getRuntime().freeMemory() / (2<<20);
        long max   = Runtime.getRuntime().maxMemory() / (2<<20);
        return "Memory: "+(total-free)+"M / "+total+"M / "+max+"M";
    }
    
    /**
     * Returns a string showing debugging info such as number of visualized
     * items and the current frame rate.
     * @return the debug string
     */
    public static String getDisplayStats(Display d) {
        float fr = Math.round(d.getFrameRate()*100f)/100f;
        
        Runtime rt = Runtime.getRuntime();
        long tm = rt.totalMemory() / (2<<20);
        long fm = rt.freeMemory() / (2<<20);
        long mm = rt.maxMemory() / (2<<20);
        
        StringBuffer sb = new StringBuffer();
        sb.append("frame rate: ").append(fr).append("fps - ");
        sb.append(d.getVisibleItemCount()).append(" items - ");
        sb.append("fonts(").append(FontLib.getCacheMissCount());
        sb.append(") colors(");
        sb.append(ColorLib.getCacheMissCount()).append(')');
        sb.append(" mem(");
        sb.append(tm-fm).append("M / ");
        sb.append(mm).append("M)");
        sb.append(" (x:");
        sb.append(StringLib.formatNumber(d.getDisplayX(),2));
        sb.append(", y:");
        sb.append(StringLib.formatNumber(d.getDisplayY(),2));
        sb.append(", z:");
        sb.append(StringLib.formatNumber(d.getScale(),5)).append(")");
        return sb.toString();
    }
    
    // ------------------------------------------------------------------------
    // VisualItem Methods
    
    /**
     * Returns a scale factor by which to scale a 2D shape to grow 
     * the area by the desired input size value. This is used to scale shapes
     * by total pixel area, rather than scaling each dimension by the
     * size value itself, which grows the pixel area quadratically rather
     * than linearly.
     */
    public static double getSize2D(double size) {
        return Math.pow(size, SIZE_SCALE_FACTOR);
    }
    
    /**
     * Get the distance between the x,y points of two VisualItems.
     * @param vi1 the first VisualItem
     * @param vi2 the second VisualItem
     * @return the distance between the items' x,y coordinates
     */
    public static double distance(VisualItem vi1, VisualItem vi2) {
        double dx = vi1.getX() - vi2.getX();
        double dy = vi1.getY() - vi2.getY();
        return Math.sqrt(dx*dx + dy*dy);
    }
    
    /**
     * Update the values in an interpolated column (a set of three columns
     * representing a current value along with starting and ending values).
     * The current value will become the new starting value, while the given
     * value will become the new current and ending values.
     * @param item the VisualItem to update
     * @param field the base field to update, start and ending fields will
     * also be updated, too.
     * @param val the value to set
     */
    public static void update(VisualItem item, String field, Object val) {
        item.set(getStartField(field), item.get(field));
        item.set(field, val);
        item.set(getEndField(field), val);
    }
    
    /**
     * Update the values in an interpolated column (a set of three columns
     * representing a current value along with starting and ending values).
     * The current value will become the new starting value, while the given
     * value will become the new current and ending values.
     * @param item the VisualItem to update
     * @param field the base field to update, start and ending fields will
     * also be updated, too.
     * @param val the value to set
     */
    public static void updateInt(VisualItem item, String field, int val) {
        item.setInt(getStartField(field), item.getInt(field));
        item.setInt(field, val);
        item.setInt(getEndField(field), val);
    }
    
    /**
     * Update the values in an interpolated column (a set of three columns
     * representing a current value along with starting and ending values).
     * The current value will become the new starting value, while the given
     * value will become the new current and ending values.
     * @param item the VisualItem to update
     * @param field the base field to update, start and ending fields will
     * also be updated, too.
     * @param val the value to set
     */
    public static void updateLong(VisualItem item, String field, long val) {
        item.setLong(getStartField(field), item.getLong(field));
        item.setLong(field, val);
        item.setLong(getEndField(field), val);
    }
    
    /**
     * Update the values in an interpolated column (a set of three columns
     * representing a current value along with starting and ending values).
     * The current value will become the new starting value, while the given
     * value will become the new current and ending values.
     * @param item the VisualItem to update
     * @param field the base field to update, start and ending fields will
     * also be updated, too.
     * @param val the value to set
     */
    public static void updateFloat(VisualItem item, String field, float val)
    {
        item.setFloat(getStartField(field), item.getFloat(field));
        item.setFloat(field, val);
        item.setFloat(getEndField(field), val);
    }
    
    /**
     * Update the values in an interpolated column (a set of three columns
     * representing a current value along with starting and ending values).
     * The current value will become the new starting value, while the given
     * value will become the new current and ending values.
     * @param item the VisualItem to update
     * @param field the base field to update, start and ending fields will
     * also be updated, too.
     * @param val the value to set
     */
    public static void updateDouble(VisualItem item, String field, double val)
    {
        item.setDouble(getStartField(field), item.getDouble(field));
        item.setDouble(field, val);
        item.setDouble(getEndField(field), val);
    }
    
    /**
     * Update the values in an interpolated column (a set of three columns
     * representing a current value along with starting and ending values).
     * The current value will become the new starting value, while the given
     * value will become the new current and ending values.
     * @param item the VisualItem to update
     * @param field the base field to update, start and ending fields will
     * also be updated, too.
     * @param b the value to set
     */
    public static void updateBoolean(VisualItem item, String field, boolean b)
    {
        item.setBoolean(getStartField(field), item.getBoolean(field));
        item.setBoolean(field, b);
        item.setBoolean(getEndField(field), b);
    }
    
    /**
     * Update the visibility of an item. The current visibility will become the
     * new starting visibility, while the given visibility value will become
     * the new current and ending visibility.
     * @param item the VisualItem to update
     * @param val the visibility value to set
     */
    public static void updateVisible(VisualItem item, boolean val) {
        item.setStartVisible(item.isVisible());
        item.setVisible(val);
        item.setEndVisible(val);
    }
    
    /**
     * Update the x-coordinate of an item. The current x value will become the
     * new starting x value, while the given value will become the new current
     * x and ending x values. This method also supports an optional referrer
     * item, whose x coordinate will become the new starting x coordinate
     * of item if item's current x value is NaN.
     * @param item the VisualItem to update
     * @param referrer an optional referrer VisualItem
     * @param x the x value to set
     */
    public static void setX(VisualItem item, VisualItem referrer, double x) {
        double sx = item.getX();
        if ( Double.isNaN(sx) )
            sx = (referrer != null ? referrer.getX() : x);
        
        item.setStartX(sx);
        item.setEndX(x);
        item.setX(x);
    }

    /**
     * Update the y-coordinate of an item. The current y value will become the
     * new starting y value, while the given value will become the new current
     * x and ending y values. This method also supports an optional referrer
     * item, whose y coordinate will become the new starting y coordinate
     * of item if item's current x value is NaN.
     * @param item the VisualItem to update
     * @param referrer an optional referrer VisualItem
     * @param y the y value to set
     */
    public static void setY(VisualItem item, VisualItem referrer, double y) {
        double sy = item.getY();
        if ( Double.isNaN(sy) )
            sy = (referrer != null ? referrer.getY() : y);
        
        item.setStartY(sy);
        item.setEndY(y);
        item.setY(y);
    }
    
    // ------------------------------------------------------------------------
    // Group Name Methods
    
    /**
     * Indicates if a group is a child group, a non-top-level data group in
     * a set of nested data groups (e.g., the node or edge table of a
     * graph or tree).
     * @return true if the group is a nested, or child, group
     */
    public static boolean isChildGroup(String group) {
        return group.indexOf(GROUP_DELIMITER) != -1;
    }
    
    /**
     * Get the parent group string of a child group, stripping off the
     * bottom-level group from the group name (e.g., graph.nodes --> graph).
     * @param group the group name
     * @return the stripped parent group name
     */
    public static String getParentGroup(String group) {
        int idx = group.lastIndexOf(GROUP_DELIMITER);
        return (idx < 0 ? null : group.substring(0,idx));
    }

    /**
     * Get the tail group name of a child group, stripping all but the
     * bottom-level group from the group name (e.g., graph.nodes --> nodes).
     * @param group the group name
     * @return the stripped child group name
     */
    public static String getChildGroup(String group) {
        int idx = group.lastIndexOf(GROUP_DELIMITER);
        return (idx < 0 ? null : group.substring(idx+1));
    }
    
    /**
     * Get the group name for the given parent and child group, simply
     * concatenating them together with a group delimiter in between.
     * @param parent the parent group name
     * @param child the child group name
     * @return the combined group name
     */
    public static String getGroupName(String parent, String child) {
        return parent + GROUP_DELIMITER + child;
    }
    
    /**
     * For a given interpolated field name, get the name of the start
     * field.
     * @param field the data field
     * @return the starting field for the interpolated column
     */
    public static String getStartField(String field) {
        return field+":start";
    }
    
    /**
     * For a given interpolated field name, get the name of the end
     * field.
     * @param field the data field
     * @return the ending field for the interpolated column
     */
    public static String getEndField(String field) {
        return field+":end";
    }
    
    // ------------------------------------------------------------------------
    // Schema Routines
    
    /**
     * Get an instance of the default Schema used for VisualItem instances.
     * Contains all the data members commonly used to model a visual element,
     * such as x,y position, stroke, fill, and text, colors, size, font,
     * and validated, visibility, interactive, fixed, highlight, and mouse
     * hover fields.
     * @return the VisualItem data Schema
     */
    public static Schema getVisualItemSchema() {
        Schema s = new Schema();
        
        // booleans
        s.addColumn(VisualItem.VALIDATED, boolean.class, Boolean.FALSE);
        s.addColumn(VisualItem.VISIBLE, boolean.class, Boolean.TRUE);
        s.addColumn(VisualItem.STARTVISIBLE, boolean.class, Boolean.FALSE);
        s.addColumn(VisualItem.ENDVISIBLE, boolean.class, Boolean.TRUE);
        s.addColumn(VisualItem.INTERACTIVE, boolean.class, Boolean.TRUE);
        s.addColumn(VisualItem.EXPANDED, boolean.class, Boolean.TRUE);
        s.addColumn(VisualItem.FIXED, boolean.class, Boolean.FALSE);
        s.addColumn(VisualItem.HIGHLIGHT, boolean.class, Boolean.FALSE);
        s.addColumn(VisualItem.HOVER, boolean.class, Boolean.FALSE);
        
        s.addInterpolatedColumn(VisualItem.X, double.class);
        s.addInterpolatedColumn(VisualItem.Y, double.class);
        
        // bounding box
        s.addColumn(VisualItem.BOUNDS, Rectangle2D.class, new Rectangle2D.Double());
        
        // color
        Integer defStroke = new Integer(ColorLib.rgba(0,0,0,0));
        s.addInterpolatedColumn(VisualItem.STROKECOLOR, int.class, defStroke);

        Integer defFill = new Integer(ColorLib.rgba(0,0,0,0));
        s.addInterpolatedColumn(VisualItem.FILLCOLOR, int.class, defFill);

        Integer defTextColor = new Integer(ColorLib.rgba(0,0,0,0));
        s.addInterpolatedColumn(VisualItem.TEXTCOLOR, int.class, defTextColor);

        // size
        s.addInterpolatedColumn(VisualItem.SIZE, double.class, new Double(1));
        
        // shape
        s.addColumn(VisualItem.SHAPE, int.class,
            new Integer(Constants.SHAPE_RECTANGLE));
        
        // stroke
        s.addColumn(VisualItem.STROKE, Stroke.class, new BasicStroke());
        
        // font
        Font defFont = FontLib.getFont("SansSerif",Font.PLAIN,10);
        s.addInterpolatedColumn(VisualItem.FONT, Font.class, defFont);
        
        // degree-of-interest
        s.addColumn(VisualItem.DOI, double.class, new Double(Double.MIN_VALUE));

        return s;
    }
    
    /**
     * Get the minimal Schema needed for a unique VisualItem. Can be useful
     * for derived groups that inherit other visual properties from a
     * another visual data group.
     * @return the minimal VisualItem data Schema
     */
    public static Schema getMinimalVisualSchema() {
        Schema s = new Schema();
        
        // booleans
        s.addColumn(VisualItem.VALIDATED, boolean.class, Boolean.FALSE);
        s.addColumn(VisualItem.VISIBLE, boolean.class, Boolean.TRUE);
        s.addColumn(VisualItem.STARTVISIBLE, boolean.class, Boolean.FALSE);
        s.addColumn(VisualItem.ENDVISIBLE, boolean.class, Boolean.TRUE);
        s.addColumn(VisualItem.INTERACTIVE, boolean.class, Boolean.TRUE);
        
        // bounding box
        s.addColumn(VisualItem.BOUNDS, Rectangle2D.class, new Rectangle2D.Double());
        
        return s;
    }
    
    /**
     * Get the VisualItem Schema used for axis tick marks and labels. Extends
     * the VisualItem Schema with an additional end-point coordinate, a
     * String label field, and numeric value field for storing the value
     * which the axis label corresponds to.
     * @return the Schema for axis tick marks and labels. 
     */
    public static Schema getAxisLabelSchema() {
        Schema s = getVisualItemSchema();

        s.setDefault(VisualItem.STARTVISIBLE, Boolean.FALSE);
        
        Integer defColor = new Integer(ColorLib.gray(230));
        s.setInterpolatedDefault(VisualItem.STROKECOLOR, defColor);
        
        defColor = new Integer(ColorLib.gray(150));
        s.setInterpolatedDefault(VisualItem.TEXTCOLOR, defColor);

        Double nan = new Double(Double.NaN);
        s.addInterpolatedColumn(VisualItem.X2, double.class);
        s.addInterpolatedColumn(VisualItem.Y2, double.class);
        
        s.addColumn(VisualItem.LABEL, String.class);
        s.addColumn(VisualItem.VALUE, double.class, nan);
        
        return s;
    }
    
} // end of class PrefuseLib
