package prefuse.visual.tuple;

import java.awt.BasicStroke;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.geom.Rectangle2D;

import prefuse.Visualization;
import prefuse.data.Graph;
import prefuse.data.Table;
import prefuse.data.Tuple;
import prefuse.data.tuple.TableTuple;
import prefuse.data.tuple.TupleSet;
import prefuse.render.Renderer;
import prefuse.visual.VisualItem;
import prefuse.visual.VisualTable;

/**
 * VisualItem implementation that uses data values from a backing VisualTable.
 * 
 * @author <a href="http://jheer.org">jeffrey heer</a>
 */
public class TableVisualItem extends TableTuple implements VisualItem {

    /**
     * Initialize a new TableVisualItem for the given table and row. This
     * method is used by the appropriate TupleManager instance, and should
     * not be called directly by client code, unless by a client-supplied
     * custom TupleManager.
     * @param table the data Table
     * @param graph ignored by this class
     * @param row the table row index
     */
    protected void init(Table table, Graph graph, int row) {
        m_table = table;
        m_row = m_table.isValidRow(row) ? row : -1;
    }
    
    /**
     * @see prefuse.visual.VisualItem#getVisualization()
     */
    public Visualization getVisualization() {
        return ((VisualTable)m_table).getVisualization();
    }
    
    /**
     * @see prefuse.visual.VisualItem#getGroup()
     */
    public String getGroup() {
        return ((VisualTable)m_table).getGroup();
    }
    
    /**
     * @see prefuse.visual.VisualItem#isInGroup(java.lang.String)
     */
    public boolean isInGroup(String group) {
        return getVisualization().isInGroup(this, group);
    }
    
    /**
     * @see prefuse.visual.VisualItem#getSourceData()
     */
    public TupleSet getSourceData() {
        VisualTable vt = (VisualTable)m_table;
        return vt.getVisualization().getSourceData(vt.getGroup());
    }

    /**
     * @see prefuse.visual.VisualItem#getSourceTuple()
     */
    public Tuple getSourceTuple() {
        VisualTable vt = (VisualTable)m_table;
        return vt.getVisualization().getSourceTuple(this);
    }
    
    /**
     * @see java.lang.Object#toString()
     */
    public String toString() {
        StringBuffer sbuf = new StringBuffer();
        sbuf.append("VisualItem[").append(getGroup());
        sbuf.append(",").append(m_row).append(',');
        VisualTable vt = (VisualTable)m_table;
        int local = vt.getLocalColumnCount();
        int inherited = vt.getColumnCount()-local;
        for ( int i=0; i<inherited; ++i ) {
            if ( i > 0 ) sbuf.append(',');
            String name = vt.getColumnName(local+i);
            sbuf.append(name);
            sbuf.append('=');
            if ( vt.canGetString(name) )
                sbuf.append(vt.getString(m_row, name));
            else
                sbuf.append(vt.get(m_row, name).toString());
        }
        sbuf.append(']');
        
        return sbuf.toString();
    }
    
    // ------------------------------------------------------------------------
    // VisualItem Methods
    
    /**
     * @see prefuse.visual.VisualItem#render(java.awt.Graphics2D)
     */
    public void render(Graphics2D g) {
        getRenderer().render(g, this);
    }
    
    /**
     * @see prefuse.visual.VisualItem#getRenderer()
     */
    public Renderer getRenderer() {
        return getVisualization().getRenderer(this);
    }
    
    /**
     * @see prefuse.visual.VisualItem#validateBounds()
     */
    public Rectangle2D validateBounds() {
        if ( isValidated() ) return getBounds();
        
        Visualization v = getVisualization();
                
        // set the new bounds from the renderer and validate
        getRenderer().setBounds(this);
        setValidated(true);
        
        // report damage from the new bounds and return
        Rectangle2D bounds = getBounds();
        v.damageReport(this, bounds);
        return bounds;
    }
    
    // -- Boolean Flags -------------------------------------------------------
       
    /**
     * @see prefuse.visual.VisualItem#isValidated()
     */
    public boolean isValidated() {
        return ((VisualTable)m_table).isValidated(m_row);
    }
    
    /**
     * @see prefuse.visual.VisualItem#setValidated(boolean)
     */
    public void setValidated(boolean value) {
        ((VisualTable)m_table).setValidated(m_row, value);
    }
    
