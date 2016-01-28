package prefuse.visual;

import java.awt.BasicStroke;
import java.awt.Font;
import java.awt.geom.Rectangle2D;

import prefuse.Visualization;
import prefuse.data.CascadedTable;
import prefuse.data.Schema;
import prefuse.data.Table;
import prefuse.data.event.EventConstants;
import prefuse.data.expression.Predicate;
import prefuse.visual.tuple.TableVisualItem;

/**
 * A visual abstraction of a Table data structure. Serves as a backing table for
 * VisualItem tuples. VisualTable dervies from CascadedTable, so can inherit
 * another table's values. Commonly, a VisualTable is used to take a raw data
 * table and "strap" visual properties on top of it. VisualTables should not be
 * created directly, they are created automatically by adding data to a
 * Visualization, for example by using the
 * {@link Visualization#addTable(String, Table)} method.
 *
 * @author <a href="http://jheer.org">jeffrey heer</a>
 */
public class VisualTable extends CascadedTable implements VisualTupleSet {

    private Visualization m_vis;
    private String m_group;

    // ------------------------------------------------------------------------
    // Constructors
    /**
     * Create a new VisualTable.
     *
     * @param parent the parent table whose values this table should inherit
     * @param vis the Visualization associated with this table
     * @param group the data group of this table
     */
    public VisualTable(Table parent, Visualization vis, String group) {
        this(parent, vis, group, null, VisualItem.SCHEMA);
    }

    /**
     * Create a new VisualTable.
     *
     * @param parent the parent table whose values this table should inherit
     * @param vis the Visualization associated with this table
     * @param group the data group of this table
     * @param rowFilter a predicate determing which rows of the parent table
     * should be inherited by this table and which should be filtered out
     */
    public VisualTable(Table parent, Visualization vis, String group,
            Predicate rowFilter) {
        this(parent, vis, group, rowFilter, VisualItem.SCHEMA);
    }

    /**
     * Create a new VisualTable.
     *
     * @param parent the parent table whose values this table should inherit
     * @param vis the Visualization associated with this table
     * @param group the data group of this table
     * @param rowFilter a predicate determing which rows of the parent table
     * should be inherited by this table and which should be filtered out
     * @param schema the data schema to use for the table's local columns
     */
    public VisualTable(Table parent, Visualization vis, String group,
            Predicate rowFilter, Schema schema) {
        super(parent, rowFilter, null, TableVisualItem.class);
        init(vis, group, schema);
    }

    // -- non-cascaded visual table -------------------------------------------
    /**
     * Create a new VisualTable without a parent table.
     *
     * @param vis the Visualization associated with this table
     * @param group the data group of this table
     */
    public VisualTable(Visualization vis, String group) {
        super(TableVisualItem.class);
        init(vis, group, VisualItem.SCHEMA);
    }

    /**
     * Create a new VisualTable without a parent table.
     *
     * @param vis the Visualization associated with this table
     * @param group the data group of this table
     * @param schema the data schema to use for the table's local columns
     */
    public VisualTable(Visualization vis, String group, Schema schema) {
        super(TableVisualItem.class);
        init(vis, group, schema);
    }

    /**
     * Create a new VisualTable without a parent table.
     *
     * @param vis the Visualization associated with this table
     * @param group the data group of this table
     * @param schema the data schema to use for the table's local columns
     * @param tupleType the type of Tuple instances to use
     */
    public VisualTable(Visualization vis, String group, Schema schema,
            Class tupleType) {
        super(tupleType);
        init(vis, group, schema);
    }

    /**
     * Initialize this VisualTable
     *
     * @param vis the Visualization associated with this table
     * @param group the data group of this table
     * @param schema the data schema to use for the table's local columns
     */
    protected void init(Visualization vis, String group, Schema schema) {
        setVisualization(vis);
        setGroup(group);
        addColumns(schema);
        if (canGetBoolean(VisualItem.VISIBLE)) {
            index(VisualItem.VISIBLE);
        }
        if (canGetBoolean(VisualItem.STARTVISIBLE)) {
            index(VisualItem.STARTVISIBLE);
        }
        if (canGetBoolean(VisualItem.VALIDATED)) {
            index(VisualItem.VALIDATED);
        }
    }

