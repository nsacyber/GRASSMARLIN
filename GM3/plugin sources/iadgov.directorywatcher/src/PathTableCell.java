package iadgov.directorywatcher;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.stage.DirectoryChooser;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class PathTableCell extends TableCell<WatcherConfigInfo, Path> {

    private final ObjectProperty<Path> path;
    private final HBox content;
    private final Button buttonBrowse;
    private final Label labelPath;
    private final HBox textBox;
    private final TextField fieldEdit;

    public PathTableCell() {
        super();
        this.path = new SimpleObjectProperty<>();

        this.content = new HBox(0);
        this.content.setAlignment(Pos.CENTER_RIGHT);
        this.textBox = new HBox(0);
        this.textBox.setAlignment(Pos.CENTER_LEFT);
        this.labelPath = new Label("");
        this.textBox.getChildren().add(labelPath);
        this.textBox.setOnMousePressed(this::handleMousePressed);
        HBox.setHgrow(labelPath, Priority.ALWAYS);
        HBox.setHgrow(this.textBox, Priority.ALWAYS);
        this.buttonBrowse = new Button("...");
        this.buttonBrowse.setOnAction(event -> {
            DirectoryChooser chooser = new DirectoryChooser();
            chooser.setInitialDirectory(path.get() != null && Files.exists(path.get()) ? path.get().toFile() : null);
            chooser.setTitle("Watch Directory");
            File dir = chooser.showDialog(this.getScene().getWindow());
            if (dir != null) {
                this.commitEdit(dir.toPath());
            }
            event.consume();
        });

        this.fieldEdit = new TextField();
        HBox.setHgrow(fieldEdit, Priority.ALWAYS);
        this.fieldEdit.setOnKeyReleased(this::handleEditKeyPressed);
        this.fieldEdit.setOnAction(event -> {
            Path newPath = Paths.get(this.fieldEdit.getText());
            this.commitEdit(newPath);
            event.consume();
        });

    }

    @Override
    public void updateItem(Path newItem, boolean empty) {
        super.updateItem(newItem, empty);
        if (newItem == null || empty) {
            this.setText("");
            this.setGraphic(null);
        } else {
            this.path.set(newItem);
            this.labelPath.setText(newItem.toString());
            this.content.getChildren().clear();
            this.content.getChildren().addAll(this.textBox, this.buttonBrowse);
            this.setGraphic(this.content);
        }
    }

    @Override
    public void startEdit() {
        super.startEdit();
        this.fieldEdit.setText(this.path.get().toString());
        this.fieldEdit.selectAll();
        content.getChildren().clear();
        content.getChildren().add(fieldEdit);
        fieldEdit.requestFocus();
    }

    @Override
    public void cancelEdit() {
        super.cancelEdit();
        this.labelPath.setText(this.path.get().toString());
        content.getChildren().clear();
        content.getChildren().addAll(textBox, buttonBrowse);
    }

    @Override
    public void commitEdit(Path newValue) {
        super.commitEdit(newValue);
    }

    private void handleEditKeyPressed(KeyEvent event) {
        if (KeyCode.ESCAPE == event.getCode()) {
            this.cancelEdit();
            event.consume();
        }
    }

    private void handleMousePressed(MouseEvent event) {
        if (event.getClickCount() > 1) {
            this.getTableView().edit(this.getTableRow().getIndex(), this.getTableColumn());
            event.consume();
        }
    }
}
