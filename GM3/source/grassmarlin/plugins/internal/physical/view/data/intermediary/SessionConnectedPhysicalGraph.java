package grassmarlin.plugins.internal.physical.view.data.intermediary;

import grassmarlin.Event;
import grassmarlin.RuntimeConfiguration;
import grassmarlin.plugins.internal.physical.view.data.*;
import grassmarlin.session.*;

import java.util.*;
import java.util.stream.Collectors;

public class SessionConnectedPhysicalGraph extends PhysicalGraph {
    public static final String PROPERTY_TRUNK = "Trunk";
    public static final String PROPERTY_GATEWAY = "Gateway";
    public static final String PROPERTY_BROADCAST = "Broadcast";

    private final Session session;

    private final Map<HardwareVertex, Segment> lookupSegmentByVertex;

    private final Map<PhysicalCloud, List<Object>> lookupCloudElements;

    public SessionConnectedPhysicalGraph(final RuntimeConfiguration config, final Session session) {
        super(config.getUiEventProvider());

        this.session = session;

        this.lookupSegmentByVertex = new HashMap<>();

        this.lookupCloudElements = new HashMap<>();

        this.session.onHardwareVertexCreated.addHandler(this.handlerHardwareVertexCreated);
        this.session.onPhysicalConnectionCreated.addHandler(this.handlerPhysicalConnectionCreated);
        this.session.onLogicalVertexCreated.addHandler(this.handlerLogicalVertexCreated);

        //TODO: Process initial state.
    }

    // == Accessors
    public Collection<Segment> getSegments() {
        synchronized(this.lookupSegmentByVertex) {
            return new HashSet<>(this.lookupSegmentByVertex.values());
        }
    }

    // == Handlers for Session Events (which will, generally, manipulate the sessions and related intermediate state)
    private final Event.EventListener<LogicalVertex> handlerLogicalVertexCreated = this::handleLogicalVertexCreated;
    private void handleLogicalVertexCreated(final Event<LogicalVertex> event, final LogicalVertex vertex) {
        synchronized(this) {
            this.lookupSegmentByVertex.get(vertex.getHardwareVertex()).addLogicalVertex(vertex);
        }
    }
    private final Event.EventListener<HardwareVertex> handlerHardwareVertexCreated = this::handleHardwareVertexCreated;
    private void handleHardwareVertexCreated(final Event<HardwareVertex> event, final HardwareVertex vertex) {
        synchronized(this) {
            vertex.onPropertyChanged.addHandler(this.handlerVertexPropertyChanged);

            if (vertex.hasPropertyValue(PROPERTY_BROADCAST, true)) {
                //If the vertex is a broadcast, we do not add it.
            } else {
                //Because it is not a broadcast address, we add it.
                //If we are creating a hardware vertex, we know it has no connections (yet) and, therefore, must belong to a new segment.
                final Segment segmentNew = createSegment();
                segmentNew.addHardwareVertex(vertex);
                this.lookupSegmentByVertex.put(vertex, segmentNew);
            }
        }
    }

    private final Event.EventListener<PropertyContainer.PropertyEventArgs> handlerVertexPropertyChanged = this::handleVertexPropertyChanged;
    private void handleVertexPropertyChanged(final Event<PropertyContainer.PropertyEventArgs> event, final PropertyContainer.PropertyEventArgs args) {
        synchronized(this) {
            if (args.getName().equals(PROPERTY_BROADCAST)) {
                final HardwareVertex vertex = (HardwareVertex) args.getContainer();
                if (args.getContainer().hasPropertyValue(PROPERTY_BROADCAST, true)) {
                    this.suppressVertex(vertex);
                } else {
                    this.unsuppressVertex(vertex);
                }
            }
        }
    }

    private final Event.EventListener<PhysicalConnection> handlerPhysicalConnectionCreated = this::handlePhysicalConnectionCreated;
    private void handlePhysicalConnectionCreated(final Event<PhysicalConnection> event, final PhysicalConnection connection) {
        if(connection.getSource().equals(connection.getDestination())) {
            //Ignore conenctions where the source and destination are the same.
            return;
        }
        synchronized(this) {
            final Segment segmentSource = this.lookupSegmentByVertex.get(connection.getSource());
            final Segment segmentDestination = this.lookupSegmentByVertex.get(connection.getDestination());

            if (segmentSource == null || segmentDestination == null) {
                //If either endpoint doesn't exist, we just skip the edge.
                return;
            } else if (segmentSource == segmentDestination) {
                //If the segments are the same, just add the edge
                segmentSource.addConnection(connection);
            } else {
                //TODO: Absorb the smaller into the larger, rather than destination into source
                this.replaceSegment(segmentDestination, segmentSource);
                segmentSource.addConnection(connection);
            }
        }
    }

