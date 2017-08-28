package grassmarlin.plugins.internal.logicalview;

import com.sun.xml.internal.txw2.output.IndentingXMLStreamWriter;
import grassmarlin.Event;
import grassmarlin.Logger;
import grassmarlin.RuntimeConfiguration;
import grassmarlin.session.Edge;
import grassmarlin.session.LogicalVertex;
import grassmarlin.session.Session;
import grassmarlin.session.ThreadManagedState;
import grassmarlin.session.pipeline.LogicalAddressMapping;
import grassmarlin.session.pipeline.Network;

import javax.xml.stream.*;
import javax.xml.stream.events.XMLEvent;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

public abstract class LogicalGraph {
    protected final Plugin.LogicalGraphState statePlugin;
    protected final Event.IAsyncExecutionProvider executor;

    //A list of all the vertices in the LogicalGraph
    protected final List<GraphLogicalVertex> vertices;
    //A mapping of LogicalVertex objects (in Session) to the GraphLogicalVertex which is the top-level entry to the graph for that LogicalVertex.
    protected final Map<LogicalVertex, GraphLogicalVertex> mappingTopLevelVertices;
    protected final LinkedList<LogicalVertex> pendingVertices;

    //A list of all the edges in the LogicalGraph.
    protected final List<GraphLogicalEdge> edges;
    //GraphLogicalEdges wrap one or more Edge objects.
    protected final Map<Edge<LogicalAddressMapping>, GraphLogicalEdge> mappingEdges;
    protected final Set<Edge<LogicalAddressMapping>> pendingEdges;
    protected final Map<Session.BidirectionalAddressPair, GraphLogicalEdge> lookupEdgesByTopLevelAddressPair;

    protected final List<Session.PacketEvent> pendingPackets;
    protected final List<Network> networks;

    protected final Map<GraphLogicalVertex, List<GraphLogicalEdge>> edgesByVertex;

    public final Event<GraphLogicalVertex> onLogicalGraphVertexCreated;
    public final Event<GraphLogicalVertex> onLogicalGraphVertexRemoved;
    public final Event<GraphLogicalEdge> onLogicalGraphEdgeCreated;
    public final Event<GraphLogicalEdge> onLogicalGraphEdgeRemoved;

    protected final ThreadManagedState state = new ThreadManagedState(RuntimeConfiguration.UPDATE_INTERVAL_MS, "LogicalGraph", Event.createThreadQueueProvider()) {
        @Override
        public void validate() {
            if (this.hasFlag(LogicalGraph.this.vertices)) {
                final ArrayList<LogicalVertex> verticesNew = new ArrayList<>();

                verticesNew.addAll(LogicalGraph.this.pendingVertices);
                LogicalGraph.this.pendingVertices.clear();

                // Rebuild list of top-level vertices by adding the elements in verticesNew

                // Find the top-level vertices in verticesNew--these may not be top level within the LogicalGraph, but only these elements have to be tested for that.
                final List<LogicalVertex> intermediateNewTopLevelVertices = verticesNew.stream()
                        .filter(vertex -> !verticesNew.stream().anyMatch(inner -> (vertex != inner) && (vertex.getHardwareVertex().getAddress().equals(inner.getHardwareVertex().getAddress())) && inner.getLogicalAddress().contains(vertex.getLogicalAddress())))
                        .collect(Collectors.toList());

                // Refine newTopLevelVertices to list of actual top-levels
                final List<LogicalVertex> finalizedNewTopLevelVertices = intermediateNewTopLevelVertices.stream()
                        .filter(vertex -> !vertices.stream().anyMatch(graphVertex -> (vertex.getHardwareVertex().getAddress().equals(graphVertex.getVertex().getHardwareVertex().getAddress())) && graphVertex.getVertex().getLogicalAddress().contains(vertex.getLogicalAddress())))
                        .collect(Collectors.toList());

                //This is where we used to find the vertices that need to be demoted, but those shouldn't exist, so we don't do that anymore.  We now use the honor system where parsers have to build from root-to-leaf
                // Create new LogicalGraphVertices for each member of finalizedNewTopLevelVertices.
                for (final LogicalVertex vertex : finalizedNewTopLevelVertices) {
                    LogicalGraph.this.addVertex(vertex);
                }

                // Anything not in the list of new top levels should be added as a subordinate
                verticesNew.removeAll(finalizedNewTopLevelVertices);
                for (final LogicalVertex vertex : verticesNew) {
                    final GraphLogicalVertex parent = LogicalGraph.this.vertices.stream()
                            .filter(existing -> existing.getVertex().getHardwareVertex().equals(vertex.getHardwareVertex()) && existing.getVertex().getLogicalAddress().contains(vertex.getLogicalAddress()))
                            .findAny().orElse(null);
                    if (parent == null) {
                        Logger.log(Logger.Severity.ERROR, "Error while building logical graph: orphaned vertex (%s)", vertex);
                    } else {
                        mappingTopLevelVertices.put(vertex, parent);
                        parent.addChildAddress(vertex);
                    }
                }
            }

            if (this.hasFlag(LogicalGraph.this.edges)) {
                final ArrayList<Edge<LogicalAddressMapping>> edgesNew = new ArrayList<>();

                edgesNew.addAll(LogicalGraph.this.pendingEdges);
                LogicalGraph.this.pendingEdges.clear();

                for (final Edge<LogicalAddressMapping> edge : edgesNew) {
                    LogicalGraph.this.addEdge(edge);
                }
            }

            if (this.hasFlag(LogicalGraph.this.pendingPackets)) {
                for (final Session.PacketEvent event : LogicalGraph.this.pendingPackets) {
                    LogicalGraph.this.mappingEdges.get(event.getEdge()).addPacket(event);
                }
                LogicalGraph.this.pendingPackets.clear();
            }

            if (this.hasFlag(LogicalGraph.this.networks)) {
                for (final GraphLogicalVertex vertex : LogicalGraph.this.vertices) {
                    vertex.testAndSetNetworks(LogicalGraph.this.networks);
                }
            }
        }
    };


