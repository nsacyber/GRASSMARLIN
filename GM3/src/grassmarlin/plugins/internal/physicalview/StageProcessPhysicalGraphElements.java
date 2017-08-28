package grassmarlin.plugins.internal.physicalview;

import grassmarlin.RuntimeConfiguration;
import grassmarlin.plugins.IPlugin;
import grassmarlin.plugins.internal.AggregatePlugin;
import grassmarlin.plugins.internal.physicalview.graph.Router;
import grassmarlin.plugins.internal.physicalview.graph.Switch;
import grassmarlin.session.HardwareAddress;
import grassmarlin.session.Session;
import grassmarlin.session.pipeline.AbstractStage;
import grassmarlin.session.pipeline.IIcmpPacketMetadata;
import grassmarlin.session.pipeline.LogicalAddressMapping;

import java.util.ArrayList;
import java.util.List;

public class StageProcessPhysicalGraphElements extends AbstractStage<Session> {
    public static final String NAME = "Build Physical Graph";
    public static final String ANNOTATION_UNREACHABLE = "Unreachable";
    public static final String ANNOTATION_ROUTE_ERROR = "Routing Error";
    public static final String ANNOTATION_REDIRECTED = "Redirected";
    public static final String ANNOTATION_GATEWAY = "Gateway";


    private final Plugin plugin;

    public StageProcessPhysicalGraphElements(final RuntimeConfiguration config, final Session session) {
        super(config, session, IIcmpPacketMetadata.class, Switch.class, Router.class);

        final IPlugin plugin = config.pluginFor(this.getClass());
        if(plugin instanceof AggregatePlugin) {
            this.plugin = ((AggregatePlugin)plugin).getMember(Plugin.class);
        } else {
            this.plugin = (Plugin)plugin;
        }
    }

    public Object process(final Object o) {
        final PhysicalGraph graph = this.plugin.stateForSession(super.getContainer()).graph;

        if(o instanceof IIcmpPacketMetadata) {
            //Look for routing issues in ICMP responses.  These messages can't be used to build the logical map, but can provide important detail to the physical map.
            final IIcmpPacketMetadata icmp = (IIcmpPacketMetadata)o;
            switch(icmp.getIcmpType()) {
                case 3: //Destination Unreachable
                    //The source of this message is the thing that could not be reached from the destination.
                    if(icmp.getDestAddress() instanceof LogicalAddressMapping) {
                        graph.addAnnotation(((LogicalAddressMapping)icmp.getDestAddress()).getHardwareAddress(), ANNOTATION_ROUTE_ERROR);
                    } else {
                        graph.addAnnotation((HardwareAddress)icmp.getDestAddress(), ANNOTATION_ROUTE_ERROR);
                    }
                    if(icmp.getSourceAddress() instanceof LogicalAddressMapping) {
                        graph.addAnnotation(((LogicalAddressMapping)icmp.getSourceAddress()).getHardwareAddress(), ANNOTATION_UNREACHABLE);
                    } else {
                        graph.addAnnotation((HardwareAddress)icmp.getSourceAddress(), ANNOTATION_UNREACHABLE);
                    }
                    break;
                case 5: //Redirect
                    //This can be completely normal, but may be a cause for concern.  The entity receiving the redirect is less of an issue than the entity for which communication has been redirected.
                    if(icmp.getSourceAddress() instanceof LogicalAddressMapping) {
                        graph.addAnnotation(((LogicalAddressMapping)icmp.getSourceAddress()).getHardwareAddress(), ANNOTATION_REDIRECTED);
                    } else {
                        graph.addAnnotation((HardwareAddress)icmp.getSourceAddress(), ANNOTATION_REDIRECTED);
                    }
                    break;
                case 9: //Router Advertisement
                    //Anything that is sending router advertisements should be treated as a gateway.
                    if(icmp.getSourceAddress() instanceof LogicalAddressMapping) {
                        graph.addAnnotation(((LogicalAddressMapping)icmp.getSourceAddress()).getHardwareAddress(), ANNOTATION_GATEWAY);
                    } else {
                        graph.addAnnotation((HardwareAddress)icmp.getSourceAddress(), ANNOTATION_GATEWAY);
                    }
                    break;
            }
        } else if(o instanceof Switch) {
            final Switch sWitch = (Switch)o;
            /* Keeping this around just in case we need it again...
            if(sWitch.compareTo(new Duck()) == 0) {
                new Fire().consume(sWitch);
            }
             */
            // Add all the hardware addresses to the graph...
            HardwareAddress prev = null;
            for(final Switch.PortGroup group : sWitch.getPortGroups()) {
                final List<HardwareAddress> addresses = new ArrayList<>();

                for(final Switch.Port port : group.getPorts().keySet()) {
                    addresses.add(port.getAddress());
                    addresses.addAll(port.getConnectedAddresses());
                }

                //We need to link all the addresses together, which we do using a chain rather than a star.
                for(final HardwareAddress address : addresses) {
                    //We can discard the vertex
                    this.getContainer().hardwareVertexFor(address);
                    if(prev != null) {
                        //The physical graph will treat the edge as bidirectional.
                        this.getContainer().createEdgeBetween(address, prev);
                    }
                    prev = address;
                }
            }
            graph.addDevice(sWitch);
        }

        return o;
    }

    @Override
    public String getName() {
        return NAME;
    }
}
