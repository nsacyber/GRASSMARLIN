package grassmarlin.ui.pipeline;

import grassmarlin.Logger;
import grassmarlin.RuntimeConfiguration;
import grassmarlin.plugins.IPlugin;
import grassmarlin.plugins.internal.logicalview.ICanHasContextMenu;
import grassmarlin.plugins.serialization.PluginSerializableWrapper;
import grassmarlin.session.pipeline.PipelineTemplate;
import grassmarlin.ui.common.BackgroundFromColor;
import grassmarlin.ui.common.IAltClickable;
import grassmarlin.ui.common.IDraggable;
import grassmarlin.ui.common.menu.ActiveMenuItem;
import javafx.beans.binding.DoubleBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableNumberValue;
import javafx.collections.ListChangeListener;
import javafx.geometry.Point2D;
import javafx.scene.control.Dialog;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextInputDialog;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Paint;
import javafx.scene.text.Text;

import java.io.*;
import java.util.*;

public class VisualStage extends VBox implements IDraggable, IAltClickable, ICanHasContextMenu, Linkable {

    private static final String CONFIG_IMAGE_PATH = "/resources/images/Options_Grey.png";

    private RuntimeConfiguration config;
    private PipelineTemplate template;
    private IPlugin.PipelineStage stage;
    private ObjectProperty<String> name;
    private Set<String> outputs;
    private boolean configurable;
    private LinkingPane container;
    private HBox titleRow;
    private Map<String, LinkableOutputRow> outputToRow;
    private BooleanProperty selectedProperty;

    // Color Properties
    private ObjectProperty<Paint> backgroundColorProperty;
    private ObjectProperty<Paint> textColorProperty;
    private ObjectProperty<Paint> titleBackgroundProperty;
    private ObjectProperty<Paint> titleTextColorProperty;
    private ObjectProperty<Paint> contrastColorProperty;


    public VisualStage(PipelineTemplate template, IPlugin.PipelineStage stage, LinkingPane container, RuntimeConfiguration config) {
        this.template = template;
        this.stage = stage;
        this.container = container;
        this.config = config;
        this.name = new SimpleObjectProperty<>(stage.getName());
        this.outputs = stage.getOutputs();
        this.configurable = stage.isConfigurable();
        this.outputToRow = new LinkedHashMap<>();
        this.selectedProperty = new SimpleBooleanProperty(false);

        this.backgroundColorProperty = new SimpleObjectProperty<>();
        this.backgroundColorProperty.bind(config.colorPipelineBackgroundProperty());
        this.textColorProperty = new SimpleObjectProperty<>();
        this.textColorProperty.bind(config.colorPipelineTextProperty());
        this.titleBackgroundProperty = new SimpleObjectProperty<>();
        this.titleBackgroundProperty.bind(config.colorPipelineTitleBackgroundProperty());
        this.titleTextColorProperty = new SimpleObjectProperty<>();
        this.titleTextColorProperty.bind(config.colorPipelineTitleTextProperty());
        this.contrastColorProperty = new SimpleObjectProperty<>();
        this.contrastColorProperty.bind(config.colorPipelineSelectorProperty());

        this.backgroundProperty().bind(new BackgroundFromColor(this.backgroundColorProperty));
        this.getLinkingPane().getSelected().addListener(this::handleSelectionChange);

        this.titleRow = this.createTitleRow();
        this.getChildren().add(this.titleRow);

        for (String output : outputs) {
            LinkableOutputRow row = createOutputRow(output);
            this.getChildren().add(row);

            outputToRow.put(output, row);
        }

        this.makeDraggable(true);
    }

    private void bindTextColor(Text toBind) {
        toBind.fillProperty().bind(textColorProperty);
    }

    private LinkableOutputRow createOutputRow(String output) {
        LinkableOutputRow row = new LinkableOutputRow(3, this, output);
        row.backgroundProperty().bind(new BackgroundFromColor(this.backgroundColorProperty));
        Text outputText = new Text(output);
        bindTextColor(outputText);

        LinkingTriangle outputTri = new LinkingTriangle(this.contrastColorProperty, row, outputText.boundsInParentProperty());

        Pane spacer = new Pane();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        row.getChildren().addAll(spacer, outputText, outputTri);

        return row;
    }

    private HBox createTitleRow() {
        HBox row = new HBox(3);

        row.backgroundProperty().bind(new BackgroundFromColor(this.titleBackgroundProperty));

        Text titleText = new Text(this.name.get());
        titleText.textProperty().bind(this.name);
        titleText.fillProperty().bind(this.titleTextColorProperty);
        HBox inputBox = new HBox();
        inputBox.prefWidthProperty().bind(new DoubleBinding() {
            {
                super.bind(titleText.boundsInParentProperty());
            }

            @Override
            protected double computeValue() {
                return titleText.boundsInParentProperty().get().getHeight();
            }
        });
        LinkingTriangle inputTri = new LinkingTriangle(this.contrastColorProperty, this, titleText.boundsInParentProperty());
        inputBox.getChildren().add(inputTri);

        HBox spacer = new HBox();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Image configImage = new Image(getClass().getResourceAsStream(CONFIG_IMAGE_PATH));
        ImageView configurableView = new ImageView();
        configurableView.setFitHeight(16);
        configurableView.setFitWidth(16);
        HBox configButton = new HBox(configurableView);
        if (configurable) {
            configurableView.setImage(configImage);
            configButton.setOnMousePressed(event -> {
                Serializable stageConfig = ((IPlugin.DefinesPipelineStages) config.pluginFor(stage.getStage()))
                        .getConfiguration(stage, template.getStageConfiguration(stage.getName()));

                template.setConfiguration(stage.getName(), stageConfig);
                this.getLinkingPane().dirtyProperty().setValue(true);
            });
        }


        row.getChildren().addAll(inputBox, titleText, spacer, configButton);

        return row;
    }

