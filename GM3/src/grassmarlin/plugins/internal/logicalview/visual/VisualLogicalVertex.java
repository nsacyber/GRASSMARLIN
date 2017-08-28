package grassmarlin.plugins.internal.logicalview.visual;

import com.sun.javafx.collections.ObservableListWrapper;
import grassmarlin.plugins.internal.logicalview.GraphLogicalVertex;
import grassmarlin.plugins.internal.logicalview.ICanHasContextMenu;
import grassmarlin.session.LogicalAddress;
import grassmarlin.session.LogicalVertex;
import grassmarlin.session.pipeline.LogicalAddressMapping;
import grassmarlin.session.serialization.XmlSerializable;
import grassmarlin.ui.common.*;
import javafx.application.Platform;
import javafx.beans.binding.BooleanExpression;
import javafx.beans.binding.DoubleExpression;
import javafx.beans.binding.ObjectExpression;
import javafx.beans.binding.When;
import javafx.beans.property.*;
import javafx.collections.ListChangeListener;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Point2D;
import javafx.scene.Node;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.MenuItem;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import java.util.*;

// A VisualLogicalVertex wraps around a GraphLogicalVertex and displays its members.
//  The display is made using a series of rows of content
//  Main Row
//  [ TRANSPARENT ] [ OPAQUE BACKGROUND ]
//  [ExternalIcons]|[Icons] | [Logical Address]
//  Subsequent rows are present for child logical addresses.  The external icons are always blank on these rows (?)
//
public class VisualLogicalVertex extends VBox implements IDraggable, IAltClickable, ICanHasContextMenu, XmlSerializable {
    private static final CornerRadii CORNER_RADII = new CornerRadii(4.0);
    private static final Insets INSETS = new Insets(4.0, 4.0, 4.0, 4.0);
    private static final ReadOnlyDoubleWrapper ZERO = new ReadOnlyDoubleWrapper(0.0);

    public class ContentRow extends HBox {
        private final HBox internal = new HBox();
        private final HBox external = new HBox();
        private final ObservableList<Node> contentExternal;
        private final ObservableList<Node> contentInternal;
        private final Set<DirectedCurve.PointExpression> bindPoints;
        private final ObjectProperty<Background> background;
        private final Text label;
        private final Pane paneInternal = new Pane();
        private final DoubleExpression visualWidth = external.widthProperty().add(paneInternal.widthProperty());
        private final ObjectExpression<Color> backgroundFill;

        protected ContentRow(final LogicalAddress<?> address) {
            this.contentExternal = new ObservableListWrapper<>(new LinkedList<>());
            this.contentExternal.addListener(this::handle_UpdateExternalContent);
            this.contentInternal = new ObservableListWrapper<>(new LinkedList<>());
            this.contentInternal.addListener(this::handle_UpdateInternalContent);
            this.bindPoints = new HashSet<>();
            this.label = new Text();
            this.background = new SimpleObjectProperty<>();

            this.internal.setOpaqueInsets(INSETS);
            this.internal.getChildren().add(this.label);

            this.backgroundFill = VertexColorAssignment.backgroundColorFor(address.getClass());
            this.background.bind(new BackgroundFromColor(backgroundFill, CORNER_RADII, null));


            final Pane spacer = new Pane();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            this.label.fillProperty().bind(VertexColorAssignment.textColorFor(address.getClass()));

            paneInternal.backgroundProperty().bind(this.background);
            paneInternal.setOpaqueInsets(INSETS);
            paneInternal.getChildren().add(internal);

            getChildren().addAll(spacer, external, paneInternal);
        }

        private void handle_UpdateExternalContent(ListChangeListener.Change c) {
            this.external.getChildren().clear();
            this.external.getChildren().addAll(this.contentExternal);
            VisualLogicalVertex.this.visualization.markAsModified();
        }
        private void handle_UpdateInternalContent(ListChangeListener.Change c) {
            this.internal.getChildren().clear();
            this.internal.getChildren().addAll(this.contentInternal);
            this.internal.getChildren().add(this.label);
            VisualLogicalVertex.this.visualization.markAsModified();
        }

        public ObjectExpression<Color> backgroundColorBinding() {
            return backgroundFill;
        }

        public ObjectProperty<Background> internalBackgroundProperty() {
            return this.background;
        }
        public StringProperty textProperty() {
            return this.label.textProperty();
        }

