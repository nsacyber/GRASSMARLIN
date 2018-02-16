package grassmarlin.plugins.internal.graph;

import grassmarlin.Logger;
import grassmarlin.RuntimeConfiguration;
import grassmarlin.session.*;
import grassmarlin.session.pipeline.*;

import java.util.Collection;
import java.util.Map;

public class StageAddProperties extends AbstractStage<Session> {
    public static final String NAME = "Add Properties";

    public StageAddProperties(final RuntimeConfiguration config, final Session session) {
        super(config, session, IHasHardwareVertexProperties.class, IHasLogicalVertexProperties.class, IHasLogicalConnectionProperties.class, IHasPhysicalConnectionProperties.class);
    }

    @Override
    public Object process(final Object obj) {
        if(obj instanceof IHasHardwareVertexProperties) {
            final IHasHardwareVertexProperties properties = (IHasHardwareVertexProperties) obj;
            final HardwareVertex hardwareVertex = getContainer().hardwareVertexFor(properties.getHardwareAddress());

            if (hardwareVertex != null) {
                for (Map.Entry<String, Collection<Property<?>>> entry : properties.getProperties().entrySet()) {
                    hardwareVertex.addProperties(properties.getPropertySource(), entry.getKey(), entry.getValue());
                }
            } else {
                Logger.log(Logger.Severity.WARNING, "Unable to set properties for non-existent hardware vertex " + properties.getHardwareAddress());
            }
        }
        if(obj instanceof IHasLogicalVertexProperties) {
            final IHasLogicalVertexProperties properties = (IHasLogicalVertexProperties) obj;
            final LogicalVertex vertex = getContainer().logicalVertexFor(properties.getAddressMapping());

            if (vertex != null) {
                for (Map.Entry<String, Collection<Property<?>>> entry : properties.getProperties().entrySet()) {
                    vertex.addProperties(properties.getPropertySource(), entry.getKey(), entry.getValue());
                }
            } else {
                Logger.log(Logger.Severity.WARNING, "Unable to set properties for non-existent logical vertex " + properties.getAddressMapping());
            }
        }
        if(obj instanceof IHasLogicalConnectionProperties) {
            final IHasLogicalConnectionProperties properties = (IHasLogicalConnectionProperties)obj;
            final LogicalConnection connection = this.getContainer().existingEdgeBetween(properties.getEndpoints());

            if(connection != null) {
                for (Map.Entry<String, Collection<Property<?>>> entry : properties.getProperties().entrySet()) {
                    connection.addProperties(properties.getPropertySource(), entry.getKey(), entry.getValue());
                }
            } else {
                Logger.log(Logger.Severity.WARNING, "Unable to set properties for non-existent edge %s", properties.getEndpoints());
            }
        }
        if(obj instanceof IHasPhysicalConnectionProperties) {
            final IHasPhysicalConnectionProperties properties = (IHasPhysicalConnectionProperties)obj;
            final PhysicalConnection connection = this.getContainer().existingEdgeBetween(properties.getEndpoints());

            if(connection != null) {
                for (Map.Entry<String, Collection<Property<?>>> entry : properties.getProperties().entrySet()) {
                    connection.addProperties(properties.getPropertySource(), entry.getKey(), entry.getValue());
                }
            } else {
                Logger.log(Logger.Severity.WARNING, "Unable to set properties for non-existent edge %s", properties.getEndpoints());
            }
        }

        return obj;
    }

    @Override
    public String getName() {
        return NAME;
    }
}
