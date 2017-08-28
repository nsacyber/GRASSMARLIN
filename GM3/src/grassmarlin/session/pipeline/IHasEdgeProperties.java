package grassmarlin.session.pipeline;

import grassmarlin.session.Property;
import grassmarlin.session.Session;

import java.util.Collection;
import java.util.Map;

public interface IHasEdgeProperties {
    String getPropertySource();
    Session.AddressPair getEndpoints();
    Map<String, Collection<Property<?>>> getProperties();
}