    // ------------------------------------------------------------------------
    /**
     * Relay table events. Ensures that updated visual items are invalidated and
     * that damage reports are issued for deleted items.
     */
    @Override
    protected void fireTableEvent(int row0, int row1, int col, int type) {
        // table attributes changed, so we invalidate the bounds
        if (type == EventConstants.UPDATE) {
            if (col != VisualItem.IDX_VALIDATED) {
                for (int r = row0; r <= row1; ++r) {
                    setValidated(r, false);
                }
            } else {
                // change in validated status
                for (int r = row0; r <= row1; ++r) {
                    if (!isValidated(r)) {
                        // retrieve the old bounds to report damage
                        m_vis.damageReport(getItem(r), getBounds(r));
                    }
                }
            }
        } else if (type == EventConstants.DELETE && col == EventConstants.ALL_COLUMNS) {
            for (int r = row0; r <= row1; ++r) {
                if (isVisible(r) && isValidated(r)) {
                    VisualItem item = (VisualItem) getTuple(r);
                    m_vis.damageReport(item, getBounds(r));
                }
            }
        }
        // now propagate the change event
        super.fireTableEvent(row0, row1, col, type);
    }

    // ------------------------------------------------------------------------
    // VisualItemTable Methods
    /**
     * @see prefuse.visual.VisualTupleSet#getVisualization()
     */
    public Visualization getVisualization() {
        return m_vis;
    }

    /**
     * Set the visualization associated with this VisualTable
     *
     * @param vis the visualization to set
     */
    public void setVisualization(Visualization vis) {
        m_vis = vis;
    }

    /**
     * Get the visualization data group name for this table
     *
     * @return the data group name
     */
    public String getGroup() {
        return m_group;
    }

    /**
     * Set the visualization data group name for this table
     *
     * @return the data group name to use
     */
    public void setGroup(String group) {
        m_group = group;
    }

    /**
     * Get the VisualItem for the given table row.
     *
     * @param row a table row index
     * @return the VisualItem for the given table row
     */
    public VisualItem getItem(int row) {
        return (VisualItem) getTuple(row);
    }

    /**
     * Add a new row to the table and return the VisualItem for that row. Only
     * allowed if there is no parent table, otherwise an exception will result.
     *
     * @return the VisualItem for the newly added table row.
     */
    public VisualItem addItem() {
        return getItem(addRow());
    }

    // ------------------------------------------------------------------------
    // VisualItem Data Access
    /**
     * Indicates if the given row is currently validated. If not,
     * validateBounds() must be run to update the bounds to a current value.
     *
     * @param row the table row
     * @return true if validated, false otherwise
     */
    public boolean isValidated(int row) {
        return getBoolean(row, VisualItem.VALIDATED);
    }

    /**
     * Set the given row's validated flag. This is for internal use by prefuse
     * and, in general, should not be called by application code.
     *
     * @param row the table row to set
     * @param value the value of the validated flag to set.
     */
    public void setValidated(int row, boolean value) {
        setBoolean(row, VisualItem.VALIDATED, value);
    }

    /**
     * Indicates if the given row is currently set to be visible. Items with the
     * visible flag set false will not be drawn by a display. Invisible items
     * are also by necessity not interactive, regardless of the value of the
     * interactive flag.
     *
     * @param row the table row
     * @return true if visible, false if invisible
     */
    public boolean isVisible(int row) {
        return getBoolean(row, VisualItem.VISIBLE);
    }

    /**
     * Set the given row's visibility.
     *
     * @param row the table row to set
     * @param value true to make the item visible, false otherwise.
     */
    public void setVisible(int row, boolean value) {
        setBoolean(row, VisualItem.VISIBLE, value);
    }