        public Region getInternalNode() {
            //HACK: Tightly coupled to structure initialized in initComponents().
            return (Region)getChildren().get(2);
        }

        public ObservableList<Node> getExternalContent() {
            return this.contentExternal;
        }

        public Set<DirectedCurve.PointExpression> getBindPoints() {
            return this.bindPoints;
        }
    }

    private final GraphLogicalVertex vertex;
    private final Map<LogicalAddressMapping, ContentRow> rowsByChild;
    private final BooleanProperty showChildren;
    private final ContentRow rowRoot;
    private final DirectedCurve.PointExpression ptRootBottom;
    private final VisualImageList visualImages;
    private final LogicalVisualization visualization;

    public ImageDirectoryWatcher.MappedImageList getImageList() {
        return this.visualImages.getImageList();
    }

    // Context Menu Items
    private final CheckMenuItem miShowConnectionDetails;

    public VisualLogicalVertex(final LogicalVisualization visualization, final GraphLogicalVertex vertex, final ImageDirectoryWatcher.MappedImageList images) {
        this.vertex = vertex;
        this.rowsByChild = new HashMap<>();
        this.visualization = visualization;
        this.rowRoot = new ContentRow(vertex.getRootLogicalAddressMapping().getLogicalAddress());
        this.visualImages = new VisualImageList(images);

        this.miShowConnectionDetails = new CheckMenuItem("Expand Child Elements");
        this.miShowConnectionDetails.selectedProperty().addListener(observable -> {
            if(miShowConnectionDetails.isSelected()) {
                VisualLogicalVertex.this.showChildren.set(true);
                if(!VisualLogicalVertex.this.rowsByChild.isEmpty()) {
                    VisualLogicalVertex.this.rowRoot.bindPoints.remove(VisualLogicalVertex.this.ptRootBottom);
                }
                for (final LogicalVertex address : VisualLogicalVertex.this.vertex.getChildAddresses()) {
                    VisualLogicalVertex.this.addChildAddress(address);
                }

            } else {
                VisualLogicalVertex.this.showChildren.set(false);
                VisualLogicalVertex.this.rowRoot.bindPoints.add(VisualLogicalVertex.this.ptRootBottom);
                VisualLogicalVertex.this.getChildren().removeAll(VisualLogicalVertex.this.rowsByChild.values());
                VisualLogicalVertex.this.rowsByChild.clear();
            }
        });
        this.showChildren = new SimpleBooleanProperty(miShowConnectionDetails.isSelected());

        rowRoot.textProperty().bind(vertex.titleProperty());
        rowRoot.getExternalContent().add(this.visualImages);
        rowRoot.bindPoints.addAll(Arrays.asList(
            new DirectedCurve.PointExpressionBase(this.rowRoot.external, VisualLogicalVertex.this.visualization.getZsp().getChildren().get(0),
                    ZERO, this.rowRoot.heightProperty().divide(2.0),
                    this.rowRoot.visualWidth.multiply(-1.0), ZERO),
            new DirectedCurve.PointExpressionBase(this.rowRoot.external, VisualLogicalVertex.this.visualization.getZsp().getChildren().get(0),
                    this.rowRoot.visualWidth, this.rowRoot.heightProperty().divide(2.0),
                    this.rowRoot.visualWidth, ZERO),
            new DirectedCurve.PointExpressionBase(this.rowRoot.external, VisualLogicalVertex.this.visualization.getZsp().getChildren().get(0),
                    this.rowRoot.visualWidth.divide(2.0), new ReadOnlyDoubleWrapper(0.0),
                    ZERO, this.rowRoot.heightProperty().multiply(-1.0))
        ));
        this.getChildren().add(rowRoot);

        this.ptRootBottom = new DirectedCurve.PointExpressionBase(this.rowRoot.external, VisualLogicalVertex.this.visualization.getZsp().getChildren().get(0),
                this.rowRoot.visualWidth.divide(2.0), this.rowRoot.heightProperty(),
                ZERO, this.rowRoot.heightProperty());

        this.makeDraggable(true);

        vertex.getChildAddresses().addListener(this::handle_childAddressesChanged);
        if(this.showChildren.get()) {
            for (final LogicalVertex address : vertex.getChildAddresses()) {
                addChildAddress(address);
            }
        }
    }

    private void handle_childAddressesChanged(final ListChangeListener.Change<? extends LogicalVertex> change) {
        while(change.next()) {
            for(final LogicalVertex address : change.getRemoved()) {
                removeChildAddress(address);
            }
            for(final LogicalVertex address : change.getAddedSubList()) {
                addChildAddress(address);
            }
        }
    }

