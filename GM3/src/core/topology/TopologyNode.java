/*
 *  Copyright (C) 2011, 2012
 *  This file is part of GRASSMARLIN.
 */
package core.topology;

import java.util.HashSet;
import java.util.Set;

/**
 *
 */
public class TopologyNode extends TopologyEntity<PhysicalNode> {
    
    public transient final Set<TopologyEdge> edges;
    private transient final Port port;
    private final Mac mac;
    private boolean included;
    
    private TopologyNode( Type type, Port port, Mac mac ) {
        super(type, port);
        this.mac = mac;
        this.port = port;
        this.included = true;
        this.edges = new HashSet<>();
        if( port == null ) {
            setName(String.format("NIC %s", mac.toString()));
        }
        set(HARDWARE_ADDRESS_FIELD, mac);
    }
    
    public TopologyNode(Mac mac, Type type) {
        this(type, null, mac);
    }
    
    public TopologyNode(Port port, Type type) {
        this(type, port, port.mac);
    }
    
    public long countEntries(Mac mac) {
        return edges.stream().filter(e -> e.canSee(mac)).count();
    }
    
    public boolean hasAdjacent(TopologyNode node) {
        return edges.stream().anyMatch(edge -> edge.contains(node));
    }
    
    public boolean hasEdges() {
        return !edges.isEmpty();
    }

    public Mac getMac() {
        return mac;
    }
    
    public boolean isIncluded() {
        return included;
    }
    
    public void setIncluded(boolean included) {
        this.included = included;
    }
    
    @Override
    public boolean hasPort() {
        return this.port != null;
    }
    
    /**
     *
     * @return
     */
    @Override
    public Port getPort() {
        return port;
    }
    
    @Override
    public PhysicalNode getDataSet() {
        return this.port.owner;
    }
    
    @Override
    public String toString() {
        return String.format("Node(%s)", hasPort() ? port : mac);
    }
    
    @Override
    public int hashCode() {
        int hash = 0;
        if( hasPort() ) {
            hash = port.hashCode();
        } else {
            hash = mac.hashCode();
        }
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final TopologyNode other = (TopologyNode) obj;
        return this.port.mac.equals( other.port.mac );
    }
    
    @Override
    public int compareTo(Object o) {
        if( o instanceof TopologyNode ) {
            TopologyNode node = (TopologyNode) o;
            if( this.port.name.equals(node.port.name) ) {
                int ret = 0;
                int len = Math.min(this.port.config.length,  node.port.config.length);
                for( int i = 0; i < len; ++i ) {
                    ret = Integer.compare(this.port.config[i], node.port.config[i]);
                    if( ret != 0 ) {
                        break;
                    }
                }
                return ret;
            } else {
                return this.port.name.compareTo(node.port.name);
            }
        }
        return -1;
    }

    @Override
    public String getDisplayText() {
        String text = null;
        if( this.hasPort() ) {
            text = String.format("%s", this.port.getName());
        } else {
            text = String.format("%s", this.mac);
        }
        return text;
    }

    @Override
    public void resetDisplayText() {
        /* do nothing */
    }

}
