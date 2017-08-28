package grassmarlin.plugins.internal.logicalview;

import grassmarlin.RuntimeConfiguration;
import grassmarlin.session.LogicalAddress;
import grassmarlin.session.Session;
import grassmarlin.session.logicaladdresses.Cidr;
import grassmarlin.session.logicaladdresses.Ipv4;
import grassmarlin.session.logicaladdresses.Ipv4WithPort;
import grassmarlin.session.pipeline.AbstractStage;
import grassmarlin.session.pipeline.LogicalAddressMapping;
import grassmarlin.session.pipeline.Network;

/**
 * This stage watches LogicalAddressMapping objects and uses the reported values to create networks.
 */
public class StageNetworkCreationFromLogicalAddresses extends AbstractStage<Session> {
    public static final String NAME = "Create Networks From Logical Addresses";
    public static final String OUTPUT_NETWORKS = "Networks";

    private boolean createSubnets;
    private int createdSubnetSize;
    private int createdSubnetConfidence;

    public StageNetworkCreationFromLogicalAddresses(final RuntimeConfiguration config, final Session session) {
        super(config, session, LogicalAddressMapping.class);

        setPassiveMode(true);

        defineOutput(OUTPUT_NETWORKS, Network.class);

        createSubnets = true;
        createdSubnetSize = 24;
        createdSubnetConfidence = 4;
    }

    @Override
    public Object process(final Object obj) {
        if(obj instanceof LogicalAddressMapping) {
            final LogicalAddressMapping mapping = (LogicalAddressMapping)obj;
            final LogicalAddress<?> address = mapping.getLogicalAddress();
            if(createSubnets) {
                if (address instanceof Ipv4) {
                    return new Network(new Cidr(((Ipv4) address).getRawAddress(), createdSubnetSize), createdSubnetConfidence);
                } else if (address instanceof Ipv4WithPort) {
                    return new Network(new Cidr(((Ipv4WithPort)address).getRawAddress(), createdSubnetSize), createdSubnetConfidence);
                } else if (address instanceof Cidr) {
                    return new Network(address, createdSubnetConfidence);
                }
                //TODO: Expand for IPv6, wireless, etc.
            }
        }
        return null;
    }

    @Override
    public String getName() {
        return NAME;
    }
}
