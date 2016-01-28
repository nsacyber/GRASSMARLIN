/*
 *  Copyright (C) 2011, 2012
 *  This file is part of GRASSMARLIN.
 */
package core.topology;

import core.types.ImmutableMap;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Contains data obtained from {@link ImportItem}s which result in
 * physical-topology.
 */
public class TopologySource {
    /** set of all interfaces on this source */
    HashSet<IfaEx> interfaces;
    /** set of all vlans on this source */
    HashSet<VlanEx> vlans;
    /** required hostname */
    String hostname;
    
    TopologySourceVersion version;
    HashSet<String> commands;
    UserInfo users;
    
    
    public TopologySource() {
        version = new TopologySourceVersion();
        users = new UserInfo();
        interfaces = new HashSet<>();
        vlans = new HashSet<>();
        commands = new HashSet<>();
    }
    
    public void setHostname(String hostname) {
        this.hostname = hostname;
    }
    
    public void add(IfaEx ifa) {
        this.interfaces.add(ifa);
    }
    
    public void add(VlanEx vlan) {
        this.vlans.add(vlan);
    }
    
    public boolean contains(IfaEx ifa) {
        return this.interfaces.stream().anyMatch(ifa::equalsConfig);
    }
    
    public IfaEx getOrSetIfa(IfaEx ifa) {
        IfaEx other = this.interfaces.stream().filter(ifa::equalsConfig).findFirst().orElse(null);
        if( other == null ) {
            add(ifa);
            other = ifa;
        }
        return other;
    }
    
    public VlanEx getOrSetVlan(VlanEx vlan) {
        VlanEx other = this.vlans.stream().filter(vlan::equalsId).findFirst().orElse(null);
        if( other == null ) {
            add(vlan);
            other = vlan;
        }
        return other;
    }
    
    public Boolean isEmpty() {
        return commands.isEmpty();
    }
    
    public void addCommand(String command) {
        commands.add(command);
    }
    
    public HashSet<String> getCommands() {
        return commands;
    }

    public TopologySourceVersion getVersion() {
        return version;
    }
    
    public UserInfo getUsers() {
        return users;
    }

    /** Report the connection of a interface to some address */
    public void addIntersection(IfaEx ifa, Mac mac) {
        IfaEx ifaex = this.getOrSetIfa(ifa);
    }


}
