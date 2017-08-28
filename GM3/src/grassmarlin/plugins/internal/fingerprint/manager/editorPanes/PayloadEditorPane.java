package grassmarlin.plugins.internal.fingerprint.manager.editorPanes;

import core.fingerprint3.Fingerprint;
import grassmarlin.plugins.internal.fingerprint.manager.FingerPrintGui;
import grassmarlin.plugins.internal.fingerprint.manager.payload.*;
import grassmarlin.plugins.internal.fingerprint.manager.tree.PayloadItem;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.*;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class PayloadEditorPane extends BorderPane implements ParentBox {

    private PayloadItem boundItem;
    private FingerPrintGui gui;
    private SimpleBooleanProperty hasAlways;
    private VBox childrenBox;
    private ScrollPane scrollPane;
    private List<OpRow> children;
    private boolean loading;

    private PayloadEditorPane(PayloadItem item, FingerPrintGui gui) {
        this.gui = gui;
        this.boundItem = item;
        this.hasAlways = new SimpleBooleanProperty(false);
        this.children = new ArrayList<>();

    }

    private void buildPane() {
        this.loading = true;
        childrenBox = new VBox(10);
        this.scrollPane = new ScrollPane(childrenBox);
        this.scrollPane.setFocusTraversable(false);
        this.scrollPane.setBackground(new Background((BackgroundFill)null));

        HBox descBox = new HBox(2);

        Label descLabel = new Label("Description:");
        TextField descField = new TextField(this.boundItem.getDescription());

        descBox.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(descField, Priority.ALWAYS);
        descBox.getChildren().addAll(descLabel, descField);
        descBox.setPadding(new Insets(0, 0, 10, 0));

        this.setTop(descBox);
        this.setCenter(this.scrollPane);


        Fingerprint.Payload.Always always = boundItem.getPayload().getAlways();
        if (always != null) {
            List<ReturnRow> returns = always.getReturn().stream()
                    .map(aReturn -> {
                        ReturnRow childRow = new ReturnRow(aReturn.getDetails(), aReturn.getExtract(), aReturn.getDirection(), aReturn.getConfidence());
                        return childRow;
                    })
                    .collect(Collectors.toList());
            OpRow newRow = new AlwaysRow(returns);
            newRow.insert(this, OpRow.NEW_ROW_INDEX);
            this.children.add(newRow);
            this.hasAlways.set(true);
        }

        boundItem.getPayload().getOperation().forEach(op -> {
            OpRow newRow = OpRowFactory.get(op);

            if (newRow != null) {
                newRow.insert(this, OpRow.NEW_ROW_INDEX);
                this.children.add(newRow);
            }
        });
        new EmptyRow().insert(this, OpRow.NEW_ROW_INDEX);
        this.loading = false;
    }

    public BooleanProperty HasAlwaysProperty() {
        return this.hasAlways;
    }


    @Override
    public ObservableList<PayloadItem.OpType> getAvailableOps() {
        ObservableList<PayloadItem.OpType> availableOps = FXCollections.observableArrayList(Arrays.asList(PayloadItem.OpType.values()));
        availableOps.removeAll(PayloadItem.OpType.RETURN);

        return availableOps;
    }

    @Override
    public VBox getChildrenBox() {
        return this.childrenBox;
    }

    @Override
    public void addChild(OpRow child) {
        this.children.add(child);
    }

    @Override
    public void removeChild(OpRow child) {
        if (child instanceof AlwaysRow) {
            this.gui.updateAlways(boundItem, null);
            this.boundItem.getPayload().setAlways(null);
            this.hasAlways.set(false);
        }
        this.children.remove(child);
    }

    @Override
    public boolean isLoading() {return this.loading;}

    @Override
    public void update() {
        this.children.forEach(child ->{
            if (child instanceof AlwaysRow) {
                this.gui.updateAlways(boundItem, (Fingerprint.Payload.Always)child.getOperation());
                this.boundItem.getPayload().setAlways(((Fingerprint.Payload.Always) child.getOperation()));
            }
        });

        List<Serializable> operationList = this.children.stream()
                .map(child -> {
                    if (child != null && !(child instanceof AlwaysRow)) {
                        return child.getOperation();
                    } else {
                        return null;
                    }
                })
                .filter(op -> op != null)
                .collect(Collectors.toList());

        this.gui.updateOperations(boundItem, operationList);
        this.boundItem.getPayload().getOperation().clear();
        this.boundItem.getPayload().getOperation().addAll(operationList);
    }

    public static PayloadEditorPane getInstance(PayloadItem item, FingerPrintGui gui) {
        PayloadEditorPane newPane = new PayloadEditorPane(item, gui);
        newPane.buildPane();
        return newPane;
    }

}
