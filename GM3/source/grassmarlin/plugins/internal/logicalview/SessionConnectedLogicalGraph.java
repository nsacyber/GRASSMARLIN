package grassmarlin.plugins.internal.logicalview;

import grassmarlin.Event;
import grassmarlin.Logger;
import grassmarlin.RuntimeConfiguration;
import grassmarlin.common.Confidence;
import grassmarlin.session.*;
import grassmarlin.session.pipeline.ILogicalPacketMetadata;
import grassmarlin.session.pipeline.LogicalAddressMapping;
import grassmarlin.session.pipeline.Network;
import grassmarlin.ui.common.SessionInterfaceController;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

public class SessionConnectedLogicalGraph extends LogicalGraph {
    private static final String NETWORK_PROPERTY_SOURCE = "SessionConnectedLogicalGraph";
    private static final String NETWORK_PROPERTY = "Network";
    private static final Confidence NETWORK_PROPERTY_CONFIDENCE = Confidence.MEDIUM_HIGH;

    private final Session session;

    // == Lookups - Mappings from Session to Logical Graph
    protected final Map<LogicalAddressMapping, GraphLogicalVertex> mappingTopLevelVertices;
    protected final Map<LogicalConnection, GraphLogicalEdge> mappingEdges;
    protected final Map<Session.LogicalAddressPair, GraphLogicalEdge> lookupEdgesByTopLevelAddressPair;

    // == Objects in Session that are not yet represented in the Logical Graph
    protected final List<LogicalVertex> pendingVertices;
    protected final List<LogicalConnection> pendingEdges;
    protected final List<ILogicalPacketMetadata> pendingPackets;

    // == We track the networks too, although this probably needs to befleshed out more.
    protected final List<Network> networks;

    // The state object manages updates to the presented state by applying the accumulated changes that have been reported by the Session
    protected final ThreadManagedState state;

