package grassmarlin.ui.pipeline;

import grassmarlin.Logger;
import grassmarlin.RuntimeConfiguration;
import grassmarlin.common.svg.Svg;
import grassmarlin.plugins.internal.logicalview.ICanHasContextMenu;
import grassmarlin.session.Session;
import grassmarlin.session.pipeline.PipelineTemplate;
import grassmarlin.ui.common.menu.ActiveMenuItem;
import grassmarlin.ui.common.menu.ColorSelectionMenuItem;
import javafx.application.Platform;
import javafx.beans.binding.StringBinding;
import javafx.geometry.Point2D;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

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

    public PipelineDialog(RuntimeConfiguration config, Session session) {
        super();

        this.config = config;
        this.session = session;

        RuntimeConfiguration.setIcons(this);
        this.initStyle(StageStyle.DECORATED);

        PipelineTemplate template = new PipelineTemplate("default", this.config.getDefaultPipelineTemplate());

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
                super.bind(PipelineDialog.this.editor.templateProperty(), PipelineDialog.this.editor.dirtyProperty());
            }

            @Override
            protected String computeValue() {
                String dirtyTag = "";
                if (PipelineDialog.this.editor.dirtyProperty().get()) {
                    dirtyTag = " * ";
                }

                if (PipelineDialog.this.editor.getTemplate() != null) {
                    return "Pipeline Editor - " + PipelineDialog.this.editor.getTemplate().getName() + dirtyTag;
                } else {
                    return "Pipeline Editor";
                }
            }
        });

        this.setOnShown(event -> {
            if (this.editor.getTemplate() == null) {
                Platform.runLater(() -> this.editor.setTemplate(template));
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
                Path pipelinePath = Paths.get(config.getPersistedString(RuntimeConfiguration.PersistableFields.DIRECTORY_PIPELINES));
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
                    this.editor.setTemplate(new PipelineTemplate(templateName, PipelineDialog.this.config.getDefaultPipelineTemplate()));
                    this.editor.dirtyProperty().setValue(false);
                }
            }
        });
        MenuItem saveTemplate = new ActiveMenuItem("_Save", event -> {
            this.editor.saveTemplate();
        });
        MenuItem loadTemplate = new ActiveMenuItem("_Load...", event -> {
            this.editor.loadTemplate();
        });
        MenuItem exportTemplate = new ActiveMenuItem("_Export...", event -> {
            this.editor.exportTemplate();
        });

        fileMenu.getItems().addAll(newItem, new SeparatorMenuItem(), saveTemplate, loadTemplate, exportTemplate);

        Menu optionsMenu = new Menu("_Options");

        MenuItem windowColor = new ColorSelectionMenuItem(config.colorPipelineWindowProperty(), "Window Background");
        MenuItem titleBackgroundColor = new ColorSelectionMenuItem(config.colorPipelineBackgroundProperty(), "Stage Title Background");
        MenuItem titleTextColor = new ColorSelectionMenuItem(config.colorPipelineTitleTextProperty(), "Stage Title Text");
        MenuItem backgroundColor = new ColorSelectionMenuItem(config.colorPipelineBackgroundProperty(), "Stage Background");
        MenuItem textColor = new ColorSelectionMenuItem(config.colorPipelineTextProperty(), "Stage Text");
        MenuItem selectorColor = new ColorSelectionMenuItem(config.colorPipelineSelectorProperty(), "Selector");
        MenuItem lineColor = new ColorSelectionMenuItem(config.colorPipelineLineProperty(), "Connection");

        optionsMenu.getItems().addAll(windowColor, titleBackgroundColor, titleTextColor, backgroundColor, textColor, selectorColor, lineColor);

        mainBar.getMenus().addAll(fileMenu, optionsMenu);

        return mainBar;
    }
}