    /**
     * @see prefuse.visual.VisualItem#isVisible()
     */
    public boolean isVisible() {
        return ((VisualTable)m_table).isVisible(m_row);
    }
    
    /**
     * @see prefuse.visual.VisualItem#setVisible(boolean)
     */
    public void setVisible(boolean value) {
        ((VisualTable)m_table).setVisible(m_row, value);
    }
    
    /**
     * @see prefuse.visual.VisualItem#isStartVisible()
     */
    public boolean isStartVisible() {
        return ((VisualTable)m_table).isStartVisible(m_row);
    }
    
    /**
     * @see prefuse.visual.VisualItem#setStartVisible(boolean)
     */
    public void setStartVisible(boolean value) {
        ((VisualTable)m_table).setStartVisible(m_row, value);
    }
    
    /**
     * @see prefuse.visual.VisualItem#isEndVisible()
     */
    public boolean isEndVisible() {
        return ((VisualTable)m_table).isEndVisible(m_row);
    }
    
    /**
     * @see prefuse.visual.VisualItem#setEndVisible(boolean)
     */
    public void setEndVisible(boolean value) {
        ((VisualTable)m_table).setEndVisible(m_row, value);
    }
    
    /**
     * @see prefuse.visual.VisualItem#isInteractive()
     */
    public boolean isInteractive() {
        return ((VisualTable)m_table).isInteractive(m_row);
    }

    /**
     * @see prefuse.visual.VisualItem#setInteractive(boolean)
     */
    public void setInteractive(boolean value) {
        ((VisualTable)m_table).setInteractive(m_row, value);
    }
    
    /**
     * @see prefuse.visual.VisualItem#isExpanded()
     */
    public boolean isExpanded() {
        return ((VisualTable)m_table).isExpanded(m_row);
    }

    
    /**
     * @see prefuse.visual.VisualItem#setExpanded(boolean)
     */
    public void setExpanded(boolean value) {
        ((VisualTable)m_table).setExpanded(m_row, value);
    }
    
    /**
     * @see prefuse.visual.VisualItem#isFixed()
     */
    public boolean isFixed() {
        return ((VisualTable)m_table).isFixed(m_row);
    }

    /**
     * @see prefuse.visual.VisualItem#setFixed(boolean)
     */
    public void setFixed(boolean value) {
        ((VisualTable)m_table).setFixed(m_row, value);
    }
    
    /**
     * @see prefuse.visual.VisualItem#isHighlighted()
     */
    public boolean isHighlighted() {
        return ((VisualTable)m_table).isHighlighted(m_row);
    }
    
    /**
     * @see prefuse.visual.VisualItem#setHighlighted(boolean)
     */
    public void setHighlighted(boolean value) {
        ((VisualTable)m_table).setHighlighted(m_row, value);
    }

    /**
     * @see prefuse.visual.VisualItem#isHover()
     */
    public boolean isHover() {
        return ((VisualTable)m_table).isHover(m_row);
    }
    
    /**
     * @see prefuse.visual.VisualItem#setHover(boolean)
     */
    public void setHover(boolean value) {
        ((VisualTable)m_table).setHover(m_row, value);
    }
    
    // ------------------------------------------------------------------------
    
    /**
     * @see prefuse.visual.VisualItem#getX()
     */
    public double getX() {
        return ((VisualTable)m_table).getX(m_row);
    }
    
    /**
     * @see prefuse.visual.VisualItem#setX(double)
     */
    public void setX(double x) {
        ((VisualTable)m_table).setX(m_row, x);
    }
    
    /**
     * @see prefuse.visual.VisualItem#getY()
     */
    public double getY() {
        return ((VisualTable)m_table).getY(m_row);
    }
    
    /**
     * @see prefuse.visual.VisualItem#setY(double)
     */
    public void setY(double y) {
        ((VisualTable)m_table).setY(m_row, y);
    }
    
    /**
     * @see prefuse.visual.VisualItem#getStartX()
     */
    public double getStartX() {
        return ((VisualTable)m_table).getStartX(m_row);
    }
    
    /**
     * @see prefuse.visual.VisualItem#setStartX(double)
     */
    public void setStartX(double x) {
        ((VisualTable)m_table).setStartX(m_row, x);
    }
    
    /**
     * @see prefuse.visual.VisualItem#getStartY()
     */
    public double getStartY() {
        return ((VisualTable)m_table).getStartY(m_row);
    }
    
