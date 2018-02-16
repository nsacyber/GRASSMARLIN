package iadgov.fingerprint.manager.payload;

import core.fingerprint3.Anchor;
import core.fingerprint3.Cursor;
import core.fingerprint3.Position;
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

public class AnchorRow extends OpRow {

    private Cursor cursor;
    private Position position;
    private int offset;

    public AnchorRow() {
        this(null);
    }

    public AnchorRow(Anchor anchor) {
        super(PayloadItem.OpType.ANCHOR);

        if (anchor != null) {
            this.cursor = anchor.getCursor() != null ? anchor.getCursor() : Cursor.MAIN;
            this.position = anchor.getPosition() != null ? anchor.getPosition() : Position.START_OF_PAYLOAD;
            this.offset = anchor.getOffset() != null ? anchor.getOffset() : DEFAULT_OFFSET;
        } else {
            this.cursor = Cursor.MAIN;
            this.position = Position.START_OF_PAYLOAD;
            this.offset = DEFAULT_OFFSET;
        }
    }

    @Override
    public HBox getInput() {
        HBox inputBox = new HBox(4);
        inputBox.setAlignment(Pos.CENTER_LEFT);

        HBox cursorBox = new HBox(2);
        cursorBox.setAlignment(Pos.CENTER_LEFT);
        Label cursorLabel = new Label("Cursor:");
        ChoiceBox<Cursor> cursorChoice = new ChoiceBox<>(FXCollections.observableArrayList(Cursor.values()));
        cursorChoice.setValue(this.cursor);
        cursorChoice.valueProperty().addListener(observable -> {
            this.cursor = cursorChoice.getValue();
            update();
        });
        cursorBox.getChildren().addAll(cursorLabel, cursorChoice);

        Tooltip cursorTip = new Tooltip("Which cursor to anchor");
        Tooltip.install(cursorLabel, cursorTip);
        Tooltip.install(cursorChoice, cursorTip);

        HBox positionBox = new HBox(2);
        positionBox.setAlignment(Pos.CENTER_LEFT);
        Label positionLabel = new Label("Position:");
        ChoiceBox<Position> positionChoice = new ChoiceBox<>(FXCollections.observableArrayList(Position.values()));
        positionChoice.setValue(this.position);
        positionChoice.valueProperty().addListener(observable -> {
            this.position = positionChoice.getValue();
            update();
        });
        positionBox.getChildren().addAll(positionLabel, positionChoice);

        Tooltip positionTip = new Tooltip("Which position to anchor the cursor to");
        Tooltip.install(positionLabel, positionTip);
        Tooltip.install(positionChoice, positionTip);

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

        Tooltip offsetTip = new Tooltip("The Offset from the Position to Anchor the Cursor to");
        Tooltip.install(offsetLabel, offsetTip);
        Tooltip.install(offsetField, offsetTip);

        inputBox.getChildren().addAll(cursorBox, positionBox, offsetBox);

        return inputBox;
    }

    @Override
    public Serializable getOperation() {
        Anchor anchor = factory.createAnchor();

        anchor.setCursor(this.cursor);
        anchor.setPosition(this.position);
        anchor.setOffset(this.offset);

        return anchor;
    }

    @Override
    public ObservableList<PayloadItem.OpType> getAvailableOps() {
        ObservableList<PayloadItem.OpType> opList = FXCollections.observableArrayList(PayloadItem.OpType.values());
        opList.removeAll(PayloadItem.OpType.ALWAYS);

        return opList;
    }
}
