package prefuse.visual;

/**
 * VisualItem that "decorates" another VisualItem. DecoratorItem instances
 * allow the decorated item to be retrieved, and used to compute visual
 * properties for this item. Example decorator items might include attaching
 * an external label to an item or adding interactive handles to another
 * item.
 * 
 * @author <a href="http://jheer.org">jeffrey heer</a>
 */
public interface DecoratorItem extends VisualItem {

    /**
     * Get the VisualItem that this item is decorating.
     * @return the decorated VisualItem
     */
    public VisualItem getDecoratedItem();
    
} // end of interface DecoratorItem
