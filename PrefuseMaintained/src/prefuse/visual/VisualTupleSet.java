/**
 * Copyright (c) 2004-2006 Regents of the University of California.
 * See "license-prefuse.txt" for licensing terms.
 */
package prefuse.visual;

import prefuse.Visualization;
import prefuse.data.tuple.TupleSet;

/**
 * TupleSet sub-interface for TupleSet instances that contain VisualItems,
 * Tuple instances that include visual properties.
 * 
 * @author <a href="http://jheer.org">jeffrey heer</a>
 */
public interface VisualTupleSet extends TupleSet {

    /**
     * Get the Visualization associated with this VisualTupleSet.
     * @return the Visualization instance
     */
    public Visualization getVisualization();
    
    /**
     * Get the data group name for this VisualTupleSet.
     * @return the data group name
     */
    public String getGroup();
    
} // end of interface VisualTupleSet
