package prefuse.data.event;

import java.util.EventListener;

import prefuse.data.Tuple;
import prefuse.data.tuple.TupleSet;

/**
 * Listener interface for monitoring changes to a TupleSet instance. Indicates
 * when tuples are added or removed from the set.
 * 
 * @author <a href="http://jheer.org">jeffrey heer</a>
 */
public interface TupleSetListener extends EventListener {
    
    /**
     * Notification that a TupleSet has changed.
     * @param tset the TupleSet that has changed
     * @param added an array (potentially zero-length) of added tuples
     * @param removed an array (potentially zero-length) of removed tuples
     */
    public void tupleSetChanged(TupleSet tset, Tuple[] added, Tuple[] removed);
    
} // end of interface TupleSetListener
