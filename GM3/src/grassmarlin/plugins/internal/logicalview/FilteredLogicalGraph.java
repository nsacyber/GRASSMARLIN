package grassmarlin.plugins.internal.logicalview;

import com.sun.javafx.collections.ObservableListWrapper;
import grassmarlin.Event;
import grassmarlin.RuntimeConfiguration;
import grassmarlin.plugins.internal.logicalview.visual.DialogPacketList;
import grassmarlin.session.ThreadManagedState;
import grassmarlin.session.pipeline.LogicalAddressMapping;
import grassmarlin.ui.common.menu.ActiveMenuItem;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.ObservableList;
import javafx.scene.control.MenuItem;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class FilteredLogicalGraph {
    private final LogicalGraph graph;

    private class FilteredLogicalGraphThreadManagedState extends ThreadManagedState {
        public FilteredLogicalGraphThreadManagedState(final Event.IAsyncExecutionProvider executor) {
            super(RuntimeConfiguration.UPDATE_INTERVAL_MS, "FilteredLogicalGraph", executor);
        }

        @Override
        public void validate() {
            boolean retestEdges = this.hasFlag(FilteredLogicalGraph.this.edges);
            synchronized(FilteredLogicalGraph.this.vertices) {
                if (this.hasFlag(FilteredLogicalGraph.this.vertices)) {
                    // Retest all vertices in parent against the predicate
                    List<GraphLogicalVertex> verticesValid = FilteredLogicalGraph.this.graph.getVertices();
                    if (FilteredLogicalGraph.this.testVertex != null) {
                        verticesValid = verticesValid.stream().filter(FilteredLogicalGraph.this.testVertex).collect(Collectors.toList());
                    }
                    final List<LogicalAddressMapping> mappingsCurrent = verticesValid.stream().map(vertex -> vertex.getRootLogicalAddressMapping()).distinct().collect(Collectors.toList());
                    FilteredLogicalGraph.this.mappings.retainAll(mappingsCurrent);
                    mappingsCurrent.removeAll(FilteredLogicalGraph.this.mappings);
                    FilteredLogicalGraph.this.mappings.addAll(mappingsCurrent);

                    retestEdges |= FilteredLogicalGraph.this.vertices.retainAll(verticesValid);
                    verticesValid.removeAll(FilteredLogicalGraph.this.vertices);
                    retestEdges |= FilteredLogicalGraph.this.vertices.addAll(verticesValid);
                }
            }
            if(retestEdges) {
                //Find all edges that connect valid vertices.
                List<GraphLogicalEdge> edgesValid = FilteredLogicalGraph.this.graph.getEdges().stream().filter(edge -> vertices.contains(edge.getSource()) && vertices.contains(edge.getDestination())).collect(Collectors.toList());

                FilteredLogicalGraph.this.edges.retainAll(edgesValid);
                edgesValid.removeAll(FilteredLogicalGraph.this.edges);
                FilteredLogicalGraph.this.edges.addAll(edgesValid);
            }
        }
    }

    private Predicate<GraphLogicalVertex> testVertex;

    private final ObservableList<LogicalAddressMapping> mappings;
    private final ObservableList<GraphLogicalVertex> vertices;
    private final ObservableList<GraphLogicalEdge> edges;
    private final SimpleStringProperty grouping;

    private final ThreadManagedState state;

    private final DialogPacketList dialogPacketList;

    public FilteredLogicalGraph(final LogicalGraph graph, final Event.IAsyncExecutionProvider executor) {
        this.graph = graph;
        this.state = new FilteredLogicalGraphThreadManagedState(executor);

        this.mappings = new ObservableListWrapper<>(new ArrayList<>());
        this.vertices = new ObservableListWrapper<>(new ArrayList<>());
        this.edges = new ObservableListWrapper<>(new ArrayList<>());
        this.grouping = new SimpleStringProperty(null);

        this.graph.onLogicalGraphVertexRemoved.addHandler(this.handlerLogicalGraphVertexAction);
        this.graph.onLogicalGraphVertexCreated.addHandler(this.handlerLogicalGraphVertexAction);

        this.graph.onLogicalGraphEdgeRemoved.addHandler(this.handlerLogicalGraphEdgeAction);
        this.graph.onLogicalGraphEdgeCreated.addHandler(this.handlerLogicalGraphEdgeAction);

        this.state.invalidate();
        this.state.waitForValid();

        this.dialogPacketList = new DialogPacketList();
    }

    private Event.EventListener<GraphLogicalVertex> handlerLogicalGraphVertexAction = this::handleLogicalGraphVertexAction;
    private void handleLogicalGraphVertexAction(final Event<GraphLogicalVertex> event, final GraphLogicalVertex vertex) {
        this.state.invalidate(this.vertices);
    }
    private Event.EventListener<GraphLogicalEdge> handlerLogicalGraphEdgeAction = this::handleLogicalGraphEdgeAction;
    private void handleLogicalGraphEdgeAction(final Event<GraphLogicalEdge> event, final GraphLogicalEdge edge) {
        this.state.invalidate(this.edges);
    }

    public void setPredicate(final Predicate<GraphLogicalVertex> predNew) {
        this.state.invalidate(this.vertices, () -> this.testVertex = predNew);
    }
    public void reapplyPredicate() {
        this.state.invalidate(this.vertices);
    }

    public ObservableList<LogicalAddressMapping> getMappings() {
        return this.mappings;
    }
    public ObservableList<GraphLogicalVertex> getVertices() {
        return this.vertices;
    }
    public ObservableList<GraphLogicalEdge> getEdges() {
        return this.edges;
    }
    public StringProperty groupingProperty() {
        return this.grouping;
    }

    public List<GraphLogicalEdge> getEdgesForEndpoint(final GraphLogicalVertex endpoint) {
        return edges.stream().filter(edge -> edge.getSource() == endpoint || edge.getDestination() == endpoint).collect(Collectors.toList());
    }

    public List<String> getGroupings() {
        return vertices.stream().flatMap(vertex -> vertex.getProperties().keySet().stream()).distinct().collect(Collectors.toList());
    }

    public GraphLogicalVertex vertexForKey(final String key) {
        return this.graph.getVertices().stream().filter(vertex -> vertex.getKey().equals(key)).findAny().orElse(null);
    }

    public void waitForValid() {
        this.state.waitForValid();
    }

    public boolean isValid() {
        return this.state.isValid();
    }

    public Collection<MenuItem> menuItemsFor(final GraphLogicalVertex vertex) {
        final List<MenuItem> result = new ArrayList<>(this.graph.getAttachedState().getPlugin().getMenuItemsFor(vertex));
        result.addAll(Arrays.asList(
                new ActiveMenuItem("Show Packet List...", event -> {
                    final DialogPacketList dlg = FilteredLogicalGraph.this.dialogPacketList;
                    dlg.setContent(FilteredLogicalGraph.this, vertex);
                    dlg.showAndWait();
                }),
                new ActiveMenuItem("Create Watch Window", event -> {
                    FilteredLogicalGraph.this.graph.getAttachedState().createWatchView(vertex);
                })
        ));
        return result;
    }

    public LogicalGraph getBackingGraph() {
        return this.graph;
    }
}
