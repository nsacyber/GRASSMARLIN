package grassmarlin.plugins.internal.logicalview;

import com.sun.javafx.collections.ObservableListWrapper;
import com.sun.xml.internal.txw2.output.IndentingXMLStreamWriter;
import grassmarlin.Event;
import grassmarlin.Logger;
import grassmarlin.RuntimeConfiguration;
import grassmarlin.plugins.IPlugin;
import grassmarlin.plugins.internal.logicalview.visual.FilteredLogicalVisualization;
import grassmarlin.plugins.internal.logicalview.visual.LogicalNavigation;
import grassmarlin.plugins.internal.logicalview.visual.LogicalVisualization;
import grassmarlin.plugins.internal.logicalview.visual.WatchLogicalVisualization;
import grassmarlin.plugins.internal.logicalview.visual.colorfactories.ConstantColorFactory;
import grassmarlin.plugins.internal.logicalview.visual.colorfactories.HashColorFactory;
import grassmarlin.plugins.internal.logicalview.visual.filters.*;
import grassmarlin.session.Session;
import grassmarlin.session.serialization.Loader;
import grassmarlin.session.serialization.Writer;
import grassmarlin.ui.common.ImageDirectoryWatcher;
import grassmarlin.ui.common.TabController;
import grassmarlin.ui.common.menu.ActiveMenuItem;
import javafx.beans.InvalidationListener;
import javafx.beans.binding.BooleanExpression;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.MenuItem;
import javafx.scene.control.Tab;
import javafx.scene.image.Image;
import javafx.scene.layout.Pane;

import javax.xml.stream.*;
import javax.xml.stream.events.XMLEvent;
import java.io.*;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.*;
import java.util.stream.Collectors;

public class Plugin implements IPlugin, IPlugin.SessionEventHooks, IPlugin.DefinesPipelineStages, IPlugin.SessionSerialization, ILogicalViewApi {
    public enum Subtype {
        DEFAULT,
        FILTER,
        WATCH
    }

    public interface IHasTitle {
        String getTitle();
    }

    private static class VertexMenuMetadata {
        private final Predicate<GraphLogicalVertex> condition;
        private final String name;
        private final Consumer<GraphLogicalVertex> action;

        public VertexMenuMetadata(final Predicate<GraphLogicalVertex> condition, final String name, final Consumer<GraphLogicalVertex> action) {
            this.condition = condition;
            this.name = name;
            this.action = action;
        }

        public MenuItem constructFor(final GraphLogicalVertex vertex) {
            final MenuItem result = new ActiveMenuItem(this.name, event -> {
                VertexMenuMetadata.this.action.accept(vertex);
            });
            result.setDisable(!this.condition.test(vertex));
            return result;
        }
    }


    private LogicalGraphVisualizationWrapper readVisualizationFrom(final LogicalGraphState state, final InputStream stream) {
        try {
            XMLInputFactory xif = XMLInputFactory.newInstance();
            XMLStreamReader reader = xif.createXMLStreamReader(stream);

            LogicalGraphVisualizationWrapper visualization = null;

            while(reader.hasNext()) {
                final int typeNext = reader.next();
                if(typeNext == XMLEvent.START_ELEMENT) {
                    switch(reader.getLocalName()) {
                        case "Wrapper":
                            Subtype subtype = Subtype.valueOf(reader.getAttributeValue(null, "Subtype"));
                            if(visualization == null) {
                                if(subtype == Subtype.DEFAULT) {
                                    visualization = state.getPrimaryVisualization();
                                } else if(subtype == Subtype.WATCH) {
                                    //We have to extract and specify the root
                                    final String keyRoot = reader.getAttributeValue(null, "keyRoot");
                                    final GraphLogicalVertex gRoot = state.graphPrimary.vertexForKey(keyRoot);  //If 'i' was an alias for 'this' and gRoot was a member variable, traditional notation would make the name 'i.m_gRoot'.  For that reason alone, I support the continued use of hungarian notation, despite the general obsolesence it faces from modern IDEs.
                                    visualization = new LogicalGraphVisualizationWrapper(config, state, subtype, gRoot);
                                } else {
                                    visualization = new LogicalGraphVisualizationWrapper(config, state, subtype);
                                }
                            }
                            break;
                        case "Visualization":
                            visualization.getVisualization().getGraph().waitForValid();
                            visualization.getVisualization().readFromXml(reader);
                            break;
                        case "UiState":
                            visualization.graph.groupingProperty().set(reader.getAttributeValue(null, "Grouping"));
                            break;
                    }
                }
            }
            return visualization;
        } catch(XMLStreamException ex) {
            ex.printStackTrace();
            return null;
        }
    }

