package grassmarlin.plugins.internal.logicalview.visual;

import grassmarlin.Event;
import grassmarlin.Logger;
import grassmarlin.Version;
import grassmarlin.common.svg.Svg;
import grassmarlin.plugins.internal.logicalview.*;
import grassmarlin.session.LogicalVertex;
import grassmarlin.session.serialization.XmlSerializable;
import grassmarlin.ui.common.ImageDirectoryWatcher;
import grassmarlin.ui.common.MutablePoint;
import grassmarlin.ui.common.ZoomableScrollPane;
import grassmarlin.ui.common.menu.ActiveMenuItem;
import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.beans.binding.BooleanExpression;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.ObservableList;
import javafx.geometry.Point2D;
import javafx.scene.Parent;
import javafx.scene.control.CheckMenuItem;
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
import java.io.File;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Supplier;

public abstract class LogicalVisualization extends StackPane implements ILogicalVisualization, XmlSerializable{
    protected static final String LAYER_AGGREGATES = "Groups";
    protected static final String LAYER_EDGES = "Edges";
    protected static final String LAYER_NODES = "Nodes";

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

    private final LogicalGraph graph;

    private final ZoomableScrollPane zsp;

    private final Map<GraphLogicalVertex, VisualLogicalVertex> vertices;
    private final Map<GraphLogicalEdge, VisualLogicalEdge> edges;

    private String groupingCurrent;
    private VisualAggregateLayer groupingVisual;
    private final Map<String, VisualAggregateLayer> groupingsVisual;

    private final ImageDirectoryWatcher imageMapper;

    private final MenuItem miExportSvg;
    private final CheckMenuItem ckRunLayout;

    private final ViewLogical view;

    private Supplier<List<MenuItem>> commonContextMenuItemSource;
    private BiFunction<GraphLogicalVertex, LogicalVertex, List<MenuItem>> vertexContextMenuItemSource;
    protected final ArrayList<MenuItem> visualizationMenuItems;

    public Event<VisualVertexEventArgs> onVisualLogicalVertexCreated;

    public LogicalVisualization(final ViewLogical view) {
        this.graph = view.getGraph();
        this.view = view;

        this.zsp = new ZoomableScrollPane(this::handle_ConstructContextMenu, this::hideContextMenu, LAYER_AGGREGATES, LAYER_EDGES, LAYER_NODES);

        this.vertices = new HashMap<>();
        this.edges = new HashMap<>();

        this.groupingCurrent = null;
        this.groupingVisual = null;
        this.groupingsVisual = new HashMap<>();

        this.imageMapper = view.getImageDirectoryWatcher();

        this.onVisualLogicalVertexCreated = new Event<>(Event.PROVIDER_JAVAFX);

        this.visualizationMenuItems = new ArrayList<>();
        this.ckRunLayout = new CheckMenuItem("Run Layout");
        this.ckRunLayout.setSelected(true);
        this.ckRunLayout.setOnAction(event -> {
            LogicalVisualization.this.requestLayout();
        });
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
        this.visualizationMenuItems.addAll(Arrays.asList(this.miExportSvg, this.ckRunLayout));

        initComponents();

        graph.onLogicalGraphVertexCreated.addHandler(this.handlerVertexAdded);
        graph.onLogicalGraphVertexRemoved.addHandler(this.handlerVertexRemoved);
        graph.onLogicalGraphEdgeCreated.addHandler(this.handlerEdgeAdded);
        graph.onLogicalGraphEdgeRemoved.addHandler(this.handlerEdgeRemoved);

        final ArrayList<GraphLogicalVertex> vertices = new ArrayList<>();
        final ArrayList<GraphLogicalEdge> edges = new ArrayList<>();

        graph.getAtomic(vertices, edges);

        for (final GraphLogicalVertex vertexAdded : vertices) {
            LogicalVisualization.this.addVertex(vertexAdded);
        }
        for(final GraphLogicalEdge edgeAdded : edges) {
            LogicalVisualization.this.addEdge(edgeAdded);
        }
        Platform.runLater(this.zsp::zoomReset);
    }

