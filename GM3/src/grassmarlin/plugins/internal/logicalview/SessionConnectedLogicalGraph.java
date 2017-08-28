package grassmarlin.plugins.internal.logicalview;

import grassmarlin.Event;
import grassmarlin.session.Edge;
import grassmarlin.session.Session;
import grassmarlin.session.pipeline.LogicalAddressMapping;
import grassmarlin.session.pipeline.Network;

import java.util.List;

public class SessionConnectedLogicalGraph extends LogicalGraph {

    public SessionConnectedLogicalGraph(final Plugin.LogicalGraphState statePlugin) {
        this(statePlugin, Event.createThreadQueueProvider());
    }
    public SessionConnectedLogicalGraph(final Plugin.LogicalGraphState statePlugin, final Event.IAsyncExecutionProvider executor) {
        super(statePlugin, executor);

        this.statePlugin.getSession().onLogicalVertexCreated.addHandler(this.handlerLogicalVertexCreated);
        this.statePlugin.getSession().onEdgeCreated.addHandler(this.handlerEdgeCreated);
        this.statePlugin.getSession().onPacketReceived.addHandler(this.handlerPacketReceived);
        this.statePlugin.getSession().onNetworkChange.addHandler(this.handlerNetworkChanged);
    }

    //<editor-fold desc="Respond to events in Session">
    private final Event.EventListener<Session.LogicalVertexEvent> handlerLogicalVertexCreated = this::handleLogicalVertexCreated;
    private void handleLogicalVertexCreated(final Event<Session.LogicalVertexEvent> event, final Session.LogicalVertexEvent args) {
        this.state.invalidate(this.vertices, () -> this.pendingVertices.add(args.getLogicalVertex()));
    }

    private final Event.EventListener<Session.EdgeEvent> handlerEdgeCreated = this::handleEdgeCreated;
    private void handleEdgeCreated(final Event<Session.EdgeEvent> event, final Session.EdgeEvent args) {
        //Edges might connect HardwareAddresses, and we ignore those (at least for now, possibly forever).
        if(args.getEdge().getSource() instanceof LogicalAddressMapping && args.getEdge().getDestination() instanceof LogicalAddressMapping) {
            //The ThreadManagedState will perform the bulk of the work.
            this.state.invalidate(this.edges, () -> this.pendingEdges.add((Edge<LogicalAddressMapping>)args.getEdge()));
        }
    }

    private final Event.EventListener<Session.PacketEvent> handlerPacketReceived = this::handlePacketReceived;
    private void handlePacketReceived(final Event<Session.PacketEvent> event, final Session.PacketEvent args) {
        this.state.invalidate(this.pendingPackets, () -> {
            this.pendingPackets.add(args);
        });
    }

    private final Event.EventListener<List<Network>> handlerNetworkChanged = this::handleNetworkChanged;
    private void handleNetworkChanged(final Event<List<Network>> event, List<Network> args) {
        this.state.invalidate(this.networks, () -> {
            this.networks.clear();
            this.networks.addAll(args);
        });
    }
    //</editor-fold>

    @Override
    public String toString() {
        StringBuilder sbResult = new StringBuilder();
        sbResult.append("Vertices:");
        for(final GraphLogicalVertex vertex : this.vertices) {
            sbResult.append(vertex);
        }
        sbResult.append("Edges:");
        for(final GraphLogicalEdge edge : this.edges) {
            sbResult.append(edge);
        }
        return sbResult.toString();
    }
}
