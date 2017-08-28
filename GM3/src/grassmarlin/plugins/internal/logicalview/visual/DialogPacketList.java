package grassmarlin.plugins.internal.logicalview.visual;

import grassmarlin.RuntimeConfiguration;
import grassmarlin.plugins.internal.logicalview.FilteredLogicalGraph;
import grassmarlin.plugins.internal.logicalview.GraphLogicalEdge;
import grassmarlin.plugins.internal.logicalview.GraphLogicalVertex;
import grassmarlin.plugins.internal.logicalview.Protocols;
import grassmarlin.session.HardwareAddress;
import grassmarlin.session.IAddress;
import grassmarlin.session.LogicalAddress;
import grassmarlin.session.logicaladdresses.IHasPort;
import grassmarlin.session.pipeline.LogicalAddressMapping;
import grassmarlin.ui.common.controls.Chart;
import grassmarlin.ui.common.menu.ActiveMenuItem;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.geometry.Orientation;
import javafx.scene.control.*;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.stream.Collectors;

public class DialogPacketList extends Dialog<ButtonType> {
    private final TableView<GraphLogicalEdge.PacketList.PacketMetadata> table;
    private final PacketChartWrapper chart;

    public DialogPacketList() {
        this.table = new TableView<>();
        this.chart = new PacketChartWrapper(new ChartPacketBytesOverTime());

        initComponents();
    }

    private void initComponents() {
        this.setResizable(true);
        RuntimeConfiguration.setIcons(this);

        final TableColumn<GraphLogicalEdge.PacketList.PacketMetadata, ZonedDateTime> colTimestamp = new TableColumn<>("Timestamp");
        colTimestamp.setCellValueFactory(param -> new ReadOnlyObjectWrapper<>(Instant.ofEpochMilli(param.getValue().getTime()).atZone(ZoneId.of("Z"))));
        final TableColumn<GraphLogicalEdge.PacketList.PacketMetadata, Object> colSourceAddress = new TableColumn<>("Source Address");
        colSourceAddress.setCellValueFactory(param -> {
            final IAddress addr = param.getValue().getSourceAddress();
            if(addr instanceof LogicalAddressMapping) {
                final LogicalAddress<?> addrLogical = ((LogicalAddressMapping)addr).getLogicalAddress();
                if(addrLogical instanceof IHasPort) {
                    return new ReadOnlyObjectWrapper<>(((IHasPort)addrLogical).getAddressWithoutPort());
                } else {
                    return new ReadOnlyObjectWrapper<>(addrLogical);
                }
            } else if(addr instanceof HardwareAddress) {
                return new ReadOnlyObjectWrapper<>(addr);
            } else {
                return null;
            }
        });
        final TableColumn<GraphLogicalEdge.PacketList.PacketMetadata, Integer> colSourcePort = new TableColumn<>("Source Port");
        colSourcePort.setCellValueFactory(param -> {
            final IAddress addr = param.getValue().getSourceAddress();
            if(addr instanceof LogicalAddressMapping) {
                final LogicalAddress<?> addrLogical = ((LogicalAddressMapping) addr).getLogicalAddress();
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
            } else {
                return null;
            }
        });
        final TableColumn<GraphLogicalEdge.PacketList.PacketMetadata, Object> colDestinationAddress = new TableColumn<>("Destination Address");
        colDestinationAddress.setCellValueFactory(param -> {
            final IAddress addr = param.getValue().getDestinationAddress();
            if(addr instanceof LogicalAddressMapping) {
                final LogicalAddress<?> addrLogical = ((LogicalAddressMapping)addr).getLogicalAddress();
                if(addrLogical instanceof IHasPort) {
                    return new ReadOnlyObjectWrapper<>(((IHasPort)addrLogical).getAddressWithoutPort());
                } else {
                    return new ReadOnlyObjectWrapper<>(addrLogical);
                }
            } else if(addr instanceof HardwareAddress) {
                return new ReadOnlyObjectWrapper<>(addr);
            } else {
                return null;
            }
        });
        final TableColumn<GraphLogicalEdge.PacketList.PacketMetadata, Integer> colDestinationPort = new TableColumn<>("Destination Port");
        colDestinationPort.setCellValueFactory(param -> {
            final IAddress addr = param.getValue().getDestinationAddress();
            if(addr instanceof LogicalAddressMapping) {
                final LogicalAddress<?> addrLogical = ((LogicalAddressMapping) addr).getLogicalAddress();
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
            } else {
                return null;
            }
        });
        final TableColumn<GraphLogicalEdge.PacketList.PacketMetadata, String> colSourceFile = new TableColumn<>("Source File");
        colSourceFile.setCellValueFactory(param -> new ReadOnlyObjectWrapper<>(param.getValue().getFile()));
        final TableColumn<GraphLogicalEdge.PacketList.PacketMetadata, Long> colFrameNumber = new TableColumn<>("Frame");
        colFrameNumber.setCellValueFactory(param -> new ReadOnlyObjectWrapper<>(param.getValue().getFrame()));
        final TableColumn<GraphLogicalEdge.PacketList.PacketMetadata, String> colProtocol = new TableColumn<>("Protocol");
        colProtocol.setCellValueFactory(param -> new ReadOnlyObjectWrapper<>(Protocols.toString(param.getValue().getTransportProtocol())));
        final TableColumn<GraphLogicalEdge.PacketList.PacketMetadata, Long> colSize = new TableColumn<>("Size");
        colSize.setCellValueFactory(param -> new ReadOnlyObjectWrapper<>(param.getValue().getSize()));

        table.getColumns().addAll(colTimestamp, colSourceAddress, colSourcePort, colDestinationAddress, colDestinationPort, colProtocol, colSize, colSourceFile, colFrameNumber);
        final ContextMenu menuTable = new ContextMenu();
        menuTable.getItems().add(new ActiveMenuItem("Open in Wireshark", action -> {
            GraphLogicalEdge.PacketList.PacketMetadata line = table.getSelectionModel().getSelectedItem();
            if(line != null) {
                RuntimeConfiguration.openPcapFile(line.getFile(), line.getFrame());
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

    public void setContent(final FilteredLogicalGraph graph, final GraphLogicalVertex endpoint) {
        this.setTitle("Packets involving " + endpoint.getRootLogicalAddressMapping());

        this.table.getItems().clear();
        //TODO: Offload this to a worker thread

        this.table.getItems().addAll(graph.getEdgesForEndpoint(endpoint).stream().flatMap(edge -> edge.getPacketLists().stream()).flatMap(packetlist -> packetlist.getPackets().stream()).sorted((o1, o2) -> Long.compare(o1.getTime(), o2.getTime())).collect(Collectors.toList()));
        this.chart.clearSeries();
        //Add a series for each endpoint pair
        //The hash code for a GraphLogicalEdge is the hash code for the underlying Edge, which is, by design, bidirectional (a reverse-direction edge will have the same hash code as the original).  This should sort bidirectional pairs to be adjacent in the series list.
        //TODO: Sort the edges
        for(GraphLogicalEdge edge : graph.getEdgesForEndpoint(endpoint)) {
            final Chart.Series<GraphLogicalEdge.PacketList.PacketMetadata> series = new Chart.Series<>(String.format("%s -> %s", edge.getSource().getVertex().getLogicalAddress(), edge.getDestination().getVertex().getLogicalAddress()));
            series.getData().addAll(edge.getPacketLists().stream().flatMap(list -> list.getPackets().stream()).collect(Collectors.toList()));
            this.chart.addSeries(series);
        }
    }
}
