package grassmarlin.plugins.internal.metadata;

import grassmarlin.RuntimeConfiguration;
import grassmarlin.common.Confidence;
import grassmarlin.session.HardwareAddress;
import grassmarlin.session.Property;
import grassmarlin.session.Session;
import grassmarlin.session.hardwareaddresses.Mac;
import grassmarlin.session.logicaladdresses.Ipv4;
import grassmarlin.session.pipeline.AbstractStage;
import grassmarlin.session.pipeline.IHasHardwareVertexProperties;
import grassmarlin.session.pipeline.IHasLogicalVertexProperties;
import grassmarlin.session.pipeline.LogicalAddressMapping;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;

public class StageApplyMetadata extends AbstractStage<Session> {
    public final static String NAME = "Metadata Lookup";

    public final static String PROPERTY_MANUFACTURER = "Manufacturer";
    public final static String PROPERTY_GEOIP_COUNTRY = "Country";
    public final static Confidence CONFIDENCE = Confidence.MEDIUM;

    public final static String OUTPUT_GEOLOCATION = "Geolocation";
    public final static String OUTPUT_MANUFACTURER = "Manufacturer";

    private Configuration options;

    //Macs are a single entry for each HardwareAddress
    private final HashSet<HardwareAddress> cacheMac;
    private final HashSet<Ipv4> cacheIpv4Geolocation;

    public StageApplyMetadata(final RuntimeConfiguration config, final Session session) {
        // Hardware addresses are used for MAC lookups
        // LogicalAddressMappings are used to identify Ipv4 and Ipv6 addresses for geolocation
        super(config, session, HardwareAddress.class, LogicalAddressMapping.class);

        setPassiveMode(true);
        this.options = new Configuration();

        defineOutput(OUTPUT_GEOLOCATION, Geolocation.class);
        defineOutput(OUTPUT_MANUFACTURER, Manufacturer.class);

        cacheMac = new HashSet<>();
        cacheIpv4Geolocation = new HashSet<>();
    }

    @Override
    public void setConfiguration(final Serializable configuration) {
        if(configuration instanceof Configuration) {
            this.options = (Configuration)configuration;
        }
    }

    protected static class Manufacturer implements IHasHardwareVertexProperties {
        private final HardwareAddress address;
        private final Property<String> manufacturer;

        public Manufacturer(final HardwareAddress address, final Property<String> manufacturer) {
            this.address = address;
            this.manufacturer = manufacturer;
        }

        @Override
        public String getPropertySource() {
            return Plugin.NAME;
        }

        @Override
        public HardwareAddress getHardwareAddress() {
            return address;
        }

        @Override
        public Map<String, Collection<Property<?>>> getProperties() {
            return Collections.singletonMap(PROPERTY_MANUFACTURER, Collections.singleton(this.manufacturer));
        }
    }

    protected static class Geolocation implements IHasLogicalVertexProperties {
        private final LogicalAddressMapping address;
        private final Property<String> country;

        public Geolocation(final LogicalAddressMapping address, final Property<String> country) {
            this.address = address;
            this.country = country;
        }

        @Override
        public String getPropertySource() {
            return Plugin.NAME;
        }

        @Override
        public LogicalAddressMapping getAddressMapping() {
            return address;
        }

        @Override
        public Map<String, Collection<Property<?>>> getProperties() {
            return Collections.singletonMap(PROPERTY_GEOIP_COUNTRY, Collections.singleton(this.country));
        }

        @Override
        public String toString() {
            return String.format("GeoIp(%s belongs to %s)", this.address, this.country.getValue());
        }
    }

    @Override
    public Object process(Object o) {
        if(o instanceof Mac) {
            if (this.options.getOuiLookupEnabled()) {
                if (!cacheMac.contains(o)) {
                    final String nameManufacturer = Plugin.manufacturerFor((Mac) o);
                    if (nameManufacturer != null) {
                        final Property<String> manufacturer = new Property<>(nameManufacturer, CONFIDENCE);
                        cacheMac.add((HardwareAddress) o);
                        return new Manufacturer((HardwareAddress) o, manufacturer);
                    }
                }
            }
        } else if(o instanceof LogicalAddressMapping) {
            final LogicalAddressMapping mapping = (LogicalAddressMapping) o;
            if (mapping.getLogicalAddress() instanceof Ipv4) {
                final Ipv4 ip = (Ipv4) mapping.getLogicalAddress();
                if (this.options.getIpv4GeolocationEnabled()) {
                    if (!cacheIpv4Geolocation.contains(ip)) {
                        final String nameCountry = Plugin.countryNameFor(ip);
                        cacheIpv4Geolocation.add(ip);
                        if (nameCountry != null) {
                            final Property<String> country = new Property<>(nameCountry, CONFIDENCE);
                            return new Geolocation(mapping, country);
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
