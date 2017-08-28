package grassmarlin.plugins.internal.physicalview;

import grassmarlin.Event;
import grassmarlin.session.Edge;
import grassmarlin.session.HardwareAddress;
import grassmarlin.session.Session;
import grassmarlin.session.hardwareaddresses.Mac;

public class SessionConnectedPhysicalGraph extends PhysicalGraph {
    /**
     * While intended to filter broadcast traffic, this functions as a generic filter instead.
     * We filter out broadcast traffic because reuse tends to create messed up layouts, but
     * sometimes that is desirable--there are several well-defined broadcast MACs for specific
     * protocols that we don't expect to see, so if they show up, something warrants deeper inspection.
     *
     * @param address
     * @return true if address should be excluded from the graph.
     */
    protected static boolean isBroadcast(final HardwareAddress address) {
        if(address instanceof Mac) {
            final int[] arr = address.getAddress();
            //IPv6 Multicast / Neighbor discovery
            if(arr[0] == 0x33 && arr[1] == 0x33) {
                return true;
            }
            //Broadcast
            if(arr[0] == 0xFF && arr[1] == 0xFF && arr[2] == 0xFF && arr[3] == 0xFF && arr[4] == 0xFF && arr[5] == 0xFF) {
                return true;
            }
        }
        return false;
    }

    public SessionConnectedPhysicalGraph(final Session session, final Event.IAsyncExecutionProvider executionProvider) {
        super(executionProvider);

        session.onHardwareVertexCreated.addHandler(this.handlerHardwareVertexCreated);
        session.onEdgeCreated.addHandler(this.handlerEdgeCreated);
        session.onLogicalVertexCreated.addHandler(this.handlerLogicalVertexCreated);
    }

    private final Event.EventListener<Session.HardwareVertexEvent> handlerHardwareVertexCreated = this::handleHardwareVertexCreated;
    private final Event.EventListener<Session.EdgeEvent> handlerEdgeCreated = this::handleEdgeCreated;
    private final Event.EventListener<Session.LogicalVertexEvent> handlerLogicalVertexCreated = this::handleLogicalVertexCreated;

    @Override
    public void addHardwareAddress(final HardwareAddress address) {
        if(!isBroadcast(address)) {
            super.addHardwareAddress(address);
        }
    }

    protected void handleHardwareVertexCreated(final Event<Session.HardwareVertexEvent> event, Session.HardwareVertexEvent args) {
        this.addHardwareAddress(args.getHardwareVertex().getAddress());
    }
    protected void handleEdgeCreated(final Event<Session.EdgeEvent> event, Session.EdgeEvent args) {
        if(args.getEdge().getSource() instanceof HardwareAddress) {
            this.addHardwareAddressConnection((Edge<HardwareAddress>)args.getEdge());
        }
    }
    protected void handleLogicalVertexCreated(final Event<Session.LogicalVertexEvent> event, Session.LogicalVertexEvent args) {
        this.addLogicalAddressMapping(args.getLogicalVertex().getLogicalAddressMapping());
    }
}
