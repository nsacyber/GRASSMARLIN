package iadgov.fingerprint.manager.payload;


import core.fingerprint3.AndThen;
import core.fingerprint3.ContentType;
import core.fingerprint3.MatchFunction;
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
import java.util.ArrayList;
import java.util.List;

public class MatchRow extends OpRow {

    private static final String PATTERN = "PATTERN";
    private static final Endian DEFAULT_ENDIAN = Endian.BIG;
    private static final boolean DEFAULT_NO_CASE = false;
    private static final int MAX_DEPTH = 65535;
    private static final int MIN_DEPTH = 0;
    private static final int DEFAULT_DEPTH = 0;
    private static final boolean DEFAULT_MOVE_CURSORS = true;

    private ContentType contentType;
    private String content;
    private int offset;
    private Endian endian;
    private boolean noCase;
    private int depth;
    private boolean relative;
    private boolean moveCursors;

    private List<String> types;

    public MatchRow() {
        this(null);
    }

    public MatchRow(MatchFunction match) {
        super(PayloadItem.OpType.MATCH);

        if (match != null) {
            this.contentType = match.getContent() != null ? match.getContent().getType() : null;
            this.content = this.contentType != null ? match.getContent().getValue() : match.getPattern();
            this.offset = match.getOffset();
            this.endian = Endian.valueOf(match.getEndian());
            this.noCase = match.isNoCase();
            this.depth = match.getDepth();
            this.relative = match.isRelative();
            this.moveCursors = match.isMoveCursors();

            if (match.getAndThen() != null && match.getAndThen().getMatchOrByteTestOrIsDataAt().size() > 0) {
                match.getAndThen().getMatchOrByteTestOrIsDataAt().forEach(op -> {
                    OpRow childRow = OpRowFactory.get(op);
                    if (childRow != null) {
                        this.addChild(childRow);
                    }
                });
            }
        } else {
            this.offset = DEFAULT_OFFSET;
            this.endian = DEFAULT_ENDIAN;
            this.noCase = DEFAULT_NO_CASE;
            this.depth = DEFAULT_DEPTH;
            this.relative = DEFAULT_RELATIVE;
            this.moveCursors = DEFAULT_MOVE_CURSORS;
        }

        types = new ArrayList<>();
        for (ContentType type : ContentType.values()) {
            types.add(type.name());
        }
        //PATTERN type matches pattern element in xsd
        //All others match the content element
        types.add(PATTERN);

        OpRow empty = new EmptyRow();
        this.addChild(empty);
    }

    @Override
    public ObservableList<PayloadItem.OpType> getAvailableOps() {
        ObservableList<PayloadItem.OpType> ops = FXCollections.observableArrayList(PayloadItem.OpType.values());
        ops.remove(PayloadItem.OpType.ALWAYS);
        if (this.getChildren().stream().filter(row -> row.getType() == PayloadItem.OpType.RETURN).count() > 0) {
            ops.remove(PayloadItem.OpType.RETURN);
        }

        return ops;
    }