    /**
     * Indicates if the start visible flag is set to true. This is the
     * visibility value consulted for the staring value of the visibility field
     * at the beginning of an animated transition.
     *
     * @param row the table row
     * @return true if this item starts out visible, false otherwise.
     */
    public boolean isStartVisible(int row) {
        return getBoolean(row, VisualItem.STARTVISIBLE);
    }

    /**
     * Set the start visible flag.
     *
     * @param row the table row to set
     * @param value true to set the start visible flag, false otherwise
     */
    public void setStartVisible(int row, boolean value) {
        setBoolean(row, VisualItem.STARTVISIBLE, value);
    }

    /**
     * Indictes if the end visible flag is set to true. This is the visibility
     * value consulted for the ending value of the visibility field at the end
     * of an animated transition.
     *
     * @param row the table row
     * @return true if this items ends visible, false otherwise.
     */
    public boolean isEndVisible(int row) {
        return getBoolean(row, VisualItem.ENDVISIBLE);
    }

    /**
     * Set the end visible flag.
     *
     * @param row the table row to set
     * @param value true to set the end visible flag, false otherwise
     */
    public void setEndVisible(int row, boolean value) {
        setBoolean(row, VisualItem.ENDVISIBLE, value);
    }

    /**
     * Indicates if this item is interactive, meaning it can potentially respond
     * to mouse and keyboard input events.
     *
     * @param row the table row
     * @return true if the item is interactive, false otherwise
     */
    public boolean isInteractive(int row) {
        return getBoolean(row, VisualItem.INTERACTIVE);
    }

    /**
     * Set the interactive status of the given row.
     *
     * @param row the table row to set
     * @param value true for interactive, false for non-interactive
     */
    public void setInteractive(int row, boolean value) {
        setBoolean(row, VisualItem.INTERACTIVE, value);
    }

    /**
     * Indicates the given row is expanded. Only used for items that are part of
     * a graph structure.
     *
     * @param row the table row
     * @return true if expanded, false otherwise
     */
    public boolean isExpanded(int row) {
        return getBoolean(row, VisualItem.EXPANDED);
    }

    /**
     * Set the expanded flag.
     *
     * @param row the table row to set
     * @param value true to set as expanded, false as collapsed.
     */
    public void setExpanded(int row, boolean value) {
        setBoolean(row, VisualItem.EXPANDED, value);
    }

    /**
     * Indicates if the given row is fixed, and so will not have its position
     * changed by any layout or distortion actions.
     *
     * @param row the table row
     * @return true if the item has a fixed position, false otherwise
     */
    public boolean isFixed(int row) {
        return getBoolean(row, VisualItem.FIXED);
    }

    /**
     * Sets if the given row is fixed in its position.
     *
     * @param row the table row to set
     * @param value true to fix the item, false otherwise
     */
    public void setFixed(int row, boolean value) {
        setBoolean(row, VisualItem.FIXED, value);
    }

    /**
     * Indicates if the given row is highlighted.
     *
     * @param row the table row
     * @return true for highlighted, false for not highlighted
     */
    public boolean isHighlighted(int row) {
        return getBoolean(row, VisualItem.HIGHLIGHT);
    }

    /**
     * Set the highlighted status of the given row. How higlighting values are
     * interpreted by the system depends on the various processing actions set
     * up for an application (e.g., how a
     * {@link prefuse.action.assignment.ColorAction} might assign colors based
     * on the flag).
     *
     * @param row the table row to set
     * @param value true to highlight the item, false for no highlighting.
     */
    public void setHighlighted(int row, boolean value) {
        setBoolean(row, VisualItem.HIGHLIGHT, value);
    }

    /**
     * Indicates if the given row currently has the mouse pointer over it.
     *
     * @param row the table row
     * @return true if the mouse pointer is over this item, false otherwise
     */
    public boolean isHover(int row) {
        return getBoolean(row, VisualItem.HOVER);
    }

