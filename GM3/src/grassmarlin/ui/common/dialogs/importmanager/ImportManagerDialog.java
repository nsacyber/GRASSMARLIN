package grassmarlin.ui.common.dialogs.importmanager;

import com.sun.istack.internal.NotNull;
import grassmarlin.Logger;
import grassmarlin.RuntimeConfiguration;
import grassmarlin.common.FileUnits;
import grassmarlin.common.ListSizeBinding;
import grassmarlin.plugins.IPlugin;
import grassmarlin.session.ImportItem;
import grassmarlin.session.Session;
import grassmarlin.session.pipeline.PipelineTemplate;
import grassmarlin.session.serialization.Quicklist;
import grassmarlin.ui.common.controls.PipelineSelector;
import grassmarlin.ui.common.menu.ActiveMenuItem;
import grassmarlin.ui.common.menu.DynamicSubMenu;
import javafx.application.Platform;
import javafx.beans.Observable;
import javafx.beans.binding.BooleanExpression;
import javafx.beans.binding.When;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class ImportManagerDialog extends Dialog {
    private final PipelineSelector pipelineSelector;
    private final BooleanProperty canChangePipeline;
    private final TableView<ImportItem> tblImports;
    private final FileChooser dialogSelectFile;
    private final FileChooser dialogSaveLoadQuicklist;
    private final RuntimeConfiguration config;

    private Session session;

    public ImportManagerDialog(final RuntimeConfiguration config) {
        this.config = config;

        this.tblImports = new TableView<>();
        this.dialogSelectFile = new FileChooser();
        this.dialogSaveLoadQuicklist = new FileChooser();

        this.canChangePipeline = new SimpleBooleanProperty(false);
        this.pipelineSelector = new PipelineSelector();
        this.pipelineSelector.disableProperty().bind(this.canChangePipeline.not());

        initComponents();
    }

    @SuppressWarnings("unchecked")
    public void showAndWait(final Session session) {
        this.session = session;

        this.canChangePipeline.bind(session.canSetPipeline());
        this.pipelineSelector.setSession(session);
        this.pipelineSelector.getSelectionModel().select(session.pipelineTemplateProperty().get());
        tblImports.setItems(session.allImportsProperty());
        //We don't actually need the result since the only option is to close the dialog.
        super.showAndWait();
    }

    private void initComponents() {
        this.setResizable(true);
        this.setTitle("Import");
        RuntimeConfiguration.setIcons(this);

        HBox pipelineBox = new HBox(5);

        this.pipelineSelector.valueProperty().addListener(this::pipelineSelectionChanged);

        Label pipelineSelectionLabel = new Label("Select Pipeline");
        pipelineSelectionLabel.setLabelFor(pipelineSelector);
        pipelineBox.getChildren().addAll(pipelineSelectionLabel, pipelineSelector);

        this.dialogSelectFile.setTitle("Add Import File...");
        this.dialogSelectFile.getExtensionFilters().add(new FileChooser.ExtensionFilter("All Files", "*"));
        for(IPlugin.HasImportProcessors plugin : config.enumeratePlugins(IPlugin.HasImportProcessors.class)) {
            for(IPlugin.ImportProcessorWrapper wrapper : plugin.getImportProcessors()) {
                final List<String> extensions = Arrays.stream(wrapper.getExtensions()).map(extension -> "*" + extension).collect(Collectors.toList());
                final String title = String.format("%s Files", wrapper.getName());
                dialogSelectFile.getExtensionFilters().add(new FileChooser.ExtensionFilter(title, extensions));
            }
        }

        this.dialogSaveLoadQuicklist.setTitle("Quicklist");
        this.dialogSaveLoadQuicklist.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Quicklists", "*.g3", "*.gm3ql"),
                new FileChooser.ExtensionFilter("All Files", "*")
        );

        final TableColumn<ImportItem, String> colProgress = new TableColumn<>("Progress");
        colProgress.setCellValueFactory(param -> new When(param.getValue().progressProperty().greaterThanOrEqualTo(0.0))
                    .then(param.getValue().progressProperty().multiply(100.0).asString("%4f%%"))
                    .otherwise("")
        );

        final TableColumn<ImportItem, Path> colFile = new TableColumn<>("File");
        colFile.setCellValueFactory(param -> new ReadOnlyObjectWrapper<>(param.getValue().getPath()));

        final TableColumn<ImportItem, FileUnits.FileSize> colSize = new TableColumn("Size");
        colSize.setCellValueFactory(param -> param.getValue().displaySizeProperty());

        final TableColumn<ImportItem, String> colImporter = new TableColumn("Importer");
        colImporter.setCellValueFactory(param -> param.getValue().importerPluginNameProperty().concat(": ").concat(param.getValue().importerFunctionNameProperty()));

        final TableColumn<ImportItem, String> colEntryPoint = new TableColumn<>("Pipeline Entry");
        colEntryPoint.setCellValueFactory(param -> param.getValue().pipelineEntryProperty());

        tblImports.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        tblImports.getColumns().addAll(colProgress, colFile, colSize, colImporter, colEntryPoint);
        tblImports.setRowFactory(view -> {
            final TableRow<ImportItem> row = new TableRow<>();
            final ContextMenu menuRow = new ContextMenu();

            final MenuItem miRemove = new ActiveMenuItem("Remove", event -> {
                ImportManagerDialog.this.removeImportItem(row.getItem());
            });
            final MenuItem miSetType = new DynamicSubMenu("Set Importer", null, () -> {
                return config.enumeratePlugins(IPlugin.HasImportProcessors.class).stream().map(plugin -> {
                    final Menu item = new Menu(plugin.getName());

                    final Collection<IPlugin.ImportProcessorWrapper> wrappers = plugin.getImportProcessors();
                    if(!wrappers.isEmpty()) {
                        item.getItems().addAll(
                                wrappers.stream().map(importer -> new ActiveMenuItem(importer.getName(), event -> {
                                    row.getItem().importerPluginNameProperty().set(config.pluginNameFor(plugin.getClass()));
                                    row.getItem().importerFunctionNameProperty().set(importer.getName());
                                })).collect(Collectors.toList())
                        );
                    } else {
                        item.setDisable(true);
                    }

                    return (MenuItem)item;
                }).filter(item -> item != null).collect(Collectors.toList());
            });
            final MenuItem miImport = new ActiveMenuItem("Import", event -> {
                ImportManagerDialog.this.beginImport(row.getItem());
            });
            final MenuItem miSetEntry = new DynamicSubMenu("Set Pipeline Entry", null, () ->
                session.getPipelineEntryPoints().stream().map(entry -> {
                    final CheckMenuItem ckResult = new CheckMenuItem(entry);
                    ckResult.setSelected(entry.equals(row.getItem().pipelineEntryProperty().get()));
                    ckResult.setOnAction(action -> row.getItem().pipelineEntryProperty().set(entry));
                    return ckResult;
                }).collect(Collectors.toList())
            );

            menuRow.getItems().addAll(miSetType, miRemove, miImport, miSetEntry);

            row.contextMenuProperty().bind(new When(row.emptyProperty()).then((ContextMenu)null).otherwise(menuRow));
            menuRow.setOnShowing(event -> {
                if(row.getItem().importStartedProperty().get()) {
                    for(final MenuItem item : menuRow.getItems()) {
                        item.setDisable(true);
                    }
                } else {
                    for(final MenuItem item : menuRow.getItems()) {
                        item.setDisable(false);
                    }
                }
            });
            return row;
        });

        final BooleanExpression isNothingSelected = new ListSizeBinding(tblImports.getSelectionModel().getSelectedCells()).isEqualTo(0);

        final Button btnAddFile = new Button("Add File");
        btnAddFile.setOnAction(event -> {
            final File fileSelected = dialogSelectFile.showOpenDialog(this.getOwner());
            if(fileSelected != null) {
                final Path path = Paths.get(fileSelected.getAbsolutePath());
                final IPlugin.ImportProcessorWrapper processor = config.importerForFile(path);  //This might be null, but the ImportItem can handle that, sort of.
                final IPlugin plugin = config.pluginFor(processor.getClass());
                if(plugin instanceof IPlugin.HasImportProcessors) {
                    final ImportItem.FromPlugin item = new ImportItem.FromPlugin(path, session.getDefaultPipelineEntry());
                    item.importerPluginNameProperty().set(config.pluginNameFor(processor.getClass()));
                    item.importerFunctionNameProperty().set(processor.getName());
                    this.addImportItem(item);
                } else {
                    Logger.log(Logger.Severity.ERROR, "Error with plugin " + plugin.getName() + ": ");
                }
            }
        });
        final Button btnLoadQuicklist = new Button("Load Quicklist");
        btnLoadQuicklist.setOnAction(event -> {
            final File file = this.dialogSaveLoadQuicklist.showOpenDialog(getOwner());
            if(file != null) {
                try {
                    final FileInputStream stream = new FileInputStream(file);
                    final Quicklist quicklist = new Quicklist();
                    quicklist.readFromStream(this.config, stream);
                    for(final ImportItem item : quicklist.getItems()) {
                        this.addImportItem(item);
                    }
                } catch(IOException ex) {
                    Logger.log(Logger.Severity.ERROR, "There was an error loading the quicklist (%s): %s", file, ex.getMessage());
                }
            }
        });
        final Button btnSaveQuicklist = new Button("Save Quicklist");
        btnSaveQuicklist.setOnAction(event -> {
            final File file = this.dialogSaveLoadQuicklist.showSaveDialog(getOwner());
            if(file != null) {
                final Quicklist quicklist = new Quicklist();
                final int cntItems = tblImports.getSelectionModel().getSelectedItems().size();
                if(cntItems == 0) {
                    //We shouldn't be able to reach this, but if we did, the appropriate action is to do nothing--we're going to write a message anyway
                    Logger.log(Logger.Severity.WARNING, "A Quicklist was saved with no content (%s)", file.getName());
                } else if(cntItems == 1) {
                    //Handle edge case where getSelectedItems() returns a list containing null when a single item is present
                    // (That is what we think is happening; we haven't pinned down the root cause yet)
                    quicklist.getItems().add(tblImports.getSelectionModel().getSelectedItem());
                } else {
                    quicklist.getItems().addAll(tblImports.getSelectionModel().getSelectedItems());
                }
                try {
                    quicklist.writeToStream(new FileOutputStream(file));
                } catch(IOException ex) {
                    Logger.log(Logger.Severity.ERROR, "There was an error saving the quicklist (%s): %s", file, ex.getMessage());
                }
            }
        });
        btnSaveQuicklist.disableProperty().bind(isNothingSelected);
        final Button btnStartImport = new Button("Begin Import");
        btnStartImport.setOnAction(event -> {
            //We operate on a copy largely for legacy reasons.
            //HACK: There is a fairly common use case where calling getSelectedItems() returns a list containing null.  Calling getSelectedIndices() before calling getSelectedItems() returns a list containing -1, but the subsequent call to getSelectedItems will return the correct content.  Because of course that is how this should behave.
            final List<Integer> indices = new ArrayList<>(tblImports.getSelectionModel().getSelectedIndices());
            final List<ImportItem> items = new ArrayList<>(tblImports.getSelectionModel().getSelectedItems());
            for(ImportItem item : items) {
                if(item != null) {
                    ImportManagerDialog.this.beginImport(item);
                } else {
                    Logger.log(Logger.Severity.WARNING, "Cannot import a null item.");
                }
            }
        });
        btnStartImport.disableProperty().bind(isNothingSelected);

        final VBox containerWindow = new VBox();
        final HBox containerButtons = new HBox();
        containerButtons.setSpacing(8.0);
        containerButtons.getChildren().addAll(btnAddFile, btnLoadQuicklist, btnSaveQuicklist, btnStartImport);
        containerWindow.getChildren().addAll(pipelineBox, tblImports, containerButtons);

        this.getDialogPane().setContent(containerWindow);
        this.getDialogPane().getButtonTypes().addAll(ButtonType.CLOSE);
    }

    protected void beginImport(@NotNull final ImportItem item) {
        if(item.importStartedProperty().get()) {
            Logger.log(Logger.Severity.WARNING, "Unable to begin import of '%s': Import is already %s", item, item.progressProperty().getValue().doubleValue() == 1.0 ? "complete." : "running.");
        } else {
            session.processImport(item);
        }
    }

    protected void addImportItem(final ImportItem item) {
        session.allImportsProperty().add(item);
        Platform.runLater(() -> {
            tblImports.getSelectionModel().select(item);
        });
    }

    protected void removeImportItem(final ImportItem item) {
        if(!item.importStartedProperty().get()) {
            session.allImportsProperty().remove(item);
        } else {
            Logger.log(Logger.Severity.INFORMATION, "Unable to remove Import: An import that has started can only be removed by a Clear Topology command.");
        }
    }

    protected void pipelineSelectionChanged(Observable observable, PipelineTemplate oldValue, PipelineTemplate newValue) {
        if (this.canChangePipeline.get()) {
            this.session.pipelineTemplateProperty().set(newValue);
        }
    }
}
