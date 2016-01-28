package ui.dialog.fingerprintManager;

import TemplateEngine.Fingerprint3.Fingerprint;
import TemplateEngine.Util.FPC;
import core.Core.ALERT;
import core.Environment;
import core.fingerprint.FManager;
import core.types.LogEmitter;
import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import ui.GrassMarlin;
import ui.dialog.DialogManager;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;

public class FingerprintManagerDialog implements Initializable {

    @FXML
    public TextField searchField;
    @FXML
    public Button openInEditorButton;
    @FXML
    public CheckBox loadOnStartCheckbox;
    @FXML
    public TableView<TableRow> table;
    @FXML
    public TableColumn colName;
    @FXML
    public TableColumn colAuthor;
    @FXML
    public TableColumn<TableRow,Boolean> colEnabled;
    @FXML
    public Button addFingerprintsButton;
    @FXML
    public Button selectAllButton;
    @FXML
    public Button removeSelectionButton;
    @FXML
    public Button discardChangesButton;
    @FXML
    public Button saveAndCloseButton;
    @FXML
    public Button saveButton;

    private final Logger logger = Logger.getLogger(FingerprintManagerDialog.class.getName());

    final static LogEmitter logEmitter = LogEmitter.factory.get();
    
    private final ObservableList<TableRow> data;
    
    public FingerprintManagerDialog() {
        this.data = FXCollections.observableArrayList();
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setButtonActions();
        initializeColumns();

        FilteredList<TableRow> filterList = new FilteredList<>(data, row -> true);
        searchField.textProperty().addListener((observable, oldVal, newVal) -> {
            filterList.setPredicate( tableRow -> {
                /** display all if no empty search-string */
                if( newVal.isEmpty() || !tableRow.preloaded ) {
                    return true;
                }
                String val = newVal.toLowerCase();
                return tableRow.getName().toLowerCase().contains(val);
            });
        });
        SortedList<TableRow> sortedList = new SortedList<>(filterList);
        sortedList.comparatorProperty().bind(table.comparatorProperty());
        table.setItems(sortedList);
        
        table.setEditable(true);
        table.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);