    public class LogicalGraphVisualizationWrapper {
        private final FilteredLogicalGraph graph;
        private final LogicalVisualization visualization;
        private final LogicalNavigation navigation;
        private final Subtype subtype;
        private final String internalName;
        private Tab tab = null;

        public LogicalGraphVisualizationWrapper(final RuntimeConfiguration config, final LogicalGraphState state) {
            this(config, state, Subtype.DEFAULT);
        }
        public LogicalGraphVisualizationWrapper(final RuntimeConfiguration config, final LogicalGraphState state, final Subtype subtype) {
            this(config, state, subtype, null);
        }
        public LogicalGraphVisualizationWrapper(final RuntimeConfiguration config, final LogicalGraphState state, final Subtype subtype, final GraphLogicalVertex root) {
            this.subtype = subtype;
            //We probably need to construct the visualization in the ui thread, so we need a final reference we can set here.
            final SimpleObjectProperty<LogicalVisualization> result = new SimpleObjectProperty<>(null);

            switch(subtype) {
                case DEFAULT:
                    this.graph = state.graphPrimary;
                    this.graph.waitForValid();
                    config.getUiEventProvider().runNow(() -> {
                        result.set(new LogicalVisualization(Plugin.this, config, this.graph, state));
                    });
                    this.internalName = "Logical Graph";
                    break;
                case FILTER:
                    this.graph = new FilteredLogicalGraph(state.graph, config.getUiEventProvider());
                    this.graph.waitForValid();
                    config.getUiEventProvider().runNow(() -> {
                        result.set(new FilteredLogicalVisualization(Plugin.this, config, this.graph, state.getPrimaryVisualization().getVisualization(), state));
                    });
                    this.internalName = "Filter View " + state.idxNextFilteredView.getAndIncrement();
                    break;
                case WATCH:
                    this.graph = new FilteredLogicalGraph(state.graph, config.getUiEventProvider());
                    this.graph.waitForValid();
                    config.getUiEventProvider().runNow(() -> {
                        result.set(new WatchLogicalVisualization(Plugin.this, config, this.graph, state.graphPrimary, root, 2, state));
                    });
                    this.internalName = "Watch View " + state.idxNextFilteredView.getAndIncrement();
                    break;
                default:
                    throw new IllegalArgumentException("Invalid logical graph subtype.");
            }
            this.visualization = result.get();
            this.navigation = new LogicalNavigation(config, graph);
        }

        public LogicalVisualization getVisualization() {
            return this.visualization;
        }
        public LogicalNavigation getNavigation() {
            return this.navigation;
        }

        public String getInternalName() {
            return this.internalName;
        }

        public void setTab(final Tab tab) {
            if(this.tab == null) {
                this.tab = tab;
            }
        }
        public Tab getTab() {
            return this.tab;
        }

