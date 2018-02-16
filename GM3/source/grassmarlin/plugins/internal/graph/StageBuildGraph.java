package grassmarlin.plugins.internal.graph;

import grassmarlin.RuntimeConfiguration;
import grassmarlin.session.HardwareAddress;
import grassmarlin.session.HardwareVertex;
import grassmarlin.session.LogicalVertex;
import grassmarlin.session.Session;
import grassmarlin.session.pipeline.*;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.function.Consumer;

public class StageBuildGraph extends AbstractStage<Session> {
    public static final String NAME = "Create Basic Graph Elements";
    public static final String OUTPUT_HARDWAREADDRESSES = "Hardware Addresses";
    public static final String OUTPUT_LOGICALADDRESSMAPPINGS = "Address Mappings";
    public static final String OUTPUT_PACKETS_RECORDABLE = "Recordable Packets";
    public static final String OUTPUT_PACKETS_QUESTIONABLE = "Questionable Packets";

    public StageBuildGraph(final RuntimeConfiguration config, final Session session) {
        //We need to listen to packet metadata to ensure that it goes through the queue so that packets are processed only after the relevant edges/vertices have been constructed.
        super(config, session, new ArrayBlockingQueue<>(10000), Object.class); //HACK: Should be: HardwareAddress.class, LogicalAddressMapping.class, Session.AddressPair.class, IPacketMetadata.class);

        // The HardwareAddress values which this class receives are probably meaningless to downstream stages, but rather than delete them they are removed from the main stream and handed to an alternate stream which is probably not connected to anything.
        defineOutput(OUTPUT_HARDWAREADDRESSES, HardwareAddress.class);
        defineOutput(OUTPUT_LOGICALADDRESSMAPPINGS, LogicalAddressMapping.class);
        defineOutput(OUTPUT_PACKETS_RECORDABLE, IPacketMetadata.class);
        defineOutput(OUTPUT_PACKETS_QUESTIONABLE);
        disallowOutputClasses(AbstractStage.DEFAULT_OUTPUT, HardwareAddress.class, LogicalAddressMapping.class, ILogicalPacketMetadata.class);
        disallowOutputClasses(OUTPUT_PACKETS_QUESTIONABLE, Object.class);
    }

    @Override
    public Object process(final Object obj) {
        if(obj instanceof ILogicalPacketMetadata) {
            if(obj instanceof IIcmpPacketMetadata) {
                //Look for routing issues in ICMP responses.  These messages can't be used to build the logical map, but can provide important detail to the physical map.
                final IIcmpPacketMetadata icmp = (IIcmpPacketMetadata)obj;
                switch(icmp.getIcmpType()) {
                    case 0:
                        //Ping response--this can be recorded in the logical graph
                        getContainer().createEdgeBetween(icmp.getSourceAddress(), icmp.getDestAddress());
                        //We return from here because we want normal processing.
                        return obj;
                    case 3: //Destination Unreachable
                        //The source of this message is the thing that could not be reached from the destination.
                        if(icmp.getDestAddress() instanceof LogicalAddressMapping) {
                            //getContainer().logicalVertexFor()
                            //graph.addAnnotation(((LogicalAddressMapping)icmp.getDestAddress()).getHardwareAddress(), ANNOTATION_ROUTE_ERROR);
                        } else {
                            //graph.addAnnotation((HardwareAddress)icmp.getDestAddress(), ANNOTATION_ROUTE_ERROR);
                        }
                        if(icmp.getSourceAddress() instanceof LogicalAddressMapping) {
                            //graph.addAnnotation(((LogicalAddressMapping)icmp.getSourceAddress()).getHardwareAddress(), ANNOTATION_UNREACHABLE);
                        } else {
                            //graph.addAnnotation((HardwareAddress)icmp.getSourceAddress(), ANNOTATION_UNREACHABLE);
                        }
                        break;
                    case 5: //Redirect
                        //This can be completely normal, but may be a cause for concern.  The entity receiving the redirect is less of an issue than the entity for which communication has been redirected.
                        if(icmp.getSourceAddress() instanceof LogicalAddressMapping) {
                            //graph.addAnnotation(((LogicalAddressMapping)icmp.getSourceAddress()).getHardwareAddress(), ANNOTATION_REDIRECTED);
                        } else {
                            //graph.addAnnotation((HardwareAddress)icmp.getSourceAddress(), ANNOTATION_REDIRECTED);
                        }
                        break;
                    case 9: //Router Advertisement
                        //Anything that is sending router advertisements should be treated as a gateway.
                        if(icmp.getSourceAddress() instanceof LogicalAddressMapping) {
                            //graph.addAnnotation(((LogicalAddressMapping)icmp.getSourceAddress()).getHardwareAddress(), ANNOTATION_GATEWAY);
                        } else {
                            //graph.addAnnotation((HardwareAddress)icmp.getSourceAddress(), ANNOTATION_GATEWAY);
                        }
                        break;
                }
                //Any ICMP packet that isn't a ping response will be discarded as questionable.
                final Consumer<Object> target = this.targetOf(OUTPUT_PACKETS_QUESTIONABLE);
                if(target != null) {
                    target.accept(obj);
                }
                return null;
            } else {
                this.getContainer().createEdgeBetween(((ILogicalPacketMetadata) obj).getSourceAddress(), ((ILogicalPacketMetadata) obj).getDestAddress());
            }
        } else if(obj instanceof Session.HardwareAddressPair) {
            getContainer().createEdgeBetween((Session.HardwareAddressPair) obj);
        } else if(obj instanceof LogicalAddressMapping) {
            final LogicalVertex logicalVertex = getContainer().logicalVertexFor((LogicalAddressMapping) obj);
        } else if(obj instanceof HardwareAddress) {
            final HardwareVertex hardwareVertex = getContainer().hardwareVertexFor((HardwareAddress) obj);
        } else if(obj instanceof Network) {
            this.getContainer().addNetwork(NAME, (Network)obj);
        }

        //We always return the original object from this stage
        return obj;
    }

    @Override
    public String getName() {
        return NAME;
    }
}
