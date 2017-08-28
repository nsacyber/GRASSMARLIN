package grassmarlin.ui.sdi;

import grassmarlin.Event;
import grassmarlin.*;
import grassmarlin.plugins.IPlugin;
import grassmarlin.plugins.internal.AggregatePlugin;
import grassmarlin.plugins.internal.livepcap.LivePcapImport;
import grassmarlin.plugins.internal.livepcap.PcapEngine;
import grassmarlin.plugins.internal.livepcap.Plugin;
import grassmarlin.session.ImportItem;
import grassmarlin.session.Session;
import grassmarlin.session.pipeline.PipelineTemplate;
import grassmarlin.ui.common.TabController;
import grassmarlin.ui.common.controls.LogViewer;
import grassmarlin.ui.common.controls.PipelineSelector;
import grassmarlin.ui.common.controls.SourceSelector;
import grassmarlin.ui.common.dialogs.about.AboutDialog;
import grassmarlin.ui.common.dialogs.about.AboutPluginDialog;
import grassmarlin.ui.common.dialogs.importmanager.ImportManagerDialog;
import grassmarlin.ui.common.dialogs.networks.NetworkDialog;
import grassmarlin.ui.common.dialogs.pluginconflict.PluginConflict;
import grassmarlin.ui.common.dialogs.pluginconflict.PluginConflictDialog;
import grassmarlin.ui.common.dialogs.preferences.PreferenceDialog;
import grassmarlin.ui.common.dialogs.preferences.PreferenceValues;
import grassmarlin.ui.common.dialogs.releasenotes.ReleaseNotesDialog;
import grassmarlin.ui.common.menu.ActiveMenuItem;
import grassmarlin.ui.common.menu.ColorSelectionMenuItem;
import grassmarlin.ui.common.menu.DynamicSubMenu;
import grassmarlin.ui.common.tasks.AsyncTaskQueue;
import grassmarlin.ui.common.tasks.AsyncUiTask;
import grassmarlin.ui.common.tasks.LoadingTask;
import grassmarlin.ui.common.tasks.SavingTask;
import grassmarlin.ui.dev_temp.TestingDialog;
import grassmarlin.ui.pipeline.PipelineDialog;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.binding.When;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Orientation;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.input.Dragboard;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Optional;
import java.util.stream.Collectors;

public class MainWindow extends Application {
    public static void launchFx(String[] args) {
        Application.launch(args);
    }

    // == Dialogs ==
    protected final FileChooser dialogSaveSession;
    protected final FileChooser dialogLoadSession;

    protected final PluginConflictDialog dialogPluginConflicts;
    protected final ImportManagerDialog dialogImportManager;

    protected final PipelineDialog dialogPipeline;

    protected final AboutDialog dialogAbout;
    protected final AboutPluginDialog dialogAboutPlugin;

    // == Layout & Controls==
    private Stage stage;
    private final BorderPane layoutPrimary;
    private final Pane paneNavigation;
    private final TabPane paneTabs;
    private final SimpleObjectProperty<TabController> tabController;

    // == Application State
    private final RuntimeConfiguration config;
    private final SimpleObjectProperty<Session> session;
    private final DocumentState state;
    private final AsyncTaskQueue taskQueue;
    private final PcapEngine pcapEngine;

    public MainWindow() {
        this.config = Launcher.getConfiguration();
        this.session = new SimpleObjectProperty<>();
        this.state = new DocumentState(this.session);

        this.taskQueue = new AsyncTaskQueue(Event.PROVIDER_JAVAFX);
        this.pcapEngine = Plugin.getPcapEngine();   //HACK: This should be an instance member, which means finding the right plugin object.

        this.dialogSaveSession = new FileChooser();
        this.dialogLoadSession = new FileChooser();
        this.dialogPluginConflicts = new PluginConflictDialog();
        this.dialogImportManager = new ImportManagerDialog(this.config);
        this.dialogPipeline = new PipelineDialog(this.config, this.session.get());

        this.dialogAbout = new AboutDialog(this.config);
        this.dialogAboutPlugin = new AboutPluginDialog(this.config);

        this.layoutPrimary = new BorderPane();
        this.paneNavigation = new Pane();
        this.paneTabs = new TabPane();
        this.tabController = new SimpleObjectProperty<>();
        this.tabController.addListener((observable, oldValue, newValue) -> {
            if(oldValue != null) {
                oldValue.attachToUi(null, null);
            }
            if(newValue != null) {
                newValue.attachToUi(paneNavigation, paneTabs);
            }
        });
    }