    @Override
    public HBox getInput() {
        HBox inputBox = new HBox(4);

        HBox typeBox = new HBox(2);
        typeBox.setAlignment(Pos.CENTER_LEFT);
        Label typeLabel = new Label("Type:");
        HBox typeInputBox = new HBox();
        ChoiceBox<String> typeChoice = new ChoiceBox<>(FXCollections.observableArrayList(types));
        typeChoice.setValue(this.contentType != null ? this.contentType.name() : PATTERN);
        TextField contentField = new TextField(this.content);
        contentField.textProperty().addListener(observable -> {
            this.content = contentField.getText();
            update();
        });
        typeChoice.valueProperty().addListener(change -> {
            if (typeChoice.getValue().equals(PATTERN)) {
                this.contentType = null;
            } else {
                this.contentType = ContentType.valueOf(typeChoice.getValue());
            }

            Platform.runLater(() -> {
                contentField.requestFocus();
                contentField.selectAll();
            });
            update();
        });
        typeInputBox.getChildren().addAll(typeChoice, contentField);
        typeBox.getChildren().addAll(typeLabel, typeInputBox);
        Tooltip typeTip = new Tooltip("The type of content to match");
        Tooltip.install(typeBox, typeTip);
        Tooltip.install(typeChoice, typeTip);
        Tooltip contentTip = new Tooltip("The content to match");
        Tooltip.install(contentField, contentTip);

        HBox noCaseBox = new HBox(2);
        noCaseBox.setAlignment(Pos.CENTER_LEFT);
        Label noCaseLabel = new Label("No Case:");
        ChoiceBox<Boolean> noCaseChoice = new ChoiceBox<>(FXCollections.observableArrayList(true, false));
        noCaseChoice.setDisable(!typeChoice.getValue().equals(ContentType.STRING.name()));
        noCaseChoice.setValue(this.noCase);
        noCaseChoice.valueProperty().addListener(change -> {
            this.noCase = noCaseChoice.getValue();
            update();
        });
        noCaseBox.getChildren().addAll(noCaseLabel, noCaseChoice);
        typeChoice.valueProperty().addListener(observable -> {
            if (typeChoice.getValue().equals(ContentType.STRING.name())) {
                noCaseChoice.setDisable(false);
            } else {
                noCaseChoice.setDisable(true);
            }
        });
        Tooltip caseTip = new Tooltip("Make the string match case insensitive");
        Tooltip.install(noCaseBox, caseTip);
        Tooltip.install(noCaseChoice, caseTip);

//        private boolean moveCursors;

        HBox offsetBox = new HBox(2);
        offsetBox.setAlignment(Pos.CENTER_LEFT);
        Label offsetLabel = new Label("Offset:");
        TextField offsetField = new TextField(Integer.toString(this.offset));
        //sizing to fit the max number for offset
        offsetField.setPrefColumnCount(3);
        offsetField.textProperty().addListener((observable, oldValue, newValue) -> {
            try {
                if (offsetField.getText().isEmpty()) {
                    offsetField.setText(Integer.toString(DEFAULT_OFFSET));
                    Platform.runLater(offsetField::selectAll);
                } else {
                    int newOffset = Integer.parseInt(newValue);
                    if (newOffset < MIN_OFFSET || newOffset > MAX_OFFSET) {
                        offsetField.setText(oldValue);
                    } else {
                        this.offset = newOffset;
                        update();
                    }
                }
            } catch (NumberFormatException e) {
                offsetField.setText(oldValue);
            }
        });
        offsetBox.getChildren().addAll(offsetLabel, offsetField);
        Tooltip offsetTip = new Tooltip("Offset from beginning to start looking for a match");
        Tooltip.install(offsetBox, offsetTip);
        Tooltip.install(offsetField, offsetTip);

        HBox endianBox = new HBox(2);
        endianBox.setAlignment(Pos.CENTER_LEFT);
        Label reverseLabel = new Label("Endian:");
        ChoiceBox<Endian> endianChoice = new ChoiceBox<>(FXCollections.observableArrayList(Endian.values()));
        endianChoice.setValue(this.endian);
        endianChoice.valueProperty().addListener(change -> {
            this.endian = endianChoice.getValue();
            update();
        });
        endianBox.getChildren().addAll(reverseLabel, endianChoice);

        HBox depthBox = new HBox(2);
        depthBox.setAlignment(Pos.CENTER_LEFT);
        Label depthLabel = new Label("Depth:");
        TextField depthField = new TextField(Integer.toString(this.depth));
        //sizing to fit the max number for depth
        depthField.setPrefColumnCount(3);
        depthField.textProperty().addListener((observable, oldValue, newValue) -> {
            try {
                if (depthField.getText().isEmpty()) {
                    depthField.setText(Integer.toString(DEFAULT_DEPTH));
                    Platform.runLater(depthField::selectAll);
                } else {
                    int newDepth = Integer.parseInt(newValue);
                    if (newDepth < MIN_DEPTH || newDepth > MAX_DEPTH) {
                        depthField.setText(oldValue);
                    } else {
                        this.depth = newDepth;
                        update();
                    }
                }
            } catch (NumberFormatException e) {
                depthField.setText(oldValue);
            }
        });
        depthBox.getChildren().addAll(depthLabel, depthField);
        Tooltip depthTip = new Tooltip("Max depth in payload (in bytes) to look for matches");
        Tooltip.install(depthBox, depthTip);
        Tooltip.install(depthField, depthTip);

        HBox relativeBox = new HBox(2);
        relativeBox.setAlignment(Pos.CENTER_LEFT);
        Label relativeLabel = new Label("Relative:");
        ChoiceBox<Boolean> relativeChoice = new ChoiceBox<>(FXCollections.observableArrayList(true, false));
        relativeChoice.setValue(this.relative);
        relativeChoice.valueProperty().addListener(change -> {
            this.relative = relativeChoice.getValue();
            update();
        });
        relativeBox.getChildren().addAll(relativeLabel, relativeChoice);

        Tooltip relativeTip = new Tooltip("Whether the offset is relative to the main cursor position or not");
        Tooltip.install(relativeLabel, relativeTip);
        Tooltip.install(relativeChoice, relativeTip);


        HBox moveCursorsBox = new HBox(2);
        moveCursorsBox.setAlignment(Pos.CENTER_LEFT);
        Label moveCursorsLabel = new Label("Move Cursors:");
        ChoiceBox<Boolean> moveCursorsChoice = new ChoiceBox<>(FXCollections.observableArrayList(true, false));
        moveCursorsChoice.setValue(this.moveCursors);
        moveCursorsChoice.valueProperty().addListener(change -> {
            this.moveCursors = moveCursorsChoice.getValue();
            update();
        });
        moveCursorsBox.getChildren().addAll(moveCursorsLabel, moveCursorsChoice);

        Tooltip moveTip = new Tooltip("If True, the start cursor will be moved to the position at the start of the matched data and the end cursor will be moved to the end of the matched data");
        Tooltip.install(moveCursorsLabel, moveTip);
        Tooltip.install(moveCursorsChoice, moveTip);


        inputBox.setAlignment(Pos.CENTER_LEFT);
        inputBox.getChildren().addAll(typeBox, noCaseBox, offsetBox, endianBox, depthBox, relativeBox, moveCursorsBox);

        return inputBox;
    }