    /**
     * Set the hover flag. This is set automatically by the prefuse framework,
     * so should not need to be set explicitly by application code.
     *
     * @param row the table row to set
     * @param value true to set the hover flag, false otherwise
     */
    public void setHover(int row, boolean value) {
        setBoolean(row, VisualItem.HOVER, value);
    }

    // ------------------------------------------------------------------------
    /**
     * Get the current x-coordinate of the given row.
     *
     * @param row the table row
     * @return the current x-coordinate
     */
    public double getX(int row) {
        return getDouble(row, VisualItem.X);
    }

    /**
     * Set the current x-coordinate of the given row.
     *
     * @param row the table row to set
     * @param x the new current x-coordinate
     */
    public void setX(int row, double x) {
        setDouble(row, VisualItem.X, x);
    }

    /**
     * Get the current y-coordinate of the given row.
     *
     * @param row the table row
     * @return the current y-coordinate
     */
    public double getY(int row) {
        return getDouble(row, VisualItem.Y);
    }

    /**
     * Set the current y-coordinate of the given row.
     *
     * @param row the table row to set
     * @param y the new current y-coordinate
     */
    public void setY(int row, double y) {
        setDouble(row, VisualItem.Y, y);
    }

    /**
     * Get the starting x-coordinate of the given row.
     *
     * @param row the table row
     * @return the starting x-coordinate
     */
    public double getStartX(int row) {
        return getDouble(row, VisualItem.STARTX);
    }

    /**
     * Set the starting x-coordinate of the given row.
     *
     * @param row the table row to set
     * @param x the new starting x-coordinate
     */
    public void setStartX(int row, double x) {
        setDouble(row, VisualItem.STARTX, x);
    }

    /**
     * Get the starting y-coordinate of the given row.
     *
     * @param row the table row
     * @return the starting y-coordinate
     */
    public double getStartY(int row) {
        return getDouble(row, VisualItem.STARTY);
    }

    /**
     * Set the starting y-coordinate of the given row.
     *
     * @param row the table row to set
     * @param y the new starting y-coordinate
     */
    public void setStartY(int row, double y) {
        setDouble(row, VisualItem.STARTY, y);
    }

    /**
     * Get the ending x-coordinate of the given row.
     *
     * @param row the table row
     * @return the ending x-coordinate
     */
    public double getEndX(int row) {
        return getDouble(row, VisualItem.ENDX);
    }

    /**
     * Set the ending x-coordinate of the given row.
     *
     * @param row the table row to set
     * @param x the new ending x-coordinate
     */
    public void setEndX(int row, double x) {
        setDouble(row, VisualItem.ENDX, x);
    }

    /**
     * Get the ending y-coordinate of the given row.
     *
     * @param row the table row
     * @return the ending y-coordinate
     */
    public double getEndY(int row) {
        return getDouble(row, VisualItem.ENDY);
    }

    /**
     * Set the ending y-coordinate of the given row.
     *
     * @param row the table row to set
     * @param y the new ending y-coordinate
     */
    public void setEndY(int row, double y) {
        setDouble(row, VisualItem.ENDY, y);
    }

    /**
     * Returns the bounds for the VisualItem at the given row index. The
     * returned reference is for the actual bounds object used by the system --
     * do <b>NOT</b> directly edit the values in this returned object!! This
     * will corrupt the state of the system.
     *
     * @param row the table row
     * @return the bounding box for the item at the given row
     */
    public Rectangle2D getBounds(int row) {
        return (Rectangle2D) get(row, VisualItem.BOUNDS);
    }

    /**
     * Set the bounding box for an item. This method is used by Renderer modules
     * when the bounds are validated, or set by processing Actions used in
     * conjunction with Renderers that do not perform bounds management.
     *
     * @param row the table row to set
     * @param x the minimum x-coordinate
     * @param y the minimum y-coorindate
     * @param w the width of this item
     * @param h the height of this item
     * @see VisualItem#BOUNDS
     */
    public void setBounds(int row, double x, double y, double w, double h) {
        getBounds(row).setRect(x, y, w, h);
        fireTableEvent(row, row,
                getColumnNumber(VisualItem.BOUNDS), EventConstants.UPDATE);
    }