    public SessionConnectedLogicalGraph(final Session session, final SessionInterfaceController controller) {
        super(controller.getUiExecutionProvider());

        this.session = session;

        this.mappingTopLevelVertices = new HashMap<>();
        this.mappingEdges = new ConcurrentHashMap<>();
        this.lookupEdgesByTopLevelAddressPair = new HashMap<>();

        this.pendingVertices = new LinkedList<>();
        this.pendingEdges = new LinkedList<>();
        this.pendingPackets = new LinkedList<>();

        this.networks = new ArrayList<>();

        this.state = new ThreadManagedState(RuntimeConfiguration.UPDATE_INTERVAL_MS, "LogicalGraph", controller.getUiExecutionProvider()) {
            @Override
            public void validate() {
                //It is important to note that, while a LogicalGraph may have items added or removed, a Session only creates new entries.
                //Therefore, while create and remove events may be fired, we never have to worry that we get a create/remove ordering wrong.

                if (this.hasFlag(SessionConnectedLogicalGraph.this.pendingVertices)) {
                    // Find the top-level vertices in pendingVertices--these may not be top level within the LogicalGraph, but only these elements have to be tested for that.
                    final List<LogicalVertex> intermediateNewTopLevelVertices = SessionConnectedLogicalGraph.this.pendingVertices.stream()
                            .filter(vertex -> !SessionConnectedLogicalGraph.this.pendingVertices.stream()
                                    .anyMatch(inner ->
                                            vertex != inner &&
                                            vertex.getHardwareVertex().getAddress().equals(inner.getHardwareVertex().getAddress()) &&
                                            inner.getLogicalAddress().contains(vertex.getLogicalAddress()))
                            )
                            .collect(Collectors.toList());

                    // Refine newTopLevelVertices to list of actual top-levels
                    final List<LogicalVertex> finalizedNewTopLevelVertices = intermediateNewTopLevelVertices.stream()
                            .filter(vertex -> !vertices.stream().anyMatch(graphVertex -> (vertex.getHardwareVertex().getAddress().equals(graphVertex.getVertex().getHardwareVertex().getAddress())) && graphVertex.getVertex().getLogicalAddress().contains(vertex.getLogicalAddress())))
                            .collect(Collectors.toList());

                    for (final LogicalVertex vertex : finalizedNewTopLevelVertices) {
                        final GraphLogicalVertex vertexNew = new GraphLogicalVertex(vertex);

                        SessionConnectedLogicalGraph.this.mappingTopLevelVertices.put(vertex.getLogicalAddressMapping(), vertexNew);
                        SessionConnectedLogicalGraph.this.addVertex(vertexNew);

                        final List<GraphLogicalVertex> verticesToRemove = new LinkedList<>();
                        for(GraphLogicalVertex vertexChild : SessionConnectedLogicalGraph.this.vertices) {
                            //Skip the identity case
                            if(vertexChild == vertexNew) {
                                continue;
                            }

                            if(vertexNew.getRootLogicalAddressMapping().contains(vertexChild.getRootLogicalAddressMapping())) {
                                vertexNew.addChildVertex(vertexChild);
                                verticesToRemove.add(vertexChild);

                                for(final LogicalAddressMapping key : mappingTopLevelVertices.entrySet().stream().filter(entry -> entry.getValue() == vertexChild).map(entry -> entry.getKey()).collect(Collectors.toList())) {
                                    mappingTopLevelVertices.put(key, vertexNew);
                                }
                            }
                        }
                        final LinkedList<GraphLogicalEdge> edgesToRemove = new LinkedList<>();
                        for(final GraphLogicalVertex vertexToRemove : verticesToRemove) {
                            SessionConnectedLogicalGraph.this.removeVertex(vertexToRemove);
                            edgesToRemove.addAll(SessionConnectedLogicalGraph.this.getEdgesForEndpoint(vertexToRemove));
                        }
                        //TODO: We now have to look through all the affected edges and categorize them:
                        // 1) Both endpoints are contained in the new vertex--the Edge can be removed.  The data will be lost, but it is already meaningless within the context of the logical graph
                        // 2) The Edge connects to an external Vertex and no other Edge connects to it.  We can redirect these, after a fashion.
                        // 3) The Edge connects to an external Vertex, and other Edges also connect to that Vertex.  This happens because A-B, A-C can become A-BC.

                        while(!edgesToRemove.isEmpty()) {
                            final GraphLogicalEdge edge = edgesToRemove.get(0);

                            if(vertexNew.getRootLogicalAddressMapping().contains(edge.getSource().getRootLogicalAddressMapping()) && vertexNew.getRootLogicalAddressMapping().contains(edge.getDestination().getRootLogicalAddressMapping())) {
                                //Both endpoints belong to the new vertex, so the edge is just going to be removed.
                                for(LogicalConnection key : mappingEdges.entrySet().stream().filter(entry -> entry.getValue() == edge).map(entry -> entry.getKey()).collect(Collectors.toList())) {
                                    mappingEdges.remove(key);
                                    lookupEdgesByTopLevelAddressPair.remove(new Session.LogicalAddressPair(
                                            edge.getSource().getRootLogicalAddressMapping(),
                                            edge.getDestination().getRootLogicalAddressMapping()
                                    ));
                                }
                                edgesToRemove.remove(edge);
                                removeEdge(edge);
                            } else {
                                //TODO: Get a list of all the edges to merge
                                //If the list has a single item, we can create a new edge and remove the old one easily.
                                final GraphLogicalVertex vertexOtherEndpoint = vertexNew.getRootLogicalAddressMapping().contains(edge.getSource().getRootLogicalAddressMapping()) ? edge.getDestination() : edge.getSource();
                                final List<GraphLogicalEdge> edgesToMerge = edgesToRemove.stream().filter(item -> item.getSource() == vertexOtherEndpoint || item.getDestination() == vertexOtherEndpoint).collect(Collectors.toList());

                                final GraphLogicalEdge edgeNew = new GraphLogicalEdge(this.executionProvider, vertexNew, vertexOtherEndpoint, edgesToMerge);
                                lookupEdgesByTopLevelAddressPair.put(new Session.LogicalAddressPair(vertexNew.getRootLogicalAddressMapping(), vertexOtherEndpoint.getRootLogicalAddressMapping()), edgeNew);

                                for(GraphLogicalEdge removed : edgesToMerge) {
                                    lookupEdgesByTopLevelAddressPair.remove(new Session.LogicalAddressPair(removed.getSource().getRootLogicalAddressMapping(), removed.getDestination().getRootLogicalAddressMapping()));

                                    removed.getPacketLists().stream().flatMap(packetList -> packetList.getEdges().stream()).forEach(edgeChild -> {
                                        mappingEdges.put(edgeChild, edgeNew);
                                    });
                                    edgesToRemove.remove(removed);
                                    removeEdge(removed);
                                }
                                addEdge(edgeNew);

                                //If the list has multiple items...  the process gets harder?  Maybe it is the same thing...  haven't looked into this enough yet to know.
                            }
                        }


                    }

                    // Anything not in the list of new top levels should be added as a subordinate
                    SessionConnectedLogicalGraph.this.pendingVertices.removeAll(finalizedNewTopLevelVertices);
                    for (final LogicalVertex vertex : SessionConnectedLogicalGraph.this.pendingVertices) {
                        final GraphLogicalVertex parent = SessionConnectedLogicalGraph.this.vertices.stream()
                                .filter(existing -> existing.getVertex().getHardwareVertex().equals(vertex.getHardwareVertex()) && existing.getVertex().getLogicalAddress().contains(vertex.getLogicalAddress()))
                                .findAny().orElse(null);
                        if (parent == null) {
                            Logger.log(Logger.Severity.ERROR, "Error while building logical graph: orphaned vertex (%s)", vertex);
                        } else {
                            SessionConnectedLogicalGraph.this.mappingTopLevelVertices.put(vertex.getLogicalAddressMapping(), parent);
                            parent.addChildAddress(vertex);
                        }
                    }

                    SessionConnectedLogicalGraph.this.pendingVertices.clear();
                }

                if (this.hasFlag(SessionConnectedLogicalGraph.this.pendingEdges)) {
                    for (final LogicalConnection edge : SessionConnectedLogicalGraph.this.pendingEdges) {
                        final GraphLogicalVertex source = mappingTopLevelVertices.get(edge.getSource().getLogicalAddressMapping());
                        final GraphLogicalVertex destination = mappingTopLevelVertices.get(edge.getDestination().getLogicalAddressMapping());
                        // This is what used to be done

                        final Session.LogicalAddressPair pairTopLevelAddresses = new Session.LogicalAddressPair(
                                source.getRootLogicalAddressMapping(),
                                destination.getRootLogicalAddressMapping()
                        );
                        //If we can find the top level address pair, then we already have an edge that connects these endpoints and this is a child edge.
                        final GraphLogicalEdge edgeExisting = lookupEdgesByTopLevelAddressPair.get(pairTopLevelAddresses);
                        if (edgeExisting != null) {
                            edgeExisting.trackEdge(edge);
                            SessionConnectedLogicalGraph.this.mappingEdges.put(edge, edgeExisting);
                            continue;
                        }

                        final GraphLogicalEdge edgeNew = new GraphLogicalEdge(controller.getUiExecutionProvider(), edge, source, destination);

                        SessionConnectedLogicalGraph.this.lookupEdgesByTopLevelAddressPair.put(pairTopLevelAddresses, edgeNew);
                        SessionConnectedLogicalGraph.this.mappingEdges.put(edge, edgeNew);

                        SessionConnectedLogicalGraph.this.addEdge(edgeNew);
                    }

                    SessionConnectedLogicalGraph.this.pendingEdges.clear();
                }

                //Don't check for the flag, check for remaining packets.
                if (!SessionConnectedLogicalGraph.this.pendingPackets.isEmpty()) {
                    final Iterator<ILogicalPacketMetadata> iterator =  SessionConnectedLogicalGraph.this.pendingPackets.iterator();
                    while(iterator.hasNext()) {
                        final ILogicalPacketMetadata packet = iterator.next();
                        final GraphLogicalEdge edgeExisting = SessionConnectedLogicalGraph.this.mappingEdges.get(
                                SessionConnectedLogicalGraph.this.session.existingEdgeBetween(packet.getSourceAddress(), packet.getDestAddress())
                        );
                        if(edgeExisting != null) {
                            edgeExisting.addPacket(packet);
                            iterator.remove();
                        } else {
                            //We can't find the edge yet, so we will wait.
                        }
                    }
                }

                if (this.hasFlag(SessionConnectedLogicalGraph.this.networks)) {
                    for (final GraphLogicalVertex vertex : SessionConnectedLogicalGraph.this.vertices) {
                        vertex.setProperties(
                                NETWORK_PROPERTY_SOURCE,
                                NETWORK_PROPERTY,
                                session.getNetworks().stream()
                                        .filter(network -> network.getValue().contains(vertex.getRootLogicalAddressMapping().getLogicalAddress()))
                                        .map(network -> new Property<LogicalAddress>(network.getValue(), NETWORK_PROPERTY_CONFIDENCE))
                                        .collect(Collectors.toList())
                        );
                    }
                }
            }
        };

        //We bind the events next-to-last.  The last thing we do is pretend that we received events for everything that already exists.
        session.onLogicalVertexCreated.addHandler(this.handlerLogicalVertexCreated);
        session.onNetworkChange.addHandler(this.handlerNetworkChanged);
        session.onLogicalConnectionCreated.addHandler(this.handlerEdgeCreated);
        //We don't have to listen for physical edges

        //TODO: Fire events to process initial state.

        session.executeOnLogicalVerticesWithLock(vertex -> SessionConnectedLogicalGraph.this.pendingVertices.add(vertex));
        session.executeOnLogicalEdgesWithLock(edge -> SessionConnectedLogicalGraph.this.pendingEdges.add(edge));
        //We need to invalidate both; this will do both as an atomic operation.
        this.state.invalidate(this.pendingVertices, () -> this.state.invalidate(this.pendingEdges));
    }

