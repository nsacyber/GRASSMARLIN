package grassmarlin.plugins.internal.logicalview;

import grassmarlin.Event;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public abstract class FilteredLogicalGraph extends LogicalGraph {
    protected final LogicalGraph graphBase;
    private Predicate<GraphLogicalVertex> testVertex;

    public FilteredLogicalGraph(final LogicalGraph graphBase) {
        super(Event.PROVIDER_IN_THREAD);

        this.graphBase = graphBase;

        this.graphBase.onLogicalGraphVertexRemoved.addHandler(this.handlerLogicalGraphVertexAction);
        this.graphBase.onLogicalGraphVertexCreated.addHandler(this.handlerLogicalGraphVertexAction);

        this.graphBase.onLogicalGraphEdgeRemoved.addHandler(this.handlerLogicalGraphEdgeAction);
        this.graphBase.onLogicalGraphEdgeCreated.addHandler(this.handlerLogicalGraphEdgeAction);
    }


    private Event.EventListener<GraphLogicalVertex> handlerLogicalGraphVertexAction = this::handleLogicalGraphVertexAction;
    private void handleLogicalGraphVertexAction(final Event<GraphLogicalVertex> event, final GraphLogicalVertex vertex) {
        if(event == this.graphBase.onLogicalGraphVertexCreated) {
            this.handleVertexAdded(vertex);
        } else {
            this.handleVertexRemoved(vertex);
        }
    }
    private Event.EventListener<GraphLogicalEdge> handlerLogicalGraphEdgeAction = this::handleLogicalGraphEdgeAction;
    private void handleLogicalGraphEdgeAction(final Event<GraphLogicalEdge> event, final GraphLogicalEdge edge) {
        if(event == this.graphBase.onLogicalGraphEdgeCreated) {
            this.handleEdgeAdded(edge);
        } else {
            this.handleEdgeRemoved(edge);
        }
    }

    protected abstract void handleVertexAdded(final GraphLogicalVertex vertex);
    protected abstract void handleVertexRemoved(final GraphLogicalVertex vertex);
    protected abstract void handleEdgeAdded(final GraphLogicalEdge edge);
    protected abstract void handleEdgeRemoved(final GraphLogicalEdge edge);

    protected void reevaluatePredicate(final List<GraphLogicalVertex> verticesInBaseGraph, final List<GraphLogicalEdge> edgesInBaseGraph) {
        //Whenever anything changes, we retest everything.
        // There might be a better way, but I can't find a better way that doesn't run afoul of at least one edge case.

        //Synchronization may not be necessary but it does prevent concurrent edits.
        synchronized(this.vertices) {
            synchronized(this.edges) {
                final List<GraphLogicalVertex> verticesValid;
                if (FilteredLogicalGraph.this.testVertex != null) {
                    verticesValid = verticesInBaseGraph.stream()
                            .filter(this.testVertex)
                            .collect(Collectors.toList());
                } else {
                    verticesValid = verticesInBaseGraph;
                }

                this.synchronizeVertices(verticesValid);

                //Find all edges that connect valid vertices.
                final List<GraphLogicalEdge> edgesValid = edgesInBaseGraph.stream()
                        .filter(edge ->
                                FilteredLogicalGraph.this.vertices.contains(edge.getSource()) &&
                                FilteredLogicalGraph.this.vertices.contains(edge.getDestination())
                        ).collect(Collectors.toList());

                this.synchronizeEdges(edgesValid);
            }
        }
    }

    protected void setPredicate(final Predicate<GraphLogicalVertex> predNew) {
        this.testVertex = predNew;

        final List<GraphLogicalVertex> verticesBase = new ArrayList<>();
        final List<GraphLogicalEdge> edgesBase = new ArrayList<>();
        this.graphBase.getAtomic(verticesBase, edgesBase);
        this.reevaluatePredicate(verticesBase, edgesBase);
    }
}
