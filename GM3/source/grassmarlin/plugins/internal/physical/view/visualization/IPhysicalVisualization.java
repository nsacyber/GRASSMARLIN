package grassmarlin.plugins.internal.physical.view.visualization;

import java.util.Collection;

public interface IPhysicalVisualization {
    Collection<IPhysicalElement> getHosts();
    Collection<IPhysicalElement> getPorts();
    Collection<IPhysicalElement> getDevices();
    Collection<VisualWire> getWires();

    Collection<IPhysicalElement> portsOf(final IPhysicalElement device);
}
