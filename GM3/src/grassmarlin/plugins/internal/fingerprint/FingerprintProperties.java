package grassmarlin.plugins.internal.fingerprint;

import grassmarlin.session.Property;
import grassmarlin.session.pipeline.IHasLogicalVertexProperties;
import grassmarlin.session.pipeline.LogicalAddressMapping;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class FingerprintProperties implements IHasLogicalVertexProperties{
    private final String source;
    private final LogicalAddressMapping address;
    private Map<String, Collection<Property<?>>> properties;

    public FingerprintProperties(String source, LogicalAddressMapping address) {
        this.source = source;
        this.address = address;

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
