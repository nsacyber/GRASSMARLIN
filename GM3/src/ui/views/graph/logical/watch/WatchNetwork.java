/*
 *  Copyright (C) 2011, 2012
 *  This file is part of GRASSMARLIN.
 */
package ui.views.graph.logical.watch;

import prefuse.data.Node;
import ui.views.graph.logical.LogicalGraphEx;
import ui.views.graph.logical.LogicalVisualizationEx;
import ui.views.tree.visualnode.VisualNode;

/**
 *
 * @author BESTDOG
 * Gives a "Watch" view of a single network within the logical view.
 */
public class WatchNetwork extends LogicalGraphEx implements Watch {
    
    VisualNode network;

    public WatchNetwork(int id, VisualNode network) {
        super(new LensVis(id));
        this.network = network;
    }
    
    @Override
    protected void processData() {
        VisualNode root = getRoot();
        int start = 0, end = 0;
        if( root != null ) {
            Node node = (Node) vis().getNode(network);
            start = node.getDegree();
            network.getChildren().forEach(host -> 
                vis().getNode(host)
            );
            end = node.getDegree();
            if( end > start ) {
                vis().validateEdges();
                vis().autoLayout();
            }
        }
    }

    @Override
    public WatchNetwork close() {
        this.network.removeVisualIndex(this.getId());
        return this;
    }

    @Override
    public void update(VisualNode root) {
        super.update(root);
    }
    
    private static class LensVis extends LogicalVisualizationEx {
        
        public LensVis(int id) {
            this.setId(id);
        }
        
        @Override
        protected boolean canConnect(VisualNode node, VisualNode peer) {
            return node.getParent().equals(peer.getParent());
        }
    }
    
    
}
