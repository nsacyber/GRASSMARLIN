/*
 *  Copyright (C) 2011, 2012
 *  This file is part of GRASSMARLIN.
 */
package core.topology;

import core.topology.TopologyEntity.Type;
import core.types.ImmutableMap;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Stream;

/**
 *
 */
public class TopologyTree {

    /* set of valid topology discovered nodes */
    Collection<PhysicalNode> dataSet;
    /* a map of hashes to nodes for ease-of-use */
    Map<Integer, TopologyNode> nodes;
    /* a map of ahahse to edges for ease-of-use */
    Map<Integer,TopologyEdge> edges;
    /* a set of all leaf nodes which are concidered to be a "host" or router */
    Set<TopologyNode> leafNodes;
    /* a set of all disjunct not that are not included in the determined topology do to incomplete information */
    Set<TopologyNode> disjunctNodes;
    /* set for clouds */
    Map<Integer,TopologyNode> cloudNodes;
    /* a map of physicalnodes (switches/seed devices) to their ports */
    public Map<PhysicalNode,Set<TopologyNode>> aggregates;
    
    public TopologyTree() {
        nodes = new HashMap<>();
        edges = new HashMap<>();
        leafNodes = new HashSet<>();
        cloudNodes = new HashMap<>();
        disjunctNodes = new HashSet<>();
        aggregates = new ImmutableMap<>(HashSet::new);
    }
    
    public void setDataSet(Collection<PhysicalNode> dataSet) {
        this.dataSet = dataSet;
    }
    
    public Collection<PhysicalNode> getDataSet() {
        return dataSet;
    }
    
    public Stream<Mac> Macs() {
        return getDataSet().stream().flatMap( n -> n.macs.stream() );
    }
    
    public TopologyNode getNode(Port port) {
        if( !nodes.containsKey(port.hashCode()) ) {
            TopologyNode node = newNode(port);
            nodes.put( port.hashCode(), node );
            return node;
        }
        return nodes.get(port.hashCode());
    }

    public Collection<TopologyNode> nodes() {
        return nodes.values();
    }
    
    public Collection<TopologyEdge> edges() {
        return edges.values();
    }
    
    public void eachNode( Consumer<TopologyNode> cb ) {
        nodes.values().iterator().forEachRemaining(cb);
    }
    
    public Stream<TopologyNode> eachNode() {
        return nodes.values().stream();
    }
    
    public TopologyEdge getEdge( TopologyNode source, TopologyNode destination ) {
        Integer hash = edgeHash( source, destination );
        
        if( !edges.containsKey(hash) ) {
            TopologyEdge edge = newEdge(source, destination);
            source.edges.add(edge);
            destination.edges.add(edge);
            edges.put( hash, edge );
            return edge;
        }
        return edges.get(hash);
    }
    
    private Integer edgeHash(TopologyNode src, TopologyNode dst) {
        return TopologyEdge.hashCode(src, dst);
    }
    
    private TopologyEdge newEdge( TopologyNode src, TopologyNode dst ) {
        return new TopologyEdge(src, dst);
    }
    
    public TopologyNode newCloud() {
        TopologyNode node = new TopologyNode(Mac.MissingMac, Type.CLOUD);
        cloudNodes.put( cloudNodes.size(), node );
        return node;
    }
    
    public TopologyNode newNode(Mac mac) {
        return new TopologyNode(mac, Type.HOST);
    }
    
    public TopologyNode newNode(Port port) {
        return new TopologyNode(port, Type.PORT);
    }

    public boolean hasNode(Port port) {
        return nodes().stream()
                .filter(TopologyNode::hasPort)
                .anyMatch(node -> node.getPort().equals(port));
    }
    
    public Stream<TopologyNode> connectedNodes() {
        return Stream.concat(cloudNodes.values().stream(), nodes().stream()).filter(n->!n.edges.isEmpty());
    }
    
    @Override
    public boolean equals(Object obj) {
        if( !(obj instanceof TopologyTree) ) {
            return false;
        }
        
        TopologyTree other = (TopologyTree) obj;
        
        return other.aggregates.keySet().containsAll(this.aggregates.keySet());
    }
    
    @Override
    public int hashCode() {
        int hash = 0;
        for( PhysicalNode node : aggregates.keySet() ) {
            hash += node.hashCode();
            hash *= 37;
        }
        return hash;
    }

    public void addLeafNode(TopologyNode leaf) {
        leafNodes.add(leaf);
    }
    
    public Stream<TopologyNode> eachLeafNode() {
        return leafNodes.stream();
    }
    
    public void addDisjunctNode(TopologyNode disjunct) {
        disjunct.setIncluded(false);
        disjunctNodes.add(disjunct);
    }
    
    public Stream<TopologyNode> eachDisjunctNode() {
        return disjunctNodes.stream();
    }

    public boolean isEmpty() {
        return dataSet.isEmpty();
    }

    public void refreshVisualRows() {
        connectedNodes().forEach( e ->  e.visualReset() );
        edges.values().forEach( e ->  e.visualReset() );
    }
    
}
