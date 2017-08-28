package grassmarlin.plugins.internal.logicalview.visual;

import grassmarlin.plugins.internal.logicalview.Plugin;
import grassmarlin.ui.common.NodeOffsetBinding;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.beans.binding.*;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.shape.Line;
import javafx.scene.shape.Polygon;
import javafx.scene.transform.Rotate;
import javafx.scene.transform.Scale;

// An earlier version of this class used a CubicCurve instead of a Line.  JavaFx just doesn't like drawing curves, though; performance was pretty awful and migrating to lines solved that problem.  We never bothered to refactor on account of the name, however.
public class DirectedCurve extends Group {
    public interface PointExpression {
        DoubleExpression xProperty();
        DoubleExpression yProperty();
        DoubleExpression xControlProperty();
        DoubleExpression yControlProperty();
    }
    public static class PointExpressionBase implements PointExpression {
        private final Node base;
        private final Node root;
        //These are set up for lazy evaluation so we can add this to the scene graph to calculate the correct offset bindings
        private NodeOffsetBinding offset = null;
        private DoubleExpression offsetX = null;
        private DoubleExpression offsetY = null;
        private DoubleExpression controlX = null;
        private DoubleExpression controlY = null;
        private final DoubleExpression x;
        private final DoubleExpression y;
        private final DoubleExpression xControl;
        private final DoubleExpression yControl;

        public PointExpressionBase(final Node base, final Node root, final DoubleExpression x, final DoubleExpression y) {
            this(base, root, x, y, null, null);
        }
        public PointExpressionBase(final Node base, final Node root, final DoubleExpression x, final DoubleExpression y, final DoubleExpression xControl, final DoubleExpression yControl) {
            this.base = base;
            this.root = root;
            this.x = x;
            this.y = y;
            this.xControl = xControl;
            this.yControl = yControl;
        }

        @Override
        public DoubleExpression xProperty() {
            if(offset == null) {
                offset = new NodeOffsetBinding(this.base, root);
            }
            if(offsetX == null) {
                offsetX = offset.getX().add(x);
            }
            return offsetX;
        }
        @Override
        public DoubleExpression yProperty() {
            if(offset == null) {
                offset = new NodeOffsetBinding(this.base, root);
            }
            if(offsetY == null) {
                offsetY = offset.getY().add(y);
            }
            return offsetY;
        }

        @Override
        public DoubleExpression xControlProperty() {
            if(controlX == null) {
                if(xControl == null) {
                    controlX = xProperty();
                } else {
                    controlX = xProperty().add(xControl);
                }
            }
            return controlX;
        }
        @Override
        public DoubleExpression yControlProperty() {
            if(controlY == null) {
                if(yControl == null) {
                    controlY = yProperty();
                } else {
                    controlY = yProperty().add(yControl);
                }
            }
            return controlY;
        }

        @Override
        public String toString() {
            return String.format("[PointExpressionBase {%s, %s}]", this.x.get(), this.y.get());
        }
    }

    private final static double[] ptsArrow = new double[] {0.0, -2.0, 2.8, 0.0, 0.0, 2.0};

    private final Line line;
    private final EstimatedCubicCurve curve;

    private final Polygon arrowAtDestination;
    private final Polygon arrowAtSource;

    private final Scale scaleEndpoints;
    private final Rotate rotateAtSource;
    private final Rotate rotateAtDestination;

    private final RebindableNumberBinding startXProperty;
    private final RebindableNumberBinding startYProperty;
    private final RebindableNumberBinding controlX1Property;
    private final RebindableNumberBinding controlY1Property;
    private final RebindableNumberBinding controlX2Property;
    private final RebindableNumberBinding controlY2Property;
    private final RebindableNumberBinding endXProperty;
    private final RebindableNumberBinding endYProperty;

    private final SimpleDoubleProperty strokeWidthProperty;

    protected static class AngledOffsetXBinding extends DoubleBinding {
        private final NumberExpression x;
        private final NumberExpression deg;
        private final NumberExpression offset;

        public AngledOffsetXBinding(final NumberExpression x, final NumberExpression deg, final NumberExpression offset) {
            this.x = x;
            this.deg = deg;
            this.offset = offset;

            super.bind(x, deg, offset);
        }

        @Override
        public double computeValue() {
            return this.x.doubleValue() + Math.cos(Math.PI / 180.0 * this.deg.doubleValue()) * this.offset.doubleValue();
        }
    }

    protected static class AngledOffsetYBinding extends DoubleBinding {
        private final NumberExpression y;
        private final NumberExpression deg;
        private final NumberExpression offset;

