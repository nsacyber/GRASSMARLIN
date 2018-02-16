package grassmarlin.plugins.internal.logicalview.visual;

import grassmarlin.Event;
import grassmarlin.plugins.internal.logicalview.GraphLogicalVertex;
import grassmarlin.plugins.internal.logicalview.LogicalGraph;
import grassmarlin.session.LogicalVertex;
import grassmarlin.session.Property;
import grassmarlin.session.PropertyContainer;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TreeItem;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Supplier;

public class LogicalNavigation {
    private final TreeItem<Object> root;
    private final LogicalGraph graph;
    private String grouping;

    /*
    Whenever the groupby changes, rebuild the contents of the root node.
    There is no state information in the navigation view, so we can throw away the elements on change.

    group root
    +-group name
      +-GraphLogicalVertex
     */

    private final HashMap<Object, TreeItem<Object>> mappingGroups;
    private final HashMap<GraphLogicalVertex, List<TreeItem<Object>>> mappingVertices;
    private Supplier<List<MenuItem>> commonContextMenuItemSource;
    private BiFunction<GraphLogicalVertex, LogicalVertex, List<MenuItem>> vertexContextMenuItemSource;

    public LogicalNavigation(final LogicalGraph graph) {
        this.root = new TreeItem<>();
        this.graph = graph;

        this.mappingGroups = new HashMap<>();
        this.mappingVertices = new HashMap<>();

        this.commonContextMenuItemSource = null;
        this.vertexContextMenuItemSource = null;

        this.graph.onLogicalGraphVertexCreated.addHandler(this.handlerVertexAdded);
        this.graph.onLogicalGraphVertexRemoved.addHandler(this.handlerVertexRemoved);

        //Build initial state
        this.setCurrentGrouping(null);
    }

    public void setContextMenuSources(final Supplier<List<MenuItem>> common, final BiFunction<GraphLogicalVertex, LogicalVertex, List<MenuItem>> vertex) {
        this.commonContextMenuItemSource = common;
        this.vertexContextMenuItemSource = vertex;
    }

    public Supplier<List<MenuItem>> getCommonContextMenuItemSource() {
        return this.commonContextMenuItemSource;
    }
    public BiFunction<GraphLogicalVertex, LogicalVertex, List<MenuItem>> getVertexContextMenuItemSource() {
        return this.vertexContextMenuItemSource;
    }

    private final Event.EventListener<PropertyContainer.PropertyEventArgs> handler_propertyChanged = this::handle_propertyChanged;

    protected void processRemoveVertex(final GraphLogicalVertex vertex) {
        final List<TreeItem<Object>> existingItems = mappingVertices.get(vertex);
        for(final TreeItem<Object> item : existingItems) {
            final TreeItem<Object> itemGroup = item.getParent();
            itemGroup.getChildren().remove(item);
            if(itemGroup.getChildren().isEmpty()) {
                final TreeItem<Object> parent = itemGroup.getParent();
                if(parent != null) {
                    parent.getChildren().remove(itemGroup);
                }
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

        final TreeItem<Object> item = new LogicalNavigationTreeItem(this, vertex);
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

    public void setCurrentGrouping(final String value) {
        this.grouping = value;

        this.root.getChildren().clear();
        this.mappingGroups.clear();
        this.mappingVertices.clear();

        final ArrayList<GraphLogicalVertex> vertices = new ArrayList<>();
        graph.getVertices(vertices);
        for(GraphLogicalVertex vertex : vertices) {
            handleVertexAdded(null, vertex);
        }
    }

    private Event.EventListener<GraphLogicalVertex> handlerVertexAdded = this::handleVertexAdded;
    private void handleVertexAdded(final Event<? extends GraphLogicalVertex> event, final GraphLogicalVertex vertex) {
        vertex.onPropertyChanged.addHandler(this.handler_propertyChanged);

        if(this.grouping == null) {
            final TreeItem<Object> itemVertex = new LogicalNavigationTreeItem(this, vertex);
            final List<TreeItem<Object>> itemsForVertex = new ArrayList<>();
            mappingVertices.put(vertex, itemsForVertex);
            itemsForVertex.add(itemVertex);
            root.getChildren().add(itemVertex);
            //When grouping is null, we are sorting a list of LogicalNavigationTreeItem.GraphLogicalVertexWrapper objects, which are Comparable.
            root.getChildren().sort((o1, o2) -> ((Comparable)(o1.getValue())).compareTo(o2.getValue()));
        } else {
            final Set<Property<?>> properties = vertex.getProperties().get(this.grouping);

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
                    final TreeItem<Object> itemVertex = new LogicalNavigationTreeItem(this, vertex);
                    itemsForVertex.add(itemVertex);
                    itemGroup.getChildren().add(itemVertex);
                    itemGroup.getChildren().sort((o1, o2) -> ((Comparable)(o1.getValue())).compareTo(o2.getValue()));
                }
            }
        }
    }
    private Event.EventListener<GraphLogicalVertex> handlerVertexRemoved = this::handleVertexRemoved;
    private void handleVertexRemoved(final Event<? extends GraphLogicalVertex> event, final GraphLogicalVertex vertex) {
        vertex.onPropertyChanged.removeHandler(this.handler_propertyChanged);
        processRemoveVertex(vertex);
    }

    private void handle_propertyChanged(Event<PropertyContainer.PropertyEventArgs> event, PropertyContainer.PropertyEventArgs args) {
        if(args.getName().equals(this.grouping)) {
            if(args.isAdded()) {
                //We only add this handler to LogicalVertices, so this is a safe cast.
                this.addVertexToGroup((GraphLogicalVertex)args.getContainer(), args.getProperty());
            } else {
                this.removeVertexFromGroup((GraphLogicalVertex)args.getContainer(), args.getProperty());
            }
        }

    }

    public TreeItem<Object> getTreeRoot() {
        return this.root;
    }
}
