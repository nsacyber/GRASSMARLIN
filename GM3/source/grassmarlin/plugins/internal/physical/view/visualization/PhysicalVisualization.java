package grassmarlin.plugins.internal.physical.view.visualization;

import grassmarlin.Version;
import grassmarlin.common.svg.Svg;
import grassmarlin.plugins.internal.logicalview.ICanHasContextMenu;
import grassmarlin.plugins.internal.physical.view.Plugin;
import grassmarlin.plugins.internal.physical.view.data.PhysicalDevice;
import grassmarlin.plugins.internal.physical.view.data.PhysicalEndpoint;
import grassmarlin.plugins.internal.physical.view.data.PhysicalGraph;
import grassmarlin.plugins.internal.physical.view.data.PhysicalWire;
import grassmarlin.ui.common.MutablePoint;
import grassmarlin.ui.common.ZoomableScrollPane;
import grassmarlin.ui.common.menu.ActiveMenuItem;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.ListChangeListener;
import javafx.geometry.Point2D;
import javafx.scene.Parent;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.SeparatorMenuItem;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;

import java.io.File;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

/**
 * When a class derived from PhysicalVisualization implements IPhysicalGraphLayout, the layout is initializaed to itself.
 */
public class PhysicalVisualization extends StackPane implements IPhysicalVisualization {
    public static final String LAYER_CLOUDS = "Clouds";
    public static final String LAYER_DEVICES = "Devices";
    public static final String LAYER_WIRES = "Wires";
    public static final String LAYER_ENDPOINTS = "Endpoints";

    private final Plugin plugin;
    private final PhysicalGraph graph;
    private final ZoomableScrollPane zsp;

    private final List<MenuItem> visualizationMenuItems;

    private final Map<PhysicalEndpoint, VisualEndpoint> lookupEndpoints;
    private final Map<PhysicalWire, VisualWire> lookupWires;
    private final Map<PhysicalDevice, VisualDevice> lookupDevices;

    private final MenuItem miExportSvg;
    private final CheckMenuItem ckRunLayout;

