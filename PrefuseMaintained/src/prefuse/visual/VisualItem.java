package prefuse.visual;

import java.awt.BasicStroke;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;

import prefuse.Visualization;
import prefuse.data.Schema;
import prefuse.data.Tuple;
import prefuse.data.tuple.TupleSet;
import prefuse.render.Renderer;
import prefuse.util.PrefuseLib;

/**
 * <p>Base interface for representing a visual item, a data object with a
 * visual interactive form. VisualItems are Tuple instances, and so
 * can support any number of data fields in a backing data table. VisualItems
 * also support data fields specific to visualization, such as a location,
 * bounding box, colors, size, and font. The VisualItem interface provides
 * convenience methods for accessing these common visual properties, but
 * the underlying mechanism is the same as any Tuple -- data stored in
 * a tabular format. Just as all Tuple instances are backed by a data Table,
 * each VisualItem is backed by a VisualTable. Additionally, each VisualItem
 * is associated with one and only one {@link prefuse.Visualization}.</p>
 * 
 * <p>VisualItems are only responsible for storing their visual data
 * properties. The final visual appearance of an item is determined by
 * a {@link prefuse.render.Renderer}, which contains instructions for drawing
 * the item. The Renderer to use for a given item is decided by the
 * {@link prefuse.render.RendererFactory} associated with the item's
 * backing Visualization.</p>
 * 
 * <p>Finally, actually setting the visual properties of VisualItems is
 * commonly done by the many {@link prefuse.action.Action} modules available
 * for processing visual data. This includes spatial layout as well as
 * color, size, and font assignment.</p> 
 * 
 * @author <a href="http://jheer.org">jeffrey heer</a>
 */
public interface VisualItem extends Tuple {

    /**
     * Get the backing Visualization of which this VisualItem is a part.
     * @return the backing Visualization
     */
    public Visualization getVisualization();
    
    /**
     * Get the primary data group of which this VisualItem is a member.
     * Returns the name of the group of this item's backing VisualTable.
     * @return the item's primary group
     */
    public String getGroup();
    
    /**
     * Indicates if this item is a member of a given group. This includes both
     * the item's primary group (that of it's backing VisualTable) and any
     * number of additional focus groups associated with the Visualization.
     * @param group the group to check for membership.
     * @return true if this item is in the group, false otherwise.
     */
    public boolean isInGroup(String group);
    
    /**
     * Returns the original backing data set from which this VisualItem is
     * derived. This could be a Table, Graph, or Tree instance. This method
     * returns null if this VisualItem is not derived from backing data.
     * @return the backing data set from which this VisualItem is derived,
     * or null if none.
     */
    public TupleSet getSourceData();

    /**
     * Returns the original backing data tuple from which this VisualItem is
     * derived. This could be a Tuple, Node, or Edge instance. This method
     * returns null if this VisualItem is not derived from backing data.
     * @return the backing data tuple from which this VisualItem is derived,
     * or null if none.
     */
    public Tuple getSourceTuple();
    
    
    // ------------------------------------------------------------------------
    // VisualItem Methods
    
    /**
     * Render this item to the given graphics context. This is typically done
     * by requesting the appropriate Renderer from the backing Visualization's
     * RendererFactory, and then using the Renderer to draw this item.
     * @param g the graphics context to render into.
     */
    public void render(Graphics2D g);
    
    /**
     * Get the Renderer instance for drawing this VisualItem. The Renderer is
     * retrieved by requesting it from the backing Visualization's
     * RendererFactory.
     * @return the Renderer for this VisualItem
     */
    public Renderer getRenderer();
    
    /**
     * Validate the bounds of this VisualItem. When a data value for a
     * VisualItem is updated, it's bounds are invalidated, as the data change
     * may have changed to appearance of the item. Revalidating the bounds
     * causes the bounds of the item to be recomputed and made current.
     * @return the validated boundig box of this item
     */
    public Rectangle2D validateBounds();
    
    // -- Boolean Flags -------------------------------------------------------
      
    /**
     * Indicates if this VisualItem is currently validated. If not,
     * validateBounds() must be run to update the bounds to a current value.
     * @return true if validated, false otherwise
     * @see #VALIDATED
     */
    public boolean isValidated();
    
