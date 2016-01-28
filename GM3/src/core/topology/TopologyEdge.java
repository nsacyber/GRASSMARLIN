/*
 *  Copyright (C) 2011, 2012
 *  This file is part of GRASSMARLIN.
 */
package core.topology;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Objects;
import java.util.Set;

/**
 *
 */
public class TopologyEdge extends TopologyEntity {
    
    public final TopologyEntity source, destination;
    String text;
    int weight;
    
    public TopologyEdge(TopologyEntity source, TopologyEntity destination) {
        super(Type.determineEdge(source, destination));
        this.text = null;
        this.source = source;
        this.destination = destination;
        weight = 0;
    }
  
    public TopologyEntity getAdjacent(TopologyEntity node) {
        if( !this.source.equals(node) && !this.destination.equals(node) ) {
            return null;
        }
        if( this.source.equals(node) ) {
            return destination;
        }
        return this.source;
    }
    
    public boolean canSee(Mac mac) {
        if( source.hasPort() && source.getPort().macs().contains(mac) ) {
            return true;
        }
        if( destination.hasPort() && destination.getPort().macs().contains(mac) ) {
            return true;
        }
        return source.getMac().equals(mac) || destination.getMac().equals(mac);
    }
    
    public void increaseWeight() {
        this.weight++;
    }
    
    public void setWeight(int weight) {
        this.weight = weight;
    }
    
    public int weight() {
        return weight;
    }
    
    public boolean contains(TopologyNode node) {
        return this.source.equals(node) || this.destination.equals(node);
    }

    void getVlanIds(Set<Integer> vlanIds, TopologyEntity node) {
        if( node.hasPort() ) {
            node.getPort().getVlans().forEach( vlan -> 
                vlanIds.add(vlan.id)
            );
        }
    }
    
    @Override
    public Mac getMac() {
        throw new UnsupportedOperationException("Not supported yet. getMac");
    }
    
    @Override
    public int compareTo(Object o) {
        return 0;
    }
    
    @Override
    public String getDisplayText() {
        String returnText = this.text;
        if( returnText == null ) {
            StringBuilder b = new StringBuilder();
            Set<Integer> vlanIds = new HashSet<>();
            getVlanIds( vlanIds, source );
            getVlanIds( vlanIds, destination );
            if( vlanIds.isEmpty() ) {
                this.text = returnText = "";
            } else {
                b.append("[ ");
                Iterator<Integer> it = vlanIds.iterator();
                if( it.hasNext() ) {
                    b.append(it.next());
                }
                while( it.hasNext() ) {
                    b.append(", ").append(it.next());
                }
                b.append(" ]");
                this.text = b.toString();
                returnText = text;
            }
        }
        return returnText;
    }
    
    @Override
    public void resetDisplayText() {
        this.text = null;
    }
    
    @Override
    public Object getDataSet() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
    @Override
    public String toString() {
        return String.format("weight : %d, src : %s, dst : %s", this.weight, this.source, this.destination);
    }
    
    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final TopologyEdge other = (TopologyEdge) obj;
        if (!Objects.equals(this.source, other.source)) {
            return false;
        }
        if (!Objects.equals(this.destination, other.destination)) {
            return false;
        }
        if (this.weight != other.weight) {
            return false;
        }
        return true;
    }
    
    @Override
    public int hashCode() {
        return hashCode( this.source, this.destination );
    }

    /* creates a naturally ordered hashcode */
    public static Integer hashCode( TopologyEntity source, TopologyEntity destination ) {
        int hash = 3;
        if( source.hashCode() < destination.hashCode() ) {
            hash = 53 * hash + source.hashCode();
            hash = 53 * hash + destination.hashCode();
        } else {
            hash = 53 * hash + destination.hashCode();
            hash = 53 * hash + source.hashCode();
        }
        return hash;
    }
    
}
