package iadgov.ping;

import grassmarlin.RuntimeConfiguration;
import grassmarlin.session.Property;
import grassmarlin.session.Session;
import grassmarlin.session.logicaladdresses.Ipv4;
import grassmarlin.session.pipeline.AbstractStage;
import grassmarlin.session.pipeline.IHasLogicalVertexProperties;
import grassmarlin.session.pipeline.LogicalAddressMapping;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class StagePing extends AbstractStage<Session> {
    public final static String NAME = "Ping";

    private ConfigStage options = new ConfigStage();
    private final Map<Ipv4, Long> debounceThresholds = new HashMap<>();

    public StagePing(final RuntimeConfiguration config, final Session session) {
        super(config, session, LogicalAddressMapping.class);

        this.setPassiveMode(true);
    }

    @Override
    public void setConfiguration(final Serializable configuration) {
        if(configuration instanceof ConfigStage) {
            this.options = (ConfigStage)configuration;
        }
    }

    @Override
    public Object process(final Object obj) {
        if(obj instanceof LogicalAddressMapping) {
            final LogicalAddressMapping mapping = (LogicalAddressMapping)obj;
            if(mapping.getLogicalAddress() instanceof Ipv4) {
                final Ipv4 ip = (Ipv4)mapping.getLogicalAddress();
                final long now = System.currentTimeMillis();
                if(debounceThresholds.get(ip) == null || debounceThresholds.get(ip) < now) {
                    debounceThresholds.put(ip, now + options.getPingDebounceMins() * 60000L);
                    boolean result = Ping.ping(ip, options.getPingTimeoutMs());
                    return new IHasLogicalVertexProperties() {
                        @Override
                        public String getPropertySource() {
                            return Plugin.NAME;
                        }

                        @Override
                        public LogicalAddressMapping getAddressMapping() {
                            return mapping;
                        }

                        @Override
                        public Map<String, Collection<Property<?>>> getProperties() {
                            final Map<String, Collection<Property<?>>> map = new HashMap<>();
                            map.put(Plugin.PROPERTY_REACHABLE, new ArrayList<>());
                            map.get(Plugin.PROPERTY_REACHABLE).add(new Property<>(result, 1));
                            return map;
                        }

                    };
                } else {
                    return null;
                }
            }
        }
        return null;
    }

    @Override
    public String getName() {
        return NAME;
    }
}
