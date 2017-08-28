package grassmarlin.plugins.internal.graph;

import grassmarlin.RuntimeConfiguration;
import grassmarlin.session.HardwareAddress;
import grassmarlin.session.HardwareVertex;
import grassmarlin.session.LogicalVertex;
import grassmarlin.session.Session;
import grassmarlin.session.pipeline.AbstractStage;
import grassmarlin.session.pipeline.IPacketMetadata;
import grassmarlin.session.pipeline.LogicalAddressMapping;
import grassmarlin.session.pipeline.Network;

import java.util.concurrent.ArrayBlockingQueue;

public class StageBuildGraph extends AbstractStage<Session> {
    public static final String NAME = "Create Basic Graph Elements";
    public static final String OUTPUT_HARDWAREADDRESSES = "Hardware Addresses";
    public static final String OUTPUT_LOGICALADDRESSMAPPINGS = "Address Mappings";

    public StageBuildGraph(final RuntimeConfiguration config, final Session session) {
        //We need to listen to packet metadata to ensure that it goes through the queue so that packets are processed only after the relevant edges/vertices have been constructed.
        super(config, session, new ArrayBlockingQueue<>(10000), Object.class); //HACK: Should be: HardwareAddress.class, LogicalAddressMapping.class, Session.AddressPair.class, IPacketMetadata.class);

        // The HardwareAddress values which this class receives are probably meaningless to downstream stages, but rather than delete them they are removed from the main stream and handed to an alternate stream which is probably not connected to anything.
        defineOutput(OUTPUT_HARDWAREADDRESSES, HardwareAddress.class);
        defineOutput(OUTPUT_LOGICALADDRESSMAPPINGS, LogicalAddressMapping.class);
        disallowOutputClasses(AbstractStage.DEFAULT_OUTPUT, HardwareAddress.class, LogicalAddressMapping.class);
    }

    @Override
    public Object process(final Object obj) {
        if(obj instanceof IPacketMetadata) {
            getContainer().createEdgeBetween(((IPacketMetadata) obj).getSourceAddress(), ((IPacketMetadata) obj).getDestAddress());
        } else if(obj instanceof Session.AddressPair) {
            getContainer().createEdgeBetween((Session.AddressPair) obj);
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