        public void writeTo(final OutputStream stream) {
            try {
                XMLOutputFactory xof = XMLOutputFactory.newInstance();
                XMLStreamWriter writer = new IndentingXMLStreamWriter(xof.createXMLStreamWriter(stream));

                writer.writeStartDocument();
                writer.writeStartElement("Wrapper");
                writer.writeAttribute("Subtype", subtype.toString());
                //Special case: we need the root to be reported here.
                if(subtype == Subtype.WATCH) {
                    writer.writeAttribute("keyRoot", ((WatchLogicalVisualization)this.visualization).getRoot().getKey());
                }

                writer.writeStartElement("UiState");
                if(this.graph.groupingProperty().get() != null) {
                    writer.writeAttribute("Grouping", this.graph.groupingProperty().get());
                    //EXTEND: If FilteredLogicalGraph has additional options, add them here.
                }
                writer.writeEndElement();

                writer.writeStartElement("Visualization");
                Writer.writeObject(writer, this.visualization);
                writer.writeEndElement();

                //The navigation lacks any state information

                writer.writeEndElement();
            } catch(XMLStreamException ex) {
                ex.printStackTrace();
            }
        }
    }

    public class LogicalGraphState {
        private final ImageDirectoryWatcher<Image> imageMapper;
        private final LogicalGraphVisualizationWrapper visualizationPrimary;
        private final LogicalGraph graph;
        private final FilteredLogicalGraph graphPrimary;
        private final Tab tabReports;
        private final Reports reports;
        private final TabController controller;
        private final Session session;
        private final Map<String, LogicalGraphVisualizationWrapper> visualizationsSecondary;
        private final AtomicInteger idxNextFilteredView = new AtomicInteger(1);
        private final AtomicInteger idxNextWatchView = new AtomicInteger(1);

        public LogicalGraphState(final Session session, final TabController tabController) {
            this.session = session;
            this.controller = tabController;

            //When building the Logical Graph, we want to wait for the state to propogate to that level before moving on to derived graphs.
            //This doesnt matter so much during a new session, but it does during loading.
            //Specifically, during load we make the assumption that the baseline data behind details that are being loaded exists, and that is not a valid assumption without waiting.
            this.graph = new SessionConnectedLogicalGraph(this);
            this.graph.waitForValid();

            this.graphPrimary = new FilteredLogicalGraph(this.graph, Plugin.this.config.getUiEventProvider());
            this.graphPrimary.waitForValid();
            this.tabReports = new Tab("Logical Graph Reports");
            this.reports = new Reports(this.graphPrimary);
            this.tabReports.setContent(this.reports);

            ImageDirectoryWatcher<Image> watcher;
            try {
                watcher = new ImageDirectoryWatcher<>(
                        Paths.get(config.getPersistedString(RuntimeConfiguration.PersistableFields.DIRECTORY_APPLICATION), "images", "logical"),
                        Event.PROVIDER_JAVAFX,
                        path -> new Image("file:" + path.toAbsolutePath().toString())
                );
            } catch(IOException ex) {
                watcher = null;
            }
            this.imageMapper = watcher;

            //This cannot be called before imageMapper is set.
            this.visualizationPrimary = new LogicalGraphVisualizationWrapper(Plugin.this.config, this);

            this.visualizationsSecondary = new HashMap<>();
        }

        public Session getSession() {
            return this.session;
        }
        public void showReports() {
            controller.addContent(tabReports, reports.getTreeRoot(), false);
            controller.showTab(tabReports);
        }
        public void hideReports() {
            controller.removeContent(tabReports);
        }
        public RuntimeConfiguration getRuntimeConfig() {
            return Plugin.this.config;
        }
        public boolean animateLayouts() {
            return ckAnimateLayouts.isSelected();
        }
        public ImageDirectoryWatcher<Image> getImageMapper() {
            return this.imageMapper;
        }

        public LogicalGraphVisualizationWrapper getPrimaryVisualization() {
            return this.visualizationPrimary;
        }

        public Map<String, LogicalGraphVisualizationWrapper> getSecondaryVisualizations() {
            return this.visualizationsSecondary;
        }

        public LogicalGraphVisualizationWrapper getVisualizationFor(final FilteredLogicalGraph graph) {
            return this.visualizationsSecondary.values().stream().filter(wrapper -> wrapper.graph == graph).findAny().orElse(null);
        }