    protected LogicalGraph(final Plugin.LogicalGraphState statePlugin, final Event.IAsyncExecutionProvider executor) {
        this.statePlugin = statePlugin;
        this.executor = executor;

        this.onLogicalGraphVertexCreated = new Event<>(executor);
        this.onLogicalGraphVertexRemoved = new Event<>(executor);
        this.onLogicalGraphEdgeCreated = new Event<>(executor);
        this.onLogicalGraphEdgeRemoved = new Event<>(executor);

                /*  The list of top-level vertices shouldn't change a lot.  Primarily additions, it will grow slowly, limited by
        the number of logical nodes in the network.  Since it only tracks top-level nodes, it should remain on a scale
        where tracking two copies of the backing array is tolerable.
            The frequency of changes should be fairly low after initial population.
            CopyOnWriteArrayList should eliminate the need to synchronize with downstream consumers.
         */
        this.vertices = new LinkedList<>();//CopyOnWriteArrayList<>();

        /*  Benchmarking suggests that IdentityHashMap performs worse.  It is a viable alternative, but benchmarking of
        large scale sets yielded identical read time but worse insertion performance for the IdentityHashMap.  The
        restricted logic of the IdentityHashMap is fine--this operates within the common range of both maps, but the
        difference in read time vanished to 0ns between the 10k and 100k item benchmarks while the IdentityHashMap
        consistently reported insertion times that were at least 10% higher.  The IdentityHashMap did have lower seek
        times for smaller data sets, but for smaller sets the difference won't have a measurable impact to the user and
        we anticipate data on scales where the difference is reduced to 0.
         */
        this.mappingTopLevelVertices = new HashMap<>();
        this.pendingVertices = new LinkedList<>();

        this.edges = new CopyOnWriteArrayList<>();
        this.mappingEdges = new HashMap<>();
        this.pendingEdges = new HashSet<>();
        this.lookupEdgesByTopLevelAddressPair = new HashMap<>();

        this.edgesByVertex = new HashMap<>();

        this.pendingPackets = new LinkedList<>();
        this.networks = new ArrayList<>();
    }


    // == Content Accessors ==
    public List<GraphLogicalVertex> getVertices() {
        synchronized(this.vertices) {
            return new ArrayList<>(this.vertices);
        }
    }
    public List<GraphLogicalEdge> getEdges() {
        synchronized(this.edges) {
            return new ArrayList<>(this.edges);
        }
    }
    public List<Network> getNetworks() {
        synchronized(this.networks) {
            return new ArrayList<>(this.networks);
        }
    }


    //<editor-fold desc="Interface to drive downstream logic, called from ThreadManagedState">

    /**
     * addHardwareVertex creates and returns a GraphLogicalVertex object to encapsulate the LogicalVertex provided as a parameter.
     * This does not handle the identification of subordinate LogicalVertices; that is the responsibility of the caller.
     * @param vertex
     */
    private GraphLogicalVertex addVertex(final LogicalVertex vertex) {
        final GraphLogicalVertex vertNew = new GraphLogicalVertex(this, vertex, statePlugin.getRuntimeConfig().getUiEventProvider());
        if(this.vertices.add(vertNew)) {
            this.mappingTopLevelVertices.put(vertex, vertNew);
            this.onLogicalGraphVertexCreated.call(vertNew);
            return vertNew;
        } else {
            return null;
        }
    }

