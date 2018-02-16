package grassmarlin.plugins.internal.physical.view.data.intermediary;

import grassmarlin.Event;
import grassmarlin.Logger;
import grassmarlin.plugins.internal.physical.view.data.PhysicalCloud;
import grassmarlin.plugins.internal.physical.view.data.PhysicalDevice;
import grassmarlin.plugins.internal.physical.view.data.PhysicalEndpoint;
import grassmarlin.session.HardwareVertex;
import grassmarlin.session.LogicalVertex;
import grassmarlin.session.PhysicalConnection;
import grassmarlin.session.hardwareaddresses.Device;

import java.util.*;
import java.util.stream.Collectors;

public class Segment {
    private final Map<HardwareVertex, PhysicalDevice> lookupDevices;
    private final Map<HardwareVertex, PhysicalEndpoint> lookupEndpoints;
    private final Map<ConnectionTreeNode, PhysicalCloud> lookupClouds;
    private final Collection<PhysicalConnection> connectionsActive;
    private final Map<PhysicalConnection, Set<PhysicalDevice>> connectionsSuppressed;
    private final ConnectionTree connectionTree;

    private final Map<Event<PhysicalConnection>, PhysicalDevice> lookupDeviceFromEvent;

    public final Event<PhysicalCloud> onCloudCreated;
    public final Event<PhysicalCloud> onCloudRemoved;
    public final Event<PhysicalDevice> onDeviceCreated;
    public final Event<PhysicalEndpoint> onEndpointCreated;
    public final Event<PhysicalEndpoint> onEndpointRemoved;
    public final Event<Object> onEndpointAddedToDevice;
    public final Event<Object> onEndpointRemovedFromDevice;

    public Segment() {
        this.onCloudCreated = new Event<>(Event.PROVIDER_IN_THREAD);
        this.onCloudRemoved = new Event<>(Event.PROVIDER_IN_THREAD);
        this.onDeviceCreated = new Event<>(Event.PROVIDER_IN_THREAD);
        this.onEndpointCreated = new Event<>(Event.PROVIDER_IN_THREAD);
        this.onEndpointRemoved = new Event<>(Event.PROVIDER_IN_THREAD);
        this.onEndpointAddedToDevice = new Event<>(Event.PROVIDER_IN_THREAD);
        this.onEndpointRemovedFromDevice = new Event<>(Event.PROVIDER_IN_THREAD);

        this.lookupDevices = new HashMap<>();
        this.lookupEndpoints = new HashMap<>();
        this.lookupClouds = new HashMap<>();
        this.connectionsActive = new HashSet<>();
        this.connectionsSuppressed = new HashMap<>();
        this.lookupDeviceFromEvent = new HashMap<>();

        //TODO: The connectionTree needs event handlers.  The handlers should manage the creation/destruction of clouds and manage their membership
        this.connectionTree = new ConnectionTree();
        this.connectionTree.onTreeCreated.addHandler(this.handlerTreeCreated);
        this.connectionTree.onTreeRemoved.addHandler(this.handlerTreeRemoved);
        this.connectionTree.onTreeMembershipChanged.addHandler(this.handlerTreeMembershipChanged);
    }

    public void absorb(final Segment other) {
        //When we absorb a segment, we add its contents to our own, but we don't need to announce events--the same objects exist, they are simply managed under a new segment, but nothing should care about that; segments exist as a means to reduce the scope of calculations, but the objects they contain don't particularly care to which segment they belong.
        this.lookupEndpoints.putAll(other.lookupEndpoints);
        this.lookupDevices.putAll(other.lookupDevices);
        this.lookupClouds.putAll(other.lookupClouds);
        this.connectionsActive.addAll(other.connectionsActive);
        this.connectionsSuppressed.putAll(other.connectionsSuppressed);
        this.connectionTree.absorb(other.connectionTree);
    }