    private void initComponents() {
        this.zsp.prefWidthProperty().bind(this.widthProperty());
        this.zsp.prefHeightProperty().bind(this.heightProperty());

        if(Version.IS_BETA) {
            final Text textBetaDisclaimer = new Text("\u03B2");
            textBetaDisclaimer.setFont(new Font("Lucida Console", 96.0));
            textBetaDisclaimer.setStroke(Color.AQUAMARINE);
            textBetaDisclaimer.setOpacity(0.6);

            this.getChildren().add(textBetaDisclaimer);
        }

        this.getChildren().add(this.zsp);
    }

    /**
     * Override in child classes to clean up any resources that need to be cleaned up.
     */
    public void close() {

    }

    @Override
    public void setContextMenuSources(final Supplier<List<MenuItem>> common, final BiFunction<GraphLogicalVertex, LogicalVertex, List<MenuItem>> vertex) {
        this.commonContextMenuItemSource = common;
        this.vertexContextMenuItemSource = vertex;
    }

    public void markAsModified() {
        this.view.markSessionAsModified();
    }

    private Event.EventListener<GraphLogicalVertex> handlerVertexAdded = this::handleVertexAdded;
    private void handleVertexAdded(final Event<GraphLogicalVertex> event, final GraphLogicalVertex added) {
        this.addVertex(added);
    }
    private Event.EventListener<GraphLogicalVertex> handlerVertexRemoved = this::handleVertexRemoved;
    private void handleVertexRemoved(final Event<GraphLogicalVertex> event, final GraphLogicalVertex removed) {
        this.removeVertex(removed);
    }

    private Event.EventListener<GraphLogicalEdge> handlerEdgeAdded = this::handleEdgeAdded;
    private void handleEdgeAdded(final Event<GraphLogicalEdge> event, final GraphLogicalEdge added) {
        this.addEdge(added);
    }
    private Event.EventListener<GraphLogicalEdge> handlerEdgeRemoved = this::handleEdgeRemoved;
    private void handleEdgeRemoved(final Event<GraphLogicalEdge> event, final GraphLogicalEdge removed) {
        this.removeEdge(removed);
    }

    @Override
    public Collection<VisualLogicalVertex> getVertices() {
        synchronized(this.vertices) {
            return new ArrayList<>(this.vertices.values());
        }
    }
    public Collection<VisualLogicalEdge> getEdges() {
        return this.edges.values();
    }

    public BooleanExpression useWeightedEdgesProperty() {
        return this.view.useWeightedEdgesProperty();
    }
    public BooleanExpression useCurvedEdgesProperty() {
        return this.view.useCurvedEdgesProperty();
    }

    /**
     * Factory method to produce VisualLogicalVertices; to be overridden in subclasses to change the visual element class.
     */
    protected VisualLogicalVertex createVisualLogicalVertexFor(final GraphLogicalVertex vertex, final ObservableList<Image> images) {
        return new VisualLogicalVertex(this, vertex, images);
    }

    protected void addVertex(final GraphLogicalVertex vertex) {
        final VisualLogicalVertex visual = createVisualLogicalVertexFor(vertex, this.imageMapper.watchContainer(vertex));
        visual.translateXProperty().addListener(this.handlerVertexMoved);
        visual.translateYProperty().addListener(this.handlerVertexMoved);

        synchronized(this.vertices) {
            vertices.put(vertex, visual);
        }
        this.zsp.addChild(visual, LAYER_NODES);
        visual.layout();

        this.onVisualLogicalVertexCreated.call(new VisualVertexEventArgs(visual));

        if(this.groupingVisual != null) {
            this.groupingVisual.requestLayout();
        }

        this.graph.onPropertyValuesChanged.addHandler(this.handlerPropertyChanged);
    }

    private final InvalidationListener handlerVertexMoved = this::handleVertexMoved;
    private void handleVertexMoved(final javafx.beans.Observable target) {
        if(this.groupingVisual != null) {
            this.groupingVisual.requestLayout();
        }
    }

    private final Event.EventListener<String> handlerPropertyChanged = this::handlePropertyChanged;
    protected void handlePropertyChanged(final Event<String> event, final String args) {
        if(this.groupingVisual != null) {
            this.groupingVisual.requestLayout();
        }
    }

