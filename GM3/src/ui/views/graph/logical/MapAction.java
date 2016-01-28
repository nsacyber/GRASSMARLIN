/*
 *  Copyright (C) 2011, 2012
 *  This file is part of GRASSMARLIN.
 */
package ui.views.graph.logical;

import prefuse.data.Tuple;
import ui.views.tree.visualnode.VisualNode;

/**
 *
 * @author BESTDOG
 */
public interface MapAction  {
    
    VisualNode get(Tuple tuple);
    
}
