package prefuse.util.collections;

import java.util.HashMap;
import java.util.LinkedList;

/**
 * Maintains a breadth-first-search queue as well as depth labels.
 * 
 * @author <a href="http://jheer.org">jeffrey heer</a>
 */
public class Queue {

    // TODO: create an optimized implementation of this class
    
    private LinkedList m_list = new LinkedList();
    private HashMap    m_map  = new HashMap();
    
    public void clear() {
        m_list.clear();
        m_map.clear();
    }
    
    public boolean isEmpty() {
        return m_list.isEmpty();
    }
    
    public void add(Object o, int depth) {
        m_list.add(o);
        visit(o, depth);
    }
    
    public int getDepth(Object o) {
        Integer d = (Integer)m_map.get(o);
        return ( d==null ? -1 : d.intValue() );
    }
    
    public void visit(Object o, int depth) {
        m_map.put(o, new Integer(depth));
    }
    
    public Object removeFirst() {
        return m_list.removeFirst();
    }
    
    public Object removeLast() {
        return m_list.removeLast();
    }
    
} // end of class Queue
