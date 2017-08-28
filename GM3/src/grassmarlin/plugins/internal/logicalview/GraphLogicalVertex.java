package grassmarlin.plugins.internal.logicalview;

import com.sun.javafx.collections.ObservableListWrapper;
import grassmarlin.Event;
import grassmarlin.RuntimeConfiguration;
import grassmarlin.session.LogicalVertex;
import grassmarlin.session.PropertyContainer;
import grassmarlin.session.ThreadManagedState;
import grassmarlin.session.graphs.IHasKey;
import grassmarlin.session.pipeline.LogicalAddressMapping;
import grassmarlin.session.pipeline.Network;
import grassmarlin.ui.common.dialogs.details.PropertyContainerDetailsDialog;
import grassmarlin.ui.common.menu.ActiveMenuItem;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.ObservableList;
import javafx.scene.control.MenuItem;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.stream.events.XMLEvent;
import java.util.*;
import java.util.stream.Collectors;

public class GraphLogicalVertex extends PropertyContainer implements IHasKey, ICanHasContextMenu {
    public final static String SOURCE_PROPERTIES = "GraphLogicalVertex";

    // The Network property is not set on the HardwareVertex; it is inferred from address and the Networks in the session.  It is then reported by the GraphLogicalVertex as a property.
    public final static String PROPERTY_NETWORKS = "Network";

    private final LogicalGraph graph;
    private final LogicalVertex vertex;
    private final String key;

    private final SimpleStringProperty title;
    private final ObservableList<LogicalVertex> childAddresses;

    private final Set<LogicalVertex> childrenToAdd;
    private final Set<LogicalVertex> childrenToRemove;
    private final Set<Network> networksNew;

    private final ThreadManagedState state;
    private class GraphLogicalVertexThreadManagedState extends ThreadManagedState {
        public GraphLogicalVertexThreadManagedState(final String title, final Event.IAsyncExecutionProvider provider) {
            super(RuntimeConfiguration.UPDATE_INTERVAL_MS, title, provider);
        }

        @Override
        public void validate() {
            if(hasFlag(childAddresses)) {
                synchronized(GraphLogicalVertex.this.childAddresses) {
                    GraphLogicalVertex.this.childrenToAdd.removeAll(GraphLogicalVertex.this.childrenToRemove);
                    for(final LogicalVertex vertNew : GraphLogicalVertex.this.childrenToAdd) {
                        GraphLogicalVertex.this.addAncestor(vertNew);
                    }
                    GraphLogicalVertex.this.childAddresses.addAll(GraphLogicalVertex.this.childrenToAdd);
                    GraphLogicalVertex.this.childrenToAdd.clear();
                    GraphLogicalVertex.this.childAddresses.removeAll(GraphLogicalVertex.this.childrenToRemove);

                    for(final LogicalVertex vertRemoved : GraphLogicalVertex.this.childrenToRemove) {
                        GraphLogicalVertex.this.removeAncestor(vertRemoved);
                    }
                    GraphLogicalVertex.this.childrenToRemove.clear();
                }
            }
            if(hasFlag(PROPERTY_NETWORKS)) {
                final Set<Network> networksToAssign = new HashSet<>();
                for(final Network network : GraphLogicalVertex.this.graph.getNetworks()) {
                    if(network.getValue().contains(GraphLogicalVertex.this.vertex.getLogicalAddress())) {
                        networksToAssign.add(network);
                    }
                }

                GraphLogicalVertex.this.setProperties(SOURCE_PROPERTIES, PROPERTY_NETWORKS, networksToAssign);
            }
        }
    }

