package iadgov.fingerprint.manager.tree;


import iadgov.fingerprint.processor.FingerprintState;
import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.control.TreeItem;
import javafx.scene.layout.HBox;
import javafx.scene.text.Text;

import java.nio.file.Path;
import java.util.List;

public class FPItem extends TreeItem<String> {

    private String name;
    private String author;
    private String description;
    private List<String> tags;

    private BooleanProperty dirtyProperty;
    private ObjectProperty<Path> pathProperty;

    public FPItem(FingerprintState fpState) {
        super(fpState.getFingerprint().getHeader().getName());
        this.pathProperty = new SimpleObjectProperty<>();
        this.pathProperty.bind(fpState.pathProperty());
        this.setName(fpState.getFingerprint().getHeader().getName());
        this.author = fpState.getFingerprint().getHeader().getAuthor();
        this.description = fpState.getFingerprint().getHeader().getDescription();
        this.dirtyProperty = new SimpleBooleanProperty();
        this.dirtyProperty.bind(fpState.dirtyProperty());

        HBox graphicsBox = new HBox(3);
        Text dirtyText = new Text();
        dirtyText.textProperty().bind(Bindings.when(dirtyProperty()).then("*").otherwise(" "));
        graphicsBox.getChildren().addAll(dirtyText);

        this.setGraphic(graphicsBox);
    }


    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
        this.setValue(name);
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public String toString() {
        return this.getName();
    }

    public BooleanProperty dirtyProperty() {
        return this.dirtyProperty;
    }

    public ObjectProperty<Path> pathProperty() {
        return this.pathProperty;
    }

}
