package prefuse.data.util;

import prefuse.data.Graph;
import prefuse.data.Node;
import prefuse.data.Tuple;
import prefuse.data.expression.AbstractPredicate;

/**
 * Filtering predicate over a potential edge table that indicates which
 * edges are valid edges according to a backing node table. Useful for
 * creating a pool of edges for which not all node have been created, and
 * then filtering out the valid edges using the node pool.
 *  
 * @author <a href="http://jheer.org">jeffrey heer</a>
 */
public class ValidEdgePredicate extends AbstractPredicate {
    
    private Graph m_g;
    
    /**
     * Creates a new ValidEdgePredicate.
     * @param g the backing graph, the node table of this graph will be used
     * to check for valid edges.
     */
    public ValidEdgePredicate(Graph g) {
        m_g = g;
    }
    
    /**
     * Indicates if the given tuple can be used as a valid edge for
     * the nodes of the backing graph.
     * @param tpl a data tuple from a potential edge table
     * @return true if the tuple contents allow it to serve as a valid
     * edge of between nodes in the backing graph
     */
    public boolean getBoolean(Tuple tpl) {
        Node s = m_g.getNodeFromKey(tpl.getInt(m_g.getEdgeSourceField()));
        Node t = m_g.getNodeFromKey(tpl.getInt(m_g.getEdgeTargetField()));
        return ( s != null && t != null );
    }
    
} // end of class ValidEdgePredicate