    /**
     * Set this item's validated flag. This is for internal use by prefuse and,
     * in general, should not be called by application code.
     * @param value the value of the validated flag to set.
     * @see #VALIDATED
     */
    public void setValidated(boolean value);
    
    /**
     * Indicates if this VisualItem is currently set to be visible. Items with
     * the visible flag set false will not be drawn by a display. Invisible
     * items are also by necessity not interactive, regardless of the value of
     * the interactive flag.
     * @return true if visible, false if invisible
     * @see #VISIBLE
     */
    public boolean isVisible();
    
    /**
     * Set this item's visibility.
     * @param value true to make the item visible, false otherwise.
     * @see #VISIBLE
     */
    public void setVisible(boolean value);
    
    /**
     * Indicates if the start visible flag is set to true. This is the
     * visibility value consulted for the staring value of the visibility
     * field at the beginning of an animated transition.
     * @return true if this item starts out visible, false otherwise.
     * @see #STARTVISIBLE
     */
    public boolean isStartVisible();
    
    /**
     * Set the start visible flag.
     * @param value true to set the start visible flag, false otherwise
     * @see #STARTVISIBLE
     */
    public void setStartVisible(boolean value);

    /**
     * Indictes if the end visible flag is set to true. This is the
     * visibility value consulted for the ending value of the visibility
     * field at the end of an animated transition.
     * @return true if this items ends visible, false otherwise.
     * @see #ENDVISIBLE
     */
    public boolean isEndVisible();
    
    /**
     * Set the end visible flag.
     * @param value true to set the end visible flag, false otherwise
     * @see #ENDVISIBLE
     */
    public void setEndVisible(boolean value);
    
    /**
     * Indicates if this item is interactive, meaning it can potentially
     * respond to mouse and keyboard input events.
     * @return true if the item is interactive, false otherwise
     * @see #INTERACTIVE
     */
    public boolean isInteractive();

    /**
     * Set the interactive status of this VisualItem.
     * @param value true for interactive, false for non-interactive
     * @see #INTERACTIVE
     */
    public void setInteractive(boolean value);
    
    /**
     * Indicates this item is expanded. Only used for items that are
     * part of a graph structure. 
     * @return true if expanded, false otherwise
     * @see #EXPANDED
     */
    public boolean isExpanded();

    /**
     * Set the expanded flag.
     * @param value true to set as expanded, false as collapsed.
     * @see #EXPANDED
     */
    public void setExpanded(boolean value);
    
    /**
     * Indicates if the item is fixed, and so will not have its position
     * changed by any layout or distortion actions.
     * @return true if the item has a fixed position, false otherwise
     * @see #FIXED
     */
    public boolean isFixed();

    /**
     * Sets if the item is fixed in its position.
     * @param value true to fix the item, false otherwise
     * @see #FIXED
     */
    public void setFixed(boolean value);
    
    /**
     * Indicates if the item is highlighted.
     * @return true for highlighted, false for not highlighted
     * @see #HIGHLIGHT
     */
    public boolean isHighlighted();
    
    /**
     * Set the highlighted status of this item. How higlighting values are
     * interpreted by the system depends on the various processing actions
     * set up for an application (e.g., how a
     * {@link prefuse.action.assignment.ColorAction} might assign colors
     * based on the flag).
     * @param value true to highlight the item, false for no highlighting.
     * @see #HIGHLIGHT
     */
    public void setHighlighted(boolean value);

    /**
     * Indicates if the item currently has the mouse pointer over it.
     * @return true if the mouse pointer is over this item, false otherwise
     * @see #HOVER
     */
    public boolean isHover();
    
    /**
     * Set the hover flag. This is set automatically by the prefuse framework,
     * so should not need to be set explicitly by application code.
     * @param value true to set the hover flag, false otherwise
     * @see #HOVER
     */
    public void setHover(boolean value);
    
    // ------------------------------------------------------------------------
    
    /**
     * Get the current x-coordinate of this item.
     * @return the current x-coordinate
     * @see #X
     */
    public double getX();

    /**
     * Set the current x-coordinate of this item.
     * @param x the new current x-coordinate
     * @see #X
     */
    public void setX(double x);
    
    /**
     * Get the current y-coordinate of this item.
     * @return the current y-coordinate
     * @see #Y
     */
    public double getY();
    