    private final void doBreakpoint() {
        return;
    }

    private void initComponents() {
        stage.titleProperty().bind(new ReadOnlyStringWrapper(Version.APPLICATION_TITLE + " [").concat(this.state.currentSessionTitleProperty()).concat(new When(this.state.dirtyProperty()).then("]*").otherwise("]")));
        stage.getIcons().add(RuntimeConfiguration.getApplicationIcon32());
        stage.getIcons().add(RuntimeConfiguration.getApplicationIcon16());
        stage.setOnCloseRequest(e -> Platform.exit());


        dialogSaveSession.setTitle("Save Session");
        dialogSaveSession.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("GrassMarlin 3.3 Session (*.gm3)", "*.gm3"),
                new FileChooser.ExtensionFilter("All Files)", "*")
        );
        dialogLoadSession.setTitle("Load Session");
        dialogLoadSession.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("GrassMarlin 3.3 Session (*.gm3)", "*.gm3"),
                new FileChooser.ExtensionFilter("All Files)", "*")
        );

        //Drag-and-drop files to start import:
        layoutPrimary.setOnDragOver(event -> {
            if (event.getGestureSource() != this && event.getDragboard().hasFiles()) {
                event.acceptTransferModes(TransferMode.COPY_OR_MOVE);
            }
            event.consume();
        });
        layoutPrimary.setOnDragDropped(event -> {
            Dragboard board = event.getDragboard();

            if (board.hasFiles()) {
                for (File file : board.getFiles()) {
                    Path path = Paths.get(file.toString());
                    final Session sessionCurrent = session.get();
                    if(sessionCurrent != null) {
                        IPlugin.ImportProcessorWrapper wrapper = config.importerForFile(path);
                        if(wrapper != null) {
                            final ImportItem.FromPlugin item = new ImportItem.FromPlugin(path, sessionCurrent.getDefaultPipelineEntry());
                            item.importerPluginNameProperty().set(config.pluginNameFor(wrapper.getClass()));
                            item.importerFunctionNameProperty().set(wrapper.getName());
                            sessionCurrent.processImport(item);
                        } else {
                            Logger.log(Logger.Severity.WARNING, "Unable to import file '%s': No handler for the file type is known.", path.getFileName().toString());
                        }
                    } else {
                        Logger.log(Logger.Severity.WARNING, "Unable to import file '%s': No session available.", path.getFileName().toString());
                    }
                }
                event.setDropCompleted(true);
            } else {
                event.setDropCompleted(false);
            }
            event.consume();
        });

        final VBox containerHeader = new VBox();
        final MenuBar menu = new MenuBar();

        /*
        File
            New Session
            Load Session...
            Save Session
            Save Session As...
            -
            Clear session
            Import Files...
            -
            Export >
            -
            Exit
        Help
            User Guide
            About
        Developer
            Debug
            Output Session
         */
        final Menu menuDev = new Menu("_Developer");
        menuDev.getItems().addAll(
                new ActiveMenuItem("_Debug", action -> {
                    System.err.println("TODO: Set a breakpoint on this line.");
                }),
                new ActiveMenuItem("Debug (2)", action -> {
                    MainWindow.this.doBreakpoint();
                }),
                new ActiveMenuItem("Output _Session", action -> {
                    System.out.println(session.get());
                }),
                new ActiveMenuItem("Output Ph_ysical Graph Data", action -> {
                    System.out.println(((AggregatePlugin)config.pluginFor(this.getClass())).getMember(grassmarlin.plugins.internal.physicalview.Plugin.class).stateForSession(session.get()).graph.toString());
                }),
                new ActiveMenuItem("Output _Profiler Data", action -> {
                    Profiler.dumpProfilerData();
                }),
                new ActiveMenuItem("_Reset Profiler Data", action -> {
                    Profiler.reset();
                }),
                new Menu("Generate Log Message") {
                    {
                        this.getItems().addAll(
                                new ActiveMenuItem("_Developer", event -> {
                                    Logger.log(Logger.Severity.PEDANTIC_DEVELOPER_SPAM, "This is a test message (PEDANTIC_DEVELOPER_SPAM)");
                                }),
                                new ActiveMenuItem("_Information", event ->  {
                                    Logger.log(Logger.Severity.INFORMATION, "This is a test message (INFORMATION)");
                                }),
                                new ActiveMenuItem("_Completion", event ->  {
                                    Logger.log(Logger.Severity.COMPLETION, "This is a test message (COMPLETION)");
                                }),
                                new ActiveMenuItem("_Warning", event ->  {
                                    Logger.log(Logger.Severity.WARNING, "This is a test message (WARNING)");
                                }),
                                new ActiveMenuItem("_Error", event ->  {
                                    Logger.log(Logger.Severity.ERROR, "This is a test message (ERROR)");
                                })
                        );
                    }
                },
                new ActiveMenuItem("Show Plugin Conflict Dialog (blank)", event -> {
                    dialogPluginConflicts.showAndWait(new ArrayList<>());
                }),
                new ActiveMenuItem("Show Plugin Conflict Dialog (conflicted)", event -> {
                    dialogPluginConflicts.showAndWait(Arrays.asList(
                            new PluginConflict("Plugin:Missing", PluginConflict.Conflict.Missing),
                            new PluginConflict("Plugin:New", PluginConflict.Conflict.New),
                            new PluginConflict("Plugin:MissingWithData", PluginConflict.Conflict.MissingWithData),
                            new PluginConflict("Plugin:NewWithoutData", PluginConflict.Conflict.NewWithoutData),
                            new PluginConflict("Plugin:UnexpectedData", PluginConflict.Conflict.UnexpectedData),
                            new PluginConflict("Plugin:MissingData", PluginConflict.Conflict.MissingData),
                            new PluginConflict("Plugin:VersionMismatch", PluginConflict.Conflict.VersionMismatch),
                            new PluginConflict("Plugin:VersionMismatchWithData", PluginConflict.Conflict.VersionMismatchWithData)
                    ));
                }),
                new ColorSelectionMenuItem(new SimpleObjectProperty<>(Color.RED), "Set color (1)..."),
                new ColorSelectionMenuItem(new SimpleObjectProperty<>(Color.BLUE), "Set color (2)..."),
                new ActiveMenuItem("Triangle Debugger", event -> {
                    new TestingDialog().showAndWait();
                })
        );
        menuDev.visibleProperty().bind(config.isDeveloperModeProperty());
        menu.getMenus().addAll(
                new Menu("_File") {
                    {
                        this.getItems().addAll(
                                new ActiveMenuItem("_New Session", action -> {
                                    newSession();
                                }),
                                new ActiveMenuItem("_Load Session...", action -> {
                                    loadSession();
                                }),
                                new ActiveMenuItem("_Save Session", action -> {
                                    saveSessionThen(null);
                                }),
                                new ActiveMenuItem("Save Session _As...", action -> {
                                    saveSessionAs(null);
                                }),
                                new SeparatorMenuItem(),
                                new ActiveMenuItem("_Import", action -> {
                                    dialogImportManager.showAndWait(session.get());
                                }).bindEnabled(session.isNotNull()),
                                new SeparatorMenuItem(),
                                new ActiveMenuItem("E_xit", action -> {
                                    MainWindow.this.stage.close();
                                })
                        );
                    }
                },
                new Menu("_View") {
                    {
                        this.getItems().addAll(
                                new ActiveMenuItem("Current _Log File", action -> {
                                    try {
                                        String viewerLog = RuntimeConfiguration.getPersistedString(RuntimeConfiguration.PersistableFields.TEXT_EDITOR_EXEC);
                                        String pathLog = Launcher.getLogFilePath().toString();
                                        Runtime.getRuntime().exec(new String[] {
                                                viewerLog,
                                                pathLog
                                        });
                                        Logger.log(Logger.Severity.COMPLETION, "displaying Log file (%s) using %s", pathLog, viewerLog);
                                    } catch(IOException | NullPointerException ex) {
                                        Logger.log(Logger.Severity.ERROR, "Unable to display log file; Ensure the Text File viewer is correctly set in the Preferences (%s)", ex.getMessage());
                                    }
                                }),
                                new ActiveMenuItem("Open Capture _Folder", event -> {
                                    String pathCapture = RuntimeConfiguration.getPersistedString(RuntimeConfiguration.PersistableFields.DIRECTORY_LIVE_CAPTURE);
                                    try {
                                        Desktop.getDesktop().open(new File(pathCapture));
                                    } catch (IOException ex) {
                                        Logger.log(Logger.Severity.ERROR, "Unable to open live capture folder (%s): %s", pathCapture, ex.getMessage());
                                    }
                                })
                        );
                    }
                },
                new Menu("_Session") {
                    {
                        this.getItems().addAll(
                                new ActiveMenuItem("_Manage Networks", event -> {
                                    new NetworkDialog(MainWindow.this.config).showAndWait(MainWindow.this.session.get().getRawNetworkList());
                                }),
                                new Menu("Live _Pcap") {
                                    {
                                        this.getItems().addAll(
                                                new DynamicSubMenu("Pipeline Entry", null, () -> {
                                                    return session.get().getPipelineEntryPoints().stream().map(entry -> {
                                                        final CheckMenuItem item = new CheckMenuItem(entry);
                                                        item.setSelected(session.get().livePcapEntryPointProperty().get().equals(entry));
                                                        item.setOnAction(action -> {
                                                            session.get().livePcapEntryPointProperty().set(entry);
                                                        });
                                                        return item;
                                                    }).collect(Collectors.toList());
                                                }).bindEnabled(MainWindow.this.config.allowLivePcapProperty()),
                                                new DynamicSubMenu("_Start Live Pcap", null, () ->
                                                        pcapEngine.getDeviceList().stream().map(device ->
                                                                new ActiveMenuItem(device.toString(), action -> session.getValue().processImport(new LivePcapImport(session.getValue().getDefaultPipelineEntry(), device)))
                                                        ).collect(Collectors.toList())
                                                ).bindEnabled(MainWindow.this.config.allowLivePcapProperty().and(pcapEngine == null ? new ReadOnlyBooleanWrapper(false) : pcapEngine.pcapRunningProperty().not())),
                                                new ActiveMenuItem("Sto_p Live Pcap", action -> {
                                                    pcapEngine.stop();
                                                }).bindEnabled(MainWindow.this.config.allowLivePcapProperty().and(pcapEngine == null ? new ReadOnlyBooleanWrapper(false) : pcapEngine.pcapRunningProperty()))
                                        );
                                    }
                                }
                        );
                    }
                },
                new Menu("_Options") {
                    {
                        this.getItems().addAll(
                                new Menu("_Pipeline Editor") {
                                    {
                                        this.getItems().addAll(
                                                new ColorSelectionMenuItem(config.colorPipelineBackgroundProperty(), "Background Color..."),
                                                new ColorSelectionMenuItem(config.colorPipelineTextProperty(), "Text Color...")
                                        );
                                    }
                                },
                                new SeparatorMenuItem(),
                                new ActiveMenuItem("_Preferences", event -> {
                                    final PreferenceValues initialState = new PreferenceValues();
                                    final PreferenceDialog<PreferenceValues> dlg = new PreferenceDialog(config, initialState);
                                    if(dlg.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
                                        dlg.getPreferences().apply();
                                    }
                                })
                        );
                    }
                },
                new Menu("_Tools") {
                    {
                        this.getItems().addAll(
                                new ActiveMenuItem("Pipeline Editor", event -> {
                                    dialogPipeline.show();
                                    dialogPipeline.toFront();
                                })
                        );
                    }
                },
                new Menu("_Plugins") {
                    {
                        this.getItems().addAll(
                                config.enumeratePlugins(IPlugin.class).stream().map(plugin -> {
                                    final Collection<MenuItem> result = plugin.getMenuItems();
                                    final Menu menu = new Menu(plugin.getName());
                                    if(result != null) {
                                        menu.getItems().addAll(result);
                                    }

                                    menu.getItems().add(new ActiveMenuItem("_About", action -> {
                                        MainWindow.this.dialogAboutPlugin.pluginProperty().set(plugin);
                                        MainWindow.this.dialogAboutPlugin.showAndWait();
                                    }));
                                    return menu;
                                }).collect(Collectors.toList())
                        );
                    }
                },
                new Menu("_Help") {
                    {
                        this.getItems().addAll(
                                new ActiveMenuItem("_User Guide", action -> {
                                    //TODO: Show user guide.  Were we sticking with PDF or moving to HTML?
                                }).bindEnabled(new ReadOnlyBooleanWrapper(false)),
                                new ActiveMenuItem("_Release Notes", action -> {
                                    new ReleaseNotesDialog().showAndWait();
                                }),
                                new ActiveMenuItem("_About", action -> {
                                    dialogAbout.showAndWait();
                                })
                        );
                    }
                },
                menuDev
        );

        final ToolBar toolbar = new ToolBar();
        //TODO: Add new/open/save/import/Pipeline selection buttons to toolbar
        final Pane spacerToolbar = new Pane();
        HBox.setHgrow(spacerToolbar, Priority.ALWAYS);
        final SourceSelector uiLivePcap = new SourceSelector(this.pcapEngine, session);

        //TODO: PipelineSelector should expose properties and move the events to be internal
        final Label labelPipelineSelector = new Label("Pipeline: ");
        final PipelineSelector pipelineSelector = new PipelineSelector();
        labelPipelineSelector.setLabelFor(pipelineSelector);
        pipelineSelector.disableProperty().bind(this.state.currentSessionCanChangePipelineProperty().not());
        pipelineSelector.setSession(session.get());
        this.session.addListener((observable, oldValue, newValue) -> {
            pipelineSelector.setSession(newValue);
            if(newValue != null) {
                pipelineSelector.getSelectionModel().select(newValue.pipelineTemplateProperty().get());
            } else {
                pipelineSelector.getSelectionModel().clearSelection();
            }
        });

        pipelineSelector.valueProperty().addListener(this.pipelineSelectionChanged);

        toolbar.getItems().addAll(labelPipelineSelector, pipelineSelector, spacerToolbar, uiLivePcap);

        containerHeader.getChildren().addAll(menu, toolbar);

        final ToolBar statusbar = new ToolBar();
        //EXTEND: Add elements to statusbar
        final Text nameCurrentTask = new Text();
        final ProgressBar progressCurrentTask = new ProgressBar();
        final Text descriptionCurrentSubtask = new Text();

        nameCurrentTask.textProperty().bind(taskQueue.taskProperty());
        progressCurrentTask.progressProperty().bind(taskQueue.progressProperty());
        descriptionCurrentSubtask.textProperty().bind(taskQueue.subtaskProperty());
        nameCurrentTask.visibleProperty().bind(taskQueue.hasTaskProperty());
        progressCurrentTask.visibleProperty().bind(taskQueue.hasTaskProperty());
        descriptionCurrentSubtask.visibleProperty().bind(taskQueue.hasTaskProperty());

        statusbar.getItems().addAll(nameCurrentTask, progressCurrentTask, descriptionCurrentSubtask);


        final SplitPane paneContent = new SplitPane();
        final SplitPane paneLeftContent = new SplitPane();
        paneLeftContent.setOrientation(Orientation.VERTICAL);

        final LogViewer paneLog = new LogViewer(config);
        paneLeftContent.getItems().addAll(paneNavigation, paneLog);

        paneContent.getItems().addAll(paneLeftContent, paneTabs);

        stage.setOnCloseRequest(event -> {
            if(this.state.dirtyProperty().get() && !config.isDeveloperModeProperty().get()) {
                final String titleForPrompt = state.currentSessionPathProperty().get() == null ? " New Session" : Paths.get(state.currentSessionPathProperty().get()).getFileName().toString();
                final Optional<ButtonType> result = new Alert(Alert.AlertType.CONFIRMATION, "Do you want to save changes to " + titleForPrompt + "?", ButtonType.YES, ButtonType.NO, ButtonType.CANCEL).showAndWait();
                if(result.isPresent() && result.get() != ButtonType.CANCEL) {
                    if(result.get() == ButtonType.YES) {
                        //Saving will be asynchronous
                        saveSessionThen((self) -> stage.close());
                        event.consume();
                    } else if(result.get() == ButtonType.NO) {
                        //Close
                    }
                } else {
                    event.consume();
                }
            } else {
                //Close
            }
        });

        //We have to run this after a layout pass if we want it to be right.
        Platform.runLater(() -> {
            paneLeftContent.setDividerPositions(0.8);
            paneContent.setDividerPositions(0.2);
        });

        layoutPrimary.setTop(containerHeader);
        layoutPrimary.setCenter(paneContent);
        layoutPrimary.setBottom(statusbar);
    }

    protected final ChangeListener<PipelineTemplate> pipelineSelectionChanged = new ChangeListener<PipelineTemplate>() {
        @Override
        public void changed(ObservableValue<? extends PipelineTemplate> observable, PipelineTemplate oldValue, PipelineTemplate newValue) {
            if(MainWindow.this.state.currentSessionCanChangePipelineProperty().get()) {
                MainWindow.this.session.get().pipelineTemplateProperty().set(newValue);
            }
        }
    };


    public void start(Stage stage) {
        this.stage = stage;
        stage.setTitle(Version.APPLICATION_TITLE);
        //stage.getIcons().add(EmbeddedIcons.Logo.getRawImage());
        stage.setOnCloseRequest(e -> Platform.exit());

        stage.setMaximized(true);
        initComponents();

        stage.setScene(new Scene(layoutPrimary));
        stage.show();

        Platform.runLater(this::newSession);
        if(!config.suppressUnchangedVersionNotesProperty().get() || !config.lastLoadedVersionProperty().get().equals(Version.APPLICATION_VERSION)) {
            Platform.runLater(() -> {
                new ReleaseNotesDialog().showAndWait();
            });
        }
        config.lastLoadedVersionProperty().set(Version.APPLICATION_VERSION);
    }

    // == Session Management (new/close/load/save/etc.)
    //Note to future self:  This section was written while the new guys were
    // nattering on about politics and abortions (again).  If anything looks
    // wrong, it probably is.
    //Note to the new guys:  Nobody cares.
    //Note to everyone else:  Putting developers in an open-floorplan office
    // is a very easy way to reduce their productivity, unless you measure
    // productivity by some metric proportional to bug count.

    public void newSession() {
        if(this.state.dirtyProperty().get()) {
            final String titleForPrompt = state.currentSessionPathProperty().get() == null ? " New Session" : Paths.get(state.currentSessionPathProperty().get()).getFileName().toString();
            final Optional<ButtonType> result = new Alert(Alert.AlertType.CONFIRMATION, "Do you want to save changes to " + titleForPrompt + "?", ButtonType.YES, ButtonType.NO, ButtonType.CANCEL).showAndWait();
            if(result.isPresent() && result.get() != ButtonType.CANCEL) {
                if(result.get() == ButtonType.YES) {
                    saveSessionThen((self) -> displaySession(new Session(config), new TabController(),  true));
                } else if(result.get() == ButtonType.NO) {
                    displaySession(new Session(config), new TabController(), true);
                }
            } else {
                //Canceled -- ignore
            }
        } else {
            displaySession(new Session(config), new TabController(), true);
        }
    }

    /**
     * Sets the currently-displayed session to the session provided.
     *
     * The DocumentState will be marked as clean at the end.
     *
     * @param session
     * @param tabsNew
     * @param isNew
     */
    protected void displaySession(final Session session, final TabController tabsNew, final boolean isNew) {
        //Inform plugins that the old session (if one exists) is closing.
        if(this.session.get() != null) {
            config.enumeratePlugins(IPlugin.SessionEventHooks.class).forEach(plugin -> {
                plugin.sessionClosed(MainWindow.this.session.get());
            });
        }

        //The DocumentState (state) monitors the session property and updates its event hooks accordingly.
        this.session.set(session);

        //Update the tab controller; we don't just modify it live because failure while loading would corrupt it; we only change on completion.
        if(this.tabController.get() != null) {
            this.tabController.get().clear();
        }
        this.tabController.set(tabsNew);

        //If this is a new session, fire the Plugins' new-session handler here.
        //If it is not new, we already called the load handler while loading.
        if(isNew) {
            config.enumeratePlugins(IPlugin.SessionEventHooks.class).forEach(plugin -> {
                plugin.sessionCreated(session, tabController.get());
            });
            state.currentSessionPathProperty().set(null);
        }

        this.state.dirtyProperty().set(false);
    }

    protected void loadSessionFromPath(final Path pathToLoad) {
        if(pathToLoad != null) {
            taskQueue.enqueue(new LoadingTask(config, pathToLoad, (loader) -> {
                displaySession(loader.getSession(), loader.getTabController(), false);
                state.currentSessionPathProperty().set(pathToLoad.toString());
            }, (self) -> {
                state.currentSessionPathProperty().set(null);
            }));
        }
    }
    public void loadSession() {
        if(this.state.dirtyProperty().get()) {
            //Prompt to save, then load
            final String titleForPrompt = state.currentSessionPathProperty().get() == null ? " New Session" : Paths.get(state.currentSessionPathProperty().get()).getFileName().toString();
            final Optional<ButtonType> result = new Alert(Alert.AlertType.CONFIRMATION, "Do you want to save changes to " + titleForPrompt + "?", ButtonType.YES, ButtonType.NO, ButtonType.CANCEL).showAndWait();
            if(result.isPresent() && result.get() != ButtonType.CANCEL) {
                if(result.get() == ButtonType.YES) {
                    saveSessionThen((saver) -> {
                        loadSessionFromPath(getPathToLoad());
                    });
                } else if(result.get() == ButtonType.NO) {
                    loadSessionFromPath(getPathToLoad());
                }
            } else {
                // Canceled -- ignore
            }
        } else {
            loadSessionFromPath(getPathToLoad());
        }
    }

    private Path getPathToLoad() {
        final File file = dialogLoadSession.showOpenDialog(stage);
        if(file == null) {
            return null;
        }
        return Paths.get(file.getAbsolutePath());
    }

    public void saveSessionThen(final AsyncUiTask.TaskCallback<SavingTask> then) {
        if(session.get() != null) {
            if(state.currentSessionPathProperty().get() == null) {
                saveSessionAs(then);
            } else {
                taskQueue.enqueue(new SavingTask(config, state, then, null));
            }
        } else {
            if(then != null) {
                then.run(null);
            }
        }
    }

    public void saveSessionAs(final AsyncUiTask.TaskCallback<SavingTask> then) {
        if(session.get() != null) {
            final File saveAs = dialogSaveSession.showSaveDialog(stage);
            if(saveAs != null) {
                final Path path = Paths.get(saveAs.getAbsolutePath());
                if(state.currentSessionPathProperty().get() == null) {
                    state.currentSessionPathProperty().set(path.toAbsolutePath().toString());
                }
                taskQueue.enqueue(new SavingTask(config, state, path, then, null));
            }
        } else {
            if(then != null) {
                then.run(null);
            }
        }
    }
}
