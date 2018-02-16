package iadgov.fingerprint.manager;

import core.fingerprint3.Fingerprint;
import core.fingerprint3.ObjectFactory;
import grassmarlin.Logger;
import grassmarlin.RuntimeConfiguration;
import iadgov.fingerprint.FPDocument;
import iadgov.fingerprint.Plugin;
import iadgov.fingerprint.manager.editorPanes.FilterEditPane;
import iadgov.fingerprint.manager.editorPanes.FilterGroupEditPane;
import iadgov.fingerprint.manager.editorPanes.FingerprintInfoPane;
import iadgov.fingerprint.manager.editorPanes.PayloadEditorPane;
import iadgov.fingerprint.manager.filters.Filter;
import iadgov.fingerprint.manager.tree.FPItem;
import iadgov.fingerprint.manager.tree.FilterGroupItem;
import iadgov.fingerprint.manager.tree.FilterItem;
import iadgov.fingerprint.manager.tree.PayloadItem;
import iadgov.fingerprint.processor.FingerprintState;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.ListChangeListener;
import javafx.event.ActionEvent;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Dialog;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextField;
import javafx.scene.input.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import javafx.util.converter.DefaultStringConverter;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import java.awt.*;
import java.io.File;
import java.io.Serializable;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

public class FingerPrintGui extends Application {

    private FPDocument document;
    private Plugin plugin;

    private SplitPane content;

    private TreeView<String> tree;

    private TreeItem<String> rootItem;
    private TreeItem<String> user;
    private TreeItem<String> gm;

    private ObjectFactory factory;

    private BooleanProperty selectedDirtyProperty;
    private BooleanProperty selectedSystemFingerprintProperty;

    private Stage primaryStage;

    // clipboard stuff
    private Clipboard clipboard;
    private DataFormat formatPayload;
    private DataFormat formatFilterGroup;
    private DataFormat formatFilter;
    private PasteOptionDialog pasteOptionDialog;
    private static final String stringFormatPayload = "application/java;grassmarlin-fingerprint-payload";
    private static final String stringFormatFilterGroup = "application/java;grassmarlin-fingerprint-filtergroup";
    private static final String stringFormatFilter = "application/java;grassmarlin-fingerprint-filter";

    public FingerPrintGui(Plugin plugin) {
        super();

        this.plugin = plugin;
    }

    @Override
    public void start(Stage primaryStage) throws Exception{
        this.primaryStage = primaryStage;
        this.clipboard = Clipboard.getSystemClipboard();
        this.formatPayload = DataFormat.lookupMimeType(stringFormatPayload) != null ?
                DataFormat.lookupMimeType(stringFormatPayload) : new DataFormat(stringFormatPayload);
        this.formatFilterGroup = DataFormat.lookupMimeType(stringFormatFilterGroup) != null ?
                DataFormat.lookupMimeType(stringFormatFilterGroup) : new DataFormat(stringFormatFilterGroup);
        this.formatFilter = DataFormat.lookupMimeType(stringFormatFilter) != null ?
                DataFormat.lookupMimeType(stringFormatFilter) : new DataFormat(stringFormatFilter);
        this.pasteOptionDialog = new PasteOptionDialog();

        primaryStage.setTitle("GrassMarlin Fingerprint Editor");


        RuntimeConfiguration.setIcons(primaryStage);
        primaryStage.setOnCloseRequest(this::checkDirtyOnClosing);

        this.document = FPDocument.getInstance();

        this.document.getFingerprints().addListener(this::handleFingerprintChange);

        this.factory = new ObjectFactory();

        this.selectedDirtyProperty = new SimpleBooleanProperty(false);
        this.selectedSystemFingerprintProperty = new SimpleBooleanProperty(false);

        this.rootItem = new TreeItem<>();
        this.user = new TreeItem<>("User");
        this.user.setExpanded(true);
        this.gm = new TreeItem<>("GM");
        this.gm.setExpanded(true);
        this.rootItem.getChildren().addAll(user, gm);

        this.rootItem.setExpanded(true);

        this.tree = new TreeView<>(rootItem);
        this.tree.setShowRoot(false);
        this.tree.setCellFactory(this::getCellWithMenu);
        this.tree.setOnKeyPressed(this::handleKeyPressed);
        this.tree.setEditable(true);
        this.tree.getSelectionModel().getSelectedItems().addListener(this::handleSelectionChange);

        if (!this.document.getFingerprints().isEmpty()) {
            Map<String, List<FPItem>> items = this.document.getFingerprints().stream()
            .map(fp -> {
                FPItem item = createTree(fp);
                return item;
            })
            .collect(Collectors.groupingBy(fp -> Boolean.toString(fp.pathProperty().get().toAbsolutePath()
                    .startsWith(plugin.getSystemFingerprintDir().toAbsolutePath()))));

            if (items.get("true") != null) {
                this.gm.getChildren().addAll(items.get("true"));
            }
            if (items.get("false") != null) {
                this.user.getChildren().addAll(items.get("false"));
            }
        }



        BorderPane root = new BorderPane();

        root.setTop(new TopMenu(this));

        content = new SplitPane();
        content.setOrientation(Orientation.HORIZONTAL);
        content.setDividerPositions(0.25);

        content.getItems().add(this.tree);

        BorderPane conditionPane = new BorderPane();

        content.getItems().add(conditionPane);

        root.setCenter(content);

        Dimension screenDimensions = Toolkit.getDefaultToolkit().getScreenSize();
        primaryStage.setScene(new Scene(root, screenDimensions.getWidth() * 0.85, screenDimensions.getHeight() * 0.85));
        primaryStage.centerOnScreen();
        primaryStage.show();
    }

    public FPDocument getDocument() {
        return this.document;
    }

    public void openFingerprint(FingerprintState state) {
        this.processFingerprintAdded(Collections.singletonList(state));
    }