    // ------------------------------------------------------------------------
    /**
     * Get the current stroke color of the row. The stroke color is used to draw
     * lines and the outlines of shapes. Color values as represented as an
     * integer containing the red, green, blue, and alpha (transparency) color
     * channels. A color with a zero alpha component is fully transparent and
     * will not be drawn.
     *
     * @param row the table row
     * @return the current stroke color, represented as an integer
     * @see prefuse.util.ColorLib
     */
    public int getStrokeColor(int row) {
        return getInt(row, VisualItem.STROKECOLOR);
    }

    /**
     * Set the current stroke color of the row. The stroke color is used to draw
     * lines and the outlines of shapes. Color values as represented as an
     * integer containing the red, green, blue, and alpha (transparency) color
     * channels. A color with a zero alpha component is fully transparent and
     * will not be drawn.
     *
     * @param row the table row to set
     * @param color the current stroke color, represented as an integer
     * @see prefuse.util.ColorLib
     */
    public void setStrokeColor(int row, int color) {
        setInt(row, VisualItem.STROKECOLOR, color);
    }

    /**
     * Get the starting stroke color of the row. The stroke color is used to
     * draw lines and the outlines of shapes. Color values as represented as an
     * integer containing the red, green, blue, and alpha (transparency) color
     * channels. A color with a zero alpha component is fully transparent and
     * will not be drawn.
     *
     * @param row the table row
     * @return the starting stroke color, represented as an integer
     * @see prefuse.util.ColorLib
     */
    public int getStartStrokeColor(int row) {
        return getInt(row, VisualItem.STARTSTROKECOLOR);
    }

    /**
     * Set the starting stroke color of the row. The stroke color is used to
     * draw lines and the outlines of shapes. Color values as represented as an
     * integer containing the red, green, blue, and alpha (transparency) color
     * channels. A color with a zero alpha component is fully transparent and
     * will not be drawn.
     *
     * @param row the table row to set
     * @param color the starting stroke color, represented as an integer
     * @see prefuse.util.ColorLib
     */
    public void setStartStrokeColor(int row, int color) {
        setInt(row, VisualItem.STARTSTROKECOLOR, color);
    }

    /**
     * Get the ending stroke color of the row. The stroke color is used to draw
     * lines and the outlines of shapes. Color values as represented as an
     * integer containing the red, green, blue, and alpha (transparency) color
     * channels. A color with a zero alpha component is fully transparent and
     * will not be drawn.
     *
     * @param row the table row
     * @return the ending stroke color, represented as an integer
     * @see prefuse.util.ColorLib
     */
    public int getEndStrokeColor(int row) {
        return getInt(row, VisualItem.ENDSTROKECOLOR);
    }

    /**
     * Set the ending stroke color of the row. The stroke color is used to draw
     * lines and the outlines of shapes. Color values as represented as an
     * integer containing the red, green, blue, and alpha (transparency) color
     * channels. A color with a zero alpha component is fully transparent and
     * will not be drawn.
     *
     * @param row the table row to set
     * @param color the ending stroke color, represented as an integer
     * @see prefuse.util.ColorLib
     */
    public void setEndStrokeColor(int row, int color) {
        setInt(row, VisualItem.ENDSTROKECOLOR, color);
    }

    /**
     * Get the current fill color of the row. The fill color is used to fill the
     * interior of shapes. Color values as represented as an integer containing
     * the red, green, blue, and alpha (transparency) color channels. A color
     * with a zero alpha component is fully transparent and will not be drawn.
     *
     * @param row the table row
     * @return the current fill color, represented as an integer
     * @see prefuse.util.ColorLib
     */
    public int getFillColor(int row) {
        return getInt(row, VisualItem.FILLCOLOR);
    }