    // == ConnectionTree handlers
    private final Event.EventListener<ConnectionTreeNode> handlerTreeCreated = this::handleTreeCreated;
    private void handleTreeCreated(final Event<ConnectionTreeNode> event, final ConnectionTreeNode root) {
        final PhysicalCloud cloud = new PhysicalCloud(root.getAllLeafValues().stream().map(vertex -> lookupEndpoints.get(vertex)).collect(Collectors.toList()));
        final PhysicalCloud cloudOld = this.lookupClouds.put(root, cloud);
        if(cloudOld != null) {
            this.onCloudRemoved.call(cloudOld);
        }
        this.onCloudCreated.call(cloud);
    }
    private final Event.EventListener<ConnectionTreeNode> handlerTreeRemoved = this::handleTreeRemoved;
    private void handleTreeRemoved(final Event<ConnectionTreeNode> event, final ConnectionTreeNode root) {
        final PhysicalCloud cloudOld = this.lookupClouds.get(root);
        if(cloudOld != null) {
            this.onCloudRemoved.call(cloudOld);
        }
    }
    private final Event.EventListener<ConnectionTreeNode> handlerTreeMembershipChanged = this::handleTreeMembershipChanged;
    private void handleTreeMembershipChanged(final Event<ConnectionTreeNode> event, final ConnectionTreeNode root) {
        final PhysicalCloud cloud = new PhysicalCloud(root.getAllLeafValues().stream().map(vertex -> lookupEndpoints.get(vertex)).collect(Collectors.toList()));
        final PhysicalCloud cloudOld = this.lookupClouds.put(root, cloud);
        if(cloudOld != null) {
            this.onCloudRemoved.call(cloudOld);
        }
        this.onCloudCreated.call(cloud);

    }

    // == Modify Segment Contents

    private void checkForSuppressedConnections(final PhysicalEndpoint portNew) {
        //portNew was just promoted to a port so we have to check to see if this causes any connections to be suppressed.
/*        for(final PhysicalConnection connection : portNew.getConnections()) {
            //The other endpoint of the connection is something that should be connected to this port...
            // TODO: We need to evaluate the connections to each of those entities and suppress them?
        }
        */
    }

    public void addLogicalVertex(final LogicalVertex vertex) {
        final PhysicalEndpoint endpoint = this.lookupEndpoints.get(vertex.getHardwareVertex());
        if(endpoint != null) {
            endpoint.addLogicalAddress(vertex.getLogicalAddress());
        }
    }

    public void addHardwareVertex(final HardwareVertex vertex) {
        if(vertex.getAddress() instanceof Device) {
            final PhysicalDevice deviceNew = new PhysicalDevice(this.lookupEndpoints, vertex);
            this.lookupDevices.put(vertex, deviceNew);
            this.lookupDeviceFromEvent.put(deviceNew.onConnectionSuppressed, deviceNew);
            this.lookupDeviceFromEvent.put(deviceNew.onConnectionUnsuppressed, deviceNew);
            deviceNew.onConnectionSuppressed.addHandler(this.handlerOnConnectionSuppressed);
            deviceNew.onConnectionUnsuppressed.addHandler(this.handlerOnConnectionUnsuppressed);
            this.onDeviceCreated.call(deviceNew);
        } else {
            final PhysicalEndpoint endpointNew = new PhysicalEndpoint(vertex);
            this.lookupEndpoints.put(vertex, endpointNew);
            this.onEndpointCreated.call(endpointNew);
            this.connectionTree.addVertex(vertex);
        }
    }

    private Event.EventListener<PhysicalConnection> handlerOnConnectionSuppressed = this::handleOnConnectionSuppressed;
    private void handleOnConnectionSuppressed(final Event<PhysicalConnection> event, final PhysicalConnection connection) {
        //If anything suppresses an endpoint, it is suppressed.
        //If it is already suppressed, we have nothing else to do.
        if(this.connectionsSuppressed.computeIfAbsent(connection, k -> new HashSet<>()).add(lookupDeviceFromEvent.get(event))) {
            this.connectionsActive.remove(connection);
            this.connectionTree.removeConnection(connection);
        }
    }

