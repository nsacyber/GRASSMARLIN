package grassmarlin.plugins.internal.logicalview.visual;

import grassmarlin.Event;
import grassmarlin.Logger;
import grassmarlin.RuntimeConfiguration;
import grassmarlin.Version;
import grassmarlin.common.svg.Svg;
import grassmarlin.plugins.internal.logicalview.*;
import grassmarlin.plugins.internal.logicalview.visual.filters.BoundEdgeProperties;
import grassmarlin.session.Property;
import grassmarlin.session.PropertyContainer;
import grassmarlin.session.serialization.Loader;
import grassmarlin.session.serialization.Writer;
import grassmarlin.session.serialization.XmlSerializable;
import grassmarlin.ui.common.ImageDirectoryWatcher;
import grassmarlin.ui.common.ZoomableScrollPane;
import grassmarlin.ui.common.menu.ActiveMenuItem;
import grassmarlin.ui.common.tasks.SynchronousPlatform;
import javafx.application.Platform;
import javafx.beans.binding.BooleanExpression;
import javafx.collections.ListChangeListener;
import javafx.geometry.Point2D;
import javafx.scene.Parent;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.image.Image;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.stream.events.XMLEvent;
import java.io.File;
import java.nio.file.Paths;
import java.util.*;

public class LogicalVisualization extends StackPane implements XmlSerializable {
    protected static final String LAYER_AGGREGATES = "Groups";
    protected static final String LAYER_EDGES = "Edges";
    protected static final String LAYER_NODES = "Nodes";

    public interface ICalculateColorsForAggregate {
        Color getBorderColor(final Object o);
        Color getBackgroundColor(final Object o);
    }

    public class VisualVertexEventArgs {
        private final VisualLogicalVertex vertex;

        public VisualVertexEventArgs(final VisualLogicalVertex vertex) {
            this.vertex = vertex;
        }

        public VisualLogicalVertex getVertex() {
            return this.vertex;
        }
        public LogicalVisualization getVisualization() {
            return LogicalVisualization.this;
        }
    }

    protected final Plugin plugin;
    protected final RuntimeConfiguration config;
    private final FilteredLogicalGraph graph;
    private final ZoomableScrollPane zsp;
    private final Map<GraphLogicalVertex, VisualLogicalVertex> vertices;
    private final Map<GraphLogicalEdge, VisualLogicalEdge> edges;
    private final Map<String, Map<Object, VisualLogicalAggregate>> aggregates;

    private final ImageDirectoryWatcher<Image> imageMapper;
    private final ICalculateColorsForAggregate colorFactory;

    private final MenuItem miNewFilteredView;
    private final SetGroupingMenuItem miSetGrouping;
    private final LogicalViewLayoutMenuItem miLayoutAll;
    private final MenuItem miExportSvg;

    private final Plugin.LogicalGraphState state;

    protected final ArrayList<MenuItem> visualizationMenuItems;

    public Event<VisualVertexEventArgs> onVisualLogicalVertexCreated;

    public LogicalVisualization(final Plugin plugin, final RuntimeConfiguration config, final FilteredLogicalGraph graph, final Plugin.LogicalGraphState state) {
        this.plugin = plugin;
        this.config = config;
        graph.waitForValid();
        this.graph = graph;
        this.zsp = new ZoomableScrollPane(this::handle_ConstructContextMenu, this::hideContextMenu, LAYER_AGGREGATES, LAYER_EDGES, LAYER_NODES);
        this.vertices = new HashMap<>();
        this.edges = new HashMap<>();
        this.aggregates = new HashMap<>();

        this.imageMapper = state.getImageMapper();
        this.colorFactory = plugin.getAggregateColorFactory();
        this.state = state;

        this.onVisualLogicalVertexCreated = new Event<>(config.getUiEventProvider());

        this.visualizationMenuItems = new ArrayList<>();
        this.miNewFilteredView = new ActiveMenuItem("New Filtered View", event -> {
            LogicalVisualization.this.state.createFilteredView();
        });
        this.miSetGrouping = new SetGroupingMenuItem(graph);
        this.miLayoutAll = new LogicalViewLayoutMenuItem("Layout All", plugin, this, vertex -> true);
        this.miExportSvg = new ActiveMenuItem("Export to SVG...", event -> {
            final FileChooser dlgExportAs = new FileChooser();
            dlgExportAs.setTitle("Export To...");
            dlgExportAs.getExtensionFilters().addAll(
                    new FileChooser.ExtensionFilter("SVG Files", "*.svg"),
                    new FileChooser.ExtensionFilter("All Files", "*")
            );
            final File selected = dlgExportAs.showSaveDialog(LogicalVisualization.this.getScene().getWindow());
            if(selected != null) {
                //HACK: Skip the ZSP itself, since that is the camera; the first (only) child is the scene graph at world coordinate scale.
                Svg.serialize((Parent) LogicalVisualization.this.zsp.getChildrenUnmodifiable().get(0), Paths.get(selected.getAbsolutePath()));
            }
        });
        this.visualizationMenuItems.addAll(Arrays.asList(miNewFilteredView, miSetGrouping, miExportSvg, miLayoutAll));

        initComponents();

        graph.getVertices().addListener(this::handle_verticesChanged);
        graph.getEdges().addListener(this::handle_edgesChanged);

        for (final GraphLogicalVertex vertexAdded : graph.getVertices()) {
            LogicalVisualization.this.addVertex(vertexAdded);
        }
        for(final GraphLogicalEdge edgeAdded : graph.getEdges()) {
            LogicalVisualization.this.addEdge(edgeAdded);
        }
    }

