package iadgov.fingerprint;

import grassmarlin.session.Property;
import grassmarlin.session.pipeline.IHasLogicalVertexProperties;
import grassmarlin.session.pipeline.LogicalAddressMapping;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class FingerprintVertexProperties implements IHasLogicalVertexProperties, IHasFingerprintProperties {
    private final String source;
    private final LogicalAddressMapping address;
    private Map<String, Collection<Property<?>>> properties;

    public FingerprintVertexProperties(String source, LogicalAddressMapping address) {
        this.source = source;
        this.address = address;

        this.properties = new HashMap<>();
    }

    @Override
    public void addProperty(String propName, Property<?> value) {
        Collection<Property<?>> props = this.properties.get(propName);
        if (props == null) {
            props = new ArrayList<>();
            this.properties.put(propName, props);
        }

        props.add(value);
    }

    @Override
    public void addProperties(String propName, Collection<Property<?>> values) {
        Collection<Property<?>> props = this.properties.get(propName);
        if (props == null) {
            props = new ArrayList<>();
            this.properties.put(propName, props);
        }

        props.addAll(values);
    }

    @Override
    public void putProperties(Map<String, Collection<Property<?>>> props) {
        this.properties.putAll(props);
    }

    @Override
    public String getPropertySource() {
        return this.source;
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
