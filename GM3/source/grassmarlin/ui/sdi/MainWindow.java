package grassmarlin.ui.sdi;

import grassmarlin.*;
import grassmarlin.plugins.IPlugin;
import grassmarlin.plugins.internal.livepcap.LivePcapImport;
import grassmarlin.plugins.internal.livepcap.PcapEngine;
import grassmarlin.session.ImportItem;
import grassmarlin.session.Session;
import grassmarlin.session.pipeline.PipelineTemplate;
import grassmarlin.ui.common.ActionStackPropertyWrapper;
import grassmarlin.ui.common.SessionInterfaceController;
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
import grassmarlin.ui.common.tree.NavigationView;
import grassmarlin.ui.pipeline.PipelineDialog;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.Observable;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.ListChangeListener;
import javafx.geometry.Orientation;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.Dragboard;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.*;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

public class MainWindow extends Application {
    public static void launchFx(String[] args) {
        Application.launch(args);
    }

    protected final static Image iconNewSession = new Image(MainWindow.class.getClassLoader().getResourceAsStream("resources/images/microsoft/Generic_Document.png"));
    protected final static Image iconOpenSession = new Image(MainWindow.class.getClassLoader().getResourceAsStream("resources/images/microsoft/Folder_Open.png"));
    protected final static Image iconSaveSession = new Image(MainWindow.class.getClassLoader().getResourceAsStream("resources/images/microsoft/FloppyDisk.png"));
    protected final static Image iconImport = new Image(MainWindow.class.getClassLoader().getResourceAsStream("resources/images/microsoft/077_AddFile_48x48_72.png"));

