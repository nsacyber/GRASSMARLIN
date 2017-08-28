package grassmarlin.plugins.internal.graph;

import grassmarlin.RuntimeConfiguration;
import grassmarlin.session.*;
import grassmarlin.session.pipeline.AbstractStage;
import grassmarlin.session.pipeline.IHasEdgeProperties;
import grassmarlin.session.pipeline.IHasHardwareVertexProperties;
import grassmarlin.session.pipeline.IHasLogicalVertexProperties;

import java.util.Collection;
import java.util.Map;

public class StageSetProperties extends AbstractStage<Session> {
    public static final String NAME = "Set Properties";

    public StageSetProperties(final RuntimeConfiguration config, final Session session) {
        super(config, session, IHasHardwareVertexProperties.class, IHasLogicalVertexProperties.class, IHasEdgeProperties.class);
    }

    @Override
    public Object process(final Object obj) {
        if(obj instanceof IHasHardwareVertexProperties) {
            final IHasHardwareVertexProperties properties = (IHasHardwareVertexProperties) obj;
            final HardwareVertex hardwareVertex = getContainer().hardwareVertexFor(properties.getHardwareAddress());

            for (Map.Entry<String, Collection<Property<?>>> entry : properties.getProperties().entrySet()) {
                hardwareVertex.addProperties(properties.getPropertySource(), entry.getKey(), entry.getValue());
            }
        }
        if(obj instanceof IHasLogicalVertexProperties) {
            final IHasLogicalVertexProperties properties = (IHasLogicalVertexProperties) obj;
            final LogicalVertex vertex = getContainer().logicalVertexFor(properties.getAddressMapping());

            for (Map.Entry<String, Collection<Property<?>>> entry : properties.getProperties().entrySet()) {
                vertex.addProperties(properties.getPropertySource(), entry.getKey(), entry.getValue());
            }
        }
        if(obj instanceof IHasEdgeProperties) {
            final IHasEdgeProperties properties = (IHasEdgeProperties)obj;
            final Edge<?> edge = getContainer().existingEdgeBetween(properties.getEndpoints());

            for(Map.Entry<String, Collection<Property<?>>> entry : properties.getProperties().entrySet()) {
                edge.addProperties(properties.getPropertySource(), entry.getKey(), entry.getValue());
            }
        }

        return obj;
    }

    @Override
    public String getName() {
        return NAME;
    }
}
