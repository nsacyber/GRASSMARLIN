package grassmarlin.plugins.internal.physicalview.visual;

import grassmarlin.ui.common.IDraggable;
import javafx.beans.property.ObjectProperty;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;
import javafx.scene.shape.CubicCurve;

public class Wire extends CubicCurve implements IDraggable {
    private final IHasControlPoint start;
    private final IHasControlPoint stop;

    //private final SimpleDoubleProperty

    public Wire(final IHasControlPoint start, final IHasControlPoint stop) {
        this.start = start;
        this.stop = stop;

        this.startXProperty().bind(start.getTerminalX());
        this.startYProperty().bind(start.getTerminalY());
        this.controlX1Property().bind(start.getControlX());
        this.controlY1Property().bind(start.getControlY());
        this.controlX2Property().bind(stop.getControlX());
        this.controlY2Property().bind(stop.getControlY());
        this.endXProperty().bind(stop.getTerminalX());
        this.endYProperty().bind(stop.getTerminalY());

        //TODO: Deal with stroke color better
        super.setStroke(Color.BLUE);
        super.setFill(Color.TRANSPARENT);

        this.makeDraggable(false);
    }

    public ObjectProperty<Paint> colorProperty() {
        return super.strokeProperty();
    }

    @Override
    public boolean equals(final Object other) {
        return (other instanceof Wire) && (
                (this.start == ((Wire) other).start && this.stop == ((Wire) other).stop)
                ||
                (this.start == ((Wire) other).stop && this.stop == ((Wire) other).start));
    }

    public boolean connectsTo(final IHasControlPoint end) {
        return this.start == end || this.stop == end;
    }

    public IHasControlPoint otherEnd(final IHasControlPoint end) {
        return this.start == end ? this.stop : this.start;
    }

    public IHasControlPoint getStart() {
        return this.start;
    }
    public IHasControlPoint getStop() {
        return this.stop;
    }
}