    private void initComponents() {
        this.zsp.prefWidthProperty().bind(this.widthProperty());
        this.zsp.prefHeightProperty().bind(this.heightProperty());

        this.graph.groupingProperty().addListener((observable, oldValue, newValue) -> {
            if(oldValue != null) {
                LogicalVisualization.this.removeExistingGroupings(oldValue);
            }
            if(newValue != null) {
                LogicalVisualization.this.setCurrentGrouping(newValue);
            }
        });

        if(Version.IS_BETA) {
            final Text textBetaDisclaimer = new Text("\u03B2");
            textBetaDisclaimer.setFont(new Font("Lucida Console", 96.0));
            textBetaDisclaimer.setStroke(Color.AQUAMARINE);
            textBetaDisclaimer.setOpacity(0.6);

            this.getChildren().add(textBetaDisclaimer);
        }

        this.getChildren().add(this.zsp);
    }

    public void markAsModified() {
        this.state.getSession().markAsModified();
    }

    private void handle_verticesChanged(final ListChangeListener.Change<? extends GraphLogicalVertex> change) {
        //Since the change object can be handed to multiple handlers, we have to reset before processing.
        change.reset();

        final ArrayList<GraphLogicalVertex> added = new ArrayList<>();
        final List<GraphLogicalVertex> removed = new ArrayList<>();

        //We don't care about ordering, we just need to see what gets added and what gets removed.
        while(change.next()) {
            //The removed list is immutable, but added is not.  Cache both lists now to prevent a race condition in the added list and to simplify the handling of the change object across multiple handlers.
            added.addAll(change.getAddedSubList());
            removed.addAll(change.getRemoved());
        }

        added.removeAll(removed);

        Platform.runLater(() -> {
            for (final GraphLogicalVertex vertexDeleted : removed) {
                LogicalVisualization.this.removeVertex(vertexDeleted);
            }
            for (final GraphLogicalVertex vertexAdded : added) {
                LogicalVisualization.this.addVertex(vertexAdded);
            }
        });
    }
    private void handle_edgesChanged(final ListChangeListener.Change<? extends GraphLogicalEdge> change) {
        change.reset();

        while(change.next()) {
            final ArrayList<GraphLogicalEdge> added = new ArrayList<>(change.getAddedSubList());
            final List<? extends GraphLogicalEdge> removed = change.getRemoved();

            Platform.runLater(() -> {
                //We don't care about ordering, we just need to see what gets added and what gets removed.
                for(final GraphLogicalEdge edgeDeleted : removed) {
                    LogicalVisualization.this.removeEdge(edgeDeleted);
                }
                for(final GraphLogicalEdge edgeAdded : added) {
                    LogicalVisualization.this.addEdge(edgeAdded);
                }
            });
        }
    }

    public Collection<VisualLogicalVertex> getVertices() {
        return this.vertices.values();
    }
    public Collection<VisualLogicalEdge> getEdges() {
        return this.edges.values();
    }