    protected static final ImageView getMenuIcon(final Image base) {
        final ImageView result = new ImageView(base);
        result.setFitHeight(14.0);
        result.setFitWidth(14.0);
        result.setPreserveRatio(true);
        return result;
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
    private final TreeView<Object> treeNavigation;
    private final Map<SessionInterfaceController.View, Tab> lookupTabs;
    private final TabPane paneTabs;
    private final PipelineSelector pipelineSelector;

    // == Application State
    private final RuntimeConfiguration config;
    private final ObjectProperty<SingleDocumentState> documentState;
    private final ActionStackPropertyWrapper actionStack;

    private final AsyncTaskQueue taskQueue;
    private final PcapEngine pcapEngine;

    public MainWindow() {
        this.config = Launcher.getConfiguration();

        //This is the only time we don't have a valid Session--we will have one before hte user takes control, but we have to start with null.
        //As part of creating a new session we inform Plugins of the fact, and Plugins expect to be called from the UI Thread with the UI subsystem up and running.  It isn't yet.
        this.documentState = new SimpleObjectProperty<>(null);
        this.documentState.addListener(this.handlerDocumentChanged);
        this.actionStack = new ActionStackPropertyWrapper();

        this.taskQueue = new AsyncTaskQueue(Event.PROVIDER_JAVAFX);
        this.pcapEngine = grassmarlin.plugins.internal.livepcap.Plugin.getPcapEngine();   //HACK: This should be an instance member, which means finding the right plugin object within the current config.

        this.dialogSaveSession = new FileChooser();
        this.dialogLoadSession = new FileChooser();
        this.dialogPluginConflicts = new PluginConflictDialog();
        this.dialogImportManager = new ImportManagerDialog(this.config);
        this.dialogPipeline = new PipelineDialog(this.config);

        this.dialogAbout = new AboutDialog(this.config);
        this.dialogAboutPlugin = new AboutPluginDialog();

        this.layoutPrimary = new BorderPane();
        this.treeNavigation = new NavigationView();
        this.treeNavigation.setShowRoot(false);
        this.lookupTabs = new HashMap<>();
        this.paneTabs = new TabPane();
        this.paneTabs.getSelectionModel().selectedItemProperty().addListener(this.handlerSelectedTabChanged);
        this.pipelineSelector = new PipelineSelector(this.config);
    }

    //<editor-fold desc="Event Hooks to update UI in response to major control events">
    private final ChangeListener<SingleDocumentState> handlerDocumentChanged = this::handleDocumentChanged;
    private void handleDocumentChanged(final Observable observable, final SingleDocumentState oldValue, final SingleDocumentState newValue) {
        //The only time oldValue will be null is during startup, when nothing has been set yet.
        //After that, there is always a non-null SingleDocumentState.
        if(oldValue != null) {
            this.stage.titleProperty().unbind();
            this.stage.titleProperty().set(Version.APPLICATION_TITLE);
            oldValue.getController().currentViewProperty().removeListener(this.handlerViewChanged);
            oldValue.getController().getVisibleViews().removeListener(this.handlerVisibleViewsChanged);

            this.pipelineSelector.setSession(null);

            this.paneTabs.getTabs().clear();

            //Once the UI is cleand up, tell the plugins to forget it.
            config.enumeratePlugins(IPlugin.SessionEventHooks.class).forEach(plugin -> {
                plugin.sessionClosed(oldValue.getSession());
            });
        }
        if(newValue != null) {
            this.stage.titleProperty().bind(new ReadOnlyStringWrapper(Version.APPLICATION_TITLE + " ").concat(newValue.currentSessionTitleProperty()));
            newValue.getController().currentViewProperty().addListener(this.handlerViewChanged);
            newValue.getController().getVisibleViews().addListener(this.handlerVisibleViewsChanged);
            for(final SessionInterfaceController.View view : newValue.getController().getVisibleViews()) {
                //Make sure we aren't running afoul of a race condition on added tabs--if it isn't there now, we assume it won't be there when we add it later.
                if(lookupTabs.containsKey(view)) {
                    continue;
                }
                this.createTabForView(view);
            }

            this.pipelineSelector.setSession(newValue.getSession());

            //Do this last since that will trigger tab selection and update the content of the panes
            handleViewChanged(newValue.getController().currentViewProperty(), null, newValue.getController().currentViewProperty().get());
        }
    }

    private ListChangeListener<SessionInterfaceController.View> handlerVisibleViewsChanged = this::handleVisibleViewsChanged;
    private void handleVisibleViewsChanged(final ListChangeListener.Change<? extends SessionInterfaceController.View> change) {
        change.reset();
        while(change.next()) {
            for(final SessionInterfaceController.View removed : change.getRemoved()) {
                final Tab result = lookupTabs.remove(removed);
                if(result != null) {
                    this.paneTabs.getTabs().remove(result);
                }
            }
            for(final SessionInterfaceController.View added : change.getAddedSubList()) {
                this.createTabForView(added);
            }
        }
    }

    private void createTabForView(final SessionInterfaceController.View view) {
        final Tab result = new Tab();
        result.setContent(view.getContent());
        if(view.isDismissable()) {
            result.setClosable(true);

            if (view.isTemporary()) {
                result.setOnClosed(event -> {
                    MainWindow.this.documentState.get().getController().removeView(view);
                });
            } else {
                result.setOnClosed(event -> {
                    MainWindow.this.documentState.get().getController().setViewVisible(view, false);
                });
            }
        } else {
            result.setClosable(false);
        }
        result.textProperty().bind(view.titleProperty());
        lookupTabs.put(view, result);
        paneTabs.getTabs().add(result);
    }

    private final ChangeListener<SessionInterfaceController.View> handlerViewChanged = this::handleViewChanged;
    private void handleViewChanged(final Observable observable, final SessionInterfaceController.View oldView, final SessionInterfaceController.View newView) {
        //We don't care about the old value, only the new value.
        if(newView == null) {
            this.treeNavigation.setRoot(null);
            this.actionStack.unbind();
            this.actionStack.set(null);
        } else {
            this.treeNavigation.setRoot(newView.getNavigationRoot());
            if(newView.undoBufferProperty() != null) {
                this.actionStack.bind(newView.undoBufferProperty());
            } else {
                this.actionStack.unbind();
                this.actionStack.set(null);
            }
        }
    }

    private final ChangeListener<Tab> handlerSelectedTabChanged = this::handleSelectedTabChanged;
    private void handleSelectedTabChanged(final Observable observable, final Tab oldValue, final Tab newValue) {
        if(newValue == null) {
            MainWindow.this.documentState.get().getController().currentViewProperty().set(null);
        } else {
            //Do a reverse hashtable lookup.
            final SessionInterfaceController.View viewForTab = lookupTabs.entrySet().stream().filter(entry -> entry.getValue() == newValue).map(entry -> entry.getKey()).findAny().orElse(null);
            MainWindow.this.documentState.get().getController().currentViewProperty().set(viewForTab);
        }
    }

    //</editor-fold>

    private void initComponents() {
        RuntimeConfiguration.setIcons(this.stage);
        this.stage.setOnCloseRequest(e -> Platform.exit());

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
                    final Session sessionCurrent = MainWindow.this.documentState.get().getSession();
                    if(sessionCurrent != null) {
                        IPlugin.ImportProcessorWrapper wrapper = config.importerForFile(path);
                        if(wrapper != null) {
                            final ImportItem.FromPlugin item = new ImportItem.FromPlugin(path, sessionCurrent.getDefaultPipelineEntry());
                            item.importerPluginNameProperty().set(RuntimeConfiguration.pluginNameFor(wrapper.getClass()));
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
                new ActiveMenuItem("Show class factories", event -> {
                    MainWindow.this.config.enumeratePlugins(IPlugin.HasClassFactory.class).stream().flatMap(plugin -> plugin.getClassFactories().stream()).forEach(factory -> {
                        Logger.log(Logger.Severity.INFORMATION, "%s -> %s", factory.getFactoryName(), factory.getFactoryClass().getCanonicalName());
                    });
                })
        );
        menuDev.visibleProperty().bind(config.isDeveloperModeProperty());
        menu.getMenus().addAll(
                new Menu("_File") {
                    {
                        this.getItems().addAll(
                                new ActiveMenuItem("_New Session", MainWindow.getMenuIcon(iconNewSession), action -> {
                                    newSession();
                                }).chainSetAccelerator(KeyCode.N, KeyCombination.SHORTCUT_DOWN),
                                new ActiveMenuItem("_Open Session...", MainWindow.getMenuIcon(iconOpenSession), action -> {
                                    loadSession();
                                }).chainSetAccelerator(KeyCode.O, KeyCombination.SHORTCUT_DOWN),
                                new ActiveMenuItem("_Save Session", MainWindow.getMenuIcon(iconSaveSession), action -> {
                                    saveSessionThen(null);
                                }).chainSetAccelerator(KeyCode.S, KeyCombination.SHORTCUT_DOWN),
                                new ActiveMenuItem("Save Session _As...", action -> {
                                    saveSessionAs(null);
                                }),
                                new SeparatorMenuItem(),
                                new ActiveMenuItem("_Import", MainWindow.getMenuIcon(iconImport), action -> {
                                    dialogImportManager.showAndWait(MainWindow.this.documentState.get().getSession());
                                }).chainSetAccelerator(KeyCode.I, KeyCombination.SHORTCUT_DOWN),
                                new SeparatorMenuItem(),
                                new ActiveMenuItem("E_xit", action -> {
                                    MainWindow.this.stage.close();
                                })
                        );
                    }
                },
                new Menu("_Edit") {
                    {
                        this.getItems().addAll(
                                new ActiveMenuItem("_Undo", event -> {
                                    MainWindow.this.actionStack.get().undo();
                                }).bindEnabled(MainWindow.this.actionStack.isUndoAvailableProperty()).chainSetAccelerator(KeyCode.Z, KeyCombination.SHORTCUT_DOWN),
                                new ActiveMenuItem("_Redo", event -> {
                                    MainWindow.this.actionStack.get().redo();
                                }).bindEnabled(MainWindow.this.actionStack.isRedoAvailableProperty()).chainSetAccelerator(KeyCode.Z, KeyCombination.SHORTCUT_DOWN, KeyCombination.SHIFT_DOWN)
                        );
                        //TODO: Clipboard support
                    }
                },
                new Menu("_View") {
                    {
                        final SeparatorMenuItem spacer = new SeparatorMenuItem();
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
                                        java.awt.Desktop.getDesktop().open(new File(pathCapture));
                                    } catch (IOException ex) {
                                        Logger.log(Logger.Severity.ERROR, "Unable to open live capture folder (%s): %s", pathCapture, ex.getMessage());
                                    }
                                }),
                                new SeparatorMenuItem(),
                                new DynamicSubMenu("Tabs", null, () -> {
                                    final List<SessionInterfaceController.View> viewsVisible = new ArrayList<>(MainWindow.this.documentState.get().getController().getVisibleViews());
                                    final List<SessionInterfaceController.View> viewsHidden = new ArrayList<>(MainWindow.this.documentState.get().getController().getHiddenViews());

                                    final List<SessionInterfaceController.View> tabsAll = new ArrayList<>();

                                    viewsVisible.removeIf(view -> !view.isDismissable());
                                    tabsAll.addAll(viewsVisible);
                                    tabsAll.addAll(viewsHidden);
                                    return tabsAll.stream().distinct().sorted((o1, o2) -> o1.titleProperty().get().compareTo(o2.titleProperty().get())).map(view -> {
                                        final CheckMenuItem item = new CheckMenuItem(view.titleProperty().get());
                                        item.setSelected(viewsVisible.contains(view));
                                        item.selectedProperty().addListener((observable, oldValue, newValue) -> {
                                            MainWindow.this.documentState.get().getController().setViewVisible(view, newValue);
                                            MainWindow.this.paneTabs.getSelectionModel().select(MainWindow.this.lookupTabs.get(view));
                                        });
                                        return item;
                                    }).collect(Collectors.toList());
                                }),
                                spacer
                        );
                        this.setOnShowing(event -> {
                            //Remove everything after spacer
                            final int idxSpacer = this.getItems().indexOf(spacer);
                            while(this.getItems().size() > idxSpacer + 1) {
                                this.getItems().remove(idxSpacer + 1);
                            }

                            // Get items from current view
                            final SessionInterfaceController.View viewCurrent = MainWindow.this.documentState.get().getController().currentViewProperty().get();
                            if(viewCurrent == null) {
                                spacer.setVisible(false);
                            } else {
                                final List<MenuItem> items = viewCurrent.getViewMenuItems();
                                if(items == null || items.isEmpty()) {
                                    spacer.setVisible(false);
                                } else {
                                    spacer.setVisible(true);
                                    this.getItems().addAll(items);
                                }
                            }

                        });
                    }
                },
                new Menu("_Session") {
                    {
                        this.getItems().addAll(
                                new ActiveMenuItem("_Manage Networks", event -> {
                                    new NetworkDialog(MainWindow.this.config).showAndWait(MainWindow.this.documentState.get().getSession().getRawNetworkList());
                                }),
                                new Menu("Live _Pcap") {
                                    {
                                        this.getItems().addAll(
                                                new DynamicSubMenu("Pipeline Entry", null, () -> {
                                                    return MainWindow.this.documentState.get().getSession().getPipelineEntryPoints().stream().map(entry -> {
                                                        final CheckMenuItem item = new CheckMenuItem(entry);
                                                        item.setSelected(MainWindow.this.documentState.get().getSession().livePcapEntryPointProperty().get().equals(entry));
                                                        item.setOnAction(action -> {
                                                            MainWindow.this.documentState.get().getSession().livePcapEntryPointProperty().set(entry);
                                                        });
                                                        return item;
                                                    }).collect(Collectors.toList());
                                                }).bindEnabled(MainWindow.this.config.allowLivePcapProperty()),
                                                new DynamicSubMenu("_Start Live Pcap", null, () ->
                                                        pcapEngine.getDeviceList().stream().map(device ->
                                                                new ActiveMenuItem(device.toString(), action -> MainWindow.this.documentState.get().getSession().processImport(new LivePcapImport(MainWindow.this.documentState.get().getSession().getDefaultPipelineEntry(), device)))
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
                                    dialogPipeline.showForSession(MainWindow.this.documentState.get().getSession());
                                    dialogPipeline.toFront();
                                }).chainSetAccelerator(KeyCode.P, KeyCombination.SHORTCUT_DOWN, KeyCombination.SHIFT_DOWN)
                        );
                    }
                },
                new Menu("_Plugins") {
                    {
                        this.getItems().addAll(
                                config.enumeratePlugins(IPlugin.class).stream().sorted((p1, p2) -> p1.getName().compareTo(p2.getName())).map(plugin -> {
                                    final Collection<MenuItem> result = plugin.getMenuItems();

                                    final javafx.scene.image.Image image = plugin.getImageForSize(14);
                                    final ImageView graphic;
                                    if(image == null) {
                                        graphic = null;
                                    } else {
                                        graphic = new ImageView(image);
                                        graphic.setFitWidth(14.0);
                                        graphic.setFitHeight(14.0);
                                        graphic.setPreserveRatio(true);
                                    }
                                    final Menu menu = new Menu(plugin.getName(), graphic);

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
                                    getHostServices().showDocument("file:" + Paths.get(RuntimeConfiguration.getPersistedString(RuntimeConfiguration.PersistableFields.DIRECTORY_APPLICATION), Version.FILENAME_USER_GUIDE).toAbsolutePath().toString());
                                }).chainSetAccelerator(KeyCode.F1),
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

        //TODO: Add images to toolbar buttons, set to display image only.
        final Button toolbarNewSession = new Button("New Session", MainWindow.getMenuIcon(iconNewSession));
        toolbarNewSession.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        toolbarNewSession.setOnAction(action -> {
            newSession();
        });
        final Button toolbarOpenSession = new Button("Open Session", MainWindow.getMenuIcon(iconOpenSession));
        toolbarOpenSession.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        toolbarOpenSession.setOnAction(action -> {
            loadSession();
        });
        final Button toolbarSaveSession = new Button("Save Session", MainWindow.getMenuIcon(iconSaveSession));
        toolbarSaveSession.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        toolbarSaveSession.setOnAction(action -> {
            saveSessionThen(null);
        });
        final Button toolbarImport = new Button("Import", MainWindow.getMenuIcon(iconImport));
        toolbarImport.setContentDisplay(ContentDisplay.GRAPHIC_ONLY);
        toolbarImport.setOnAction(action -> {
            dialogImportManager.showAndWait(MainWindow.this.documentState.get().getSession());
        });

        final Pane spacerToolbar = new Pane();
        HBox.setHgrow(spacerToolbar, Priority.ALWAYS);
        final SourceSelector uiLivePcap = new SourceSelector(this.pcapEngine, this.documentState);

        //TODO: PipelineSelector should expose properties and move the events to be internal
        final Label labelPipelineSelector = new Label("Pipeline: ");
        labelPipelineSelector.setLabelFor(this.pipelineSelector);
        this.pipelineSelector.valueProperty().addListener(this.pipelineSelectionChanged);

        toolbar.getItems().addAll(labelPipelineSelector, this.pipelineSelector, toolbarNewSession, toolbarOpenSession, toolbarSaveSession, toolbarImport, spacerToolbar, uiLivePcap);

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

        final LogViewer paneLog = new LogViewer(config, config.getStartupMessages());
        paneLeftContent.getItems().addAll(treeNavigation, paneLog);

        paneContent.getItems().addAll(paneLeftContent, paneTabs);

        stage.setOnCloseRequest(event -> {
            if(MainWindow.this.documentState.get().dirtyProperty().get() && !config.isDeveloperModeProperty().get()) {
                final String titleForPrompt = MainWindow.this.documentState.get().currentSessionPathProperty().get() == null ? " New Session" : Paths.get(MainWindow.this.documentState.get().currentSessionPathProperty().get()).getFileName().toString();
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
            MainWindow.this.documentState.set(new SingleDocumentState(MainWindow.this.config, MainWindow.this.actionStack));
        });

        layoutPrimary.setTop(containerHeader);
        layoutPrimary.setCenter(paneContent);
        layoutPrimary.setBottom(statusbar);
    }

    protected final ChangeListener<PipelineTemplate> pipelineSelectionChanged = new ChangeListener<PipelineTemplate>() {
        @Override
        public void changed(ObservableValue<? extends PipelineTemplate> observable, PipelineTemplate oldValue, PipelineTemplate newValue) {
            if(MainWindow.this.documentState.get().getSession().canSetPipeline().get()) {
                MainWindow.this.documentState.get().getSession().pipelineTemplateProperty().set(newValue);
            }
        }
    };


    public void start(Stage stage) {
        this.stage = stage;
        stage.setTitle(Version.APPLICATION_TITLE);
        stage.setOnCloseRequest(e -> Platform.exit());

        stage.setMaximized(true);
        initComponents();

        stage.setScene(new Scene(layoutPrimary));
        stage.show();

        if(!RuntimeConfiguration.suppressUnchangedVersionNotesProperty().get() || !config.lastLoadedVersionProperty().get().equals(Version.APPLICATION_VERSION)) {
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
        if(this.documentState.get().dirtyProperty().get()) {
            final String titleForPrompt = this.documentState.get().currentSessionPathProperty().get() == null ? " New Session" : Paths.get(this.documentState.get().currentSessionPathProperty().get()).getFileName().toString();
            final Optional<ButtonType> result = new Alert(Alert.AlertType.CONFIRMATION, "Do you want to save changes to " + titleForPrompt + "?", ButtonType.YES, ButtonType.NO, ButtonType.CANCEL).showAndWait();
            if(result.isPresent() && result.get() != ButtonType.CANCEL) {
                if(result.get() == ButtonType.YES) {
                    saveSessionThen((self) -> MainWindow.this.documentState.set(new SingleDocumentState(this.config, this.actionStack)));
                } else if(result.get() == ButtonType.NO) {
                    this.documentState.set(new SingleDocumentState(this.config, this.actionStack));
                }
            } else {
                //Canceled -- ignore
            }
        } else {
            this.documentState.set(new SingleDocumentState(this.config, this.actionStack));
        }
    }

    protected void loadSessionFromPath(final Path pathToLoad) {
        if(pathToLoad != null) {
            taskQueue.enqueue(new LoadingTask(config, MainWindow.this.actionStack, pathToLoad, (loader) -> {
                this.documentState.set(loader.getDocumentState());
            }, (self) -> {
                MainWindow.this.documentState.get().currentSessionPathProperty().set(null);
            }));
        }
    }
    public void loadSession() {
        if(MainWindow.this.documentState.get().dirtyProperty().get()) {
            //Prompt to save, then load
            final String titleForPrompt = MainWindow.this.documentState.get().currentSessionPathProperty().get() == null ? " New Session" : Paths.get(MainWindow.this.documentState.get().currentSessionPathProperty().get()).getFileName().toString();
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
        if(MainWindow.this.documentState.get().currentSessionPathProperty().get() == null) {
            saveSessionAs(then);
        } else {
            taskQueue.enqueue(new SavingTask(config, MainWindow.this.documentState.get(), then, null));
        }
    }

    public void saveSessionAs(final AsyncUiTask.TaskCallback<SavingTask> then) {
        final File saveAs = dialogSaveSession.showSaveDialog(stage);
        if(saveAs != null) {
            final Path path = Paths.get(saveAs.getAbsolutePath());
            if(MainWindow.this.documentState.get().currentSessionPathProperty().get() == null) {
                MainWindow.this.documentState.get().currentSessionPathProperty().set(path.toAbsolutePath().toString());
            }
            taskQueue.enqueue(new SavingTask(config, MainWindow.this.documentState.get(), path, then, null));
        }
    }
}