        public AngledOffsetYBinding(final NumberExpression y, final NumberExpression deg, final NumberExpression offset) {
            this.y = y;
            this.deg = deg;
            this.offset = offset;

            super.bind(y, deg, offset);
        }

        @Override
        public double computeValue() {
            return this.y.doubleValue() + Math.sin(Math.PI / 180.0 * this.deg.doubleValue()) * this.offset.doubleValue();
        }
    }

    protected static class ControlPointAngleBinding extends DoubleBinding {
        private final NumberExpression cx;
        private final NumberExpression cy;
        private final NumberExpression x;
        private final NumberExpression y;

        public ControlPointAngleBinding(final NumberExpression cx, final NumberExpression cy, final NumberExpression x, final NumberExpression y) {
            this.x = x;
            this.y = y;
            this.cx = cx;
            this.cy = cy;

            super.bind(cx, cy, x, y);
        }

        @Override
        public double computeValue() {
            double dx = x.doubleValue() - cx.doubleValue();
            double dy = y.doubleValue() - cy.doubleValue();

            if(dx == 0.0 && dy == 0.0) {
                return 0.0;
            } else {
                //The rotate transform requires input in radians, but the offset binding requires degrees, so there is no good choice.
                return 180.0 / Math.PI * Math.atan2(dy, dx);
            }
        }
    }

    private static class RebindableNumberBinding extends DoubleBinding {
        private DoubleExpression value = null;

        public RebindableNumberBinding() {

        }

        public RebindableNumberBinding(final DoubleExpression source) {
            this.bindTo(source);
        }

        public void bindTo(final DoubleExpression source) {
            if(this.value == source) {
                return;
            }

            if(this.value != null) {
                super.unbind(this.value);
            }
            this.value = source;
            if(this.value != null) {
                super.bind(this.value);
            }
            invalidate();
        }

        @Override
        public double computeValue() {
            if(this.value == null) {
                return 0.0;
            } else {
                return this.value.doubleValue();
            }
        }
    }

    private final VisualLogicalVertex.ContentRow rowStart;
    private final VisualLogicalVertex.ContentRow rowEnd;
    private PointExpression pointStart = null;
    private PointExpression pointEnd = null;


