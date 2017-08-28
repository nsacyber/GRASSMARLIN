package grassmarlin.plugins.internal.metadata;

import grassmarlin.RuntimeConfiguration;
import grassmarlin.plugins.internal.AggregatePlugin;
import grassmarlin.session.HardwareAddress;
import grassmarlin.session.Property;
import grassmarlin.session.Session;
import grassmarlin.session.hardwareaddresses.Mac;
import grassmarlin.session.logicaladdresses.Ipv4;
import grassmarlin.session.pipeline.AbstractStage;
import grassmarlin.session.pipeline.IHasHardwareVertexProperties;
import grassmarlin.session.pipeline.IHasLogicalVertexProperties;
import grassmarlin.session.pipeline.LogicalAddressMapping;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class StageApplyMetadata extends AbstractStage<Session> {
    public final static String NAME = "Metadata Lookup";

    public final static String SOURCE_PROPERTIES = "StageApplyMetadata";

    public final static String PROPERTY_MANUFACTURER = "Manufacturer";
    public final static String PROPERTY_GEOIP_COUNTRY = "Country";
    public final static int CONFIDENCE = 3;

    public final static String OUTPUT_GEOLOCATION = "Geolocation";
    public final static String OUTPUT_MANUFACTURER = "Manufacturer";

    //The AbstractStage class tracks the plugin that created this, but since this goes through an AggregatePlugin, we need to identify it separately.
    protected final Plugin pluginForConfiguration;


    //Macs are a single entry for each HardwareAddress
    private final HashMap<HardwareAddress, Property<String>> cacheMac;
    private final HashMap<Ipv4, Property<String>> cacheIpv4Geolocation;

    public StageApplyMetadata(final RuntimeConfiguration config, final Session session) {
        // Hardware addresses are used for MAC lookups
        // LogicalAddressMappings are used to identify Ipv4 and Ipv6 addresses for geolocation
        super(config, session, HardwareAddress.class, LogicalAddressMapping.class);

        setPassiveMode(true);

        if(super.plugin instanceof AggregatePlugin) {
            this.pluginForConfiguration = ((AggregatePlugin)super.plugin).getMember(Plugin.class);
        } else {
            //Not loaded via aggregate.  Assume the plugin is the correct type.
            this.pluginForConfiguration = (Plugin)super.plugin;
        }

        defineOutput(OUTPUT_GEOLOCATION, Geolocation.class);
        defineOutput(OUTPUT_MANUFACTURER, Manufacturer.class);

        cacheMac = new HashMap<>();
        cacheIpv4Geolocation = new HashMap<>();
    }

    protected class Manufacturer implements IHasHardwareVertexProperties {
        private final HardwareAddress address;
        private final Property<String> manufacturer;

        public Manufacturer(final HardwareAddress address, final Property<String> manufacturer) {
            this.address = address;
            this.manufacturer = manufacturer;
        }

        @Override
        public String getPropertySource() {
            return SOURCE_PROPERTIES;
        }

        @Override
        public HardwareAddress getHardwareAddress() {
            return address;
        }

        @Override
        public Map<String, Collection<Property<?>>> getProperties() {
            return new HashMap<String, Collection<Property<?>>>() {
                {
                    this.put(PROPERTY_MANUFACTURER, Arrays.asList(Manufacturer.this.manufacturer));
                }
            };
        }
    }

    protected class Geolocation implements IHasLogicalVertexProperties {
        private final LogicalAddressMapping address;
        private final Property<String> country;

        public Geolocation(final LogicalAddressMapping address, final Property<String> country) {
            this.address = address;
            this.country = country;
        }

        @Override
        public String getPropertySource() {
            return SOURCE_PROPERTIES;
        }

        @Override
        public LogicalAddressMapping getAddressMapping() {
            return address;
        }

        @Override
        public Map<String, Collection<Property<?>>> getProperties() {
            return new HashMap<String, Collection<Property<?>>>() {
                {
                    this.put(PROPERTY_GEOIP_COUNTRY, Arrays.asList(Geolocation.this.country));
                }
            };
        }

        @Override
        public String toString() {
            return String.format("GeoIp(%s belongs to %s)", this.address, this.country.getValue());
        }
    }

    @Override
    public Object process(Object o) {
        if(o instanceof Mac) {
            if (pluginForConfiguration.isMacLookupEnabled()) {
                if (!cacheMac.containsKey(o)) {
                    final String nameManufacturer = pluginForConfiguration.manufacturerFor((Mac) o);
                    if (nameManufacturer != null) {
                        final Property<String> manufacturer = new Property<>(nameManufacturer, CONFIDENCE);
                        cacheMac.put((HardwareAddress) o, manufacturer);
                        return new Manufacturer((HardwareAddress) o, manufacturer);
                    }
                }
            }
        } else if(o instanceof LogicalAddressMapping) {
            final LogicalAddressMapping mapping = (LogicalAddressMapping) o;
            if (mapping.getLogicalAddress() instanceof Ipv4) {
                final Ipv4 ip = (Ipv4) mapping.getLogicalAddress();
                if (pluginForConfiguration.isIpv4GeoIpEnabled()) {
                    if (!cacheIpv4Geolocation.containsKey(ip)) {
                        final String nameCountry = pluginForConfiguration.countryNameFor(ip);
                        if (nameCountry != null) {
                            final Property<String> country = new Property<>(nameCountry, CONFIDENCE);
                            cacheIpv4Geolocation.put(ip, country);
                            return new Geolocation(mapping, country);
                        } else {
                            cacheIpv4Geolocation.put(ip, null);
                        }
                    }
                }
            }
            //TODO: Ipv6 Geolocation
        }
        return null;
    }

    @Override
    public String getName() {
        return NAME;
    }
}