    /**
     * Set the current fill color of the row. The stroke color is used to fill
     * the interior of shapes. Color values as represented as an integer
     * containing the red, green, blue, and alpha (transparency) color channels.
     * A color with a zero alpha component is fully transparent and will not be
     * drawn.
     *
     * @param row the table row to set
     * @param color the current fill color, represented as an integer
     * @see prefuse.util.ColorLib
     */
    public void setFillColor(int row, int color) {
        setInt(row, VisualItem.FILLCOLOR, color);
    }

    /**
     * Get the starting fill color of the row. The fill color is used to fill
     * the interior of shapes. Color values as represented as an integer
     * containing the red, green, blue, and alpha (transparency) color channels.
     * A color with zero alpha component is fully transparent and will not be
     * drawn.
     *
     * @param row the table row
     * @return the starting fill color, represented as an integer
     * @see prefuse.util.ColorLib
     */
    public int getStartFillColor(int row) {
        return getInt(row, VisualItem.STARTFILLCOLOR);
    }

    /**
     * Set the starting fill color of the row. The stroke color is used to fill
     * the interior of shapes. Color values as represented as an integer
     * containing the red, green, blue, and alpha (transparency) color channels.
     * A color with a zero alpha component is fully transparent and will not be
     * drawn.
     *
     * @param row the table row to set
     * @param color the starting fill color, represented as an integer
     * @see prefuse.util.ColorLib
     */
    public void setStartFillColor(int row, int color) {
        setInt(row, VisualItem.STARTFILLCOLOR, color);
    }

    /**
     * Get the ending fill color of the row. The fill color is used to fill the
     * interior of shapes. Color values as represented as an integer containing
     * the red, green, blue, and alpha (transparency) color channels. A color
     * with zero alpha component is fully transparent and will not be drawn.
     *
     * @param row the table row
     * @return the ending fill color, represented as an integer
     * @see prefuse.util.ColorLib
     */
    public int getEndFillColor(int row) {
        return getInt(row, VisualItem.ENDFILLCOLOR);
    }

    /**
     * Set the ending fill color of the row. The stroke color is used to fill
     * the interior of shapes. Color values as represented as an integer
     * containing the red, green, blue, and alpha (transparency) color channels.
     * A color with a zero alpha component is fully transparent and will not be
     * drawn.
     *
     * @param row the table row to set
     * @param color the ending fill color, represented as an integer
     * @see prefuse.util.ColorLib
     */
    public void setEndFillColor(int row, int color) {
        setInt(row, VisualItem.ENDFILLCOLOR, color);
    }

    /**
     * Get the current text color of the row. The text color is used to draw
     * text strings for the item. Color values as represented as an integer
     * containing the red, green, blue, and alpha (transparency) color channels.
     * A color with zero alpha component is fully transparent and will not be
     * drawn.
     *
     * @param row the table row
     * @return the current text color, represented as an integer
     * @see prefuse.util.ColorLib
     */
    public int getTextColor(int row) {
        return getInt(row, VisualItem.TEXTCOLOR);
    }

    /**
     * Set the current text color of the row. The text color is used to draw
     * text strings for the item. Color values as represented as an integer
     * containing the red, green, blue, and alpha (transparency) color channels.
     * A color with a zero alpha component is fully transparent and will not be
     * drawn.
     *
     * @param row the table row to set
     * @param color the current text color, represented as an integer
     * @see prefuse.util.ColorLib
     */
    public void setTextColor(int row, int color) {
        setInt(row, VisualItem.TEXTCOLOR, color);
    }

    /**
     * Get the starting text color of the row. The text color is used to draw
     * text strings for the item. Color values as represented as an integer
     * containing the red, green, blue, and alpha (transparency) color channels.
     * A color with zero alpha component is fully transparent and will not be
     * drawn.
     *
     * @param row the table row
     * @return the starting text color, represented as an integer
     * @see prefuse.util.ColorLib
     */
    public int getStartTextColor(int row) {
        return getInt(row, VisualItem.STARTTEXTCOLOR);
    }

