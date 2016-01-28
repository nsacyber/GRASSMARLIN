package prefuse.data.util;

import java.util.Iterator;

import prefuse.data.Edge;
import prefuse.data.Node;

/**
 * Iterator over neighbors of a given Node. Resolves Edge instances to
 * provide direct iteration over the Node instances.
 * 
 * @author <a href="http://jheer.org">jeffrey heer</a>
 */
public class NeighborIterator implements Iterator {

    private Iterator m_edges;
    private Node     m_node;
    
    /**
     * Create a new NeighborIterator.
     * @param n the source node
     * @param edges the node edges to iterate over
     */
    public NeighborIterator(Node n, Iterator edges) {
        m_node = n;
        m_edges = edges;
    }
    
    /**
     * @see java.util.Iterator#remove()
     */
    public void remove() {
        throw new UnsupportedOperationException();
    }

    /**
     * @see java.util.Iterator#hasNext()
     */
    public boolean hasNext() {
        return m_edges.hasNext();
    }

    /**
     * @see java.util.Iterator#next()
     */
    public Object next() {
        Edge e = (Edge)m_edges.next();
        return e.getAdjacentNode(m_node);
    }

} // end of class NeighborIterator
