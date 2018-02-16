package iadgov.visualpipeline;

import grassmarlin.common.fxobservables.FxDoubleProperty;
import grassmarlin.common.fxobservables.FxStringProperty;
import grassmarlin.session.pipeline.AbstractStage;
import grassmarlin.ui.common.IDraggable;
import grassmarlin.ui.common.NodeOffsetBinding;
import javafx.beans.binding.BooleanExpression;
import javafx.beans.binding.DoubleExpression;
import javafx.beans.binding.When;
import javafx.beans.property.DoubleProperty;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.text.Text;

import java.time.LocalDate;
import java.time.Month;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

public class VisualAbstractStage extends VBox implements IDraggable {
    private final AbstractStage<?> stage;
    private final Node linkTarget;
    private final NodeOffsetBinding linkAsTarget;
    private final Map<String, NodeOffsetBinding> linksAsSource;

    private final DoubleProperty queueFillRatio;


    public VisualAbstractStage(final AbstractStage<?> stage) {
        this.stage = stage;
        this.linksAsSource = new HashMap<>();
        this.queueFillRatio = new FxDoubleProperty(0.0);

        this.makeDraggable(true);

        this.linkTarget = new Circle(4.0, Color.GREEN);

        this.initComponents();

        //Binding has to wait until the full construction has been performed.
        this.linkAsTarget = new NodeOffsetBinding(this.linkTarget, this);
    }

    private final DragContext dragContext = new DragContext();
    @Override
    public DragContext getDragContext() {
        return this.dragContext;
    }


    private void initComponents() {
        final LocalDate today = LocalDate.now();
        final boolean showSpm = today.getMonth() == Month.MARCH && today.getDayOfMonth() == 31;
        final DoubleExpression bindingQueue = this.stage.getQueue();

        this.setBackground(new Background(new BackgroundFill(VisualPipeline.VISUAL_ELEMENT_CLIENT_FILL, VisualPipeline.VISUAL_ELEMENT_CORNER_RADII, null)));
        this.setBorder(new Border(new BorderStroke(Color.BLACK, BorderStrokeStyle.SOLID, VisualPipeline.VISUAL_ELEMENT_CORNER_RADII, new BorderWidths(1.0))));

        final HBox containerTitle = new HBox(VisualPipeline.VISUAL_ELEMENT_PADDING);
        containerTitle.setBackground(new Background(new BackgroundFill(VisualPipeline.VISUAL_ELEMENT_TITLE_FILL, VisualPipeline.VISUAL_ELEMENT_CORNER_RADII_TOP_ONLY, null)));
        final HBox containerContent = new HBox(VisualPipeline.VISUAL_ELEMENT_PADDING * 2.0);
        final GridPane contentStats = new GridPane();
        contentStats.setOpaqueInsets(new Insets(VisualPipeline.VISUAL_ELEMENT_PADDING));
        final VBox containerOutputs = new VBox(VisualPipeline.VISUAL_ELEMENT_PADDING);

        this.getChildren().addAll(containerTitle, containerContent);
        containerContent.getChildren().addAll(contentStats, containerOutputs);

        // == Header ==
        final Text title = new Text(this.stage.getName());
        title.setFill(VisualPipeline.VISUAL_ELEMENT_TITLE_TEXT_FILL);
        containerTitle.getChildren().addAll(linkTarget, title);
        this.linkTarget.translateYProperty().bind(containerTitle.heightProperty().subtract(((Circle)linkTarget).radiusProperty().multiply(2.0)).divide(2.0));

        // == Stats ==
        //Progress bar spans all columns in the first row.
        final ProgressBar queueState = new ProgressBar();
        queueState.prefWidthProperty().bind(contentStats.widthProperty());
        if(showSpm) {
            queueState.styleProperty().bind(new When(bindingQueue.isEqualTo(-1.0, 0.0)).then("-fx-accent: red;").otherwise(""));
        }
        queueFillRatio.bind(bindingQueue);
        queueState.progressProperty().bind(queueFillRatio);
        contentStats.add(queueState, 0, 0, 3, 1);

        contentStats.add(new Text("Accept Delay"), 0, 1);
        final Label labelAcceptDelay = new Label("");
        labelAcceptDelay.textProperty().bind(new FxStringProperty(this.stage.getAcceptDelay().asString()));
        labelAcceptDelay.setMinWidth(50.0);
        contentStats.add(labelAcceptDelay, 1, 1);
        contentStats.add(new Text("ms"), 2, 1);

        contentStats.add(new Text("Inputs Accepted"), 0, 2);
        final Text textInputsAccepted = new Text();
        textInputsAccepted.textProperty().bind(new FxStringProperty(this.stage.getInputsAccepted().asString()));
        contentStats.add(textInputsAccepted, 1, 2);
        //contentStats.add(new Text(""), 2, 2);

        contentStats.add(new Text("Inputs Processed"), 0, 3);
        final Text textInputsProcessed = new Text();
        textInputsProcessed.textProperty().bind(new FxStringProperty(this.stage.getInputsProcessed().asString()));
        contentStats.add(textInputsProcessed, 1, 3);
        //contentStats.add(new Text("ms"), 2, 3);

        contentStats.add(new Text("Outputs Processed"), 0, 4);
        final Text textOutputsProcessed = new Text();
        textOutputsProcessed.textProperty().bind(new FxStringProperty(this.stage.getOutputsProcessed().asString()));
        contentStats.add(textOutputsProcessed, 1, 4);
        //contentStats.add(new Text("ms"), 2, 4);

        contentStats.add(new Text("Outputs Dropped"), 0, 5);
        final Text textOutputsDropped = new Text();
        textOutputsDropped.textProperty().bind(new FxStringProperty(this.stage.getOutputsDropped().asString()));
        contentStats.add(textOutputsDropped, 1, 5);
        //contentStats.add(new Text("ms"), 2, 5);

        if(showSpm) {
            final Text textSpm = new Text("Super Pursuit Mode");
            final Text textSpmValue = new Text("Off");
            contentStats.add(textSpm, 0, 6);
            contentStats.add(textSpmValue, 1, 6);

            textSpm.visibleProperty().bind(bindingQueue.isEqualTo(-1.0, 0.0));
            textSpmValue.visibleProperty().bind(bindingQueue.isEqualTo(-1.0, 0.0));
        }



        // == Ouptuts ==
        for(final String output : this.stage.getOutputs().stream().sorted().collect(Collectors.toList()) ) {
            final HBox containerOutput = new HBox(VisualPipeline.VISUAL_ELEMENT_PADDING);
            final Circle link = new Circle(4.0, Color.GREEN);
            final Pane spacer = new Pane();
            containerOutput.getChildren().addAll(spacer, new Text(output), link);
            HBox.setHgrow(spacer, Priority.ALWAYS);
            link.translateYProperty().bind(containerTitle.heightProperty().subtract(((Circle)linkTarget).radiusProperty().multiply(2.0)).divide(2.0));
            containerOutputs.getChildren().add(containerOutput);
            linksAsSource.put(output, new NodeOffsetBinding(link, this));
        }
    }

    public NodeOffsetBinding getLinkPoint() {
        return this.linkAsTarget;
    }

    public NodeOffsetBinding getLinkPoint(final String output) {
        return this.linksAsSource.get(output);
    }

    public BooleanExpression queueSaturatedProperty() {
        return this.queueFillRatio.greaterThan(0.9);
    }
    public BooleanExpression queueBusyProperty() {
        return this.queueFillRatio.greaterThan(0.1);
    }
}
