package iadgov.visualpipeline;

import grassmarlin.session.pipeline.AbstractStage;
import grassmarlin.session.pipeline.Pipeline;
import grassmarlin.ui.common.ZoomableScrollPane;
import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.scene.Node;
import javafx.scene.layout.CornerRadii;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public class VisualPipeline extends ZoomableScrollPane {
    protected static final String LAYER_NODES = "Nodes";
    protected static final String LAYER_EDGES = "Edges";
    public static final CornerRadii VISUAL_ELEMENT_CORNER_RADII = new CornerRadii(4.0);
    public static final CornerRadii VISUAL_ELEMENT_CORNER_RADII_TOP_ONLY = new CornerRadii(4.0, 4.0, 0.0, 0.0, false);
    public static final Paint VISUAL_ELEMENT_TITLE_FILL = Color.DARKGREY;
    public static final Paint VISUAL_ELEMENT_CLIENT_FILL = Color.BEIGE;
    public static final Paint VISUAL_ELEMENT_TITLE_TEXT_FILL = Color.WHITE;
    public static final double VISUAL_ELEMENT_PADDING = 2.0;

    protected Pipeline pipeline;

    public VisualPipeline(final ObjectProperty<Pipeline> pipelineProperty) {
        super(null, null, LAYER_EDGES, LAYER_NODES);

        this.setPipeline(pipelineProperty.get());

        pipelineProperty.addListener(this::handlePipelineChanged);
        if(pipelineProperty.get() != null) {
            this.handlePipelineChanged(pipelineProperty, null, pipelineProperty.get());
        }
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

    public void setPipeline(final Pipeline pipeline) {
        this.pipeline = pipeline;

        this.clear();
        if(this.pipeline != null) {
            final HashMap<AbstractStage<?>, VisualAbstractStage> stages = new HashMap<>();
            final Map<Node, List<Node>> dependencies = new HashMap<>();

            for(final AbstractStage<?> stage : pipeline.getStages()) {
                final VisualAbstractStage visual = new VisualAbstractStage(stage);
                stages.put(stage, visual);
                this.addChild(visual, "Nodes");
                visual.layout();
                dependencies.put(visual, new LinkedList<>());
            }

            //Second pass to enumerate connections, now that we have visual entities for every stage
            for(final AbstractStage<?> stage : pipeline.getStages()) {
                for(final String output : stage.getOutputs()) {
                    final Consumer<Object> consumer = stage.targetOf(output);
                    if(consumer != null && consumer instanceof AbstractStage) {
                        this.addChild(new VisualConnection(stages.get(stage), output, stages.get(consumer)), LAYER_EDGES);
                        dependencies.get(stages.get(consumer)).add(stages.get(stage));
                    }
                }
            }

            for(final String entry : pipeline.getEntryPoints()) {
                final VisualPipelineEntry visual = new VisualPipelineEntry(entry);
                this.addChild(visual);
                visual.layout();
                dependencies.put(visual, new LinkedList<>());

                final Consumer<Object> consumer = pipeline.getEntryForLabel(entry);
                if(consumer != null && consumer instanceof AbstractStage) {
                    this.addChild(new VisualConnection(visual, stages.get(consumer)), LAYER_EDGES);
                    dependencies.get(stages.get(consumer)).add(visual);
                }
            }

            double offsetX = 0.0;
            while (!dependencies.isEmpty()) {
                final List<Node> nodesInCurrentGeneration = dependencies.entrySet().stream().filter(entry -> entry.getValue().size() == 0).map(entry -> entry.getKey()).collect(Collectors.toList());
                if (nodesInCurrentGeneration.size() != 0) {
                    final double widthGeneration = 64.0 + nodesInCurrentGeneration.stream().mapToDouble(node -> node.getBoundsInParent().getWidth()).max().orElse(0.0);

                    double offsetY = 0.0;
                    for (Node node : nodesInCurrentGeneration) {
                        node.setTranslateX(offsetX);
                        node.setTranslateY(offsetY);
                        offsetY += 32.0 + node.getBoundsInParent().getHeight();

                        dependencies.remove(node);
                    }
                    dependencies.values().forEach(list -> list.removeAll(nodesInCurrentGeneration));

                    offsetX += widthGeneration;
                } else {
                    // There are loops; drop everything in the current generation then end.
                    double offsetY = 0.0;
                    for (Node node : dependencies.keySet()) {
                        node.setTranslateX(offsetX);
                        node.setTranslateY(offsetY);
                        offsetY += 32.0 + node.getBoundsInParent().getHeight();
                    }
                    break;
                }
            }

            //TODO: The zoomtoFit doesn't work right on loading.
            this.zoomToFit();
        }
    }
}
