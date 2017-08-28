package grassmarlin.ui.common.dialogs.networks;

import com.sun.javafx.collections.ObservableListWrapper;
import grassmarlin.Event;
import grassmarlin.RuntimeConfiguration;
import grassmarlin.plugins.IPlugin;
import grassmarlin.session.LogicalAddress;
import grassmarlin.session.NetworkList;
import grassmarlin.session.pipeline.Network;
import grassmarlin.ui.common.controls.ObjectField;
import grassmarlin.ui.common.menu.ActiveMenuItem;
import javafx.application.Platform;
import javafx.beans.binding.When;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class NetworkDialog extends Dialog {
    public final static String SOURCE_USER = "User";
    private final RuntimeConfiguration config;

    private NetworkList networks = null;
    private final List<IPlugin.HasClassFactory.ClassFactory<? extends LogicalAddress<?>>> logicalAddressFactories;
    private final FilteredList<TableItem> tableItems;
    private final ObservableList<TableItem> allTableItems;

    protected class TableItem {
        private final String source;
        private final LogicalAddress<?> network;
        private final String type;
        private final int confidence;

        public TableItem(final String source, final LogicalAddress<?> network, final int confidence) {
            this.source = source;
            this.network = network;
            this.confidence = confidence;

            final IPlugin.HasClassFactory.ClassFactory factoryForNetwork = NetworkDialog.this.logicalAddressFactories.stream().filter(factory -> factory.getFactoryClass().equals(network.getClass())).findAny().orElse(null);
            this.type = factoryForNetwork.getFactoryName();
        }

        public String getSource() {
            return this.source;
        }
        public LogicalAddress<?> getNetwork() {
            return this.network;
        }
        public String getType() {
            return this.type;
        }
        public int getConfidence() {
            return this.confidence;
        }
        public boolean isEditable() {
            //Yes, we want reference equality.
            return this.source == SOURCE_USER;
        }
    }

    public NetworkDialog(final RuntimeConfiguration config) {
        this.config = config;
        this.logicalAddressFactories =  NetworkDialog.this.config.enumeratePlugins(IPlugin.HasClassFactory.class).stream().flatMap(plugin -> plugin.getClassFactories().stream()).map(factory -> LogicalAddress.class.isAssignableFrom(factory.getFactoryClass()) ? (IPlugin.HasClassFactory.ClassFactory<? extends LogicalAddress<?>>)factory : null).filter(factory -> factory != null).collect(Collectors.toList());
        this.allTableItems = new ObservableListWrapper<>(new ArrayList<>());
        this.tableItems = new FilteredList<>(this.allTableItems, null);

        initComponents();
    }

    private void initComponents() {
        this.setTitle("Manage Networks");
        this.setResizable(true);
        RuntimeConfiguration.setIcons(this);

        final TableView<TableItem> table = new TableView();
        table.setItems(this.tableItems);
        table.setRowFactory(view -> {
            final TableRow<TableItem> row = new TableRow<>();
            final ContextMenu menuRow = new ContextMenu();

            final MenuItem miRemove = new ActiveMenuItem("Remove", event -> {
                NetworkDialog.this.networks.removeNetwork(SOURCE_USER, new Network(row.getItem().getNetwork(), row.getItem().getConfidence()));
            });
            menuRow.getItems().addAll(miRemove);

            row.contextMenuProperty().bind(new When(row.emptyProperty()).then((ContextMenu)null).otherwise(menuRow));
            menuRow.setOnShowing(event -> {
                if(row.getItem() != null) {
                    miRemove.setDisable(!row.getItem().isEditable());
                }
            });
            return row;
        });

        final TableColumn<TableItem, LogicalAddress<?>> colNetwork = new TableColumn<>("Network");
        colNetwork.setCellValueFactory(param -> new ReadOnlyObjectWrapper<>(param.getValue() == null ? null : param.getValue().getNetwork()));

        final TableColumn<TableItem, String> colType = new TableColumn<>("Type");
        colType.setCellValueFactory(param -> new ReadOnlyStringWrapper(param.getValue().getType()));

        final TableColumn<TableItem, String> colSource = new TableColumn<>("Source");
        colSource.setCellValueFactory(param -> new ReadOnlyStringWrapper(param.getValue().getSource()));

        final TableColumn<TableItem, Integer> colConfidence = new TableColumn<>("Confidence");
        colConfidence.setCellValueFactory(param -> new ReadOnlyObjectWrapper<>(param.getValue().getConfidence()));

        table.getColumns().addAll(
                colNetwork,
                colType,
                colSource,
                colConfidence
        );

        final CheckBox ckShowAllNetworks = new CheckBox("Show all Reported Networks");
        ckShowAllNetworks.setSelected(true);
        ckShowAllNetworks.selectedProperty().addListener((observable, oldValue, newValue) -> {
            if(newValue) {
                this.tableItems.setPredicate(null);
            } else {
                this.tableItems.setPredicate(tableItem -> NetworkDialog.this.networks.getCalculatedNetworksCopy().contains(new Network(tableItem.getNetwork(), tableItem.getConfidence())));
            }
        });

        final ObjectField<LogicalAddress<?>> networkEntry = new ObjectField<>(this.config, LogicalAddress.class);
        networkEntry.buttonTextProperty().set("Add");
        networkEntry.onActionProperty().set(event -> {
            final LogicalAddress<?> address = networkEntry.createInstanceFromText();
            networkEntry.clear();
            NetworkDialog.this.networks.addNetwork(SOURCE_USER, new Network(address, 0));
        });

        final VBox layout = new VBox(2.0);
        layout.getChildren().addAll(
                table,
                ckShowAllNetworks,
                networkEntry
        );

        this.getDialogPane().setContent(layout);
        this.getDialogPane().getButtonTypes().addAll(ButtonType.CLOSE);
    }

    public void showAndWait(final NetworkList networks) {
        this.networks = networks;

        this.networks.onNetworkReported.addHandler(this.handlerNetworkReported);

        handleNetworkReported(null, null);
        try {
            this.showAndWait();
        } finally {
            //We're going to reattach the handler next time we try to show this.
            if (this.networks != null) {
                this.networks.onNetworkReported.removeHandler(this.handlerNetworkReported);
            }
        }
    }

    private Event.EventListener<NetworkList.NetworkReportedEventArgs> handlerNetworkReported = this::handleNetworkReported;
    private void handleNetworkReported(final Event<NetworkList.NetworkReportedEventArgs> event, final NetworkList.NetworkReportedEventArgs args) {
        Platform.runLater(() -> {
            if(args == null) {
                //Rebuild All.  this is called explicitly when networks is set and implicitly when a network is removed.
                this.allTableItems.clear();
                this.allTableItems.addAll(
                        this.networks.getAllReportedNetworks().entrySet().stream()
                                .flatMap(entry -> entry.getValue().stream()
                                        .map(network -> new TableItem(entry.getKey(), network.getValue(), network.getConfidence())))
                                .collect(Collectors.toList())
                );
            } else {
                this.allTableItems.add(new TableItem(args.getSource(), args.getNetwork().getValue(), args.getNetwork().getConfidence()));
            }
        });
    }
}
