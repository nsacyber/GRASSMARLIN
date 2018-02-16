package grassmarlin.plugins.internal.logicalview;

import com.sun.javafx.collections.ObservableListWrapper;
import grassmarlin.Logger;
import grassmarlin.RuntimeConfiguration;
import grassmarlin.plugins.IPlugin;
import grassmarlin.plugins.internal.logicalview.visual.colorfactories.ConstantColorFactory;
import grassmarlin.plugins.internal.logicalview.visual.colorfactories.HashColorFactory;
import grassmarlin.plugins.internal.logicalview.visual.filters.*;
import grassmarlin.session.LogicalVertex;
import grassmarlin.session.Session;
import grassmarlin.session.pipeline.ILogicalPacketMetadata;
import grassmarlin.ui.common.ImageDirectoryWatcher;
import grassmarlin.ui.common.SessionInterfaceController;
import grassmarlin.ui.common.menu.ActiveMenuItem;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.control.MenuItem;
import javafx.scene.image.Image;
import javafx.scene.layout.Pane;

import javax.xml.stream.*;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class Plugin implements IPlugin, IPlugin.SessionEventHooks, IPlugin.DefinesPipelineStages, IPlugin.SessionSerialization, ILogicalViewApi {
    private static class VertexMenuMetadata {
        private final BiPredicate<GraphLogicalVertex, LogicalVertex> condition;
        private final String name;
        private final TriConsumer<Session, GraphLogicalVertex, LogicalVertex> action;

        public VertexMenuMetadata(final BiPredicate<GraphLogicalVertex, LogicalVertex> condition, final String name, final TriConsumer<Session, GraphLogicalVertex, LogicalVertex> action) {
            this.condition = condition;
            this.name = name;
            this.action = action;
        }

        public MenuItem constructFor(final Session session, final GraphLogicalVertex vertex, final LogicalVertex row) {
            final MenuItem result = new ActiveMenuItem(this.name, event -> {
                VertexMenuMetadata.this.action.accept(session, vertex, row);
            });
            result.setDisable(!this.condition.test(vertex, row));
            return result;
        }
    }

    private static List<PipelineStage> stages = new ArrayList<PipelineStage>() {
        {
            this.add(new PipelineStage(false, StageRecordLogicalPacketData.NAME, StageRecordLogicalPacketData.class, StageRecordLogicalPacketData.DEFAULT_OUTPUT, StageRecordLogicalPacketData.OUTPUT_PCAP, StageRecordLogicalPacketData.OUTPUT_TCP, StageRecordLogicalPacketData.OUTPUT_UDP));
            this.add(new PipelineStage(false, StageNetworkCreationFromLogicalAddresses.NAME, StageNetworkCreationFromLogicalAddresses.class, StageNetworkCreationFromLogicalAddresses.DEFAULT_OUTPUT));
        }
    };

    protected final RuntimeConfiguration config;
    private final Map<Session, SessionState> states;

    protected final ImageDirectoryWatcher imageMapper;

    // Collections manipulated by other plugins via the Logical Graph API
    //TODO: Fix access for edgeRuleUiElements
    final ObservableList<Plugin.EdgeRuleUiFactory> edgeRuleUiElements;
    private final Map<String, Supplier<ILogicalViewApi.ICalculateColorsForAggregate>> colorFactoryFactories;

    public Plugin(final RuntimeConfiguration config) {
        this.config = config;
        this.states = new HashMap<>();
        this.edgeRuleUiElements = new ObservableListWrapper<>(new LinkedList<>());
        this.addEdgeStyleRuleUi("Has Property", ControlHasProperty::new, ControlHasProperty::getEdgeStyleRule, ControlHasProperty::setEdgeStyleRule);
        this.addEdgeStyleRuleUi("Property Has Value", () -> new ControlPropertyHasValue(config), ControlPropertyHasValue::getEdgeStyleRule, ControlPropertyHasValue::setEdgeStyleRule);
        this.addEdgeStyleRuleUi("Always", () -> new Pane(), pane -> new AlwaysStyleRule(), (pane, edgeStyleRule) -> {});

        this.colorFactoryFactories = new HashMap<>();
        this.addGroupColorFactory("Compute From Group", () -> new HashColorFactory());
        this.addGroupColorFactory("Fixed Color", () -> new ConstantColorFactory());

        this.imageMapper = new ImageDirectoryWatcher(config.getUiEventProvider());
        this.imageMapper.addWatchDirectory(Paths.get(RuntimeConfiguration.getPersistedString(RuntimeConfiguration.PersistableFields.DIRECTORY_APPLICATION), "images", "logical"));
    }

    public Map<String, Supplier<ILogicalViewApi.ICalculateColorsForAggregate>> getColorFactoryFactories() {
        synchronized(this.colorFactoryFactories) {
            return new HashMap<>(this.colorFactoryFactories);
        }
    }

    @Override
    public void sessionCreated(final Session session, final SessionInterfaceController controller) {
        final SessionState sessionState = new SessionState(session, controller, this);
        states.put(session, sessionState);
    }
    @Override
    public void sessionLoaded(final Session session, final InputStream stream, final GetChildStream fnGetStream) throws IOException {
        final SessionState state = this.states.get(session);
        if(state == null) {
            //TODO: this is a pretty serious error if it happens, but it should never happen.
            return;
        } else {
            try {
                final XMLStreamReader xmlIn = XMLInputFactory.newInstance().createXMLStreamReader(stream);
                xmlIn.nextTag();
                state.readFromXml(xmlIn);
            } catch(XMLStreamException ex) {
                Logger.log(Logger.Severity.ERROR, "There was an error restoring the Logical Graph: %s", ex.getMessage());
            }

            for(final Map.Entry<String, SessionInterfaceController.View> entry : state.getViews()) {
                if(entry.getValue() instanceof ViewLogical) {
                    //TODO: Restore visualization elements
                    final InputStream streamView = fnGetStream.getStream(entry.getKey());
                    try {
                        final XMLStreamReader xmlView = XMLInputFactory.newInstance().createXMLStreamReader(streamView);
                        xmlView.nextTag();
                        final ViewLogical view = (ViewLogical)entry.getValue();
                        //The restoration of the visual lements has to run in the UI thread.
                        this.config.getUiEventProvider().runNow(() -> {
                            try {
                                view.readFromXml(xmlView);
                            } catch(Exception ex) {
                                Logger.log(Logger.Severity.WARNING, "There was an error restoring a view (%s: %s)", entry.getKey(), entry.getValue().titleProperty().get());
                            }
                        });
                    } catch(XMLStreamException ex) {
                        Logger.log(Logger.Severity.WARNING, "There was an error restoring a view (%s: %s)", entry.getKey(), entry.getValue().titleProperty().get());
                    }
                }
            }
        }
    }

    @Override
    public void sessionSaving(final grassmarlin.session.Session session, final java.io.OutputStream stream, final IPlugin.SessionSerialization.CallbackCreateStream fnCreateStream) throws IOException{
        final SessionState state = this.states.get(session);
        if(state == null) {
            //Really, this should be an error, but we're just going to ignore it and move on.
            //There is no reason why we should be in this state and, if we are, saving anything is more wrong than saving nothing.
            //This isn't the sort of situation that should drive a failure, either.
            return;
        } else {
            //The session state is written into the default stream.
            try {
                final XMLStreamWriter xmlOut = XMLOutputFactory.newInstance().createXMLStreamWriter(stream);
                xmlOut.writeStartDocument();
                xmlOut.writeStartElement("LogicalGraph");
                state.writeToXml(xmlOut);
                xmlOut.writeEndElement();
                xmlOut.writeEndDocument();
            } catch(XMLStreamException ex) {
                //TODO: Report failure
            }

            for(final Map.Entry<String, SessionInterfaceController.View> entry : state.getViews()) {
                if(entry.getValue() instanceof ViewLogical) {
                    try {
                        final OutputStream out = fnCreateStream.getStream(entry.getKey(), false);
                        final XMLStreamWriter xmlOut = XMLOutputFactory.newInstance().createXMLStreamWriter(out);
                        xmlOut.writeStartDocument();
                        xmlOut.writeStartElement("View");
                        ((ViewLogical<?, ?>) entry.getValue()).writeToXml(xmlOut);
                        xmlOut.writeEndElement();
                        xmlOut.writeEndDocument();
                    } catch(XMLStreamException ex) {
                        //TODO: Report failure
                    }
                }
            }
        }
    }
    @Override
    public void sessionClosed(final Session session) {
        final SessionState state = states.get(session);
        if(state != null) {
            this.imageMapper.unwatchAll(state.getPrimaryView().graph.vertices);
        }
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
        return null;
    }

    // ==== API Methods =======================================================
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
    public void addGroupColorFactory(final String name, final Supplier<ILogicalViewApi.ICalculateColorsForAggregate> factory) {
        synchronized(colorFactoryFactories) {
            colorFactoryFactories.put(name, factory);
        }
    }

    @Override
    public void addVertexContextMenuItem(final BiPredicate<GraphLogicalVertex, LogicalVertex> condition, final String name, final TriConsumer<Session, GraphLogicalVertex, LogicalVertex> action) {
        this.vertexMenuItems.add(new VertexMenuMetadata(condition, name, action));
    }

    @Override
    public <N extends Node, R extends EdgeStyleRule> void addEdgeStyleRuleUi(final String name, final Supplier<N> uiFactory, final Function<N, R> getter, final BiConsumer<N, R> setter) {
        this.edgeRuleUiElements.add(new EdgeRuleUiFactory(name, uiFactory, getter, setter));
    }
    @Override
    public ObservableList<Style> getEdgeStylesForSession(final Session session) {
        return this.states.get(session).getEdgeStyles();
    }
    @Override
    public ObservableList<EdgeStyleRule> getEdgeStyleRulesForSession(final Session session) {
        return this.states.get(session).getEdgeRules();
    }

    @Override
    public void addMappedImage(final String nameProperty, final String valueProperty, final Image image) {
        this.imageMapper.addImage(nameProperty, valueProperty, image);
    }

    private final List<VertexMenuMetadata> vertexMenuItems = new ArrayList<>();

    public List<MenuItem> getMenuItemsFor(final Session session, final GraphLogicalVertex vertex, final LogicalVertex row) {
        return vertexMenuItems.stream().map(item -> item.constructFor(session, vertex, row)).collect(Collectors.toList());
    }

    // == Internal-use methods
    void recordPacketForSession(final Session session, final ILogicalPacketMetadata packet) {
        this.states.get(session).recordPacket(packet);
    }
}