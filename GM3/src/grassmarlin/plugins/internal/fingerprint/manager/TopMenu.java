package grassmarlin.plugins.internal.fingerprint.manager;


import javafx.event.ActionEvent;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.stage.FileChooser;

import javax.xml.bind.JAXBException;
import java.io.File;
import java.nio.file.Path;
import java.util.List;

public class TopMenu extends MenuBar{

    FingerPrintGui gui;

    public TopMenu(FingerPrintGui gui) {
        super();
        this.gui = gui;
        createMenu();
    }

    private void createMenu() {
        Menu fileMenu = new Menu("_File");

        MenuItem openItem = new MenuItem("_Open...");
        openItem.setOnAction(this::showOpenDialog);
        openItem.setAccelerator(new KeyCodeCombination(KeyCode.O, KeyCodeCombination.SHORTCUT_DOWN));

        MenuItem saveItem = new MenuItem("_Save");
        saveItem.setOnAction(this.gui::saveFingerprintWODialog);
        saveItem.setAccelerator(new KeyCodeCombination(KeyCode.S, KeyCodeCombination.SHORTCUT_DOWN));
        saveItem.disableProperty().bind(this.gui.isSelectedDirtyProperty().not());

        MenuItem saveAsItem = new MenuItem("Save _As...");
        saveAsItem.setOnAction(this.gui::saveFingerprintWDialog);

        MenuItem newItem = new MenuItem("_New Fingerprint");
        newItem.setOnAction(this::createFingerprint);
        newItem.setAccelerator(new KeyCodeCombination(KeyCode.N, KeyCodeCombination.SHORTCUT_DOWN));

        MenuItem exitItem = new MenuItem("E_xit");
        exitItem.setOnAction(this::handleExit);
        exitItem.setAccelerator(new KeyCodeCombination(KeyCode.X, KeyCodeCombination.SHORTCUT_DOWN));

        fileMenu.getItems().addAll(newItem, new SeparatorMenuItem(), openItem, saveItem, saveAsItem, new SeparatorMenuItem(), exitItem);
        fileMenu.setOnShowing(event -> {
            boolean isSelected = this.gui.getSelectedFPItem() != null;
            saveAsItem.setDisable(!isSelected);
        });

        Menu editMenu = new Menu("_Edit");

        MenuItem copyItem = new MenuItem("_Copy");
        copyItem.setOnAction(this.gui::copySelectedItem);
        copyItem.setAccelerator(new KeyCodeCombination(KeyCode.C, KeyCombination.SHORTCUT_DOWN));

        MenuItem pasteItem = new MenuItem("_Paste");
        pasteItem.setOnAction(this.gui::pasteToSelectedItem);
        pasteItem.setAccelerator(new KeyCodeCombination(KeyCode.V, KeyCombination.SHORTCUT_DOWN));

        editMenu.getItems().addAll(copyItem, pasteItem);


        this.getMenus().addAll(fileMenu, editMenu);
    }

    private void showOpenDialog(ActionEvent event) {
        FileChooser chooser = new FileChooser();
        Path initialDir = gui.getDocument().getPlugin().getDefaultFingerprintDir();
        if (null != initialDir) {
            chooser.setInitialDirectory(initialDir.toFile());
        }

        chooser.setTitle("Open...");
        FileChooser.ExtensionFilter xmlFilter = new FileChooser.ExtensionFilter("Fingerprint", "*.xml");
        FileChooser.ExtensionFilter everything = new FileChooser.ExtensionFilter("All", "*.*");
        chooser.getExtensionFilters().addAll(xmlFilter, everything);
        chooser.setSelectedExtensionFilter(xmlFilter);
        List<File> toLoadList = chooser.showOpenMultipleDialog(this.getScene().getWindow());

        if (null != toLoadList) {
            for (File toLoad : toLoadList) {
                try {
                    if (this.gui.getDocument().alreadyLoaded(null, toLoad.toPath())) {
                        Alert loadedAlert = new Alert(Alert.AlertType.INFORMATION);
                        loadedAlert.setTitle("Already Open");
                        loadedAlert.setHeaderText("Fingerprint Already Open");
                        loadedAlert.showAndWait();
                    } else {
                        this.gui.getDocument().load(toLoad.toPath());
                    }
                } catch (JAXBException e) {
                    Alert error = new Alert(Alert.AlertType.ERROR, "Unable to load file");
                    error.showAndWait();
                }
            }
        }
    }

    private void createFingerprint(ActionEvent event) {
        this.gui.newFingerprint();
    }

    private void handleExit(ActionEvent event) {
        this.gui.exit();
    }
}