    public DirectedCurve(final Plugin plugin, final VisualLogicalVertex.ContentRow rowSource, final VisualLogicalVertex.ContentRow rowDestination, final DoubleExpression multiplierOpacity, final DoubleExpression multiplierWeight) {
        this.line = new Line();

        this.rowStart = rowSource;
        this.rowEnd = rowDestination;

        this.startXProperty = new RebindableNumberBinding();
        this.startYProperty = new RebindableNumberBinding();
        this.controlX1Property = new RebindableNumberBinding();
        this.controlY1Property = new RebindableNumberBinding();
        this.controlX2Property = new RebindableNumberBinding();
        this.controlY2Property = new RebindableNumberBinding();
        this.endXProperty = new RebindableNumberBinding();
        this.endYProperty = new RebindableNumberBinding();

        this.strokeWidthProperty = new SimpleDoubleProperty(1.0);

        this.arrowAtDestination = new Polygon(ptsArrow);
        this.arrowAtSource = new Polygon(ptsArrow);

        //The scale will respond to the edge weighting; we need the scale to compute the size of the arrow for the offset point for the curve.
        this.scaleEndpoints = new Scale();
        this.scaleEndpoints.xProperty().bind(this.line.strokeWidthProperty());
        this.scaleEndpoints.yProperty().bind(this.line.strokeWidthProperty());

        //Calculate the angle at the end of the provided points--we can't use the actual curve since that would lead to a recursive definition (angle depends on curve endpoints depends on angle...).
        final ControlPointAngleBinding degAtSource = new ControlPointAngleBinding(new When(plugin.useCurvedEdgesBinding()).then(this.controlX1Property).otherwise(this.endXProperty), new When(plugin.useCurvedEdgesBinding()).then(this.controlY1Property).otherwise(this.endYProperty), this.startXProperty, this.startYProperty);
        final ControlPointAngleBinding degAtDestination = new ControlPointAngleBinding(new When(plugin.useCurvedEdgesBinding()).then(this.controlX2Property).otherwise(this.startXProperty), new When(plugin.useCurvedEdgesBinding()).then(this.controlY2Property).otherwise(this.startYProperty), this.endXProperty, this.endYProperty);

        this.rotateAtSource = new Rotate();
        this.rotateAtSource.angleProperty().bind(degAtSource);
        this.rotateAtDestination = new Rotate();
        this.rotateAtDestination.angleProperty().bind(degAtDestination);

        final NumberBinding startX = new When(this.arrowAtSource.visibleProperty()).then(new AngledOffsetXBinding(this.startXProperty, degAtSource, this.scaleEndpoints.xProperty().multiply(-2.8))).otherwise(this.startXProperty);
        final NumberBinding startY = new When(this.arrowAtSource.visibleProperty()).then(new AngledOffsetYBinding(this.startYProperty, degAtSource, this.scaleEndpoints.yProperty().multiply(-2.8))).otherwise(this.startYProperty);
        final NumberBinding endX = new When(this.arrowAtDestination.visibleProperty()).then(new AngledOffsetXBinding(this.endXProperty, degAtDestination, this.scaleEndpoints.xProperty().multiply(-2.8))).otherwise(this.endXProperty);
        final NumberBinding endY = new When(this.arrowAtDestination.visibleProperty()).then(new AngledOffsetYBinding(this.endYProperty, degAtDestination, this.scaleEndpoints.yProperty().multiply(-2.8))).otherwise(this.endYProperty);

        this.line.startXProperty().bind(startX);
        this.line.startYProperty().bind(startY);
        this.line.endXProperty().bind(endX);
        this.line.endYProperty().bind(endY);

        this.line.strokeProperty().bind(new BoundLinearGradient(startX, startY, endX, endY, rowSource.backgroundColorBinding(), rowDestination.backgroundColorBinding()));
        this.curve = new EstimatedCubicCurve(
                25, // 10 - Crude, 25 - Adequate, 100 - Nice, 1000 - I have processors to spare and/or I hate my computer.
                startX, startY,
                controlX1Property, controlY1Property,
                controlX2Property, controlY2Property,
                endX, endY,
                rowSource.backgroundColorBinding(),
                rowDestination.backgroundColorBinding()
        );
        this.curve.strokeWidthProperty().bind(this.line.strokeWidthProperty());
        this.line.strokeWidthProperty().bind(this.strokeWidthProperty.multiply(multiplierWeight));

        this.opacityProperty().bind(multiplierOpacity);

        this.line.visibleProperty().bind(plugin.useStraightEdgesBinding());
        this.curve.visibleProperty().bind(plugin.useCurvedEdgesBinding());

        //The arrows are always aligned to the end of the curve.
        this.arrowAtSource.getTransforms().addAll(this.scaleEndpoints, this.rotateAtSource);
        this.arrowAtDestination.getTransforms().addAll(this.scaleEndpoints, this.rotateAtDestination);

        this.arrowAtSource.translateXProperty().bind(this.line.startXProperty());
        this.arrowAtSource.translateYProperty().bind(this.line.startYProperty());
        this.arrowAtDestination.translateXProperty().bind(this.line.endXProperty());
        this.arrowAtDestination.translateYProperty().bind(this.line.endYProperty());

        this.arrowAtSource.fillProperty().bind(rowSource.backgroundColorBinding());
        this.arrowAtDestination.fillProperty().bind(rowDestination.backgroundColorBinding());

        this.arrowAtSource.setStrokeWidth(0.0);
        this.arrowAtDestination.setStrokeWidth(0.0);

        this.getChildren().addAll(this.line, this.curve, this.arrowAtSource, this.arrowAtDestination);

        this.startXProperty.addListener(this.handlerBoundPointInvalidated);
        this.startYProperty.addListener(this.handlerBoundPointInvalidated);
        this.endXProperty.addListener(this.handlerBoundPointInvalidated);
        this.endYProperty.addListener(this.handlerBoundPointInvalidated);

        this.requestLayout();
    }

    @Override
    protected void layoutChildren() {
        this.evaluateShortestPath();
        super.layoutChildren();
    }

    private final InvalidationListener handlerBoundPointInvalidated = this::handleBoundPointInvalidated;
    private void handleBoundPointInvalidated(Observable observable) {
        this.requestLayout();
    }

