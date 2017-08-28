package grassmarlin.plugins.internal.logicalview.visual;

import grassmarlin.RuntimeConfiguration;
import grassmarlin.plugins.internal.logicalview.FilteredLogicalGraph;
import grassmarlin.plugins.internal.logicalview.GraphLogicalEdge;
import grassmarlin.plugins.internal.logicalview.GraphLogicalVertex;
import grassmarlin.plugins.internal.logicalview.Plugin;
import grassmarlin.ui.common.ImageDirectoryWatcher;
import grassmarlin.ui.common.menu.ActiveMenuItem;
import javafx.beans.Observable;
import javafx.beans.property.DoubleProperty;
import javafx.scene.control.CustomMenuItem;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.layout.HBox;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.stream.events.XMLEvent;
import java.util.*;
import java.util.stream.Collectors;

public class WatchLogicalVisualization extends LogicalVisualization implements Plugin.IHasTitle {
    private final GraphLogicalVertex root;
    private final DoubleProperty degreesProperty;
    private final HashMap<GraphLogicalVertex, Integer> distanceCache;

    private final FilteredLogicalGraph graphComplete;

    public WatchLogicalVisualization(final Plugin plugin, final RuntimeConfiguration config, final FilteredLogicalGraph graph, final FilteredLogicalGraph graphComplete, final GraphLogicalVertex root, final int degrees, final Plugin.LogicalGraphState state) {
        super(plugin, config, graph, state);

        this.graphComplete = graphComplete;

        this.root = root;

        this.distanceCache = new HashMap<>();
        this.distanceCache.put(this.root, 0);

        //TODO: This is horribly inefficient to listen to all of these; we're going to needlessly rebuild that graph so many times.
        graph.getVertices().addListener(this::handleGraphEdgesChanged);
        graph.getEdges().addListener(this::handleGraphEdgesChanged);
        graphComplete.getVertices().addListener(this::handleGraphEdgesChanged);
        graphComplete.getEdges().addListener(this::handleGraphEdgesChanged);

        final Slider sliderSelectDegrees = new Slider(1.0, 10.0, (double)degrees);
        sliderSelectDegrees.setSnapToTicks(true);
        sliderSelectDegrees.setMajorTickUnit(1.0);
        sliderSelectDegrees.setMinorTickCount(0);
        sliderSelectDegrees.setShowTickMarks(true);
        final Label lblSelectDegrees = new Label("Degrees:");
        lblSelectDegrees.setLabelFor(sliderSelectDegrees);
        final HBox nodeDegreesSliderMenuItem = new HBox();
        nodeDegreesSliderMenuItem.getChildren().addAll(lblSelectDegrees, sliderSelectDegrees);
        this.degreesProperty = sliderSelectDegrees.valueProperty();
        this.degreesProperty.addListener(observable -> {
            WatchLogicalVisualization.this.markAsModified();
            //When shrinking we don't have to rebuild the distance cache.
            if(WatchLogicalVisualization.this.degreesProperty.intValue() > WatchLogicalVisualization.this.distanceCache.size()) {
                WatchLogicalVisualization.this.rebuildDistanceCache();
            } else {
                WatchLogicalVisualization.this.getGraph().reapplyPredicate();
            }
        });

        //This also sets the predicate.  Must be called after this.degreesProperty is set.
        rebuildDistanceCache();

        visualizationMenuItems.addAll(Arrays.asList(
                new ActiveMenuItem("Close Watch Window", event -> {
                    state.close(WatchLogicalVisualization.this);
                }),
                new CustomMenuItem(nodeDegreesSliderMenuItem, true)
        ));
    }

    private void handleGraphEdgesChanged(Observable observable) {
        rebuildDistanceCache();
    }

    private void rebuildDistanceCache() {
        distanceCache.clear();

        final int maxDistance = degreesProperty.intValue();
        int distance = 0;
        final List<GraphLogicalVertex> pending = new ArrayList<>();
        pending.add(this.root);

        // Pull the full list of edges once and remove edges used to process each iteration; reducing overhead for successive generations.
        final LinkedList<GraphLogicalEdge> edges = new LinkedList<>(graphComplete.getEdges());
        while(!pending.isEmpty() && distance <= maxDistance) {
            for(final GraphLogicalVertex vertex : pending) {
                distanceCache.put(vertex, distance);
            }
            //If neither endpoint is in pending, ignore it.
            //If both are in pending, ignore it.
            //If one is in pending, add the other if it is not already in
            final List<GraphLogicalVertex> next = edges.stream()
                    .filter(edge -> pending.contains(edge.getSource()) != pending.contains(edge.getDestination()))
                    .map(edge -> pending.contains(edge.getSource()) ? edge.getDestination() : edge.getSource())
                    .filter(vertex -> !distanceCache.containsKey(vertex))
                    .collect(Collectors.toList());
            edges.removeIf(edge -> pending.contains(edge.getSource()) != pending.contains(edge.getDestination()));
            pending.clear();
            pending.addAll(next);
            distance++;
        }

        getGraph().setPredicate(this::applyFilters);
    }

    private boolean applyFilters(final GraphLogicalVertex vertex) {
        final int degrees = this.degreesProperty.intValue();
        final Integer distance = distanceCache.get(vertex);
        return (distance != null) && (distance <= degrees);
    }

    @Override
    public void readFromXml(final XMLStreamReader source) throws XMLStreamException {
        final String terminalTag = source.getLocalName();

        //The key for the root has to be pulled out at the parent stage.
        this.degreesProperty.set(Integer.parseInt(source.getAttributeValue(null, "Degrees")));

        while(source.hasNext()) {
            final int typeNext = source.next();
            final String tag;
            switch (typeNext) {
                case XMLEvent.START_ELEMENT:
                    tag = source.getLocalName();
                    if (tag.equals("Parent")) {
                        super.readFromXml(source);
                    }
                    break;
                case XMLEvent.END_ELEMENT:
                    tag = source.getLocalName();
                    if (tag.equals(terminalTag)) {
                        //We didn't evaluate the predicate while loading, we evaluate it once now for all vertices.
                        this.getGraph().setPredicate(this::applyFilters);
                        return;
                    }
                    break;
            }
        }
    }

    @Override
    public void writeToXml(final XMLStreamWriter target) throws XMLStreamException {
        //The root will be written in the wrapper in plugin, since we have to parse it before reaching the Visualization element.
        target.writeAttribute("Degrees", Integer.toString(this.degreesProperty.intValue()));

        target.writeStartElement("Parent");
        super.writeToXml(target);
        target.writeEndElement();
    }

    @Override
    public String getTitle() {
        return "Watch View (" + this.root.getRootLogicalAddressMapping().toString() + ")";
    }

    public GraphLogicalVertex getRoot() {
        return this.root;
    }

    @Override
    protected VisualLogicalVertex createVisualLogicalVertexFor(final GraphLogicalVertex vertex, final ImageDirectoryWatcher.MappedImageList images) {
        final VisualLogicalVertex result = super.createVisualLogicalVertexFor(vertex, images);
        //If the vertex is in the primary visualization, copy its location for the new node on the watch window.
        final VisualLogicalVertex vertexMaster = this.getSessionState().getPrimaryVisualization().getVisualization().vertexFor(vertex);
        if(vertexMaster != null) {
            result.setTranslateX(vertexMaster.getTranslateX());
            result.setTranslateY(vertexMaster.getTranslateY());
        }
        return result;
    }

}