        public void createFilteredView() {
            final LogicalGraphVisualizationWrapper visualizationNew = new LogicalGraphVisualizationWrapper(Plugin.this.config, this, Subtype.FILTER);
            final String name = visualizationNew.getInternalName();
            visualizationNew.setTab(this.controller.addContent(name, visualizationNew.getVisualization(), visualizationNew.getNavigation().getTreeRoot()));

            this.visualizationsSecondary.put(name, visualizationNew);
        }

        public void createWatchView(final GraphLogicalVertex root) {
            final String title = "Watch View (" + root.getVertex().getLogicalAddressMapping().toString() + ")";
            final LogicalGraphVisualizationWrapper visualizationNew = new LogicalGraphVisualizationWrapper(Plugin.this.config, this, Subtype.WATCH, root);
            final String name = visualizationNew.getInternalName();
            visualizationNew.setTab(this.controller.addContent(title, visualizationNew.getVisualization(), visualizationNew.getNavigation().getTreeRoot()));

            this.visualizationsSecondary.put(name, visualizationNew);
        }

        public void close(final LogicalVisualization content) {
            for(Map.Entry<String, LogicalGraphVisualizationWrapper> entry : visualizationsSecondary.entrySet()) {
                if(entry.getValue().getVisualization().equals(content)) {
                    visualizationsSecondary.remove(entry.getKey());
                    this.controller.removeContent(entry.getValue().getTab());
                }
            }
        }

        public Plugin getPlugin() {
            return Plugin.this;
        }
    }
    private static List<PipelineStage> stages = new ArrayList<PipelineStage>() {
        {
            this.add(new PipelineStage(false, StageRecordLogicalPacketData.NAME, StageRecordLogicalPacketData.class, StageRecordLogicalPacketData.DEFAULT_OUTPUT, StageRecordLogicalPacketData.OUTPUT_PCAP, StageRecordLogicalPacketData.OUTPUT_TCP, StageRecordLogicalPacketData.OUTPUT_UDP));
            this.add(new PipelineStage(false, StageNetworkCreationFromLogicalAddresses.NAME, StageNetworkCreationFromLogicalAddresses.class, StageNetworkCreationFromLogicalAddresses.DEFAULT_OUTPUT, StageNetworkCreationFromLogicalAddresses.OUTPUT_NETWORKS));
            this.add(new PipelineStage(false, StageLogicalGraphPacketFilter.NAME, StageLogicalGraphPacketFilter.class, StageLogicalGraphPacketFilter.DEFAULT_OUTPUT, StageLogicalGraphPacketFilter.OUTPUT_REJECTED_BY_FILTER));
        }
    };

    protected final RuntimeConfiguration config;
    private final Map<Session, LogicalGraphState> states;

    protected final List<MenuItem> miPlugin;
    protected final CheckMenuItem ckShowReports;
    protected final CheckMenuItem ckAnimateLayouts;
    protected final CheckMenuItem ckUseWeightedEdges;
    protected final CheckMenuItem ckCurvedEdges;

    protected final ColorFactoryMenuItem miSetAggregateColorFactory;
    protected final ObservableList<Style> styles;
    protected final ObservableList<EdgeStyleRule> edgeRules;
    protected final ObservableList<EdgeRuleUiFactory> edgeRuleUiElements;

