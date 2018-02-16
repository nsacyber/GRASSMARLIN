package iadgov.fingerprint;

import grassmarlin.session.Property;

import java.util.Collection;
import java.util.Map;

public interface IHasFingerprintProperties {

    void addProperty(String name, Property<?> prop);
    void addProperties(String name, Collection<Property<?>> prop);
    void putProperties(Map<String, Collection<Property<?>>> props);
}
