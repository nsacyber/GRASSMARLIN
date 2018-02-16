package grassmarlin.plugins.internal.logicalview.visual;

import grassmarlin.plugins.internal.logicalview.GraphLogicalVertex;
import grassmarlin.plugins.internal.logicalview.ICanHasContextMenu;
import grassmarlin.session.LogicalVertex;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.control.TreeItem;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Supplier;

public class LogicalNavigationTreeItem extends TreeItem<Object> {
    public static class GraphLogicalVertexWrapper implements Comparable<GraphLogicalVertexWrapper>, ICanHasContextMenu {
        private final GraphLogicalVertex vertex;
        private final LogicalVertex row;
        private final LogicalNavigation navigation;

        private GraphLogicalVertexWrapper(final GraphLogicalVertex vertex, final LogicalVertex row, final LogicalNavigation navigation) {
            this.vertex = vertex;
            this.row = row;
            this.navigation = navigation;
        }

        public GraphLogicalVertex getVertex() {
            return this.vertex;
        }

        @Override
        public String toString() {
            if(this.row == this.vertex.getVertex()) {
                return this.vertex.getRootLogicalAddressMapping().toString();
            } else {
                return this.row.getLogicalAddress().toString();
            }
        }

        @Override
        public int compareTo(GraphLogicalVertexWrapper o) {
            return this.vertex.getRootLogicalAddressMapping().compareTo(o.vertex.getRootLogicalAddressMapping());
        }

        @Override
        public List<MenuItem> getContextMenuItems() {
            final ArrayList<MenuItem> result = new ArrayList<>();

            final Supplier<List<MenuItem>> commonSource = this.navigation.getCommonContextMenuItemSource();
            if(commonSource != null) {
                final List<MenuItem> commonItems = commonSource.get();
                if(commonItems != null && !commonItems.isEmpty()) {
                    result.add(new SeparatorMenuItem());
                    result.addAll(commonItems);
                }
            }

            final BiFunction<GraphLogicalVertex, LogicalVertex, List<MenuItem>> vertexSource = this.navigation.getVertexContextMenuItemSource();
            if(vertexSource != null) {
                //TODO: The navigation context menu should be able to handle child addresses--we need to construct child address rows.
                final List<MenuItem> vertexItems = vertexSource.apply(this.vertex, this.row);
                if(vertexItems != null && !vertexItems.isEmpty()) {
                    result.add(new SeparatorMenuItem());
                    result.addAll(vertexItems);
                }
            }

            while(!result.isEmpty() && result.get(0) instanceof SeparatorMenuItem) {
                result.remove(0);
            }

            return result;
        }
    }

    private final GraphLogicalVertex root;
    private final LogicalNavigation navigation;
    private final Map<LogicalVertex, TreeItem<Object>> lookupChildren;

    public LogicalNavigationTreeItem(final LogicalNavigation navigation, final GraphLogicalVertex vertex) {
        super(new GraphLogicalVertexWrapper(vertex, vertex.getVertex(), navigation));

        this.root = vertex;
        this.navigation = navigation;
        this.lookupChildren = new HashMap<>();

        vertex.getChildAddresses().addListener(this.handlerChildAddressesChanged);
        this.handleChildAddressesChanged(null);
    }

    private final InvalidationListener handlerChildAddressesChanged = this::handleChildAddressesChanged;
    private void handleChildAddressesChanged(final Observable obs) {
        final Set<LogicalVertex> keysToRemove = new HashSet<>(lookupChildren.keySet());
        keysToRemove.removeAll(this.root.getChildAddresses());
        for(final LogicalVertex vertex : keysToRemove) {
            this.getChildren().remove(this.lookupChildren.remove(vertex));
        }
        this.getChildren().clear();
        for(final LogicalVertex vertex : this.root.getChildAddresses()) {
            this.getChildren().add(lookupChildren.computeIfAbsent(vertex, k -> new TreeItem<>(new GraphLogicalVertexWrapper(LogicalNavigationTreeItem.this.root, k, LogicalNavigationTreeItem.this.navigation))));
        }
    }
}