    /**
     * Set the starting text color of the row. The text color is used to draw
     * text strings for the item. Color values as represented as an integer
     * containing the red, green, blue, and alpha (transparency) color channels.
     * A color with a zero alpha component is fully transparent and will not be
     * drawn.
     *
     * @param row the table row to set
     * @param color the starting text color, represented as an integer
     * @see prefuse.util.ColorLib
     */
    public void setStartTextColor(int row, int color) {
        setInt(row, VisualItem.STARTTEXTCOLOR, color);
    }

    /**
     * Get the ending text color of the row. The text color is used to draw text
     * strings for the item. Color values as represented as an integer
     * containing the red, green, blue, and alpha (transparency) color channels.
     * A color with zero alpha component is fully transparent and will not be
     * drawn.
     *
     * @param row the table row
     * @return the ending text color, represented as an integer
     * @see prefuse.util.ColorLib
     */
    public int getEndTextColor(int row) {
        return getInt(row, VisualItem.ENDTEXTCOLOR);
    }

    /**
     * Set the ending text color of the row. The text color is used to draw text
     * strings for the item. Color values as represented as an integer
     * containing the red, green, blue, and alpha (transparency) color channels.
     * A color with a zero alpha component is fully transparent and will not be
     * drawn.
     *
     * @param row the table row to set
     * @param color the ending text color, represented as an integer
     * @see prefuse.util.ColorLib
     */
    public void setEndTextColor(int row, int color) {
        setInt(row, VisualItem.ENDTEXTCOLOR, color);
    }

    // ------------------------------------------------------------------------
    /**
     * Get the current size value of the row. Size values are typically used to
     * scale an item, either in one-dimension (e.g., a bar chart length) or
     * two-dimensions (e.g., using pixel area to encode a quantitative value).
     *
     * @param row the table row
     * @return the current size value
     */
    public double getSize(int row) {
        return getDouble(row, VisualItem.SIZE);
    }

    /**
     * Set the current size value of the row. Size values are typically used to
     * scale an item, either in one-dimension (e.g., a bar chart length) or
     * two-dimensions (e.g., using pixel area to encode a quantitative value).
     *
     * @param row the table row to set
     * @param size the current size value
     */
    public void setSize(int row, double size) {
        setDouble(row, VisualItem.SIZE, size);
    }

    /**
     * Get the starting size value of the row. Size values are typically used to
     * scale an item, either in one-dimension (e.g., a bar chart length) or
     * two-dimensions (e.g., using pixel area to encode a quantitative value).
     *
     * @param row the table row
     * @return the starting size value
     */
    public double getStartSize(int row) {
        return getDouble(row, VisualItem.STARTSIZE);
    }

    /**
     * Set the starting size value of the row. Size values are typically used to
     * scale an item, either in one-dimension (e.g., a bar chart length) or
     * two-dimensions (e.g., using pixel area to encode a quantitative value).
     *
     * @param row the table row to set
     * @param size the starting size value
     */
    public void setStartSize(int row, double size) {
        setDouble(row, VisualItem.STARTSIZE, size);
    }

    /**
     * Get the ending size value of the row. Size values are typically used to
     * scale an item, either in one-dimension (e.g., a bar chart length) or
     * two-dimensions (e.g., using pixel area to encode a quantitative value).
     *
     * @param row the table row
     * @return the ending size value
     */
    public double getEndSize(int row) {
        return getDouble(row, VisualItem.ENDSIZE);
    }

    /**
     * Set the ending size value of the row. Size values are typically used to
     * scale an item, either in one-dimension (e.g., a bar chart length) or
     * two-dimensions (e.g., using pixel area to encode a quantitative value).
     *
     * @param row the table row to set
     * @param size the ending size value
     */
    public void setEndSize(int row, double size) {
        setDouble(row, VisualItem.ENDSIZE, size);
    }

