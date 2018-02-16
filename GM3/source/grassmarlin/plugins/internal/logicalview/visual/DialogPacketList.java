package grassmarlin.plugins.internal.logicalview.visual;

import grassmarlin.Coalesce;
import grassmarlin.RuntimeConfiguration;
import grassmarlin.plugins.internal.logicalview.GraphLogicalEdge;
import grassmarlin.plugins.internal.logicalview.GraphLogicalVertex;
import grassmarlin.plugins.internal.logicalview.LogicalGraph;
import grassmarlin.plugins.internal.logicalview.Protocols;
import grassmarlin.session.LogicalAddress;
import grassmarlin.session.logicaladdresses.IHasPort;
import grassmarlin.ui.common.controls.Chart;
import grassmarlin.ui.common.menu.ActiveMenuItem;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.geometry.Orientation;
import javafx.scene.control.*;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.stream.Collectors;

public class DialogPacketList extends Dialog<ButtonType> {
    private final TableView<GraphLogicalEdge.PacketMetadata> table;
    private final PacketChartWrapper<GraphLogicalEdge.PacketMetadata> chart;

    public DialogPacketList() {
        this.table = new TableView<>();
        this.chart = new PacketChartWrapper(new ChartPacketBytesOverTime());

        initComponents();
    }

    private void initComponents() {
        this.setResizable(true);
        RuntimeConfiguration.setIcons(this);

        final TableColumn<GraphLogicalEdge.PacketMetadata, ZonedDateTime> colTimestamp = new TableColumn<>("Timestamp");
        colTimestamp.setCellValueFactory(param -> new ReadOnlyObjectWrapper<>(Instant.ofEpochMilli(param.getValue().getTime()).atZone(ZoneId.of("Z"))));
        final TableColumn<GraphLogicalEdge.PacketMetadata, Object> colSourceAddress = new TableColumn<>("Source Address");
        colSourceAddress.setCellValueFactory(param -> {
            final LogicalAddress addrLogical = param.getValue().getSourceAddress().getLogicalAddress();
            if(addrLogical instanceof IHasPort) {
                return new ReadOnlyObjectWrapper<>(((IHasPort)addrLogical).getAddressWithoutPort());
            } else {
                return new ReadOnlyObjectWrapper<>(addrLogical);
            }
        });
        final TableColumn<GraphLogicalEdge.PacketMetadata, Integer> colSourcePort = new TableColumn<>("Source Port");
        colSourcePort.setCellValueFactory(param -> {
            final LogicalAddress addrLogical = param.getValue().getSourceAddress().getLogicalAddress();
            if(addrLogical instanceof IHasPort) {
                final int port = ((IHasPort)addrLogical).getPort();
                if(port == -1) {
                    return null;
                } else {
                    return new ReadOnlyObjectWrapper<>(port);
                }
            } else {
                return null;
            }
        });
        final TableColumn<GraphLogicalEdge.PacketMetadata, Object> colDestinationAddress = new TableColumn<>("Destination Address");
        colDestinationAddress.setCellValueFactory(param -> {
            final LogicalAddress addrLogical = param.getValue().getDestinationAddress().getLogicalAddress();
            if(addrLogical instanceof IHasPort) {
                return new ReadOnlyObjectWrapper<>(((IHasPort)addrLogical).getAddressWithoutPort());
            } else {
                return new ReadOnlyObjectWrapper<>(addrLogical);
            }
        });
        final TableColumn<GraphLogicalEdge.PacketMetadata, Integer> colDestinationPort = new TableColumn<>("Destination Port");
        colDestinationPort.setCellValueFactory(param -> {
            final LogicalAddress addrLogical = param.getValue().getDestinationAddress().getLogicalAddress();
            if(addrLogical instanceof IHasPort) {
                final int port = ((IHasPort)addrLogical).getPort();
                if(port == -1) {
                    return null;
                } else {
                    return new ReadOnlyObjectWrapper<>(port);
                }
            } else {
                return null;
            }
        });
        final TableColumn<GraphLogicalEdge.PacketMetadata, String> colSourceFile = new TableColumn<>("Source File");
        colSourceFile.setCellValueFactory(param -> new ReadOnlyObjectWrapper<>(param.getValue().getFile()));
        final TableColumn<GraphLogicalEdge.PacketMetadata, Long> colFrameNumber = new TableColumn<>("Frame");
        colFrameNumber.setCellValueFactory(param -> new ReadOnlyObjectWrapper<>(param.getValue().getFrame()));
        final TableColumn<GraphLogicalEdge.PacketMetadata, String> colProtocol = new TableColumn<>("Protocol");
        colProtocol.setCellValueFactory(param -> new ReadOnlyObjectWrapper<>(Protocols.toString(param.getValue().getTransportProtocol())));
        final TableColumn<GraphLogicalEdge.PacketMetadata, Long> colSize = new TableColumn<>("Size");
        colSize.setCellValueFactory(param -> new ReadOnlyObjectWrapper<>(param.getValue().getSize()));

        table.getColumns().addAll(colTimestamp, colSourceAddress, colSourcePort, colDestinationAddress, colDestinationPort, colProtocol, colSize, colSourceFile, colFrameNumber);
        final ContextMenu menuTable = new ContextMenu();
        menuTable.getItems().add(new ActiveMenuItem("Open in Wireshark", action -> {
            GraphLogicalEdge.PacketMetadata line = table.getSelectionModel().getSelectedItem();
            if(line != null) {
                RuntimeConfiguration.openPcapFile(line.getFile(), Coalesce.of(line.getFrame(), 0L));
            }
        }));
        table.setContextMenu(menuTable);

        final SplitPane layout = new SplitPane();
        layout.setOrientation(Orientation.VERTICAL);
        layout.getItems().add(this.table);
        layout.getItems().add(this.chart);

        this.getDialogPane().setContent(layout);
        this.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
    }

    public void setContent(final LogicalGraph graph, final GraphLogicalVertex endpoint) {
        this.setTitle("Packets involving " + endpoint.getRootLogicalAddressMapping());

        this.table.getItems().clear();
        //TODO: Offload this to a worker thread

        this.table.getItems().addAll(graph.getEdgesForEndpoint(endpoint).stream().flatMap(edge -> {
            final ArrayList<GraphLogicalEdge.PacketMetadata> list = new ArrayList<>();
            edge.getPacketList(list);
            return list.stream();
        }).sorted((o1, o2) -> Long.compare(o1.getTime(), o2.getTime())).collect(Collectors.toList()));
        this.chart.clearSeries();
        //Add a series for each endpoint pair
        for(GraphLogicalEdge edge : graph.getEdgesForEndpoint(endpoint)) {
            final Chart.Series<GraphLogicalEdge.PacketMetadata> series = new Chart.Series<>(String.format("%s -> %s", edge.getSource().getVertex().getLogicalAddress(), edge.getDestination().getVertex().getLogicalAddress()));
            edge.getPacketList(series.getData());
            this.chart.addSeries(series);
        }
    }
}
