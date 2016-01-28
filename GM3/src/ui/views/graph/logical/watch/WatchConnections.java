/*
 *  Copyright (C) 2011, 2012
 *  This file is part of GRASSMARLIN.
 */
package ui.views.graph.logical.watch;

import java.util.function.Function;
import java.util.function.Predicate;
import ui.views.graph.logical.LogicalGraphEx;
import ui.views.graph.logical.LogicalVisualizationEx;
import ui.views.tree.visualnode.PeerVisualNode;
import ui.views.tree.visualnode.VisualNode;

/**
 *
 * @author BESTDOG A Watch seeded by a host, shows all reachable hosts and
 * networks of hosts.
 */
public class WatchConnections extends LogicalGraphEx implements Watch {

    VisualNode node;

    public WatchConnections(int id, VisualNode node) {
        super(new LensVis(id));
        this.node = node;
    }



    @Override
    public void processData() {
        VisualNode root = getRoot();
        vis().getNode(node.getParent());
        vis().getNode(node);

        Predicate<VisualNode> isPeer = p -> p instanceof PeerVisualNode;
        Function<VisualNode, PeerVisualNode> toPeer = p -> (PeerVisualNode) p;

        vis().getHosts().flatMap(n -> n.getChildren().stream().filter(isPeer).map(toPeer))
                .map(PeerVisualNode::getOriginal)
                .filter(this::missing)
                .forEach(host -> {
                    if (host.getVisualAgg(vis().getId()) == -1) {
                        vis().getNode(host.getParent());
                    }
                    vis().getNode(host);
                });
        vis().validateEdges();
        vis().autoLayout();
    }

    private boolean missing(VisualNode node) {
        return node.getVisualRow(vis().getId()) == -1;
    }

    @Override
    public int getId() {
        return vis().getId();
    }

    @Override
    public void setId(int id) {
        throw new UnsupportedOperationException("Not supported yet.");
    }


    @Override
    public WatchConnections close() {
        VisualNode node = this.node;
        while( node != null ) {
            if( node.getParent() == null ) {
                node.removeVisualIndex(this.getId());
            }
            node = node.getParent();
        }
        return this;
    }

    private static class LensVis extends LogicalVisualizationEx {
        public LensVis(int id) {
            this.setId(id);
        }
        @Override
        protected void initVisualization() {
            super.initVisualization();
        }

    }

}