    /**
     * Factory method to produce VisualLogicalVertices; to be overridden in subclasses to change the visual element class.
     */
    protected VisualLogicalVertex createVisualLogicalVertexFor(final GraphLogicalVertex vertex, final ImageDirectoryWatcher.MappedImageList images) {
        return new VisualLogicalVertex(this, vertex, images);
    }

    protected void addVertex(final GraphLogicalVertex vertex) {
        final VisualLogicalVertex visual = createVisualLogicalVertexFor(vertex, this.imageMapper.startWatching(vertex));

        vertices.put(vertex, visual);
        this.zsp.addChild(visual, LAYER_NODES);

        this.onVisualLogicalVertexCreated.call(new VisualVertexEventArgs(visual));

        //TODO: Check to see if visual is supposed to belong to any groups, and add it to the visual aggregates.
        final String nameCurrentGrouping = this.graph.groupingProperty().get();
        final Set<Property<?>> propertiesForCurrentGrouping = vertex.getProperties().get(nameCurrentGrouping);
        if(propertiesForCurrentGrouping != null && !propertiesForCurrentGrouping.isEmpty()) {
            Map<Object, VisualLogicalAggregate> tempAggregatesForGrouping = aggregates.get(nameCurrentGrouping);
            if(tempAggregatesForGrouping == null) {
                tempAggregatesForGrouping = new HashMap<>();
                aggregates.put(nameCurrentGrouping, tempAggregatesForGrouping);
            }
            final Map<Object, VisualLogicalAggregate> aggregatesForGrouping = tempAggregatesForGrouping;

            propertiesForCurrentGrouping.stream().map(property -> property.getValue()).forEach(group -> {

                VisualLogicalAggregate aggregate = aggregatesForGrouping.get(group);
                if(aggregate == null) {
                    aggregate = new VisualLogicalAggregate(this, this.graph, nameCurrentGrouping, group);
                    aggregatesForGrouping.put(group, aggregate);
                    LogicalVisualization.this.zsp.addLayeredChild(aggregate);
                }

                aggregate.addMember(visual);
            });
        }

        vertex.onPropertyChanged.addHandler(this.handlerPropertyChanged);
    }

    private final Event.EventListener<PropertyContainer.PropertyEventArgs> handlerPropertyChanged = this::handlePropertyChanged;
    protected void handlePropertyChanged(final Event<PropertyContainer.PropertyEventArgs> event, final PropertyContainer.PropertyEventArgs args) {
        if(!(args.getContainer() instanceof GraphLogicalVertex)) {
            return;
        }
        final GraphLogicalVertex vertex = (GraphLogicalVertex)args.getContainer();

        //If the property that is changed isn't part of the current grouping, then we can ignore it, since changing the grouping will rebuild the group memberships.
        if(args.getName().equals(this.graph.groupingProperty().get())) {
            Map<Object, VisualLogicalAggregate> aggregatesForGrouping = aggregates.get(args.getName());
            if(aggregatesForGrouping == null) {
                aggregatesForGrouping = new HashMap<>();
                aggregates.put(args.getName(), aggregatesForGrouping);
            }

            VisualLogicalAggregate aggregate = aggregatesForGrouping.get(args.getValue());
            if(aggregate == null) {
                aggregate = new VisualLogicalAggregate(this, this.graph, args.getName(), args.getValue());
                aggregatesForGrouping.put(args.getValue(), aggregate);
                LogicalVisualization.this.zsp.addLayeredChild(aggregate);
            }

            if(args.isAdded()) {
                aggregate.addMember(vertexFor(vertex));
            } else {
                aggregate.removeMember(vertexFor(vertex));
            }
        }
    }

    protected void removeVertex(final GraphLogicalVertex vertex) {
        final VisualLogicalVertex visual = vertices.get(vertex);
        this.imageMapper.stopWatching(vertex, visual.getImageList());
        if(visual != null) {
            vertices.remove(vertex);
            this.zsp.removeChild(visual, LAYER_NODES);
            vertex.onPropertyChanged.removeHandler(this.handlerPropertyChanged);
            //Remove from all aggregates; this is overkill (we only need to iterate over the current set) but this is easier and, until there is a performance reason to change this, a completely adequate solution.
            this.aggregates.values().stream().flatMap(entry -> entry.values().stream()).forEach(aggregate -> aggregate.removeMember(visual));
        }
    }