    public Plugin(final RuntimeConfiguration config) {
        this.config = config;
        this.states = new HashMap<>();
        this.styles = new ObservableListWrapper<>(new LinkedList<>());
        this.styles.addAll(Arrays.stream(RuntimeConfiguration.getPersistedString(Style.STYLE_LIST).split("\\|")).map(name -> Style.getStyle(name)).collect(Collectors.toList()));
        this.styles.addListener(this.handlerStylesChanged);
        this.edgeRules = new ObservableListWrapper<>(new LinkedList<>());
        this.loadEdgeRules();
        this.edgeRules.addListener(this.handlerEdgeRulesChanged);
        this.edgeRuleUiElements = new ObservableListWrapper<>(new LinkedList<>());
        this.addEdgeStyleRuleUi("Has Property", ControlHasProperty::new, ControlHasProperty::getEdgeStyleRule, ControlHasProperty::setEdgeStyleRule);
        this.addEdgeStyleRuleUi("Property Has Value", () -> new ControlPropertyHasValue(config), ControlPropertyHasValue::getEdgeStyleRule, ControlPropertyHasValue::setEdgeStyleRule);
        this.addEdgeStyleRuleUi("Always", () -> new Pane(), pane -> new AlwaysStyleRule(), (pane, edgeStyleRule) -> {});

        this.ckShowReports = new CheckMenuItem("View Reports");
        this.ckShowReports.setOnAction(event -> {
            if(ckShowReports.isSelected()) {
                states.values().forEach(LogicalGraphState::showReports);
            } else {
                states.values().forEach(LogicalGraphState::hideReports);
            }
        });
        this.ckCurvedEdges = new CheckMenuItem("Use Curved Edges");
        this.ckUseWeightedEdges = new CheckMenuItem("Use Weighted Edges");
        this.ckAnimateLayouts = new CheckMenuItem("Animate Layouts");
        this.ckAnimateLayouts.setSelected(true);

        this.miSetAggregateColorFactory = new ColorFactoryMenuItem("Set Group Color Theme");

        final ConstantColorFactory factoryConstantColor = new ConstantColorFactory();
        final HashColorFactory factoryObjectHash = new HashColorFactory();

        final MenuItem miDumpLogicalGraphDataToConsole = new ActiveMenuItem("Dump Logical Graph to Console", event -> {
            for(LogicalGraphState state : states.values()) {
                System.out.println("==LOGICAL GRAPH==");
                System.out.println(state.graph.toString());
            }
        });
        miDumpLogicalGraphDataToConsole.visibleProperty().bind(config.isDeveloperModeProperty());

        this.miPlugin = new ArrayList<>();
        this.miPlugin.addAll(Arrays.asList(ckShowReports, miDumpLogicalGraphDataToConsole, ckCurvedEdges, ckUseWeightedEdges, ckAnimateLayouts, miSetAggregateColorFactory,
                new ActiveMenuItem("Edit Edge Styles", event -> {
                    new StyleEditor(Plugin.this.config, Plugin.this.styles, Plugin.this.edgeRules, Plugin.this.edgeRuleUiElements).showAndWait();
                })
        ));
    }

    public BooleanExpression useWeightedEdgesBinding() {
        return ckUseWeightedEdges.selectedProperty();
    }
    public BooleanExpression useCurvedEdgesBinding() {
        return ckCurvedEdges.selectedProperty();
    }
    private BooleanExpression useStraightEdgesBinding = null;
    public BooleanExpression useStraightEdgesBinding() {
        if(useStraightEdgesBinding == null) {
            useStraightEdgesBinding = ckCurvedEdges.selectedProperty().not();
        }
        return useStraightEdgesBinding;
    }
    public BooleanExpression animateLayoutsBinding() {
        return ckAnimateLayouts.selectedProperty();
    }

    protected final InvalidationListener handlerStylesChanged = this::handleStylesChanged;
    private void handleStylesChanged(final javafx.beans.Observable o) {
        this.config.setPersistedString(Style.STYLE_LIST, this.styles.stream().map(Style::getName).collect(Collectors.joining("|")));
    }

    private void loadEdgeRules() {
        this.edgeRules.clear();
        final String rulesAsString = RuntimeConfiguration.getPersistedString(EdgeStyleRule.RULE_LIST);
        if(rulesAsString.equals("")) {
            return;
        }

        try {
            final XMLStreamReader reader = XMLInputFactory.newFactory().createXMLStreamReader(new ByteArrayInputStream(Base64.getDecoder().decode(rulesAsString)));

            int tag;
            while((tag = reader.nextTag()) != XMLEvent.END_DOCUMENT) {
                if(tag == XMLEvent.START_ELEMENT && reader.getLocalName().equals("Rule")) {
                    this.edgeRules.add((EdgeStyleRule) Loader.readObject(reader));
                }
            }
        } catch(XMLStreamException ex) {
            Logger.log(Logger.Severity.ERROR, "There was an error restoring the list of Edge Style Rules: %s", ex.getMessage());
        }
    }

