package prefuse.data.tuple;

import prefuse.data.Edge;
import prefuse.data.Graph;
import prefuse.data.Node;
import prefuse.data.Table;

/**
 * Edge implementation that reads Edge data from a backing edge table.
 * 
 * @author <a href="http://jheer.org">jeffrey heer</a>
 */
public class TableEdge extends TableTuple implements Edge {

    /**
     * The backing graph.
     */
    protected Graph m_graph;   
    
    /**
     * Initialize a new Edge backed by an edge table. This method is used by
     * the appropriate TupleManager instance, and should not be called
     * directly by client code, unless by a client-supplied custom
     * TupleManager.
     * @param table the edge Table
     * @param graph the backing Graph
     * @param row the row in the edge table to which this Node instance
     *  corresponds.
     */
    protected void init(Table table, Graph graph, int row) {
        m_table = table;
        m_graph = graph;
        m_row = m_table.isValidRow(row) ? row : -1;
    }
    
    /**
     * @see prefuse.data.Edge#getGraph()
     */
    public Graph getGraph() {
        return m_graph;
    }
    
    /**
     * @see prefuse.data.Edge#isDirected()
     */
    public boolean isDirected() {
        return m_graph.isDirected();
    }
    
    /**
     * @see prefuse.data.Edge#getSourceNode()
     */
    public Node getSourceNode() {
        return m_graph.getSourceNode(this);
    }

    /**
     * @see prefuse.data.Edge#getTargetNode()
     */
    public Node getTargetNode() {
        return m_graph.getTargetNode(this);
    }

    /**
     * @see prefuse.data.Edge#getAdjacentNode(prefuse.data.Node)
     */
    public Node getAdjacentNode(Node n) {
        return m_graph.getAdjacentNode(this, n);
    }

} // end of class TableEdge
