package prefuse.data;

import java.util.Iterator;


/**
 * Tuple sub-interface that represents a node in a graph or tree structure.
 * This interface supports both graph and tree methods, tree methods invoked
 * on a node in a general graph typically default to operations on the
 * graph's generated spanning tree.
 * 
 * @author <a href="http://jheer.org">jeffrey heer</a>
 */
public interface Node extends Tuple {

    
    /**
     * Get the Graph of which this Node is a member.
     * @return the backing Graph.
     */
    public Graph getGraph();

    // ------------------------------------------------------------------------
    // Graph Methods
    
    /**
     * Get the in-degree of the node, the number of edges for which this node
     * is the target.
     * @return the in-degree of the node
     */
    public int getInDegree();
    
    /**
     * Get the out-degree of the node, the number of edges for which this node
     * is the source.
     * @return the out-degree of the node
     */
    public int getOutDegree();
    
    /**
     * Get the degree of the node, the number of edges for which this node
     * is either the source or the target.
     * @return the total degree of the node
     */
    public int getDegree();
    
    /**
     * Get an iterator over all incoming edges, those for which this node
     * is the target.
     * @return an Iterator over all incoming edges
     */
    public Iterator inEdges();
    
    /**
     * Get an iterator over all outgoing edges, those for which this node
     * is the source.
     * @return an Iterator over all outgoing edges
     */
    public Iterator outEdges();
    
    /**
     * Get an iterator over all incident edges, those for which this node
     * is either the source or the target.
     * @return an Iterator over all incident edges
     */
    public Iterator edges();
    
    /**
     * Get an iterator over all adjacent nodes connected to this node by an
     * incoming edge (i.e., all nodes that "point" at this one).
     * @return an Iterator over all neighbors with in-links on this node
     */
    public Iterator inNeighbors();
    
    /**
     * Get an iterator over all adjacent nodes connected to this node by an
     * outgoing edge (i.e., all nodes "pointed" to by this one).
     * @return an Iterator over all neighbors with out-links from this node
     */
    public Iterator outNeighbors();
    
    /**
     * Get an iterator over all nodes connected to this node.
     * @return an Iterator over all neighbors of this node
     */
    public Iterator neighbors();
    
    // ------------------------------------------------------------------------
    // Tree Methods
    
    /**
     * Get the parent node of this node in a tree structure.
     * @return this node's parent node, or null if there is none.
     */
    public Node getParent();
    
    /**
     * Get the edge between this node and its parent node in a tree
     * structure.
     * @return the edge between this node and its parent
     */
    public Edge getParentEdge();
    
    /**
     * Get the tree depth of this node.
     * @return the tree depth of this node. The root's tree depth is
     * zero, and each level of the tree is one depth level greater.
     */
    public int getDepth();
    
    /**
     * Get the number of tree children of this node.
     * @return the number of child nodes
     */
    public int getChildCount();
    
    /**
     * Get the ordering index of the give node child in a tree
     * structure.
     * @param child the child node to look up
     * @return the index of the child node, or -1 if the node is
     * not a child of this one.
     */
    public int getChildIndex(Node child);
    
    /**
     * Get the tree child node at the given index.
     * @param idx the ordering index
     * @return the child node at the given index
     */
    public Node getChild(int idx);
    
    /**
     * Get this node's first tree child. This is the
     * same as looking up the node at index 0.
     * @return this node's first child node
     */
    public Node getFirstChild();
    
    /**
     * Get this node's last tree child. This is the
     * same as looking up the node at the child count
     * minus 1.
     * @return this node's last child node
     */
    public Node getLastChild();
    
    /**
     * Get this node's previous tree sibling.
     * @return the previous sibling, or null if none
     */
    public Node getPreviousSibling();
    
    /**
     * Get this node's next tree sibling.
     * @return the next sibling, or null if none
     */
    public Node getNextSibling();
    
    /**
     * Get an iterator over this node's tree children.
     * @return an iterator over this node's children
     */
    public Iterator children();
    
    /**
     * Get an iterator over the edges from this node to its tree children.
     * @return an iterator over the edges to the child nodes
     */
    public Iterator childEdges();
    
} // end of interface Node
