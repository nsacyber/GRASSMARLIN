package grassmarlin.plugins.internal.logicalview.visual;

import com.sun.javafx.collections.ObservableListWrapper;
import grassmarlin.plugins.internal.logicalview.GraphLogicalVertex;
import grassmarlin.plugins.internal.logicalview.ICanHasContextMenu;
import grassmarlin.plugins.internal.logicalview.IVisualLogicalVertex;
import grassmarlin.session.LogicalAddress;
import grassmarlin.session.LogicalVertex;
import grassmarlin.session.pipeline.LogicalAddressMapping;
import grassmarlin.ui.common.BackgroundFromColor;
import grassmarlin.ui.common.IAltClickable;
import grassmarlin.ui.common.IDraggable;
import grassmarlin.ui.common.VisualImageList;
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
import javafx.scene.image.Image;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Text;

import java.util.*;

// A VisualLogicalVertex wraps around a GraphLogicalVertex and displays its members.
//  The display is made using a series of rows of content
//  Main Row
//  [ TRANSPARENT ] [ OPAQUE BACKGROUND ]
//  [ExternalIcons]|[Icons] | [Logical Address]
//  Subsequent rows are present for child logical addresses.  The external icons are always blank on these rows (?)
//
public class VisualLogicalVertex extends VBox implements IDraggable, IAltClickable, ICanHasContextMenu, IVisualLogicalVertex {
    private static final CornerRadii CORNER_RADII = new CornerRadii(4.0);
    private static final Insets INSETS = new Insets(4.0, 4.0, 4.0, 4.0);
    private static final ReadOnlyDoubleWrapper ZERO = new ReadOnlyDoubleWrapper(0.0);

    public class ContentRow extends HBox {
        private final LogicalVertex vertex;
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

        protected ContentRow(final LogicalVertex vertex) {
            this.vertex = vertex;
            this.contentExternal = new ObservableListWrapper<>(new LinkedList<>());
            this.contentExternal.addListener(this::handle_UpdateExternalContent);
            this.contentInternal = new ObservableListWrapper<>(new LinkedList<>());
            this.contentInternal.addListener(this::handle_UpdateInternalContent);
            this.bindPoints = new HashSet<>();
            this.label = new Text();
            this.background = new SimpleObjectProperty<>();

            this.internal.setOpaqueInsets(INSETS);
            this.internal.getChildren().add(this.label);

            this.backgroundFill = VertexColorAssignment.backgroundColorFor(vertex.getLogicalAddress().getClass());
            this.background.bind(new BackgroundFromColor(backgroundFill, CORNER_RADII, null));

            final Pane spacer = new Pane();
            HBox.setHgrow(spacer, Priority.ALWAYS);

            this.label.fillProperty().bind(VertexColorAssignment.textColorFor(vertex.getLogicalAddress().getClass()));

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

        public LogicalVertex getVertex() {
            return this.vertex;
        }
    }

    private final GraphLogicalVertex vertex;
    private final Map<LogicalAddressMapping, ContentRow> rowsByChild;
    private final BooleanProperty showChildren;
    private final ContentRow rowRoot;
    private final DirectedCurve.PointExpression ptRootBottom;
    private final VisualImageList visualImages;
    private final LogicalVisualization visualization;

    private final IDraggable.DragContext dragContext = new DragContext();

    // Context Menu Items
    private final CheckMenuItem miShowConnectionDetails;
    private final CheckMenuItem ckApplyLayout;

    public VisualLogicalVertex(final LogicalVisualization visualization, final GraphLogicalVertex vertex, final ObservableList<Image> images) {
        this.vertex = vertex;
        this.rowsByChild = new HashMap<>();
        this.visualization = visualization;
        this.rowRoot = new ContentRow(vertex.getVertex());
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
        this.ckApplyLayout = new CheckMenuItem("Apply Layout");
        this.ckApplyLayout.setSelected(true);
        this.ckApplyLayout.selectedProperty().addListener(observable -> {
            VisualLogicalVertex.this.requestLayout();
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
        this.setOnMouseReleased(event -> {
            if(event.isShiftDown()) {
                this.ckApplyLayout.setSelected(true);
                event.consume();
            }
        });

        vertex.getChildAddresses().addListener(this::handle_childAddressesChanged);
        if(this.showChildren.get()) {
            for (final LogicalVertex address : vertex.getChildAddresses()) {
                addChildAddress(address);
            }
        }

        this.layoutXProperty().bind(this.widthProperty().divide(-2.0));
        this.layoutYProperty().bind(this.heightProperty().divide(-2.0));
    }

    @Override
    public DragContext getDragContext() {
        return this.dragContext;
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

    public boolean isSubjectToLayout() {
        return this.ckApplyLayout.isSelected();
    }
    public void setSubjectToLayout(final boolean value) {
        this.ckApplyLayout.setSelected(value);
    }

    protected void buildChildRow(final LogicalVertex vertex) {
        final ContentRow rowNew = new ContentRow(vertex);
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

        //We want to sort the children, but have to do that outside the getChildren collection--otherwise there will be errors with adding Nodes which already exist.
        this.getChildren().add(rowNew);
        final List<Node> children = new ArrayList<>(this.getChildren());
        children.remove(0); //First one is the root row.  It shouldn't matter if it is sorted, but take no chances.
        children.sort((row1, row2) -> {
            if(row1 instanceof ContentRow && row2 instanceof ContentRow) {
                final LogicalAddress addr1 = ((ContentRow)row1).getVertex().getLogicalAddress();
                final LogicalAddress addr2 = ((ContentRow)row2).getVertex().getLogicalAddress();

                if(addr1.contains(addr2)) {
                    return -1;
                } else if(addr2.contains(addr1)) {
                    return 1;
                } else {
                    return addr1.compareTo(addr2);
                }
            } else {
                final int hash1 = row1.hashCode();
                final int hash2 = row2.hashCode();

                return hash1 == hash2 ? 0 : hash2 - hash1;
            }
        });
        this.getChildren().removeAll(children);
        this.getChildren().addAll(children);
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
        return Arrays.asList(this, this.getVertex());
    }

    @Override
    public GraphLogicalVertex getVertex() {
        return this.vertex;
    }

    public BooleanProperty showChildrenProperty() {
        return this.showChildren;
    }

    @Override
    public List<MenuItem> getContextMenuItems() {
        final ArrayList<MenuItem> result = new ArrayList<>();
        result.add(this.miShowConnectionDetails);
        result.add(this.ckApplyLayout);
        return result;
    }

    @Override
    public void dragTo(final Point2D ptDestination) {
        this.ckApplyLayout.setSelected(false);
        visualization.markAsModified();

        IDraggable.super.dragTo(ptDestination);
    }
}