    /**
     * Set the current y-coordinate of this item.
     * @param y the new current y-coordinate
     * @see #Y
     */
    public void setY(double y);
    
    /**
     * Get the starting x-coordinate of this item.
     * @return the starting x-coordinate
     * @see #STARTX
     */
    public double getStartX();
    
    /**
     * Set the starting x-coordinate of this item.
     * @param x the new starting x-coordinate
     * @see #STARTX
     */
    public void setStartX(double x);
    
    /**
     * Get the starting y-coordinate of this item.
     * @return the starting y-coordinate
     * @see #STARTY
     */
    public double getStartY();
    
    /**
     * Set the starting y-coordinate of this item.
     * @param y the new starting y-coordinate
     * @see #STARTY
     */
    public void setStartY(double y);
    
    /**
     * Get the ending x-coordinate of this item.
     * @return the ending x-coordinate
     * @see #ENDX
     */
    public double getEndX();
    
    /**
     * Set the ending x-coordinate of this item.
     * @param x the new ending x-coordinate
     * @see #ENDX
     */
    public void setEndX(double x);
    
    /**
     * Get the ending y-coordinate of this item.
     * @return the ending y-coordinate
     * @see #ENDY
     */
    public double getEndY();
    
    /**
     * Set the ending y-coordinate of this item.
     * @param y the new ending y-coordinate
     * @see #ENDY
     */
    public void setEndY(double y);

    /**
     * Get the bounding box for this VisualItem. If necessary, the bounds
     * will be validated before returning the bounding box.
     * @return the item's bounding box
     * @see #BOUNDS
     */
    public Rectangle2D getBounds();
    
    /**
     * Set the bounding box for this item. This method is used by Renderer
     * modules when the bounds are validated, or set by processing Actions
     * used in conjunction with Renderers that do not perform bounds
     * management.
     * @param x the minimum x-coordinate
     * @param y the minimum y-coorindate
     * @param w the width of this item
     * @param h the height of this item
     * @see #BOUNDS
     */
    public void setBounds(double x, double y, double w, double h);
    
    // ------------------------------------------------------------------------
    
    /**
     * Get the current stroke color of this item. The stroke color is used to
     * draw lines and the outlines of shapes. Color values as represented as an
     * integer containing the red, green, blue, and alpha (transparency)
     * color channels. A color with a zero alpha component is fully
     * transparent and will not be drawn.
     * @return the current stroke color, represented as an integer
     * @see prefuse.util.ColorLib
     * @see #STROKECOLOR
     */
    public int getStrokeColor();
    
    /**
     * Set the current stroke color of this item. The stroke color is used to
     * draw lines and the outlines of shapes. Color values as represented as an
     * integer containing the red, green, blue, and alpha (transparency)
     * color channels. A color with a zero alpha component is fully
     * transparent and will not be drawn.
     * @param color the current stroke color, represented as an integer
     * @see prefuse.util.ColorLib
     * @see #STROKECOLOR
     */
    public void setStrokeColor(int color);
    
    /**
     * Get the starting stroke color of this item. The stroke color is used to
     * draw lines and the outlines of shapes. Color values as represented as an
     * integer containing the red, green, blue, and alpha (transparency)
     * color channels. A color with a zero alpha component is fully
     * transparent and will not be drawn.
     * @return the starting stroke color, represented as an integer
     * @see prefuse.util.ColorLib
     * @see #STARTSTROKECOLOR
     */
    public int getStartStrokeColor();
    
    /**
     * Set the starting stroke color of this item. The stroke color is used to
     * draw lines and the outlines of shapes. Color values as represented as an
     * integer containing the red, green, blue, and alpha (transparency)
     * color channels. A color with a zero alpha component is fully
     * transparent and will not be drawn.
     * @param color the starting stroke color, represented as an integer
     * @see prefuse.util.ColorLib
     * @see #STARTSTROKECOLOR
     */
    public void setStartStrokeColor(int color);
    
    /**
     * Get the ending stroke color of this item. The stroke color is used to
     * draw lines and the outlines of shapes. Color values as represented as an
     * integer containing the red, green, blue, and alpha (transparency)
     * color channels. A color with a zero alpha component is fully
     * transparent and will not be drawn.
     * @return the ending stroke color, represented as an integer
     * @see prefuse.util.ColorLib
     * @see #ENDSTROKECOLOR
     */
    public int getEndStrokeColor();
    
