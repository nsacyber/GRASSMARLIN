package grassmarlin.ui.pipeline;

import javafx.beans.binding.Bindings;
import javafx.beans.property.ObjectProperty;
import javafx.scene.paint.Paint;
import javafx.scene.shape.CubicCurve;

public class VisualConnection implements LinkingConnection<Linkable, Linkable> {

    private Linkable sourceOutput;
    private Linkable destStage;

    private CubicCurve conLine;

    public VisualConnection(final Linkable source, final Linkable dest, ObjectProperty<Paint> color) {
        this.sourceOutput = source;
        this.destStage = dest;

        this.conLine = new CubicCurve();
        this.conLine.setFill(null);
        this.conLine.startXProperty().bind(source.getSourceXProperty());
        this.conLine.startYProperty().bind(source.getSourceYProperty());
        this.conLine.controlX1Property().bind(Bindings.add(source.getSourceControlXProperty(), Bindings.createDoubleBinding(() -> Math.abs(source.getSourceYProperty().doubleValue() - dest.getLinkYProperty().doubleValue()), source.getSourceYProperty(), dest.getLinkYProperty()).multiply(0.2)));
        this.conLine.controlY1Property().bind(source.getSourceControlYProperty());
        this.conLine.endXProperty().bind(dest.getLinkXProperty());
        this.conLine.endYProperty().bind(dest.getLinkYProperty());
        this.conLine.controlX2Property().bind(Bindings.subtract(dest.getLinkControlXProperty(), Bindings.createDoubleBinding(() -> Math.abs(source.getSourceYProperty().doubleValue() - dest.getLinkYProperty().doubleValue()), source.getSourceYProperty(), dest.getLinkYProperty()).multiply(0.2)));
        this.conLine.controlY2Property().bind(dest.getLinkControlYProperty());
        this.conLine.strokeProperty().bind(color);
    }

    @Override
    public CubicCurve getConLine() {
        return this.conLine;
    }

    @Override
    public Linkable getSource() {
        return this.sourceOutput;
    }

    @Override
    public Linkable getDest() {
        return this.destStage;
    }
}
