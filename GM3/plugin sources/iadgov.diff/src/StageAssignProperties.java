package iadgov.diff;

import grassmarlin.RuntimeConfiguration;
import grassmarlin.session.HardwareAddress;
import grassmarlin.session.Property;
import grassmarlin.session.Session;
import grassmarlin.session.pipeline.*;
import grassmarlin.ui.common.dialogs.preferences.PreferenceDialog;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class StageAssignProperties extends AbstractStage<Session> {
    public final static String NAME = "Assign Properties";
    public final static String OUTPUT_GENERATED_PROPERTIES = "Generated Properties";

    public static class EdgeProperties implements IHasEdgeProperties {
        private final Session.AddressPair endpoints;
        private final Map<String, Collection<Property<?>>> properties;

        public EdgeProperties(final Session.AddressPair endpoints, final String property, final Serializable value) {
            this.endpoints = endpoints;

            this.properties = new HashMap<>();
            this.properties.put(property, Arrays.asList(new Property<>(value, 0)));
        }

        @Override
        public String getPropertySource() {
            return NAME;
        }
        @Override
        public Session.AddressPair getEndpoints() {
            return this.endpoints;
        }
        @Override
        public Map<String, Collection<Property<?>>> getProperties() {
            return this.properties;
        }
    }
    public static class HardwareAddressProperties implements IHasHardwareVertexProperties {
        private final HardwareAddress address;
        private final Map<String, Collection<Property<?>>> properties;

        public HardwareAddressProperties(final HardwareAddress address, final String property, final Serializable value) {
            this.address = address;

            this.properties = new HashMap<>();
            this.properties.put(property, Arrays.asList(new Property<>(value, 0)));
        }
        @Override
        public String getPropertySource() {
            return NAME;
        }
        @Override
        public HardwareAddress getHardwareAddress() {
            return this.address;
        }
        @Override
        public Map<String, Collection<Property<?>>> getProperties() {
            return this.properties;
        }
    }
    public static class LogicalAddressMappingProperties implements IHasLogicalVertexProperties {
        private final LogicalAddressMapping address;
        private final Map<String, Collection<Property<?>>> properties;

        public LogicalAddressMappingProperties(final LogicalAddressMapping address, final String property, final Serializable value) {
            this.address = address;

            this.properties = new HashMap<>();
            this.properties.put(property, Arrays.asList(new Property<>(value, 0)));
        }
        @Override
        public String getPropertySource() {
            return NAME;
        }
        @Override
        public LogicalAddressMapping getAddressMapping() {
            return this.address;
        }
        @Override
        public Map<String, Collection<Property<?>>> getProperties() {
            return this.properties;
        }
    }

    public static class Configuration implements Serializable, Cloneable {
        @PreferenceDialog.Field(name="Apply to Edges", accessorName="TagEdges", nullable = false)
        private Boolean tagEdges;
        @PreferenceDialog.Field(name="Apply to Hardware Addresses", accessorName="TagHardwareAddresses", nullable = false)
        private Boolean tagHardwareAddresses;
        @PreferenceDialog.Field(name="Apply to Logical Address Mappings", accessorName="TagLogicalAddressMappings", nullable = false)
        private Boolean tagLogicalAddressMappings;
        @PreferenceDialog.Field(name="Property Name", accessorName="Property", nullable = false)
        private String property;
        @PreferenceDialog.Field(name="Property Value", accessorName="Value", nullable = true)
        private Serializable value;

        public Configuration() {
            this.tagEdges = true;
            this.tagHardwareAddresses = true;
            this.tagLogicalAddressMappings = true;
            this.property = null;
            this.value = null;
        }

        @Override
        public Object clone() throws CloneNotSupportedException {
            return super.clone();
        }

        public Boolean getTagEdges() {
            return tagEdges;
        }

        public void setTagEdges(Boolean tagEdges) {
            this.tagEdges = tagEdges;
        }

        public Boolean getTagHardwareAddresses() {
            return tagHardwareAddresses;
        }

        public void setTagHardwareAddresses(Boolean tagHardwareAddresses) {
            this.tagHardwareAddresses = tagHardwareAddresses;
        }

        public Boolean getTagLogicalAddressMappings() {
            return tagLogicalAddressMappings;
        }

        public void setTagLogicalAddressMappings(Boolean tagLogicalAddressMappings) {
            this.tagLogicalAddressMappings = tagLogicalAddressMappings;
        }

        public String getProperty() {
            return property;
        }

        public void setProperty(String property) {
            this.property = property;
        }

        public Serializable getValue() {
            return value;
        }

        public void setValue(Serializable value) {
            this.value = value;
        }
    }

    private Configuration options;

    public StageAssignProperties(final RuntimeConfiguration config, final Session session) {
        super(config, session, IPacketMetadata.class, Session.AddressPair.class, HardwareAddress.class, LogicalAddressMapping.class);

        this.options = new Configuration();

        this.defineOutput(OUTPUT_GENERATED_PROPERTIES, EdgeProperties.class, HardwareAddressProperties.class, LogicalAddressMappingProperties.class);
        this.disallowOutputClasses(AbstractStage.DEFAULT_OUTPUT, EdgeProperties.class, HardwareAddressProperties.class, LogicalAddressMappingProperties.class);

        this.setPassiveMode(true);
    }


    @Override
    public void setConfiguration(final Serializable configuration) {
        if(configuration instanceof Configuration) {
            this.options = (Configuration)configuration;
        }
    }

    public Configuration getConfiguration() {
        return this.options;
    }

    @Override
    public Object process(final Object o) {
        if(o instanceof Session.AddressPair) {
            if (this.options.tagEdges) {
                return new EdgeProperties((Session.AddressPair) o, this.options.property, this.options.value);
            }
        } else if(o instanceof IPacketMetadata) {
            //HACK: These should be reported as AddressPairs as well
            if(this.options.tagEdges) {
                return new EdgeProperties(new Session.AddressPair(((IPacketMetadata)o).getSourceAddress(), ((IPacketMetadata)o).getDestAddress()), this.options.property, this.options.value);
            }
        } else if(o instanceof LogicalAddressMapping) {
            if(this.options.tagLogicalAddressMappings) {
                return new LogicalAddressMappingProperties((LogicalAddressMapping)o, this.options.property, this.options.value);
            }
        } else if(o instanceof HardwareAddress) {
            if(this.options.tagHardwareAddresses) {
                return new HardwareAddressProperties((HardwareAddress)o, this.options.property, this.options.value);
            }
        }

        return o;
    }

    @Override
    public String getName() {
        return NAME;
    }
}
