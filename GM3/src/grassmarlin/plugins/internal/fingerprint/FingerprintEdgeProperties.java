package grassmarlin.plugins.internal.fingerprint;

import grassmarlin.session.Property;
import grassmarlin.session.Session;
import grassmarlin.session.pipeline.IHasEdgeProperties;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class FingerprintEdgeProperties implements IHasEdgeProperties {
    private final String source;
    private final Session.AddressPair endpoints;
    private Map<String, Collection<Property<?>>> properties;

    public FingerprintEdgeProperties(String source, Session.AddressPair endpoints) {
        this.source = source;
        this.endpoints = endpoints;

        this.properties = new HashMap<>();
    }

    public void addProperty(String propName, Property<?> value) {
        Collection<Property<?>> props = this.properties.get(propName);
        if (props == null) {
            props = new ArrayList<>();
            this.properties.put(propName, props);
        }

        props.add(value);
    }


    @Override
    public String getPropertySource() {
        return source;
    }

    @Override
    public Session.AddressPair getEndpoints() {
        return endpoints;
    }

    @Override
    public Map<String, Collection<Property<?>>> getProperties() {
        return properties;
    }
}
