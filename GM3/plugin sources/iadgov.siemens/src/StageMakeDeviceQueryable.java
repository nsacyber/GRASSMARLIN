package iadgov.siemens;

import grassmarlin.RuntimeConfiguration;
import grassmarlin.common.Confidence;
import grassmarlin.session.Property;
import grassmarlin.session.Session;
import grassmarlin.session.logicaladdresses.Ipv4;
import grassmarlin.session.logicaladdresses.Ipv4WithPort;
import grassmarlin.session.pipeline.AbstractStage;
import grassmarlin.session.pipeline.IHasLogicalVertexProperties;
import grassmarlin.session.pipeline.LogicalAddressMapping;

import java.util.Set;
import java.util.function.Consumer;

public class StageMakeDeviceQueryable extends AbstractStage<Session> {
    public static final Confidence MIN_CONFIDENCE = Confidence.MEDIUM_LOW;
    public static final String PROPERTY_RACK = "Siemens.Rack";
    public static final String PROPERTY_SLOT = "Siemens.Slot";

    public static final String OUTPUT_ADDRESSES = "Siemens Address Output";

    public StageMakeDeviceQueryable(final RuntimeConfiguration config, final Session session) {
        super(config, session, IHasLogicalVertexProperties.class);
    }

    @Override
    public Object process(Object obj) {
        if(obj instanceof IHasLogicalVertexProperties) {
            final IHasLogicalVertexProperties properties = (IHasLogicalVertexProperties)obj;

            //This is a pretty awful solution, but it works.
            final Set<Property<?>> propertiesRack = this.getContainer().logicalVertexFor(properties.getAddressMapping()).getProperties().get(PROPERTY_RACK);
            final Set<Property<?>> propertiesSlot = this.getContainer().logicalVertexFor(properties.getAddressMapping()).getProperties().get(PROPERTY_SLOT);

            if(propertiesRack == null || propertiesSlot == null) {
                //It cannot be converted--insufficient information.
            } else {
                final Ipv4 ipBase;
                if(properties.getAddressMapping().getLogicalAddress() instanceof Ipv4WithPort) {
                    ipBase = ((Ipv4WithPort)properties.getAddressMapping().getLogicalAddress()).getAddressWithoutPort();
                } else if(properties.getAddressMapping().getLogicalAddress() instanceof Ipv4) {
                    ipBase = (Ipv4)properties.getAddressMapping().getLogicalAddress();
                } else {
                    //This isn't passive, so if it isn't a hit we have to return obj.
                    return obj;
                }
                //HACK: Should probably look to add multiple addresses.
                //TODO: There is no way to parse this data through fingerprints, so it will have to be faked elsewhere...
                // We are to support:
                //   S7 300 (Rack 0, Slot 2)
                //   S7 400 (Varies)
                //   S7 1200 (Rack 0, Slot 0, or Rack 0, Slot 1)
                final int rack = propertiesRack.stream().filter(property -> property.getValue() instanceof Integer).mapToInt(property -> (Integer)property.getValue()).findAny().orElse(0);
                final int slot = propertiesSlot.stream().filter(property -> property.getValue() instanceof Integer).mapToInt(property -> (Integer)property.getValue()).findAny().orElse(0);

                final LogicalAddressMapping mappingNew = new LogicalAddressMapping(properties.getAddressMapping().getHardwareAddress(), new Ipv4WithRackAndSlot(ipBase, rack, slot));

                //Create the mapping in the session
                this.getContainer().logicalVertexFor(mappingNew);
                //Output the mapping to the addresses output line.
                final Consumer<Object> out = this.targetOf(OUTPUT_ADDRESSES);
                if(out != null) {
                    out.accept(mappingNew);
                }
            }
        }
        return obj;
    }

    @Override
    public String getName() {
        return "Siemens Make Devices Queryable";
    }
}
