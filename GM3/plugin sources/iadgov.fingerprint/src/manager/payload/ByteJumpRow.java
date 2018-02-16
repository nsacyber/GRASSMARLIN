package iadgov.fingerprint.manager.payload;

import core.fingerprint3.AndThen;
import core.fingerprint3.ByteJumpFunction;
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
import java.util.regex.Pattern;

public class ByteJumpRow extends OpRow {

    private static final String calcRegex = "((\\d+)|x)([-+/*]((\\d+)|x))*";

    private String calc;
    private Pattern calcPattern;
    private int offset;
    private boolean relative;
    private Endian endian;
    private int bytes;

    public ByteJumpRow() {
        this(null);
    }

    public ByteJumpRow(ByteJumpFunction jump) {
        super(PayloadItem.OpType.BYTE_JUMP);
        calcPattern = Pattern.compile(calcRegex);

        if (jump != null) {
            this.calc = jump.getCalc();
            this.offset = jump.getOffset() != null ? jump.getOffset() : DEFAULT_OFFSET;
            this.relative = jump.isRelative();
            this.endian = jump.getEndian() != null ? Endian.valueOf(jump.getEndian()) : Endian.getDefault();
            this.bytes = jump.getBytes();

            if (jump.getAndThen() != null && jump.getAndThen().getMatchOrByteTestOrIsDataAt().size() > 0) {
                jump.getAndThen().getMatchOrByteTestOrIsDataAt().forEach(op -> {
                    OpRow childRow = OpRowFactory.get(op);
                    if (childRow != null) {
                        this.addChild(childRow);
                    }
                });
            }
        } else {
            this.calc = "";
            this.offset = DEFAULT_OFFSET;
            this.endian = Endian.getDefault();
            this.bytes = DEFAULT_BYTES;
            this.relative = DEFAULT_RELATIVE;
        }

        this.getChildren().add(new EmptyRow());
    }

    @Override
    public HBox getInput() {
        HBox inputBox = new HBox(4);
        inputBox.setAlignment(Pos.CENTER_LEFT);

        HBox calcBox = new HBox(2);
        calcBox.setAlignment(Pos.CENTER_LEFT);
        Label calcLabel = new Label("Calc:");
        TextField calcField = new TextField(this.calc);
        calcField.focusedProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue) {
                String calcText = calcField.getText() != null ? calcField.getText().replaceAll("\\s", "") : null;
                // the != check is checking for if they are both null
                if (this.calc != calcText) {
                    if (calcText != null && !calcText.equals(this.calc)) {
                        if (calcPattern.matcher(calcText).matches() || calcText.isEmpty() || calcText == null) {
                            this.calc = calcText;
                            update();
                        } else {
                            Platform.runLater(calcField::requestFocus);
                        }
                    } else if (calcText == null) {
                        this.calc = null;
                        update();
                    }
                }
            }
        });
        calcBox.getChildren().addAll(calcLabel, calcField);

        Tooltip calcTip = new Tooltip("The calculation, if any, that needs to be done using the extracted value to get the byte value to jump to. (Use 'x' in place of the extracted value)");
        Tooltip.install(calcLabel, calcTip);
        Tooltip.install(calcField, calcTip);

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

        Tooltip relativeTip = new Tooltip("Whether or not the jump will be relative to the current location of the main cursor");
        Tooltip.install(relativeLabel, relativeTip);
        Tooltip.install(relativeChoice, relativeTip);

        HBox endianBox = new HBox(2);
        endianBox.setAlignment(Pos.CENTER_LEFT);
        Label endianLabel = new Label("Endian:");
        ChoiceBox<Endian> endianChoiceBox = new ChoiceBox<>(FXCollections.observableArrayList(Endian.values()));
        endianChoiceBox.setValue(this.endian);
        endianChoiceBox.valueProperty().addListener(observable -> {
            this.endian = endianChoiceBox.getValue();
            update();
        });
        endianBox.getChildren().addAll(endianLabel, endianChoiceBox);

        Tooltip endianTip = new Tooltip("The endianness, either Big(most significant byte first) or Little(most significant byte last), of the extracted value");
        Tooltip.install(endianLabel, endianTip);
        Tooltip.install(endianChoiceBox, endianTip);

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

        Tooltip offsetTip = new Tooltip("The offset at which to extract the value");
        Tooltip.install(offsetLabel, offsetTip);
        Tooltip.install(offsetField, offsetTip);

        HBox bytesBox = new HBox(2);
        bytesBox.setAlignment(Pos.CENTER_LEFT);
        Label bytesLabel = new Label("Bytes:");
        TextField bytesField = new TextField(Integer.toString(this.bytes));
        //size for max value
        bytesField.setPrefColumnCount(2);
        bytesField.textProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue.isEmpty()) {
                bytesField.setText(Integer.toString(DEFAULT_BYTES));
                Platform.runLater(bytesField::selectAll);
            } else {
                try {
                    int temp = Integer.parseInt(bytesField.getText());
                    if (temp < MIN_BYTES || temp > MAX_BYTES) {
                        bytesField.setText(oldValue);
                    } else {
                        this.bytes = temp;
                        update();
                    }
                } catch (NumberFormatException e) {
                    bytesField.setText(oldValue);
                }
            }
        });
        bytesBox.getChildren().addAll(bytesLabel, bytesField);

        Tooltip bytesTip = new Tooltip("The number of bytes that should be extracted");
        Tooltip.install(bytesLabel, bytesTip);
        Tooltip.install(bytesField, bytesTip);



        inputBox.getChildren().addAll(calcBox, offsetBox, relativeBox, endianBox, bytesBox);

        return inputBox;
    }

    @Override
    public Serializable getOperation() {
        ByteJumpFunction jump = factory.createByteJumpFunction();

        jump.setCalc(this.calc);
        jump.setOffset(this.offset);
        jump.setRelative(this.relative);
        jump.setEndian(this.endian.name());
        jump.setBytes(this.bytes);

        if (this.getChildren().size() > 0) {
            if (!(this.getChildren().get(0) instanceof EmptyRow)) {
                AndThen then = factory.createAndThen();
                this.getChildren().forEach(child -> {
                    if (!(child instanceof EmptyRow)) {
                        then.getMatchOrByteTestOrIsDataAt().add(child.getOperation());
                    }
                });
                jump.setAndThen(then);
            }
        }

        return jump;
    }

    @Override
    public ObservableList<PayloadItem.OpType> getAvailableOps() {
        ObservableList<PayloadItem.OpType> opList = FXCollections.observableArrayList(PayloadItem.OpType.values());
        opList.removeAll(PayloadItem.OpType.ALWAYS);

        return opList;
    }
}
