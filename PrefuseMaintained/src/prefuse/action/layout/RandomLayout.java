package prefuse.action.layout;

import java.awt.geom.Rectangle2D;
import java.util.Iterator;
import java.util.Random;

import prefuse.visual.VisualItem;


/**
 * Performs a random layout of items within the layout bounds.
 *
 * @author <a href="http://jheer.org">jeffrey heer</a>
 */
public class RandomLayout extends Layout {

    private Random r = new Random(12345678L);
    
    /**
     * Create a new RandomLayout that processes all items.
     */
    public RandomLayout() {
        super();
    }
    
    /**
     * Create a new RandomLayout.
     * @param group the data group to layout
     */
    public RandomLayout(String group) {
        super(group);
    }

    /**
     * Set the seed value for the random number generator.
     * @param seed the random seed value
     */
    public void setRandomSeed(long seed) {
        r.setSeed(seed);
    }
    
    /**
     * @see prefuse.action.Action#run(double)
     */
    public void run(double frac) {
        Rectangle2D b = getLayoutBounds();
        double x, y;
        double w = b.getWidth();
        double h = b.getHeight();
        Iterator iter = getVisualization().visibleItems(m_group);
        while ( iter.hasNext() ) {
            VisualItem item = (VisualItem)iter.next();
            x = (int)(b.getX() + r.nextDouble()*w);
            y = (int)(b.getY() + r.nextDouble()*h);
            setX(item,null,x);
            setY(item,null,y);
        }
    }

} // end of class RandomLayout
