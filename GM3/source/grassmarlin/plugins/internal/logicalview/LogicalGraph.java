package grassmarlin.plugins.internal.logicalview;

import grassmarlin.Event;
import grassmarlin.session.PropertyContainer;

import java.util.*;
import java.util.stream.Collectors;

public abstract class LogicalGraph {
    // The Edges and Vertices Observable Lists will be used in things like reports.  They also serve as the basis for operations that need to take a snapshot of the current state.
    protected final Set<GraphLogicalVertex> vertices;
    protected final Set<GraphLogicalEdge> edges;

    // These events are used to drive the creation and destruction of visual representations of individual elements (e.g. Visualizations).
    public final Event<GraphLogicalVertex> onLogicalGraphVertexCreated;
    public final Event<GraphLogicalVertex> onLogicalGraphVertexRemoved;
    public final Event<GraphLogicalEdge> onLogicalGraphEdgeCreated;
    public final Event<GraphLogicalEdge> onLogicalGraphEdgeRemoved;

    public final Event<String> onPropertyValuesChanged;

    protected LogicalGraph(final Event.IAsyncExecutionProvider executor) {
        this.onLogicalGraphVertexCreated = new Event<>(executor);
        this.onLogicalGraphVertexRemoved = new Event<>(executor);
        this.onLogicalGraphEdgeCreated = new Event<>(executor);
        this.onLogicalGraphEdgeRemoved = new Event<>(executor);

        this.onPropertyValuesChanged = new Event<>(executor);

        this.vertices = new HashSet<>();
        this.edges = new HashSet<>();
    }

    // == Content Accessors ==

    public void getVertices(final Collection<GraphLogicalVertex> collection) {
        synchronized(this.vertices) {
            collection.addAll(this.vertices);
        }
    }
    public void getEdges(final Collection<GraphLogicalEdge> collection) {
        synchronized(this.edges) {
            collection.addAll(this.edges);
        }
    }
    public void getAtomic(final Collection<GraphLogicalVertex> cVertices, final Collection<GraphLogicalEdge> cEdges) {
        synchronized(this.vertices) {
            synchronized(this.edges) {
                cVertices.addAll(this.vertices);
                cEdges.addAll(this.edges);
            }
        }
    }
    public void getGroupings(final Collection<String> collection) {
        synchronized(this.vertices) {
            collection.addAll(
                    this.vertices.stream()
                            .flatMap(vertex -> vertex.getProperties().keySet().stream())
                            .distinct()
                            .collect(Collectors.toList())
            );
        }
    }

    // == Wrappers for add/remove logic
    // Whenever an element is added or removed, these blocks are called.

    protected void processVertexAdded(final GraphLogicalVertex vertex) {
        vertex.onPropertyChanged.addHandler(this.handlerVertexPropertyChanged);

        this.onLogicalGraphVertexCreated.call(vertex);
    }

    protected void processVertexRemoved(final GraphLogicalVertex vertex) {
        vertex.onPropertyChanged.removeHandler(this.handlerVertexPropertyChanged);
        this.onLogicalGraphVertexRemoved.call(vertex);
    }

    private Event.EventListener<PropertyContainer.PropertyEventArgs> handlerVertexPropertyChanged = this::handleVertexPropertyChanged;
    private void handleVertexPropertyChanged(final Event<PropertyContainer.PropertyEventArgs> event, final PropertyContainer.PropertyEventArgs args) {
        this.onPropertyValuesChanged.call(args.getName());
    }

    // addVertex/Edge - Add a single item
    // removeVertex/Edge - Remove a single item
    // synchronizeVertex/Edge - Bulk addition and removal (from the ObservableList) and repeated calls to Events, as necessary, to make the lists look identical.

    protected void addVertex(final GraphLogicalVertex vertex) {
        synchronized(this.vertices) {
            if(this.vertices.add(vertex)) {
                this.processVertexAdded(vertex);
            }
        }
    }

    protected void synchronizeVertices(final Collection<GraphLogicalVertex> verticesValid) {
        synchronized(this.vertices) {
            final List<GraphLogicalVertex> pendingAdd = new LinkedList<>(verticesValid);
            pendingAdd.removeAll(this.vertices);
            final List<GraphLogicalVertex> pendingRemovals = new LinkedList<>(this.vertices);
            pendingRemovals.removeAll(verticesValid);

            this.vertices.removeAll(pendingRemovals);
            for(final GraphLogicalVertex removed : pendingRemovals) {
                this.processVertexRemoved(removed);
            }

            this.vertices.addAll(pendingAdd);
            for(final GraphLogicalVertex added : pendingAdd) {
                this.processVertexAdded(added);
            }
        }
    }

    protected void removeVertex(final GraphLogicalVertex vertex) {
        synchronized(this.vertices) {
            if(this.vertices.remove(vertex)) {
                this.processVertexRemoved(vertex);
            }
        }
    }

    protected void addEdge(final GraphLogicalEdge edge) {
        synchronized(this.edges) {
            if(this.edges.add(edge)) {
                onLogicalGraphEdgeCreated.call(edge);
            }
        }
    }

    protected void synchronizeEdges(final Collection<GraphLogicalEdge> edgesValid) {
        synchronized(this.edges) {
            final List<GraphLogicalEdge> pendingAdd = new LinkedList<>(edgesValid);
            pendingAdd.removeAll(this.edges);
            final List<GraphLogicalEdge> pendingRemovals = new LinkedList<>(this.edges);
            pendingRemovals.removeAll(edgesValid);

            this.edges.removeAll(pendingRemovals);
            for(final GraphLogicalEdge removed : pendingRemovals) {
                this.onLogicalGraphEdgeRemoved.call(removed);
            }

            this.edges.addAll(pendingAdd);
            for(final GraphLogicalEdge added : pendingAdd) {
                this.onLogicalGraphEdgeCreated.call(added);
            }
        }
    }

    protected void removeEdge(final GraphLogicalEdge edge) {
        synchronized(this.edges) {
            if(this.edges.remove(edge)) {
                onLogicalGraphEdgeRemoved.call(edge);
            }
        }
    }

    // == Serialization Support ==
    public GraphLogicalVertex vertexForKey(final String key) {
        synchronized(this.vertices) {
            return this.vertices.stream().filter(vertex -> vertex.getKey().equals(key)).findAny().orElse(null);
        }
    }
    public GraphLogicalEdge edgeForKey(final String key) {
        synchronized(this.edges) {
            //Try to find a forward-direction match then fall back to a reverse match.
            return this.edges.stream().filter(edge -> edge.getKey().equals(key)).findAny().orElse(this.edges.stream().filter(edge -> edge.getReverseKey().equals(key)).findAny().orElse(null));
        }
    }

    public Collection<GraphLogicalEdge> getEdgesForEndpoint(final GraphLogicalVertex vertex) {
        synchronized(this.edges) {
            return this.edges.stream().filter(edge -> edge.getSource() == vertex || edge.getDestination() == vertex).collect(Collectors.toList());
        }
    }
}