    protected void addChildAddress(final LogicalVertex vertex) {
        if(this.showChildren.get()) {
            rowRoot.bindPoints.remove(this.ptRootBottom);
            this.buildChildRow(vertex);
        }
    }

    protected void buildChildRow(final LogicalVertex vertex) {
        final ContentRow rowNew = new ContentRow(vertex.getLogicalAddress());
        rowNew.textProperty().set(vertex.getLogicalAddress().toString());
        rowNew.visibleProperty().bind(VisualLogicalVertex.this.showChildren);
        //When hidden, reduce size to zero.
        rowNew.prefHeightProperty().bind(new When(rowNew.visibleProperty()).then(-1).otherwise(0));
        rowNew.prefWidthProperty().bind(rowNew.prefHeightProperty());
        rowNew.bindPoints.addAll(Arrays.asList(
                new DirectedCurve.PointExpressionBase(rowNew.external, VisualLogicalVertex.this.visualization.getZsp().getChildren().get(0),
                        new ReadOnlyDoubleWrapper(0.0), rowNew.heightProperty().divide(2.0),
                        rowNew.visualWidth.multiply(-1.0), ZERO),
                new DirectedCurve.PointExpressionBase(rowNew.external, VisualLogicalVertex.this.visualization.getZsp().getChildren().get(0),
                        rowNew.visualWidth, rowNew.heightProperty().divide(2.0),
                        rowNew.visualWidth, ZERO)
        ));

        this.getChildren().add(rowNew);
        this.rowsByChild.put(vertex.getLogicalAddressMapping(), rowNew);
    }

    protected void removeChildAddress(final LogicalVertex vertex) {
        final ContentRow rowToRemove = this.rowsByChild.get(vertex);
        if(rowToRemove != null) {
            this.rowsByChild.remove(vertex);
            this.getChildren().remove(rowToRemove);

            if(miShowConnectionDetails.isSelected() && this.rowsByChild.isEmpty()) {
                rowRoot.bindPoints.add(this.ptRootBottom);
            }
        }
    }

    public ContentRow rowFor(final LogicalAddressMapping address) {
        ContentRow existing = rowsByChild.get(address);
        if(existing != null) {
            //It may be in a pending state, so wait and retry.
            getVertex().waitForState();
            existing = rowsByChild.get(address);
            if(existing != null) {
                return existing;
            }
        }
        return rowRoot;
    }

    //IAltClickable
    @Override
    public List<Object> getRespondingNodes(final Point2D point) {
        return Arrays.asList(this);
    }

    public GraphLogicalVertex getVertex() {
        return this.vertex;
    }

    public BooleanExpression showChildrenProperty() {
        return this.showChildren;
    }

    @Override
    public List<MenuItem> getContextMenuItems() {
        final ArrayList<MenuItem> result = new ArrayList<>();
        final List<MenuItem> itemsRoot = vertex.getContextMenuItems();
        if(itemsRoot != null) {
            result.addAll(itemsRoot);
        }
        result.addAll(this.visualization.getGraph().menuItemsFor(this.vertex));
        result.add(miShowConnectionDetails);
        return result;
    }

    @Override
    public void writeToXml(final XMLStreamWriter writer) throws XMLStreamException {
        writer.writeAttribute("x", Double.toString(this.getTranslateX()));
        writer.writeAttribute("y", Double.toString(this.getTranslateY()));

        writer.writeAttribute("key", this.vertex.getKey());

        writer.writeAttribute("showChildren", Boolean.toString(this.showChildren.get()));
    }

    @Override
    public void readFromXml(final XMLStreamReader reader) throws XMLStreamException {
        final boolean showChildren = Boolean.parseBoolean(reader.getAttributeValue(null, "showChildren"));
        final double x = Double.parseDouble(reader.getAttributeValue(null, "x"));
        final double y = Double.parseDouble(reader.getAttributeValue(null, "y"));

        Platform.runLater(() -> {
            VisualLogicalVertex.this.setTranslateX(x);
            VisualLogicalVertex.this.setTranslateY(y);
            VisualLogicalVertex.this.miShowConnectionDetails.selectedProperty().set(showChildren);
        });
    }

    @Override
    public void handleMouseDragged(final MouseEvent event) {
        visualization.markAsModified();
        IDraggable.super.handleMouseDragged(event);
    }
}