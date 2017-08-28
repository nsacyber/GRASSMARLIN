package iadgov.visualpipeline;

import javafx.beans.binding.When;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.shape.CubicCurve;

public class VisualConnection extends Group {
    public static DoubleProperty CONTROL_POINT_MAGNITUDE = new SimpleDoubleProperty(0.7);

    private final CubicCurve curve;
    private final VisualAbstractStage target;

    public VisualConnection(final VisualPipelineEntry source, final VisualAbstractStage target) {
        this.curve = new CubicCurve();
        this.target = target;

        this.curve.startXProperty().bind(source.getLinkPoint().getX().add(source.translateXProperty()));
        this.curve.startYProperty().bind(source.getLinkPoint().getY().add(source.translateYProperty()));

        this.curve.controlX1Property().bind(this.curve.startXProperty().add( this.curve.endXProperty().subtract(this.curve.startXProperty()).multiply(CONTROL_POINT_MAGNITUDE) ));
        this.curve.controlY1Property().bind(this.curve.startYProperty());

        this.curve.controlX2Property().bind(this.curve.endXProperty().add( this.curve.startXProperty().subtract(this.curve.endXProperty()).multiply(CONTROL_POINT_MAGNITUDE) ));
        this.curve.controlY2Property().bind(this.curve.endYProperty());

        this.curve.endXProperty().bind(target.getLinkPoint().getX().add(target.translateXProperty()));
        this.curve.endYProperty().bind(target.getLinkPoint().getY().add(target.translateYProperty()));

        initComponents();
    }

    public VisualConnection(final VisualAbstractStage source, final String output, final VisualAbstractStage target) {
        this.curve = new CubicCurve();
        this.target = target;

        this.curve.startXProperty().bind(source.getLinkPoint(output).getX().add(source.translateXProperty()));
        this.curve.startYProperty().bind(source.getLinkPoint(output).getY().add(source.translateYProperty()));

        this.curve.controlX1Property().bind(this.curve.startXProperty().add( this.curve.endXProperty().subtract(this.curve.startXProperty()).multiply(CONTROL_POINT_MAGNITUDE) ));
        this.curve.controlY1Property().bind(this.curve.startYProperty());

        this.curve.controlX2Property().bind(this.curve.endXProperty().add( this.curve.startXProperty().subtract(this.curve.endXProperty()).multiply(CONTROL_POINT_MAGNITUDE) ));
        this.curve.controlY2Property().bind(this.curve.endYProperty());

        this.curve.endXProperty().bind(target.getLinkPoint().getX().add(target.translateXProperty()));
        this.curve.endYProperty().bind(target.getLinkPoint().getY().add(target.translateYProperty()));

        initComponents();
    }

    private void initComponents() {
        this.curve.setFill(Color.TRANSPARENT);
        this.curve.strokeProperty().bind(new When(this.target.queueBusyProperty()).then(new When(this.target.queueSaturatedProperty()).then(Color.RED).otherwise(Color.ORANGE)).otherwise(Color.GREEN));

        this.getChildren().add(curve);
    }
}
