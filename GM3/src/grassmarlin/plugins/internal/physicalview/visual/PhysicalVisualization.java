package grassmarlin.plugins.internal.physicalview.visual;

import grassmarlin.Event;
import grassmarlin.Logger;
import grassmarlin.Version;
import grassmarlin.common.svg.Svg;
import grassmarlin.plugins.internal.logicalview.ICanHasContextMenu;
import grassmarlin.plugins.internal.physicalview.PhysicalGraph;
import grassmarlin.plugins.internal.physicalview.SessionConnectedPhysicalGraph;
import grassmarlin.plugins.internal.physicalview.graph.Segment;
import grassmarlin.plugins.internal.physicalview.graph.Switch;
import grassmarlin.ui.common.ImageDirectoryWatcher;
import grassmarlin.ui.common.ZoomableScrollPane;
import grassmarlin.ui.common.menu.ActiveMenuItem;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.geometry.Point2D;
import javafx.geometry.Point3D;
import javafx.scene.Parent;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.image.Image;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;

import java.io.File;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

public class PhysicalVisualization extends StackPane {
    public static final String LAYER_DEVICES = "Devices";
    public static final String LAYER_WIRES = "Wires";
    public static final String LAYER_ENDPOINTS = "Endpoints";

    private final PhysicalGraph graph;
    private final BooleanProperty editable;
    private final ImageDirectoryWatcher<Image> imageMapper;

    private final ZoomableScrollPane zsp;

    protected final ArrayList<MenuItem> visualizationMenuItems;

    public PhysicalVisualization(final PhysicalGraph graph, final ImageDirectoryWatcher<Image> imageMapper) {
        this.graph = graph;
        this.editable = new SimpleBooleanProperty(!(graph instanceof SessionConnectedPhysicalGraph));
        this.imageMapper = imageMapper;

        this.zsp = new ZoomableScrollPane(this::handle_ConstructContextMenu, this::hideContextMenu, LAYER_DEVICES, LAYER_WIRES, LAYER_ENDPOINTS);

        initComponents();

        this.graph.onSegmentsModified.addHandler(this.handlerSegmentsModified);

        //Build context menu items
        this.visualizationMenuItems = new ArrayList<>();
        //this.miLayoutAll = new LogicalViewLayoutMenuItem("Layout All", plugin, this, vertex -> true);
        final MenuItem miExportSvg = new ActiveMenuItem("Export to SVG...", event -> {
            final FileChooser dlgExportAs = new FileChooser();
            dlgExportAs.setTitle("Export To...");
            dlgExportAs.getExtensionFilters().addAll(
                    new FileChooser.ExtensionFilter("SVG Files", "*.svg"),
                    new FileChooser.ExtensionFilter("All Files", "*")
            );
            final File selected = dlgExportAs.showSaveDialog(PhysicalVisualization.this.getScene().getWindow());
            if(selected != null) {
                //HACK: Skip the ZSP itself, since that is the camera; the first (only) child is the scene graph at world coordinate scale.
                Svg.serialize((Parent) PhysicalVisualization.this.zsp.getChildrenUnmodifiable().get(0), Paths.get(selected.getAbsolutePath()));
            }
        });
        final MenuItem miLayout = new ActiveMenuItem("Layout", event -> {
            PhysicalVisualization.this.performLayout();
        });
        this.visualizationMenuItems.addAll(Arrays.asList(miExportSvg, miLayout));
    }

    private final Event.EventListener<PhysicalGraph.SegmentEventArgs> handlerSegmentsModified = this::handleSegmentsModified;

    private void initComponents() {
        this.zsp.prefWidthProperty().bind(this.widthProperty());
        this.zsp.prefHeightProperty().bind(this.heightProperty());

        if(Version.IS_BETA) {
            final Text textBetaDisclaimer = new Text("\u03B2");
            textBetaDisclaimer.setFont(new Font("Lucida Console", 96.0));
            textBetaDisclaimer.setStroke(Color.AQUAMARINE);
            textBetaDisclaimer.setOpacity(0.6);

            this.getChildren().add(textBetaDisclaimer);
        }

        this.getChildren().add(this.zsp);
    }

    private final Map<Segment, VisualSegment> lookupSegments = new HashMap<>();

    protected void handleSegmentsModified(final Event<PhysicalGraph.SegmentEventArgs> event, PhysicalGraph.SegmentEventArgs args) {
        if(args.isValid()) {
            final VisualSegment segment = new VisualSegment(args.getSegment(), imageMapper);
            lookupSegments.put(args.getSegment(), segment);
            this.zsp.addLayeredChild(segment);
        } else {
            final VisualSegment segment = lookupSegments.remove(args.getSegment());
            if(segment != null) {
                this.zsp.removeLayeredChild(segment);
                segment.detachHandlers();
            } else {
                Logger.log(Logger.Severity.PEDANTIC_DEVELOPER_SPAM, "Removing VisualSegment that doesn't exist.");
            }
        }
    }

