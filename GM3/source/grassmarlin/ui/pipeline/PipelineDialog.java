package grassmarlin.ui.pipeline;

import grassmarlin.Logger;
import grassmarlin.RuntimeConfiguration;
import grassmarlin.common.edit.IAction;
import grassmarlin.common.svg.Svg;
import grassmarlin.plugins.internal.logicalview.ICanHasContextMenu;
import grassmarlin.session.Session;
import grassmarlin.session.pipeline.PipelineTemplate;
import grassmarlin.ui.common.menu.ActiveMenuItem;
import grassmarlin.ui.common.menu.ColorSelectionMenuItem;
import javafx.beans.binding.StringBinding;
import javafx.geometry.Point2D;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import javafx.util.StringConverter;

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class PipelineDialog extends Stage {

    private PanePipelineEditor editor;
    private ContextMenu contextMenu;
    private MenuBar mainMenu;
    private Scene scene;
    private RuntimeConfiguration config;
    private Session session;

    public PipelineDialog(RuntimeConfiguration config) {
        super();

        this.config = config;

        RuntimeConfiguration.setIcons(this);
        this.initStyle(StageStyle.DECORATED);

        BorderPane mainPane = new BorderPane();

        this.editor = new PanePipelineEditor(this::createContextMenu, this::hideContextMenu, config);
        this.contextMenu = new ContextMenu();
        this.mainMenu = createMainMenu();

        mainPane.setTop(mainMenu);
        mainPane.setCenter(this.editor);

        scene = new Scene(mainPane);
        this.setScene(this.scene);
        this.scene.setOnKeyPressed(this.editor::handleKeyPressed);
        this.scene.setOnKeyReleased(this.editor::handleKeyReleased);

        this.titleProperty().bind(new StringBinding() {
            {
                super.bind(PipelineDialog.this.editor.templateProperty(), PipelineDialog.this.editor.dirtyProperty(), PipelineDialog.this.editor.liveEditProperty());
            }

            @Override
            protected String computeValue() {
                String dirtyTag = "";
                if (PipelineDialog.this.editor.dirtyProperty().get()) {
                    dirtyTag = " * ";
                }

                String liveEditTag = "";
                if (PipelineDialog.this.editor.liveEditProperty().get()) {
                    liveEditTag = " - Live Edit";
                }

                if (PipelineDialog.this.editor.getTemplate() != null) {
                    return "Pipeline Editor - " + PipelineDialog.this.editor.getTemplate().getName() + liveEditTag + dirtyTag;
                } else {
                    return "Pipeline Editor";
                }
            }
        });

        this.setResizable(true);
        this.setMaximized(true);
        this.setOnCloseRequest(event -> {
            if (this.editor.checkForSaveOnClose()) {
                event.consume();
            }
        });
    }

    public void showForSession(final Session session) {
        this.session = session;
        this.editor.setTemplate(new PipelineTemplate(session.getSessionDefaultTemplate().getName(), session.getSessionDefaultTemplate()));
        this.editor.setSession(session);
        this.editor.setLiveEdit(true);
        this.show();
    }

    private void createContextMenu(List<Object> objects, Point2D location) {
        contextMenu.hide();
        contextMenu.getItems().clear();

        contextMenu.getItems().addAll(
                new ActiveMenuItem("Export to SVG...", event -> {
                    final FileChooser dlgExportAs = new FileChooser();
                    dlgExportAs.setTitle("Export To...");
                    dlgExportAs.getExtensionFilters().addAll(
                            new FileChooser.ExtensionFilter("SVG Files", "*.svg"),
                            new FileChooser.ExtensionFilter("All Files", "*")
                    );
                    final File selected = dlgExportAs.showSaveDialog(PipelineDialog.this.getScene().getWindow());
                    if(selected != null) {
                        //HACK: Skip the ZSP itself, since that is the camera; the first (only) child is the scene graph at world coordinate scale.
                        Svg.serialize((Parent) PipelineDialog.this.editor.getChildrenUnmodifiable().get(0), Paths.get(selected.getAbsolutePath()));
                    }
                }),
                new SeparatorMenuItem()
        );

        for (final Object object : objects) {
            if (object instanceof ICanHasContextMenu) {
                final List<MenuItem> items = ((ICanHasContextMenu) object).getContextMenuItems();
                if (items != null && !items.isEmpty()) {
                    if (!contextMenu.getItems().isEmpty()) {
                        contextMenu.getItems().add(new SeparatorMenuItem());
                    }
                    contextMenu.getItems().addAll(items);
                }
            }
        }

        if (!contextMenu.getItems().isEmpty()) {
            contextMenu.show(this, location.getX(), location.getY());
        }
    }

    private void hideContextMenu(Point2D location) {
        contextMenu.hide();
    }

    private MenuBar createMainMenu() {
        MenuBar mainBar = new MenuBar();

        Menu fileMenu = new Menu("_File");

        TextInputDialog nameSelection = new TextInputDialog();
        nameSelection.setTitle("New Pipeline");
        nameSelection.setContentText("Please enter a name:");
        nameSelection.setHeaderText("");
        nameSelection.setGraphic(null);
        nameSelection.setOnShown(event -> nameSelection.getEditor().setText("New Pipeline"));

        MenuItem newItem = new ActiveMenuItem("_New", event -> {
            boolean canceled = this.editor.checkForSaveOnClose();
            if (!canceled) {
                // find existing pipelines to check names against
                Path pipelinePath = Paths.get(RuntimeConfiguration.getPersistedString(RuntimeConfiguration.PersistableFields.DIRECTORY_PIPELINES));
                List<String> names = new ArrayList<>();
                if (Files.exists(pipelinePath) && Files.isDirectory(pipelinePath)) {
                    try (DirectoryStream<Path> files = Files.newDirectoryStream(pipelinePath)) {
                        files.forEach(path -> names.add(path.getFileName().toString()));
                    } catch (IOException ioe) {
                        Logger.log(Logger.Severity.ERROR, "Can not read pipeline directory " + pipelinePath.toAbsolutePath());
                    }
                } else {
                    Logger.log(Logger.Severity.ERROR, "Pipeline directory " + pipelinePath.toAbsolutePath() + " Does not exist");
                }
                boolean done = false;
                String templateName = "";
                do {
                    Optional<String> name = nameSelection.showAndWait();
                    if (name.isPresent()) {
                        if (names.contains(name.get() + ".pt") || name.get().equalsIgnoreCase("default")) {
                            Alert alert = new Alert(Alert.AlertType.INFORMATION);
                            alert.setTitle("Invalid Name");
                            alert.setHeaderText("Name entered is invalid or already exists");
                            alert.setGraphic(null);
                            alert.showAndWait();
                        } else {
                            templateName = name.get();
                            done = true;
                        }
                    } else {
                        done = true;
                    }
                } while (!done);
                if (!templateName.isEmpty()) {
                    String name = templateName;
                    this.editor.doEdit(new IAction() {
                        @Override
                        public boolean doAction() {
                            PipelineDialog.this.editor.setTemplate(new PipelineTemplate(name, PipelineDialog.this.session.getSessionDefaultTemplate()));
                            PipelineDialog.this.editor.setLiveEdit(false);
                            return true;
                        }
                    });
                }
            }
        });
        newItem.setAccelerator(new KeyCodeCombination(KeyCode.N, KeyCombination.SHORTCUT_DOWN));
        MenuItem saveTemplate = new ActiveMenuItem("_Save", event -> {
            this.editor.saveTemplate();
        });
        saveTemplate.setAccelerator(new KeyCodeCombination(KeyCode.S, KeyCombination.SHORTCUT_DOWN));
        MenuItem loadTemplate = new ActiveMenuItem("_Load...", event -> {
            this.editor.loadTemplate();
        });
        loadTemplate.setAccelerator(new KeyCodeCombination(KeyCode.L, KeyCombination.SHORTCUT_DOWN));
        MenuItem editTemplate = new ActiveMenuItem("_Edit", event -> {
            Dialog<PipelineTemplate> dialogPipelineSelection = new Dialog<>();
            RuntimeConfiguration.setIcons(dialogPipelineSelection);
            dialogPipelineSelection.setTitle("Select Pipeline");
            dialogPipelineSelection.setContentText(null);
            dialogPipelineSelection.setGraphic(null);
            dialogPipelineSelection.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

            GridPane content = new GridPane();

            ComboBox<PipelineTemplate> boxPipeline = new ComboBox<>(this.config.getPipelineTemplates());
            boxPipeline.setConverter(new StringConverter<PipelineTemplate>() {
                @Override
                public String toString(PipelineTemplate object) {
                    return object.getName();
                }

                @Override
                public PipelineTemplate fromString(String string) {
                    return null;
                }
            });
            Label labelBox = new Label("Pipelines:");

            content.add(labelBox, 1, 1);
            content.add(boxPipeline, 1, 2);

            dialogPipelineSelection.getDialogPane().setContent(content);

            dialogPipelineSelection.setResultConverter(button -> {
                if (button.getButtonData() == ButtonBar.ButtonData.OK_DONE) {
                    return boxPipeline.getValue();
                } else {
                    return null;
                }
            });

            PipelineTemplate template = dialogPipelineSelection.showAndWait().orElse(null);

            if (template != null) {
                this.editor.setTemplate(new PipelineTemplate(template.getName(), template));
                this.editor.setLiveEdit(true);
            }
        });
        editTemplate.setAccelerator(new KeyCodeCombination(KeyCode.E, KeyCombination.SHORTCUT_DOWN));
        MenuItem exportTemplate = new ActiveMenuItem("E_xport...", event -> {
            this.editor.exportTemplate();
        });

        fileMenu.getItems().addAll(newItem, new SeparatorMenuItem(), saveTemplate, loadTemplate, editTemplate, exportTemplate);

        Menu editMenu = new Menu("_Edit");

        MenuItem undo = new ActiveMenuItem("_Undo", event -> {
            this.editor.undoEdit();
        }).chainSetAccelerator(new KeyCodeCombination(KeyCode.Z, KeyCombination.SHORTCUT_DOWN));
        undo.disableProperty().bind(this.editor.canUndoProperty().not());
        MenuItem redo = new ActiveMenuItem("_Redo", event -> {
            this.editor.redoEdit();
        }).chainSetAccelerator(new KeyCodeCombination(KeyCode.Z, KeyCombination.SHIFT_DOWN, KeyCombination.SHORTCUT_DOWN));
        redo.disableProperty().bind(this.editor.canRedoProperty().not());

        editMenu.getItems().addAll(undo, redo);

        Menu optionsMenu = new Menu("_Options");

        MenuItem windowColor = new ColorSelectionMenuItem(config.colorPipelineWindowProperty(), "Window Background");
        MenuItem titleBackgroundColor = new ColorSelectionMenuItem(config.colorPipelineBackgroundProperty(), "Stage Title Background");
        MenuItem titleTextColor = new ColorSelectionMenuItem(config.colorPipelineTitleTextProperty(), "Stage Title Text");
        MenuItem backgroundColor = new ColorSelectionMenuItem(config.colorPipelineBackgroundProperty(), "Stage Background");
        MenuItem textColor = new ColorSelectionMenuItem(config.colorPipelineTextProperty(), "Stage Text");
        MenuItem selectorColor = new ColorSelectionMenuItem(config.colorPipelineSelectorProperty(), "Selector");
        MenuItem lineColor = new ColorSelectionMenuItem(config.colorPipelineLineProperty(), "Connection");

        optionsMenu.getItems().addAll(windowColor, titleBackgroundColor, titleTextColor, backgroundColor, textColor, selectorColor, lineColor);

        mainBar.getMenus().addAll(fileMenu, editMenu, optionsMenu);

        return mainBar;
    }
}
