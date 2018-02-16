package grassmarlin.plugins.internal.physical.deviceimport;

import grassmarlin.common.Confidence;
import grassmarlin.session.HardwareAddress;
import grassmarlin.session.LogicalAddress;
import grassmarlin.session.Property;
import grassmarlin.session.pipeline.IHasHardwareVertexProperties;
import grassmarlin.session.pipeline.LogicalAddressMapping;

import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;

public class Host {
    public static Confidence CONFIDENCE = Confidence.MEDIUM_HIGH;

    private final Map<String, Collection<Property<?>>> properties;
    private HardwareAddress addrHw;
    private Collection<LogicalAddress> addrsLogical;

    public Host() {
        this.properties = new HashMap<>();
        this.addrHw = null;
        this.addrsLogical = new HashSet<>();
    }

    public void setHardwareAddress(final HardwareAddress address) {
        this.addrHw = address;
    }

    public void addLogicalAddress(final LogicalAddress address) {
        this.addrsLogical.add(address);
    }

    public void addProperty(final String property, final Serializable value) {
        this.addProperty(property, value, CONFIDENCE);
    }

    public void addProperty(final String property, final Serializable value, final Confidence confidence) {
        final Collection<Property<?>> properties = this.properties.get(property);
        if(properties == null) {
            this.properties.put(property, new HashSet<>(Collections.singleton(new Property<>(value, confidence))));
        }
    }

    public boolean isValid() {
        return
                (addrHw != null)
                &&
                (!addrsLogical.isEmpty());
    }

    public Collection<Object> getEntities() {
        final List<Object> elements = new ArrayList<>();
        elements.add(this.addrHw);
        elements.addAll(this.addrsLogical.stream().map(logical -> new LogicalAddressMapping(addrHw, logical)).collect(Collectors.toList()));
        if(!properties.isEmpty()) {
            elements.add(new IHasHardwareVertexProperties() {
                @Override
                public String getPropertySource() {
                    return Plugin.NAME;
                }

                @Override
                public HardwareAddress getHardwareAddress() {
                    return Host.this.addrHw;
                }

                @Override
                public Map<String, Collection<Property<?>>> getProperties() {
                    return Host.this.properties;
                }
            });
        }
        return elements;
    }
}