    protected void addEdge(final GraphLogicalEdge edge) {
        final VisualLogicalEdge visual = new VisualLogicalEdge(this, edge, vertices.get(edge.getSource()), vertices.get(edge.getDestination()));
        visual.setEdgeProperties(new BoundEdgeProperties(visual, plugin.getEdgeRules()));
        edges.put(edge, visual);
        this.zsp.addChild(visual, LAYER_EDGES);
    }

    protected void removeEdge(final GraphLogicalEdge edge) {
        final VisualLogicalEdge visual = edges.get(edge);
        if(visual != null) {
            edges.remove(edge);
            this.zsp.removeChild(visual, LAYER_EDGES);
        }
    }

    protected VisualLogicalVertex vertexFor(final String key) {
        return this.vertexFor(graph.vertexForKey(key));
    }
    public VisualLogicalVertex vertexFor(final GraphLogicalVertex vertex) {
        return vertices.get(vertex);
    }
    public VisualLogicalEdge edgeFor(final GraphLogicalEdge edge) {
        return edges.get(edge);
    }

    private final ContextMenu menu = new ContextMenu();

    private void handle_ConstructContextMenu(final List<Object> objects, final Point2D screenLocation) {
        menu.hide();
        menu.getItems().clear();
        menu.getItems().addAll(this.visualizationMenuItems);

        for(final Object object : objects) {
            if(object instanceof ICanHasContextMenu) {
                final List<MenuItem> items = ((ICanHasContextMenu)object).getContextMenuItems();
                if(items != null && !items.isEmpty()) {
                    if(!menu.getItems().isEmpty()) {
                        menu.getItems().add(new SeparatorMenuItem());
                    }
                    menu.getItems().addAll(items);
                }
            }
        }
        if(!menu.getItems().isEmpty()) {
            menu.show(this, screenLocation.getX(), screenLocation.getY());
        }
    }

    private void hideContextMenu(Point2D location) {
        menu.hide();
    }

    public FilteredLogicalGraph getGraph() {
        return this.graph;
    }

    // Grouping and Aggregates
    public void removeExistingGroupings(final String nameGroupProperty) {
        if(aggregates.containsKey(nameGroupProperty)) {
            this.aggregates.get(nameGroupProperty).values().forEach(aggregate -> {
                LogicalVisualization.this.zsp.removeLayeredChild(aggregate);
                aggregate.clearMembers();
            });
        }
    }
    public void setCurrentGrouping(final String nameGroupProperty) {
        if(!aggregates.containsKey(nameGroupProperty)) {
            aggregates.put(nameGroupProperty, new HashMap<>());
        }
        this.aggregates.get(nameGroupProperty).values().forEach(VisualLogicalAggregate::clearMembers);

        for(final VisualLogicalVertex vertex : this.vertices.values()) {
            final Set<Property<?>> values = vertex.getVertex().getProperties().get(nameGroupProperty);
            if(values != null && !values.isEmpty()) {
                for(final Property<?> propertyGroup : values) {
                    final Object objGroup = propertyGroup.getValue();
                    if(objGroup == null) {
                        continue;
                    }

                    VisualLogicalAggregate aggregate = aggregates.get(nameGroupProperty).get(objGroup);
                    if(aggregate == null) {
                        aggregate = new VisualLogicalAggregate(this, this.graph, nameGroupProperty, objGroup);
                        aggregates.get(nameGroupProperty).put(objGroup, aggregate);
                    }

                    aggregate.addMember(vertex);
                    zsp.addLayeredChild(aggregate);
                }
            }
        }
    }