    /**
     * @see prefuse.visual.VisualItem#setStartY(double)
     */
    public void setStartY(double y) {
        ((VisualTable)m_table).setStartY(m_row, y);
    }
    
    /**
     * @see prefuse.visual.VisualItem#getEndX()
     */
    public double getEndX() {
        return ((VisualTable)m_table).getEndX(m_row);
    }
    
    /**
     * @see prefuse.visual.VisualItem#setEndX(double)
     */
    public void setEndX(double x) {
        ((VisualTable)m_table).setEndX(m_row, x);
    }
    
    /**
     * @see prefuse.visual.VisualItem#getEndY()
     */
    public double getEndY() {
        return ((VisualTable)m_table).getEndY(m_row);
    }
    
    /**
     * @see prefuse.visual.VisualItem#setEndY(double)
     */
    public void setEndY(double y) {
        ((VisualTable)m_table).setEndY(m_row, y);
    }

    /**
     * @see prefuse.visual.VisualItem#getBounds()
     */
    public Rectangle2D getBounds() {
        if ( !isValidated() ) {
            return validateBounds();
        }
        return ((VisualTable)m_table).getBounds(m_row);
    }
    
    /**
     * @see prefuse.visual.VisualItem#setBounds(double, double, double, double)
     */
    public void setBounds(double x, double y, double w, double h) {
        ((VisualTable)m_table).setBounds(m_row, x, y, w, h);
    }
    
    // ------------------------------------------------------------------------
    
    /**
     * @see prefuse.visual.VisualItem#getStrokeColor()
     */
    public int getStrokeColor() {
        return ((VisualTable)m_table).getStrokeColor(m_row);
    }
    
    /**
     * @see prefuse.visual.VisualItem#setStrokeColor(int)
     */
    public void setStrokeColor(int color) {
        ((VisualTable)m_table).setStrokeColor(m_row, color);
    }
    
    /**
     * @see prefuse.visual.VisualItem#getStartStrokeColor()
     */
    public int getStartStrokeColor() {
        return ((VisualTable)m_table).getStartStrokeColor(m_row);
    }
    
    /**
     * @see prefuse.visual.VisualItem#setStartStrokeColor(int)
     */
    public void setStartStrokeColor(int color) {
        ((VisualTable)m_table).setStartStrokeColor(m_row, color);
    }
    
    /**
     * @see prefuse.visual.VisualItem#getEndStrokeColor()
     */
    public int getEndStrokeColor() {
        return ((VisualTable)m_table).getEndStrokeColor(m_row);
    }
    
    /**
     * @see prefuse.visual.VisualItem#setEndStrokeColor(int)
     */
    public void setEndStrokeColor(int color) {
        ((VisualTable)m_table).setEndStrokeColor(m_row, color);
    }
    
    /**
     * @see prefuse.visual.VisualItem#getFillColor()
     */
    public int getFillColor() {
        return ((VisualTable)m_table).getFillColor(m_row);
    }
    
    /**
     * @see prefuse.visual.VisualItem#setFillColor(int)
     */
    public void setFillColor(int color) {
        ((VisualTable)m_table).setFillColor(m_row, color);
    }
    
    /**
     * @see prefuse.visual.VisualItem#getStartFillColor()
     */
    public int getStartFillColor() {
        return ((VisualTable)m_table).getStartFillColor(m_row);
    }

    /**
     * @see prefuse.visual.VisualItem#setStartFillColor(int)
     */
    public void setStartFillColor(int color) {
        ((VisualTable)m_table).setStartFillColor(m_row, color);
    }
    
    /**
     * @see prefuse.visual.VisualItem#getEndFillColor()
     */
    public int getEndFillColor() {
        return ((VisualTable)m_table).getEndFillColor(m_row);
    }
    
    /**
     * @see prefuse.visual.VisualItem#setEndFillColor(int)
     */
    public void setEndFillColor(int color) {
        ((VisualTable)m_table).setEndFillColor(m_row, color);
    }
    
    /**
     * @see prefuse.visual.VisualItem#getTextColor()
     */
    public int getTextColor() {
        return ((VisualTable)m_table).getTextColor(m_row);
    }
    
    /**
     * @see prefuse.visual.VisualItem#setTextColor(int)
     */
    public void setTextColor(int color) {
        ((VisualTable)m_table).setTextColor(m_row, color);
    }
    
    /**
     * @see prefuse.visual.VisualItem#getStartTextColor()
     */
    public int getStartTextColor() {
        return ((VisualTable)m_table).getStartTextColor(m_row);
    }
    
