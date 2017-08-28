package grassmarlin.ui.common.controls;

import grassmarlin.RuntimeConfiguration;
import grassmarlin.plugins.IPlugin;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.StringProperty;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.util.StringConverter;

import java.util.stream.Collectors;

public class ObjectField<T> extends HBox {
    protected final RuntimeConfiguration config;
    protected final Class<? super T> clazz;
    private final TextField text;
    private final ComboBox<IPlugin.HasClassFactory.ClassFactory<? extends T>> combo;
    private final Button btnAdd;

    public ObjectField(final RuntimeConfiguration config, final Class<? super T> clazz) {
        super(2.0);

        this.config = config;
        this.clazz = clazz;

        this.text = new TextField();
        this.combo = new ComboBox<>();
        this.btnAdd = new Button("Add");

        initComponents();
    }

    private void initComponents() {
        this.combo.getItems().addAll(this.config.enumeratePlugins(IPlugin.HasClassFactory.class).stream()
                .flatMap(plugin -> plugin.getClassFactories().stream())
                .filter(factory -> clazz.isAssignableFrom(factory.getFactoryClass()))
                .map(factory -> (IPlugin.HasClassFactory.ClassFactory<T>)factory)
                .sorted((factory1, factory2) -> factory1.getFactoryName().compareTo(factory2.getFactoryName()))
                .collect(Collectors.toList())
        );

        HBox.setHgrow(this.text, Priority.ALWAYS);
        this.getChildren().addAll(this.text, this.combo, this.btnAdd);

        if(this.combo.getItems().size() == 0) {
            this.text.setDisable(true);
            this.combo.setDisable(true);
            this.btnAdd.setDisable(true);
            return;
        } else {
            this.combo.setValue(this.combo.getItems().get(0));
        }
        this.combo.setConverter(new StringConverter<IPlugin.HasClassFactory.ClassFactory<? extends T>>() {
            public String toString(IPlugin.HasClassFactory.ClassFactory<? extends T> object) {
                return object.getFactoryName();
            }

            public IPlugin.HasClassFactory.ClassFactory<? extends T> fromString(String string) {
                //This isn't necessary for the relevant use case.
                return null;
            }
        });
        this.text.textProperty().addListener((observable, oldValue, newValue) -> {
            if(this.combo.getSelectionModel().getSelectedItem() == null) {
                this.btnAdd.setDisable(true);
            } else {
                this.btnAdd.setDisable(!this.combo.getSelectionModel().getSelectedItem().validateText(newValue));
            }
        });
        this.btnAdd.setDisable(!this.combo.getSelectionModel().getSelectedItem().validateText(this.text.getText()));
        this.btnAdd.visibleProperty().bind(this.btnAdd.onActionProperty().isNotNull());
    }

    public T createInstanceFromText() {
        return this.combo.getSelectionModel().getSelectedItem().createInstance(this.text.getText());
    }
    public void setValue(final T value) {
        if(value == null) {
            this.combo.getSelectionModel().clearSelection();
            this.text.clear();
        } else {
            this.combo.getSelectionModel().select(this.combo.getItems().stream().filter(item -> item.getFactoryClass().equals(value.getClass())).findAny().orElse(null));
            this.text.setText(value.toString());
        }
    }

    public ObjectProperty<EventHandler<ActionEvent>> onActionProperty() {
        return this.btnAdd.onActionProperty();
    }
    public StringProperty buttonTextProperty() {
        return this.btnAdd.textProperty();
    }

    public void clear() {
        this.text.clear();
    }
}
