package grassmarlin.plugins.internal.logicalview.visual;

import grassmarlin.Event;
import grassmarlin.RuntimeConfiguration;
import grassmarlin.plugins.internal.logicalview.FilteredLogicalGraph;
import grassmarlin.plugins.internal.logicalview.GraphLogicalVertex;
import grassmarlin.plugins.internal.logicalview.ICanHasContextMenu;
import grassmarlin.plugins.internal.logicalview.Plugin;
import grassmarlin.session.Property;
import grassmarlin.session.PropertyContainer;
import grassmarlin.ui.common.menu.ActiveMenuItem;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TreeItem;

import java.util.*;

public class LogicalNavigation {
    private class VertexWrapper  implements ICanHasContextMenu, Comparable<VertexWrapper> {
        private final GraphLogicalVertex vertex;

        public VertexWrapper(final GraphLogicalVertex vertex) {
            this.vertex = vertex;
        }

        @Override
        public List<MenuItem> getContextMenuItems() {
            final List<MenuItem> result = new ArrayList<>();
            result.addAll(LogicalNavigation.this.graph.menuItemsFor(this.vertex));
            result.addAll(this.vertex.getContextMenuItems());
            result.addAll(Arrays.asList(
                    new ActiveMenuItem("Find in View", event -> {
                        Plugin.LogicalGraphVisualizationWrapper wrapper = LogicalNavigation.this.graph.getBackingGraph().getAttachedState().getVisualizationFor(LogicalNavigation.this.graph);
                        if(wrapper != null) {
                            wrapper.getVisualization().zoomToVertex(vertex);
                        } else {
                            LogicalNavigation.this.graph.getBackingGraph().getAttachedState().getPrimaryVisualization().getVisualization().zoomToVertex(vertex);
                        }
                    })
            ));

            return result;
        }

        @Override
        public int compareTo(final VertexWrapper other) {
            return this.vertex.getRootLogicalAddressMapping().compareTo(other.vertex.getRootLogicalAddressMapping());
        }

        @Override
        public String toString() {
            return this.vertex.toString();
        }
    }

    private final TreeItem<Object> root;
    private final FilteredLogicalGraph graph;

    /*
    Whenever the groupby changes, rebuild the contents of the root node.
    There is no state information in the navigation view, so we can throw away the elements on change.

    group root
    +-group name
      +-GraphLogicalVertex
     */

    private final HashMap<Object, TreeItem<Object>> mappingGroups;
    private final HashMap<GraphLogicalVertex, List<TreeItem<Object>>> mappingVertices;

    public LogicalNavigation(final RuntimeConfiguration config, final FilteredLogicalGraph graph) {
        this.root = new TreeItem<>();
        this.graph = graph;

        this.mappingGroups = new HashMap<>();
        this.mappingVertices = new HashMap<>();

        graph.groupingProperty().addListener(this.handler_groupingChanged);
        graph.getVertices().addListener(this.handler_verticesChanged);

        //Build initial state
        handle_groupingChanged(null, null, graph.groupingProperty().get());
    }

    private final ChangeListener<String> handler_groupingChanged = this::handle_groupingChanged;
    private final ListChangeListener<GraphLogicalVertex> handler_verticesChanged = this::handle_verticesChanged;
    private final Event.EventListener<PropertyContainer.PropertyEventArgs> handler_propertyChanged = this::handle_propertyChanged;

