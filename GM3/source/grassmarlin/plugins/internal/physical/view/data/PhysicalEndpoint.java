package grassmarlin.plugins.internal.physical.view.data;

import grassmarlin.session.HardwareVertex;
import grassmarlin.session.LogicalAddress;
import grassmarlin.session.PhysicalConnection;

import java.util.*;
import java.util.stream.Collectors;

public class PhysicalEndpoint {
    private final HardwareVertex vertex;
    private final Set<LogicalAddress> logicalAddresses;
    private final Map<PhysicalConnection, PhysicalEndpoint> connections;
    private PhysicalDevice owner;

    public PhysicalEndpoint(final HardwareVertex vertex) {
        this.vertex = vertex;
        this.logicalAddresses = new HashSet<>();
        this.connections = new HashMap<>();
        this.owner = null;
    }

    public HardwareVertex getVertex() {
        return this.vertex;
    }

    public boolean addLogicalAddress(final LogicalAddress address) {
        return this.logicalAddresses.add(address);
    }

    public Collection<LogicalAddress> getLogicalAddresses() {
        return this.logicalAddresses;
    }

    public void setOwner(final PhysicalDevice owner) {
        this.owner = owner;
    }
    public PhysicalDevice getOwner() {
        return this.owner;
    }

    public boolean isPort() {
        return this.owner != null;
    }
    public boolean connectsToPort() {
        return this.connections.values().stream().anyMatch(endpoint -> endpoint.isPort());
    }

    public boolean addConnection(final PhysicalConnection connection, final PhysicalEndpoint endpointOther) {
        //TODO: Since this will be called for both endpoints, we can isolate the checks to allow a single endpoint to take responsibility for suppressing an edge, but need to define the exact mechanism for suppression better.
        if(this.connections.putIfAbsent(connection, endpointOther) == null) {
            if(endpointOther.isPort() || this.isPort()) {
                //TODO: A trunk port should suppress connections to a non-trunk port--this may not be the correct rule, which is why it isn't implemented yet.
                //We always accept all edges involving ports...
                return true;
            } else {
                //TODO: Test to see if either endpoint connects to a port..
            }
        }
        //HACK: Just need this to compile for now.
        return false;
    }

    public Collection<PhysicalConnection> getConnections() {
        return this.connections.keySet();
    }

    @Override
    public int hashCode() {
        if(this.vertex == null) {
            return super.hashCode();
        } else {
            return this.vertex.hashCode();
        }
    }

    @Override
    public boolean equals(final Object other) {
        //If this has a null vertex, then it is unique; we use a null vertex for things like clouds.
        if(this.vertex == null) {
            return this == other;
        }
        return other != null && (other instanceof PhysicalEndpoint) && this.getVertex().equals(((PhysicalEndpoint)other).getVertex());
    }

    @Override
    public String toString() {
        if(this.vertex != null) {
            return String.format("[%s {%s} %d %s]", this.vertex.getAddress(), this.logicalAddresses.stream().map(address -> address.toString()).collect(Collectors.joining(", ")), connections.size(), this.owner == null ? "" : this.owner.getName());
        } else {
            return "[Cloud]";
        }
    }
}
