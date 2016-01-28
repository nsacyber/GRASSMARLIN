package prefuse.data;


/**
 * Tuple sub-interface that represents an edge in a graph structure. Each edge
 * has both a source node and a target node. For directed edges, this
 * distinction indicates the directionality of the edge. For undirected edges
 * this distinction merely reflects the underlying storage of the nodes.
 * 
 * @author <a href="http://jheer.org">jeffrey heer</a>
 */
public interface Edge extends Tuple {

    /**
     * Returns the graph of which this Edge is a member.
     * @return the Graph containing this Edge
     */
    public Graph getGraph();
    
    /**
     * Indicates if this edge is directed or undirected.
     * @return true if directed, false if undirected.
     */
    public boolean isDirected();
    
    /**
     * Returns the first, or source, node upon which this Edge
     * is incident.
     * @return the source Node
     */
    public Node getSourceNode();
    
    /**
     * Returns the second, or target, node upon which this Edge
     * is incident.
     * @return the source Node
     */
    public Node getTargetNode();
    
    /**
     * Given a Node upon which this Edge is incident, the opposite incident
     * Node is returned. Throws an exception if the input node is not incident
     * on this Edge.
     * @param n a Node upon which this Edge is incident
     * @return the other Node touched by this Edge
     */
    public Node getAdjacentNode(Node n);
    
} // end of interface Edge
