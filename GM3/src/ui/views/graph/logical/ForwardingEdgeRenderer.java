/*
 *  Copyright (C) 2011, 2012
 *  This file is part of GRASSMARLIN.
 */
package ui.views.graph.logical;

import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;

import prefuse.data.Tuple;
import prefuse.render.EdgeRenderer;
import prefuse.visual.EdgeItem;
import prefuse.visual.VisualItem;
import ui.views.tree.visualnode.VisualNode;

/**
 *
 * @author BESTDOG Assists in creating the collapse feature by allowing two
 * methods to simulate edges being connected to networks instead the actual
 * node.
 */
public class ForwardingEdgeRenderer extends EdgeRenderer {


    /**
     * Used to forward edges when networks are collapsed.
     */
    final ConcurrentHashMap<Integer, VisualItem> nodeForwarding;

    public ForwardingEdgeRenderer() {
        this.nodeForwarding = new ConcurrentHashMap<>();
    }

    private void recompute() {
        
    }

    @Override
    protected boolean shouldSkip(VisualItem source, VisualItem target) {
        return !(source.isVisible() && target.isVisible());
    }

    @Override
    protected VisualItem getTarget(EdgeItem item) {
        VisualItem target = item.getTargetItem();
        if (willForward(target.getRow())) {
            target = get(target.getRow());
        }
        return target;
    }

    @Override
    protected VisualItem getSource(EdgeItem item) {
        VisualItem source = item.getSourceItem();
        if (willForward(source.getRow())) {
            source = get(source.getRow());
        }
        return source;
    }

    public void clear() {
        nodeForwarding.clear();
    }

    /**
     * Stops forwarding a node so that it's edges draw to it.
     *
     * @param row Row to stop forwarding.
     */
    void unforward(Integer row) {
        nodeForwarding.remove(row);
        recompute();
    }

    /**
     * Will begin forwarding a nodes edges to it's parents position.
     *
     * @param row Row of node being forwarded.
     * @param node Node to forward to.
     */
    void forward(Integer row, VisualItem node) {
        nodeForwarding.put(row, node);
        recompute();
    }

    VisualItem get(Integer row) {
        return nodeForwarding.get(row);
    }

    /**
     * Checks if a node's row appears to be forwarded to a parent node as a
     * result of collapse.
     *
     * @param row Row to check.
     * @return True if node was hidden as a result of a network collapsing.
     */
    boolean willForward(Integer row) {
        return nodeForwarding.containsKey(row);
    }

}
