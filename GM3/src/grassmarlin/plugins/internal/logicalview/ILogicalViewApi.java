package grassmarlin.plugins.internal.logicalview;

import grassmarlin.plugins.internal.logicalview.visual.filters.EdgeStyleRule;
import javafx.scene.Node;

import java.util.function.*;

public interface ILogicalViewApi {
    void addGroupColorFactory(final Supplier<ColorFactoryMenuItem.IColorFactory> factory);
    void addVertexContextMenuItem(final Predicate<GraphLogicalVertex> condition, final String name, final Consumer<GraphLogicalVertex> action);
    <N extends Node, R extends EdgeStyleRule> void addEdgeStyleRuleUi(final String name, final Supplier<N> uiFactory, final Function<N, R> getter, final BiConsumer<N, R> setter);
}
