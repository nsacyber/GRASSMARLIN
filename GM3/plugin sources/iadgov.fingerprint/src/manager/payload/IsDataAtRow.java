package iadgov.fingerprint.manager.payload;

import core.fingerprint3.AndThen;
import core.fingerprint3.IsDataAtFunction;
import iadgov.fingerprint.manager.tree.PayloadItem;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;

import java.io.Serializable;

public class IsDataAtRow extends OpRow {

    private int offset;
    private boolean relative;

    public IsDataAtRow() {
        this(null);
    }

    public IsDataAtRow(IsDataAtFunction at) {
        super(PayloadItem.OpType.IS_DATA_AT);

        if (at != null) {
            this.offset = at.getOffset();
            this.relative = at.isRelative();

            if (at.getAndThen() != null && at.getAndThen().getMatchOrByteTestOrIsDataAt().size() > 0) {
                at.getAndThen().getMatchOrByteTestOrIsDataAt().forEach(op -> {
                    OpRow childRow = OpRowFactory.get(op);
                    if (childRow != null) {
                        this.addChild(childRow);
                    }
                });
            }
        } else {
            this.offset = DEFAULT_OFFSET;
            this.relative = DEFAULT_RELATIVE;
        }

        this.getChildren().add(new EmptyRow());
    }

    @Override
    public HBox getInput() {
        HBox inputBox = new HBox(4);
        inputBox.setAlignment(Pos.CENTER_LEFT);

        HBox offsetBox = new HBox(2);
        offsetBox.setAlignment(Pos.CENTER_LEFT);
        Label offsetLabel = new Label("Offset:");
        TextField offsetField = new TextField(Integer.toString(this.offset));
        //size appropriately
        offsetField.setPrefColumnCount(3);
        offsetField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue.isEmpty()) {
                offsetField.setText("0");
                Platform.runLater(offsetField::selectAll);
            } else {
                try {
                    int temp = Integer.parseInt(offsetField.getText());
                    if (temp < MIN_OFFSET || temp > MAX_OFFSET) {
                        offsetField.setText(oldValue);
                    } else {
                        this.offset = temp;
                        update();
                    }
                } catch (NumberFormatException e) {
                    offsetField.setText(oldValue);
                }
            }
        });
        offsetBox.getChildren().addAll(offsetLabel, offsetField);

        Tooltip offsetTip = new Tooltip("The offset at which to check for data");
        Tooltip.install(offsetLabel, offsetTip);
        Tooltip.install(offsetField, offsetTip);

        HBox relativeBox = new HBox(2);
        relativeBox.setAlignment(Pos.CENTER_LEFT);
        Label relativeLabel = new Label("Relative:");
        ChoiceBox<Boolean> relativeChoice = new ChoiceBox<>(FXCollections.observableArrayList(true, false));
        relativeChoice.setValue(this.relative);
        relativeChoice.valueProperty().addListener(observable -> {
            this.relative = relativeChoice.getValue();
            update();
        });
        relativeBox.getChildren().addAll(relativeLabel, relativeChoice);

        Tooltip relativeTip = new Tooltip("Whether the offset is relative to the main cursor position or not");
        Tooltip.install(relativeLabel, relativeTip);
        Tooltip.install(relativeChoice, relativeTip);

        inputBox.getChildren().addAll(offsetBox, relativeBox);

        return inputBox;
    }

    @Override
    public Serializable getOperation() {
        IsDataAtFunction at = factory.createIsDataAtFunction();

        at.setOffset(this.offset);
        at.setRelative(this.relative);

        if (this.getChildren().size() > 0) {
            if (!(this.getChildren().get(0) instanceof EmptyRow)) {
                AndThen then = factory.createAndThen();
                this.getChildren().forEach(child -> {
                    if (!(child instanceof EmptyRow)) {
                        then.getMatchOrByteTestOrIsDataAt().add(child.getOperation());
                    }
                });
                at.setAndThen(then);
            }
        }

        return at;
    }

    @Override
    public ObservableList<PayloadItem.OpType> getAvailableOps() {
        ObservableList<PayloadItem.OpType> opList = FXCollections.observableArrayList(PayloadItem.OpType.values());
        opList.removeAll(PayloadItem.OpType.ALWAYS);

        return opList;
    }
}
