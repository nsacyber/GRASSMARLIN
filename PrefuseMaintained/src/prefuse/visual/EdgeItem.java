package prefuse.visual;

import prefuse.data.Edge;

/**
 * VisualItem that represents an edge in a graph. This interface combines
 * the {@link VisualItem} interface with the {@link prefuse.data.Edge}
 * interface.
 * 
 * @author <a href="http://jheer.org">jeffrey heer</a>
 */
public interface EdgeItem extends VisualItem, Edge {

    /**
     * Get the first, or source, NodeItem upon which this edge is incident.
     * @return the source NodeItem
     */
    public NodeItem getSourceItem();
    
    /**
     * Get the second, or target, NodeItem upon which this edge is incident.
     * @return the target NodeItem
     */
    public NodeItem getTargetItem();
    
    /**
     * Get the NodeItem connected to the given NodeItem by this edge.
     * @param n a NodeItem upon which this edge is incident. If this item
     * is not connected to this edge, a runtime exception will be thrown.
     * @return the other NodeItem connected to this edge
     */
    public NodeItem getAdjacentItem(NodeItem n);
    
} // end of interface EdgeItem
