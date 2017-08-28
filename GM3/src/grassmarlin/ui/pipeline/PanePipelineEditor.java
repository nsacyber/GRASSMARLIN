package grassmarlin.ui.pipeline;

import com.sun.istack.internal.NotNull;
import com.sun.istack.internal.Nullable;
import com.sun.javafx.collections.ObservableListWrapper;
import grassmarlin.Logger;
import grassmarlin.RuntimeConfiguration;
import grassmarlin.plugins.IPlugin;
import grassmarlin.session.pipeline.PipelineStageConnection;
import grassmarlin.session.pipeline.PipelineTemplate;
import grassmarlin.ui.common.BackgroundFromColor;
import grassmarlin.ui.common.TextMeasurer;
import grassmarlin.ui.common.ZoomableScrollPane;
import grassmarlin.ui.common.menu.ActiveMenuItem;
import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.input.KeyEvent;
import javafx.scene.paint.Paint;
import javafx.stage.FileChooser;

import javax.xml.stream.XMLStreamException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class PanePipelineEditor extends ZoomableScrollPane implements LinkingPane<IPlugin.PipelineStage, PipelineStageConnection> {

    public static FileChooser.ExtensionFilter saveExtension = new FileChooser.ExtensionFilter("Pipeline Template", "*.pt");
    public static FileChooser.ExtensionFilter allExtension = new FileChooser.ExtensionFilter("All Files", "*.*");
    public static FileChooser.ExtensionFilter exportExtension = new FileChooser.ExtensionFilter("Pipeline Template Export", "*.pte");

    private static Path lastExportedPath = null;


    private ReadOnlyObjectWrapper<PipelineTemplate> templateProperty;
    private Map<IPlugin.PipelineStage, VisualStage> stageToVisual;
    private Map<PipelineStageConnection, VisualConnection> connectionToVisual;
    private Map<String, VisualEntry> entryToVisual;
    private List<VisualEntry> entryPoints;
    private List<VisualConnection> entryConnections;
    private ObservableList<Linkable> selected;
    private BooleanProperty createConnection;
    private BooleanProperty clearConnection;
    private ObjectProperty<Paint> lineColorProperty;
    private BooleanProperty diryProperty;

    private RuntimeConfiguration config;

    private static String STAGE_LAYER = "STAGE";
    private static String CONNECTION_LAYER = "CONNECTION";

    public PanePipelineEditor(@Nullable BiConsumer<List<Object>, Point2D> altClickConsumer, @Nullable Consumer<Point2D> clickConsumer, @NotNull final RuntimeConfiguration config) {
        super(altClickConsumer, clickConsumer, CONNECTION_LAYER, STAGE_LAYER);

        this.config = config;

        this.createConnection = new SimpleBooleanProperty(false);
        this.clearConnection = new SimpleBooleanProperty(false);

        this.diryProperty = new SimpleBooleanProperty(false);

        this.templateProperty = new ReadOnlyObjectWrapper<>();
        this.templateProperty.addListener(this::handleTemplateChanged);

        this.lineColorProperty = new SimpleObjectProperty<>();
        this.lineColorProperty.bind(config.colorPipelineLineProperty());

        this.backgroundProperty().bind(new BackgroundFromColor(config.colorPipelineWindowProperty()));

        this.stageToVisual = new HashMap<>();
        this.connectionToVisual = new HashMap<>();
        this.entryToVisual = new HashMap<>();
        this.entryPoints = new ArrayList<>();
        this.entryConnections = new ArrayList<>();
        this.selected = new ObservableListWrapper<>(new ArrayList<>());

        this.setPrefSize(1200, 600);

        this.setOnKeyPressed(this::handleKeyPressed);
        this.setOnKeyReleased(this::handleKeyReleased);
    }

    private void handleTemplateChanged(ObservableValue<? extends PipelineTemplate> template, PipelineTemplate oldValue, PipelineTemplate newValue) {
        this.clear();

        this.buildGraph();

        this.layoutGraph();

        this.diryProperty.setValue(false);
    }

    public void clear() {
        super.clear();
        this.stageToVisual.clear();
        this.connectionToVisual.clear();
        this.entryPoints.clear();
        this.entryToVisual.clear();
        this.entryConnections.clear();
        this.selected.clear();
        this.diryProperty.setValue(false);
    }

    public boolean checkForSaveOnClose() {
        boolean canceled = false;
        if (this.dirtyProperty().get()) {
            Optional<ButtonType> choice = this.getSaveOnCloseDialog().showAndWait();

            if (choice.isPresent()) {
                switch (choice.get().getButtonData()) {
                    case YES:
                        canceled = this.saveTemplate();
                        break;
                    case NO:
                        break;
                    case CANCEL_CLOSE:
                        canceled = true;
                        break;
                    default:
                }
            }
        }

        return canceled;
    }

    public Dialog<ButtonType> getSaveOnCloseDialog() {
        Dialog<ButtonType> saveDialog = new Dialog<>();
        saveDialog.getDialogPane().getButtonTypes().addAll(ButtonType.YES, ButtonType.NO, ButtonType.CANCEL);
        ((Button) saveDialog.getDialogPane().lookupButton(ButtonType.YES)).setDefaultButton(false);
        saveDialog.setTitle("Closing");
        saveDialog.setHeaderText("Closing \"" + getTemplate().getName() + "\"\n\nWould you like to Save?");

        return saveDialog;
    }

    public boolean saveTemplate() {
        boolean canceled = false;
        if (this.dirtyProperty().get()) {

            if (this.getTemplate().getName().equalsIgnoreCase("default")) {
                Logger.log(Logger.Severity.WARNING, "Can not modify default template");
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setHeaderText("Default Template Can Not Be Modified");
                alert.setTitle("");
                RuntimeConfiguration.setIcons(alert);
                alert.showAndWait();
            } else {

                List<Map.Entry<String, List<IPlugin.PipelineStage>>> unconnectedEntryPoints = this.getTemplate().getEntryPoints().entrySet().stream()
                        .filter(entry -> entry.getValue() == null || entry.getValue().isEmpty())
                        .collect(Collectors.toList());
                if (!unconnectedEntryPoints.isEmpty()) {
                    Alert unconnectedAlert = new Alert(Alert.AlertType.CONFIRMATION, "Do you want to save anyway?", ButtonType.YES, ButtonType.CANCEL);
                    unconnectedAlert.setGraphic(null);
                    final String unconnectedEntryString = unconnectedEntryPoints.stream().map(entry -> entry.getKey()).collect(Collectors.joining(","));
                    unconnectedAlert.setHeaderText(String.format("Unconnected Entry Point(s) - %s", unconnectedEntryString));
                    ButtonType choice = unconnectedAlert.showAndWait().orElse(ButtonType.CANCEL);
                    if (!choice.equals(ButtonType.YES)) {
                        canceled = true;
                    }
                }

                if (!canceled) {
                    File toSave = Paths.get(config.getPersistedString(RuntimeConfiguration.PersistableFields.DIRECTORY_PIPELINES)).toFile();
                    if (toSave != null) {
                        Path savePath = toSave.toPath().resolve(this.getTemplate().getName() + ".pt");
                        boolean save = false;
                        if (Files.exists(savePath)) {
                            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
                            alert.setTitle("Overwrite");
                            alert.setHeaderText("Pipeline " + this.getTemplate().getName() + " Already Exists");
                            alert.setContentText("Overwrite?");

                            Optional<ButtonType> result = alert.showAndWait();
                            if (result.isPresent() && result.get() == ButtonType.OK) {
                                save = true;
                            }
                        } else {
                            save = true;
                        }
                        if (save) {
                            try (OutputStream out = Files.newOutputStream(savePath)) {
                                PipelineTemplate.saveTemplate(config, this.getTemplate(), out);
                                this.dirtyProperty().setValue(false);
                            } catch (IOException | XMLStreamException e) {
                                Logger.log(Logger.Severity.ERROR, "Failed to Save to %s, reason: %s", toSave.getAbsolutePath(), e.getMessage());
                            }
                        }
                    }
                }
            }
        }
        return canceled;
    }

    public void loadTemplate() {
        boolean canceled = this.checkForSaveOnClose();
        if (!canceled) {
            FileChooser chooser = new FileChooser();
            chooser.setInitialDirectory(Paths.get(config.getPersistedString(RuntimeConfiguration.PersistableFields.DIRECTORY_PIPELINES)).toFile());
            chooser.getExtensionFilters().addAll(saveExtension, exportExtension, allExtension);
            chooser.setSelectedExtensionFilter(saveExtension);
            File toLoad = chooser.showOpenDialog(this.getScene().getWindow());
            if (toLoad != null) {
                try (InputStream in = Files.newInputStream(toLoad.toPath())) {
                    this.setTemplate(PipelineTemplate.loadTemplate(config, in));
                } catch (ClassNotFoundException | IOException | XMLStreamException e) {
                    Logger.log(Logger.Severity.ERROR, "Can not open %s, reason: %s", toLoad.getAbsolutePath(), e.getMessage());
                }
            }
        }
    }

    public void exportTemplate() {
        FileChooser chooser = new FileChooser();
        if (lastExportedPath != null) {
            chooser.setInitialDirectory(lastExportedPath.toFile());
        }
        chooser.getExtensionFilters().addAll(exportExtension);
        chooser.setSelectedExtensionFilter(exportExtension);
        File exportPath = chooser.showSaveDialog(this.getScene().getWindow());
        if (exportPath != null) {
            try (OutputStream out = Files.newOutputStream(exportPath.toPath())) {
                PipelineTemplate.saveTemplate(config, this.getTemplate(), out);
                lastExportedPath = exportPath.toPath().getParent();
            } catch (IOException | XMLStreamException e) {
                Logger.log(Logger.Severity.ERROR, "Failed to Export to %s, reason: %s", exportPath.getAbsolutePath(), e.getMessage());
            }
        }
    }

    private void buildGraph() {
        for (IPlugin.PipelineStage stage : this.templateProperty.get().getStages()) {
            if (stage != null) {

                VisualStage vs = this.stageToVisual.get(stage);

                if (vs == null) {
                    vs = new VisualStage(getTemplate(), stage, this, config);
                    this.stageToVisual.put(stage, vs);
                }

                this.addChild(vs, STAGE_LAYER);
            }
        }

        for (Map.Entry<String, List<IPlugin.PipelineStage>> entry : this.templateProperty.get().getEntryPoints().entrySet()) {
            VisualEntry ve = new VisualEntry(entry.getKey(), this, config);
            this.entryPoints.add(ve);
            this.entryToVisual.put(entry.getKey(), ve);

            this.addChild(ve, STAGE_LAYER);

            for (IPlugin.PipelineStage stage : entry.getValue()) {
                if (stage != null) {
                    VisualConnection link = new VisualConnection(ve, stageToVisual.get(stage), this.lineColorProperty);
                    this.entryConnections.add(link);
                    this.addChild(link.getConLine(), CONNECTION_LAYER);
                }
            }
        }

        for (PipelineStageConnection connection : this.templateProperty.get().getConnections()) {

            VisualConnection link = this.connectionToVisual.get(connection);

            if (link == null) {
                LinkableOutputRow source = this.stageToVisual.get(connection.getSourceStage()).getRow(connection.getOutput());
                VisualStage dest = this.stageToVisual.get(connection.getDestStage());
                link = new VisualConnection(source, dest, this.lineColorProperty);
                this.connectionToVisual.put(connection, link);
            }

            this.addChild(link.getConLine(), CONNECTION_LAYER);
        }

    }

    private void layoutGraph() {
        List<Map.Entry<String, List<IPlugin.PipelineStage>>> entryPoints = new ArrayList<>(this.getTemplate().getEntryPoints().entrySet());
        entryPoints.sort((entry1, entry2) -> entry1.getKey().compareTo(entry2.getKey()));
        List<IPlugin.PipelineStage> entryConnections = new ArrayList<>();
        entryPoints.forEach(entryPoint -> entryConnections.addAll(this.getTemplate().getEntryPoints().get(entryPoint.getKey())));
        List<IPlugin.PipelineStage> firstStages  = entryPoints.stream()
                .flatMap(entry -> entry.getValue().stream())
                .collect(Collectors.toList());
        List<IPlugin.PipelineStage> connectionDestinations = this.getTemplate().getConnections().stream()
                .map(con -> con.getDestStage())
                .collect(Collectors.toList());
        List<IPlugin.PipelineStage> unconnectedStages = this.getTemplate().getStages().stream()
                .filter(stage -> !(firstStages.contains(stage) || connectionDestinations.contains(stage)))
                .collect(Collectors.toList());

        // add stages that have no previous connected stage to first stages
        firstStages.addAll(unconnectedStages);


        if (firstStages != null) {
            Map<IPlugin.PipelineStage, Integer> stageToGeneration = new HashMap<>();
            PipelineTemplate template = this.templateProperty.get();
            List<PipelineStageConnection> connections = template.getConnections();

            for (IPlugin.PipelineStage stage1 : firstStages) {
                if (stage1 != null) {
                    stageToGeneration.put(stage1, 0);
                }
            }
            int generation = 0;
            AtomicInteger finalGeneration = new AtomicInteger();
            while (stageToGeneration.values().contains(generation)) {
                //rudimentary loop detection
                if (generation > this.getTemplate().getStages().size()) {
                    break;
                }
                finalGeneration.set(generation);
                List<IPlugin.PipelineStage> parentStages = stageToGeneration.entrySet().stream()
                        .filter(entry -> entry.getValue() == finalGeneration.get())
                        .map(entry -> entry.getKey())
                        .collect(Collectors.toList());
                for (IPlugin.PipelineStage s : parentStages) {
                    connections.stream()
                            .filter(con -> con.getSourceStage().equals(s))
                            .map(con -> con.getDestStage())
                            .filter(stage -> stage != null)
                            .forEach(ps -> stageToGeneration.put(ps, finalGeneration.get() + 1));
                }

                generation++;
            }

            double xOffset = 0;
            double yOffset = 0;
            int padding = 30;

            double maxWidth = 0;
            for (Map.Entry<String, List<IPlugin.PipelineStage>> entryPoint : entryPoints) {
                VisualEntry entry = this.entryToVisual.get(entryPoint.getKey());

                entry.setTranslateY(yOffset);

                yOffset += entry.getBoundsInParent().getHeight() + (padding);
                // the ten is to account for the linking triangle
                double width = TextMeasurer.measureText(entry.getName(), entry.getFont()).getWidth() + entry.getSpacing() + 10;
                if (width > maxWidth) {
                    maxWidth = width;
                }
            }

            xOffset += maxWidth + padding;

            generation = 0;
            int maxGen = stageToGeneration.values().stream().max(Integer::compare).get();
            List<IPlugin.PipelineStage> generationStages = Collections.emptyList();
            while (generation <= maxGen) {
                maxWidth = 0;
                yOffset = 0;
                final int currentGen = generation;
                List<IPlugin.PipelineStage> previousGenerationsStages = generationStages;
                generationStages = stageToGeneration.entrySet().stream()
                        .filter(entry -> entry.getValue() == currentGen)
                        .map(entry -> entry.getKey())
                        .collect(Collectors.toList());
                if (!generationStages.isEmpty()) {
                    int numStagesInGeneration = generationStages.size();
                    // sort current generation based on connections to previous
                    Map<IPlugin.PipelineStage, List<Integer>> previousConnectionMap = new HashMap<>();
                    // special handling for first gen because entry point connections are stored differently than
                    // other connections
                    if (currentGen == 0) {
                        List<IPlugin.PipelineStage> connectedToEntries = generationStages.stream().filter(stage -> entryConnections.contains(stage)).collect(Collectors.toList());
                        for (IPlugin.PipelineStage stage : connectedToEntries) {
                            ArrayList<Integer> connectedIndices = new ArrayList<>();
                            for (int i = 0; i < entryPoints.size(); i++) {
                                if (entryPoints.get(i).getValue().contains(stage)) {
                                    connectedIndices.add(i);
                                }
                            }

                            previousConnectionMap.put(stage, connectedIndices);
                        }

                        for (IPlugin.PipelineStage stage : unconnectedStages) {
                            previousConnectionMap.put(stage, new ArrayList<>(Arrays.asList(new Integer[] {numStagesInGeneration})));
                        }
                    } else {
                        for (IPlugin.PipelineStage stage : generationStages) {
                            List<Integer> connectedIndices = new ArrayList<>();
                            for (int i = 0; i < previousGenerationsStages.size(); i++) {
                                template.getConnections().stream()
                                        .filter(con -> con.getDestStage().equals(stage))
                                        .forEach(con -> connectedIndices.add(previousGenerationsStages.indexOf(con.getSourceStage())));
                            }

                            previousConnectionMap.put(stage, connectedIndices);
                        }
                    }

                    generationStages.sort((stage1, stage2) -> {
                        Double averageForStage1 = previousConnectionMap.get(stage1).stream()
                                .collect(Collectors.averagingDouble(index -> (double)index));
                        Double averageForStage2 = previousConnectionMap.get(stage2).stream()
                                .collect(Collectors.averagingDouble(index -> (double)index));

                        return averageForStage1.compareTo(averageForStage2);
                    });

                    for (IPlugin.PipelineStage s : generationStages) {
                        VisualStage vs = stageToVisual.get(s);
                        vs.setTranslateX(xOffset);
                        vs.setTranslateY(yOffset);
                        if (vs.getBoundsInParent().getWidth() + vs.getBoundsInParent().getHeight() > maxWidth) {
                            maxWidth = vs.getBoundsInParent().getWidth() + vs.getBoundsInParent().getHeight();
                        }
                        // HACK: Apparently the height is the height of a single row, so multiply by the number of rows (title + outputs)
                        yOffset += vs.getBoundsInParent().getHeight() * (vs.getStage().getOutputs().size() + 1) + padding;
                    }
                    xOffset += maxWidth + padding;
                }
                generation++;
            }
        }

        Platform.runLater(() -> this.zoomToFit());
    }


    public void setTemplate(PipelineTemplate template) {
        this.templateProperty.set(template);
    }

    public PipelineTemplate getTemplate() {
        return this.templateProperty.get();
    }

    public ReadOnlyObjectProperty<PipelineTemplate> templateProperty() {
        return this.templateProperty.getReadOnlyProperty();
    }

    public BooleanProperty dirtyProperty() {
        return this.diryProperty;
    }

    public boolean removeEntry(VisualEntry entry) {
        List<IPlugin.PipelineStage> connections = getTemplate().removeEntryPoint(entry.getName());
        boolean removed = connections != null;
        if (removed) {
            removed = this.entryPoints.remove(entry);
            this.entryToVisual.remove(entry.getName());
            if (removed) {
                removed = this.removeChild(entry, STAGE_LAYER);
                if (removed) {
                    Optional<VisualConnection> con = this.entryConnections.stream()
                            .filter(vc -> vc.getSource().equals(entry))
                            .findFirst();
                    if (con.isPresent()) {
                        this.entryConnections.remove(con.get());
                        this.removeChild(con.get().getConLine(), CONNECTION_LAYER);
                    }
                } else {
                    this.entryPoints.add(entry);
                    this.entryToVisual.put(entry.getName(), entry);
                    getTemplate().addEntryPoint(entry.getName(), connections.toArray(new IPlugin.PipelineStage[connections.size()]));
                }
            } else {
                getTemplate().addEntryPoint(entry.getName(), connections.toArray(new IPlugin.PipelineStage[connections.size()]));
            }
        }

        this.diryProperty.setValue(true);
        return removed;
    }

    @Override
    public void handleKeyPressed(KeyEvent event) {
        switch (event.getCode()) {
            case SHIFT:
                createConnection.setValue(true);
                break;
            case CONTROL:
                clearConnection.setValue(true);
                break;
        }
    }

    @Override
    public void handleKeyReleased(KeyEvent event) {
        switch (event.getCode()) {
            case SHIFT:
                createConnection.setValue(false);
                break;
            case CONTROL:
                clearConnection.setValue(false);
                break;
        }
    }

    @Override
    public void handleLinkerSelected(Linker linker) {
        Linkable linkable = linker.getLinkable();
        if (linkable instanceof LinkableOutputRow) {
            Linkable dest = null;
            if (this.getSelected().size() > 0) {
                dest = this.getSelected().get(0);
            }
            if (clearConnection.get() && !createConnection.get()) {
                List<Map.Entry<PipelineStageConnection, VisualConnection>> connectionsToRemove = this.connectionToVisual.entrySet().stream()
                        .filter(entry -> entry.getValue().getSource().equals(linkable))
                        .collect(Collectors.toList());
                for (Map.Entry<PipelineStageConnection, VisualConnection> entry : connectionsToRemove) {
                    this.templateProperty.get().removeConnection(entry.getKey());
                    this.removeAll(CONNECTION_LAYER, entry.getValue().getConLine());
                    this.connectionToVisual.remove(entry.getKey());
                }
                if (!connectionsToRemove.isEmpty()) {
                    this.diryProperty.setValue(true);
                }
            } else if (createConnection.get() && !clearConnection.get()) {
                if (dest != null && dest instanceof VisualStage) {
                    PipelineStageConnection psc = new PipelineStageConnection(((LinkableOutputRow) linkable).getParentStage().getStage(), linkable.getName(), ((VisualStage) dest).getStage());
                    this.templateProperty.get().addConnection(psc);
                    VisualConnection connection = new VisualConnection(((LinkableOutputRow) linkable), ((VisualStage) dest), this.lineColorProperty);
                    this.connectionToVisual.put(psc, connection);
                    this.addChild(connection.getConLine(), CONNECTION_LAYER);
                    this.diryProperty.setValue(true);
                }
            } else {
                if (linkable.isSelected()) {
                    this.unSelect(linkable);
                } else {
                    this.select(linkable);
                }
            }
        } else if (linkable instanceof VisualStage) {
            Linkable source = null;
            if (this.getSelected().size() > 0) {
                source = this.getSelected().get(0);
            }
            if (createConnection.get() && !clearConnection.get()) {
                if (source != null) {
                    if (source instanceof LinkableOutputRow) {
                        PipelineStageConnection psc = new PipelineStageConnection(((LinkableOutputRow) source).getParentStage().getStage(), source.getName(), ((VisualStage) linkable).getStage());
                        this.templateProperty.get().addConnection(psc);
                        VisualConnection connection = new VisualConnection(source, linkable, this.lineColorProperty);
                        this.connectionToVisual.put(psc, connection);
                        this.addChild(connection.getConLine(), CONNECTION_LAYER);
                    } else if (source instanceof VisualEntry) {
                        this.templateProperty.get().addEntryPoint(source.getName(), ((VisualStage) linkable).getStage());
                        VisualConnection connection = new VisualConnection(source, linkable, this.lineColorProperty);
                        this.entryConnections.add(connection);
                        this.addChild(connection.getConLine(), CONNECTION_LAYER);
                    }
                    this.diryProperty.setValue(true);
                }
            } else if (clearConnection.get() && !createConnection.get()) {
                List<Map.Entry<PipelineStageConnection, VisualConnection>> connectionsToRemove = this.connectionToVisual.entrySet().stream()
                        .filter(entry -> entry.getValue().getDest().equals(linkable))
                        .collect(Collectors.toList());
                for (Map.Entry<PipelineStageConnection, VisualConnection> entry : connectionsToRemove) {
                    this.templateProperty.get().removeConnection(entry.getKey());
                    this.removeAll(CONNECTION_LAYER, entry.getValue().getConLine());
                    this.connectionToVisual.remove(entry.getKey());
                }

                List<VisualConnection> entryConnectionsToRemove = this.entryConnections.stream()
                        .filter(con -> con.getDest().equals(linkable))
                        .collect(Collectors.toList());
                for (VisualConnection con : entryConnectionsToRemove) {
                    this.templateProperty.get().removeEntryPointConnection(con.getSource().getName(), ((VisualStage) con.getDest()).getStage());
                    this.removeAll(CONNECTION_LAYER, con.getConLine());
                    this.entryConnections.remove(con);
                }

                if (!(connectionsToRemove.isEmpty() && entryConnectionsToRemove.isEmpty())) {
                    this.diryProperty.setValue(true);
                }
            } else {
                if (linkable.isSelected()) {
                    this.unSelect(linkable);
                } else {
                    this.select(linkable);
                }
            }
        } else if (linkable instanceof VisualEntry) {
            Linkable dest = null;
            if (this.getSelected().size() > 0) {
                dest = this.getSelected().get(0);
            }
            if (clearConnection.get() && !createConnection.get()) {
                List<VisualConnection> connectionsToRemove = this.entryConnections.stream()
                        .filter(con -> con.getSource().equals(linkable))
                        .collect(Collectors.toList());
                for (VisualConnection con : connectionsToRemove) {
                    this.templateProperty.get().removeEntryPointConnection(con.getSource().getName(), ((VisualStage) con.getDest()).getStage());
                    this.removeAll(CONNECTION_LAYER, con.getConLine());
                    this.entryConnections.remove(con);
                }
                if (!connectionsToRemove.isEmpty()) {
                    this.diryProperty.setValue(true);
                }
            } else if (createConnection.get() && !clearConnection.get()) {
                if (dest != null && dest instanceof VisualStage) {
                    this.templateProperty.get().addEntryPoint(linkable.getName(), ((VisualStage) dest).getStage());
                    VisualConnection connection = new VisualConnection(linkable, dest, this.lineColorProperty);
                    this.entryConnections.add(connection);
                    this.addChild(connection.getConLine(), CONNECTION_LAYER);
                    this.diryProperty.setValue(true);
                }
            } else {
                if (linkable.isSelected()) {
                    this.unSelect(linkable);
                } else {
                    this.select(linkable);
                }
            }
        }
    }

    @Override
    public List<Linkable> getLinkables() {
        return new ArrayList<>(this.stageToVisual.values());
    }

    @Override
    public List<LinkingConnection> getLinks() {
        return new ArrayList<>(this.connectionToVisual.values());
    }

    @Override
    public VisualStage addLinkable(IPlugin.PipelineStage stage) {
        return this.addLinkable(stage, new Point2D(0, 0));
    }

    public VisualStage addLinkable(IPlugin.PipelineStage stage, Point2D spawnLocation) {
        VisualStage vs = new VisualStage(getTemplate(), stage, this, config);
        vs.setTranslateX(spawnLocation.getX());
        vs.setTranslateY(spawnLocation.getY());
        this.addChild(vs, STAGE_LAYER);
        this.templateProperty.get().addStage(stage);
        if (templateProperty.get().getStageConfiguration(stage.getName()) == null) {
            templateProperty.get().setConfiguration(stage.getName(),
                    ((IPlugin.DefinesPipelineStages) config.pluginFor(stage.getStage())).getDefaultConfiguration(stage));
        }
        this.diryProperty.setValue(true);
        return this.stageToVisual.put(stage, vs);
    }

    @Override
    public boolean removeLinkable(IPlugin.PipelineStage stage) {
        List<PipelineStageConnection> connections = this.connectionToVisual.keySet().stream()
                .filter(con -> con.getDestStage().equals(stage) || con.getSourceStage().equals(stage))
                .collect(Collectors.toList());

        List<Node> visualConnections = connections.stream()
                .map(connection -> this.connectionToVisual.get(connection).getConLine())
                .collect(Collectors.toList());

        List<VisualConnection> entryConnections = this.entryConnections.stream()
                .filter(con -> con.getDest().equals(this.stageToVisual.get(stage)))
                .collect(Collectors.toList());

        visualConnections.addAll(entryConnections.stream()
                .map(con -> con.getConLine())
                .collect(Collectors.toList()));


        Node visualStage = this.stageToVisual.get(stage);

        boolean success = true;
        PipelineTemplate template = this.templateProperty.get();
        for (PipelineStageConnection connection : connections) {
            success &= template.removeConnection(connection);
            success &= this.connectionToVisual.remove(connection) != null;
        }

        success &= template.removeStage(stage);
        success &= this.stageToVisual.remove(stage) != null;

        for (VisualConnection connection : entryConnections) {
            success &= template.removeEntryPointConnection(connection.getSource().getName(), stage);
        }

        success &= this.removeAll(CONNECTION_LAYER, visualConnections.toArray(new Node[visualConnections.size()]));
        success &= this.removeAll(STAGE_LAYER, visualStage);

        if (success && stage.isConfigurable()) {
            this.templateProperty.get().cleanConfiguration(stage.getName());
        }

        this.diryProperty.setValue(true);
        return success;
    }

    @Override
    public LinkingConnection addLink(PipelineStageConnection connection) {
        VisualStage sourceStage = this.stageToVisual.get(connection.getSourceStage());
        VisualStage destStage = this.stageToVisual.get(connection.getDestStage());
        LinkableOutputRow row = sourceStage.getRow(connection.getOutput());
        VisualConnection vcon = new VisualConnection(row, destStage, this.lineColorProperty);
        this.addChild(vcon.getConLine(), CONNECTION_LAYER);

        this.diryProperty.setValue(true);

        return this.connectionToVisual.put(connection, vcon);
    }

    @Override
    public boolean removeLink(PipelineStageConnection connection) {
        return false;
    }

    @Override
    public boolean select(Linkable linkable) {
        // single selection at the moment, possible multi-select in the future
        this.selected.clear();
        return this.selected.add(linkable);
    }

    @Override
    public boolean unSelect(Linkable linkable) {
        return this.selected.remove(linkable);
    }

    @Override
    public ObservableList<Linkable> getSelected() {
        return this.selected;
    }

    @Override
    public List<MenuItem> getContextMenuItems() {
        List<MenuItem> items = new ArrayList<>();
        items.addAll(super.getContextMenuItems());
        Menu addStage = new Menu("Add Stage");
        this.addStageMenuItems(addStage);

        MenuItem addEntry = new ActiveMenuItem("Add Entry Point", event -> {
            Dialog<String> getName = new TextInputDialog("New Entry");
            getName.setGraphic(null);
            getName.setHeaderText(null);
            getName.setContentText("Entry Name:");
            getName.setTitle("New Entry");

            Optional<String> entryName = getName.showAndWait();
            if (entryName.isPresent() && !entryName.get().isEmpty()) {
                getTemplate().addEntryPoint(entryName.get());
                Point2D spawnLocation = getSpawnLocation(event);
                VisualEntry entry = new VisualEntry(entryName.get(), this, config);
                entry.setTranslateX(spawnLocation.getX());
                entry.setTranslateY(spawnLocation.getY());
                this.entryPoints.add(entry);
                this.entryToVisual.put(entry.getName(), entry);
                this.addChild(entry, STAGE_LAYER);
                this.dirtyProperty().setValue(true);
            }
        });


        items.add(new SeparatorMenuItem());
        items.add(addStage);
        items.add(addEntry);

        return items;
    }


    private void addStageMenuItems(final Menu topMenu) {
        for (IPlugin.DefinesPipelineStages plugin : config.enumeratePlugins(IPlugin.DefinesPipelineStages.class)) {
            Menu pluginMenu = new Menu(plugin.getName());
            if (plugin.getPipelineStages().isEmpty()) {
                MenuItem emptyItem = new MenuItem("No Stages Defined");
                pluginMenu.getItems().add(emptyItem);
            } else {
                for (IPlugin.PipelineStage stage : plugin.getPipelineStages()) {
                    IPlugin.PipelineStage clone = new IPlugin.PipelineStage(stage.isConfigurable(), stage.getName(), stage.getStage(), stage.getOutputs());
                    MenuItem addStageItem = new ActiveMenuItem(stage.getName(), event -> {
                        Point2D spawnLocation = getSpawnLocation(event);
                        this.addLinkable(clone, spawnLocation);
                    });

                    pluginMenu.getItems().add(addStageItem);
                }
            }

            topMenu.getItems().add(pluginMenu);
        }
    }

    private Point2D getSpawnLocation(ActionEvent event) {
        Point2D spawnLocation = new Point2D(0, 0);
        if (event.getSource() instanceof MenuItem) {
            MenuItem parent = ((MenuItem) event.getSource());
            while(parent.getParentMenu() != null) {
                parent = parent.getParentMenu();
            }
            if (((MenuItem) event.getSource()).getParentPopup() instanceof ContextMenu) {
                spawnLocation = new Point2D(parent.getParentPopup().getAnchorX(), parent.getParentPopup().getAnchorY());
                spawnLocation = this.getLayer(STAGE_LAYER).screenToLocal(spawnLocation);
            }
        }

        return spawnLocation;
    }
}