    private Event.EventListener<PhysicalConnection> handlerOnConnectionUnsuppressed = this::handleOnConnectionUnsuppressed;
    private void handleOnConnectionUnsuppressed(final Event<PhysicalConnection> event, final PhysicalConnection connection) {
        final Set<PhysicalDevice> set = this.connectionsSuppressed.get(connection);
        if(set != null && set.remove(this.lookupDeviceFromEvent.get(event))) {
            //If it is still suppressed, then there is nothing else to do.
            if(set.isEmpty()) {
                this.connectionsSuppressed.remove(connection);
                this.connectionsActive.add(connection);
                this.connectionTree.addConnection(connection);
            }
        }
    }
    //This is generally only expected to be called when a vertex is marked as a broadcast vertex.
    public boolean removeHardwareVertex(final HardwareVertex vertex) {
        for(final PhysicalConnection connectionToRemove : this.connectionsActive.stream().filter(connection -> connection.getSource().equals(vertex) || connection.getDestination().equals(vertex)).collect(Collectors.toList())) {
            this.removeConnection(connectionToRemove);
        }
        this.connectionsSuppressed.entrySet().removeIf(entry -> entry.getKey().getSource().equals(vertex) || entry.getKey().getDestination().equals(vertex));

        this.connectionTree.removeVertex(vertex);
        if(vertex.getAddress() instanceof Device) {
            //We really shouldn't be doing this, but that is not a good enough reason to avoid the implementation...
            //Actually, on second thought, it is.  This is only middleware to link the Session to the Physical Graph, and removing vertices is already rare.  You can't have a broadcast device, and you can't remove a device that was explicitly imported, not at this layer.
            throw new IllegalArgumentException("Cannot remove a device from the physical graph.");
        } else {
            final PhysicalEndpoint endpointRemoved = this.lookupEndpoints.remove(vertex);
            if(endpointRemoved != null) {
                this.onEndpointRemoved.call(endpointRemoved);
                //Remove from tree
            }
        }
        //TODO: Clean up event listeners
        //If, after removing the vertex, this segment is fragmented, return false to indicate to the SessionConnectedPhysicalGraph that the segment has to be removed and new segments constructed.
        return !isFragmented();
    }

