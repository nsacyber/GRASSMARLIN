/*
 *  Copyright (C) 2011, 2012
 *  This file is part of GRASSMARLIN.
 */
package ui.views.viewfilter;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import java.util.List;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.embed.swing.JFXPanel;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.SelectionMode;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import javax.swing.JFrame;
import ui.icon.Icons;

/**
 *
 * Simple generic {@link java.util.List} editor.
 * @author BESTDOG - 10/15/15
 */
public class FilterSelectionDialog {

    @FXML
    public ListView listView;
    @FXML
    public Label label;

    private JFXPanel panel;
    String name;
    private JFrame frame;
    private List current;
    private List possible;
    private Runnable onClose;
    
    public FilterSelectionDialog() {
        
    }

    public static void newDialog(String name, List possible, List current, Consumer<FilterSelectionDialog> onLoad, Runnable onClose) {
        JFXPanel panel = new JFXPanel();
        Platform.runLater(()->{
            FilterSelectionDialog fsd = null;
            try {
                URL url = FilterSelectionDialog.class.getResource("FilterSelection.fxml");
                FXMLLoader loader = new FXMLLoader(url);
                Parent root = loader.load();
                fsd = loader.getController();
                fsd.panel = panel;
                fsd.onClose = onClose;
                fsd.setLabel(name);
                fsd.setLists(possible, current);
                panel.setScene(new Scene(root));
                onLoad.accept(fsd);
            } catch (IOException ex) {
                Logger.getLogger(FilterSelectionDialog.class.getName()).log(Level.SEVERE, null, ex);
            }
        });
    }
    
    public void show() {
        frame = new JFrame();
        frame.setSize(340, 480);
        frame.setTitle("Filter Manager / " + name);
        frame.setIconImage(Icons.Filter.get32());
        frame.add(panel);
        frame.setVisible(true);
    }
    
    public FilterSelectionDialog setLists(List possible, List current) {
        this.possible = possible;
        this.current = current;
        ObservableList list = FXCollections.observableArrayList();
        list.addAll(this.possible);
        this.listView.setItems(list);
        this.listView.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        current.forEach(this.listView.getSelectionModel()::select);
        return this;
    }

    public FilterSelectionDialog setLabel(String text) {
        label.setText(text);
        this.name = text;
        return this;
    }

    public void apply() {
        current.clear();
        current.addAll(listView.getSelectionModel().getSelectedItems());
        onClose.run();
        frame.setVisible(false);
        frame.dispose();
    }

    public void deselectAll() {
        listView.getSelectionModel().clearSelection();
    }

    public void selectAll() {
        listView.getSelectionModel().selectAll();
    }

}
