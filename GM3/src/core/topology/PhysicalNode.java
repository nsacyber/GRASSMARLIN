/*
 *  Copyright (C) 2011, 2012
 *  This file is part of GRASSMARLIN.
 */
package core.topology;

import java.util.Collections;
import java.util.HashSet;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Data bundle containing any / all physical network information usable in
 * topology discovery.
 */
public class PhysicalNode extends Entities.BasicEntity<PhysicalNode> {

    public static final String VLAN_KEY = "Vlans";
    public static final String PORT_KEY = "Ports";
    public static final String VERSION_KEY = "Version";
    public static final String USER_KEY = "User-info";
    public static final String COMMANDS = "Commands Used";

    public final String GUID;
    public final Set<Mac> macs;
    public final Set<Port> ports;
    public final Set<Vlan> vlans;
    private final Set<String> commandsProcessed;

    private final TopologySourceVersion version;
    private final UserInfo userInfo;

    public PhysicalNode(String GUID) {
        super();
        this.GUID = GUID;
        this.setName(GUID);
        macs = new HashSet<>();
        ports = new HashSet<>();
        vlans = new HashSet<>();
        version = new TopologySourceVersion();
        commandsProcessed = new HashSet<>();
        userInfo = new UserInfo();
        this.set(COMMANDS, commandsProcessed);
        this.set(VERSION_KEY, version);
        this.set(USER_KEY, userInfo);
        this.set(VLAN_KEY, vlans);
        /* this.set(PORT_KEY, ports);  */ // these are added later
    }

    /**
     * Counts the number of ports that see traffic from this address.
     *
     * @param mac Mac to check for.
     * @return Times MAC is seen.
     */
    public long countEntries(Mac mac) {
        return ports.stream().filter(port -> port.macs().contains(mac)).count();
    }

    /**
     * @return Retrieves the version information object for this source of
     * topology data.
     */
    public TopologySourceVersion getVersion() {
        return (TopologySourceVersion) get(VERSION_KEY);
    }
    
    /**
     * @return A list of commands processed while constructing this data set.
     */
    public Set<String> getCommands() {
        return commandsProcessed;
    }

    /**
     * @return The UserInfo object which contains lists of Usernames and
     * passwords from config files.
     */
    public UserInfo getUserInfo() {
        return userInfo;
    }

    /**
     * Retrieves a Port for a given port number and type, may be null.
     *
     * @param other Other port to test
     * @return Port if exists, else null.
     */
    public Port getPort(Port other) {
        Port port = ports.stream().filter(other::equalsConfig).findFirst().orElse(null);
        if (port == null) {
            port = other;
            ports.add(port);
        }
        return port;
    }

    /**
     * @param vlanId Id of the Vlan to check for.
     * @return True if this Vlan-Id is exists.
     */
    public boolean hasVlan(int vlanId) {
        return vlans.stream().filter(v -> v.id == vlanId).findAny().isPresent();
    }

    /**
     * Retrieves a Vlan for a given Vlan-id if the Vlan is not present it will
     * be created and added to the set.
     *
     * @param vlanId The id number of the Vlan to retrieve.
     * @return The existing or newly created Vlan for the provided Vlan-id.
     */
    public Vlan getVlan(int vlanId) {
        Optional<Vlan> optVlan = vlans.stream().filter(v -> v.id == vlanId).findAny();
        Vlan vlan;
        if (optVlan.isPresent()) {
            vlan = optVlan.get();
        } else {
            vlan = new Vlan(vlanId);
            vlans.add(vlan);
        }
        return vlan;
    }

    Set<Vlan> getVlans(Port port) {
        if (!ports.contains(port)) {
            return Collections.EMPTY_SET;
        }
        return vlans.stream()
                .filter(vlan -> vlan.ports.contains(port))
                .collect(Collectors.toSet()
                );
    }

    @Override
    public String toString() {
        return String.format("%s", GUID);
    }

    @Override
    public int hashCode() {
        return GUID == null ? 0 : GUID.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final PhysicalNode other = (PhysicalNode) obj;
        if (!Objects.equals(this.GUID, other.GUID)) {
            return false;
        }
        return true;
    }

    public Port portSees(Mac mac) {
        Port ret = null;
        for( Port port : ports ) {
            if( port.macs().isEmpty() ) {
                continue;
            }
            if( port.macs().contains(mac) ) {
                ret = port;
                break;
            }
        }
        return ret;
    }

    public void addCommand(String command) {
        this.commandsProcessed.add(command);
    }

}
