/*
 *  Copyright (C) 2011, 2012
 *  This file is part of GRASSMARLIN.
 */
package ui.dialog.detaileditor;

import core.types.DataDetails;
import core.types.VisualDetails;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Pane;
import javafx.util.Callback;
import javax.imageio.ImageIO;
import ui.icon.Icons;
import ui.views.tree.visualnode.VisualNode;

/**
 *
 * @author BESTDOG
 *
 * Allows the HashMap backing the {@link VisualDetail} object to be edited by a
 * user.
 *
 * The map is displayed as a table, and has two text fields to change/add
 * key-value pairs, as well as a right-click option to remove the key
 * completely.
 *
 */
public class DetailEditor implements Initializable {

    @FXML
    GridPane gridPane; // root
    @FXML
    Pane panelImage;
    @FXML
    Label labelName;
    @FXML
    Label labelAddress;
    @FXML
    Label labelCategory;
    @FXML
    Label labelNodeType;
    @FXML
    TableView table;
    @FXML
    Button cancelButton;
    @FXML
    Button buttonRefresh;
    @FXML
    TextField selectedValue;
    @FXML
    TextField selectedName;
    @FXML
    ImageView imageView;
    @FXML
    Button buttonSave;
    @FXML
    Button buttonAdd;

    private final ObservableList<TableEntry> list;
    private TableEntry selectedEntry;
    /**
     * callback called from {@link #saveEntry() } when the new key-value pair is
     * valid and will be put in the node's data.
     */
    private Consumer<VisualNode> onChange;
    private VisualNode node;
    /**
     * set once, indicates the {@link #node} is valid and it contains details.
     */
    private boolean valid;
    /**
     * The parameter passed on the last call of {@link #setEditing(boolean) }
     */
    private boolean editing;

    public DetailEditor() {
        list = FXCollections.observableArrayList();
    }

    private boolean validate() {
        valid = node != null && node.hasDetails();
        return valid;
    }

    private VisualDetails getDetails() {
        return node.getDetails();
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        this.buttonRefresh.setGraphic(new ImageView(this.getImage(Icons.Refresh.get())));

        TableColumn keyColumn = (TableColumn) table.getColumns().get(0);
        TableColumn valueColumn = (TableColumn) table.getColumns().get(1);
        keyColumn.setCellValueFactory(new PropertyValueFactory<>("Key"));
        valueColumn.setCellValueFactory(new PropertyValueFactory<>("Value"));
        table.setItems(list);

        /**
         * Calls {@link #setSelectedEntry} on each row-selection change.
         */
        table.getSelectionModel().selectedItemProperty().addListener((observable, oldValue, newValue) -> {
            final Object obj = table.getSelectionModel().getSelectedItem();
            if (obj != null) {
                final TableEntry e = (TableEntry) obj;
                setSelectedEntry(e);
                updateFields();
            }
        });

        /**
         * adds the right-click->remove option to the table.
         */
        table.setRowFactory(new Callback<TableView<TableEntry>, TableRow<TableEntry>>() {
            @Override
            public TableRow<TableEntry> call(TableView<TableEntry> param) {
                final TableRow<TableEntry> row = new TableRow();
                final ContextMenu menu = new ContextMenu();
                final MenuItem removeItem = new MenuItem("Remove");
                removeItem.setOnAction(e -> {
                    String key = row.getItem().getKey();
                    if (valid) {
                        DetailEditor.this.getDetails().remove(key);
                        DetailEditor.this.updateList();
                    }
                });
                menu.getItems().add(removeItem);
                row.contextMenuProperty().bind(
                        Bindings.when(
                                row.emptyProperty())
                        .then((ContextMenu) null)
                        .otherwise(menu)
                );
                return row;
            }
        });

    }

    /**
     * Changes the image displayed in the top-left corner of the window.
     */
    private void updateImage() {
        Image fxImage = getImage(getDetails().image.getDefault());
        this.imageView.setImage(fxImage);
    }

    /**
     * gets a scaled version of a RenderedImage or a centered version of a
     * unmanaged image within a 64x64 grey box
     */
    private Image getImage(java.awt.Image awtImage) {
        if (!(awtImage instanceof RenderedImage)) {

            int x = 64;
            int y = 64;
            int width = awtImage.getWidth(null);
            int height = awtImage.getHeight(null);

            x = 32 - width / 2;
            y = 32 - height / 2;

            BufferedImage bi = new BufferedImage(
                    64,
                    64,
                    BufferedImage.TYPE_INT_ARGB
            );

            Graphics g = bi.getGraphics();
            g.setColor(Color.GRAY);
            g.fillRect(0, 0, 64, 64);
            g.drawImage(awtImage, x, y, null);
            g.dispose();
            awtImage = bi;
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            ImageIO.write((RenderedImage) awtImage, "png", out);
            out.flush();
        } catch (IOException ex) {
            Logger.getLogger(DetailEditor.class.getName()).log(Level.SEVERE, null, ex);
        }
        ByteArrayInputStream in = new ByteArrayInputStream(out.toByteArray());
        return new Image(in);
    }

