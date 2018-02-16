package grassmarlin.session.pipeline;

import grassmarlin.session.Property;

import java.util.Collection;
import java.util.Map;

public interface IHasLogicalVertexProperties {
    String getPropertySource();
    LogicalAddressMapping getAddressMapping();
    Map<String, Collection<Property<?>>> getProperties();
}
