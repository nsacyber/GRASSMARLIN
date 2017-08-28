package iadgov.visualpipeline;

import grassmarlin.session.pipeline.AbstractStage;
import grassmarlin.session.pipeline.Pipeline;
import grassmarlin.ui.common.ZoomableScrollPane;
import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.scene.Node;
import javafx.scene.control.Tab;
import javafx.scene.control.TreeItem;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class VisualTab extends Tab {
    protected static final String LAYER_NODES = "Nodes";
    protected static final String LAYER_EDGES = "Edges";
    public static final CornerRadii VISUAL_ELEMENT_CORNER_RADII = new CornerRadii(4.0);
    public static final CornerRadii VISUAL_ELEMENT_CORNER_RADII_TOP_ONLY = new CornerRadii(4.0, 4.0, 0.0, 0.0, false);
    public static final Paint VISUAL_ELEMENT_TITLE_FILL = Color.DARKGREY;
    public static final Paint VISUAL_ELEMENT_CLIENT_FILL = Color.BEIGE;
    public static final Paint VISUAL_ELEMENT_TITLE_TEXT_FILL = Color.WHITE;
    public static final double VISUAL_ELEMENT_PADDING = 2.0;

    protected Pipeline pipeline;

    protected final StatePollingScheduler poller;

    protected final ZoomableScrollPane zsp;
    protected final Pane layoutTab;

    private final TreeItem<Object> root;

    public VisualTab(final ObjectProperty<Pipeline> pipelineProperty) {
        super("Visual Pipeline");

        this.root = new TreeItem<>();

        this.poller = new StatePollingScheduler();

        this.zsp = new ZoomableScrollPane(null, null, LAYER_EDGES, LAYER_NODES);
        this.layoutTab = new Pane();

        this.initComponents();
        this.setPipeline(pipelineProperty.get());

        pipelineProperty.addListener(this::handlePipelineChanged);
    }

    private void handlePipelineChanged(ObservableValue<? extends Pipeline> value, final Pipeline oldValue, final Pipeline newValue) {
        if(!Platform.isFxApplicationThread()) {
            Platform.runLater(() -> {
                this.setPipeline(newValue);
            });
        } else {
            this.setPipeline(newValue);
        }
    }

    public void terminate() {
        this.poller.terminate();
    }

    private void initComponents() {
        this.zsp.prefWidthProperty().bind(this.layoutTab.widthProperty());
        this.zsp.prefHeightProperty().bind(this.layoutTab.heightProperty());
        this.layoutTab.getChildren().add(this.zsp);

        this.setContent(layoutTab);
    }

    public void setPipeline(final Pipeline pipeline) {
        this.pipeline = pipeline;

        this.zsp.clear();
        if(this.pipeline != null) {
            final HashMap<AbstractStage<?>, VisualAbstractStage> stages = new HashMap<>();
            final Map<Node, List<Node>> dependencies = new HashMap<>();

            for(final AbstractStage<?> stage : pipeline.getStages()) {
                final VisualAbstractStage visual = new VisualAbstractStage(stage, this.poller);
                stages.put(stage, visual);
                this.zsp.addChild(visual, "Nodes");
                dependencies.put(visual, new LinkedList<>());
            }

            //Second pass to enumerate connections, now that we have visual entities for every stage
            for(final AbstractStage<?> stage : pipeline.getStages()) {
                for(final String output : stage.getOutputs()) {
                    final Consumer<Object> consumer = stage.targetOf(output);
                    if(consumer != null && consumer instanceof AbstractStage) {
                        this.zsp.addChild(new VisualConnection(stages.get(stage), output, stages.get(consumer)), LAYER_EDGES);
                        dependencies.get(stages.get(consumer)).add(stages.get(stage));
                    }
                }
            }

            for(final String entry : pipeline.getEntryPoints()) {
                final VisualPipelineEntry visual = new VisualPipelineEntry(entry);
                this.zsp.addChild(visual);
                dependencies.put(visual, new LinkedList<>());

                final Consumer<Object> consumer = pipeline.getEntryForLabel(entry);
                if(consumer != null && consumer instanceof AbstractStage) {
                    this.zsp.addChild(new VisualConnection(visual, stages.get(consumer)), LAYER_EDGES);
                    dependencies.get(stages.get(consumer)).add(visual);
                }
            }

            //Everything is built, so now we can position all the nodes.  To do this we have the pre-built dependency graph.
            Platform.runLater(() -> {
                double offsetX = 0.0;
                while(!dependencies.isEmpty()) {
                    final List<Node> nodesInCurrentGeneration = dependencies.entrySet().stream().filter(entry -> entry.getValue().size() == 0).map(entry -> entry.getKey()).collect(Collectors.toList());
                    if(nodesInCurrentGeneration.size() != 0) {
                        final double widthGeneration = 32.0 + nodesInCurrentGeneration.stream().mapToDouble(node -> node.getLayoutBounds().getWidth()).max().orElse(0.0);

                        double offsetY = 0.0;
                        for(Node node : nodesInCurrentGeneration) {
                            node.setTranslateX(offsetX);
                            node.setTranslateY(offsetY);
                            offsetY += 16.0 + node.getLayoutBounds().getHeight();

                            dependencies.remove(node);
                        }
                        dependencies.values().stream().forEach(list -> list.removeAll(nodesInCurrentGeneration));

                        offsetX += widthGeneration;
                    } else {
                        // There are loops; drop everything in the current generation then end.
                        double offsetY = 0.0;
                        for(Node node : dependencies.keySet()) {
                            node.setTranslateX(offsetX);
                            node.setTranslateY(offsetY);
                            offsetY += 16.0 + node.getLayoutBounds().getHeight();
                        }
                        break;
                    }
                }

                this.zsp.zoomToFit();
            });
        }
    }

    public TreeItem<Object> getNavigationRoot() {
        return root;
    }
}
