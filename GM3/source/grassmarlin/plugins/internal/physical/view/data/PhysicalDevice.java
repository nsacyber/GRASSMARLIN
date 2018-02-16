package grassmarlin.plugins.internal.physical.view.data;

import grassmarlin.Event;
import grassmarlin.Logger;
import grassmarlin.plugins.internal.physical.view.data.intermediary.SessionConnectedPhysicalGraph;
import grassmarlin.session.HardwareVertex;
import grassmarlin.session.PhysicalConnection;
import grassmarlin.session.Property;
import grassmarlin.session.PropertyContainer;

import java.util.*;

public class PhysicalDevice {
    private final Map<HardwareVertex, PhysicalEndpoint> lookupEndpoints;

    private final HardwareVertex vertex;
    private final Map<PhysicalEndpoint, PhysicalConnection> ports;
    private final Map<PhysicalEndpoint, Collection<PhysicalConnection>> portConnections;
    private final Set<PhysicalEndpoint> trunks;

    public final Event<PhysicalEndpoint> onPortAdded;
    public final Event<PhysicalEndpoint> onPortRemoved;
    public final Event<PhysicalConnection> onConnectionSuppressed;
    public final Event<PhysicalConnection> onConnectionUnsuppressed;

    public PhysicalDevice(final Map<HardwareVertex, PhysicalEndpoint> lookupEndpoints, final HardwareVertex vertex) {
        this.lookupEndpoints = lookupEndpoints;
        this.vertex = vertex;

        this.ports = new HashMap<>();
        this.portConnections = new HashMap<>();
        this.trunks = new HashSet<>();

        this.onPortAdded = new Event<>(Event.PROVIDER_IN_THREAD);
        this.onPortRemoved = new Event<>(Event.PROVIDER_IN_THREAD);
        this.onConnectionSuppressed = new Event<>(Event.PROVIDER_IN_THREAD);
        this.onConnectionUnsuppressed = new Event<>(Event.PROVIDER_IN_THREAD);
    }

    public String getName() {
        return this.vertex.getAddress().toString();
    }

    public HardwareVertex getVertex() {
        return this.vertex;
    }

    public Collection<PhysicalEndpoint> getPorts() {
        synchronized(this.ports) {
            return new ArrayList<>(this.ports.keySet());
        }
    }

    public PhysicalConnection getPortDetails(final PhysicalEndpoint port) {
        return this.ports.get(port);
    }

    public void promoteToPort(final PhysicalConnection connection, final PhysicalEndpoint endpoint) {
        if(endpoint == null) {
            Logger.log(Logger.Severity.ERROR, "Attempted to promote null to a Port on the Physical Graph.");
        }
        synchronized(this.ports) {
            //Neither the connection nor the endpoint can appear more than once.
            if (!this.ports.containsValue(connection)) {
                if (null == this.ports.putIfAbsent(endpoint, connection)) {
                    endpoint.getVertex().onPropertyChanged.addHandler(this.handlerVertexPropertyChanged);
                    endpoint.setOwner(this);
                    this.onPortAdded.call(endpoint);
                }
            }
        }
    }
    public void removePort(final PhysicalEndpoint endpoint) {
        //TODO: Add PhysicalDevice.removePort implementation.
        endpoint.setOwner(null);
        throw new UnsupportedOperationException("TODO: Add PhysicalDevice.removePort implementation.");
    }

    private final Event.EventListener<PropertyContainer.PropertyEventArgs> handlerVertexPropertyChanged = this::handleVertexPropertyChanged;
    private void handleVertexPropertyChanged(final Event<PropertyContainer.PropertyEventArgs> event, final PropertyContainer.PropertyEventArgs args) {
        switch(args.getName()) {
            case SessionConnectedPhysicalGraph.PROPERTY_TRUNK:
                final Set<Property<?>> values = (args.getContainer().getProperties().get(SessionConnectedPhysicalGraph.PROPERTY_TRUNK));
                if(values == null || !values.stream().filter(property -> property.getValue().equals(Boolean.TRUE)).findAny().isPresent()) {
                    //If there are no values, then it is not a trunk.
                    //If there are no True values, then it is not a trunk.
                    this.makeNonTrunk(this.lookupEndpoints.get(args.getContainer()));
                } else {
                    //There are values and at least one is true.
                    this.makeTrunk(this.lookupEndpoints.get(args.getContainer()));
                }
                break;
        }
    }

    private void makeTrunk(final PhysicalEndpoint endpoint) {
        if(endpoint == null) {
            return;
        }

        if(this.trunks.add(endpoint)) {
            // Find every edge that connects to endpoint and suppress it if it connects to a non-port endpoint, unsuppress it otherwise
            for(final PhysicalConnection connection : this.portConnections.get(endpoint)) {
                final PhysicalEndpoint endpointOther = this.lookupEndpoints.get(connection.other(endpoint.getVertex()));
                if(endpointOther.isPort()) {
                    this.onConnectionUnsuppressed.call(connection);
                } else {
                    this.onConnectionSuppressed.call(connection);
                }
            }
        }
    }
    private void makeNonTrunk(final PhysicalEndpoint endpoint) {
        if(endpoint == null) {
            return;
        }

        if(this.trunks.remove(endpoint)) {
            // Inverse of the makeTrunk logic above
            for(final PhysicalConnection connection : this.portConnections.get(endpoint)) {
                final PhysicalEndpoint endpointOther = this.lookupEndpoints.get(connection.other(endpoint.getVertex()));
                if(!endpointOther.isPort()) {
                    this.onConnectionUnsuppressed.call(connection);
                } else {
                    this.onConnectionSuppressed.call(connection);
                }
            }
        }
    }
}
