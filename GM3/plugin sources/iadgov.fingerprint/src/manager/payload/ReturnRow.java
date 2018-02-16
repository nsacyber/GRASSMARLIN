package iadgov.fingerprint.manager.payload;

import core.fingerprint3.DetailGroup;
import core.fingerprint3.Direction;
import core.fingerprint3.Extract;
import core.fingerprint3.Return;
import grassmarlin.common.Confidence;
import iadgov.fingerprint.manager.editorPanes.DetailsDialog;
import iadgov.fingerprint.manager.editorPanes.ExtractDialog;
import iadgov.fingerprint.manager.tree.PayloadItem;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.stage.Window;
import javafx.util.StringConverter;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;


public class ReturnRow extends OpRow {

    private DetailsDialog details;
    private ExtractDialog extract;
    private Direction direction;
    private Confidence confidence;


    public ReturnRow(DetailGroup details, List<Extract> extractList, Direction direction, int confidence) {
        super(PayloadItem.OpType.RETURN);

        Map<String, String> detailMap = new HashMap<>();
        if (details != null) {
            details.getDetail().forEach(detail -> detailMap.put(detail.getName(), detail.getValue()));

            this.details = new DetailsDialog(
                    details.getCategory() != null ? Category.valueOf(details.getCategory()) : null,
                    details.getRole() != null ? Role.valueOf(details.getRole()) : null,
                    detailMap);
        } else {
            this.details = new DetailsDialog();
        }
        if (extractList != null && !extractList.isEmpty()) {
            this.extract = new ExtractDialog(extractList);
        } else {
            this.extract = new ExtractDialog();
        }

        this.details.setOnShowing(this::centerDetails);
        this.extract.setOnShowing(this::centerExtract);

        this.direction = direction;
        this.confidence = Confidence.fromNumber(confidence);
    }

    public ReturnRow() {
        this(null, null, null, 0);
    }

    @Override
    public HBox getInput() {
        HBox input = new HBox();

        Button detailsButton = new Button("Details...");
        detailsButton.setOnAction(event -> {
            Optional<DetailsDialog> tempDetails = this.details.showAndWait();
            tempDetails.ifPresent(details -> this.details = details);
            update();
        });

        detailsButton.setTooltip(new Tooltip("Details to assign"));

        Button extractButton = new Button("Extract...");
        extractButton.setOnAction(event -> {
            Optional<ExtractDialog> tempExtract = this.extract.showAndWait();
            tempExtract.ifPresent(extract -> this.extract = extract);
            update();
        });

        extractButton.setTooltip(new Tooltip("Values to extract from the packet data"));

        Label directionLabel = new Label("Direction:");
        ChoiceBox<Direction> directionBox = new ChoiceBox<>(FXCollections.observableArrayList(Direction.values()));
        directionBox.setValue(this.direction != null ? this.direction : Direction.SOURCE);
        this.direction = directionBox.getValue();
        directionBox.valueProperty().addListener(change -> {
            this.direction = directionBox.getValue();
            update();
        });

        Tooltip dirTip = new Tooltip("The part of the communication to assign the properties to");
        Tooltip.install(directionLabel, dirTip);
        Tooltip.install(directionBox, dirTip);

        Label confLabel = new Label("Confidence:");
        ChoiceBox<Confidence> confBox = new ChoiceBox<>(FXCollections.observableArrayList(Confidence.getAssignableConfidenceList()));
        confBox.setConverter(new StringConverter<Confidence>() {
            @Override
            public String toString(Confidence object) {
                return object.asString();
            }

            @Override
            public Confidence fromString(String string) {
                return Confidence.fromString(string);
            }
        });
        confBox.setValue(this.confidence != null ? this.confidence : Confidence.LOW);
        this.confidence = confBox.getValue();
        confBox.valueProperty().addListener(change -> {
            this.confidence = confBox.getValue();
            update();
        });

        Tooltip confTip = new Tooltip("The confidence value to attach to the properties (5-lowest to 1-highest)");
        Tooltip.install(confLabel, confTip);
        Tooltip.install(confBox, confTip);

        input.setAlignment(Pos.CENTER_LEFT);
        input.setSpacing(2);
        input.getChildren().addAll(detailsButton, extractButton, directionLabel, directionBox, confLabel, confBox);

        return input;
    }

    @Override
    public ObservableList<PayloadItem.OpType> getAvailableOps() {
        return null;
    }

    @Override
    public Serializable getOperation() {
        Return ret = factory.createReturn();
        ret.setDirection(this.direction);
        ret.setConfidence(this.confidence.asNumber());
        DetailGroup detailGroup = factory.createDetailGroup();
        detailGroup.setCategory(this.details.getCategory());
        detailGroup.setRole(this.details.getRole());
        detailGroup.getDetail().addAll(
                this.details.getDetails().entrySet().stream()
                    .map(entry -> {
                        DetailGroup.Detail detail = factory.createDetailGroupDetail();
                        detail.setName(entry.getKey());
                        detail.setValue(entry.getValue());
                        return detail;
                    })
                    .collect(Collectors.toList())
        );
        
        ret.setDetails(detailGroup);
        ret.getExtract().addAll(this.extract.getExtractList());

        return ret;
    }

    private void centerDetails(DialogEvent event) {
        Platform.runLater(() -> this.center(this.details));
    }

    private void centerExtract(DialogEvent event) {
        Platform.runLater(() -> this.center(this.extract));
    }

    private void center(Dialog dialog) {
        if (parent != null) {
            Window window = parent.getChildrenBox().getScene().getWindow();
            double x = window.getX() + window.getWidth() / 2 - dialog.getWidth() / 2;
            double y = window.getY() + window.getHeight() / 2 - dialog.getHeight() / 2;
            dialog.setX(x);
            dialog.setY(y);
        }
    }
}