    @Override
    public Serializable getOperation() {
        MatchFunction match = factory.createMatchFunction();
        if (this.contentType == null) {
            match.setPattern(this.content);
        } else {
            MatchFunction.Content content = factory.createMatchFunctionContent();
            content.setType(this.contentType);
            if (this.contentType == ContentType.HEX) {
                if (this.content != null) {
                    content.setValue(this.content.toUpperCase());
                } else {
                    content.setValue(null);
                }
            } else {
                if (this.content != null) {
                    content.setValue(this.content);
                } else {
                    content.setValue(null);
                }
            }
            match.setContent(content);
        }

        match.setOffset(this.offset);
        match.setEndian(this.endian.name());
        match.setNoCase(this.noCase);
        match.setDepth(this.depth);
        match.setRelative(this.relative);
        match.setMoveCursors(this.moveCursors);

        if (this.getChildren().size() > 0) {
            if (!(this.getChildren().get(0) instanceof EmptyRow)) {
                AndThen then = factory.createAndThen();
                this.getChildren().forEach(child -> {
                    if (!(child instanceof EmptyRow)) {
                        then.getMatchOrByteTestOrIsDataAt().add(child.getOperation());
                    }
                });
                match.setAndThen(then);
            }
        }

        return match;
    }

    @Override
    public void removeChild(OpRow row) {
        super.removeChild(row);

        this.getChildren().forEach(child -> child.updateOps());
    }
}