    /**
     * Set the ending stroke color of this item. The stroke color is used to
     * draw lines and the outlines of shapes. Color values as represented as an
     * integer containing the red, green, blue, and alpha (transparency)
     * color channels. A color with a zero alpha component is fully
     * transparent and will not be drawn.
     * @param color the ending stroke color, represented as an integer
     * @see prefuse.util.ColorLib
     * @see #ENDSTROKECOLOR
     */
    public void setEndStrokeColor(int color);
    
    /**
     * Get the current fill color of this item. The fill color is used to
     * fill the interior of shapes. Color values as represented as an
     * integer containing the red, green, blue, and alpha (transparency)
     * color channels. A color with a zero alpha component is fully
     * transparent and will not be drawn.
     * @return the current fill color, represented as an integer
     * @see prefuse.util.ColorLib
     * @see #FILLCOLOR
     */
    public int getFillColor();
    
    /**
     * Set the current fill color of this item. The fill color is used to
     * fill the interior of shapes. Color values as represented as an
     * integer containing the red, green, blue, and alpha (transparency)
     * color channels. A color with a zero alpha component is fully
     * transparent and will not be drawn.
     * @param color the current fill color, represented as an integer
     * @see prefuse.util.ColorLib
     * @see #FILLCOLOR
     */
    public void setFillColor(int color);
    
    /**
     * Get the starting fill color of this item. The fill color is used to
     * fill the interior of shapes. Color values as represented as an
     * integer containing the red, green, blue, and alpha (transparency)
     * color channels. A color with zero alpha component is fully
     * transparent and will not be drawn.
     * @return the starting fill color, represented as an integer
     * @see prefuse.util.ColorLib
     * @see #STARTFILLCOLOR
     */
    public int getStartFillColor();

    /**
     * Set the starting fill color of this item. The fill color is used to
     * fill the interior of shapes. Color values as represented as an
     * integer containing the red, green, blue, and alpha (transparency)
     * color channels. A color with a zero alpha component is fully
     * transparent and will not be drawn.
     * @param color the starting fill color, represented as an integer
     * @see prefuse.util.ColorLib
     * @see #STARTFILLCOLOR
     */
    public void setStartFillColor(int color);
    
    /**
     * Get the ending fill color of this item. The fill color is used to
     * fill the interior of shapes. Color values as represented as an
     * integer containing the red, green, blue, and alpha (transparency)
     * color channels. A color with zero alpha component is fully
     * transparent and will not be drawn.
     * @return the ending fill color, represented as an integer
     * @see prefuse.util.ColorLib
     * @see #ENDFILLCOLOR
     */
    public int getEndFillColor();
    
    /**
     * Set the ending fill color of this item. The fill color is used to
     * fill the interior of shapes. Color values as represented as an
     * integer containing the red, green, blue, and alpha (transparency)
     * color channels. A color with a zero alpha component is fully
     * transparent and will not be drawn.
     * @param color the ending fill color, represented as an integer
     * @see prefuse.util.ColorLib
     * @see #ENDFILLCOLOR
     */
    public void setEndFillColor(int color);
    
    /**
     * Get the current text color of this item. The text color is used to
     * draw text strings for the item. Color values as represented as an
     * integer containing the red, green, blue, and alpha (transparency)
     * color channels. A color with zero alpha component is fully
     * transparent and will not be drawn.
     * @return the current text color, represented as an integer
     * @see prefuse.util.ColorLib
     * @see #TEXTCOLOR
     */
    public int getTextColor();
    
    /**
     * Set the current text color of this item. The text color is used to
     * draw text strings for the item. Color values as represented as an
     * integer containing the red, green, blue, and alpha (transparency)
     * color channels. A color with a zero alpha component is fully
     * transparent and will not be drawn.
     * @param color the current text color, represented as an integer
     * @see prefuse.util.ColorLib
     * @see #TEXTCOLOR
     */
    public void setTextColor(int color);
    
    /**
     * Get the starting text color of this item. The text color is used to
     * draw text strings for the item. Color values as represented as an
     * integer containing the red, green, blue, and alpha (transparency)
     * color channels. A color with zero alpha component is fully
     * transparent and will not be drawn.
     * @return the starting text color, represented as an integer
     * @see prefuse.util.ColorLib
     * @see #STARTTEXTCOLOR
     */
    public int getStartTextColor();
    
