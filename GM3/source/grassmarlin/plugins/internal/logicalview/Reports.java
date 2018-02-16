package grassmarlin.plugins.internal.logicalview;

import com.sun.javafx.collections.ObservableListWrapper;
import grassmarlin.Event;
import grassmarlin.common.ProtocolSetExpression;
import grassmarlin.session.HardwareAddress;
import grassmarlin.session.LogicalAddress;
import grassmarlin.session.Property;
import grassmarlin.session.PropertyContainer;
import grassmarlin.session.serialization.Csv;
import grassmarlin.ui.common.RevalidatingFilteredList;
import grassmarlin.ui.common.menu.ActiveMenuItem;
import grassmarlin.ui.common.tree.SelectableTreeItem;
import javafx.beans.binding.ObjectBinding;
import javafx.beans.binding.ObjectExpression;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

import java.io.File;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class Reports extends Pane {
    protected enum ReportModes {
        ENDPOINTS,
        NODES,
        CONNECTIONS,
        DIRECTIONAL_CONNECTIONS,
        AGGREGATE_CONNECTIONS,
        INTERGROUP_CONNECTIONS,
        MULTIPLE_LOGICAL_PER_HARDWARE,
        MULTIPLE_HARDWARE_PER_LOGICAL
    }

    private final static class DirectionalEdge {
        private final GraphLogicalEdge edge;
        private final boolean forward;

        public DirectionalEdge(final GraphLogicalEdge edge, final boolean isForward) {
            this.edge = edge;
            this.forward = isForward;
        }

        public GraphLogicalEdge getEdge() {
            return this.edge;
        }
        public boolean isForward() {
            return this.forward;
        }
        public ObjectExpression<Long> packetCountProperty() {
            return this.forward ? edge.sentPacketCountProperty().asObject() : edge.recvPacketCountProperty().asObject();
        }
        public ObjectExpression<Long> packetSizeProperty() {
            return this.forward ? edge.sentPacketSizeProperty().asObject() : edge.recvPacketSizeProperty().asObject();
        }
        public ObjectProperty<LogicalAddress> sourceProperty() {
            return new ReadOnlyObjectWrapper<>((this.forward ? edge.getSource() : edge.getDestination()).getRootLogicalAddressMapping().getLogicalAddress());
        }
        public ObjectProperty<LogicalAddress> destinationProperty() {
            return new ReadOnlyObjectWrapper<>((!this.forward ? edge.getSource() : edge.getDestination()).getRootLogicalAddressMapping().getLogicalAddress());
        }

        public ObjectExpression<String> buildSourceGroupProperty(final ObjectExpression<String> grouping) {
            return new ObjectBinding<String>() {
                {
                    super.bind(grouping);
                    if(forward) {
                        DirectionalEdge.this.edge.getSource().onPropertyChanged.addHandler(this.handlerPropertyChanged);
                    } else {
                        DirectionalEdge.this.edge.getDestination().onPropertyChanged.addHandler(this.handlerPropertyChanged);
                    }
                }

                private Event.EventListener<PropertyContainer.PropertyEventArgs> handlerPropertyChanged = this::handlePropertyChanged;
                private void handlePropertyChanged(final Event<PropertyContainer.PropertyEventArgs> event, final PropertyContainer.PropertyEventArgs args) {
                    if(args.getName().equals(grouping.get())) {
                        this.invalidate();
                    }
                }

                @Override
                protected String computeValue() {
                    final Set<Property<?>> properties;
                    if(forward) {
                        properties = DirectionalEdge.this.edge.getSource().getProperties().get(grouping.get());
                    }  else {
                        properties = DirectionalEdge.this.edge.getDestination().getProperties().get(grouping.get());
                    }
                    if(properties == null) {
                        return "";
                    } else {
                        return properties.stream().map(Property::toString).collect(Collectors.joining(", "));
                    }
                }
            };
        }
        public ObjectExpression<String> buildDestinationGroupProperty(final ObjectExpression<String> grouping) {
            return new ObjectBinding<String>() {
                {
                    super.bind(grouping);
                    if(!forward) {
                        DirectionalEdge.this.edge.getSource().onPropertyChanged.addHandler(this.handlerPropertyChanged);
                    } else {
                        DirectionalEdge.this.edge.getDestination().onPropertyChanged.addHandler(this.handlerPropertyChanged);
                    }
                }

                private Event.EventListener<PropertyContainer.PropertyEventArgs> handlerPropertyChanged = this::handlePropertyChanged;
                private void handlePropertyChanged(final Event<PropertyContainer.PropertyEventArgs> event, final PropertyContainer.PropertyEventArgs args) {
                    if(args.getName().equals(grouping.get())) {
                        this.invalidate();
                    }
                }

                @Override
                protected String computeValue() {
                    final Set<Property<?>> properties;
                    if(!forward) {
                        properties = DirectionalEdge.this.edge.getSource().getProperties().get(grouping.get());
                    }  else {
                        properties = DirectionalEdge.this.edge.getDestination().getProperties().get(grouping.get());
                    }
                    if(properties == null) {
                        return "";
                    } else {
                        return properties.stream().map(Property::toString).collect(Collectors.joining(", "));
                    }
                }
            };
        }
    }

    private final LogicalGraph graph;
    private ReportModes mode;
    private final Pane container;
    private final Map<ReportModes, Node> content;
    private final TreeItem<Object> rootTree;
    private final FileChooser chooserExport;

    private final ObservableList<GraphLogicalVertex> observableVertices;
    private final ObservableList<GraphLogicalEdge> observableEdges;
    private final ObservableList<DirectionalEdge> directionalEdges;

    public Reports(final LogicalGraph graph) {
        this.graph = graph;
        this.mode = null;
        this.container = new Pane();
        this.content = new HashMap<>();
        this.rootTree = new TreeItem<>();

        this.chooserExport = new FileChooser();
        this.chooserExport.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("CSV Files", "*.csv"),
                new FileChooser.ExtensionFilter("All Files", "*")
        );
        this.chooserExport.setTitle("Export to...");

        this.observableVertices = new ObservableListWrapper<>(new LinkedList<>());
        this.observableEdges = new ObservableListWrapper<>(new LinkedList<>());
        this.directionalEdges = new ObservableListWrapper<DirectionalEdge>(new LinkedList<>());

        //These events fire in the UI Thread
        this.graph.onLogicalGraphVertexCreated.addHandler(this.handlerVertexAdded);
        this.graph.onLogicalGraphVertexRemoved.addHandler(this.handlerVertexRemoved);
        this.graph.onLogicalGraphEdgeCreated.addHandler(this.handlerEdgeAdded);
        this.graph.onLogicalGraphEdgeRemoved.addHandler(this.handlerEdgeRemoved);

        initComponents();
    }

    private Event.EventListener<GraphLogicalVertex> handlerVertexAdded = this::handleVertexAdded;
    private void handleVertexAdded(final Event<GraphLogicalVertex> event, final GraphLogicalVertex added) {
        this.observableVertices.add(added);
    }
    private Event.EventListener<GraphLogicalVertex> handlerVertexRemoved = this::handleVertexRemoved;
    private void handleVertexRemoved(final Event<GraphLogicalVertex> event, final GraphLogicalVertex removed) {
        this.observableVertices.remove(removed);
    }

    private Event.EventListener<GraphLogicalEdge> handlerEdgeAdded = this::handleEdgeAdded;
    private void handleEdgeAdded(final Event<GraphLogicalEdge> event, final GraphLogicalEdge added) {
        this.observableEdges.add(added);
        this.directionalEdges.add(new DirectionalEdge(added, true));
        this.directionalEdges.add(new DirectionalEdge(added, false));
    }
    private Event.EventListener<GraphLogicalEdge> handlerEdgeRemoved = this::handleEdgeRemoved;
    private void handleEdgeRemoved(final Event<GraphLogicalEdge> event, final GraphLogicalEdge removed) {
        this.observableEdges.remove(removed);
        this.directionalEdges.add(new DirectionalEdge(removed, true));
        this.directionalEdges.add(new DirectionalEdge(removed, false));
    }

    private void initComponents() {
        this.container.prefWidthProperty().bind(this.widthProperty());
        this.container.prefHeightProperty().bind(this.heightProperty());

        this.initNodesReports();

        this.initConnectionsReport();
        this.initDirectionalConnectionsReport();

        this.initIntergroupConnections();
        this.initOneToManyReports();

        final VBox layout = new VBox();
        layout.prefWidthProperty().bind(this.widthProperty());
        layout.prefHeightProperty().bind(this.heightProperty());

        layout.getChildren().add(this.container);
        getChildren().add(layout);
    }

    private void initNodesReports() {
        final VBox containerNodes = new VBox();

        final Predicate<GraphLogicalVertex> predNodesAll = logicalVertex -> true;
        final Predicate<GraphLogicalVertex> predNodesSource = logicalVertex -> this.observableEdges.stream().anyMatch(edge -> logicalVertex.getVertex().getLogicalAddress().contains(edge.getSource().getVertex().getLogicalAddress()));
        final Predicate<GraphLogicalVertex> predNodesDest = logicalVertex -> this.observableEdges.stream().anyMatch(edge -> logicalVertex.getVertex().getLogicalAddress().contains(edge.getDestination().getVertex().getLogicalAddress()));
        final Predicate<GraphLogicalVertex> predNodesBoth = logicalVertex -> predNodesSource.test(logicalVertex) && predNodesDest.test(logicalVertex);
        final TableView<GraphLogicalVertex> tblNodes = new TableView<>();
        final RevalidatingFilteredList<GraphLogicalVertex> lstNodes = new RevalidatingFilteredList<>(this.observableVertices, predNodesAll);
        final TableColumn<GraphLogicalVertex, LogicalAddress> colIp = new TableColumn<>("Address");
        colIp.setCellValueFactory(new PropertyValueFactory<>("title"));
        tblNodes.getColumns().addAll(colIp);
        tblNodes.setItems(lstNodes.getComputedList());
        tblNodes.prefWidthProperty().bind(containerNodes.widthProperty());
        tblNodes.prefHeightProperty().bind(containerNodes.heightProperty());

        // Add interface for adding columns to containerNodes
        final ComboBox<String> cbFields = new ComboBox<>();
        final Button btnAddField = new Button("Add");
        cbFields.setOnShowing(event -> {
            // Enumerate available columns
            final List<String> fields = this.observableVertices.stream().flatMap(vertex -> vertex.getProperties().keySet().stream()).distinct().sorted().collect(Collectors.toList());
            /*
            There has been some debate regarding whether to remove already-added columns from the list.
            We don't, because we have a couple "good enough" reasons to make duplicate columns viable:
            1) Need to match an expected format for some other application.
            2) It can make dumping it into a complicated spreadsheet more convenient.
            Neither of these is regarded as a good reason, but generally the person tasked with using either of those
            solutions is powerless to change the process, and if we can make what is generally a thankless, annoying
            task that is mandated by management (that probably doesn't even use the end result) even that much more
            tolerable, then we should.  Those of us that are so lucky we aren't subject to such tasking will take one
            for the team on this.

            Also, this is easier.
             */

            cbFields.getItems().retainAll(fields);
            fields.removeAll(cbFields.getItems());
            cbFields.getItems().addAll(fields);
        });
        btnAddField.disableProperty().bind(cbFields.getSelectionModel().selectedItemProperty().isNull());
        btnAddField.setOnAction(event -> {
            final String key = cbFields.getSelectionModel().getSelectedItem();
            if(key == null || key.isEmpty()) {
                //Ignore it
                return;
            }

            final TableColumn<GraphLogicalVertex, Object> colNew = new TableColumn<>(key);
            colNew.setCellValueFactory(param -> new ReadOnlyObjectWrapper<>(param.getValue().getProperties().getOrDefault(key, null)));
            colNew.setContextMenu(new ContextMenu());
            colNew.getContextMenu().getItems().add(new ActiveMenuItem("Remove Column", event1 -> {
                tblNodes.getColumns().remove(colNew);
            }));
            tblNodes.getColumns().add(colNew);
            cbFields.getSelectionModel().clearSelection();
        });

        final Pane paneSpacer = new Pane();
        HBox.setHgrow(paneSpacer, Priority.ALWAYS);

        final Button btnExportNodesToCsv = new Button("Export...");
        btnExportNodesToCsv.setOnAction(event -> {
            final File fileDestination = Reports.this.chooserExport.showSaveDialog(Reports.this.getScene().getWindow());
            if(fileDestination != null) {
                Csv.fromTable(tblNodes, Paths.get(fileDestination.getAbsoluteFile().toString()));
            }
        });

        final HBox ctrNodesToolbar = new HBox();
        ctrNodesToolbar.setSpacing(4.0);
        ctrNodesToolbar.getChildren().addAll(cbFields, btnAddField, paneSpacer, btnExportNodesToCsv);

        containerNodes.getChildren().addAll(ctrNodesToolbar, tblNodes);
        containerNodes.prefWidthProperty().bind(this.container.widthProperty());
        containerNodes.prefHeightProperty().bind(this.container.heightProperty());

        this.content.put(ReportModes.NODES, containerNodes);

        final TreeItem<Object> treeNodes = new SelectableTreeItem<>("Nodes", event -> {
            setMode(null);
        });
        final TreeItem<Object> treeNodesAll = new SelectableTreeItem<>("All", event -> {
            lstNodes.setPredicate(predNodesAll);
            setMode(ReportModes.NODES);
        });
        final TreeItem<Object> treeNodesSource = new SelectableTreeItem<>("Source", event -> {
            lstNodes.setPredicate(predNodesSource);
            setMode(ReportModes.NODES);
        });
        final TreeItem<Object> treeNodesDestination = new SelectableTreeItem<>("Destination", event -> {
            lstNodes.setPredicate(predNodesDest);
            setMode(ReportModes.NODES);
        });
        final TreeItem<Object> treeNodesBoth = new SelectableTreeItem<>("Both", event -> {
            lstNodes.setPredicate(predNodesBoth);
            setMode(ReportModes.NODES);
        });

        treeNodes.getChildren().addAll(treeNodesAll, treeNodesSource, treeNodesDestination, treeNodesBoth);
        treeNodes.setExpanded(true);
        rootTree.getChildren().add(treeNodes);
    }
    private void initConnectionsReport() {
        //TODO: Connections Report with endpoint granularity (Will be of PacketList?)
        final VBox containerConnections = new VBox();

        final TableView<GraphLogicalEdge> tblConnections = new TableView<>();
        final TableColumn<GraphLogicalEdge, LogicalAddress> colSource = new TableColumn<>("Source");
        colSource.setCellValueFactory(param -> new ReadOnlyObjectWrapper<>(param.getValue().getSource().getVertex().getLogicalAddress()));
        final TableColumn<GraphLogicalEdge, LogicalAddress> colDestination = new TableColumn<>("Destination");
        colDestination.setCellValueFactory(param -> new ReadOnlyObjectWrapper<>(param.getValue().getDestination().getVertex().getLogicalAddress()));
        final TableColumn<GraphLogicalEdge, Long> colTrafficBytes = new TableColumn<>("Traffic (Bytes)");
        colTrafficBytes.setCellValueFactory(param -> param.getValue().totalPacketSizeProperty().asObject());
        final TableColumn<GraphLogicalEdge, Long> colTrafficPackets = new TableColumn<>("Traffic (Packets)");
        colTrafficPackets.setCellValueFactory(param -> param.getValue().totalPacketCountProperty().asObject());
        final TableColumn<GraphLogicalEdge, Long> colSentBytes = new TableColumn<>("Bytes Sent");
        colSentBytes.setCellValueFactory(param -> param.getValue().sentPacketSizeProperty().asObject());
        final TableColumn<GraphLogicalEdge, Long> colRecvBytes = new TableColumn<>("Bytes Received");
        colRecvBytes.setCellValueFactory(param -> param.getValue().recvPacketSizeProperty().asObject());
        final TableColumn<GraphLogicalEdge, Long> colSentPackets = new TableColumn<>("Packets Sent");
        colSentPackets.setCellValueFactory(param -> param.getValue().sentPacketCountProperty().asObject());
        final TableColumn<GraphLogicalEdge, Long> colRecvPackets = new TableColumn<>("Packets Received");
        colRecvPackets.setCellValueFactory(param -> param.getValue().recvPacketCountProperty().asObject());

        final TableColumn<GraphLogicalEdge, String> colProtocols = new TableColumn<>("Protocols");
        colProtocols.setCellValueFactory(param -> new ProtocolSetExpression(param.getValue().protocolsProperty()));
        tblConnections.getColumns().addAll(colSource, colDestination, colTrafficBytes, colTrafficPackets, colProtocols, colSentBytes, colRecvBytes, colSentPackets, colRecvPackets);
        tblConnections.setItems(this.observableEdges);
        tblConnections.prefWidthProperty().bind(containerConnections.widthProperty());
        tblConnections.prefHeightProperty().bind(containerConnections.heightProperty());

        final Pane paneSpacer = new Pane();
        HBox.setHgrow(paneSpacer, Priority.ALWAYS);

        final Button btnExportConnectionsToCsv = new Button("Export...");
        btnExportConnectionsToCsv.setOnAction(event -> {
            final File fileDestination = Reports.this.chooserExport.showSaveDialog(Reports.this.getScene().getWindow());
            if(fileDestination != null) {
                Csv.fromTable(tblConnections, Paths.get(fileDestination.getAbsoluteFile().toString()));
            }
        });

        final HBox ctrNodesToolbar = new HBox();
        ctrNodesToolbar.setSpacing(4.0);
        ctrNodesToolbar.getChildren().addAll(paneSpacer, btnExportConnectionsToCsv);

        containerConnections.getChildren().addAll(ctrNodesToolbar, tblConnections);
        containerConnections.prefWidthProperty().bind(this.container.widthProperty());
        containerConnections.prefHeightProperty().bind(this.container.heightProperty());

        this.content.put(ReportModes.CONNECTIONS, containerConnections);

        final TreeItem<Object> treeConnections = new SelectableTreeItem<>("Connections", event -> {
            setMode(ReportModes.CONNECTIONS);
        });
        rootTree.getChildren().add(treeConnections);
    }
    private void initDirectionalConnectionsReport() {
        //TODO: Connections Report with endpoint granularity (Will be of PacketList?)
        final VBox containerConnections = new VBox();

        final TableView<DirectionalEdge> tblConnections = new TableView<>();
        final TableColumn<DirectionalEdge, LogicalAddress> colSource = new TableColumn<>("Source");
        colSource.setCellValueFactory(param -> param.getValue().sourceProperty());
        final TableColumn<DirectionalEdge, LogicalAddress> colDestination = new TableColumn<>("Destination");
        colDestination.setCellValueFactory(param -> param.getValue().destinationProperty());

        final TableColumn<DirectionalEdge, Long> colSentBytes = new TableColumn<>("Bytes Sent");
        colSentBytes.setCellValueFactory(param -> param.getValue().packetSizeProperty());
        final TableColumn<DirectionalEdge, Long> colSentPackets = new TableColumn<>("Packets Sent");
        colSentPackets.setCellValueFactory(param -> param.getValue().packetCountProperty());

        //TODO: Directional protocol tracking
        //final TableColumn<GraphLogicalEdge, String> colProtocols = new TableColumn<>("Protocols");
        //colProtocols.setCellValueFactory(param -> new ProtocolSetExpression(param.getValue().protocolsProperty()));
        tblConnections.getColumns().addAll(colSource, colDestination, colSentBytes, colSentPackets);
        tblConnections.setItems(this.directionalEdges);
        tblConnections.prefWidthProperty().bind(containerConnections.widthProperty());
        tblConnections.prefHeightProperty().bind(containerConnections.heightProperty());

        final Pane paneSpacer = new Pane();
        HBox.setHgrow(paneSpacer, Priority.ALWAYS);

        final Button btnExportConnectionsToCsv = new Button("Export...");
        btnExportConnectionsToCsv.setOnAction(event -> {
            final File fileDestination = Reports.this.chooserExport.showSaveDialog(Reports.this.getScene().getWindow());
            if(fileDestination != null) {
                Csv.fromTable(tblConnections, Paths.get(fileDestination.getAbsoluteFile().toString()));
            }
        });

        final HBox ctrNodesToolbar = new HBox();
        ctrNodesToolbar.setSpacing(4.0);
        ctrNodesToolbar.getChildren().addAll(paneSpacer, btnExportConnectionsToCsv);

        containerConnections.getChildren().addAll(ctrNodesToolbar, tblConnections);
        containerConnections.prefWidthProperty().bind(this.container.widthProperty());
        containerConnections.prefHeightProperty().bind(this.container.heightProperty());

        this.content.put(ReportModes.DIRECTIONAL_CONNECTIONS, containerConnections);

        final TreeItem<Object> treeConnections = new SelectableTreeItem<>("Directed Connections", event -> {
            Reports.this.setMode(ReportModes.DIRECTIONAL_CONNECTIONS);
        });
        rootTree.getChildren().add(treeConnections);
    }

    private void initIntergroupConnections() {
        final FilteredList<DirectionalEdge> listIntergroupEdges = new FilteredList<>(this.directionalEdges);
        final ComboBox<String> cbGroupBy = new ComboBox<>();
        cbGroupBy.setOnShowing(event -> {
            // Enumerate available columns
            final List<String> fields = this.observableVertices.stream().flatMap(vertex -> vertex.getProperties().keySet().stream()).distinct().sorted().collect(Collectors.toList());
            cbGroupBy.getItems().retainAll(fields);
            fields.removeAll(cbGroupBy.getItems());
            cbGroupBy.getItems().addAll(fields);
        });
        cbGroupBy.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            if(newValue == null) {
                //When there is no grouping, then we treat every element as its own group, so every connection spans groups.
                listIntergroupEdges.setPredicate(null);
            } else {
                listIntergroupEdges.setPredicate(edge -> {
                    final Set<Property<?>> propertiesSource = edge.getEdge().getSource().getProperties().get(newValue);
                    final Set<Property<?>> propertiesDestination = edge.getEdge().getDestination().getProperties().get(newValue);

                    if(propertiesSource == null) {
                        return propertiesDestination != null;
                    }
                    if(propertiesDestination == null) {
                        return true;
                    }
                    return propertiesSource.size() != propertiesDestination.size() || !propertiesSource.containsAll(propertiesDestination);
                });
            }
        });

        final VBox containerConnections = new VBox();

        final TableView<DirectionalEdge> tblConnections = new TableView<>();
        final TableColumn<DirectionalEdge, LogicalAddress> colSource = new TableColumn<>("Source");
        colSource.setCellValueFactory(param -> param.getValue().sourceProperty());
        final TableColumn<DirectionalEdge, LogicalAddress> colDestination = new TableColumn<>("Destination");
        colDestination.setCellValueFactory(param -> param.getValue().destinationProperty());
        final TableColumn<DirectionalEdge, String> colSourceGroup = new TableColumn<>("Source Group(s)");
        colSourceGroup.setCellValueFactory(param -> param.getValue().buildSourceGroupProperty(cbGroupBy.getSelectionModel().selectedItemProperty()));
        final TableColumn<DirectionalEdge, String> colDestinationGroup = new TableColumn<>("Destination Group(s)");
        colDestinationGroup.setCellValueFactory(param -> param.getValue().buildDestinationGroupProperty(cbGroupBy.getSelectionModel().selectedItemProperty()));

        final TableColumn<DirectionalEdge, Long> colSentBytes = new TableColumn<>("Bytes Sent");
        colSentBytes.setCellValueFactory(param -> param.getValue().packetSizeProperty());
        final TableColumn<DirectionalEdge, Long> colSentPackets = new TableColumn<>("Packets Sent");
        colSentPackets.setCellValueFactory(param -> param.getValue().packetCountProperty());

        //TODO: Directional protocol tracking
        //final TableColumn<GraphLogicalEdge, String> colProtocols = new TableColumn<>("Protocols");
        //colProtocols.setCellValueFactory(param -> new ProtocolSetExpression(param.getValue().protocolsProperty()));
        tblConnections.getColumns().addAll(colSource, colDestination, colSourceGroup, colDestinationGroup, colSentBytes, colSentPackets);
        tblConnections.setItems(listIntergroupEdges);
        tblConnections.prefWidthProperty().bind(containerConnections.widthProperty());
        tblConnections.prefHeightProperty().bind(containerConnections.heightProperty());

        final Pane paneSpacer = new Pane();
        HBox.setHgrow(paneSpacer, Priority.ALWAYS);

        final Button btnExportConnectionsToCsv = new Button("Export...");
        btnExportConnectionsToCsv.setOnAction(event -> {
            final File fileDestination = Reports.this.chooserExport.showSaveDialog(Reports.this.getScene().getWindow());
            if(fileDestination != null) {
                Csv.fromTable(tblConnections, Paths.get(fileDestination.getAbsoluteFile().toString()));
            }
        });

        final HBox ctrNodesToolbar = new HBox();
        ctrNodesToolbar.setSpacing(4.0);
        ctrNodesToolbar.getChildren().addAll(cbGroupBy, paneSpacer, btnExportConnectionsToCsv);

        containerConnections.getChildren().addAll(ctrNodesToolbar, tblConnections);
        containerConnections.prefWidthProperty().bind(this.container.widthProperty());
        containerConnections.prefHeightProperty().bind(this.container.heightProperty());

        this.content.put(ReportModes.INTERGROUP_CONNECTIONS, containerConnections);
        final TreeItem<Object> treeIntergroupConnections = new SelectableTreeItem<>("Intergroup Connections", event -> {
            Reports.this.setMode(ReportModes.INTERGROUP_CONNECTIONS);
        });

        rootTree.getChildren().addAll(treeIntergroupConnections);
    }

    private void initOneToManyReports() {
        final VBox containerOneToMany = new VBox();

        final RevalidatingFilteredList<GraphLogicalVertex> lstVertices = new RevalidatingFilteredList<>(this.observableVertices);
        final Predicate<GraphLogicalVertex> predMultipleLogicalPerHardware = graphLogicalVertex -> {
            return this.observableVertices.stream().filter(vertex -> vertex.getVertex().getHardwareVertex().equals(graphLogicalVertex.getVertex().getHardwareVertex())).count() > 1;
        };
        final Predicate<GraphLogicalVertex> predMultipleHardwarePerLogical = graphLogicalVertex -> {
            return this.observableVertices.stream().filter(vertex -> vertex.getRootLogicalAddressMapping().getLogicalAddress().equals(graphLogicalVertex.getRootLogicalAddressMapping().getLogicalAddress())).count() > 1;
        };

        final TableView<GraphLogicalVertex> tblOneToMany = new TableView<>();
        tblOneToMany.setItems(lstVertices.getComputedList());
        tblOneToMany.prefWidthProperty().bind(containerOneToMany.widthProperty());
        tblOneToMany.prefHeightProperty().bind(containerOneToMany.heightProperty());
        final TableColumn<GraphLogicalVertex, HardwareAddress> colHardware = new TableColumn<>("Hardware Address");
        colHardware.setCellValueFactory(param -> new ReadOnlyObjectWrapper<>(param.getValue().getVertex().getHardwareVertex().getAddress()));
        final TableColumn<GraphLogicalVertex, LogicalAddress> colLogical = new TableColumn<>("Logical Address");
        colLogical.setCellValueFactory(param -> new ReadOnlyObjectWrapper<>(param.getValue().getVertex().getLogicalAddress()));
        tblOneToMany.getColumns().addAll(colHardware, colLogical);

        // Add interface for adding columns to containerNodes
        final ComboBox<String> cbFields = new ComboBox<>();
        final Button btnAddField = new Button("Add");
        cbFields.setOnShowing(event -> {
            // Enumerate available columns
            final List<String> fields = this.observableVertices.stream().flatMap(vertex -> vertex.getProperties().keySet().stream()).distinct().sorted().collect(Collectors.toList());
            cbFields.getItems().retainAll(fields);
            fields.removeAll(cbFields.getItems());
            cbFields.getItems().addAll(fields);
        });
        btnAddField.disableProperty().bind(cbFields.getSelectionModel().selectedItemProperty().isNull());
        btnAddField.setOnAction(event -> {
            final String key = cbFields.getSelectionModel().getSelectedItem();
            if(key == null || key.isEmpty()) {
                //Ignore it
                return;
            }

            final TableColumn<GraphLogicalVertex, Object> colNew = new TableColumn<>(key);
            colNew.setCellValueFactory(param -> new ReadOnlyObjectWrapper<>(param.getValue().getProperties().getOrDefault(key, null)));
            colNew.setContextMenu(new ContextMenu());
            colNew.getContextMenu().getItems().add(new ActiveMenuItem("Remove Column", event1 -> {
                tblOneToMany.getColumns().remove(colNew);
            }));
            tblOneToMany.getColumns().add(colNew);
            cbFields.getSelectionModel().clearSelection();
        });

        final Pane paneSpacer = new Pane();
        HBox.setHgrow(paneSpacer, Priority.ALWAYS);

        final Button btnExportNodesToCsv = new Button("Export...");
        btnExportNodesToCsv.setOnAction(event -> {
            final File fileDestination = Reports.this.chooserExport.showSaveDialog(Reports.this.getScene().getWindow());
            if(fileDestination != null) {
                Csv.fromTable(tblOneToMany, Paths.get(fileDestination.getAbsoluteFile().toString()));
            }
        });

        final HBox ctrNodesToolbar = new HBox();
        ctrNodesToolbar.setSpacing(4.0);
        ctrNodesToolbar.getChildren().addAll(cbFields, btnAddField, paneSpacer, btnExportNodesToCsv);

        containerOneToMany.getChildren().addAll(ctrNodesToolbar, tblOneToMany);
        containerOneToMany.prefWidthProperty().bind(this.container.widthProperty());
        containerOneToMany.prefHeightProperty().bind(this.container.heightProperty());

        this.content.put(ReportModes.MULTIPLE_LOGICAL_PER_HARDWARE, containerOneToMany);
        this.content.put(ReportModes.MULTIPLE_HARDWARE_PER_LOGICAL, containerOneToMany);

        final TreeItem<Object> treeOneToMany = new SelectableTreeItem<>("One-to-Many Relationships", event -> {
            setMode(null);
        });
        final TreeItem<Object> treeOneHardwareWithMultipleLogical = new SelectableTreeItem<>("Hardware with multiple Logical", event -> {
            lstVertices.setPredicate(predMultipleLogicalPerHardware);
            setMode(ReportModes.MULTIPLE_LOGICAL_PER_HARDWARE);
        });
        final TreeItem<Object> treeOneLogicalWithMultipleHardware = new SelectableTreeItem<>("Logical with multiple Hardware", event -> {
            lstVertices.setPredicate(predMultipleHardwarePerLogical);
            setMode(ReportModes.MULTIPLE_HARDWARE_PER_LOGICAL);
        });
        treeOneToMany.getChildren().addAll(treeOneHardwareWithMultipleLogical, treeOneLogicalWithMultipleHardware);
        treeOneToMany.setExpanded(true);
        rootTree.getChildren().add(treeOneToMany);
    }

    protected void setMode(ReportModes mode) {
        if(this.mode == mode) {
            // No change
        } else if(mode == null) {
            //Adding a null child is bad[citation needed], so we special case the null condition.
            this.container.getChildren().clear();

            this.mode = null;
        } else {
            this.container.getChildren().clear();
            this.container.getChildren().add(this.content.get(mode));

            this.mode = mode;
        }
    }

    public TreeItem<Object> getTreeRoot() {
        return this.rootTree;
    }
}
