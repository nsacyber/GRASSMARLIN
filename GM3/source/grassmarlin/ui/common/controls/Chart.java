package grassmarlin.ui.common.controls;

import com.sun.javafx.geom.BaseBounds;
import grassmarlin.common.svg.Svg;
import grassmarlin.ui.common.DrawBuffer;
import grassmarlin.ui.common.TextMeasurer;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.geometry.Point2D;
import javafx.geometry.Rectangle2D;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

public abstract class Chart<T, TX extends Comparable<TX>, TY extends Comparable<TY>> extends Canvas implements Svg.PaintToSvg {
    public final static Rectangle2D DEFAULT_VIEWPORT = new Rectangle2D(-0.05, 0.0, 1.1, 1.05);
    private static final Color[] colors = new Color[] {Color.RED, Color.BLUE, Color.GREEN, Color.PURPLE, Color.ORANGE, Color.YELLOW, Color.LIGHTGREEN, Color.DARKCYAN, Color.DARKGREEN, Color.MAGENTA, Color.LIGHTBLUE, Color.DARKBLUE};

    private int idxSeriesColor = 0;

    //TODO: Use properties for Series
    public static class Series<T> {
        private final String name;
        private final List<T> data;
        private Color color;
        private final SimpleBooleanProperty visible;

        public Series(final String name) {
            this(name, null);
        }
        public Series(final String name, final Color color) {
            this.name = name;
            this.data = new ArrayList<>();
            this.color = color;
            this.visible = new SimpleBooleanProperty(true);
        }

        public List<T> getData() {
            return this.data;
        }
        public String getName() {
            return this.name;
        }
        public Color getColor() {
            return this.color;
        }
        public void setColor(final Color color) {
            this.color = color;
        }
        public BooleanProperty visibleProperty() {
            return this.visible;
        }
    }
    public static class Range<T extends Comparable<T>> {
        private final T min;
        private final T max;

        public Range(T min, T max) {
            this.min = min;
            this.max = max;
        }

        public T getMin() {
            return this.min;
        }
        public T getMax() {
            return this.max;
        }

