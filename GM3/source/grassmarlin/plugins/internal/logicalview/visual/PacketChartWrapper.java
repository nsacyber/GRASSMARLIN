package grassmarlin.plugins.internal.logicalview.visual;

import com.sun.javafx.collections.ObservableListWrapper;
import grassmarlin.RuntimeConfiguration;
import grassmarlin.common.ListSizeBinding;
import grassmarlin.common.svg.Svg;
import grassmarlin.plugins.internal.logicalview.GraphLogicalEdge;
import grassmarlin.ui.common.controls.Chart;
import grassmarlin.ui.common.menu.ActiveMenuItem;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.geometry.Rectangle2D;
import javafx.scene.control.*;
import javafx.scene.input.MouseButton;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.stage.FileChooser;

import java.io.File;
import java.nio.file.Paths;
import java.util.LinkedList;
import java.util.List;

//TODO: Finish making this a generic chart wrapper, allow toggling of X/Y zoom independently, move location-sensitive context menu generation into chart, etc.
public class PacketChartWrapper<T> extends GridPane {
    protected static class LegendEntry<T> extends HBox {
        private final Chart.Series<T> series;

        public LegendEntry(final Chart.Series<T> series) {
            this.series = series;

            initComponents();
        }

        private void initComponents() {
            final Circle glyph = new Circle();
            glyph.setStroke(series.getColor());
            glyph.setFill(Color.TRANSPARENT);
            glyph.setStrokeWidth(2.0);
            glyph.setRadius(5.0);

            Label lblTitle = new Label(series.getName());

            this.getChildren().addAll(glyph, lblTitle);
        }
    }

    protected final Chart<T, ?, ?> chart;
    protected final ObservableList<Rectangle2D> zoomHistory;
    protected final VBox legend;
    protected final Label tooltip;

    protected final Rectangle polyHighlight;
    protected double xInitialHighlight;

    protected final ContextMenu menu;
    protected final Menu menuOpenInWireshark;
    protected final Menu menuSeries;

    public PacketChartWrapper(final Chart<T, ?, ?> chart) {
        this.chart = chart;
        this.zoomHistory = new ObservableListWrapper<>(new LinkedList<>());
        this.legend = new VBox();
        this.polyHighlight = new Rectangle();
        this.tooltip = new Label();

        this.menu = new ContextMenu();
        this.menuOpenInWireshark = new Menu("Open in _Wireshark");
        this.menuSeries = new Menu("_Series");

        initComponents();
    }