    /**
     * This method can easily cause crippling performance issues as it is called
     * every time an endpoint of an edge moves.  Especially during layout
     * operations, this is quite common.  Because of this, we skip on error
     * checking that is probably extraneous--generally we would be testing
     * conditions that have been coded to not exist.  Future work, bugs, plugins,
     * etc. might change this, however the design is such that we don't need to
     * test for errors like null ContentRows, empty BindPoint lists, etc.
     *
     * The default view uses vertices with 4 bind points, and the number of
     * vertices should match the number of logical graph vertices, N.
     *
     * N is expected to be of similar magnitude to the number of physical
     * entities; this number may be high, but we expect resources to scale
     * accordingly.
     *
     * The total number of endpoints (root + child logical addresses) tends to be
     * a linear multiple (k) of the number of root vertices (In practice, 10 times
     * tends to be close, but experience suggests that our test data sets are not
     * representative of any actual data).  The number of edges tends to be closer
     * to N-squared.
     *
     * The default view, where all nodes are collapsed, consists of N nodes with
     * at most N**2 edges.  At the opposite end of the spectrum there are N*k nodes
     * with up to (N*k)**2 edges.  Every edge (noting that these are, specifically,
     * visually distinct edges) has a corresponding DirectedCurve and every
     * DirectedCurve must be updated every frame when performing layout.  Cutting
     * a few corners on good form that shouldn't be necessary pays dividends on
     * performance.
     *
     * Taking a method that is called N**2 times and sticking a polynomial loop in
     * it feels like a bad idea, but, for a given graph, when the fewest
     * number of nodes is shown, the loop executes 16 times (4 bind points each)
     * and when the most are shown it drops to 4 (2 bind points each).
     * Technically this doesn't have to hold for all data types, but in practice
     * these serve as reasonable bounds.
     *
     * This puts the expected worst-case inner iteration count at between
     * 16N**2 and 4(N*k)**2 times.
     *
     * We can't avoid the N-squared component, so we're stuck considering 16 vs 4k**2,
     * and since k is often 10 in practice, the polynomial loop is called most often
     * when it executes the fewest times--and the best alternative would be
     * consecutive, rather than nested, loops, but that would still be 4 total
     * iterations and produces a sub-par visual component.  In short, if the nested
     * loops cause problems, un-nesting them won't solve the problem.
     *
     * This isn't particularly noteworthy, certainly not enough to warrant a comment
     * this long, but I'm about to go on vacation and if I don't document this I'll
     * be stuck trying to remember it after I get back.
     */
    protected void evaluateShortestPath() {
        //Since we are likely to rebind start/end x/y properties, we prevent this from being reentrant.
        PointExpression shortestStart = null;
        PointExpression shortestEnd = null;

        double shortest = Double.MAX_VALUE;
        for (final PointExpression ptStart : rowStart.getBindPoints()) {
            for (final PointExpression ptEnd : rowEnd.getBindPoints()) {
                final double dx = ptEnd.xProperty().get() - ptStart.xProperty().get();
                final double dy = ptEnd.yProperty().get() - ptStart.yProperty().get();

                /*  Given the performance focus for this bit of code, we've profiled several
                 * alternatives.  This can certainly change with hardware, JVM, and other
                 * constraints, so if something else performs better and you need that edge,
                 * go for it.  Performance was measured as total loop runtime with various
                 * data sets outside the GUI framework normalized to the baseline.
                 *
                 * dx * dx + dy * dy is considered the baseline (1.0)
                 * dx + dy is about 0.6, but is also wrong.
                 * Math.abs(dx) + Math.abs(dy) is about 6.0
                 * (dx * (dx < 0.0 ? -1.0 : 1.0)) + (dy * (dy < 0.0 ? -1 : 1.0)) is about 1.05
                 * The previous line, but implemented as "if" statements is about 1.15
                 *
                 *  The actual results end up with a decent range of variation, but averages
                 * tend to be worse than the baseline average.  Allowing for some error in
                 * sampling, the fact that the correct calculation is highly competitive lends
                 * itself to accepting that as the baseline--we're also talking about
                 * 1ns/iteration baseline on test hardware.  At 10k visible edges, this will
                 * cause a clear slowdown during animated layouts.  This is why we recommend
                 * disabling animated layouts when working with data sets that large.
                 */
                final double distanceSquared = dx * dx + dy * dy;
                if (distanceSquared < shortest) {
                    shortest = distanceSquared;
                    shortestStart = ptStart;
                    shortestEnd = ptEnd;
                }
            }
        }

        if (this.pointEnd != shortestEnd) {
            this.pointEnd = shortestEnd;
            this.endXProperty.bindTo(this.pointEnd.xProperty());
            this.endYProperty.bindTo(this.pointEnd.yProperty());
            this.controlX2Property.bindTo(this.pointEnd.xControlProperty());
            this.controlY2Property.bindTo(this.pointEnd.yControlProperty());
        }
        if (this.pointStart != shortestStart) {
            this.pointStart = shortestStart;
            this.startXProperty.bindTo(this.pointStart.xProperty());
            this.startYProperty.bindTo(this.pointStart.yProperty());
            this.controlX1Property.bindTo(this.pointStart.xControlProperty());
            this.controlY1Property.bindTo(this.pointStart.yControlProperty());
        }
    }

    public DoubleProperty strokeWidthProperty() {
        return this.strokeWidthProperty;
    }

    public BooleanProperty isSourceArrowVisibleProperty() {
        return this.arrowAtSource.visibleProperty();
    }

    public BooleanProperty isDestinationArrowVisibleProperty() {
        return this.arrowAtDestination.visibleProperty();
    }

}
