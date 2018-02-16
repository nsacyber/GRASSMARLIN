package grassmarlin.plugins.internal.logicalview;

import com.sun.javafx.collections.ObservableListWrapper;
import javafx.beans.property.ReadOnlyListWrapper;
import javafx.collections.ObservableList;

import java.util.LinkedList;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class BlacklistFilteredLogicalGraph extends FilteredLogicalGraph {
    private final ObservableList<GraphLogicalVertex> listHidden;

    public BlacklistFilteredLogicalGraph(final LogicalGraph base) {
        super(base);

        this.listHidden = new ObservableListWrapper<>(new LinkedList<>());

        this.setPredicate(this.predicate);
    }

    private final Predicate<GraphLogicalVertex> predicate = this::testVertex;

    @Override
    protected void handleVertexAdded(GraphLogicalVertex vertex) {
        if(!this.listHidden.contains(vertex)) {
            this.addVertex(vertex);
        }
    }

    @Override
    protected void handleVertexRemoved(GraphLogicalVertex vertex) {
        this.removeVertex(vertex);
    }

    @Override
    protected void handleEdgeAdded(GraphLogicalEdge edge) {
        synchronized(this.vertices) {
            if (this.vertices.contains(edge.getSource()) && this.vertices.contains(edge.getDestination())) {
                this.addEdge(edge);
            }
        }
    }

    @Override
    protected void handleEdgeRemoved(GraphLogicalEdge edge) {
        this.removeEdge(edge);
    }

    private boolean testVertex(final GraphLogicalVertex vertex) {
        return !listHidden.contains(vertex);
    }

    public void hideVertex(final GraphLogicalVertex vertex) {
        synchronized(this.vertices) {
            synchronized(this.edges) {
                this.listHidden.add(vertex);
                this.removeVertex(vertex);
                final List<GraphLogicalEdge> edgesToRemove = this.edges.stream().filter(edge -> edge.getSource() == vertex || edge.getDestination() == vertex).collect(Collectors.toList());
                edgesToRemove.forEach(BlacklistFilteredLogicalGraph.this::removeEdge);
            }
        }
    }

    public void unhideVertex(final GraphLogicalVertex vertex) {
        synchronized(this.vertices) {
            synchronized(this.edges) {
                synchronized(graphBase.edges) {
                    if (this.listHidden.remove(vertex)) {
                        this.addVertex(vertex);
                        this.graphBase.edges.stream().filter(edge ->
                                (edge.getSource() == vertex && !this.listHidden.contains(edge.getDestination()))
                                        ||
                                        (edge.getDestination() == vertex && !this.listHidden.contains(edge.getSource()))
                        ).forEach(BlacklistFilteredLogicalGraph.this::addEdge);
                    }
                }
            }
        }
    }

    public ReadOnlyListWrapper<GraphLogicalVertex> getHiddenVertices() {
        return new ReadOnlyListWrapper<>(this.listHidden);
    }
}