    /**
     * @see prefuse.visual.VisualItem#setStartTextColor(int)
     */
    public void setStartTextColor(int color) {
        ((VisualTable)m_table).setStartTextColor(m_row, color);
    }
    
    /**
     * @see prefuse.visual.VisualItem#getEndTextColor()
     */
    public int getEndTextColor() {
        return ((VisualTable)m_table).getEndTextColor(m_row);
    }
    
    /**
     * @see prefuse.visual.VisualItem#setEndTextColor(int)
     */
    public void setEndTextColor(int color) {
        ((VisualTable)m_table).setEndTextColor(m_row, color);
    }
    
    // ------------------------------------------------------------------------
    
    /**
     * @see prefuse.visual.VisualItem#getSize()
     */
    public double getSize() {
        return ((VisualTable)m_table).getSize(m_row);
    }
    
    /**
     * @see prefuse.visual.VisualItem#setSize(double)
     */
    public void setSize(double size) {
        ((VisualTable)m_table).setSize(m_row, size);
    }
    
    /**
     * @see prefuse.visual.VisualItem#getStartSize()
     */
    public double getStartSize() {
        return ((VisualTable)m_table).getStartSize(m_row);
    }
    
    /**
     * @see prefuse.visual.VisualItem#setStartSize(double)
     */
    public void setStartSize(double size) {
        ((VisualTable)m_table).setStartSize(m_row, size);
    }
    
    /**
     * @see prefuse.visual.VisualItem#getEndSize()
     */
    public double getEndSize() {
        return ((VisualTable)m_table).getEndSize(m_row);
    }
    
    /**
     * @see prefuse.visual.VisualItem#setEndSize(double)
     */
    public void setEndSize(double size) {
        ((VisualTable)m_table).setEndSize(m_row, size);
    }
    
    // ------------------------------------------------------------------------
    
    /**
     * @see prefuse.visual.VisualItem#getShape()
     */
    public int getShape() {
        return ((VisualTable)m_table).getShape(m_row);
    }
    
    /**
     * @see prefuse.visual.VisualItem#setShape(int)
     */
    public void setShape(int shape) {
        ((VisualTable)m_table).setShape(m_row, shape);
    }
    
    // ------------------------------------------------------------------------
    
    /**
     * @see prefuse.visual.VisualItem#getStroke()
     */
    public BasicStroke getStroke() {
        return ((VisualTable)m_table).getStroke(m_row);
    }
    
    /**
     * @see prefuse.visual.VisualItem#setStroke(java.awt.BasicStroke)
     */
    public void setStroke(BasicStroke stroke) {
        ((VisualTable)m_table).setStroke(m_row, stroke);
    }
    
    // ------------------------------------------------------------------------    
    
    /**
     * @see prefuse.visual.VisualItem#getFont()
     */
    public Font getFont() {
        return ((VisualTable)m_table).getFont(m_row);
    }

    /**
     * @see prefuse.visual.VisualItem#setFont(java.awt.Font)
     */
    public void setFont(Font font) {
        ((VisualTable)m_table).setFont(m_row, font);
    }
    
    /**
     * @see prefuse.visual.VisualItem#getStartFont()
     */
    public Font getStartFont() {
        return ((VisualTable)m_table).getStartFont(m_row);
    }

    /**
     * @see prefuse.visual.VisualItem#setStartFont(java.awt.Font)
     */
    public void setStartFont(Font font) {
        ((VisualTable)m_table).setStartFont(m_row, font);
    }
    
    /**
     * @see prefuse.visual.VisualItem#getEndFont()
     */
    public Font getEndFont() {
        return ((VisualTable)m_table).getEndFont(m_row);
    }
    
    /**
     * @see prefuse.visual.VisualItem#setEndFont(java.awt.Font)
     */
    public void setEndFont(Font font) {
        ((VisualTable)m_table).setEndFont(m_row, font);
    }
    
    // ------------------------------------------------------------------------
    
    /**
     * @see prefuse.visual.VisualItem#getDOI()
     */
    public double getDOI() {
        return ((VisualTable)m_table).getDOI(m_row);
    }
    
    /**
     * @see prefuse.visual.VisualItem#setDOI(double)
     */
    public void setDOI(double doi) {
        ((VisualTable)m_table).setDOI(m_row, doi);
    }
    
} // end of class TableVisualItem
