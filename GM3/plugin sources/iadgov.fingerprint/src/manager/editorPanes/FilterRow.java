package iadgov.fingerprint.manager.editorPanes;

import grassmarlin.Logger;
import iadgov.fingerprint.manager.filters.Filter;
import iadgov.fingerprint.manager.tree.FilterItem;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.util.Callback;
import javafx.util.StringConverter;

import javax.xml.bind.JAXBElement;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;

public class FilterRow {
    
    private FilterItem boundItem;
    private Filter<?> filter;
    private ComboBox<Filter.FilterType> filterBox;
    private int row;

    private FilterRow(FilterItem item, Filter<?> filter){
        this.boundItem = item;
        this.filter = filter;
    }

    public static FilterRow newFilterRow(FilterItem item, JAXBElement<?> value) {
        FilterRow row = null;
        if (null != item) {
            try {
                Constructor<? extends Filter> constructor = item.getType().getImplementingClass().getConstructor(JAXBElement.class);
                Filter<?> filter = constructor.newInstance(value);
                row = new FilterRow(item, filter);
            } catch (IllegalAccessException | InstantiationException | NoSuchMethodException | InvocationTargetException e) {
                // Something has gone horribly wrong but the only thing we can do at this point is to just let it return null
                Logger.log(Logger.Severity.PEDANTIC_DEVELOPER_SPAM, "Can not construct new fingerprint filter: " + e.getMessage());
            }
        }

        return row;
    }

    public void insert(GridPane parent, int row) {
        this.row = row;
        filterBox = new ComboBox<>();
        Arrays.stream(Filter.FilterType.values())
                .sorted((type, otherType) -> type.getName().compareTo(otherType.getName()))
                .forEach(type -> filterBox.getItems().add(type));

        filterBox.setValue(boundItem.getType());

        filterBox.setConverter(new FilterStringConverter());
        filterBox.setCellFactory(new TooltipCellFactory());
        filterBox.setVisibleRowCount(5);
        filterBox.setEditable(false);
        filterBox.setOnAction(event -> {
            Filter.FilterType type = filterBox.getSelectionModel().getSelectedItem();

            boundItem.setType(type);

            try {
                filter = type.getImplementingClass().newInstance();
                addListener(filter);
                boundItem.updateValue(filter.elementProperty().get());
                HBox input = filter.getInput();
                input.setAlignment(Pos.CENTER_LEFT);
                Node replaceMe = null;
                for (Node node : parent.getChildren()) {
                    if (node instanceof HBox && GridPane.getColumnIndex(node) == 1 && GridPane.getRowIndex(node) == this.row) {
                        replaceMe = node;
                        //found it
                        break;
                    }
                }
                if (null != replaceMe) {
                    parent.getChildren().remove(replaceMe);
                }
                parent.add(input, 1, this.row);
            } catch (IllegalAccessException | InstantiationException e) {
                //TODO proper error handling
                e.printStackTrace();
            }
        });

        // initialize on default filter
        if (filter == null) {
            Filter.FilterType type = filterBox.getSelectionModel().getSelectedItem();

            try {
                filter = type.getImplementingClass().newInstance();
                addListener(filter);
            } catch (IllegalAccessException | InstantiationException e) {
                //TODO proper error handling
                e.printStackTrace();
            }

        }
        if (filter != null) {
            HBox input = filter.getInput();
            input.setAlignment(Pos.CENTER_LEFT);
            Node replaceMe = null;
            for (Node node : parent.getChildren()) {
                if (node instanceof HBox && GridPane.getColumnIndex(node) == 1 && GridPane.getRowIndex(node) == this.row) {
                    replaceMe = node;
                    break;
                }
            }
            if (null != replaceMe) {
                parent.getChildren().remove(replaceMe);
            }
            parent.add(input, 1, this.row);
        }

        parent.add(filterBox, 0, this.row);

    }

    private void addListener(Filter<?> filter) {
        if (null != filter.elementProperty()) {
            filter.elementProperty().addListener((observable, oldValue, newValue) -> boundItem.updateValue(newValue));
        }
    }

    public void setFocus() {
        this.filterBox.requestFocus();
    }

    public void setRow(int index) {
        this.row = index;
    }

    private class FilterStringConverter extends StringConverter<Filter.FilterType> {
        @Override
        public String toString(Filter.FilterType type) {
            return type.getName();
        }

        @Override
        public Filter.FilterType fromString(String name) {
            Filter.FilterType returnType = null;
            for (Filter.FilterType type : Filter.FilterType.values()) {
                if (type.getName().equals(name)) {
                   returnType = type;
                }
            }

            return returnType;
        }
    }

    private class TooltipCellFactory implements Callback<ListView<Filter.FilterType>, ListCell<Filter.FilterType>> {
        @Override
        public ListCell<Filter.FilterType> call(ListView<Filter.FilterType> view) {
            return new ListCell<Filter.FilterType>() {
                @Override
                protected void updateItem(Filter.FilterType item, boolean empty) {
                    super.updateItem(item, empty);

                    if (item == null || empty) {
                        setTooltip(null);
                        setText(null);
                    } else {
                        setTooltip(new Tooltip(item.getTooltip()));
                        setText(item.getName());
                    }
                }
            };
        }
    }

    public JAXBElement getElement() {
        return this.filter.elementProperty().get();
    }
}
