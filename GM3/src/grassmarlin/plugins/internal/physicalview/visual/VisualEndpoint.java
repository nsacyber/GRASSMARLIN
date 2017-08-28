package grassmarlin.plugins.internal.physicalview.visual;

import com.sun.javafx.collections.ObservableSetWrapper;
import grassmarlin.plugins.internal.physicalview.StageProcessPhysicalGraphElements;
import grassmarlin.plugins.internal.physicalview.graph.Segment;
import grassmarlin.session.HardwareAddress;
import grassmarlin.session.pipeline.LogicalAddressMapping;
import grassmarlin.ui.common.IDraggable;
import grassmarlin.ui.common.ImageDirectoryWatcher;
import grassmarlin.ui.common.VisualImageList;
import javafx.beans.binding.DoubleExpression;
import javafx.beans.binding.When;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.ObservableSet;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

public class VisualEndpoint extends VBox implements IDraggable, IHasControlPoint {
    public final static ObjectProperty<Paint> TEXT_FILL_GATEWAY = new SimpleObjectProperty<>(Color.GREEN);
    public final static ObjectProperty<Paint> TEXT_FILL_NORMAL = new SimpleObjectProperty<>(Color.WHITE);

    public final static Map<String, Node> VISUAL_ANNOTATIONS = new HashMap<String, Node>() {
        {
            this.put(StageProcessPhysicalGraphElements.ANNOTATION_GATEWAY, null);
            this.put(StageProcessPhysicalGraphElements.ANNOTATION_REDIRECTED, null);
            this.put(StageProcessPhysicalGraphElements.ANNOTATION_ROUTE_ERROR, null);
            this.put(StageProcessPhysicalGraphElements.ANNOTATION_UNREACHABLE, null);
        }
    };


    private final ImageView icon;
    private final Label title;
    private final VisualImageList annotationImages;
    private final VBox labels;

    private final Segment segment;
    private final HardwareAddress address;

    private final ObservableSet<String> annotations;
    private final Map<LogicalAddressMapping, Node> lookupMappings;

    public VisualEndpoint(final Segment segment, final HardwareAddress address, final ImageDirectoryWatcher<Image> watcher) {
        this.segment = segment;
        this.address = address;
        this.annotations = new ObservableSetWrapper<>(new HashSet<>());
        this.lookupMappings = new HashMap<>();

        this.icon = new ImageView();
        this.title = new Label();
        this.annotationImages = new VisualImageList(watcher);
        this.labels = new VBox();

        this.initComponents();

        watcher.startWatching(this.annotations, "annotations", this.annotationImages.getImageList());

        this.makeDraggable(true);
    }

    private void initComponents() {
        //TODO: Initialize icon

        this.title.setText(this.address.toString());
        this.title.setTextFill(Color.WHITE);

        final VBox headerBlock = new VBox();
        headerBlock.setBackground(new Background(new BackgroundFill(Color.BLACK, new CornerRadii(4.0), null)));
        headerBlock.setOpaqueInsets(new Insets(2.0, 8.0, 2.0, 2.0));
        headerBlock.getChildren().addAll(
                this.title,
                this.annotationImages,
                this.labels
        );
        headerBlock.visibleProperty().bind(this.title.visibleProperty().or(this.annotationImages.visibleProperty()).or(this.labels.visibleProperty()));
        this.getChildren().addAll(
                this.icon,
                headerBlock
        );
    }


    public void addMapping(final LogicalAddressMapping mapping) {
        final Label labelNew = new Label(mapping.getLogicalAddress().toString());

        labelNew.textFillProperty().bind(new When(new SimpleListProperty<>(this.labels.getChildren()).sizeProperty().greaterThan(1)).then(TEXT_FILL_GATEWAY).otherwise(TEXT_FILL_NORMAL));
        lookupMappings.put(mapping, labelNew);
        this.labels.getChildren().add(labelNew);
    }
    public void removeMapping(final LogicalAddressMapping mapping) {
        this.labels.getChildren().remove(lookupMappings.remove(mapping));
    }

    public void updateAnnotations(final Collection<String> annotations) {
        this.annotations.retainAll(annotations);
        this.annotations.addAll(annotations);
    }

    public HardwareAddress getAddress() {
        return this.address;
    }

    //TODO: Better point handling for VisualEndpoint
    @Override
    public DoubleExpression getTerminalX() {
        return this.translateXProperty().add(this.widthProperty().divide(2.0));
    }
    @Override
    public DoubleExpression getTerminalY() {
        return this.translateYProperty().add(this.heightProperty().divide(2.0));
    }
    @Override
    public DoubleExpression getControlX() {
        return this.translateXProperty().add(this.widthProperty().divide(2.0));
    }
    @Override
    public DoubleExpression getControlY() {
        return this.translateYProperty().add(this.heightProperty().divide(2.0));
    }
}
