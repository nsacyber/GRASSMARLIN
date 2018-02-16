package grassmarlin.session.pipeline;

import grassmarlin.session.HardwareAddress;
import grassmarlin.session.Property;

import java.util.Collection;
import java.util.Map;

public interface IHasHardwareVertexProperties {
    String getPropertySource();
    HardwareAddress getHardwareAddress();
    Map<String, Collection<Property<?>>> getProperties();
}
