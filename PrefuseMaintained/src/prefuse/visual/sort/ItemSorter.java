package prefuse.visual.sort;

import java.util.Comparator;

import prefuse.Visualization;
import prefuse.visual.AggregateItem;
import prefuse.visual.DecoratorItem;
import prefuse.visual.EdgeItem;
import prefuse.visual.VisualItem;

/**
 * ItemSorter instances provide an integer score for each VisualItem;
 * these scores are then used to sort the items in ascending order of score.
 * ItemSorters are used to determine the rendering order of items in a
 * Display.
 * 
 * @author <a href="http://jheer.org">jeffrey heer</a>
 */
public class ItemSorter implements Comparator {

    protected static final int AGGREGATE = 0;
    protected static final int EDGE      = 1;
    protected static final int ITEM      = 2;
    protected static final int DECORATOR = 3;
    
    /**
     * <p>Return an ordering score for an item. The default scoring imparts
     * the following order:
     * hover items > highlighted items > items in the
     * {@link prefuse.Visualization#FOCUS_ITEMS} set >
     * {@link prefuse.Visualization#SEARCH_ITEMS} set >
     * DecoratorItem instances > normal VisualItem instances. A zero
     * score is returned for normal items, with scores starting at
     * 1&lt;&lt;27 for other items, leaving the number range beneath that
     * value open for additional nuanced scoring.</p> 
     * 
     * <p>Subclasses can override this method to provide custom sorting
     * criteria.</p>
     * @param item the VisualItem to provide an ordering score
     * @return the ordering score
     */
    public int score(VisualItem item) {
        int type = ITEM;
        if ( item instanceof EdgeItem ) {
            type = EDGE;
        } else if ( item instanceof AggregateItem ) {
            type = AGGREGATE;
        } else if ( item instanceof DecoratorItem ) {
            type = DECORATOR;
        }
        
        int score = (1<<(26+type));
        if ( item.isHover() ) {
            score += (1<<25);
        }
        if ( item.isHighlighted() ) {
            score += (1<<24);
        }
        if ( item.isInGroup(Visualization.FOCUS_ITEMS) ) {
            score += (1<<23);
        }
        if ( item.isInGroup(Visualization.SEARCH_ITEMS) ) {
            score += (1<<22);
        }

        return score;
    }
    
    /**
     * Compare two items based on their ordering scores. Calls the
     * {@link #score(VisualItem)} on each item and compares the result.
     * @param v1 the first VisualItem to compare
     * @param v2 the second VisualItem to compare
     * @return -1 if score(v1) &lt; score(v2), 1 if score(v1) &gt; score(v2)
     * and 0 if score(v1) == score(v2).
     */
    public int compare(VisualItem v1, VisualItem v2) {
        int score1 = score(v1);
        int score2 = score(v2);
        return (score1<score2 ? -1 : (score1==score2 ? 0 : 1));
    }
    
    /**
     * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
     * @see #compare(VisualItem, VisualItem)
     */
    public int compare(Object o1, Object o2) {
        if ( !(o1 instanceof VisualItem && o2 instanceof VisualItem) ) {
            throw new IllegalArgumentException();
        }
        return compare((VisualItem)o1, (VisualItem)o2);
    }

} // end of class ItemSorter
