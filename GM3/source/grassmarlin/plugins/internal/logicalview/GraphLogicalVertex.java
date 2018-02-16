package grassmarlin.plugins.internal.logicalview;

import com.sun.javafx.collections.ObservableListWrapper;
import grassmarlin.Event;
import grassmarlin.RuntimeConfiguration;
import grassmarlin.session.LogicalVertex;
import grassmarlin.session.PropertyContainer;
import grassmarlin.session.ThreadManagedState;
import grassmarlin.session.graphs.IHasKey;
import grassmarlin.session.pipeline.LogicalAddressMapping;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.ObservableList;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class GraphLogicalVertex extends PropertyContainer implements IHasKey {
    private final LogicalVertex vertex;
    private final String key;

    private final SimpleStringProperty title;
    private final ObservableList<LogicalVertex> childAddresses;

    private final Set<LogicalVertex> childrenToAdd;
    private final Set<LogicalVertex> childrenToRemove;

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
        }
    }

    public GraphLogicalVertex(final LogicalVertex vertex) {
        super(Event.PROVIDER_IN_THREAD);

        this.vertex = vertex;
        //key should be unique within each LogicalGraph
        this.key = String.format("[%s@%s].[%s@%s]", vertex.getHardwareVertex().getAddress().getClass().getName(), vertex.getHardwareVertex().getAddress(), vertex.getLogicalAddress().getClass().getName(), vertex.getLogicalAddress());

        this.title = new SimpleStringProperty(String.format("%s (%s)", this.vertex.getLogicalAddress(), this.vertex.getHardwareVertex().getAddress()));
        this.childAddresses = new ObservableListWrapper<>(new ArrayList<>());

        this.childrenToAdd = new HashSet<>();
        this.childrenToRemove = new HashSet<>();

        //This will cause a deferred repopulation of all Ui fields.
        this.state = new GraphLogicalVertexThreadManagedState(this.toString(), Event.PROVIDER_IN_THREAD);

        //The event handler can only be attached after the state is initialized.
        this.addAncestor(vertex);

        this.state.invalidate();
    }

    public void addChildVertex(final GraphLogicalVertex child) {
        this.state.invalidate(this.childAddresses, () -> {
            this.childrenToAdd.add(child.getVertex());
            this.childrenToAdd.addAll(child.getChildAddresses());
        });
    }
    public void addChildAddress(final LogicalVertex child) {
        this.state.invalidate(this.childAddresses, () -> this.childrenToAdd.add(child));
    }
    public void removeChildAddress(final LogicalVertex child) {
        this.state.invalidate(this.childAddresses, () -> this.childrenToRemove.add(child));
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

    @Override
    public final String getKey() {
        return this.key;
    }

    @Override
    public final String toString() {
        return this.title.get();
    }

    public void waitForState() {
        this.state.waitForValid();
    }
}