    protected void removeVertex(final GraphLogicalVertex vertex) {
        final VisualLogicalVertex visual = vertices.get(vertex);
        visual.translateXProperty().removeListener(this.handlerVertexMoved);
        visual.translateYProperty().removeListener(this.handlerVertexMoved);

        this.imageMapper.unwatchContainer(vertex);
        if(visual != null) {
            synchronized(this.vertices) {
                vertices.remove(vertex);
            }
            this.zsp.removeChild(visual, LAYER_NODES);

            if(this.groupingVisual != null) {
                this.groupingVisual.requestLayout();
            }
        }
    }

    protected void addEdge(final GraphLogicalEdge edge) {
        final VisualLogicalEdge visual;
        synchronized(this.vertices) {
            visual = new VisualLogicalEdge(this, edge, vertices.get(edge.getSource()), vertices.get(edge.getDestination()), this.view.getEdgeRules(), this.view.useStyledEdgesProperty());
        }
        this.edges.put(edge, visual);
        this.zsp.addChild(visual, LAYER_EDGES);
        visual.layout();
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
    @Override
    public VisualLogicalVertex vertexFor(final GraphLogicalVertex vertex) {
        synchronized(this.vertices) {
            return vertices.get(vertex);
        }
    }
    public VisualLogicalEdge edgeFor(final GraphLogicalEdge edge) {
        return edges.get(edge);
    }

    private final ContextMenu menu = new ContextMenu();

    private void handle_ConstructContextMenu(final List<Object> objects, final Point2D screenLocation) {
        menu.hide();
        menu.getItems().clear();
        menu.getItems().addAll(this.visualizationMenuItems);
        menu.getItems().addAll(this.commonContextMenuItemSource.get());

        for(final Object object : objects) {
            if(object instanceof GraphLogicalVertex) {
                //Find the row under the cursor
                final VisualLogicalVertex visual = vertices.get(object);
                if(visual == null || !visual.showChildrenProperty().get()) {
                    //There is no visual element or children are hidden -> no row is available, so use the root row.
                    menu.getItems().addAll(this.vertexContextMenuItemSource.apply((GraphLogicalVertex)object, ((GraphLogicalVertex)object).getVertex()));
                } else {
                    final Point2D localLocation = visual.screenToLocal(screenLocation);
                    visual.getChildren().stream().filter(child -> child.getBoundsInParent().contains(localLocation)).filter(child -> child instanceof VisualLogicalVertex.ContentRow).map(child -> (VisualLogicalVertex.ContentRow)child).forEach(row -> {
                        menu.getItems().addAll(LogicalVisualization.this.vertexContextMenuItemSource.apply((GraphLogicalVertex)object, row.getVertex()));
                    });
                }
            } else if(object instanceof ICanHasContextMenu) {
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

    // Grouping and Aggregates
    public void setCurrentGrouping(final String nameGroupProperty) {
        //TODO: Ensure the grouping is actually changing (?)

        if(this.groupingVisual != null) {
            this.zsp.removeChild(this.groupingVisual);
            this.groupingVisual.clear();
            this.groupingVisual = null;
        }
        if(nameGroupProperty != null) {
            this.groupingVisual = this.groupingsVisual.get(nameGroupProperty);
            if(this.groupingVisual == null) {
                this.groupingVisual = new VisualAggregateLayer(this, nameGroupProperty, this.graph);
                this.groupingVisual.requestLayout();
                this.groupingsVisual.put(nameGroupProperty, this.groupingVisual);
            } else {
                this.groupingVisual.restore();
            }
            this.zsp.addChild(this.groupingVisual);
        }
        this.groupingCurrent = nameGroupProperty;
    }
    @Override
    public String getCurrentGrouping() {
        return this.groupingCurrent;
    }

    public ILogicalViewApi.ICalculateColorsForAggregate getColorFactory() {
        return this.view.getAggregateColorFactory();
    }

    ZoomableScrollPane getZsp() {
        return this.zsp;
    }

    public void zoomToVertex(final GraphLogicalVertex vertex) {
        synchronized(this.vertices) {
            this.zsp.zoomToVertex(vertices.get(vertex));
        }
    }

    @Override
    public void readFromXml(XMLStreamReader source) throws XMLStreamException {
        final String elementTerminal = source.getLocalName();

        while(source.hasNext()) {
            switch (source.nextTag()) {
                case XMLStreamReader.START_ELEMENT:
                    switch(source.getLocalName()) {
                        case "Vertex":
                            final String key = source.getAttributeValue(null, "Key");
                            final VisualLogicalVertex visual = this.vertexFor(key);
                            visual.setTranslateX(Double.parseDouble(source.getAttributeValue(null, "X")));
                            visual.setTranslateY(Double.parseDouble(source.getAttributeValue(null, "Y")));
                            visual.showChildrenProperty().set(Boolean.parseBoolean(source.getAttributeValue(null, "ShowChildren")));
                            break;
                        //TODO: Restore edge data
                        case "AggregateGroup":
                            final String nameProperty = source.getAttributeValue(null, "Grouping");
                            final VisualAggregateLayer layer = new VisualAggregateLayer(this, nameProperty, this.graph);
                            layer.readFromXml(source);
                            layer.requestLayout();

                            //If we just loaded the current grouping then reset the grouping.  This will clean up the old one and references the new one.
                            if(nameProperty.equals(this.groupingCurrent)) {
                                this.setCurrentGrouping(this.groupingCurrent);
                                layer.requestLayout();
                            }

                            this.groupingsVisual.put(nameProperty, layer);
                            break;
                        default:
                            //Don't know what to do with it.
                            Logger.log(Logger.Severity.WARNING, "Unexpected element reading Logical View: %s", source.getLocalName());
                    }
                    break;
                case XMLStreamReader.END_ELEMENT:
                    if(source.getLocalName().equals(elementTerminal)) {
                        //TODO: The Edges need a layout pass to re-choose the endpoints
                        return;
                    }
                    break;
                case XMLStreamReader.END_DOCUMENT:
                    return;
            }
        }
    }

    @Override
    public void writeToXml(XMLStreamWriter target) throws XMLStreamException {
        for(final VisualLogicalVertex visualVertex : this.vertices.values()) {
            target.writeStartElement("Vertex");
            target.writeAttribute("Key", visualVertex.getVertex().getKey());

            target.writeAttribute("X", Double.toString(visualVertex.getTranslateX()));
            target.writeAttribute("Y", Double.toString(visualVertex.getTranslateY()));

            target.writeAttribute("ShowChildren", Boolean.toString(visualVertex.showChildrenProperty().get()));

            target.writeEndElement();
        }
        //TODO: Write edge data
        for(final Map.Entry<String, VisualAggregateLayer> entry : this.groupingsVisual.entrySet()) {
            target.writeStartElement("AggregateGroup");
            target.writeAttribute("Grouping", entry.getKey());

            entry.getValue().writeToXml(target);

            target.writeEndElement();
        }
    }

    // == Layout

    protected final BooleanProperty autoLayout = new SimpleBooleanProperty(true);
    protected final ObjectProperty<ILogicalGraphLayout> layoutProperty = new SimpleObjectProperty<>(null);

    @Override
    protected void layoutChildren() {
        super.layoutChildren();

        if(this.autoLayout.get()) {
            final ILogicalGraphLayout layout = this.layoutProperty.get();
            if(layout != null && this.ckRunLayout.isSelected()) {
                //Run the layout in a worker thread then call back to the UI thread
                final Thread threadLayout = new Thread(() -> {
                    final Map<IVisualLogicalVertex, MutablePoint> locations = layout.executeLayout(LogicalVisualization.this);
                    if(locations != null && !locations.isEmpty()) {
                        Platform.runLater(() -> {
                            final Collection<VisualLogicalVertex> vertices = LogicalVisualization.this.vertices.values();
                            for (Map.Entry<IVisualLogicalVertex, MutablePoint> entry : locations.entrySet()) {
                                if (vertices.contains(entry.getKey()) && entry.getKey().isSubjectToLayout()) {
                                    entry.getKey().setTranslateX(Math.floor(entry.getValue().getX()));
                                    entry.getKey().setTranslateY(Math.floor(entry.getValue().getY()));
                                }
                            }
                            if(layout.requiresForcedUpdate()) {
                                LogicalVisualization.this.requestLayout();
                            }
                        });
                    }
                });
                threadLayout.setDaemon(true);
                threadLayout.start();
            }
        }
    }

    @Override
    public LogicalGraph getGraph() {
        return this.graph;
    }
}