    void newFingerprint() {
        String defaultName = "New Fingerprint";

        boolean exists;
        int count = 1;
        do {
            exists = false;
            for (FingerprintState fingerprint : this.document.getFingerprints()) {
                if (fingerprint.equals(defaultName, null)) {
                    defaultName = "New Fingerprint" + count++;
                    exists = true;
                    break;
                }
            }
        } while (exists);

        String defaultUser = System.getProperty("user.name", "User");

        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_DATE_TIME);

        String defaultDescription = "A new fingerprint created by " + defaultUser + " at " + timestamp;

        this.document.newFingerprint(defaultName, defaultUser, defaultDescription);
    }

    void newPayload(FPItem fingerprint) {
        String defaultName = "New Payload";

        boolean exists;
        int count = 1;

        do {
            exists = false;
            for (TreeItem<String> payload : fingerprint.getChildren()) {
                if (payload.getValue().equals(defaultName)) {
                    defaultName = "New Payload" + count++;
                    exists = true;
                    break;
                }
            }
        } while (exists);

        boolean added = this.document.newPayload(fingerprint.getName(), fingerprint.pathProperty().get(), defaultName);
        if (added) {
            Fingerprint.Payload payload = factory.createFingerprintPayload();
            payload.setFor(defaultName);
            PayloadItem pl = new PayloadItem(payload);
            fingerprint.getChildren().add(pl);
            this.tree.getSelectionModel().select(pl);
        }
    }

    void newFilterGroup(PayloadItem payload) {
        String defaultName = "New Group";

        boolean exists;
        int count = 1;

        do {
            exists = false;
            for (TreeItem<String> group : payload.getChildren()) {
                if (group.getValue().equals(defaultName)) {
                    defaultName = "New Group" + count++;
                    exists = true;
                    break;
                }
            }
        } while (exists);

        if (payload.getParent() instanceof FPItem) {
            FPItem fp = ((FPItem) payload.getParent());

            boolean added = this.document.newFilterGroup(fp.getName(), fp.pathProperty().get(), payload.getName(), defaultName);
            if (added) {
                FilterGroupItem fg = new FilterGroupItem(defaultName);
                payload.getChildren().add(fg);
                this.tree.getSelectionModel().select(fg);
            }
        }
    }

    private void handleSelectionChange(ListChangeListener.Change<? extends TreeItem<String>> change) {

        while (change.next()) {

            if (change.wasReplaced() || change.wasAdded()) {
                TreeItem<String> selected = change.getAddedSubList().get(0);

                Pane editPane;

                if (selected instanceof FPItem) {
                    FPItem fp = (FPItem) selected;
                    editPane = new FingerprintInfoPane(fp, this);
                } else if (selected instanceof PayloadItem) {
                    PayloadItem payload = (PayloadItem) selected;
                    editPane = PayloadEditorPane.getInstance(payload, this);
                } else if (selected instanceof FilterItem) {
                    FilterItem fi = (FilterItem) selected;
                    editPane = new FilterEditPane(fi, this);
                } else if (selected instanceof FilterGroupItem) {
                    FilterGroupItem fgi = ((FilterGroupItem) selected);
                    editPane = new FilterGroupEditPane(fgi, this);
                } else {
                    Pane empty = new Pane();
                    editPane = empty;
                }

                FPItem fp = this.getSelectedFPItem();
                if (fp != null) {
                    this.selectedDirtyProperty.bind(fp.dirtyProperty());
                    if (fp.pathProperty().get() != null) {
                        this.selectedSystemFingerprintProperty.set(fp.pathProperty().get().toAbsolutePath().startsWith(plugin.getSystemFingerprintDir().toAbsolutePath()));
                    } else {
                        this.selectedSystemFingerprintProperty.set(false);
                    }
                } else {
                    this.selectedDirtyProperty.unbind();
                    this.selectedDirtyProperty.setValue(false);
                    this.selectedSystemFingerprintProperty.set(false);
                }

                Node conditionPane = content.getItems().get(1);
                if (conditionPane instanceof BorderPane) {
                    ((BorderPane) conditionPane).setCenter(editPane);
                }
            } else if (change.wasRemoved()) {
                Node conditionPane = content.getItems().get(1);
                if (conditionPane instanceof  BorderPane) {
                    ((BorderPane) conditionPane).setCenter(new Pane());
                }

                this.selectedDirtyProperty.unbind();
                this.selectedDirtyProperty.setValue(false);
            }
        }
    }

    private void handleFingerprintChange(ListChangeListener.Change<? extends FingerprintState> change) {
        while(change.next()) {
            if (change.wasAdded()) {
                processFingerprintAdded(change.getAddedSubList());
            }
        }
    }

    private void handleKeyPressed(KeyEvent event) {
        TreeItem<String> selected = this.tree.getSelectionModel().getSelectedItem();
        if (null != selected && event.getCode() == KeyCode.DELETE) {
            if (selected instanceof PayloadItem) {
                PayloadItem pl = ((PayloadItem) selected);
                FPItem fp = getFPItem(pl);
                if (this.document.delPayload(fp.getName(), fp.pathProperty().get(), pl.getName())) {
                    SelectionModel<TreeItem<String>> selectionModel = this.tree.getSelectionModel();
                    TreeItem<String> parent = selected.getParent();
                    selected.getParent().getChildren().remove(selected);
                    int numPayloads = parent.getChildren().size();
                    if (numPayloads == 0) {
                        //no payloads left
                        selectionModel.select(parent);
                    } else if (numPayloads == 1) {
                        //select the only payload
                        selectionModel.select(parent.getChildren().get(0));
                    } else {
                        //find the next payload or previous if no next
                        int startIndex = selectionModel.getSelectedIndex();
                        //Since this is a tree the next node will either be the next sibling of the
                        //deleted node or the next sibling of its parent or nothing
                        selectionModel.select(startIndex + 1);
                        if (!(selectionModel.getSelectedItem() instanceof PayloadItem)) {
                            while (!(selectionModel.getSelectedItem() instanceof PayloadItem)) {
                                selectionModel.select(startIndex--);
                            }
                        }
                    }
                }

            }  else if (selected instanceof FilterGroupItem) {
                FilterGroupItem group = ((FilterGroupItem) selected);
                FPItem fp = getFPItem(group);
                if (this.document.delFilterGroup(fp.getName(), fp.pathProperty().get(), getPayloadItem(group).getName(), group.getName())) {
                    SelectionModel<TreeItem<String>> selectionModel = this.tree.getSelectionModel();
                    TreeItem<String> parent = selected.getParent();
                    selected.getParent().getChildren().remove(selected);
                    int numGroups = parent.getChildren().size();
                    if (numGroups == 0) {
                        selectionModel.select(parent);
                    } else if (numGroups == 1) {
                        selectionModel.select(parent.getChildren().get(0));
                    } else {
                        int startIndex = selectionModel.getSelectedIndex();

                        selectionModel.select(startIndex + 1);
                        if (!(selectionModel.getSelectedItem() instanceof FilterGroupItem)) {
                            while (!(selectionModel.getSelectedItem() instanceof FilterGroupItem)) {
                                selectionModel.select(startIndex--);
                            }
                        }
                    }
                }
            } else if (selected instanceof FilterItem) {
                FilterItem filter = ((FilterItem) selected);
                SelectionModel<TreeItem<String>> selectionModel = this.tree.getSelectionModel();
                if (this.deleteFilter(filter)) {
                    TreeItem<String> parent = filter.getParent();
                    parent.getChildren().remove(filter);
                    int numFilters = parent.getChildren().size();
                    if (numFilters == 0) {
                        selectionModel.select(parent);
                    } else if (numFilters == 1) {
                        selectionModel.select(parent.getChildren().get(0));
                    } else {
                        int startIndex = selectionModel.getSelectedIndex();

                        selectionModel.select(startIndex + 1);
                        if (!(selectionModel.getSelectedItem() instanceof FilterItem)) {
                            while (!(selectionModel.getSelectedItem() instanceof FilterItem)) {
                                selectionModel.select(startIndex--);
                            }
                        }
                    }
                }
            }
        }
    }

    private void processFingerprintAdded(List<? extends FingerprintState> added) {
        TreeItem<String> firstNewItem = null;
        for (FingerprintState fingerprint : added) {
            FPItem item = createTree(fingerprint);
            if (item.pathProperty().get() != null && item.pathProperty().get().toAbsolutePath().startsWith(this.plugin.getSystemFingerprintDir())) {
                this.gm.getChildren().add(item);
                this.gm.getChildren().sort((ti1, ti2) -> ti1.getValue().compareTo(ti2.getValue()));
            } else {
                this.user.getChildren().add(item);
                this.user.getChildren().sort((ti1, ti2) -> ti1.getValue().compareTo(ti2.getValue()));
            }
            if (null == firstNewItem) {
                firstNewItem = item;
            }
        }
        this.tree.getSelectionModel().select(firstNewItem);
    }

    private FPItem createTree(FingerprintState fpState) {

        Map<String, PayloadItem> payloadMap = new HashMap<>();
        Fingerprint fp = fpState.getFingerprint();
        FPItem item = new FPItem(fpState);

        fp.getPayload().forEach(payload -> {
            PayloadItem payloadItem = new PayloadItem(payload);
            payloadMap.put(payload.getFor(), payloadItem);
            item.getChildren().add(payloadItem);
        });
        fp.getFilter().forEach(group -> {
            FilterGroupItem groupItem = new FilterGroupItem(group.getName());
            group.getAckAndMSSAndDsize().forEach(filter -> {
                FilterItem newFilter = new FilterItem(Filter.FilterType.valueOf(filter.getName().toString().replaceAll(" ", "").toUpperCase()), group.getAckAndMSSAndDsize().indexOf(filter), this, filter);
                groupItem.getChildren().add(newFilter);
            });
            PayloadItem payload = payloadMap.get(group.getFor());
            payload.getChildren().add(groupItem);
        });

        return item;
    }

    private TreeCell<String> getCellWithMenu(TreeView<String> view) {
        ValueAddedTextFieldTreeCell<String> newCell = new ValueAddedTextFieldTreeCell<String>(new DefaultStringConverter()){

            @Override
            public void updateItem(String item, boolean empty) {
                this.setBinding(null);
                if (empty || item == null) {
                    setText(null);
                    setGraphic(null);
                } else if (!item.isEmpty()) {
                    TreeItem<String> treeItem = this.getTreeItem();
                    if (treeItem instanceof FPItem) {
                        FPItem fpItem = (FPItem) treeItem;
                        this.setBinding(Bindings.when(fpItem.pathProperty().isNotNull()).then(Bindings.concat("    ", fpItem.pathProperty())).otherwise(""));
                        // only need to do this if the name has actually changed
                        if (!fpItem.getName().equals(item)) {
                            boolean updated = FingerPrintGui.this.getDocument().updateFingerprintName(fpItem.getName(), item, fpItem.pathProperty().get());
                            if (!updated) {
                                this.cancelEdit();
                                this.getTreeItem().setValue(fpItem.getName());
                            } else {
                                fpItem.setName(item);
                            }
                        }
                    } else if (treeItem instanceof PayloadItem) {
                        PayloadItem payloadItem = (PayloadItem) treeItem;
                        // only need to do this if the name has actually changed
                        if (!payloadItem.getName().equals(item)) {
                            FPItem fp = getFPItem(payloadItem);
                            boolean updated = FingerPrintGui.this.getDocument().updatePayloadName
                                    (fp.getName(), fp.pathProperty().get(), payloadItem.getName(), item);
                            if (!updated) {
                                this.cancelEdit();
                                this.getTreeItem().setValue(payloadItem.getName());
                            } else {
                                payloadItem.setName(item);
                            }
                        }
                    } else if (treeItem instanceof FilterGroupItem) {
                        FilterGroupItem fgItem = (FilterGroupItem) treeItem;
                        // only need to do this if the name has actually changed
                        if (!fgItem.getName().equals(item)) {
                            FPItem fp = getFPItem(fgItem);
                            boolean updated = FingerPrintGui.this.getDocument().updateFilterGroupName
                                    (fp.getName(), fp.pathProperty().get(), getPayloadItem(fgItem).getName(), fgItem.getName(), item);
                            if (!updated) {
                                this.cancelEdit();
                                this.getTreeItem().setValue(fgItem.getName());
                            } else {
                                fgItem.setName(item);
                            }
                        }
                    }
                } else {
                    this.cancelEdit();
                }

                super.updateItem(item, empty);
            }
        };

        newCell.treeItemProperty().addListener((observable, oldValue, newValue) -> {
            if (null != newValue) {
                if (newValue instanceof FPItem) {
                    newCell.setContextMenu(getFingerprintCM((FPItem) newValue));
                } else if (newValue instanceof PayloadItem) {
                    newCell.setContextMenu(getPayloadCM((PayloadItem) newValue));
                } else if (newValue instanceof FilterGroupItem) {
                    newCell.setContextMenu(getGroupCM((FilterGroupItem) newValue));
                } else if (newValue instanceof FilterItem) {
                    newCell.setContextMenu(getFilterCM((FilterItem) newValue));
                    newCell.setEditable(false);
                }
            }
        });

        newCell.setConcatFill(Color.GRAY);
        newCell.setConcatOverrun(OverrunStyle.CENTER_ELLIPSIS);
        newCell.prefWidthProperty().bind(this.tree.widthProperty().subtract(10));


        return newCell;
    }

    public void copySelectedItem(ActionEvent event) {
        TreeItem<?> selected = this.tree.getSelectionModel().getSelectedItem();
        if (selected != null) {
            if (selected instanceof PayloadItem) {
                copyPayload(((PayloadItem) selected));
            } else if (selected instanceof FilterGroupItem) {
                copyFilterGroup(((FilterGroupItem) selected));
            } else if (selected instanceof FilterItem) {
                copyFilter(((FilterItem) selected));
            }
        }
    }

    public void pasteToSelectedItem(ActionEvent event) {
        TreeItem<?> selected = this.tree.getSelectionModel().getSelectedItem();

        if (selected != null) {
            if (selected instanceof FPItem) {
                pastePayload(((FPItem) selected));
            } else if (selected instanceof PayloadItem) {
                pasteFilterGroup(((PayloadItem) selected));
            } else if (selected instanceof FilterGroupItem) {
                pasteFilter(((FilterGroupItem) selected));
            }
        }
    }

    private void copyPayload(PayloadItem source) {
        ClipboardContent content = new ClipboardContent();
        content.put(formatPayload, source.getPayload());
        content.putString(source.getPayload().getFor() + System.lineSeparator() + source.getPayload().getDescription());

        clipboard.setContent(content);
    }
    private void pastePayload(FPItem destination) {
        if (clipboard.hasContent(formatPayload)) {
            Fingerprint.Payload newPayload = (Fingerprint.Payload)clipboard.getContent(formatPayload);
            Optional<PayloadItem> payloadItem = destination.getChildren().stream()
                    .filter(pl -> pl.getValue().equals(newPayload.getFor()))
                    .map(pl -> (PayloadItem)pl)
                    .findFirst();
            if (payloadItem.isPresent()) {
                Optional<ButtonType> selection = this.pasteOptionDialog.showAndWait(newPayload.getFor());
                if (selection.isPresent()) {
                    ButtonType clicked = selection.get();
                    boolean exists;
                    if (clicked.equals(PasteOptionDialog.RENAME)) {
                        AtomicInteger copyNumber = new AtomicInteger(1);
                        do {
                            int i = copyNumber.incrementAndGet();
                            exists = destination.getChildren().stream()
                                    .anyMatch(pl -> pl.getValue().equals(newPayload.getFor() + "(" + i + ")"));
                        } while (exists);

                        newPayload.setFor(newPayload.getFor() + "(" + copyNumber.get() + ")");

                        boolean added = this.document.addPayload(destination.getName(), destination.pathProperty().get(), newPayload);

                        if (added) {
                            PayloadItem item = new PayloadItem(newPayload);
                            destination.getChildren().addAll(item);
                            destination.setExpanded(true);
                        }
                        else {
                            Logger.log(Logger.Severity.ERROR, "Copy Rename Failed");
                        }
                    } else if (clicked.equals(PasteOptionDialog.OVERWRITE)) {
                        Optional<FingerprintState> state = this.document.getState(destination.getName(), destination.pathProperty().get());
                        Optional<Fingerprint.Payload> oldPayload = state.get().getFingerprint().getPayload().stream()
                                .filter(pl -> pl.getFor().equals(newPayload.getFor()))
                                .findFirst();
                        boolean removed = this.document.delPayload(destination.getName(), destination.pathProperty().get(), newPayload.getFor());

                        if (removed) {
                            destination.getChildren().remove(payloadItem.get());
                            boolean added = this.document.addPayload(destination.getName(), destination.pathProperty().get(), newPayload);

                            if (added) {
                                destination.getChildren().add(new PayloadItem(newPayload));
                                destination.setExpanded(true);
                            } else {
                                Logger.log(Logger.Severity.ERROR, "Copy Overwrite Failed, Trying to revert");
                                boolean reverted = this.document.addPayload(destination.getName(), destination.pathProperty().get(), oldPayload.get());
                                if (reverted) {
                                    destination.getChildren().add(new PayloadItem(oldPayload.get()));
                                } else {
                                    Logger.log(Logger.Severity.ERROR, "Overwrite Revert Failed");
                                }
                            }
                        }
                    }
                }
            } else {
                boolean added = this.document.addPayload(destination.getName(), destination.pathProperty().get(), newPayload);

                if (added) {
                    PayloadItem item = new PayloadItem(newPayload);
                    destination.getChildren().addAll(item);
                    destination.setExpanded(true);
                }
                else {
                    Logger.log(Logger.Severity.ERROR, "Paste Failed");
                }
            }
        }
    }

    private void copyFilterGroup(FilterGroupItem source) {
        FPItem fpItem = getFPItem(source);
        PayloadItem plItem = getPayloadItem(source);
        Fingerprint fp = this.document.getState(fpItem.getName(), fpItem.pathProperty().get()).get().getFingerprint();
        Fingerprint.Filter filterGroup = fp.getFilter().stream()
                .filter(fg -> fg.getFor().equals(plItem.getName()) && fg.getName().equals(source.getName()))
                .findFirst().get();

        ClipboardContent content = new ClipboardContent();
        content.put(formatFilterGroup, filterGroup);
        content.putString(filterGroup.getFor() + ":" + filterGroup.getName());

        this.clipboard.setContent(content);
    }

    private void pasteFilterGroup(PayloadItem destination) {
        if (clipboard.hasContent(formatFilterGroup)) {
            FPItem fpItem = getFPItem(destination);
            Fingerprint.Filter newFilterGroup = ((Fingerprint.Filter) clipboard.getContent(formatFilterGroup));
            newFilterGroup.setFor(destination.getPayload().getFor());
            Optional<FilterGroupItem> filterGroupItem = destination.getChildren().stream()
                    .filter(fgi -> fgi.getValue().equals(newFilterGroup.getName()))
                    .map(fgi -> (FilterGroupItem)fgi)
                    .findFirst();
            if (filterGroupItem.isPresent()) {
                Optional<ButtonType> selection = pasteOptionDialog.showAndWait(newFilterGroup.getName());
                if (selection.isPresent()) {
                    ButtonType clicked = selection.get();
                    boolean exists;
                    if (clicked.equals(PasteOptionDialog.RENAME)) {
                        AtomicInteger copyNumber = new AtomicInteger(1);
                        do {
                            int i = copyNumber.incrementAndGet();
                            exists = destination.getChildren().stream()
                                    .anyMatch(pl -> pl.getValue().equals(newFilterGroup.getName() + "(" + i + ")"));
                        } while (exists);

                        newFilterGroup.setName(newFilterGroup.getName() + "(" + copyNumber.get() + ")");

                        boolean added = this.document.addFilterGroup(fpItem.getName(), fpItem.pathProperty().get(), newFilterGroup);

                        if (added) {
                            FilterGroupItem item = new FilterGroupItem(newFilterGroup.getName());
                            destination.getChildren().addAll(item);
                            destination.setExpanded(true);

                            for (int i = 0; i < newFilterGroup.getAckAndMSSAndDsize().size(); i++) {
                                JAXBElement<?> filter = newFilterGroup.getAckAndMSSAndDsize().get(i);
                                FilterItem fi = new FilterItem(Filter.FilterType.valueOf(filter.getName().toString().replaceAll(" ", "").toUpperCase()), i, this, filter);
                                item.getChildren().add(fi);
                            }
                        }
                        else{
                            Logger.log(Logger.Severity.ERROR, "Copy Rename Failed");
                        }
                    } else if (clicked.equals(PasteOptionDialog.OVERWRITE)) {
                        Optional<FingerprintState> state = this.document.getState(fpItem.getName(), fpItem.pathProperty().get());
                        Optional<Fingerprint.Filter> oldFilterGroup = state.get().getFingerprint().getFilter().stream()
                                .filter(group -> group.getFor().equals(newFilterGroup.getFor()) && group.getName().equals(newFilterGroup.getName()))
                                .findFirst();
                        boolean removed = this.document.delFilterGroup(fpItem.getName(), fpItem.pathProperty().get(), oldFilterGroup.get().getFor(), oldFilterGroup.get().getName());

                        if (removed) {
                            destination.getChildren().remove(filterGroupItem.get());
                            boolean added = this.document.addFilterGroup(fpItem.getName(), fpItem.pathProperty().get(), newFilterGroup);

                            if (added) {
                                FilterGroupItem item = new FilterGroupItem(newFilterGroup.getName());
                                destination.getChildren().addAll(item);

                                for (int i = 0; i < newFilterGroup.getAckAndMSSAndDsize().size(); i++) {
                                    JAXBElement<?> filter = newFilterGroup.getAckAndMSSAndDsize().get(i);
                                    FilterItem fi = new FilterItem(Filter.FilterType.valueOf(filter.getName().toString().replaceAll(" ", "").toUpperCase()), i, this, filter);
                                    item.getChildren().add(fi);
                                }
                                destination.setExpanded(true);
                            } else {
                                Logger.log(Logger.Severity.ERROR, "Copy Overwrite Failed, Trying to revert");
                                boolean reverted = this.document.addFilterGroup(fpItem.getName(), fpItem.pathProperty().get(), oldFilterGroup.get());
                                if (reverted) {
                                    FilterGroupItem item = new FilterGroupItem(oldFilterGroup.get().getName());
                                    destination.getChildren().addAll(item);

                                    for (int i = 0; i < oldFilterGroup.get().getAckAndMSSAndDsize().size(); i++) {
                                        JAXBElement<?> filter = oldFilterGroup.get().getAckAndMSSAndDsize().get(i);
                                        FilterItem fi = new FilterItem(Filter.FilterType.valueOf(filter.getName().toString().replaceAll(" ", "").toUpperCase()), i, this, filter);
                                        item.getChildren().add(fi);
                                    }
                                } else {
                                    Logger.log(Logger.Severity.ERROR, "Overwrite Revert Failed");
                                }
                            }
                        }
                    }
                }
            } else {
                boolean added = this.document.addFilterGroup(fpItem.getName(), fpItem.pathProperty().get(), newFilterGroup);
                if (added) {
                    FilterGroupItem item = new FilterGroupItem(newFilterGroup.getName());
                    destination.getChildren().addAll(item);

                    for (int i = 0; i < newFilterGroup.getAckAndMSSAndDsize().size(); i++) {
                        JAXBElement<?> filter = newFilterGroup.getAckAndMSSAndDsize().get(i);
                        FilterItem fi = new FilterItem(Filter.FilterType.valueOf(filter.getName().toString().replaceAll(" ", "").toUpperCase()), i, this, filter);
                        item.getChildren().add(fi);
                    }
                    destination.setExpanded(true);
                }
                else {
                    Logger.log(Logger.Severity.ERROR, "Paste Failed");
                }
            }
        }
    }

    private void copyFilter(FilterItem source) {
        FilterGroupItem fgi = getGroupItem(source);
        PayloadItem pli = getPayloadItem(source);
        FPItem fpi = getFPItem(source);

        Optional<FingerprintState> state = this.document.getState(fpi.getName(), fpi.pathProperty().get());

        if (state.isPresent()) {
            Optional<Fingerprint.Filter> filterGroup = state.get().getFingerprint().getFilter().stream()
                    .filter(group -> group.getFor().equals(pli.getName()) && group.getName().equals(fgi.getName()))
                    .findFirst();
            if (filterGroup.isPresent()) {
                if (filterGroup.get().getAckAndMSSAndDsize().size() > source.getIndex()) {
                    JAXBElement<?> filter = filterGroup.get().getAckAndMSSAndDsize().get(source.getIndex());
                    ClipboardContent content = new ClipboardContent();
                    content.put(formatFilter, filter);
                    content.putString(filter.getName().toString() + filter.getValue());
                    this.clipboard.setContent(content);
                }
            }
        }
    }

    private void pasteFilter(FilterGroupItem destination) {
        if (this.clipboard.hasContent(formatFilter)) {
            JAXBElement<?> filter = (JAXBElement<?>)clipboard.getContent(formatFilter);
            FPItem fpi = getFPItem(destination);
            PayloadItem pli = getPayloadItem(destination);

            int index = this.document.addFilter(fpi.getName(), fpi.pathProperty().get(), pli.getName(), destination.getName(), filter);

            if (index >= 0 ) {
                FilterItem newItem = new FilterItem(Filter.FilterType.valueOf(filter.getName().toString().replaceAll(" ", "").toUpperCase()), index, this, filter);
                destination.getChildren().add(newItem);
            }
            destination.setExpanded(true);
        }
    }

    private ContextMenu getFingerprintCM(FPItem source) {
        ContextMenu menu = new ContextMenu();
        menu.addEventFilter(MouseEvent.MOUSE_RELEASED, this::disableRightClick);

        MenuItem newItem = new MenuItem("New Payload");
        newItem.setOnAction(event -> {
            FingerPrintGui.this.newPayload(source);
            source.setExpanded(true);
        });

        MenuItem pasteItem = new MenuItem("Paste");
        pasteItem.setOnAction(event -> {
            this.pastePayload(source);
        });

        MenuItem saveItem = new MenuItem("Save");
        saveItem.setOnAction(this::saveFingerprintWODialog);
        saveItem.disableProperty().bind(source.dirtyProperty().not());

        MenuItem expandItem = new MenuItem("Expand");
        expandItem.setOnAction(event -> this.expandAll(source));

        MenuItem collapseItem = new MenuItem("Collapse");
        collapseItem.setOnAction(event -> this.collapseAll(source));

        menu.getItems().addAll(newItem, new SeparatorMenuItem(), saveItem, new SeparatorMenuItem(), pasteItem, new SeparatorMenuItem(), expandItem, collapseItem);

        return menu;
    }

    private ContextMenu getPayloadCM(PayloadItem source) {
        ContextMenu menu = new ContextMenu();
        menu.addEventFilter(MouseEvent.MOUSE_RELEASED, this::disableRightClick);

        MenuItem newGroupItem = new MenuItem("New Filter Group");
        newGroupItem.setOnAction(event -> {
            FingerPrintGui.this.newFilterGroup(source);
            source.setExpanded(true);
        });

        MenuItem copyItem = new MenuItem("Copy");
        copyItem.setOnAction(event -> {
            this.copyPayload(source);
        });
        MenuItem pasteItem = new MenuItem("Paste");
        pasteItem.setOnAction(event -> {
            this.pasteFilterGroup(source);
        });

        MenuItem expandItem = new MenuItem("Expand");
        expandItem.setOnAction(event -> this.expandAll(source));

        MenuItem collapseItem = new MenuItem("Collapse");
        collapseItem.setOnAction(event -> this.collapseAll(source));


        menu.getItems().addAll(newGroupItem, new SeparatorMenuItem(), copyItem, pasteItem, new SeparatorMenuItem(), expandItem, collapseItem);

        return menu;
    }

    private ContextMenu getGroupCM(FilterGroupItem source) {
        ContextMenu menu = new ContextMenu();
        menu.addEventFilter(MouseEvent.MOUSE_RELEASED, this::disableRightClick);

        MenuItem item = new MenuItem("New Filter");
        item.setOnAction(event -> {
            FingerPrintGui.this.addFilter(source, true);
            source.setExpanded(true);
        });

        MenuItem copyItem = new MenuItem("Copy");
        copyItem.setOnAction(event -> {
            this.copyFilterGroup(source);
        });
        MenuItem pasteItem = new MenuItem("Paste");
        pasteItem.setOnAction(event -> {
            this.pasteFilter(source);
        });

        MenuItem expandItem = new MenuItem("Expand");
        expandItem.setOnAction(event -> this.expandAll(source));

        MenuItem collapseItem = new MenuItem("Collapse");
        collapseItem.setOnAction(event -> this.collapseAll(source));

        menu.getItems().addAll(item, new SeparatorMenuItem(), copyItem, pasteItem, new SeparatorMenuItem(), expandItem, collapseItem);

        return menu;
    }

    private ContextMenu getFilterCM(FilterItem source) {
        ContextMenu menu = new ContextMenu();
        menu.addEventFilter(MouseEvent.MOUSE_RELEASED, this::disableRightClick);

        MenuItem add = new MenuItem("Add");
        add.setOnAction(event -> {
            TreeItem<String> parent = source.getParent();
            FingerPrintGui.this.addFilter(parent, true);
        });

        MenuItem copyItem = new MenuItem("Copy");
        copyItem.setOnAction(event -> {
            this.copyFilter(source);
        });

        MenuItem expandItem = new MenuItem("Expand");
        expandItem.setOnAction(event -> this.expandAll(source));

        MenuItem collapseItem = new MenuItem("Collapse");
        collapseItem.setOnAction(event -> this.collapseAll(source));


        menu.getItems().addAll(add, new SeparatorMenuItem(), copyItem, new SeparatorMenuItem(), expandItem, collapseItem);

        return menu;
    }

    private void expandAll(TreeItem<String> item) {
        for (TreeItem<String> child : item.getChildren()) {
            expandAll(child);
        }
        item.setExpanded(true);

        Platform.runLater(() -> this.tree.getSelectionModel().select(item));
    }

    private void collapseAll(TreeItem<String> item) {
        for (TreeItem<String> child : item.getChildren()) {
            collapseAll(child);
        }
        item.setExpanded(false);
    }

    private void disableRightClick(MouseEvent event) {
        if (event.getButton() == MouseButton.SECONDARY) {
            event.consume();
        }
    }

    public void addFilter(TreeItem<String> parent, boolean focus) {
        FPItem fp = getFPItem(parent);
        int index = this.document.addFilter(fp.getName(), fp.pathProperty().get(), getPayloadItem(parent).getName(),
                getGroupItem(parent).getName(), getDefaultFilterElement());

        if (index >= 0) {
            FilterItem filter = new FilterItem(Filter.FilterType.DSTPORT, index, this);

            parent.getChildren().add(filter);
            if (focus) {
                this.tree.getSelectionModel().select(filter);
                filter.setFocus();
            }
        }
    }

    private boolean deleteFilter(FilterItem filter) {
        boolean deleted = false;
        HashMap<Integer, Integer> newIndices = new HashMap<>();
        FPItem fp = getFPItem(filter);
        if (this.document.deleteFilter(newIndices, fp.getName(), fp.pathProperty().get(),
                getPayloadItem(filter).getName(), getGroupItem(filter).getName(), filter.getIndex())) {
            deleted = true;
            TreeItem<String> parent = filter.getParent();
            parent.getChildren().forEach(child -> {
                if (child instanceof FilterItem) {
                    Integer newIndex = newIndices.get(((FilterItem) child).getIndex());
                    if (newIndex != null) {
                        ((FilterItem) child).setIndex(newIndex);
                    }
                }
            });
        }

        return deleted;
    }

    public JAXBElement<Integer> getDefaultFilterElement() {
        return factory.createFingerprintFilterDstPort(80);
    }

    public FPItem getFPItem(TreeItem<String> item) {
        if (item instanceof FPItem) {
            return ((FPItem) item);
        } else if (item instanceof PayloadItem) {
            return getFPItem((PayloadItem) item);
        } else if (item instanceof FilterGroupItem) {
            return getFPItem((FilterGroupItem) item);
        } else if (item instanceof FilterItem) {
            return getFPItem((FilterItem) item);
        } else {
            return null;
        }
    }

    public PayloadItem getPayloadItem(TreeItem<String> item) {
        if (item instanceof PayloadItem) {
            return ((PayloadItem) item);
        } else if (item instanceof FilterGroupItem) {
            return getPayloadItem((FilterGroupItem) item);
        } else if (item instanceof FilterItem) {
            return getPayloadItem((FilterItem) item);
        } else {
            throw new IllegalArgumentException("Unknown tree item type");
        }
    }

    public FilterGroupItem getGroupItem(TreeItem<String> item) {
        if (item instanceof FilterGroupItem) {
            return ((FilterGroupItem) item);
        } else if (item instanceof FilterItem) {
            return getGroupItem((FilterItem) item);
        } else {
            throw new IllegalArgumentException("Unknown tree item type");
        }
    }

    public FPItem getFPItem(PayloadItem item) {
        if (item.getParent() instanceof FPItem) {
            return ((FPItem) item.getParent());
        } else {
            return null;
        }
    }

    public FPItem getFPItem(FilterGroupItem item) {
        if (item.getParent().getParent() instanceof FPItem) {
            return ((FPItem) item.getParent().getParent());
        } else {
            return null;
        }
    }

    public FPItem getFPItem(FilterItem item) {
        if (item.getParent().getParent().getParent() instanceof FPItem) {
            return ((FPItem) item.getParent().getParent().getParent());
        } else {
            return null;
        }
    }

    public PayloadItem getPayloadItem(FilterGroupItem item) {
        if (item.getParent() instanceof PayloadItem) {
            return ((PayloadItem) item.getParent());
        } else {
            return null;
        }
    }

    public PayloadItem getPayloadItem(FilterItem item) {
        if (item.getParent().getParent() instanceof PayloadItem) {
            return ((PayloadItem) item.getParent().getParent());
        } else {
            return null;
        }
    }

    public FilterGroupItem getGroupItem(FilterItem item) {
        if (item.getParent() instanceof FilterGroupItem) {
            return ((FilterGroupItem) item.getParent());
        } else {
            return null;
        }
    }

    public static void selectAll(TextField field) {
        // currently required to make this work
        // apparently the select all gets overridden  if it's not run later
        Platform.runLater(() -> {
            if (field.isFocused()) {
                field.selectAll();
            }
        });
    }

    public void updateAlways(PayloadItem payload, Fingerprint.Payload.Always always) {
        FPItem fp = getFPItem(payload);
        this.document.updateAlways(fp.getName(), fp.pathProperty().get(), payload.getPayload().getFor(), always);
    }

    public void updateOperations(PayloadItem payload, List<Serializable> operationList) {
        FPItem fp = getFPItem(payload);
        this.document.updateOperations(fp.getName(), fp.pathProperty().get(), payload.getPayload().getFor(), operationList);
    }

    public FPItem getSelectedFPItem() {
        TreeItem<String> selected = this.tree.getSelectionModel().getSelectedItem();
        FPItem fpName = selected != null ? getFPItem(selected) : null;

        return fpName;
    }

    public BooleanBinding isSelectedSavable() {
        return this.selectedDirtyProperty.and(this.selectedSystemFingerprintProperty.not());
    }


    public boolean saveFingerprintWDialog(ActionEvent event) {
        FPItem fp = this.getSelectedFPItem();

        boolean saved =  this.saveFingerprintWDialog(fp.getName(), fp.pathProperty().get());

        return saved;
    }


    public boolean saveFingerprintWDialog(String fpName, Path loadPath) {
        boolean saved;
        FileChooser chooser = new FileChooser();

        String fileName;
        Path initialDir;
        if (loadPath != null) {
            initialDir = loadPath.getParent();
            fileName = loadPath.getFileName().toString();
        } else {
            initialDir = this.document.getPlugin().getDefaultFingerprintDir();
            fileName = fpName;
        }
        if (initialDir != null) {
            chooser.setInitialDirectory(initialDir.toFile());
        }


        FileChooser.ExtensionFilter xmlFilter = new FileChooser.ExtensionFilter("Fingerprint", "*.xml");
        FileChooser.ExtensionFilter everything = new FileChooser.ExtensionFilter("All", "*.*");
        chooser.getExtensionFilters().addAll(xmlFilter, everything);
        chooser.setSelectedExtensionFilter(xmlFilter);
        chooser.setInitialFileName(fileName);

        File toSave = chooser.showSaveDialog(this.content.getScene().getWindow());

        while (toSave != null &&
                (toSave.toPath().toAbsolutePath().startsWith(this.plugin.getSystemFingerprintDir()))) {
            Alert badPathAlert = new Alert(Alert.AlertType.WARNING, "Can not save to the Default Fingerprints Directory");
            badPathAlert.showAndWait();
            toSave = chooser.showSaveDialog(this.content.getScene().getWindow());
        }

        if (toSave != null) {
            saved = this.saveFingerprint(fpName, loadPath, toSave.toPath());
        } else {
            saved = false;
        }

        return saved;
    }

    public boolean saveFingerprintWODialog(ActionEvent event) {

        FPItem fp = this.getSelectedFPItem();
        Path fpPath = fp.pathProperty().get();

        return this.saveFingerprintWODialog(fp.getName(), fpPath);
    }

    public boolean saveFingerprintWODialog(String fpName, Path loadPath) {
        boolean saved;
        if (!(loadPath == null || loadPath.toAbsolutePath().startsWith(this.plugin.getDefaultFingerprintDir().toAbsolutePath()))) {
            saved = this.saveFingerprint(fpName, loadPath, loadPath);
        } else {
            saved = saveFingerprintWDialog(fpName, loadPath);
        }

        return saved;
    }

    private boolean saveFingerprint(String fpName, Path loadPath, Path toSave) {
        boolean saved = false;
        if (null != toSave) {
            try {
                this.document.save(fpName, loadPath, toSave);
                saved = true;
            } catch (Exception e) {
                if (e.getCause() != null && e.getCause().getMessage() != null) {
                    String[] errorArray = e.getCause().getMessage().split(":");
                    Alert error = new Alert(Alert.AlertType.WARNING, "Unable to save Fingerprint: " + errorArray[errorArray.length - 1]);
                    error.showAndWait();
                } else {
                    Alert error = new Alert(Alert.AlertType.ERROR, "Unknown error saving Fingerprint!");
                    error.showAndWait();
                }
            }
        }
        return saved;
    }

    private void checkDirtyOnClosing(WindowEvent event) {
        List<FingerprintState> unsavedStates = new ArrayList<>();
        boolean canceled = false;

        mainLoop:
        for (FingerprintState state : this.document.getFingerprints()) {
            if (state.dirtyProperty().get()) {
                Optional<ButtonType> choice = this.getSaveOnCloseDialog(state.getFingerprint().getHeader().getName(),
                        state.pathProperty().get()).showAndWait();

                if (choice.isPresent()) {
                    switch (choice.get().getButtonData()) {
                        case YES:
                            boolean saved = this.saveFingerprintWODialog(state.getFingerprint().getHeader().getName(), state.pathProperty().get());
                            if (!saved) {
                                event.consume();
                                break mainLoop;
                            }
                            break;
                        case NO:
                            unsavedStates.add(state);
                            break;
                        case CANCEL_CLOSE:
                            canceled = true;
                            event.consume();
                            break mainLoop;
                        default:
                    }
                }
            }
        }

        if (!canceled) {
            for (FingerprintState state : unsavedStates) {
                this.document.delFingerprint(state.getFingerprint().getHeader().getName(), state.pathProperty().get());

                if (state.pathProperty().get() != null) {
                    try {
                        this.document.registerFingerprint(this.document.load(state.pathProperty().get()));
                    } catch (JAXBException | NullPointerException exc) {
                        Logger.log(Logger.Severity.ERROR, "Unable to restore previous state of fingerprint %s", state.pathProperty().get());
                    }
                }
            }
        }
    }

    public Dialog<ButtonType> getSaveOnCloseDialog(String fpName, Path loadPath) {
        String atString = loadPath != null && !loadPath.toString().isEmpty() ? loadPath.toString() : "";
        Dialog<ButtonType> saveDialog = new Dialog<>();
        saveDialog.getDialogPane().getButtonTypes().addAll(ButtonType.YES, ButtonType.NO, ButtonType.CANCEL);
        ((Button) saveDialog.getDialogPane().lookupButton(ButtonType.YES)).setDefaultButton(false);
        saveDialog.setTitle("Closing");
        saveDialog.setHeaderText("Closing \"" + fpName + "\"\n\nWould you like to Save?");
        saveDialog.setContentText(atString);

        return saveDialog;
    }

    public void exit() {
        WindowEvent.fireEvent(this.primaryStage, new WindowEvent(this.primaryStage, WindowEvent.WINDOW_CLOSE_REQUEST));
    }

    public static void main(String[] args) {
        launch(args);
    }
}