    // Serialization
    @Override
    public void readFromXml(final XMLStreamReader source) throws XMLStreamException {
        final String terminalTag = source.getLocalName();
        String nameGroup = null;
        while(source.hasNext()) {
            final int typeNext = source.nextTag();
            final String tag;
            switch(typeNext) {
                case XMLEvent.START_ELEMENT:
                    tag = source.getLocalName();
                    if(tag.equals("Vertex")) {
                        final String key = source.getAttributeValue(null, "key");
                        final VisualLogicalVertex vertex = this.vertexFor(key);
                        if(vertex != null) {
                            vertex.readFromXml(source);
                        } else {
                            Logger.log(Logger.Severity.WARNING, "Unable to identify vertex to restore: [%s]", key);
                        }
                    } else if(tag.equals("Edge")) {
                        //There are no edge options, so we have nothing to do here.
                    } else if(tag.equals("Grouping")) {
                        nameGroup = source.getAttributeValue(null, "Group");
                    } else if(tag.equals("VisualElement")) { // Aggregates
                        if(nameGroup == null) {
                            Logger.log(Logger.Severity.WARNING, "Attempting to restore grouping contents without an identified grouping");
                        } else {
                            final Object obj = Loader.readNextObject(source, Object.class);
                            //This should move to the closing For tag
                            source.nextTag();
                            //this should move the cursor to the start of the Options element
                            source.nextTag();
                            final String group = nameGroup;
                            SynchronousPlatform.runNow(() -> {
                                //TODO: Loading groups needs to be fixed
                                //this.createGrouping(group);
                            });
                            this.aggregates.get(nameGroup).get(obj).readFromXml(source);
                        }
                    } else if(tag.equals("Viewport")) {
                        this.zsp.readFromXml(source);
                    }
                    break;
                case XMLEvent.END_ELEMENT:
                    tag = source.getLocalName();
                    if(tag.equals("Grouping")) {
                        nameGroup = null;
                    } else if(tag.equals(terminalTag)) {
                        return;
                    }
                    break;
            }
        }
    }
    @Override
    public void writeToXml(final XMLStreamWriter writer) throws XMLStreamException {
        writer.writeStartElement("Viewport");
        this.zsp.writeToXml(writer);    //This writes the end tag for the Viewport element

        writer.writeStartElement("Vertices");
        for(final VisualLogicalVertex vertex : vertices.values()) {
            writer.writeStartElement("Vertex");
            Writer.writeObject(writer, vertex);
            writer.writeEndElement();
        }
        writer.writeEndElement();

        //The groupings don't need to be serialized since it is just an association between objects without any real state.
        writer.writeStartElement("Aggregates");
        for(final Map.Entry<String, Map<Object, VisualLogicalAggregate>> groupingEntry : this.aggregates.entrySet()) {
            writer.writeStartElement("Grouping");
            writer.writeAttribute("Group", groupingEntry.getKey());
            for(Map.Entry<Object, VisualLogicalAggregate> entry : groupingEntry.getValue().entrySet()) {
                //TODO: Finish saving visual aggregates
                writer.writeStartElement("VisualElement");
                writer.writeStartElement("For");
                Writer.writeObject(writer, entry.getKey());
                writer.writeEndElement();
                writer.writeStartElement("Options");
                entry.getValue().writeToXml(writer);
                writer.writeEndElement();
                writer.writeEndElement();
            }
            writer.writeEndElement();
        }
        writer.writeEndElement();
    }

    public BooleanExpression useWeightedEdgesBinding() {
        return this.plugin.useWeightedEdgesBinding();
    }

    private boolean layoutPending = false;
    public void executeLayout(final ILogicalGraphFullLayout layout, final List<VisualLogicalVertex> vertices) {
        //HACK: The uiProcessor should be used, rather than Platform.runLater...  then again, this is a JavaFX class.
        if(layoutPending) {
            Logger.log(Logger.Severity.WARNING, "A layout operation is currently executing; a new layout cannot begin until the existing layout completes.");
        } else {
            layoutPending = true;
            final Thread thread = new Thread(() -> {
                try {
                    final Map<VisualLogicalVertex, Point2D> result = layout.layout(graph.groupingProperty().get(), vertices, new ArrayList<>(this.edges.values()));
                    if (result != null) {
                        Platform.runLater(() -> {
                            for (Map.Entry<VisualLogicalVertex, Point2D> entry : result.entrySet()) {
                                entry.getKey().setTranslateX(entry.getValue().getX());
                                entry.getKey().setTranslateY(entry.getValue().getY());
                            }
                        });
                    }
                } catch(Exception ex) {
                     ex.printStackTrace();
                } finally {
                    layoutPending = false;
                }
            });
            thread.setDaemon(true);
            thread.start();
        }
    }

    public ImageDirectoryWatcher<Image> getImageMapper() {
        return this.imageMapper;
    }

    public Plugin getPlugin() {
        return this.plugin;
    }
    Plugin.LogicalGraphState getSessionState() {
        return this.state;
    }

    public ICalculateColorsForAggregate getColorFactory() {
        return this.colorFactory;
    }

    ZoomableScrollPane getZsp() {
        return this.zsp;
    }

    public void zoomToVertex(final GraphLogicalVertex vertex) {
        this.zsp.zoomToVertex(vertices.get(vertex));
    }
}
