package grassmarlin.plugins.internal.logicalview;

import grassmarlin.session.LogicalVertex;
import grassmarlin.session.serialization.XmlSerializable;
import javafx.scene.control.MenuItem;

import java.util.Collection;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Supplier;

public interface ILogicalVisualization extends XmlSerializable {
    void setCurrentGrouping(final String newValue);
    String getCurrentGrouping();
    void setContextMenuSources(final Supplier<List<MenuItem>> common, final BiFunction<GraphLogicalVertex, LogicalVertex, List<MenuItem>> vertex);

    void zoomToVertex(final GraphLogicalVertex vertex);
    void cleanup();

    LogicalGraph getGraph();

    Collection<? extends IVisualLogicalVertex> getVertices();
    <T extends IVisualLogicalVertex> T vertexFor(final GraphLogicalVertex vertex);
}