    @Override
    protected void processVertexAdded(final GraphLogicalVertex vertex) {
        super.processVertexAdded(vertex);

        vertex.setProperties(
                SessionConnectedLogicalGraph.NETWORK_PROPERTY_SOURCE,
                SessionConnectedLogicalGraph.NETWORK_PROPERTY,
                this.session.getNetworks().stream()
                        .filter(network -> network.getValue().contains(vertex.getRootLogicalAddressMapping().getLogicalAddress()))
                        .map(network -> new Property<LogicalAddress>(network.getValue(), SessionConnectedLogicalGraph.NETWORK_PROPERTY_CONFIDENCE))
                        .collect(Collectors.toList())
        );
    }

    //<editor-fold desc="Respond to events in Session">
    private final Event.EventListener<LogicalVertex> handlerLogicalVertexCreated = this::handleLogicalVertexCreated;
    private void handleLogicalVertexCreated(final Event<LogicalVertex> event, final LogicalVertex vertex) {
        this.state.invalidate(this.pendingVertices, () -> this.pendingVertices.add(vertex));
    }

    private final Event.EventListener<LogicalConnection> handlerEdgeCreated = this::handleEdgeCreated;
    private void handleEdgeCreated(final Event<LogicalConnection> event, final LogicalConnection edge) {
        //The ThreadManagedState will perform the bulk of the work.
        this.state.invalidate(this.pendingEdges, () -> this.pendingEdges.add(edge));
    }


    public void recordPacket(final ILogicalPacketMetadata packet) {
        final GraphLogicalEdge edgeExisting = this.mappingEdges.get(this.session.existingEdgeBetween(packet.getSourceAddress(), packet.getDestAddress()));
        if(edgeExisting != null) {
            edgeExisting.addPacket(packet);
        } else {
            this.state.invalidate(this.pendingPackets, () -> {
                this.pendingPackets.add(packet);
            });
        }
    }

    private final Event.EventListener<List<Network>> handlerNetworkChanged = this::handleNetworkChanged;
    private void handleNetworkChanged(final Event<List<Network>> event, List<Network> args) {
        this.state.invalidate(this.networks, () -> {
            this.networks.clear();
            this.networks.addAll(args);
        });
    }
    //</editor-fold>

    public void waitForValid() {
        this.state.waitForValid();
    }
}