    /**
     * Sets a method to run each time the node's internal data is changed.
     *
     * @param onChange Callback to run on change.
     */
    void setOnChangeCallback(Consumer<VisualNode> onChange) {
        this.onChange = onChange;
    }

    /**
     * Sets the node used to populate this window.
     *
     * @param node A VisualNode with a {@link VisualNode#hasDetails() } method
     * expected to return true.
     */
    void setNode(VisualNode node) {
        this.node = node;
        validate();
        refreshData();
    }

    /**
     * Refreshes the entire window from the original object with current data.
     */
    public void refreshData() {
        if (this.node == null || getDetails() == null) {
            return;
        }
        this.updateText();
        this.updateList();
        this.updateFields();
        this.updateImage();
    }

    /**
     * Updates text on the ui, except key-value fields.
     */
    private void updateText() {
        if (valid) {
            //<editor-fold defaultstate="collapsed" desc="name & address block">
            String addressText;
            String nameText;
            if (node.hasUserDefinedName()) {
                addressText = String.format("Address: %s", node.getAddress());
                nameText = String.format("Name: %s", node.getName());
            } else {
                addressText = "";
                nameText = String.format("Address: %s", node.getAddress());
            }
            this.labelAddress.setText(addressText);
            this.labelName.setText(nameText);
            //</editor-fold>
            this.labelNodeType.setText(node.isHost() ? "Host" : "Network");
            this.labelCategory.setText(node.getDetails().getCategory().getPrettyPrint());
        }
    }

    /**
     * Updates the key-value pairs in the table and resets selection. It will
     * also clear the {@link #getSelectedEntry() } object and reset the
     * key-value fields.
     */
    private void updateList() {
        this.list.clear();
        DataDetails details = this.getDetails();
        details.forEach((k, v)
                -> this.list.add(new TableEntry(details, k, v))
        );
        this.setSelectedEntry(null);
        this.updateFields();
    }

    /**
     * Updates the key-value fields to the last {@link TableEntry} passed to {@link #setSelectedEntry(ui.dialog.detaileditor.DetailEditor.TableEntry)
     * }
     * and turns off editing.
     *
     * If the null wass passed to the setSelctedEntry method then the fields
     * will be cleared.
     */
    private void updateFields() {
        TableEntry entry = this.getSelectedEntry();
        if (entry == null) {
            this.selectedName.setText("");
            this.selectedValue.setText("");
        } else {
            this.selectedName.setText(entry.getKey());
            this.selectedValue.setText(entry.getValue());
        }
        this.setEditing(false);
    }

    /**
     * Cancels editing the key-value fields.
     */
    public void cancel() {
        this.selectedName.setText("");
        this.selectedValue.setText("");
        setEditing(false);
    }

    /**
     * Returns last TableEntry clicked on. Nullable.
     */
    public TableEntry getSelectedEntry() {
        return selectedEntry;
    }

    public void setSelectedEntry(TableEntry selectedEntry) {
        this.selectedEntry = selectedEntry;
    }

    /**
     * enables the key-value text fields so that a new Entry can be added. Calls {@link #setEditing(boolean)
     * }.
     */
    public void addEntry() {
        this.setEditing(true);
    }

    /**
     * Get last parameter passed to {@link#setEditing(boolean) }
     */
    public boolean isEditing() {
        return editing;
    }

    /**
     * Updates the disabled states of buttons and text-fields in the key-value
     * field part ( bottom part ) of the window. These components are for
     * editing the VisualNodes VisualDetails.
     *
     * @param editing True if editing, else false and only the "Add" button is
     * enabled.
     */
    private void setEditing(boolean editing) {
        this.editing = editing;
        if (this.editing) {
            this.buttonAdd.setDisable(true);
            this.buttonSave.setDisable(false);
            this.cancelButton.setDisable(false);

            this.selectedName.setDisable(false);
            this.selectedValue.setDisable(false);

            this.selectedName.requestFocus();
        } else {
            this.buttonAdd.setDisable(false);
            this.buttonSave.setDisable(true);
            this.cancelButton.setDisable(true);

            this.selectedName.setDisable(true);
            this.selectedValue.setDisable(true);

            this.table.requestFocus();
        }
    }

    /**
     * Saves the map and fires the {@link #onChange} callback.
     */
    public void saveEntry() {
        String key = selectedName.getText();
        String value = selectedValue.getText();
        if (key == null || value == null || key.isEmpty() || value.isEmpty()) {
            return;
        } else {
            getDetails().put(key, value);
            updateList();
            this.onChange.accept(this.node);
        }
    }

    /**
     * The row model for the DetailEditor's TableView.
     */
    public static class TableEntry {

        private final SimpleStringProperty key;
        private final SimpleStringProperty value;
        Map<String, String> source;

        private TableEntry(Map<String, String> source, String key, String value) {
            this.source = source;
            this.key = new SimpleStringProperty(key);
            this.value = new SimpleStringProperty(value);
        }

        public String getKey() {
            return this.key.get();
        }

        public String getValue() {
            return this.value.get();
        }

        public void setKey(String key) {
//            this.key.set(key);
        }

        public void setValue(String value) {
            this.value.set(value);
            this.source.put(key.get(), value);
        }

    }

}
