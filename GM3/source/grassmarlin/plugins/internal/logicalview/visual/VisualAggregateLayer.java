package grassmarlin.plugins.internal.logicalview.visual;

import grassmarlin.Event;
import grassmarlin.plugins.internal.logicalview.GraphLogicalVertex;
import grassmarlin.plugins.internal.logicalview.LogicalGraph;
import grassmarlin.session.Property;
import grassmarlin.session.serialization.XmlSerializable;
import javafx.application.Platform;
import javafx.scene.Group;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import java.util.*;
import java.util.stream.Collectors;

public class VisualAggregateLayer extends Group implements XmlSerializable {
    private final LogicalVisualization visualization;
    private final String nameProperty;
    private final LogicalGraph graph;
    private final Map<Property<?>, VisualLogicalAggregate> mapAggregates;

    public VisualAggregateLayer(final LogicalVisualization visualization, final String name, final LogicalGraph graph) {
        this.visualization = visualization;
        this.nameProperty = name;
        this.graph = graph;
        this.mapAggregates = new HashMap<>();

        this.graph.onPropertyValuesChanged.addHandler(this.handlerPropertyChanged);
    }

    public Event.EventListener<String> handlerPropertyChanged = this::handlePropertyChanged;
    private void handlePropertyChanged(final Event<String> event, final String property) {
        if(this.nameProperty.equals(property)) {
            this.requestLayout();
        }
    }

    public void clear() {
        for(final VisualLogicalAggregate visual : mapAggregates.values()) {
            this.visualization.getZsp().removeLayeredChild(visual);
        }
        //TODO: This used to clear mapAggregates; anything already in there isn't re-added when this is reactivated.
    }
    public void restore() {
        for(final VisualLogicalAggregate visual : mapAggregates.values()) {
            this.visualization.getZsp().addLayeredChild(visual);
        }
    }

    @Override
    protected void layoutChildren() {
        // Identify all necessary groupings
        final ArrayList<GraphLogicalVertex> vertices = new ArrayList<>();
        this.graph.getVertices(vertices);

        final List<Property<?>> lstGroups = vertices.stream().map(vertex -> vertex.getProperties().get(VisualAggregateLayer.this.nameProperty)).filter(set -> set != null).flatMap(set -> set.stream()).distinct().collect(Collectors.toList());

        // Ensure the right groupings exist
        final LinkedList<Property<?>> removed = new LinkedList<>(mapAggregates.keySet());
        removed.removeAll(lstGroups);
        for(final Property<?> key : removed) {
            this.visualization.getZsp().removeLayeredChild(this.mapAggregates.remove(key));
        }
        final LinkedList<Property<?>> added = new LinkedList<>(lstGroups);
        added.removeAll(this.mapAggregates.keySet());
        for(final Property<?> key : added) {
            final VisualLogicalAggregate aggregate = new VisualLogicalAggregate(this.visualization, this.nameProperty, key);
            final VisualLogicalAggregate oldValue = this.mapAggregates.put(key, aggregate);
            if(oldValue != null) {
                //In case of race condition, don't leave orphaned hulls on the graph.
                this.visualization.getZsp().removeLayeredChild(oldValue);
            }
            this.visualization.getZsp().addLayeredChild(aggregate);
        }

        // Recalculate all the hulls
        //We don't bother with a call to super.layoutChildren because this group doesn't actually have children (it would, except MultiLayeredNode-based children require special handling)
        for(VisualLogicalAggregate visual : this.mapAggregates.values()) {
            Platform.runLater(() -> visual.requestLayout());
        }
    }

    @Override
    public void readFromXml(XMLStreamReader source) throws XMLStreamException {
        final String elementTerminal = source.getLocalName();

        Property<?> property = null;
        VisualLogicalAggregate visual = null;

        while(source.hasNext()) {
            switch(source.nextTag()) {
                case XMLStreamReader.START_ELEMENT:
                    switch(source.getLocalName()) {
                        case "Entry":
                            property = null;
                            visual = null;
                            break;
                        case "Property":
                            property = new Property<>(source);
                            break;
                        case "Visual":
                            visual = new VisualLogicalAggregate(this.visualization, this.nameProperty, property);
                            visual.readFromXml(source);
                            break;
                    }
                    break;
                case XMLStreamReader.END_ELEMENT:
                    if(source.getLocalName().equals(elementTerminal)) {
                        return;
                    } else if(source.getLocalName().equals("Entry")) {
                        if(property != null && visual != null) {
                            final VisualLogicalAggregate oldValue = this.mapAggregates.put(property, visual);
                            if(oldValue != null) {
                                this.visualization.getZsp().removeLayeredChild(oldValue);
                            }
                        }
                    }
                    break;
                case XMLStreamReader.END_DOCUMENT:
                    return;
            }
        }
    }

    @Override
    public void writeToXml(XMLStreamWriter target) throws XMLStreamException {
        //Don't write the property name attribute--it has to be provided to the constructor which means the caller will write it.
        for(Map.Entry<Property<?>, VisualLogicalAggregate> entry : this.mapAggregates.entrySet()) {
            target.writeStartElement("Entry");

            target.writeStartElement("Property");
            entry.getKey().writeToXml(target);
            target.writeEndElement();

            target.writeStartElement("Visual");
            entry.getValue().writeToXml(target);
            target.writeEndElement();

            target.writeEndElement();
        }
    }
}