    public GraphLogicalVertex(final LogicalGraph graph, final LogicalVertex vertex, final Event.IAsyncExecutionProvider uiProvider) {
        super(uiProvider);

        this.graph = graph;
        this.vertex = vertex;
        //key should be unique within each LogicalGraph
        this.key = String.format("[%s@%s].[%s@%s]", vertex.getHardwareVertex().getAddress().getClass().getName(), vertex.getHardwareVertex().getAddress(), vertex.getLogicalAddress().getClass().getName(), vertex.getLogicalAddress());

        this.title = new SimpleStringProperty(String.format("%s (%s)", this.vertex.getLogicalAddress(), this.vertex.getHardwareVertex().getAddress()));
        this.childAddresses = new ObservableListWrapper<>(new ArrayList<>());

        this.childrenToAdd = new HashSet<>();
        this.childrenToRemove = new HashSet<>();
        this.networksNew = new HashSet<>();

        //This will cause a deferred repopulation of all Ui fields.
        this.state = new GraphLogicalVertexThreadManagedState(this.toString(), uiProvider);

        //The event handler can only be attached after the state is initialized.
        this.addAncestor(vertex);

        this.state.invalidate();
    }

    public void addChildAddress(final LogicalVertex child) {
        this.state.invalidate(this.childAddresses, () -> this.childrenToAdd.add(child));
    }
    public void removeChildAddress(final LogicalVertex child) {
        this.state.invalidate(this.childAddresses, () -> this.childrenToRemove.add(child));
    }
    public void testAndSetNetworks(final List<Network> networksAll) {
        state.invalidate(PROPERTY_NETWORKS);
    }

    public LogicalVertex getVertex() {
        return this.vertex;
    }

    public LogicalAddressMapping getRootLogicalAddressMapping() {
        return this.vertex.getLogicalAddressMapping();
    }

    public StringProperty titleProperty() {
        return this.title;
    }
    public ObservableList<LogicalVertex> getChildAddresses() {
        return childAddresses;
    }

    private void showDetailsDialog(final GraphLogicalVertex vertex) {
        final PropertyContainerDetailsDialog dlgDetails = new PropertyContainerDetailsDialog();
        dlgDetails.targetProperty().set(vertex.getProperties());
        dlgDetails.setTitle("Details for " + vertex.getVertex().getLogicalAddressMapping());
        dlgDetails.showAndWait();
    }
    private void showDetailsDialog(final GraphLogicalEdge edge) {
        final PropertyContainerDetailsDialog dlgDetails = new PropertyContainerDetailsDialog();
        dlgDetails.targetProperty().set(edge.getProperties());
        dlgDetails.setTitle("Details for " + edge.getSource().getVertex().getLogicalAddressMapping() + " to " + edge.getDestination().getVertex().getLogicalAddressMapping());
        dlgDetails.showAndWait();
    }

    @Override
    public List<MenuItem> getContextMenuItems() {
        final List<MenuItem> result = new ArrayList<>();
        result.addAll(Arrays.asList(
                new ActiveMenuItem("Details for " + getVertex().getLogicalAddress() + "...", event -> GraphLogicalVertex.this.showDetailsDialog(GraphLogicalVertex.this))
        ));
        result.addAll(
                graph.getEdges().stream()
                        .filter(edge -> edge.getSource() == this || edge.getDestination() == this)
                        .map(edge -> new ActiveMenuItem(String.format("Details for %s to %s...", edge.getSource().getRootLogicalAddressMapping(), edge.getDestination().getRootLogicalAddressMapping()), event -> GraphLogicalVertex.this.showDetailsDialog(edge)))
                        .collect(Collectors.toList())
        );
        return result;
    }

    @Override
    public final String getKey() {
        return this.key;
    }

    @Override
    public final String toString() {
        return this.title.get();
    }

    @Override
    public void writeToXml(final XMLStreamWriter writer) throws XMLStreamException {
        writer.writeAttribute("key", getKey());
        writer.writeStartElement("Properties");
        super.writeToXml(writer);
        writer.writeEndElement();
    }

    @Override
    public void readFromXml(final XMLStreamReader reader) throws XMLStreamException {
        while(reader.hasNext()) {
            final int typeNext = reader.next();
            switch(typeNext) {
                case XMLEvent.START_ELEMENT:
                    if(reader.getLocalName().equals("Properties")) {
                        super.readFromXml(reader);
                    }
                    break;
                case XMLEvent.END_ELEMENT:
                    if(reader.getLocalName().equals("Vertex")) {
                        this.state.invalidate();
                        return;
                    }
                    break;
            }
        }
    }

    public void waitForState() {
        this.state.waitForValid();
    }
}
