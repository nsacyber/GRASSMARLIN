package grassmarlin.plugins.internal.logicalview;

import grassmarlin.plugins.internal.logicalview.visual.filters.EdgeStyleRule;
import grassmarlin.plugins.internal.logicalview.visual.filters.Style;
import grassmarlin.session.LogicalVertex;
import grassmarlin.session.Session;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;

import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Supplier;

public interface ILogicalViewApi {
    interface TriConsumer<A, B, C> {
        void accept(A a, B b, C c);

        default TriConsumer<A, B, C> andThen(TriConsumer<? super A, ? super B, ? super C> after) {
            Objects.requireNonNull(after);

            return (a, b, c) -> {
                accept(a, b, c);
                after.accept(a, b, c);
            };
        }
    }

    // == Edge Styles and Rules
    <N extends Node, R extends EdgeStyleRule> void addEdgeStyleRuleUi(final String name, final Supplier<N> uiFactory, final Function<N, R> getter, final BiConsumer<N, R> setter);
    ObservableList<Style> getEdgeStylesForSession(final Session session);
    ObservableList<EdgeStyleRule> getEdgeStyleRulesForSession(final Session session);

    // == Color Factories
    interface ICalculateColorsForAggregate {
        Color getBorderColor(final Object o);
        Color getBackgroundColor(final Object o);
    }

    void addGroupColorFactory(final String name, final Supplier<ICalculateColorsForAggregate> factory);

    // == Assorted Vertex Stuff
    void addVertexContextMenuItem(final BiPredicate<GraphLogicalVertex, LogicalVertex> condition, final String name, final TriConsumer<Session, GraphLogicalVertex, LogicalVertex> action);
    void addMappedImage(final String nameProperty, final String valueProperty, final Image image);
}
