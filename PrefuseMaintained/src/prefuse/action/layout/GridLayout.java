package prefuse.action.layout;

import java.awt.geom.Rectangle2D;
import java.util.Iterator;

import prefuse.data.Node;
import prefuse.data.tuple.TupleSet;
import prefuse.visual.VisualItem;


/**
 * Implements a uniform grid-based layout. This component can either use
 * preset grid dimensions or analyze a grid-shaped graph to determine them
 * automatically.
 * 
 * @author <a href="http://jheer.org">jeffrey heer</a>
 */
public class GridLayout extends Layout {

    protected int rows;
    protected int cols;
    protected boolean analyze = false;
    
    /**
     * Create a new GridLayout without preset dimensions. The layout will
     * attempt to analyze an input graph to determine grid parameters.
     * @param group the data group to layout. In this automatic grid
     * analysis configuration, the group <b>must</b> resolve to a set of
     * graph nodes.
     */
    public GridLayout(String group) {
        super(group);
        analyze = true;
    }
    
    /**
     * Create a new GridLayout using the specified grid dimensions. If the
     * input data has more elements than the grid dimensions can hold, the
     * left over elements will not be visible.
     * @param group the data group to layout
     * @param nrows the number of rows of the grid
     * @param ncols the number of columns of the grid
     */
    public GridLayout(String group, int nrows, int ncols) {
        super(group);
        rows = nrows;
        cols = ncols;
        analyze = false;
    }
    
    /**
     * @see prefuse.action.Action#run(double)
     */
    public void run(double frac) {
        Rectangle2D b = getLayoutBounds();
        double bx = b.getMinX(), by = b.getMinY();
        double w = b.getWidth(), h = b.getHeight();
        
        TupleSet ts = m_vis.getGroup(m_group);
        int m = rows, n = cols;
        if ( analyze ) {
            int[] d = analyzeGraphGrid(ts);
            m = d[0]; n = d[1];
        }
        
        Iterator iter = ts.tuples();
        // layout grid contents
        for ( int i=0; iter.hasNext() && i < m*n; ++i ) {
            VisualItem item = (VisualItem)iter.next();
            item.setVisible(true);
            double x = bx + w*((i%n)/(double)(n-1));
            double y = by + h*((i/n)/(double)(m-1));
            setX(item,null,x);
            setY(item,null,y);
        }
        // set left-overs invisible
        while ( iter.hasNext() ) {
            VisualItem item = (VisualItem)iter.next();
            item.setVisible(false);
        }
    }
    
    /**
     * Analyzes a set of nodes to try and determine grid dimensions. Currently
     * looks for the edge count on a node to drop to 2 to determine the end of
     * a row.
     * @param ts TupleSet ts a set of nodes to analyze. Contained tuples
     * <b>must</b> implement be Node instances.
     * @return a two-element int array with the row and column lengths
     */
    public static int[] analyzeGraphGrid(TupleSet ts) {
        // TODO: more robust grid analysis?
        int m, n;
        Iterator iter = ts.tuples(); iter.next();
        for ( n=2; iter.hasNext(); n++ ) {
            Node nd = (Node)iter.next();
            if ( nd.getDegree() == 2 )
                break;
        }
        m = ts.getTupleCount() / n;
        return new int[] {m,n};
    }
    
    /**
     * Get the number of grid columns.
     * @return the number of grid columns
     */
    public int getNumCols() {
        return cols;
    }
    
    /**
     * Set the number of grid columns.
     * @param cols the number of grid columns to use
     */
    public void setNumCols(int cols) {
        this.cols = cols;
    }
    
    /**
     * Get the number of grid rows.
     * @return the number of grid rows
     */
    public int getNumRows() {
        return rows;
    }
    
    /**
     * Set the number of grid rows.
     * @param rows the number of grid rows to use
     */
    public void setNumRows(int rows) {
        this.rows = rows;
    }
    
} // end of class GridLayout