    public PhysicalVisualization(final Plugin plugin, final PhysicalGraph graph) {
        this.plugin = plugin;
        this.graph = graph;
        this.zsp = new ZoomableScrollPane(this::handleConstructContextMenu, this::hideContextMenu, LAYER_DEVICES, LAYER_ENDPOINTS, LAYER_WIRES, LAYER_CLOUDS);

        this.lookupEndpoints = new HashMap<>();
        this.lookupWires = new HashMap<>();
        this.lookupDevices = new HashMap<>();

        if(this instanceof IPhysicalGraphLayout) {
            this.layoutProperty = new SimpleObjectProperty<>((IPhysicalGraphLayout)this);
        } else {
            this.layoutProperty = new SimpleObjectProperty<>(null);
        }

        //Build context menu items
        this.visualizationMenuItems = new ArrayList<>();
        this.miExportSvg = new ActiveMenuItem("Export to SVG...", event -> {
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
        this.ckRunLayout = new CheckMenuItem("Run Layout");
        this.ckRunLayout.setSelected(true);
        this.ckRunLayout.setOnAction(event -> {
            PhysicalVisualization.this.requestLayout();
        });
        this.visualizationMenuItems.addAll(Arrays.asList(this.miExportSvg, this.ckRunLayout));
        final MenuItem miDumpVisualContentsToConsole = new ActiveMenuItem("Dump visualization contents to console", event -> {
            final StringBuilder result = new StringBuilder();
            result.append(this.toString()).append("\n");
            result.append("Endpoints:\n");
            for(final VisualEndpoint visualEndpoint : this.lookupEndpoints.values()) {
                result.append(visualEndpoint.toString()).append("\n");
            }
            result.append("Devices:\n");
            for(final VisualDevice visualDevice : this.lookupDevices.values()) {
                result.append(visualDevice.toString()).append("\n");
            }
            result.append("Wires:\n");
            for(final VisualWire visualWire : this.lookupWires.values()) {
                result.append(visualWire.toString()).append("\n");
            }
            System.out.println(result.toString());
        });
        this.visualizationMenuItems.add(miDumpVisualContentsToConsole);

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

        //TODO: Process initial state of the Physical Graph.
        // This is only necessary if there is a means to create a Physical Visualization after a data import
        // has happened--watch and filter views, for example.  Until these features exist on the Physical
        // Graph, we won't notice this omission, but when we do, it will have taken weeks to discover the
        // cause.  At that point we'll send a developer back in time to leave this comment.  If time and
        // budget permit, the developer will also be tasked with stopping the machine uprising.
        //NOTE:  There was enough time, but not budget.  In this timeline the IPO overvalued the machine's
        //       stock by a shocking amount, but people (or scripts) bought it anyway.  -KR

        this.graph.getEndpoints().addListener(this::handleEndpointsChanged);
        this.graph.getWires().addListener(this::handleWiresChanged);
        this.graph.getDevices().addListener(this::handleDevicesChanged);
    }

    public Plugin getPlugin() {
        return this.plugin;
    }

    public VisualEndpoint visualEndpointFor(final PhysicalEndpoint endpoint) {
        return this.lookupEndpoints.get(endpoint);
    }
    public VisualDevice visualDeviceFor(final PhysicalDevice device) {
        return this.lookupDevices.get(device);
    }

    // == Component Management
    //  Thread sync isn't important--this is all done in the UI thread, so multithreading isn't a concern
    //  When a port is moved into a device edges stay attached, so preserving the endpoint object makes sense...
    //  An endpoint is freefloating whereas a device port is an endpoint that is positioned by the owning device.

    private void handleEndpointsChanged(final ListChangeListener.Change<? extends PhysicalEndpoint> change) {
        change.reset();
        synchronized(this.lookupEndpoints) {
            while (change.next()) {
                for (final PhysicalEndpoint endpoint : change.getRemoved()) {
                    final VisualEndpoint visual = this.lookupEndpoints.remove(endpoint);
                    if (visual != null) {
                        visual.detachHandlers();
                        if (endpoint.getVertex() == null) {
                            this.zsp.removeChild(visual, LAYER_CLOUDS);
                        } else {
                            this.zsp.removeChild(visual, LAYER_ENDPOINTS);
                        }
                    }
                }
                for (final PhysicalEndpoint endpoint : change.getAddedSubList()) {
                    final VisualEndpoint visual = this.lookupEndpoints.computeIfAbsent(endpoint, k -> new VisualEndpoint(PhysicalVisualization.this, endpoint));
                    //HACK: There should be a better way of determining that this is a cloud endpoint.
                    if (endpoint.getVertex() == null) {
                        visual.scaleProperty().set(128.0);
                        this.zsp.addChild(visual, LAYER_CLOUDS);
                    } else {
                        //If added as a duplicate, this should just change the Z-ordering.
                        this.zsp.addChild(visual, LAYER_ENDPOINTS);
                    }
                }
            }
        }
    }

    private void handleWiresChanged(final ListChangeListener.Change<? extends PhysicalWire> change) {
        change.reset();
        synchronized(this.lookupWires) {
            while (change.next()) {
                for (final PhysicalWire wire : change.getRemoved()) {
                    final VisualWire visual = this.lookupWires.remove(wire);
                    if (visual != null) {
                        this.zsp.removeChild(visual, LAYER_WIRES);
                    }
                }
                for (final PhysicalWire wire : change.getAddedSubList()) {
                    final VisualWire visual = this.lookupWires.computeIfAbsent(wire, w -> new VisualWire(PhysicalVisualization.this.lookupEndpoints.get(w.getSource()), PhysicalVisualization.this.lookupEndpoints.get(w.getDestination())));
                    this.zsp.addChild(visual, LAYER_WIRES);
                }
            }
        }
    }

    private void handleDevicesChanged(final ListChangeListener.Change<? extends PhysicalDevice> change) {
        change.reset();
        synchronized(this.lookupDevices) {
            while (change.next()) {
                for (final PhysicalDevice device : change.getRemoved()) {
                    final VisualDevice visual = this.lookupDevices.get(device);
                    if (visual != null) {
                        visual.clear();
                        this.zsp.removeLayeredChild(visual);
                    }
                }
                for (final PhysicalDevice device : change.getAddedSubList()) {
                    final VisualDevice visual = this.lookupDevices.computeIfAbsent(device, d -> new VisualDevice(PhysicalVisualization.this, d));
                    this.zsp.addLayeredChild(visual);
                }
            }
        }
    }


    // == Context Menu stuff
    private final ContextMenu menu = new ContextMenu();

    private void handleConstructContextMenu(final List<Object> objects, final Point2D screenLocation) {
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

    // == Layout

    protected final BooleanProperty autoLayout = new SimpleBooleanProperty(true);
    protected final ObjectProperty<IPhysicalGraphLayout> layoutProperty;
    protected final AtomicBoolean isLayoutRunning = new AtomicBoolean(false);

    @Override
    protected void layoutChildren() {
        super.layoutChildren();

        if(this.autoLayout.get()) {
            final IPhysicalGraphLayout layout = this.layoutProperty.get();
            if(layout != null && this.ckRunLayout.isSelected() && !isLayoutRunning.getAndSet(true)) {
                //Run the layout in a worker thread then call back to the UI thread
                final Thread threadLayout = new Thread(() -> {
                    try {
                        final Map<IPhysicalElement, MutablePoint> locations = layout.executeLayout(PhysicalVisualization.this);
                        if (locations != null && !locations.isEmpty()) {
                            Platform.runLater(() -> {
                                try {
                                    for (Map.Entry<IPhysicalElement, MutablePoint> entry : locations.entrySet()) {
                                        //TODO: Test to ensure the element is part of this visualization
                                        if (entry.getKey().isSubjectToLayoutProperty().get()) {
                                            entry.getKey().translateXProperty().set(Math.floor(entry.getValue().getX()));
                                            entry.getKey().translateYProperty().set(Math.floor(entry.getValue().getY()));
                                        }
                                    }
                                } finally {
                                    isLayoutRunning.set(false);
                                }
                                if (layout.requiresForcedUpdate()) {
                                    PhysicalVisualization.this.requestLayout();
                                }
                            });
                        } else {
                            isLayoutRunning.set(false);
                        }
                    } catch(Exception ex) {
                        ex.printStackTrace();
                    }
                });
                threadLayout.setDaemon(true);
                threadLayout.start();
            }
        }
    }

    public PhysicalGraph getGraph() {
        return this.graph;
    }

    @Override
    public Collection<IPhysicalElement> getHosts() {
        synchronized(this.lookupEndpoints) {
            return this.lookupEndpoints.values().stream().filter(endpoint -> endpoint.getEndpoint() != null && !endpoint.getEndpoint().isPort()).collect(Collectors.toSet());
        }
    }
    @Override
    public Collection<IPhysicalElement> getPorts() {
        synchronized(this.lookupEndpoints) {
            return this.lookupEndpoints.values().stream().filter(endpoint -> endpoint.getEndpoint() != null && endpoint.getEndpoint().isPort()).collect(Collectors.toSet());
        }
    }
    @Override
    public Collection<IPhysicalElement> getDevices() {
        synchronized(this.lookupDevices) {
            return new HashSet<>(this.lookupDevices.values());
        }
    }
    @Override
    public Collection<VisualWire> getWires() {
        synchronized(this.lookupWires) {
            return new ArrayList<>(this.lookupWires.values());
        }
    }
    @Override
    public Collection<IPhysicalElement> portsOf(final IPhysicalElement device) {
        synchronized(this.lookupEndpoints) {
            if (device instanceof PhysicalDevice) {
                return ((PhysicalDevice) device).getPorts().stream().map(port -> PhysicalVisualization.this.lookupEndpoints.get(port)).filter(visual -> visual != null).collect(Collectors.toList());
            } else {
                return Collections.EMPTY_LIST;
            }
        }
    }
}
