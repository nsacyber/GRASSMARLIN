/*
 *  Copyright (C) 2011, 2012
 *  This file is part of GRASSMARLIN.
 */
package ui.views.graph.logical;

import java.awt.Color;
import java.awt.Font;
import prefuse.Visualization;
import prefuse.data.expression.Predicate;
import prefuse.util.ColorLib;
import prefuse.util.FontLib;
import prefuse.visual.expression.HoverPredicate;
import prefuse.visual.expression.InGroupPredicate;
import prefuse.visual.expression.SearchPredicate;
import ui.views.tree.visualnode.VisualNode;

/**
 *
 * @author BESTDOG
 */
public class LogicalParameters {

    public final String MAP = "map";
    
    
    public final String GRAPH = "graph";         // group of all the data
    public final String NODES = "graph.nodes";   // group of nodes
    public final String EDGES = "graph.edges";   // group of edges
    public final String AGGR = "aggregates";     // group of the shapes around host-nodes
    public final String AGGR_COLOR = "aggColor"; // field on AggregateItems of group AGGR that is a user-defined color
    public final String LINEAR = "linear";       // group of the items that comprise the circle we drasw the aggregates around
    public final String LAYOUT = "layout";       // key for main layout that makes the circle shape
    public final String SUBLAYOUT = "layout.sub";// key for layout that repositions nodes in the aggregate sets
    public final String REPAINT = "repaint";     // key for action - same effect as repaint on a swing component
    public final String REPAINT_NO = "repaint node only";   // key for action - repaints only nodes, just used for inital node
    public final String USERMODE = "user";       // key for action that constantly repaints, letting users move around nodes
    public final String ANIMATE = "animate";     // key for animation of new nodes entering
    public final String FILTER = "filter";       // key for major layout and animation in the correct order
    public final String REACHABLE = "in.reach";  // key for a subset of nodes within reach of a user designated node
    public final String NO_ITEM = "null.set";    // key for absolutely nothing

    public final String EDGE_BETWEEN_ROOT_NETWORK = "ebrn"; // category key for edge
    public final String EDGE_BETWEEN_NETWORK_HOST = "ebnh"; // category key for edge
    public final String EDGE_BETWEEN_HOST_HOST = "ebhh"; // category key for edge
    public final String EDGE_BETWEEN_COLLAPSED_NETWORK_OTHER = "ebcno"; // category key for edge
    public final String NETWORKS = VisualNode.NETWORK;  // type key for networks
    public final String COUNTS = NETWORKS + ".counts";
    public final String HOSTS = VisualNode.HOST;  // type key for hosts
    public final String ROOTS = VisualNode.ROOT;  // type key for root
    public final String PROPERTY_AUTO_UPDATE = "auto.update";
    public final String PROPERTY_HIDE_NETWORKS = "hide.networks";

    /**
     * Misc constants
     */
    public final int FILTER_DURATION = 480; // time it takes to animate the filterting in of new visual items
    public final String DEFAULT_SEARCH_KEY = "Search"; // text to display in search box, cannot search if input is equal
    public final String DEFAULT_VISIBILITY_TEXT = "Hosts 0/0, Networks 0/0"; // text to display in search box, cannot search if input is equal
    public final int ROOT_ROW = 0;

    /** fonts */
    Font normal = FontLib.getFont("Tahoma", 12);
    Font large = FontLib.getFont("Tahoma", 13);
  
    public final Predicate hover = new HoverPredicate();
    public final Predicate searchGroup = new SearchPredicate(false); // if search finds no items highlight nothing
    public final Predicate focusGroup = new InGroupPredicate(Visualization.FOCUS_ITEMS);
    
    public final int[] aggregatePalette = {
        ColorLib.rgba(41, 181, 95, 30),
        ColorLib.rgba(41, 95, 181, 30),
        ColorLib.rgba(181, 41, 95, 30),
        ColorLib.rgba(95, 41, 181, 30),
        ColorLib.rgba(181, 95, 41, 30),
        ColorLib.rgba(95, 181, 41, 30)
    };

    public final int[] edgePalette = {
        Color.RED.getRGB(),
        Color.GREEN.getRGB(),
        Color.BLUE.getRGB(),
        Color.ORANGE.getRGB()
    };

}
