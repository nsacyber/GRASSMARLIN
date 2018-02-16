package grassmarlin.plugins.internal.logicalview;

import javafx.beans.binding.IntegerExpression;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Wraps the logic for generating the predicate to generate a Watch graph
 */
public class WatchFilteredLogicalGraph extends FilteredLogicalGraph {
    private final GraphLogicalVertex vertexRoot;
    private final IntegerExpression degrees;

    private final Map<GraphLogicalVertex, Integer> distanceCache;

    public WatchFilteredLogicalGraph(final LogicalGraph base, final GraphLogicalVertex vertex, final IntegerExpression degrees) {
        super(base);

        this.distanceCache = new HashMap<>();
        this.degrees = degrees;

        this.vertexRoot = vertex;
        this.distanceCache.put(vertexRoot, 0);

        this.degrees.addListener((observable, oldValue, newValue) -> {
            if(newValue.intValue() <= oldValue.intValue()) {
                //We don't need to rebuild the cache since it is a shrink and the old cache is equally valid.
                //We just have to reapply the predicate.
                //TODO: Track the actual degree set in the cache and compare to that.
                this.setPredicate(this::applyFilters);
            } else {
                this.rebuildDistanceCache();
            }
        });

        //Rebuilding the distance cache will also trigger an assignment of the predicate, etc.
        this.rebuildDistanceCache();
    }

    protected void handleVertexAdded(final GraphLogicalVertex vertex) {
        //Adding a vertex doesn't affet the graph (there are, in theory, no edges to it)
        //TODO: it might be possible to derive a watch view from a view that removes the root element, in which case there would be a potential change when adding a vertex
    }
    protected void handleVertexRemoved(final GraphLogicalVertex vertex) {
        //As with adding a vertex, removing one doesn't have an impact, either--it is the removal of edges that matter.
        //TODO: Removing the root would impact a watch display.
    }
    protected void handleEdgeAdded(final GraphLogicalEdge edge) {
        this.rebuildDistanceCache();
    }
    protected void handleEdgeRemoved(final GraphLogicalEdge edge) {
        this.rebuildDistanceCache();
    }


    private void rebuildDistanceCache() {
        this.distanceCache.clear();
        final int maxDistance = this.degrees.get();

        int distance = 0;
        final List<GraphLogicalVertex> pending = new ArrayList<>();
        pending.add(this.vertexRoot);

        // Pull the full list of edges once and remove edges used to process each iteration; reducing overhead for successive generations.
        final LinkedList<GraphLogicalEdge> edges = new LinkedList<>();
        this.graphBase.getEdges(edges);

        while(!pending.isEmpty() && distance <= maxDistance) {
            for(final GraphLogicalVertex vertex : pending) {
                distanceCache.put(vertex, distance);
            }
            //If neither endpoint is in pending, ignore it.
            //If both are in pending, ignore it.
            //If one is in pending, add the other if it is not already in
            final List<GraphLogicalVertex> next = edges.stream()
                    .filter(edge -> pending.contains(edge.getSource()) != pending.contains(edge.getDestination()))
                    .map(edge -> pending.contains(edge.getSource()) ? edge.getDestination() : edge.getSource())
                    .filter(vertex -> !distanceCache.containsKey(vertex))
                    .collect(Collectors.toList());
            edges.removeIf(edge -> pending.contains(edge.getSource()) != pending.contains(edge.getDestination()));
            pending.clear();
            pending.addAll(next);
            distance++;
        }

        this.setPredicate(this::applyFilters);
    }

    private boolean applyFilters(final GraphLogicalVertex vertex) {
        final int degrees = this.degrees.get();
        final Integer distance = distanceCache.get(vertex);
        return (distance != null) && (distance <= degrees);
    }
}
