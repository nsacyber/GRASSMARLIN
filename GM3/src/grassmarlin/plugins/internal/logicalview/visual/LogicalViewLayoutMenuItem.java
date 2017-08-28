package grassmarlin.plugins.internal.logicalview.visual;

import grassmarlin.plugins.internal.logicalview.Plugin;
import grassmarlin.plugins.internal.logicalview.visual.layouts.FullForceDirectedGraph;
import grassmarlin.ui.common.menu.ActiveMenuItem;
import grassmarlin.ui.common.menu.DynamicSubMenu;
import javafx.scene.Node;
import javafx.scene.control.MenuItem;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class LogicalViewLayoutMenuItem extends DynamicSubMenu implements DynamicSubMenu.IGetMenuItems {
    private final LogicalVisualization visualization;
    private final Predicate<VisualLogicalVertex> fnFilter;
    private final List<ILogicalGraphFullLayout> layouts;

    public LogicalViewLayoutMenuItem(final String title, final Plugin owner, final LogicalVisualization visualization, final Predicate<VisualLogicalVertex> fnFilter) {
        this(title, null, owner, visualization, fnFilter);
    }

    public LogicalViewLayoutMenuItem(final String title, final Node graphic, final Plugin owner, final LogicalVisualization visualization, final Predicate<VisualLogicalVertex> fnFilter) {
        super(title, graphic);

        this.visualization = visualization;
        this.fnFilter = fnFilter;
        this.layouts = new LinkedList<>();

        layouts.add(new FullForceDirectedGraph(owner));
    }

    @Override
    public Collection<MenuItem> getDynamicItems() {
        final List<VisualLogicalVertex> vertices = this.visualization.getVertices().stream().filter(this.fnFilter).collect(Collectors.toList());
        final List<VisualLogicalEdge> edges = new ArrayList<>(this.visualization.getEdges());
        return this.layouts.stream()
                .map(layout -> new ActiveMenuItem(layout.toString(), event -> {
                    LogicalViewLayoutMenuItem.this.visualization.executeLayout(layout, vertices);
                }))
                .collect(Collectors.toList());
    }
}