    private GraphLogicalEdge addEdge(final Edge<LogicalAddressMapping> edgeNew) {
        final LogicalVertex vertexSource = this.statePlugin.getSession().nonblockingLogicalVertexFor(edgeNew.getSource());
        final LogicalVertex vertexDestination = this.statePlugin.getSession().nonblockingLogicalVertexFor(edgeNew.getDestination());

        final GraphLogicalVertex graphSource = mappingTopLevelVertices.get(vertexSource);
        final GraphLogicalVertex graphDestination = mappingTopLevelVertices.get(vertexDestination);

        final Session.BidirectionalAddressPair pairTopLevelAddresses = new Session.BidirectionalAddressPair(graphSource.getRootLogicalAddressMapping(), graphDestination.getRootLogicalAddressMapping());
        GraphLogicalEdge edgeExisting = lookupEdgesByTopLevelAddressPair.get(pairTopLevelAddresses);
        if (edgeExisting != null) {
            edgeExisting.trackEdge(edgeNew);
            mappingEdges.put(edgeNew, edgeExisting);
            return edgeExisting;
        }

        final GraphLogicalEdge edge = new GraphLogicalEdge(graphSource, graphDestination, statePlugin.getRuntimeConfig().getUiEventProvider());
        lookupEdgesByTopLevelAddressPair.put(pairTopLevelAddresses, edge);
        this.edges.add(edge);
        edge.trackEdge(edgeNew);

        List<GraphLogicalEdge> listSource = edgesByVertex.get(graphSource);
        if (listSource == null) {
            listSource = new ArrayList<>();
            edgesByVertex.put(graphSource, listSource);
        }
        listSource.add(edge);
        List<GraphLogicalEdge> listDestination = edgesByVertex.get(graphDestination);
        if (listDestination == null) {
            listDestination = new ArrayList<>();
            edgesByVertex.put(graphDestination, listDestination);
        }
        listDestination.add(edge);

        mappingEdges.put(edgeNew, edge);

        onLogicalGraphEdgeCreated.call(edge);

        return edge;
    }
    //</editor-fold>


    // == State Management ==
    public void waitForValid() {
        this.state.waitForValid();
    }

    public Plugin.LogicalGraphState getAttachedState() {
        return this.statePlugin;
    }

    // == Serialization ==
    public void writeTo(final OutputStream stream) {
        try {
            XMLOutputFactory xof = XMLOutputFactory.newInstance();
            XMLStreamWriter writer = new IndentingXMLStreamWriter(xof.createXMLStreamWriter(stream));

            writer.writeStartDocument();

            writer.writeStartElement("LogicalGraph");

            //Write the contents of vertices and edges.
            //We won't have references back into the session data--but we will recreate elements from session data through normal session interfaces on load, and can identify the logical graph elemetns by key

            writer.writeStartElement("Vertices");
            for(GraphLogicalVertex vertex : vertices) {
                writer.writeStartElement("Vertex");
                vertex.writeToXml(writer);
                writer.writeEndElement();
            }
            writer.writeEndElement();

            writer.writeStartElement("Edges");
            for(GraphLogicalEdge edge : edges) {
                writer.writeStartElement("Edge");
                edge.writeToXml(writer);
                writer.writeEndElement();
            }
            writer.writeEndElement();

            writer.writeEndDocument();

            writer.flush();
        } catch(XMLStreamException ex) {
            //TODO: Cry a little, then fail.
            ex.printStackTrace();
        }
    }

    public void readFrom(final InputStream stream) {
        //It would be bad if we tried to update things that don't exist yet.
        this.state.waitForValid();

        //TODO: Move this into a loader class that can be subclassed as necessary to handle newer/different versions.
        try {
            XMLInputFactory xif = XMLInputFactory.newInstance();
            XMLStreamReader reader = xif.createXMLStreamReader(stream);

            while(reader.hasNext()) {
                final int typeNext = reader.next();
                switch(typeNext) {
                    case XMLEvent.START_ELEMENT:
                        if(reader.getLocalName().equals("Vertex")) {
                            final String keyVertex = reader.getAttributeValue(null, "key");
                            final GraphLogicalVertex vert = this.vertices.stream().filter(vertex -> vertex.getKey().equals(keyVertex)).findAny().orElse(null);
                            if(vert == null) {
                                Logger.log(Logger.Severity.ERROR, "Unable to reassociate details with the following vertex: %s", keyVertex);
                            } else {
                                vert.readFromXml(reader);
                            }
                        } else if(reader.getLocalName().equals("Edge")) {
                            final String keyEdge = reader.getAttributeValue(null, "key");
                            final GraphLogicalEdge edge = this.edges.stream().filter(e -> e.getKey().equals(keyEdge)).findAny().orElse(null);
                            if(edge == null) {
                                //If the edge lookup fails, the endpoints might be reversed....
                                final GraphLogicalEdge edgeReverse = this.edges.stream().filter(e -> e.getReverseKey().equals(keyEdge)).findAny().orElse(null);
                                if(edgeReverse == null) {
                                    Logger.log(Logger.Severity.ERROR, "Unable to reassociate details with the following edge: %s", keyEdge);
                                } else {
                                    edgeReverse.readFromXml(reader);
                                }
                            } else {
                                edge.readFromXml(reader);
                            }
                        }
                        break;
                    case XMLEvent.END_ELEMENT:
                        if(reader.getLocalName().equals("LogicalGraph")) {
                            return;
                        }
                    default:
                        continue;
                }
            }
        } catch(XMLStreamException ex) {
            //TODO: Cry a little, then fail.
            ex.printStackTrace();
        } finally {
            this.waitForValid();
        }
    }
}
