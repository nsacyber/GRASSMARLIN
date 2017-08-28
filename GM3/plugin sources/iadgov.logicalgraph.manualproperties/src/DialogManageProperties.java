package iadgov.logicalgraph.manualproperties;

import com.sun.javafx.collections.ObservableListWrapper;
import grassmarlin.RuntimeConfiguration;
import grassmarlin.plugins.internal.logicalview.GraphLogicalVertex;
import grassmarlin.session.Property;
import grassmarlin.ui.common.controls.AutocommitTextFieldTableCell;
import grassmarlin.ui.common.menu.ActiveMenuItem;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.ObservableList;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class DialogManageProperties extends Dialog {
    private static class PropertyDescription {
        private final StringProperty name;
        private final StringProperty value;

        public PropertyDescription() {
            this("Property", "");
        }
        public PropertyDescription(final String name, final String value) {
            this.name = new SimpleStringProperty(name);
            this.value= new SimpleStringProperty(value);
        }

        public StringProperty nameProperty() {
            return this.name;
        }
        public StringProperty valueProperty() {
            return this.value;
        }
    }

    public DialogManageProperties(final GraphLogicalVertex vertex) {
        RuntimeConfiguration.setIcons(this);
        this.setTitle("Edit Properties for " + vertex.getVertex().getLogicalAddressMapping());
        this.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

        final VBox layout = new VBox();

        //HACK: We're only letting the user enter text for now--this could be expanded to all types supported by ObjectField, but for a quick feature this will work
        final ObservableList<PropertyDescription> list = new ObservableListWrapper<>(new ArrayList<>());
        if(vertex.getPropertiesForSource(Plugin.NAME) != null) {
            list.addAll(vertex.getPropertiesForSource(Plugin.NAME).entrySet().stream().flatMap(entry -> entry.getValue().stream().filter(value -> value.getValue() instanceof String).map(value -> new PropertyDescription(entry.getKey(), (String)value.getValue()))).collect(Collectors.toList()));
        }
        final TableView<PropertyDescription> tableEditableProperties = new TableView<>(list);
        tableEditableProperties.setEditable(true);

        final TableColumn<PropertyDescription, String> colProperty = new TableColumn<>("Property");
        colProperty.setCellValueFactory(param -> param.getValue().nameProperty());
        colProperty.setCellFactory(AutocommitTextFieldTableCell.forTableColumn());
        colProperty.setEditable(true);
        final TableColumn<PropertyDescription, String> colValue = new TableColumn<>("Value");
        colValue.setCellValueFactory(param -> param.getValue().valueProperty());
        colValue.setCellFactory(AutocommitTextFieldTableCell.forTableColumn());
        colValue.setEditable(true);

        tableEditableProperties.getColumns().addAll(colProperty, colValue);

        layout.getChildren().addAll(tableEditableProperties);

        this.getDialogPane().setContent(layout);

        final ContextMenu menuTable = new ContextMenu();
        menuTable.getItems().addAll(
                new ActiveMenuItem("Add", event -> {
                    if(list.add(new PropertyDescription())) {
                        tableEditableProperties.getSelectionModel().select(list.size() - 1);
                    }
                }),
                new ActiveMenuItem("Remove", event -> {
                    list.removeAll(tableEditableProperties.getSelectionModel().getSelectedItems());
                }).bindEnabled(tableEditableProperties.getSelectionModel().selectedItemProperty().isNotNull())
        );
        tableEditableProperties.setContextMenu(menuTable);

        // On closing, migrate contents of list to the vertex
        this.setOnCloseRequest(event -> {
            final Map<String, List<PropertyDescription>> map = list.stream().filter(item -> !item.nameProperty().get().equals("") && !item.valueProperty().get().equals("")).collect(Collectors.groupingBy(o -> o.nameProperty().get(), Collectors.toList()));
            // Remove anything that isn't part of the new map
            if(vertex.getPropertiesForSource(Plugin.NAME) != null) {
                for (final String key : vertex.getPropertiesForSource(Plugin.NAME).keySet()) {
                    if (!map.keySet().contains(key)) {
                        vertex.setProperties(Plugin.NAME, key);
                    }
                }
            }

            for(final String key : map.keySet()) {
                vertex.setProperties(Plugin.NAME, key, map.get(key).stream().map(text -> new Property<>(text.valueProperty().get(), 0)).collect(Collectors.toList()));
            }
        });
    }
}
