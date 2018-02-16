package grassmarlin.plugins.internal.physical.view.data;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;

/**
 * A PhysicalCloud represents a block of interconnected device.
 *
 * Within a PhyscialGraph, every PhysicalEndpoint should belong to at most one PhysicalCloud.
 */
public class PhysicalCloud {
    private final HashSet<PhysicalEndpoint> members;
    private final PhysicalEndpoint endpointInternal;

    public PhysicalCloud(final Collection<PhysicalEndpoint> endpoints) {
        this.members = new HashSet<>(endpoints);

        this.endpointInternal = new PhysicalEndpoint(null);
    }
    public PhysicalCloud(final PhysicalEndpoint... endpoints) {
        this(Arrays.asList(endpoints));
    }

    public Collection<PhysicalEndpoint> getEndpoints() {
        return this.members;
    }
    public PhysicalEndpoint getInternalEndpoint() {
        return this.endpointInternal;
    }
}