    private void handleSelectionChange(ListChangeListener.Change<Linkable> change) {
        while(change.next()) {
            if (change.getAddedSubList().contains(this)) {
                this.selectedProperty.setValue(true);
            } else if (change.getRemoved().contains(this)) {
                this.selectedProperty.setValue(false);
            }
        }
    }

    public LinkableOutputRow getRow(String output) {
        return this.outputToRow.get(output);
    }

    public IPlugin.PipelineStage getStage() {
        return this.stage;
    }

    @Override
    public String getName() {
        return this.name.get();
    }

    @Override
    public List<Object> getRespondingNodes(Point2D point) {
        if (this.contains(point)) {
            return Arrays.asList(this);
        } else {
            return Collections.emptyList();
        }
    }

    @Override
    public void setSelected(boolean selected) {
        this.selectedProperty.setValue(selected);
    }

    @Override
    public boolean isSelected() {
        return this.selectedProperty.get();
    }

    @Override
    public BooleanProperty getSelectedProperty() {
        return this.selectedProperty;
    }

    @Override
    public LinkingPane getLinkingPane() {
        return this.container;
    }

    @Override
    public ObservableNumberValue getLinkXProperty() {
        return this.titleRow.layoutXProperty().add(this.translateXProperty()).add(1.0);
    }

    @Override
    public ObservableNumberValue getLinkYProperty() {
        return this.titleRow.layoutYProperty().add(this.translateYProperty()).add(this.titleRow.heightProperty().divide(2.0));
    }

    @Override
    public ObservableNumberValue getLinkControlXProperty() {
        return this.titleRow.layoutXProperty().add(this.translateXProperty()).subtract(30);
    }

    @Override
    public ObservableNumberValue getLinkControlYProperty() {
        return this.getLinkYProperty();
    }

    @Override
    public ObservableNumberValue getSourceXProperty() {
        return null;
    }

    @Override
    public ObservableNumberValue getSourceYProperty() {
        return null;
    }

    @Override
    public ObservableNumberValue getSourceControlXProperty() { return null; }

    @Override
    public ObservableNumberValue getSourceControlYProperty() { return null; }

    @Override
    public List<MenuItem> getContextMenuItems() {
        List<MenuItem> items = new ArrayList<>();

        MenuItem rename = new ActiveMenuItem("Rename", event -> {
            Dialog<String> getName = new TextInputDialog(this.name.get());
            getName.setTitle("Stage Name");
            getName.setHeaderText(null);
            getName.setGraphic(null);
            getName.setContentText("Name:");
            RuntimeConfiguration.setIcons(getName);
            String newName = getName.showAndWait().orElse(this.name.get());
            String oldName = this.name.get();

            if (!oldName.equals(newName)) {
                this.name.setValue(newName);
                this.stage.setName(newName);
                this.getLinkingPane().dirtyProperty().setValue(true);

                if (this.stage.isConfigurable()) {
                    //copy current configuration if no configuration for new name exists
                    //the easiest way to actually create a copy is to serialize/deserialize the config object
                    Serializable currentConfig = this.template.getStageConfiguration(oldName);
                    IPlugin.DefinesPipelineStages plugin = (IPlugin.DefinesPipelineStages) this.config.pluginFor(currentConfig.getClass());
                    if (this.template.getStageConfiguration(newName) == null) {
                        try {
                            ByteArrayOutputStream bytesOut = new ByteArrayOutputStream();
                            ObjectOutputStream objOut = new ObjectOutputStream(bytesOut);

                            objOut.writeObject(new PluginSerializableWrapper(RuntimeConfiguration.pluginNameFor(currentConfig.getClass()), currentConfig));
                            objOut.close();
                            bytesOut.close();

                            ByteArrayInputStream bytesIn = new ByteArrayInputStream(bytesOut.toByteArray());
                            ObjectInputStream objIn = new ObjectInputStream(bytesIn);
                            PluginSerializableWrapper copyConfiguration = ((PluginSerializableWrapper) objIn.readObject());

                            this.template.setConfiguration(newName, copyConfiguration.getWrapped());
                        } catch (IOException | ClassNotFoundException e) {
                            // Failed to copy for some reason, set to default config
                            Logger.log(Logger.Severity.WARNING, "Unable to copy stage configuration during rename to %s, error - %s", newName, e.getMessage());
                            this.template.setConfiguration(newName, plugin.getDefaultConfiguration(this.stage));
                        }
                    }

                    this.template.cleanConfiguration(oldName);
                }
            }
        });
        MenuItem delete = new ActiveMenuItem("Remove Stage", event -> this.getLinkingPane().removeLinkable(this.getStage()));

        items.add(rename);
        items.add(delete);

        return items;
    }
}
