package grassmarlin.plugins.internal.logicalview.visual.layouts;

import com.sun.istack.internal.NotNull;
import grassmarlin.Event;
import grassmarlin.plugins.internal.logicalview.GraphLogicalVertex;
import grassmarlin.plugins.internal.logicalview.visual.ILogicalGraphFullLayout;
import grassmarlin.plugins.internal.logicalview.visual.LogicalVisualization;
import grassmarlin.plugins.internal.logicalview.visual.VisualLogicalEdge;
import grassmarlin.plugins.internal.logicalview.visual.VisualLogicalVertex;
import javafx.geometry.Point2D;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CopyFromSource implements ILogicalGraphFullLayout {
    private final LogicalVisualization target;
    private final LogicalVisualization source;

    public CopyFromSource(final LogicalVisualization target, final LogicalVisualization source) {
        this.target = target;
        this.source = source;

        //Process all existing elements (generally this is empty)
        source.getVertices().forEach(visualSource -> {
            final VisualLogicalVertex visualTarget = CopyFromSource.this.target.vertexFor(visualSource.getVertex());
            if(visualTarget != null) {
                visualTarget.translateXProperty().bind(visualSource.translateXProperty());
                visualTarget.translateYProperty().bind(visualSource.translateYProperty());
            }
        });

        //Listen for new elements
        target.onVisualLogicalVertexCreated.addHandler(handlerVertexAdded);
        source.onVisualLogicalVertexCreated.addHandler(handlerVertexAdded);
    }

    private final Event.EventListener<LogicalVisualization.VisualVertexEventArgs> handlerVertexAdded = this::handleVertexAdded;
    private void handleVertexAdded(final Event<LogicalVisualization.VisualVertexEventArgs> event, final LogicalVisualization.VisualVertexEventArgs args) {
        establishBindingFor(args.getVertex().getVertex());
    }

    public void establishBindingFor(final GraphLogicalVertex vertex) {
        if(source.getGraph().getVertices().contains(vertex) && target.getGraph().getVertices().contains(vertex)) {
            //If this vertex exists in both graphs, we can bind the properties.
            final VisualLogicalVertex visualSource = source.vertexFor(vertex);
            final VisualLogicalVertex visualTarget = target.vertexFor(vertex);

            //It may not have made its way to the visual tier of both graphs, so we check for that before binding.
            if(visualSource != null && visualTarget != null) {
                visualTarget.translateXProperty().bind(visualSource.translateXProperty());
                visualTarget.translateYProperty().bind(visualSource.translateYProperty());
            }
        }

    }

    @NotNull
    public Map<VisualLogicalVertex, Point2D> layout(final String groupBy, @NotNull final List<VisualLogicalVertex> vertices, @NotNull final List<VisualLogicalEdge> edges) {
        //TODO: Force reset layout
        return new HashMap<>();
    }
}
