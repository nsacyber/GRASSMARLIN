package grassmarlin.plugins.internal.logicalview.swing;

import grassmarlin.Event;
import grassmarlin.plugins.internal.logicalview.*;
import grassmarlin.plugins.internal.logicalview.visual.layouts.ForceDirected;
import grassmarlin.session.LogicalVertex;
import grassmarlin.ui.common.MutablePoint;
import javafx.embed.swing.SwingNode;
import javafx.scene.control.MenuItem;

import javax.swing.*;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.lang.reflect.InvocationTargetException;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiFunction;
import java.util.function.Supplier;

public class LogicalVisualization extends SwingNode implements ILogicalVisualization {
    private final static String LAYER_NODES = "NODES";
    private final static String LAYER_EDGES = "EDGES";

    private final ZoomableScrollPane zsp;
    private final ViewLogical view;

    private final Thread threadLayout;
    private final AtomicBoolean terminateLayout;
    private final ForceDirected layout;

    private final Map<GraphLogicalVertex, VisualLogicalVertex> lookupVertices;
    private final Map<GraphLogicalEdge, VisualLogicalEdge> lookupEdges;

    public LogicalVisualization(final ViewLogical view) {
        this.zsp = new ZoomableScrollPane(LAYER_EDGES, LAYER_NODES);
        this.view = view;

        this.layout = new ForceDirected();

        this.lookupVertices = new HashMap<>();
        this.lookupEdges = new HashMap<>();

        this.view.getGraph().onLogicalGraphVertexCreated.addHandler(this.handlerVertexCreated);
        this.view.getGraph().onLogicalGraphVertexRemoved.addHandler(this.handlerVertexRemoved);
        this.view.getGraph().onLogicalGraphEdgeCreated.addHandler(this.handlerEdgeCreated);
        this.view.getGraph().onLogicalGraphEdgeRemoved.addHandler(this.handlerEdgeRemoved);

        super.setContent(this.zsp);

        this.terminateLayout = new AtomicBoolean(false);
        this.threadLayout = new Thread(this::threadRunLayout);
        threadLayout.setDaemon(true);
        threadLayout.start();
    }

    @Override
    public void cleanup() {
        this.terminateLayout.set(true);
    }

    // == Layout

    private void threadRunLayout() {
        while(!terminateLayout.get()) {
            final long msStart = System.currentTimeMillis();

            final Map<IVisualLogicalVertex, MutablePoint> positionsNew = this.layout.executeLayout(this);

            try {
                SwingUtilities.invokeAndWait(() -> {
                    for (Map.Entry<IVisualLogicalVertex, MutablePoint> entry : positionsNew.entrySet()) {
                        if (entry.getKey().isSubjectToLayout()) {
                            entry.getKey().setTranslateX(entry.getValue().getX());
                            entry.getKey().setTranslateY(entry.getValue().getY());
                        }
                    }
                    LogicalVisualization.this.zsp.repaint();
                });
            } catch(InterruptedException ex) {
                return;
            } catch(InvocationTargetException ex) {
                ex.printStackTrace();
            }

            final long msDuration = System.currentTimeMillis() - msStart;
            try {
                final long delay = (1000 / 60) - msDuration;
                if(delay > 0) {
                    Thread.sleep(delay);
                }
            } catch(InterruptedException ex) {
                return;
            }
        }
    }

    // == Vertices

    private final Event.EventListener<GraphLogicalVertex> handlerVertexCreated = this::handleVertexCreated;
    protected void handleVertexCreated(final Event<GraphLogicalVertex> event, final GraphLogicalVertex vertex) {
        final VisualLogicalVertex visual = createVisualLogicalVertexFor(vertex);

        this.lookupVertices.put(vertex, visual);
        this.zsp.addChild(visual, LAYER_NODES);
    }

    private final Event.EventListener<GraphLogicalVertex> handlerVertexRemoved = this::handleVertexRemoved;
    protected void handleVertexRemoved(final Event<GraphLogicalVertex> event, final GraphLogicalVertex vertex) {
        final VisualLogicalVertex visual = this.lookupVertices.remove(vertex);
        if(visual != null) {
            this.zsp.removeChild(visual, LAYER_NODES);
        }
    }

    protected VisualLogicalVertex createVisualLogicalVertexFor(final GraphLogicalVertex vertex) {
        return new VisualLogicalVertex(vertex);
    }

    @Override
    public VisualLogicalVertex vertexFor(final GraphLogicalVertex vertex) {
        synchronized(this.lookupVertices) {
            return this.lookupVertices.get(vertex);
        }
    }

    @Override
    public Collection<? extends IVisualLogicalVertex> getVertices() {
        synchronized(this.lookupVertices) {
            return new ArrayList<>(this.lookupVertices.values());
        }
    }

    // == Edges
    private final Event.EventListener<GraphLogicalEdge> handlerEdgeCreated = this::handleEdgeCreated;
    protected void handleEdgeCreated(final Event<GraphLogicalEdge> event, final GraphLogicalEdge edge) {
        final VisualLogicalVertex source = lookupVertices.get(edge.getSource());
        final VisualLogicalVertex destination = lookupVertices.get(edge.getDestination());

        final VisualLogicalEdge visual = new VisualLogicalEdge(edge, source, destination);
        this.lookupEdges.put(edge, visual);
        this.zsp.addChild(visual, LAYER_EDGES);
    }

    private final Event.EventListener<GraphLogicalEdge> handlerEdgeRemoved = this::handleEdgeRemoved;
    protected void handleEdgeRemoved(final Event<GraphLogicalEdge> event, final GraphLogicalEdge edge) {
        final VisualLogicalEdge visual = this.lookupEdges.remove(edge);
        if(visual != null) {
            this.zsp.removeChild(visual, LAYER_EDGES);
        }
    }


    // == Other

    public LogicalGraph getGraph() {
        return this.view.getGraph();
    }

    @Override
    public void setCurrentGrouping(String newValue) {
        //TODO: Implement grassmarlin.plugins.internal.logicalview.swing.LogicalVisualization.setCurrentGrouping
    }
    @Override
    public String getCurrentGrouping() {
        //TODO: Implement grassmarlin.plugins.internal.logicalview.swing.LogicalVisualization.getCurrentGrouping
        return null;
    }

    @Override
    public void setContextMenuSources(Supplier<List<MenuItem>> common, BiFunction<GraphLogicalVertex, LogicalVertex, List<MenuItem>> vertex) {
        //TODO: Implement grassmarlin.plugins.internal.logicalview.swing.LogicalVisualization.setContextMenuSources
    }

    @Override
    public void zoomToVertex(GraphLogicalVertex vertex) {
        //TODO: Implement grassmarlin.plugins.internal.logicalview.swing.LogicalVisualization.zoomToVertex
        throw new UnsupportedOperationException("Not Implemented Yet");
    }

    @Override
    public void writeToXml(XMLStreamWriter target) throws XMLStreamException {
        //TODO: Implement grassmarlin.plugins.internal.logicalview.swing.LogicalVisualization.writeToXml
        throw new UnsupportedOperationException("Not Implemented Yet");
    }
}