    // ------------------------------------------------------------------------
    /**
     * Get the current shape value of the row. One of the SHAPE constants
     * included in the {@link prefuse.Constants} class. This value only has an
     * effect if a Renderer that supports different shapes is used (e.g.,
     * {@link prefuse.render.ShapeRenderer}.
     *
     * @param row the table row
     * @return the current shape value
     */
    public int getShape(int row) {
        return getInt(row, VisualItem.SHAPE);
    }

    /**
     * Set the current shape value of the row. One of the SHAPE constants
     * included in the {@link prefuse.Constants} class. This value only has an
     * effect if a Renderer that supports different shapes is used (e.g.,
     * {@link prefuse.render.ShapeRenderer}.
     *
     * @param row the table row to set
     * @param shape the shape value to use
     */
    public void setShape(int row, int shape) {
        setInt(row, VisualItem.SHAPE, shape);
    }

    // ------------------------------------------------------------------------
    /**
     * Get the current stroke used to draw lines and shape outlines for the item
     * at the given row.
     *
     * @return the stroke used to draw lines and shape outlines
     */
    public BasicStroke getStroke(int row) {
        return (BasicStroke) get(row, VisualItem.STROKE);
    }

    /**
     * Set the current stroke used to draw lines and shape outlines.
     *
     * @param stroke the stroke to use to draw lines and shape outlines
     */
    public void setStroke(int row, BasicStroke stroke) {
        set(row, VisualItem.STROKE, stroke);
    }

    // ------------------------------------------------------------------------
    /**
     * Get the current font for the row. The font is used as the default
     * typeface for drawing text for this item.
     *
     * @param row the table row
     * @return the current font value
     */
    public Font getFont(int row) {
        return (Font) get(row, VisualItem.FONT);
    }

    /**
     * Set the current font for the the row. The font is used as the default
     * typeface for drawing text for this item.
     *
     * @param row the table row to set
     * @param font the current font value
     */
    public void setFont(int row, Font font) {
        set(row, VisualItem.FONT, font);
    }

    /**
     * Get the starting font for the row. The font is used as the default
     * typeface for drawing text for this item.
     *
     * @param row the table row
     * @return the starting font value
     */
    public Font getStartFont(int row) {
        return (Font) get(row, VisualItem.STARTFONT);
    }

    /**
     * Set the starting font for the row. The font is used as the default
     * typeface for drawing text for this item.
     *
     * @param row the table row to set
     * @param font the starting font value
     */
    public void setStartFont(int row, Font font) {
        set(row, VisualItem.STARTFONT, font);
    }

    /**
     * Get the ending font for the row. The font is used as the default typeface
     * for drawing text for this item.
     *
     * @param row the table row
     * @return the ending font value
     */
    public Font getEndFont(int row) {
        return (Font) get(row, VisualItem.ENDFONT);
    }

    /**
     * Set the ending font for the row. The font is used as the default typeface
     * for drawing text for this item.
     *
     * @param row the table row to set
     * @param font the ending font value
     */
    public void setEndFont(int row, Font font) {
        set(row, VisualItem.ENDFONT, font);
    }

    // ------------------------------------------------------------------------
    /**
     * Get the degree-of-interest (DOI) value. The degree-of-interet is an
     * optional value that can be used to sort items by importance, control item
     * visibility, or influence particular visual encodings. A common example is
     * to use the DOI to store the graph distance of a node from the nearest
     * selected focus node.
     *
     * @param row the table row
     * @return the DOI value of this item
     */
    public double getDOI(int row) {
        return getDouble(row, VisualItem.DOI);
    }

    /**
     * Set the degree-of-interest (DOI) value. The degree-of-interet is an
     * optional value that can be used to sort items by importance, control item
     * visibility, or influence particular visual encodings. A common example is
     * to use the DOI to store the graph distance of a node from the nearest
     * selected focus node.
     *
     * @param row the table row to set
     * @param doi the DOI value of this item
     */
    public void setDOI(int row, double doi) {
        setDouble(row, VisualItem.DOI, doi);
    }

} // end of class VisualTable
