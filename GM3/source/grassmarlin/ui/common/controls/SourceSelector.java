package grassmarlin.ui.common.controls;

import com.sun.istack.internal.NotNull;
import grassmarlin.plugins.internal.livepcap.LivePcapImport;
import grassmarlin.plugins.internal.livepcap.PcapEngine;
import grassmarlin.ui.sdi.SingleDocumentState;
import javafx.beans.value.ObservableObjectValue;
import javafx.event.ActionEvent;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.layout.HBox;

public class SourceSelector extends HBox{
    private final ObservableObjectValue<SingleDocumentState> stateProperty;
    private final ChoiceBox<PcapEngine.Device> cbDevices;
    private final Button btnStart;
    private final Button btnStop;
    private PcapEngine engine;

    public SourceSelector(@NotNull final PcapEngine engine, @NotNull final ObservableObjectValue<SingleDocumentState> stateProperty) {
        this.stateProperty = stateProperty;
        this.cbDevices = new ChoiceBox<>(engine == null ? null : engine.getDeviceList());
        this.btnStart = new Button("Start");
        this.btnStop = new Button("Stop");

        initComponents();

        setEngine(engine);
    }

    private void initComponents() {
        btnStart.setOnAction(this::handle_Start);
        btnStop.setOnAction(this::handle_Stop);
        if(!cbDevices.getItems().isEmpty()) {
            this.cbDevices.getSelectionModel().select(0);
        }

        this.getChildren().addAll(cbDevices, btnStart, btnStop);
    }

    private void handle_Start(ActionEvent event) {
        final SingleDocumentState state = this.stateProperty.get();
        if(state != null) {
            state.getSession().processImport(new LivePcapImport(state.getSession().getDefaultPipelineEntry(), this.cbDevices.getValue()));
        }
    }
    private void handle_Stop(ActionEvent event) {
        this.engine.stop();
    }

    public PcapEngine getEngine() {
        return this.engine;
    }

    public void setEngine(@NotNull final PcapEngine engine) {
        if(engine == null) {
            throw new IllegalArgumentException("engine cannot be null.");
        } else {
            this.engine = engine;

            this.cbDevices.setItems(engine.getDeviceList());
            this.disableProperty().bind(engine.pcapAvailableProperty().not());
            this.btnStart.disableProperty().bind(engine.pcapRunningProperty().or(this.cbDevices.valueProperty().isNull()));
            this.btnStop.disableProperty().bind(engine.pcapRunningProperty().not());
        }
    }
}