    /**
     * Set the starting text color of this item. The text color is used to
     * draw text strings for the item. Color values as represented as an
     * integer containing the red, green, blue, and alpha (transparency)
     * color channels. A color with a zero alpha component is fully
     * transparent and will not be drawn.
     * @param color the starting text color, represented as an integer
     * @see prefuse.util.ColorLib
     * @see #STARTTEXTCOLOR
     */
    public void setStartTextColor(int color);
    
    /**
     * Get the ending text color of this item. The text color is used to
     * draw text strings for the item. Color values as represented as an
     * integer containing the red, green, blue, and alpha (transparency)
     * color channels. A color with zero alpha component is fully
     * transparent and will not be drawn.
     * @return the ending text color, represented as an integer
     * @see prefuse.util.ColorLib
     * @see #ENDTEXTCOLOR
     */
    public int getEndTextColor();
    
    /**
     * Set the ending text color of this item. The text color is used to
     * draw text strings for the item. Color values as represented as an
     * integer containing the red, green, blue, and alpha (transparency)
     * color channels. A color with a zero alpha component is fully
     * transparent and will not be drawn.
     * @param color the ending text color, represented as an integer
     * @see prefuse.util.ColorLib
     * @see #ENDTEXTCOLOR
     */
    public void setEndTextColor(int color);
    
    // ------------------------------------------------------------------------
    
    /**
     * Get the current size value of the item. Size values are typically used
     * to scale an item, either in one-dimension (e.g., a bar chart length) or
     * two-dimensions (e.g., using pixel area to encode a quantitative value).
     * @return the current size value
     * @see #SIZE
     */
    public double getSize();
    
    /**
     * Set the current size value of the item. Size values are typically used
     * to scale an item, either in one-dimension (e.g., a bar chart length) or
     * two-dimensions (e.g., using pixel area to encode a quantitative value).
     * @param size the current size value
     * @see #SIZE
     */
    public void setSize(double size);
    
    /**
     * Get the starting size value of the item. Size values are typically used
     * to scale an item, either in one-dimension (e.g., a bar chart length) or
     * two-dimensions (e.g., using pixel area to encode a quantitative value).
     * @return the starting size value
     * @see #STARTSIZE
     */
    public double getStartSize();
    
    /**
     * Set the starting size value of the item. Size values are typically used
     * to scale an item, either in one-dimension (e.g., a bar chart length) or
     * two-dimensions (e.g., using pixel area to encode a quantitative value).
     * @param size the starting size value
     * @see #STARTSIZE
     */
    public void setStartSize(double size);
    
    /**
     * Get the ending size value of the item. Size values are typically used
     * to scale an item, either in one-dimension (e.g., a bar chart length) or
     * two-dimensions (e.g., using pixel area to encode a quantitative value).
     * @return the ending size value
     * @see #ENDSIZE
     */
    public double getEndSize();
    
    /**
     * Set the ending size value of the item. Size values are typically used
     * to scale an item, either in one-dimension (e.g., a bar chart length) or
     * two-dimensions (e.g., using pixel area to encode a quantitative value).
     * @param size the ending size value
     * @see #ENDSIZE
     */
    public void setEndSize(double size);
    
    // ------------------------------------------------------------------------
    
    /**
     * Get the current shape value of the item. One of the SHAPE constants
     * included in the {@link prefuse.Constants} class. This value only has an
     * effect if a Renderer that supports different shapes is used
     * (e.g., {@link prefuse.render.ShapeRenderer}.
     * @return the current shape value
     * @see #SHAPE
     */
    public int getShape();
    
    /**
     * Set the current shape value of the item. One of the SHAPE constants
     * included in the {@link prefuse.Constants} class. This value only has an
     * effect if a Renderer that supports different shapes is used
     * (e.g., {@link prefuse.render.ShapeRenderer}.
     * @param shape the shape value to use
     * @see #SHAPE
     */
    public void setShape(int shape);
    
    // ------------------------------------------------------------------------
    
    /**
     * Get the current stroke used to draw lines and shape outlines.
     * @return the stroke used to draw lines and shape outlines
     */
    public BasicStroke getStroke();
    