    public ObservableList<EdgeStyleRule> getEdgeRules() {
        return this.edgeRules;
    }

    protected final InvalidationListener handlerEdgeRulesChanged = this::handleEdgeStylesChanged;
    private void handleEdgeStylesChanged(final javafx.beans.Observable o) {
        final ByteArrayOutputStream stream = new ByteArrayOutputStream();
        try {
            final XMLStreamWriter writer = XMLOutputFactory.newFactory().createXMLStreamWriter(stream);
            writer.writeStartElement("Rules");
            for(final EdgeStyleRule rule : this.edgeRules) {
                writer.writeStartElement("Rule");
                Writer.writeObject(writer, rule);
                writer.writeEndElement();
            }
            writer.writeEndElement();
        } catch(XMLStreamException ex) {
            //TODO: Log error
        }
        RuntimeConfiguration.setPersistedString(EdgeStyleRule.RULE_LIST, Base64.getEncoder().encodeToString(stream.toByteArray()));
    }

    @Override
    public void sessionCreated(final Session session, final TabController tabs) {
        final LogicalGraphState state = new LogicalGraphState(session, tabs);
        states.put(session, state);

        tabs.addContent("Logical Graph", state.getPrimaryVisualization().getVisualization(), state.getPrimaryVisualization().getNavigation().getTreeRoot());

        ckShowReports.setSelected(false);
    }
    @Override
    public void sessionLoaded(final Session session, final InputStream stream, final GetChildStream fnGetStream) throws IOException {
        final LogicalGraphState state = states.get(session);

        // We need to read the default stream to identify what visualizations exist.
        final List<String> names = new LinkedList<>();
        try {
            XMLInputFactory xif = XMLInputFactory.newInstance();
            XMLStreamReader reader = xif.createXMLStreamReader(stream);

            //We need to advance to the UiState element
            while(reader.hasNext()) {
                final int typeNext = reader.next();
                if(typeNext == XMLEvent.START_ELEMENT) {
                    switch(reader.getLocalName()) {
                        case "Manifest":
                            state.idxNextFilteredView.set(Integer.parseInt(reader.getAttributeValue(null, "NextFilteredView")));
                            state.idxNextWatchView.set(Integer.parseInt(reader.getAttributeValue(null, "NextWatchView")));
                            break;
                        case "Secondary":
                            names.add(reader.getAttributeValue(null, "name"));
                            break;
                    }
                }
            }
            //The Filters should already have been read--that is how we got this object in the first place.
        } catch(XMLStreamException ex) {
            ex.printStackTrace();
        }

        state.graph.readFrom(fnGetStream.getStream("Graph"));

        //Special Case: We always have the primary visualization, so it is special cased in state as a final value--this will read the data into the existing Visualization.
        readVisualizationFrom(state, fnGetStream.getStream("PrimaryVisualization"));

        //Construct and load all secondary visualizations
        for(final String name : names) {
            final LogicalGraphVisualizationWrapper visualization = readVisualizationFrom(state, fnGetStream.getStream("SecondaryVisualization." + name));
            state.getSecondaryVisualizations().put(name, visualization);
            if(visualization.getVisualization() instanceof IHasTitle) {
                state.controller.addContent(((IHasTitle)visualization.getVisualization()).getTitle(), visualization.getVisualization(), visualization.getNavigation().getTreeRoot());
            } else {
                state.controller.addContent(name, visualization.getVisualization(), visualization.getNavigation().getTreeRoot());
            }
        }
    }

