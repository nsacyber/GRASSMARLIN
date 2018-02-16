package grassmarlin.session.pipeline;

import grassmarlin.session.Property;

import java.util.Collection;
import java.util.Map;

interface IHasEdgeProperties<T> {
    String getPropertySource();
    T getEndpoints();
    Map<String, Collection<Property<?>>> getProperties();
}