    /**
     * Set the current stroke used to draw lines and shape outlines.
     * @param stroke the stroke to use to draw lines and shape outlines
     */
    public void setStroke(BasicStroke stroke);
    
    // ------------------------------------------------------------------------
    
    /**
     * Get the current font for the item. The font is used as the default
     * typeface for drawing text for this item.
     * @return the current font value
     * @see #FONT
     */
    public Font getFont();

    /**
     * Set the current font for the item. The font is used as the default
     * typeface for drawing text for this item.
     * @param font the current font value
     * @see #FONT
     */
    public void setFont(Font font);
    
    /**
     * Get the starting font for the item. The font is used as the default
     * typeface for drawing text for this item.
     * @return the starting font value
     * @see #STARTFONT
     */
    public Font getStartFont();

    /**
     * Set the starting font for the item. The font is used as the default
     * typeface for drawing text for this item.
     * @param font the starting font value
     * @see #STARTFONT
     */
    public void setStartFont(Font font);
    
    /**
     * Get the ending font for the item. The font is used as the default
     * typeface for drawing text for this item.
     * @return the ending font value
     * @see #ENDFONT
     */
    public Font getEndFont();
    
    /**
     * Set the ending font for the item. The font is used as the default
     * typeface for drawing text for this item.
     * @param font the ending font value
     * @see #ENDFONT
     */
    public void setEndFont(Font font);
    
    // ------------------------------------------------------------------------
    
    /**
     * Get the degree-of-interest (DOI) value. The degree-of-interet is an
     * optional value that can be used to sort items by importance, control
     * item visibility, or influence particular visual encodings. A common
     * example is to use the DOI to store the graph distance of a node from
     * the nearest selected focus node.
     * @return the DOI value of this item
     * @see #DOI
     */
    public double getDOI();
    
    /**
     * Set the degree-of-interest (DOI) value. The degree-of-interet is an
     * optional value that can be used to sort items by importance, control
     * item visibility, or influence particular visual encodings. A common
     * example is to use the DOI to store the graph distance of a node from
     * the nearest selected focus node.
     * @param doi the DOI value of this item
     * @see #DOI
     */
    public void setDOI(double doi);
    
    // ------------------------------------------------------------------------
    // VisualItem Base Schema
        
    /** The validated data field */
    public static final String VALIDATED
        = PrefuseLib.FIELD_PREFIX+"validated";
    /** The visible data field */
    public static final String VISIBLE
        = PrefuseLib.FIELD_PREFIX+"visible";
    /** The start visible data field */
    public static final String STARTVISIBLE
        = PrefuseLib.getStartField(VISIBLE);
    /** The end visible data field */
    public static final String ENDVISIBLE
        = PrefuseLib.getEndField(VISIBLE);
    /** The interactive data field */
    public static final String INTERACTIVE
        = PrefuseLib.FIELD_PREFIX+"interactive";
    /** The expanded data field */
    public static final String EXPANDED
        = PrefuseLib.FIELD_PREFIX+"expanded";
    /** The fixed data field */
    public static final String FIXED
        = PrefuseLib.FIELD_PREFIX+"fixed";
    /** The highlight data field */
    public static final String HIGHLIGHT
        = PrefuseLib.FIELD_PREFIX+"highlight";
    /** The hover data field */
    public static final String HOVER
        = PrefuseLib.FIELD_PREFIX+"hover";
    /** The x data field */
    public static final String X
        = PrefuseLib.FIELD_PREFIX+"x";
    /** The y data field */
    public static final String Y
        = PrefuseLib.FIELD_PREFIX+"y";
    /** The start x data field */
    public static final String STARTX
        = PrefuseLib.getStartField(X);
    /** The start y data field */
    public static final String STARTY
        = PrefuseLib.getStartField(Y);
    /** The end x data field */
    public static final String ENDX
        = PrefuseLib.getEndField(X);
    /** The end y data field */
    public static final String ENDY
        = PrefuseLib.getEndField(Y);
    /** The bounds data field */
    public static final String BOUNDS
        = PrefuseLib.FIELD_PREFIX+"bounds";
    /** The stroke color data field */
    public static final String STROKECOLOR
        = PrefuseLib.FIELD_PREFIX+"strokeColor";
    /** The start stroke color data field */
    public static final String STARTSTROKECOLOR
        = PrefuseLib.getStartField(STROKECOLOR);
    /** The end stroke color data field */
    public static final String ENDSTROKECOLOR
        = PrefuseLib.getEndField(STROKECOLOR);
    /** The fill color data field */
    public static final String FILLCOLOR
        = PrefuseLib.FIELD_PREFIX+"fillColor";
    /** The start fill color data field */
    public static final String STARTFILLCOLOR
        = PrefuseLib.getStartField(FILLCOLOR);
    /** The end fill color data field */
    public static final String ENDFILLCOLOR
        = PrefuseLib.getEndField(FILLCOLOR);
    /** The text color data field */
    public static final String TEXTCOLOR
        = PrefuseLib.FIELD_PREFIX+"textColor";
    /** The start text color data field */
    public static final String STARTTEXTCOLOR
        = PrefuseLib.getStartField(TEXTCOLOR);
    /** The end text color data field */
    public static final String ENDTEXTCOLOR
        = PrefuseLib.getEndField(TEXTCOLOR);
    /** The size data field */
    public static final String SIZE
        = PrefuseLib.FIELD_PREFIX+"size";
    /** The start size data field */
    public static final String STARTSIZE
        = PrefuseLib.getStartField(SIZE);
    /** The end size data field */
    public static final String ENDSIZE
        = PrefuseLib.getEndField(SIZE);
    /** The shape data field */
    public static final String SHAPE
        = PrefuseLib.FIELD_PREFIX+"shape";
    /** The stroke data field */
    public static final String STROKE
        = PrefuseLib.FIELD_PREFIX+"stroke";
    /** The font data field */
    public static final String FONT
        = PrefuseLib.FIELD_PREFIX+"font";
    /** The start font data field */
    public static final String STARTFONT
        = PrefuseLib.getStartField(FONT);
    /** The end font data field */
    public static final String ENDFONT
        = PrefuseLib.getEndField(FONT);
    /** The doi data field */
    public static final String DOI
        = PrefuseLib.FIELD_PREFIX+"doi";
    
