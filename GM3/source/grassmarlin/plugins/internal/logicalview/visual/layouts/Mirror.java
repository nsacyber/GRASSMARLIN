package grassmarlin.plugins.internal.logicalview.visual.layouts;

import grassmarlin.plugins.internal.logicalview.ILogicalVisualization;
import grassmarlin.plugins.internal.logicalview.IVisualLogicalVertex;
import grassmarlin.plugins.internal.logicalview.visual.ILogicalGraphLayout;
import grassmarlin.ui.common.MutablePoint;
import javafx.scene.Node;

import java.util.HashMap;
import java.util.Map;

public class Mirror implements ILogicalGraphLayout {
    private final ILogicalVisualization source;

    public Mirror(final ILogicalVisualization source) {
        this.source = source;
    }

    @Override
    public boolean requiresForcedUpdate() {
        return true;
    }

    @Override
    public <T extends Node & ILogicalVisualization> Map<IVisualLogicalVertex, MutablePoint> executeLayout(T visualization) {
        final Map<IVisualLogicalVertex, MutablePoint> result = new HashMap<>();

        for(final IVisualLogicalVertex vertex : visualization.getVertices()) {
            final IVisualLogicalVertex vertexInSource = source.vertexFor(vertex.getVertex());

            //We don't have to test isSubjectToLayout, but checking here cuts the overhead a tiny amount.
            if(vertexInSource != null && vertex.isSubjectToLayout()) {
                result.put(vertex, new MutablePoint(vertexInSource.getTranslateX(), vertexInSource.getTranslateY()));
            }
        }

        return result;
    }
}