        public boolean contains(T val) {
            if(val == null) {
                return false;
            }

            if(min != null) {
                if(min.compareTo(val) > 0) {
                    return false;
                }
            }
            if(max != null) {
                if(max.compareTo(val) < 0) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public String toString() {
            return String.format("<%s, %s>", min, max);
        }
    }
    protected static class Point<T, TX, TY> {
        private final Point2D location;
        private final TX x;
        private final TY y;
        private final T datum;
        private final Color color;

        public Point(final T datum, final TX x, final TY y, final Point2D location, final Color color) {
            this.location = location;
            this.x = x;
            this.y = y;
            this.datum = datum;
            this.color = color;
        }

        public T getDatum() {
            return this.datum;
        }
    }

    private final List<Series<T>> series;
    private final AtomicBoolean redrawPending = new AtomicBoolean(false);
    private final AtomicBoolean redrawSuspended = new AtomicBoolean(false);

    //Implementation details to be provided by subclass.
    protected abstract TX calculateXFor(final T value);
    protected abstract TY calculateYFor(final T value);
    protected abstract double normalizeX(final Range<TX> bounds, final TX value);
    protected abstract double normalizeY(final Range<TY> bounds, final TY value);
    protected abstract List<TX> generateXTicks(final Range<TX> base, final double low, final double high);
    protected abstract List<TY> generateYTicks(final Range<TY> base, final double low, final double high);
    protected abstract String formatX(final TX x);
    protected abstract String formatY(final TY y);

    private Range<TX> rangeX;
    private Range<TY> rangeY;

    protected final static double pxTickLength = 5.0;
    protected final static double pointRadius = 5.0;
    protected final static double widthLine = 1.0;

    protected double interactRadius = 15.0;
    private Font fontLabels;

    private Rectangle2D viewport;
    protected final List<Point<T, TX, TY>> renderedPoints = new ArrayList<>();
    private Rectangle2D rectChart = new Rectangle2D(0, 0, 200, 200);

    protected Chart() {
        this.series = new LinkedList<>();
        this.viewport = DEFAULT_VIEWPORT;
        this.fontLabels = Font.font("MONOSPACE", 12.0);

        super.setWidth(200.0);
        super.setHeight(200.0);
        widthProperty().addListener((observable, oldValue, newValue) -> this.redraw());
        heightProperty().addListener((observable, oldValue, newValue) -> this.redraw());
    }

    @Override
    public boolean isResizable() {
        return true;
    }

    @Override
    public double minHeight(final double width) {
        return 0.0;
    }

    @Override
    public double maxHeight(final double width) {
        return 4320.0;  //Full height of 16k display.  We need some limit due to the memory considerations, but we don't want to restrict any normal use.  Considering how long I've spent asking for a 1080p display it seems reasonable to treat 16k as an upper limit.
    }

    @Override
    public double minWidth(final double height) {
        return 0.0;
    }

    @Override
    public double maxWidth(final double height) {
        return 7680.0; //Full width of 16k display; see maxHeight for justification.
    }

    @Override
    public double prefWidth(final double height) {
        return 0.0;
    }

    @Override
    public double prefHeight(final double width) {
        return 0.0;
    }

    @Override
    public void resize(final double width, final double height) {
        setWidth(width);
        setHeight(height);
    }

    public void clearSeries() {
        this.series.clear();
        idxSeriesColor = 0;
        this.redraw();
    }

    public void addSeries(final Series<T> series) {
        if(series.getColor() == null) {
            series.setColor(colors[idxSeriesColor++ % colors.length]);
        }
        this.series.add(series);
        this.redraw();
    }

    public void removeSeries(final Series<T> series) {
        if(this.series.remove(series)) {
            this.redraw();
        }
    }

    public void setXRange(final Range<TX> range) {
        this.rangeX = range;
        this.redraw();
    }

    public void setYRange(final Range<TY> range) {
        this.rangeY = range;
        this.redraw();
    }

    public void suspendLayout(boolean state) {
        if(state) {
            redrawSuspended.set(true);
        } else {
            if(redrawSuspended.getAndSet(false) && redrawPending.get()) {
                this.redraw();
            }
        }
    }

    public void zoom(Rectangle2D viewport) {
        this.viewport = viewport;
        this.redraw();
    }

    public double viewportXForControlX(final double pxX) {
        final Rectangle2D rect = this.rectChart;
        return (pxX - rect.getMinX()) / rect.getWidth() * viewport.getWidth() + viewport.getMinX();
    }
    public boolean pointInsideViewport(final Point2D pt) {
        return rectChart.contains(pt);
    }

    protected double chartXFromDataX(Range<TX> axisX, TX x) {
        if(axisX.getMin().equals(axisX.getMax())) {
            return 0.5 * rectChart.getWidth() + rectChart.getMinX();
        } else {
            final double normalizedX = this.normalizeX(axisX, x);
            return (normalizedX - viewport.getMinX()) / viewport.getWidth() * rectChart.getWidth() + rectChart.getMinX();
        }
    }
    protected double chartYFromDataY(Range<TY> axisY, TY y) {
        return (1 - (this.normalizeY(axisY, y) - viewport.getMinY()) / viewport.getHeight()) * rectChart.getHeight() + rectChart.getMinY();
    }

    protected final void redraw() {
        redrawPending.set(true);
        if(!Platform.isFxApplicationThread()) {
            Platform.runLater(this::redraw);
        } else {
            //If drawing is suspended, just abort now--there will be a new redraw call when redrawing is un-suspended.
            if(!redrawSuspended.get()) {
                //If we haven't requested a redraw since the last draw we can skip this redraw because nothing will have changed.
                if(redrawPending.getAndSet(false)) {
                    final DrawBuffer buffer = new DrawBuffer();
                    this.paint(buffer);

                    GraphicsContext gc = this.getGraphicsContext2D();
                    gc.clearRect(0.0, 0.0, this.getWidth(), this.getHeight());
                    buffer.renderTo(gc);
                }
            }
        }
    }

    @SuppressWarnings("OptionalGetWithoutIsPresent") //isPresent() is not needed because we've ensured the collection on which the stream operations is based is non-empty and no filtering is performed.
    @Override
    public void paint(final DrawBuffer gc) {
        //Copy data to prevent tearing and broken iterators.  If we have to hold any locks to get data, they can be released after this bit.
        final HashMap<Series, LinkedList<T>> data = new HashMap<>();
        for(final Series<T> series : this.series) {
            final LinkedList<T> seriesData = new LinkedList<>(series.getData());
            seriesData.sort((o1, o2) -> calculateXFor(o1).compareTo(calculateXFor(o2)));
            data.put(series, seriesData);
        }

        //Early-abort checks.
        if(data.size() == 0) {
            return;
        }
        if(data.values().stream().flatMap(LinkedList::stream).count() == 0) {
            return;
        }

        //Calculate the axis ranges
        Range<TX> axisX;
        Range<TY> axisY;
        //TODO: Optimize X checks--we just sorted by X so there are some improvements that can be made.
        if(rangeX != null) {
            axisX = new Range<>(
                    rangeX.min == null ? data.values().stream().flatMap(LinkedList::stream).map(this::calculateXFor).min(TX::compareTo).get() : rangeX.min,
                    rangeX.max == null ? data.values().stream().flatMap(LinkedList::stream).map(this::calculateXFor).max(TX::compareTo).get() : rangeX.max
            );
        } else {
            axisX = new Range<>(
                    data.values().stream().flatMap(LinkedList::stream).map(this::calculateXFor).min(TX::compareTo).get(),
                data.values().stream().flatMap(LinkedList::stream).map(this::calculateXFor).max(TX::compareTo).get()
            );
        }
        if(rangeY != null) {
            axisY = new Range<>(
                    rangeY.min == null ? data.values().stream().flatMap(LinkedList::stream).map(this::calculateYFor).min(TY::compareTo).get() : rangeY.min,
                    rangeY.max == null ? data.values().stream().flatMap(LinkedList::stream).map(this::calculateYFor).max(TY::compareTo).get() : rangeY.max
            );
        } else {
            axisY = new Range<>(
                    data.values().stream().flatMap(LinkedList::stream).map(this::calculateYFor).min(TY::compareTo).get(),
                    data.values().stream().flatMap(LinkedList::stream).map(this::calculateYFor).max(TY::compareTo).get()
            );
        }
        final List<TX> ticksX = generateXTicks(axisX, viewport.getMinX(), viewport.getMaxX());
        final List<TY> ticksY = generateYTicks(axisY, viewport.getMinY(), viewport.getMaxY());

        //Calculate the width of the widest Y-axis label and the height of the tallest X-axis label.
        double maxWidth = 0.0;
        double pxMinSpacingBetweenTicks = 0.0;
        for(final TY tickY : ticksY) {
            final BaseBounds bounds = TextMeasurer.measureText(formatY(tickY), this.fontLabels);
            maxWidth = Math.max(maxWidth, bounds.getWidth());
            pxMinSpacingBetweenTicks = Math.max(pxMinSpacingBetweenTicks, 2.0 * bounds.getHeight());
        }
        double maxHeight = 0.0;
        for(final TX tickX : ticksX) {
            //X-axis labels are displayed at a 30-degree incline.
            //The approximate width of the rotated text is 0.7*{width}
            //The distance from the top of the bounding to the origin from which text should be drawn is 0.5*{length} + 0.87*{height}
            //We could just calculate bounds using a 30-degree rotation, but math works too. (Note to my high school Trig teacher:  I wasn't actually paying attention in class, but I accepted the value of the material and learned it just the same)
            final BaseBounds bounds = TextMeasurer.measureText(formatX(tickX), this.fontLabels);
            maxHeight = Math.max(maxHeight, 0.5 * bounds.getWidth() + 0.87 * bounds.getHeight());
        }
        if(maxWidth <= 0.0 || maxHeight <= 0.0) {
            return;
        }

        final Rectangle2D sizeAxisLabel = new Rectangle2D(0.0, 0.0, maxWidth, maxHeight);

        //If the display can't render the labels, don't render anything.
        if(this.getWidth() <= sizeAxisLabel.getWidth() || this.getHeight() <= sizeAxisLabel.getHeight()) {
            return;
        }

        try {
            rectChart = new Rectangle2D(sizeAxisLabel.getWidth() + pxTickLength, 0.0, getWidth() - sizeAxisLabel.getWidth() - pxTickLength, getHeight() - sizeAxisLabel.getHeight() - pxTickLength);
        } catch(IllegalArgumentException ex) {
            //The width or height was calculated to <= 0
            return;
        }

        //Render series data and build tooltip cache.
        this.renderedPoints.clear();
        for(final Map.Entry<Series, LinkedList<T>> entry : data.entrySet()) {
            Point2D ptPrev = null;

            for(final T value : entry.getValue()) {
                final TX x = this.calculateXFor(value);
                final TY y = this.calculateYFor(value);

                //Add rectViewport.getMinY() instead of subtracting because we're mirroring the Y coordinate around the X axis.
                Point2D ptNew = new Point2D(
                        chartXFromDataX(axisX, x),
                        chartYFromDataY(axisY, y));
                Point<T, TX, TY> pt = new Point<>(
                        value,
                        x, y,
                        ptNew, entry.getKey().getColor()
                );
                renderedPoints.add(pt);

                if(ptPrev != null) {
                    gc.drawLine(entry.getKey().getColor(), widthLine, ptPrev.getX(), ptPrev.getY(), ptNew.getX(), ptNew.getY());
                }
                gc.drawOval(entry.getKey().getColor(), widthLine, null, ptNew.getX() - pointRadius, ptNew.getY() - pointRadius, pointRadius * 2.0, pointRadius * 2.0);
                ptPrev = ptNew;
            }
        }

        //Render axes
        //Clear the axis area (because the point circles probably overlap the axis label area)
        gc.drawRectangle(null, 0.0, Color.WHITE, 0.0, 0.0, sizeAxisLabel.getWidth() + pxTickLength, this.getHeight());
        gc.drawRectangle(null, 0.0, Color.WHITE, 0.0, this.getHeight() - sizeAxisLabel.getHeight() - pxTickLength, this.getWidth(), sizeAxisLabel.getHeight() + pxTickLength);
        //Draw the axes

        gc.drawLine(Color.BLACK, 0.5, rectChart.getMinX(), rectChart.getMinY(), rectChart.getMinX(), rectChart.getMaxY());
        gc.drawLine(Color.BLACK, 0.5, rectChart.getMinX(), rectChart.getMaxY(), rectChart.getMaxX(), rectChart.getMaxY());

        double pxLast = -pxMinSpacingBetweenTicks;
        for(final TX tickX : ticksX) {
            final double pxX = chartXFromDataX(axisX, tickX);
            if(pxLast + pxMinSpacingBetweenTicks > pxX) {
                continue;
            }
            pxLast = pxX;
            gc.drawLine(Color.BLACK, 0.5, pxX, rectChart.getMaxY(), pxX, rectChart.getMaxY() + pxTickLength);
            final String textLabel = formatX(tickX);
            final BaseBounds boundsText = TextMeasurer.measureText(textLabel, this.fontLabels);
            final double offsetY = 0.5 * boundsText.getWidth() + 0.87 * boundsText.getHeight();
            final double offsetX = -0.87 * boundsText.getWidth();

            gc.save();
            // Translate then rotate to rotate text around the local origin rather than rotating around the canvas origin.
            // Rotating and drawing at an offset would result in drawing at the rotated offset coordinates.
            gc.translate(pxX + offsetX, rectChart.getMaxY() + offsetY);
            gc.rotate(-30.0);
            gc.drawText(null, 0.5, Color.BLACK, this.fontLabels, 0.0, 0.0, textLabel);
            gc.restore();
        }
        for(final TY tickY : ticksY) {
            final double pxY = chartYFromDataY(axisY, tickY);
            gc.drawLine(Color.BLACK, 1.0, rectChart.getMinX() - pxTickLength, pxY, rectChart.getMinX(), pxY);
            final String textLabel = formatY(tickY);
            gc.drawText(null, 0.5, Color.BLACK, this.fontLabels, 0.0, pxY + TextMeasurer.measureText(textLabel, this.fontLabels).getHeight(), textLabel);
        }
    }

    public List<T> pointsNearLocation(final Point2D screenNear) {
        final Point2D ptNear = this.screenToLocal(screenNear);
        return renderedPoints.stream()
                .filter(point -> point.location.distance(ptNear) < interactRadius)
                .sorted((o1, o2) -> Double.compare(o1.location.distance(ptNear), o2.location.distance(ptNear)))
                .limit(50)  //Completely arbitrary number; drawing thousands of menu items causes performance issues and if you have more than 50 items, you should be filtering or zooming.  The screen real estate for 50 items ends up at an estimated 12.5 vertical inches, which would be a screen size of 21 inches (4:3) or 26 inches (16:9), which is a completely reasonable size.  Generally if you have to scroll a context menu, something went wrong.  So 50.  Because all these numbers make it look reasonable and not imposing a limit crashes my machine during load testing.
                .map(point -> point.getDatum())
                .collect(Collectors.toList());
    }

    public T pointNearestLocation(final Point2D screenNear) {
        final Point2D ptNear = this.screenToLocal(screenNear);
        final Optional<T> result = renderedPoints.stream()
                .filter(point -> point.location.distance(ptNear) < this.interactRadius)
                .sorted((o1, o2) -> Double.compare(o1.location.distance(ptNear), o2.location.distance(ptNear)))
                .map(point -> point.getDatum())
                .findFirst();
        return result.isPresent() ? result.get() : null;
    }
}
