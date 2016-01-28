/**
 * Copyright (c) 2004-2006 Regents of the University of California.
 * See "license-prefuse.txt" for licensing terms.
 */
package prefuse.data.util;

import java.util.ArrayList;
import java.util.Iterator;

import prefuse.data.Node;

/**
 * A depth-first iterator over the subtree rooted at given node.
 * 
 * @author <a href="http://jheer.org">jeffrey heer</a>
 */
public class TreeNodeIterator implements Iterator {

    private ArrayList m_stack;
    private Node m_root;
    private boolean m_preorder = true;
    
    /**
     * Create a new TreeNodeIterator over the given subtree.
     * @param root the root of the subtree to traverse
     */
    public TreeNodeIterator(Node root) {
    	this(root, true);
    }
    
    /**
     * Create a new TreeNodeIterator over the given subtree.
     * @param root the root of the subtree to traverse
     * @param preorder true to use a pre-order traversal, false
     *  for a post-order traversal
     */
    public TreeNodeIterator(Node root, boolean preorder) {
    	m_preorder = preorder;
    	m_root = root;
    	m_stack = new ArrayList();
    	m_stack.add(root);
    	
    	if (!preorder) {
    		for (Node n = root.getChild(0); n!=null; n=n.getChild(0))
    			m_stack.add(n);
    	}
        
    }
    
    /**
     * @see java.util.Iterator#hasNext()
     */
    public boolean hasNext() {
        return !m_stack.isEmpty();
    }

    /**
     * @see java.util.Iterator#next()
     */
    public Object next() {
    	Node c, x = null;
    	if (m_preorder) {
    		x = (Node)m_stack.get(m_stack.size()-1);
	    	if ( (c=x.getChild(0)) != null ) {
	    		m_stack.add(c);
	    	} else if ( (c=x.getNextSibling()) != null ) {
	    		m_stack.set(m_stack.size()-1, c);
	    	} else {
	    		m_stack.remove(m_stack.size()-1);
	    		while (!m_stack.isEmpty()) {
		    		c = (Node)m_stack.remove(m_stack.size()-1);
		    		if ( c == m_root ) {
		    			break;
		    		} else if ( (c=c.getNextSibling()) != null ) {
		    			m_stack.add(c); break;
		    		}
	    		}
	    	}
    	} else {
    		x = (Node)m_stack.remove(m_stack.size()-1);
    		if ( x != m_root && (c=x.getNextSibling()) != null ) {
    			for (; c != null; c=c.getChild(0)) {
    				m_stack.add(c);
    			}
    		}
    	}
    	return x;
    }

    /**
     * Throws an UnsupportedOperationException
     * @see java.util.Iterator#remove()
     */
    public void remove() {
        throw new UnsupportedOperationException("Remove not supported");
    }

} // end of class TreeNodeIterator
