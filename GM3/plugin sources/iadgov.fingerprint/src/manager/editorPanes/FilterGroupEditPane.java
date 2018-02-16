package iadgov.fingerprint.manager.editorPanes;

import iadgov.fingerprint.manager.FingerPrintGui;
import iadgov.fingerprint.manager.filters.Filter;
import iadgov.fingerprint.manager.tree.FPItem;
import iadgov.fingerprint.manager.tree.FilterGroupItem;
import iadgov.fingerprint.manager.tree.FilterItem;
import javafx.beans.InvalidationListener;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TreeItem;
import javafx.scene.layout.*;

import java.util.List;
import java.util.stream.Collectors;

public class FilterGroupEditPane extends BorderPane {

    private FilterGroupItem boundItem;
    private FingerPrintGui gui;
    private GridPane content;
    private ScrollPane scrollPane;

    public FilterGroupEditPane(FilterGroupItem boundItem, FingerPrintGui gui) {
        super();
        this.boundItem = boundItem;
        this.gui = gui;
        this.scrollPane = new ScrollPane();
        this.scrollPane.setFitToHeight(true);
        this.scrollPane.setFitToWidth(true);
        this.scrollPane.setBackground(new Background((BackgroundFill)null));
        this.content = new GridPane();
        this.scrollPane.setContent(content);
        this.setCenter(this.scrollPane);

        this.content.setVgap(5);
        this.content.setHgap(2);
        this.setPadding(new Insets(5));

        this.init();

        InvalidationListener childListener = observable -> {
            this.content.getChildren().clear();
            this.init();
        };

        this.boundItem.getChildren().addListener(childListener);
    }

    private void init() {
        if (boundItem.getChildren().isEmpty()) {
            this.gui.addFilter(boundItem, false);
            ((FilterItem) boundItem.getChildren().get(0)).getRow().insert(this.content, 0);
            this.content.add(getButtons(), 2, 0);
        } else {
            for (int i = 0; i < boundItem.getChildren().size(); i++) {
                TreeItem<String> child = boundItem.getChildren().get(i);
                if (child instanceof FilterItem) {
                    FilterItem filter = ((FilterItem) child);
                    filter.getRow().insert(this.content, i);
                    this.content.add(getButtons(), 2, i);
                }
            }
        }
    }

    private HBox getButtons() {
        HBox buttonBox = new HBox(1);
        Button addButton = new Button("+");
        Button delButton = new Button("-");

        addButton.setOnAction(event -> {
            FPItem fp = this.gui.getFPItem(this.boundItem);
            int storedIndex = gui.getDocument().addFilter(fp.getName(), fp.pathProperty().get(),
                    gui.getPayloadItem(this.boundItem).getName(), this.boundItem.getName(), gui.getDefaultFilterElement());

            int currentIndex = GridPane.getRowIndex(buttonBox);
            int newRowIndex = currentIndex + 1;

            if (storedIndex >= 0) {
                FilterItem newItem = new FilterItem(Filter.FilterType.DSTPORT, storedIndex, gui);
                //insert after row that contains the button that was pressed;
                this.content.getChildren().forEach(child -> {
                    int row = GridPane.getRowIndex(child);
                    if (row > currentIndex) {
                        GridPane.setRowIndex(child, row + 1);
                        ((FilterItem) boundItem.getChildren().get(row)).setRowIndex(row + 1);
                    }
                });
                newItem.getRow().insert(this.content, newRowIndex);
                this.content.add(getButtons(), 2, newRowIndex);
                boundItem.getChildren().add(newRowIndex, newItem);
            }
        });

        delButton.setOnAction(event -> {
            int currentIndex = GridPane.getRowIndex(buttonBox);
            List<Node> toRemove = this.content.getChildren().stream()
                            .filter(child -> GridPane.getRowIndex(child) == currentIndex)
                            .collect(Collectors.toList());
            this.content.getChildren().removeAll(toRemove);
            boundItem.getChildren().remove(currentIndex);

            this.content.getChildren().forEach(child -> {
                int row = GridPane.getRowIndex(child);
                if (row > currentIndex) {
                    GridPane.setRowIndex(child, row - 1);
                }
            });
        });

        buttonBox.getChildren().addAll(addButton, delButton);

        return buttonBox;
    }
}
