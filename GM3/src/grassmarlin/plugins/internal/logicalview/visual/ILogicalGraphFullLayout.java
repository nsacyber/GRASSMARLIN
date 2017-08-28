package grassmarlin.plugins.internal.logicalview.visual;

import com.sun.istack.internal.NotNull;
import javafx.geometry.Point2D;

import java.util.List;
import java.util.Map;

public interface ILogicalGraphFullLayout {
    @NotNull
    Map<VisualLogicalVertex, Point2D> layout(final String groupBy, @NotNull final List<VisualLogicalVertex> vertices, @NotNull final List<VisualLogicalEdge> edges);
}
