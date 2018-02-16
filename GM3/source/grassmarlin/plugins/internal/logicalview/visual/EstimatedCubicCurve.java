package grassmarlin.plugins.internal.logicalview.visual;

import javafx.beans.binding.NumberBinding;
import javafx.beans.binding.ObjectExpression;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.scene.Group;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.shape.StrokeLineCap;

import java.util.ArrayList;
import java.util.List;

public class EstimatedCubicCurve extends Group {
    private static class PointExpression {
        public final NumberBinding x;
        public final NumberBinding y;
        public final ObjectExpression<Color> color;

        private PointExpression(final NumberBinding x, final NumberBinding y, final ObjectExpression<Color> color) {
            this.x = x;
            this.y = y;
            this.color = color;
        }
    }

    private final PointExpression[] segments;
    private final Gradient gradient;
    private final SimpleDoubleProperty strokeWidthProperty;

    public EstimatedCubicCurve(
            final int cntSegments,  //TODO: Make this bindable and rebuild--that would allow a mutable graphic quality to be set
            final NumberBinding x1, final NumberBinding y1,
            final NumberBinding cx1, final NumberBinding cy1,
            final NumberBinding cx2, final NumberBinding cy2,
            final NumberBinding x2, final NumberBinding y2,
            final ObjectExpression<Color> colorSource,
            final ObjectExpression<Color> colorDestination) {

        this.segments = new PointExpression[cntSegments + 1];
        this.gradient = new Gradient(colorSource, colorDestination);
        this.strokeWidthProperty = new SimpleDoubleProperty(1.0);

        this.segments[0] = new PointExpression(x1, y1, this.gradient.colorAt(0.0));
        for(int idx = 1; idx < cntSegments; idx++) {
            final double offset = (double)idx / (double)(cntSegments + 1);
            this.segments[idx] = new PointExpression(
                    x1.multiply(Math.pow(1.0 - offset, 3.0)).add(cx1.multiply(3.0 * Math.pow(1.0 - offset, 2.0) * offset)).add(cx2.multiply(3.0 * (1.0 - offset) * Math.pow(offset, 2.0))).add(x2.multiply(Math.pow(offset, 3.0))),
                    y1.multiply(Math.pow(1.0 - offset, 3.0)).add(cy1.multiply(3.0 * Math.pow(1.0 - offset, 2.0) * offset)).add(cy2.multiply(3.0 * (1.0 - offset) * Math.pow(offset, 2.0))).add(y2.multiply(Math.pow(offset, 3.0))),
                    gradient.colorAt((double)idx / (double)cntSegments)
            );
        }
        this.segments[cntSegments] = new PointExpression(x2, y2, this.gradient.colorAt(1.0));

        final List<Line> lines = new ArrayList<>(this.segments.length - 1);
        for(int idx = 0; idx < this.segments.length - 1; idx++) {
            final Line line = new Line();
            line.setStrokeLineCap(StrokeLineCap.ROUND);
            line.startXProperty().bind(this.segments[idx].x);
            line.startYProperty().bind(this.segments[idx].y);
            line.endXProperty().bind(this.segments[idx + 1].x);
            line.endYProperty().bind(this.segments[idx + 1].y);
            line.strokeProperty().bind(this.segments[idx].color);
            line.strokeWidthProperty().bind(this.strokeWidthProperty);

            lines.add(line);
        }
        this.getChildren().addAll(lines);
    }

    public DoubleProperty strokeWidthProperty() {
        return this.strokeWidthProperty;
    }
}