    protected void processNewVertex(final GraphLogicalVertex vertex, final String grouping) {
        if(grouping == null) {
            final TreeItem<Object> itemVertex = new TreeItem<>(new VertexWrapper(vertex));
            final List<TreeItem<Object>> itemsForVertex = new ArrayList<>();
            mappingVertices.put(vertex, itemsForVertex);
            itemsForVertex.add(itemVertex);
            root.getChildren().add(itemVertex);
            //When grouping is null, we are sorting a list of VertexWrapper objects, which are Comparable.
            root.getChildren().sort((o1, o2) -> ((Comparable)o1.getValue()).compareTo(o2.getValue()));
        } else {
            final Set<Property<?>> properties = vertex.getProperties().get(grouping);

            final List<TreeItem<Object>> itemsForVertex = new ArrayList<>();
            mappingVertices.put(vertex, itemsForVertex);
            if (properties != null && !properties.isEmpty()) {
                for (Property<?> property : properties) {
                    //Find the group-level entry, building if it doesn't exist already
                    TreeItem<Object> itemGroup = mappingGroups.get(property);
                    if (itemGroup == null) {
                        itemGroup = new TreeItem<>(property);
                        this.root.getChildren().add(itemGroup);
                        //When grouping is non-null, we are sorting a list of property values, which are not necessarily comparable, so we sort by the toString of the value.
                        this.root.getChildren().sort((o1, o2) -> ((Property<?>)o1.getValue()).compareTo((Property<?>)o2.getValue()));
                        mappingGroups.put(property, itemGroup);
                    }

                    //We always have to add the vertex-level entry
                    final TreeItem<Object> itemVertex = new TreeItem<>(new VertexWrapper(vertex));
                    itemsForVertex.add(itemVertex);
                    itemGroup.getChildren().add(itemVertex);
                    itemGroup.getChildren().sort((o1, o2) -> ((Comparable)o1.getValue()).compareTo(o2.getValue()));
                }
            }
        }
    }
    protected void processRemoveVertex(final GraphLogicalVertex vertex) {
        final List<TreeItem<Object>> existingItems = mappingVertices.get(vertex);
        for(final TreeItem<Object> item : existingItems) {
            final TreeItem<Object> itemGroup = item.getParent();
            itemGroup.getChildren().remove(item);
            if(itemGroup.getChildren().isEmpty()) {
                itemGroup.getParent().getChildren().remove(itemGroup);
                mappingGroups.remove(itemGroup);
            }
        }
        mappingVertices.remove(vertex);
    }
    protected void addVertexToGroup(final GraphLogicalVertex vertex, final Property<?> group) {
        //This only happens in response to the properties changing in the current grouping, so it only happens for non-null groups.
        TreeItem<Object> itemGroup = mappingGroups.get(group);
        if(itemGroup == null) {
            itemGroup = new TreeItem<>(group);
            this.root.getChildren().add(itemGroup);
            this.root.getChildren().sort((o1, o2) -> ((Property<?>)o1.getValue()).compareTo((Property<?>)o2.getValue()));
            mappingGroups.put(group, itemGroup);
        }

        final TreeItem<Object> item = new TreeItem<>(new VertexWrapper(vertex));
        itemGroup.getChildren().add(item);
        itemGroup.getChildren().sort((o1, o2) -> ((Comparable)o1.getValue()).compareTo(o2.getValue()));

        List<TreeItem<Object>> itemsVertex = mappingVertices.get(vertex);
        if(itemsVertex == null) {
            itemsVertex = new ArrayList<>();
            mappingVertices.put(vertex, itemsVertex);
        }
        itemsVertex.add(item);
    }
    protected void removeVertexFromGroup(final GraphLogicalVertex vertex, final Property<?> group) {
        final TreeItem<Object> itemGroup = mappingGroups.get(group);
        final List<TreeItem<Object>> itemsForVertex = mappingVertices.get(vertex);

        // Find the overlapping item and remove it--there should only be one.
        final TreeItem<Object> itemVertex = itemGroup.getChildren().stream().filter(itemsForVertex::contains).findFirst().orElseGet(() -> null);
        if(itemVertex == null) {
            //No item shouldn't happen but, if it does, then remove nothing and move on.
            return;
        }
        itemsForVertex.remove(itemVertex);
        itemGroup.getChildren().remove(itemVertex);

        // If the item has no more memberships, remove it.
        if(itemsForVertex.isEmpty()) {
            mappingVertices.remove(vertex);
        }

        // If the group has no more members, remove it.
        if(itemGroup.getChildren().isEmpty()) {
            mappingGroups.remove(group);
            this.root.getChildren().remove(itemGroup);
        }
    }

    private void handle_groupingChanged(final ObservableValue<? extends String> observable, final String oldValue, final String newValue) {
        //This fires in the Fx thread, since grouping is a user control.
        this.root.getChildren().clear();
        this.mappingGroups.clear();
        this.mappingVertices.clear();

        for(GraphLogicalVertex vertex : graph.getVertices()) {
            processNewVertex(vertex, newValue);
        }
    }
    private void handle_verticesChanged(ListChangeListener.Change<? extends GraphLogicalVertex> change) {
        //Vertices is presently changed in worker threads, so we need to run the events in the Fx thread.
        Platform.runLater(() -> {
            change.reset();
            while(change.next()) {
                for(final GraphLogicalVertex vertex : change.getRemoved()) {
                    removeVertex(vertex);
                }
                for(final GraphLogicalVertex vertex : change.getAddedSubList()) {
                    addVertex(vertex);
                }
            }
        });
    }
    private void handle_propertyChanged(Event<PropertyContainer.PropertyEventArgs> event, PropertyContainer.PropertyEventArgs args) {
        if(args.getName().equals(this.graph.groupingProperty().get())) {
            if(args.isAdded()) {
                //We only add this handler to LogicalVertices, so this is a safe cast.
                this.addVertexToGroup((GraphLogicalVertex)args.getContainer(), args.getProperty());
            } else {
                this.removeVertexFromGroup((GraphLogicalVertex)args.getContainer(), args.getProperty());
            }
        }

    }

    protected void addVertex(final GraphLogicalVertex vertex) {
        vertex.onPropertyChanged.addHandler(this.handler_propertyChanged);
        processNewVertex(vertex, graph.groupingProperty().get());
    }

    protected void removeVertex(final GraphLogicalVertex vertex) {
        vertex.onPropertyChanged.removeHandler(this.handler_propertyChanged);
        processRemoveVertex(vertex);
    }

    public TreeItem<Object> getTreeRoot() {
        return this.root;
    }
}