    @Override
    public void sessionSaving(final grassmarlin.session.Session session, final java.io.OutputStream stream, final IPlugin.SessionSerialization.CallbackCreateStream fnCreateStream) throws IOException{
        final LogicalGraphState state = states.get(session);
        if(state != null) {
            //Write the manifest, which should summarize all the contents of state.
            try {
                XMLOutputFactory xof = XMLOutputFactory.newInstance();
                XMLStreamWriter writer = new IndentingXMLStreamWriter(xof.createXMLStreamWriter(stream));

                writer.writeStartDocument();
                writer.writeStartElement("Manifest");

                writer.writeAttribute("NextFilteredView", Integer.toString(state.idxNextFilteredView.get()));
                writer.writeAttribute("NextWatchView", Integer.toString(state.idxNextWatchView.get()));

                for(Map.Entry<String, LogicalGraphVisualizationWrapper> entry : state.getSecondaryVisualizations().entrySet()) {
                    writer.writeStartElement("Secondary");
                    writer.writeAttribute("name", entry.getKey());
                }

                writer.writeEndElement();
                writer.writeEndDocument();
                writer.flush();
            } catch(XMLStreamException ex) {
                //TODO: Cry a little, then fail.
                ex.printStackTrace();
            }

            //We need to serialize state...  but most is just transient resource wrappers and accessors.  Aside from the graph and visualizations, we don't need to save anything.  I think.
            state.graph.writeTo(fnCreateStream.getStream("Graph", false));

            //Write the details for each visualization to individual streams.
            state.visualizationPrimary.writeTo(fnCreateStream.getStream("PrimaryVisualization", false));
            for(Map.Entry<String, LogicalGraphVisualizationWrapper> entry : state.getSecondaryVisualizations().entrySet()) {
                entry.getValue().writeTo(fnCreateStream.getStream("SecondaryVisualization." + entry.getKey(), false));
            }
        }
    }
    @Override
    public void sessionClosed(final Session session) {
        states.remove(session);
    }

    @Override
    public Collection<PipelineStage> getPipelineStages() {
        return stages;
    }

    @Override
    public String getName() {
        return "Logical View";
    }

    @Override
    public Collection<MenuItem> getMenuItems() {
        return this.miPlugin;
    }

    public LogicalVisualization.ICalculateColorsForAggregate getAggregateColorFactory() {
        return this.miSetAggregateColorFactory.getFactory();
    }

    // ==== API Methods =======================================================
    @Override
    public void addGroupColorFactory(final Supplier<ColorFactoryMenuItem.IColorFactory> factory) {
        miSetAggregateColorFactory.addFactory(factory);
    }

    @Override
    public void addVertexContextMenuItem(final Predicate<GraphLogicalVertex> condition, final String name, final Consumer<GraphLogicalVertex> action) {
        this.vertexMenuItems.add(new VertexMenuMetadata(condition, name, action));
    }

    public static class EdgeRuleUiFactory<N extends Node, R extends EdgeStyleRule> {
        private final String name;
        private final Supplier<N> uiFactory;
        private final Function<N, R> getter;
        private final BiConsumer<N, R> setter;

        public EdgeRuleUiFactory(final String name, final Supplier<N> uiFactory, final Function<N, R> getter, final BiConsumer<N, R> setter) {
            this.name = name;
            this.uiFactory = uiFactory;
            this.getter = getter;
            this.setter = setter;
        }

        public String getName() {
            return name;
        }

        public Supplier<N> getUiFactory() {
            return uiFactory;
        }

        public Function<N, R> getGetter() {
            return getter;
        }

        public BiConsumer<N, R> getSetter() {
            return setter;
        }

        @Override
        public String toString() {
            return this.name;
        }
    }

    @Override
    public <N extends Node, R extends EdgeStyleRule> void addEdgeStyleRuleUi(final String name, final Supplier<N> uiFactory, final Function<N, R> getter, final BiConsumer<N, R> setter) {
        this.edgeRuleUiElements.add(new EdgeRuleUiFactory(name, uiFactory, getter, setter));
    }



    private final List<VertexMenuMetadata> vertexMenuItems = new ArrayList<>();

    public List<MenuItem> getMenuItemsFor(final GraphLogicalVertex vertex) {
        return vertexMenuItems.stream().map(item -> item.constructFor(vertex)).collect(Collectors.toList());
    }
}
