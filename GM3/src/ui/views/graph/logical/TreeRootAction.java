/*
 *  Copyright (C) 2011, 2012
 *  This file is part of GRASSMARLIN.
 */
package ui.views.graph.logical;

import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;
import prefuse.Visualization;
import prefuse.action.GroupAction;
import prefuse.data.Graph;
import prefuse.data.Node;
import prefuse.data.tuple.TupleSet;

public class TreeRootAction extends GroupAction {

    /**
     * Default constructor
     *     
* @param graphGroup The group holding the graph
     */
    public TreeRootAction(String graphGroup) {
        super(graphGroup);
    }

    /**
     * @param frac Value between 0.0 and 1.0, 0.0 being the start and 1.0 being
     * the end... 0.5 would be half-way through.
     */
    public void run(double frac) {
        TupleSet focus = m_vis.getGroup(Visualization.FOCUS_ITEMS);
        if (focus == null || focus.getTupleCount() == 0) {
            return;
        }
        Graph g = (Graph) m_vis.getGroup(m_group);
        Node f = null;
        Iterator iter = focus.tuples();
        while (iter.hasNext()) {
            Object o = iter.next();
            if (o instanceof Node) {
                f = (Node) o;
                continue;
            }
        }
        if (f == null) {
            return;
        }
        try {
            if( f.isValid() ) {
                g.getSpanningTree(f);
            }
        } catch (java.lang.IllegalArgumentException ex) {
            Logger.getAnonymousLogger().log(Level.SEVERE, "TreeRootActionFailed", ex);
        }
    }
}
