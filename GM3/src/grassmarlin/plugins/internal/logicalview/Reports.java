package grassmarlin.plugins.internal.logicalview;

import com.sun.javafx.collections.ObservableListWrapper;
import grassmarlin.common.ProtocolSetExpression;
import grassmarlin.session.HardwareAddress;
import grassmarlin.session.LogicalAddress;
import grassmarlin.session.pipeline.LogicalAddressMapping;
import grassmarlin.session.serialization.Csv;
import grassmarlin.ui.common.menu.ActiveMenuItem;
import grassmarlin.ui.common.tree.SelectableTreeItem;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.property.ReadOnlyObjectWrapper;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class Reports extends Pane {
    protected enum ReportModes {
        ENDPOINTS,
        NODES,
        CONNECTIONS,
        AGGREGATE_CONNECTIONS,
        INTERGROUP_CONNECTIONS,
        MULTIPLE_LOGICAL_PER_HARDWARE,
        MULTIPLE_HARDWARE_PER_LOGICAL
    }

    private final FilteredLogicalGraph graph;
    private ReportModes mode;
    private final Pane container;
    private final Map<ReportModes, Node> content;
    private final TreeItem<Object> rootTree;
    private final FileChooser chooserExport;

    public Reports(final FilteredLogicalGraph graph) {
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

        initComponents();
    }

    private void initComponents() {
        //TODO: ENDPOINTS

        this.initNodesReport();
        this.initAddressReports();

        //TODO: CONNECTIONS,

        //TODO: INTERGROUP_CONNECTIONS

        final VBox layout = new VBox();
        layout.prefWidthProperty().bind(this.widthProperty());
        layout.prefHeightProperty().bind(this.heightProperty());
        //TODO: Mode Selection
        //TODO: Export Interface
        layout.getChildren().add(this.container);
        getChildren().add(layout);
    }

    private void initNodesReport() {
        this.container.prefWidthProperty().bind(this.widthProperty());
        this.container.prefHeightProperty().bind(this.heightProperty());

        this.initNodesReports();
        this.initConnectionsReport();
        this.initIntergroupConnections();
    }

    private void initNodesReports() {
        final VBox containerNodes = new VBox();

        final Predicate<GraphLogicalVertex> predNodesAll = logicalVertex -> true;
        final Predicate<GraphLogicalVertex> predNodesSource = logicalVertex -> graph.getEdges().stream().anyMatch(edge -> logicalVertex.getVertex().getLogicalAddress().contains(edge.getSource().getVertex().getLogicalAddress()));
        final Predicate<GraphLogicalVertex> predNodesDest = logicalVertex -> graph.getEdges().stream().anyMatch(edge -> logicalVertex.getVertex().getLogicalAddress().contains(edge.getDestination().getVertex().getLogicalAddress()));
        final Predicate<GraphLogicalVertex> predNodesBoth = logicalVertex -> predNodesSource.test(logicalVertex) && predNodesDest.test(logicalVertex);
        final TableView<GraphLogicalVertex> tblNodes = new TableView<>();
        final FilteredList<GraphLogicalVertex> lstNodes = new FilteredList<>(graph.getVertices(), predNodesAll);
        final TableColumn<GraphLogicalVertex, LogicalAddress<?>> colIp = new TableColumn<>("Address");
        colIp.setCellValueFactory(new PropertyValueFactory<>("title"));
        tblNodes.getColumns().addAll(colIp);
        tblNodes.setItems(lstNodes);
        tblNodes.prefWidthProperty().bind(containerNodes.widthProperty());
        tblNodes.prefHeightProperty().bind(containerNodes.heightProperty());

        // Add interface for adding columns to containerNodes
        final ComboBox<String> cbFields = new ComboBox<>();
        final Button btnAddField = new Button("Add");
        cbFields.setOnShowing(event -> {
            // Enumerate available columns
            final List<String> fields = graph.getVertices().stream().flatMap(vertex -> vertex.getProperties().keySet().stream()).distinct().sorted().collect(Collectors.toList());
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

            cbFields.getItems().clear();
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
            //System.out.println(graph.toString());
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
        rootTree.getChildren().add(treeNodes);
    }
    private void initConnectionsReport() {
        //TODO: Connections Report with directional traffic (Will be of {GraphLogicalEdge, direction} tuples, but the GraphLogicalEdge doesn't track directional stats right now)
        //TODO: Connections Report with endpoint granularity (Will be of PacketList?)
        final VBox containerConnections = new VBox();

        final TableView<GraphLogicalEdge> tblConnections = new TableView<>();
        //TODO: The "VisualEndpoint" labeling is intentionally bad; the traffic is not pulled as directional, but directional labels feel more appropriate.  We can proably find something better.
        final TableColumn<GraphLogicalEdge, LogicalAddress<?>> colSource = new TableColumn<>("VisualEndpoint");
        colSource.setCellValueFactory(param -> new ReadOnlyObjectWrapper<>(param.getValue().getSource().getVertex().getLogicalAddress()));
        final TableColumn<GraphLogicalEdge, LogicalAddress<?>> colDestination = new TableColumn<>("VisualEndpoint");
        colDestination.setCellValueFactory(param -> new ReadOnlyObjectWrapper<>(param.getValue().getDestination().getVertex().getLogicalAddress()));
        final TableColumn<GraphLogicalEdge, Long> colTrafficBytes = new TableColumn<>("Traffic (Bytes)");
        colTrafficBytes.setCellValueFactory(param -> param.getValue().totalPacketSizeProperty().asObject());
        final TableColumn<GraphLogicalEdge, Long> colTrafficPackets = new TableColumn<>("Traffic (Packets)");
        colTrafficPackets.setCellValueFactory(param -> param.getValue().totalPacketCountProperty().asObject());

        final TableColumn<GraphLogicalEdge, String> colProtocols = new TableColumn<>("Protocols");
        colProtocols.setCellValueFactory(param -> new ProtocolSetExpression(param.getValue().protocolsProperty()));
        tblConnections.getColumns().addAll(colSource, colDestination, colTrafficBytes, colTrafficPackets, colProtocols);
        tblConnections.setItems(graph.getEdges());
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
    private void initIntergroupConnections() {
        //TODO: Intergroup Connections Report
        //TODO: Allow selection of grouping criteria, show source/target group names (option to remove directionality?)
        final TreeItem<Object> treeIntergroupConnections = new SelectableTreeItem<>("Intergroup Connections", event -> {

        });

        //We're not ready to unleash this yet, so not adding it to the tree.
        //rootTree.getChildren().addAll(treeIntergroupConnections);
    }
    private void initAddressReports() {
        final VBox container = new VBox();

        final TableView<LogicalAddressMapping> tblConnections = new TableView<>();
        //TODO: The "VisualEndpoint" labeling is intentionally bad; the traffic is not pulled as directional, but directional labels feel more appropriate.  We can proably find something better.
        final TableColumn<LogicalAddressMapping, LogicalAddress<?>> colLogical = new TableColumn<>("Logical Address");
        colLogical.setCellValueFactory(param -> new ReadOnlyObjectWrapper<>(param.getValue().getLogicalAddress()));
        final TableColumn<LogicalAddressMapping, HardwareAddress> colHardware = new TableColumn<>("Hardware Address");
        colHardware.setCellValueFactory(param -> new ReadOnlyObjectWrapper<>(param.getValue().getHardwareAddress()));

        final ObservableListWrapper<LogicalAddressMapping> lstMultipleLogicalPerHardware = new ObservableListWrapper<>(new ArrayList<>());
        final ObservableListWrapper<LogicalAddressMapping> lstMultipleHardwarePerLogical = new ObservableListWrapper<>(new ArrayList<>());
        this.graph.getMappings().addListener(new InvalidationListener() {
            @Override
            public void invalidated(Observable observable) {
                final List<LogicalAddressMapping> currentEntriesMultipleLogical = Reports.this.graph.getMappings().stream().filter(mapping -> Reports.this.graph.getMappings().stream().filter(inner -> inner.getHardwareAddress().equals(mapping.getHardwareAddress())).count() > 1).collect(Collectors.toList());
                lstMultipleLogicalPerHardware.retainAll(currentEntriesMultipleLogical);
                currentEntriesMultipleLogical.removeAll(lstMultipleLogicalPerHardware);
                lstMultipleLogicalPerHardware.addAll(currentEntriesMultipleLogical);

                final List<LogicalAddressMapping> currentEntriesMultipleHardware = Reports.this.graph.getMappings().stream().filter(mapping -> Reports.this.graph.getMappings().stream().filter(inner -> inner.getLogicalAddress().equals(mapping.getLogicalAddress())).count() > 1).collect(Collectors.toList());
                lstMultipleHardwarePerLogical.retainAll(currentEntriesMultipleHardware);
                currentEntriesMultipleHardware.removeAll(lstMultipleHardwarePerLogical);
                lstMultipleHardwarePerLogical.addAll(currentEntriesMultipleHardware);
            }
        });

        tblConnections.getColumns().addAll(colLogical, colHardware);
        //TODO: Add support for changing the predicate
        tblConnections.setItems(lstMultipleLogicalPerHardware);
        tblConnections.prefWidthProperty().bind(container.widthProperty());
        tblConnections.prefHeightProperty().bind(container.heightProperty());

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

        container.getChildren().addAll(ctrNodesToolbar, tblConnections);
        container.prefWidthProperty().bind(this.container.widthProperty());
        container.prefHeightProperty().bind(this.container.heightProperty());

        this.content.put(ReportModes.MULTIPLE_LOGICAL_PER_HARDWARE, container);
        this.content.put(ReportModes.MULTIPLE_HARDWARE_PER_LOGICAL, container);

        final TreeItem<Object> treeMultipleIpsForSingleMac = new SelectableTreeItem<>("Hardware Addresses with multiple Logical Addresses", event -> {
            setMode(ReportModes.MULTIPLE_LOGICAL_PER_HARDWARE);
            tblConnections.setItems(lstMultipleLogicalPerHardware);
        });
        final TreeItem<Object> treeMultipleMacsForSingleIp = new SelectableTreeItem<>("Logical Addresses with multiple Hardware Addresses", event -> {
            setMode(ReportModes.MULTIPLE_HARDWARE_PER_LOGICAL);
            tblConnections.setItems(lstMultipleHardwarePerLogical);
        });

        rootTree.getChildren().addAll(treeMultipleIpsForSingleMac, treeMultipleMacsForSingleIp);
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