        table.setRowFactory(tableView -> {
            javafx.scene.control.TableRow<TableRow> row = new javafx.scene.control.TableRow<>();

            row.itemProperty().addListener((observable, oldValue, newValue) -> {
                if (newValue == null) {
                    //setColor(row, "#FFFFFF");
                    return;
                }

                if(!newValue.searchHit.get()) {
                    RowStyle.clear.setStyleClass(row);
                }
                else {

                    boolean loadAttempted = newValue.loadAttempted.get();
                    boolean loadSuccess = newValue.loadSuccess.get();

                    if (loadAttempted) {
                        if (loadSuccess) {
                            RowStyle.green.setStyleClass(row);
                        } else {
                            RowStyle.red.setStyleClass(row);
                        }
                    } else {
                        RowStyle.white.setStyleClass(row);
                    }
                }
            });

            return row;
        });
    }
    
    private void setButtonActions() {
        this.saveButton.setOnMouseClicked(this::saveList);
        this.loadOnStartCheckbox.setOnMouseClicked(this::toggleAutoLoad);
        this.addFingerprintsButton.setOnMouseClicked(this::chooseFiles);
        this.openInEditorButton.setOnMouseClicked((this::openSelectedInEditor));
        this.selectAllButton.setOnMouseClicked((this::selectAll));
        this.saveAndCloseButton.setOnMouseClicked(this::saveAndClose);
        this.removeSelectionButton.setOnMouseClicked(this::removeSelected);
        this.searchField.setOnKeyReleased(this::doSearch);
    }

    private void doSearch(KeyEvent keyEvent) {
        String searchString = this.searchField.getText();

        //Platform.runLater(() -> {
            data.parallelStream().forEach(tableRow ->  {
                if(tableRow.getName().toLowerCase().startsWith(searchString.toLowerCase())) {
                    tableRow.setSearchHit(true);
                }
                else {
                    tableRow.setSearchHit(false);
                }
            });
        //});
        refreshTable();
    }

    private void removeSelected(MouseEvent mouseEvent) {
        ArrayList<TableRow> toRemove = new ArrayList<>();
        toRemove.addAll(table.getSelectionModel().getSelectedItems().stream().collect(Collectors.toList()));
        this.data.removeAll(toRemove);
        refreshTable();
    }

    private void initializeColumns() {
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colAuthor.setCellValueFactory(new PropertyValueFactory<>("author"));
        //colEnabled.setCellValueFactory(new PropertyValueFactory<>("enabled"));
        colEnabled.setCellValueFactory(cellData -> {
            TableRow tableRow = cellData.getValue();
            SimpleBooleanProperty enabled = tableRow.getEnabled();
            enabled.addListener((observable, oldValue, newValue) -> {

            });
            return enabled;
        });
        colEnabled.setCellFactory(CheckBoxTableCell.forTableColumn(colEnabled));
        colEnabled.setEditable(true);
    }

    private enum RowStyle {
        green("row-loaded"),
        red("row-failed"),
        white("row-not-attempted"),
        blue("row-not-enabled"),
        clear("hide-row");

        private String styleClass;
        private static ArrayList<RowStyle> values = new ArrayList<>();

        static {
            RowStyle.values.addAll(Arrays.asList(RowStyle.values()));
        }

        RowStyle(String styleClass) {
            this.styleClass = styleClass;
        }

        public void setStyleClass(Node node) {
            RowStyle.values.stream().filter(this::otherStyle).forEach(oldStyle -> oldStyle.removeStyleClass(node));
            node.getStyleClass().add(this.styleClass);
        }

        private void removeStyleClass(Node node) {
            node.getStyleClass().remove(this.styleClass);
        }

        private boolean otherStyle(RowStyle style) {
            return style != this;
        }
    }

    public void setAutoLoad(boolean autoLoad) {
        Platform.runLater(()->{
            this.loadOnStartCheckbox.setSelected(autoLoad);
        });
    }

    private void toggleAutoLoad(MouseEvent mouseEvent) {
        Boolean b = this.loadOnStartCheckbox.isSelected();
        try {
            FManager.saveSetting(FManager.AUTOLOAD, b.toString());
        } catch (IOException ex) {
            this.logger.log(Level.SEVERE, null, ex);
            this.logEmitter.emit(this, ALERT.WARNING, "Could not set AUTOLOAD="+b.toString());
        }
    }

    private void selectAll(MouseEvent mouseEvent) {
        table.getSelectionModel().selectAll();
        data.stream().forEach(tableRow -> tableRow.setEnabled(true));
    }

    private void openSelectedInEditor(MouseEvent mouseEvent) {
        table.getSelectionModel().getSelectedItems().parallelStream().map(TableRow::getFile).forEach(FingerprintManagerDialog::openInEditor);
    }

    public static boolean openInEditor( File pathToXML ) {
        GrassMarlin.window.openFingerprintEditor(pathToXML.getAbsolutePath());
        return false;
    }
    
    /**
     * Route the {@link #addFingerprintsButton} onMouseClick event.
     *
     * @param e Event from the source of this methods invocation.
     */
    private void chooseFiles(Event e) {
        Window window = ((Node) e.getSource()).getScene().getWindow();
        chooseFiles(window).ifPresent(this::addFiles);
    }

    /**
     * Returns an optional list of Files selected by the user from a FileChooser
     *
     * @param win Window parent of the FileChooser.
     * @return Optionally a list of Files, if the user aborted selected the list
     * will not be present.
     */
    private Optional<List<File>> chooseFiles(Window win) {
        FileChooser fc = new FileChooser();
        fc.setTitle("Add Fingerprints");
        fc.setInitialDirectory(Environment.DIR_FPRINT_USER.getDir());
        return Optional.ofNullable(
                fc.showOpenMultipleDialog(win)
        );
    }

    public void setFpToDisplay(Map<File,Boolean> fpToDisplay) {
        this.data.clear();
        fpToDisplay.entrySet().stream()
                .map(entry -> new TableRow(entry.getKey(),entry.getValue()))
                .forEach(row -> {
                    if (!data.contains(row)) {
                        data.add(row);
                    }
                });
        maybePreload();
    }

    private void maybePreload() {
        data.parallelStream()
                .filter(TableRow::notPreloaded)
                .forEach(TableRow::preLoad);
        refreshTable();
    }

    private void refreshTable() {
        Platform.runLater(() -> {
            table.getColumns().get(0).setVisible(false);
            table.getColumns().get(0).setVisible(true);
        });
    }

    /**
     * Add multiple files to the table.
     * @param files Files to add.
     */
    private void addFiles(List<File> files) {
        files.stream()
                .flatMap(file -> {
                    if (file.isDirectory()) {
                        return Arrays.asList(file.listFiles()).stream();
                    } else {
                        return Stream.of(file);
                    }
                })
                .map(TableRow::new)
                .forEach(row -> {
                    if (!data.contains(row)) {
                        data.add(row);
                    }
                });
        maybePreload();
    }

    private void loadDataIntoGm() {
        GrassMarlin.window.manager.reload(data.stream().filter(TableRow::isEnabled).map(TableRow::getFile).collect(Collectors.toList()));
    }

    private void saveAndClose(MouseEvent mouseEvent) {
        saveList(mouseEvent);
        DialogManager.FMDialog(false);
    }

    private void saveList(MouseEvent mouseEvent) {
        if( data.isEmpty() ) return;
        loadDataIntoGm();
        try {
            GrassMarlin.window.manager.saveSettings(data.stream().collect(Collectors.toMap(row -> row.getAbsolutePath(), row -> row.getEnabled().get()+"")),this.loadOnStartCheckbox.isSelected());
        } catch (IOException e) {
            LogEmitter.factory.get().emit(this, ALERT.WARNING, "Fingerprints could not be saved.");
        }
    }
    
    public class TableRow {
        static final String LOADING = "Loading...";
        static final String FAILED = "Failed to load...";

        File file;
        FManager manager;
        private final SimpleStringProperty name;
        private final SimpleStringProperty author;
        private final SimpleBooleanProperty enabled;
        /**
         * False is not yet attempted to preLoad, true on attempt to preLoad.
         */
        private final SimpleBooleanProperty loadAttempted;
        private final SimpleBooleanProperty loadSuccess;
        private final SimpleBooleanProperty searchHit;
        private boolean preloaded;

        public TableRow(File file, boolean enabled) {
            this.file = file;
            this.name = new SimpleStringProperty(String.format("%s %s", LOADING, file.getName()));
            this.author = new SimpleStringProperty(LOADING);
            this.enabled = new SimpleBooleanProperty(enabled);
            this.loadAttempted = new SimpleBooleanProperty(false);
            this.loadSuccess = new SimpleBooleanProperty(false);
            this.searchHit = new SimpleBooleanProperty(true);
            this.preloaded = false;
        }

        public TableRow(File file) {
            this(file, false);
        }

        public String getAbsolutePath() {
            return file.getAbsolutePath();
        }

        public void setSearchHit(boolean searchHit) {
            this.searchHit.set(searchHit);
        }

        public File getFile() {
            return file;
        }

        public SimpleBooleanProperty getEnabled() {
            return enabled;
        }

        public SimpleBooleanProperty enabledProperty() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled.set(enabled);
        }

        public boolean isEnabled() {
            return enabled.get();
        }
        
        public boolean isLoadAttempted() {
            return loadAttempted.get();
        }

        public boolean isLoadSuccess() {
            return loadSuccess.get();
        }
        
        public String getName() {
            return name.get();
        }
        
        public String getAuthor() {
            return author.get();
        }
        
        public void preLoad() {
            Platform.runLater(this::preLoadInternal);
        }
        
        private void preLoadInternal() {
            preloaded = true;
            loadAttempted.set(true);
            try {
                
                JAXBContext jcx = FPC.getContext();
                Fingerprint fingerprint = (Fingerprint) jcx.createUnmarshaller().unmarshal(this.file);
                String $name = fingerprint.getHeader().getName();
                String $author = fingerprint.getHeader().getAuthor();
                
                name.set($name);
                author.set($author);
                //enabled.set(true);
                loadSuccess.set(true);
            } catch (JAXBException ex) {
                this.author.set(FAILED);
                this.name.set(this.name.get().replace(LOADING,""));
                LogEmitter.factory.get().emit(this, ALERT.DANGER, ex.getLocalizedMessage());
                Logger.getLogger(FingerprintManagerDialog.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
        
        public boolean notPreloaded() {
            return !preloaded;
        }
        
        @Override
        public int hashCode() {
            return this.file.hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            final TableRow other = (TableRow) obj;
            if (!Objects.equals(this.file, other.file)) {
                return false;
            }
            return true;
        }
        
    }

}