    /** A permanent, locked copy of the base VisualItem schema */
    public static final Schema SCHEMA
        = PrefuseLib.getVisualItemSchema().lockSchema();
    
    /** 
     *  The column index of the validated data field. This should be safe to do
     *  because a unique (non-inherited) validated field is required of all
     *  VisualItems, and should always be the first field included.
     */
    public static final int IDX_VALIDATED 
        = SCHEMA.getColumnIndex(VALIDATED);
    
    // ------------------------------------------------------------------------
    // VisualItem Extended Schema
    
    /** The label data field.
     * Not included in the VisualItem schema by default */
    public static final String LABEL
        = PrefuseLib.FIELD_PREFIX+"label";
    /** The value data field.
     * Not included in the VisualItem schema by default */
    public static final String VALUE
        = PrefuseLib.FIELD_PREFIX+"value";
    
    /** The polygon data field.
     * Not included in the VisualItem schema by default */
    public static final String POLYGON
        = PrefuseLib.FIELD_PREFIX+"polygon";
    /** The start polygon data field.
     * Not included in the VisualItem schema by default */
    public static final String STARTPOLYGON
        = PrefuseLib.getStartField(POLYGON);
    /** The end polygon data field.
     * Not included in the VisualItem schema by default */
    public static final String ENDPOLYGON
        = PrefuseLib.getEndField(POLYGON);
    
    /** The x2 data field.
     * Not included in the VisualItem schema by default */
    public static final String X2
        = PrefuseLib.FIELD_PREFIX+"x2";
    /** The y2 data field.
     * Not included in the VisualItem schema by default */
    public static final String Y2
        = PrefuseLib.FIELD_PREFIX+"y2";
    /** The start x2 data field.
     * Not included in the VisualItem schema by default */
    public static final String STARTX2
        = PrefuseLib.getStartField(X2);
    /** The start y2 data field.
     * Not included in the VisualItem schema by default */
    public static final String STARTY2
        = PrefuseLib.getStartField(Y2);
    /** The end x2 data field.
     * Not included in the VisualItem schema by default */
    public static final String ENDX2
        = PrefuseLib.getEndField(X2);
    /** The end y2 data field.
     * Not included in the VisualItem schema by default */
    public static final String ENDY2
        = PrefuseLib.getEndField(Y2);
    
} // end of interface VisualItem