    protected Segment createSegment() {
        final Segment segmentNew = new Segment();

        //TODO: Attach event handlers
        segmentNew.onEndpointCreated.addHandler(this.handlerEndpointCreated);
        segmentNew.onEndpointRemoved.addHandler(this.handlerEndpointRemoved);
        segmentNew.onDeviceCreated.addHandler(this.handlerDeviceCreated);
        segmentNew.onCloudCreated.addHandler(this.handlerCloudCreated);
        segmentNew.onCloudRemoved.addHandler(this.handlerCloudRemoved);
        segmentNew.onEndpointAddedToDevice.addHandler(null);
        segmentNew.onEndpointRemovedFromDevice.addHandler(null);

        return segmentNew;
    }
    protected void replaceSegment(final Segment segmentRemoved, final Segment segmentPreserved) {
        for(final HardwareVertex vertex : this.lookupSegmentByVertex.entrySet().stream().filter(entry -> entry.getValue() == segmentRemoved).map(entry -> entry.getKey()).collect(Collectors.toList())) {
            this.lookupSegmentByVertex.put(vertex, segmentPreserved);
        }
        segmentPreserved.absorb(segmentRemoved);

        segmentRemoved.onEndpointCreated.removeHandler(this.handlerEndpointCreated);
        segmentRemoved.onEndpointRemoved.removeHandler(this.handlerEndpointRemoved);
        segmentRemoved.onDeviceCreated.removeHandler(this.handlerDeviceCreated);
        segmentRemoved.onCloudCreated.removeHandler(this.handlerCloudCreated);
        segmentRemoved.onCloudRemoved.removeHandler(this.handlerCloudRemoved);
        //TODO: Remove event handlers from segmentRemoved
    }

    private void suppressVertex(final HardwareVertex vertex) {
        //When a vertex is suppressed then we still need to track connections related to that vertex in case it is ever unsuppressed.
    }
    private void unsuppressVertex(final HardwareVertex vertex) {

    }

    // == Handlers for Segment Events (which modify the Physical Graph entities)
    private final Event.EventListener<PhysicalEndpoint> handlerEndpointCreated = this::handleEndpointCreated;
    private void handleEndpointCreated(final Event<PhysicalEndpoint> event, final PhysicalEndpoint endpoint) {
        super.addPhysicalEndpoint(endpoint);
    }
    private final Event.EventListener<PhysicalEndpoint> handlerEndpointRemoved = this::handleEndpointRemoved;
    private void handleEndpointRemoved(final Event<PhysicalEndpoint> event, final PhysicalEndpoint endpoint) {
        super.removePhysicalEndpoint(endpoint);
    }

    private final Event.EventListener<PhysicalDevice> handlerDeviceCreated = this::handleDeviceCreated;
    private void handleDeviceCreated(final Event<PhysicalDevice> event, final PhysicalDevice device) {
        super.addPhysicalDevice(device);
    }

    //TODO: When a cloud is created we need to start listening for changes to its endpoint list and process the initial state.
    // - If there are 0 or 1 endpoints, we do nothing (the cloud shouldn't exist)
    // - If there are 2 endpoints, we create a wire between them.
    // - If there are more then 2 endpoints we need to construct an endpoint for the cloud and then create a wire from the cloud to every member.
    //Clouds have fixed content--we don't change the contents, we create new clouds.
    private final Event.EventListener<PhysicalCloud> handlerCloudCreated = this::handleCloudCreated;
    private void handleCloudCreated(final Event<PhysicalCloud> event, final PhysicalCloud cloud) {
        final List<Object> visuals;

        switch(cloud.getEndpoints().size()) {
            case 0:
            case 1:
                //We can ignore the cloud.
                return;
            case 2:
                //TODO: Create a single wire between the two endpoints
                final Iterator<PhysicalEndpoint> iterator = cloud.getEndpoints().iterator();
                visuals = Collections.singletonList(new PhysicalWire(iterator.next(), iterator.next()));
                break;
            default:
                //TODO: Add the cloud endpoint, create a wire between the cloud endpoint and each member.
                visuals = new ArrayList<>(cloud.getEndpoints().size() + 1);
                visuals.add(cloud.getInternalEndpoint());
                visuals.addAll(cloud.getEndpoints().stream().map(endpoint -> new PhysicalWire(endpoint, cloud.getInternalEndpoint())).collect(Collectors.toList()));
                break;
        }

        //TODO: Add all the elements of visuals to the display, cache the list for future removal.
        this.lookupCloudElements.put(cloud, visuals);
        for(final Object o : visuals) {
            if(o instanceof PhysicalWire) {
                super.addPhysicalWire((PhysicalWire)o);
            } else if(o instanceof PhysicalEndpoint) {
                super.addPhysicalEndpoint((PhysicalEndpoint)o);
            }
        }
    }
    private final Event.EventListener<PhysicalCloud> handlerCloudRemoved = this::handleCloudRemoved;
    private void handleCloudRemoved(final Event<PhysicalCloud> event, final PhysicalCloud cloud) {
        final List<Object> visuals = this.lookupCloudElements.remove(cloud);
        if(visuals != null) {
            for (final Object o : visuals) {
                if (o instanceof PhysicalWire) {
                    super.removePhysicalWire((PhysicalWire) o);
                } else if (o instanceof PhysicalEndpoint) {
                    super.removePhysicalEndpoint((PhysicalEndpoint) o);
                }
            }
        }
    }

}