    // == Context Menu stuff
    private final ContextMenu menu = new ContextMenu();

    private void handle_ConstructContextMenu(final List<Object> objects, final Point2D screenLocation) {
        menu.hide();
        menu.getItems().clear();
        menu.getItems().addAll(this.visualizationMenuItems);

        for(final Object object : objects) {
            if(object instanceof ICanHasContextMenu) {
                final List<MenuItem> items = ((ICanHasContextMenu)object).getContextMenuItems();
                if(items != null && !items.isEmpty()) {
                    if(!menu.getItems().isEmpty()) {
                        menu.getItems().add(new SeparatorMenuItem());
                    }
                    menu.getItems().addAll(items);
                }
            }
        }
        if(!menu.getItems().isEmpty()) {
            menu.show(this, screenLocation.getX(), screenLocation.getY());
        }
    }

    private void hideContextMenu(final Point2D point) {
        menu.hide();
    }
    private final static Point2D vecXAxis = new Point2D(1.0, 0.0);

    protected void performLayout() {
        //TODO: Routers get displayed in a column on the left, segments will be displayed to the right of them (with space for padding).  Once router identification is supported, we will need to lay out the router column and remember the width
        double widthRouters = 0.0;

        //TODO: Segments should be ordered based on which routers they talk to and the display order of the routers.
        double topCurrentSegment = 0.0;
        for(final Segment segment : this.graph.getSegments()) {
            final VisualSegment visualSegment = lookupSegments.get(segment);

            //We're going to build a map of port-offset pairs and find the total height we need before we start positioning elements.
            final Map<Switch.Port, Point2D> locationForPortConnections = new HashMap<>();
            double heightCurrentSegment = 0.0;
            final double heightPerPosition = 32.0;  //HACK: arbitrary constant that works fine in testing.

            for(final VisualSwitch visualSwitch : visualSegment.getVisualSwitches()) {
                final Switch sWitch = visualSwitch.getSwitch();
                final double widthPerPosition = visualSwitch.getWidth() * -1.5 / (double)sWitch.getPortGroups().stream().flatMap(group -> group.getPorts().entrySet().stream()).count();

                int idxCurrent = 0;
                double highestEndpoint = 0.0;
                double lowestEndpoint = 0.0;
                final double idxMax = (double)sWitch.getPortGroups().stream().flatMap(group -> group.getPorts().entrySet().stream()).count() - 1.0;
                for(final Switch.Port port : sWitch.getPortGroups().stream()
                        .flatMap(group -> group.getPorts().keySet().stream())
                        .collect(Collectors.toList())) {
                    final IHasControlPoint source = visualSegment.endpointFor(port.getAddress());
                    final Point2D ptEnd = new Point2D(source.getTerminalX().doubleValue(), source.getTerminalY().doubleValue());
                    final Point2D ptEndControl = new Point2D(source.getControlX().doubleValue(), source.getControlY().doubleValue());

                    final Point2D vecControl = ptEndControl.subtract(ptEnd).normalize();
                    //cross product of vecControl (with 0 Z) and (0, 0, 1) should give a vector that points 90 degrees counter-clockwise of vecControl.
                    //We're going to use vecControl as the pseudo-X and this new vector as the pseudo-Y to determine the location and orientation of the endpoint relative to the port.
                    final Point3D vec3Control = new Point3D(vecControl.getX(), vecControl.getY(), 0.0);
                    final Point3D vec3Axis = new Point3D(0.0, 0.0, 1.0);
                    final Point3D vec3Offset = vec3Control.crossProduct(vec3Axis);
                    final Point2D vecOffset = new Point2D(vec3Offset.getX(), vec3Offset.getY()).normalize();

                    //We define the location relative to the top left of the switch, so as we position switches, we can position the connected elements easily.
                    final Point2D ptOffsetFromPort =
                            vecControl
                                    .multiply(heightPerPosition * (2.0 + idxMax / 2.0 - Math.abs((double)(idxCurrent) - idxMax / 2.0)))
                            .add(vecOffset
                                    .multiply(
                                            visualSwitch.getWidth()
                                            + visualSwitch.getWidth() * ((double)idxCurrent * -1.6 / idxMax) - 0.8));
                    final Point2D ptLocation = ptOffsetFromPort.add(ptEnd).subtract(visualSwitch.getLayoutX(), visualSwitch.getLayoutY());

                    locationForPortConnections.put(port, ptLocation);

                    lowestEndpoint = Math.min(lowestEndpoint, ptOffsetFromPort.getY());
                    highestEndpoint = Math.max(highestEndpoint, ptOffsetFromPort.getY());

                    //This isn't entirely accurate, but it is an easy computation and rounds to being slightly larger than necessary
                    heightCurrentSegment = Math.max(heightCurrentSegment, highestEndpoint - lowestEndpoint + visualSwitch.getHeight());
                    idxCurrent++;
                }
            }

            //We can now iterate over switches to position them within the identified vertical space.
            double horizontalOffsetCurrentSwitch = 0.0;
            final double horizontalSpacing = 200.0;
            for(final VisualSwitch visualSwitch : visualSegment.getVisualSwitches()) {
                //Center switch vertically and use a horizontal offset of half the width (we're going to use twice the width as the spacer for the next
                //Every time I write a centering function, it gets more and more difficult to not make a crack about NodeJS, but I made that joke once before, so I should just reference it here, rather than rewrite it.
                visualSwitch.setLayoutX(horizontalOffsetCurrentSwitch + visualSwitch.getWidth() * 0.5);
                visualSwitch.setLayoutY(topCurrentSegment + (heightCurrentSegment - visualSwitch.getHeight()) / 2.0);

                //Connect endpoints.  We trace through visual entities rather than the back-end since the back-end has no concept of cloud nodes.
                //This also ensures that the visual components are laid out regardless of any issues that crop up in the non-visual layout algorithm.
                for(final Switch.Port port : visualSwitch.getSwitch().getPortGroups().stream().flatMap(group -> group.getPorts().keySet().stream()).collect(Collectors.toList())) {
                    final VisualSwitchPort visualPort = visualSwitch.getPortMapping().get(port);
                    final Collection<Wire> wires = visualSegment.wiresConnectedTo(port.getAddress());
                    if(wires.size() > 1) {
                        //This should not be possible; if this would be the case, we should have created a cloud node.
                        Logger.log(Logger.Severity.WARNING, "During layout multiple connections to a single port (%s) were observed.", port.getAddress());
                    }

                    for(final IHasControlPoint endpoint : wires.stream()
                            .map(wire -> wire.otherEnd(visualSegment.endpointFor(port.getAddress())))
                            .collect(Collectors.toList())) {
                        if(endpoint instanceof VisualEndpoint) {
                            final VisualEndpoint visual = (VisualEndpoint)endpoint;
                            final Point2D ptLocation = locationForPortConnections.get(port)
                                    .add(visualSwitch.getLayoutX(), visualSwitch.getLayoutY());
                            visual.setLayoutX(ptLocation.getX() - 0.5 * visual.getWidth());
                            visual.setLayoutY(ptLocation.getY() - 0.5 * visual.getHeight());
                        } else if(endpoint instanceof VisualCloud) {
                            final VisualCloud visual = (VisualCloud)endpoint;
                            final Point2D ptLocation = locationForPortConnections.get(port)
                                    .add(visualSwitch.getLayoutX(), visualSwitch.getLayoutY());
                            //The connection will be to the center of the cloud
                            visual.setLayoutX(ptLocation.getX() - 0.5 * visual.getWidth());
                            visual.setLayoutY(ptLocation.getY() - 0.5 * visual.getHeight());

                            // As a cloud, we will have to layout all connected VisualEndpoint elements.
                            //We're going to do some messed-up math with regards to the angles and Y-axis to deal with conversion between screen and cartesian coordinates.
                            final List<VisualEndpoint> endpoints = visualSegment.wiresConnectedTo(visual).stream()
                                    .map(wire -> wire.otherEnd(endpoint))
                                    .filter(end -> end instanceof VisualEndpoint)
                                    .map(end -> (VisualEndpoint)end)
                                    .collect(Collectors.toList());
                            final double degCenter = vecXAxis.angle(
                                    ptLocation
                                            .subtract(visualPort.getControlX().get(), visualPort.getControlY().get()));
                            final double degOffset = 90.0 / (double)(endpoints.size() + 1);

                            double degCurrent = degCenter + 45.0;
                            double radius = heightPerPosition;
                            for(final VisualEndpoint endpointConnectedToSwitch : endpoints) {
                                degCurrent -= degOffset;
                                radius += heightPerPosition;

                                //TODO: We set the center to be the target, which works as long as that is where the control/endpoint are (they are in this case because we haven't finished the detailed implementation, but when that changes, this will need an update too)
                                endpointConnectedToSwitch.setLayoutX(
                                        ptLocation.getX()
                                                + 1.5 * radius * Math.cos(Math.toRadians(degCurrent))
                                                - 0.5 * endpointConnectedToSwitch.getWidth());
                                endpointConnectedToSwitch.setLayoutY(
                                        ptLocation.getY()
                                                - 1.5 * radius * Math.sin(Math.toRadians(degCurrent))
                                                - 0.5 * endpointConnectedToSwitch.getHeight());
                            }
                        } else {
                            //Not a cloud, not an endpoint...  all that is left is a VisualSwitchport, which will be handled elsewhere.  Well, either that or something *really* wrong happened.
                        }
                    }
                }

                horizontalOffsetCurrentSwitch += 2.0 * visualSwitch.getWidth() + horizontalSpacing;
            }

            //TODO: Anything that belongs to this segment that wasn't connected to a switch needs to be positioned (And shouldn't be part of this segment?).

            topCurrentSegment += heightCurrentSegment;
        }
    }
}