    public void addConnection(final PhysicalConnection connection) {
        //We can't record an edge unless we have both endpoints
        if((this.lookupEndpoints.containsKey(connection.getSource()) || this.lookupDevices.containsKey(connection.getSource())) &&
                (this.lookupEndpoints.containsKey(connection.getDestination())) || this.lookupDevices.containsKey(connection.getDestination())) {

            //This connection might be ignored, added, or added suppressed.
            if(this.lookupDevices.containsKey(connection.getSource()) && this.lookupDevices.containsKey(connection.getDestination())) {
                //Ignore (both endpoints are devices--this connection shouldn't exist)
                Logger.log(Logger.Severity.PEDANTIC_DEVELOPER_SPAM, "A link between devices was detected--devices should only connect to their ports. [%s]", connection);
            } else if(this.lookupDevices.containsKey(connection.getSource())) {
                // The source is a device, so the destination should be made into a port.  This is an active connection.
                if(this.connectionsActive.add(connection)) {
                    this.lookupDevices.get(connection.getSource()).promoteToPort(connection, lookupEndpoints.get(connection.getDestination()));
                    this.checkForSuppressedConnections(this.lookupEndpoints.get(connection.getSource()));
                }
            } else if(this.lookupDevices.containsKey(connection.getDestination())) {
                // The destination is a device, so the source should be made into a port.  This is an active connection.
                if(this.connectionsActive.add(connection)) {
                    this.lookupDevices.get(connection.getDestination()).promoteToPort(connection, lookupEndpoints.get(connection.getSource()));
                    this.checkForSuppressedConnections(this.lookupEndpoints.get(connection.getDestination()));
                }
            } else {
                //We should add the connection regardless of any other considerations--the endpoint-port-device relationship should determine whether or not to suppress it, but it has to be tracked either way.
                final PhysicalEndpoint endpointSource = lookupEndpoints.get(connection.getSource());
                final PhysicalEndpoint endpointDestination = lookupEndpoints.get(connection.getDestination());

                if(endpointSource.isPort() || endpointDestination.isPort()) {
                    //This is either a trunk line (both are ports) or is a connection between a port and an endpoint.
                    //In either case, the handling is the same, but for very different reasons.
                    if(this.connectionsActive.add(connection)) {
                        endpointSource.addConnection(connection, endpointDestination);
                        endpointDestination.addConnection(connection, endpointSource);
                        this.connectionTree.addConnection(connection);
                    }
                } else if(endpointSource.connectsToPort() && endpointDestination.connectsToPort()) {
                    //If both endpoints connect to ports, then, in theory, routing logic should handle this, so we can add this edge, suppressed.
                    final HashSet<PhysicalDevice> set = new HashSet<>();
                    set.add(endpointSource.getOwner());
                    set.add(endpointDestination.getOwner());
                    this.connectionsSuppressed.put(connection, set);
                } else {
                    //We accept the edge--we might suppress it later, but for now it is valid.
                    if(this.connectionsActive.add(connection)) {
                        endpointSource.addConnection(connection, endpointDestination);
                        endpointDestination.addConnection(connection, endpointSource);
                        this.connectionTree.addConnection(connection);
                    }
                }
            }
        }
    }

    //We don't explicitly remove connections--we only remove them because we removed a vertex and, in so doing, we have to remove all the connections involving that vertex.
    private void removeConnection(final PhysicalConnection connection) {
        this.connectionsSuppressed.remove(connection);
        if(this.connectionsActive.remove(connection)) {
            //TODO: If an endpoint of the connection is a device, the other endpoint should no longer be a port and should no longer be owned by the device.
        }
    }

    private boolean isFragmented() {
        //Check for the trivial cases first.
        if(this.lookupDevices.isEmpty() && this.lookupEndpoints.isEmpty()) {
            //If there are no endpoints or devices, then it isn't fragmented, because it is empty.
            return false;
        }
        if(this.connectionsActive.isEmpty()) {
            //If there are no active connections, then it is fragmented if there are more than 1 endpoints/devices.
            return this.lookupDevices.size() + this.lookupEndpoints.size() == 1;
        }

        //Check to ensure that, from any node, all other nodes are reachable by following the defined connections.
        //We can become fragmented when a vertex is removed (perhaps because it was identified as a broadcast address)
        //We only track active connections; suppressed connections should always be redundant with the active graph (A-port-device-port-port-device-port-B instead of A-B)
        final Collection<HardwareVertex> verticesVisited = new HashSet<>();
        final List<HardwareVertex> verticesPending = new LinkedList<>();
        verticesPending.add(this.connectionsActive.iterator().next().getSource());    //We have already established that there is at least one connection.

        while(!verticesPending.isEmpty()) {
            final HardwareVertex vertex = verticesPending.remove(0);
            verticesVisited.add(vertex);

            verticesPending.addAll(this.connectionsActive.stream().map(connection -> connection.getSource().equals(vertex) ? connection.getDestination() : connection.getDestination().equals(vertex) ? connection.getSource() : null).filter(endpoint -> endpoint != null).distinct().collect(Collectors.toList()));
        }

        return verticesVisited.size() == this.lookupDevices.size() + this.lookupEndpoints.size();
    }

    public ConnectionTree getConnectionTree() {
        return this.connectionTree;
    }
}