    private void initComponents() {
        final StackPane paneChart = new StackPane();

        polyHighlight.setVisible(false);
        polyHighlight.heightProperty().bind(chart.heightProperty());
        polyHighlight.setFill(Color.color(0.0, 0.0, 1.0, 0.5));

        tooltip.setVisible(false);
        tooltip.setBackground(new Background(new BackgroundFill(Color.BEIGE, null, null)));
        tooltip.setPadding(new Insets(2, 2, 2, 2));
        tooltip.setBorder(new Border(new BorderStroke(Color.BLACK, BorderStrokeStyle.SOLID, null, new BorderWidths(1.0))));

        xInitialHighlight = -1.0;

        Pane overlaysChart = new Pane();
        overlaysChart.getChildren().addAll(polyHighlight, tooltip);
        overlaysChart.prefWidthProperty().bind(paneChart.widthProperty());
        overlaysChart.prefHeightProperty().bind(paneChart.heightProperty());

        paneChart.getChildren().addAll(chart, overlaysChart);

        final ScrollPane legendScroll = new ScrollPane(legend);
        legendScroll.setBackground(new Background(new BackgroundFill(Color.TRANSPARENT, null, null)));

        this.setHgap(4.0);
        this.add(legendScroll, 0, 0);
        this.add(paneChart, 1, 0);


        GridPane.setVgrow(paneChart, Priority.ALWAYS);
        GridPane.setHgrow(paneChart, Priority.ALWAYS);

        menu.getItems().addAll(
                new ActiveMenuItem("_Reset Zoom", event -> PacketChartWrapper.this.zoomReset()),
                new ActiveMenuItem("_Undo Zoom", event -> PacketChartWrapper.this.zoomPrevious())
                    .bindEnabled(new ListSizeBinding(zoomHistory).greaterThan(0)),
                new SeparatorMenuItem(),
                menuSeries,
                menuOpenInWireshark,
                new ActiveMenuItem("Export to SVG...", event -> {
                    final FileChooser dialog = new FileChooser();
                    dialog.setTitle("Export to SVG");
                    dialog.getExtensionFilters().add(new FileChooser.ExtensionFilter("SVG Files", "svg"));
                    dialog.setInitialDirectory(new File(RuntimeConfiguration.getPersistedString(RuntimeConfiguration.PersistableFields.DIRECTORY_USER_DATA)));
                    dialog.setSelectedExtensionFilter(dialog.getExtensionFilters().get(0));
                    //TODO: Get the name of the chart (it is held in the parent)
                    dialog.setInitialFileName("PacketsOverTime.svg");
                    final File target = dialog.showSaveDialog(this.getScene().getWindow());
                    if(target != null) {
                        Svg.serialize(this, Paths.get(target.getAbsolutePath()));
                    }
                })
        );

        //Tooltip Event Handlers
        overlaysChart.setOnMouseMoved(event -> {
            T point = chart.pointNearestLocation(new Point2D(event.getScreenX(), event.getScreenY()));
            if(point != null) {
                tooltip.setText(point.toString());
                tooltip.setVisible(true);
            } else {
                tooltip.setVisible(false);
            }
        });
        overlaysChart.setOnMouseExited(event -> {
            tooltip.setVisible(false);
        });
        //Zooming Event Handlers
        this.setOnMousePressed(event -> {
            menu.hide();
            if(event.getButton() == MouseButton.PRIMARY) {
                xInitialHighlight = chart.screenToLocal(event.getScreenX(), event.getScreenY()).getX();
                polyHighlight.setVisible(false);

                event.consume();
            } else if(event.isPopupTrigger()) {
                menu.show(this, event.getScreenX(), event.getScreenY());
                event.consume();
            }
        });
        this.setOnMouseDragged(event -> {
            if(event.isPrimaryButtonDown()) {
                final double x = chart.screenToLocal(event.getScreenX(), event.getScreenY()).getX();

                //TODO: Clamp to valid range rather than aborting.
                if (!chart.pointInsideViewport(chart.screenToLocal(event.getScreenX(), event.getScreenY()))) {
                    return;
                }

                final double min = Math.min(xInitialHighlight, x);
                final double max = Math.max(xInitialHighlight, x);

                polyHighlight.setLayoutX(min);
                polyHighlight.setWidth(max - min);
                polyHighlight.setVisible(true);
            }
        });
        this.setOnMouseReleased(event -> {
            if (polyHighlight.isVisible() && polyHighlight.getWidth() > 1.0) {
                event.consume();

                final double xMin = chart.viewportXForControlX(polyHighlight.getLayoutX());
                final double xMax = chart.viewportXForControlX(polyHighlight.getLayoutX() + polyHighlight.getWidth());

                final Rectangle2D rectZoom = new Rectangle2D(xMin, 0.0, xMax - xMin, 1.05);

                zoomHistory.add(rectZoom);
                chart.zoom(rectZoom);
            }
            polyHighlight.setVisible(false);
        });
        this.setOnScroll(event -> {
            //Only fire for a change in Y-scroll (even though we're scrolling X), and only process if there is at least 1 zoom on the stack (initial positions can't scroll since they are implicitly everything)
            if(event.getDeltaY() != 0.0 && zoomHistory.size() > 0) {
                final Rectangle2D rectBase = zoomHistory.get(zoomHistory.size() - 1);
                final double delta = (event.getDeltaY() > 0 ? 1.0 : -1.0) * 0.05 * rectBase.getWidth();
                final Rectangle2D rectNew = new Rectangle2D(rectBase.getMinX() + delta, 0.0, rectBase.getWidth(), rectBase.getHeight());

                zoomHistory.add(rectNew);
                chart.zoom(rectNew);

                event.consume();
            }
        });
        //Context menu handling
        this.setOnContextMenuRequested(event -> {
            event.consume();
            final Point2D screen = new Point2D(event.getScreenX(), event.getScreenY());

            menuOpenInWireshark.getItems().clear();
            List<T> records = chart.pointsNearLocation(screen);

            if(records.isEmpty()) {
                menuOpenInWireshark.setDisable(true);
            } else {
                menuOpenInWireshark.setDisable(false);
                for(final T record : records) {
                    if(record instanceof GraphLogicalEdge.PacketMetadata) {
                        final GraphLogicalEdge.PacketMetadata packet = (GraphLogicalEdge.PacketMetadata)record;
                        menuOpenInWireshark.getItems().add(new ActiveMenuItem(record.toString(), action -> {
                            RuntimeConfiguration.openPcapFile(packet.getFile(), packet.getFrame());
                        }));
                    }
                }
            }


            menu.show(this, event.getScreenX(), event.getScreenY());
        });
        this.setOnMouseClicked(event -> {
            menu.hide();
        });
    }

    public void clearSeries() {
        this.legend.getChildren().clear();
        this.chart.clearSeries();
        this.menuSeries.getItems().clear();
        this.menuSeries.setDisable(true);
        this.zoomReset();
    }

    public void addSeries(final Chart.Series<T> series) {
        //Add the series to the chart first since this is what will set the color, if no color is previously defined.
        this.chart.addSeries(series);
        this.menuSeries.setDisable(false);
        this.legend.getChildren().add(new LegendEntry<>(series));

        Circle glyph = new Circle();
        glyph.setStroke(series.getColor());
        glyph.setFill(Color.TRANSPARENT);
        glyph.setStrokeWidth(2.0);
        glyph.setRadius(5.0);
        //HACK: The glyph isn't aligned properly.  This helps the positioning but doesn't remedy some of the larger layout issues--still better than not doing it.
        glyph.setTranslateX(-5.0);
        glyph.setTranslateY(-5.0);

        CheckMenuItem itemSeries = new CheckMenuItem(series.getName(), glyph);
        itemSeries.setSelected(true);
        itemSeries.setOnAction(event -> {
            if(itemSeries.isSelected()) {
                chart.addSeries(series);
            } else {
                chart.removeSeries(series);
            }
        });

        this.menuSeries.getItems().add(itemSeries);
    }

    public void zoomReset() {
        chart.zoom(Chart.DEFAULT_VIEWPORT);
        zoomHistory.clear();
    }

    public void zoomPrevious() {
        if(zoomHistory.size() > 0) {
            zoomHistory.remove(zoomHistory.size() - 1);
            if(zoomHistory.size() > 0) {
                chart.zoom(zoomHistory.get(zoomHistory.size() - 1));
            } else {
                zoomReset();
            }
        }
    }
}
