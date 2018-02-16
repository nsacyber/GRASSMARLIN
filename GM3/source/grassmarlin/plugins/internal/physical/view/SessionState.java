package grassmarlin.plugins.internal.physical.view;

import grassmarlin.plugins.internal.physical.view.data.PhysicalDevice;
import grassmarlin.plugins.internal.physical.view.data.PhysicalEndpoint;
import grassmarlin.plugins.internal.physical.view.data.intermediary.Segment;
import grassmarlin.plugins.internal.physical.view.data.intermediary.SessionConnectedPhysicalGraph;
import grassmarlin.plugins.internal.physical.view.visualization.ForceDirectedPhysicalVisualization;
import grassmarlin.plugins.internal.physical.view.visualization.PhysicalVisualization;
import grassmarlin.session.Session;
import grassmarlin.ui.common.SessionInterfaceController;
import grassmarlin.ui.common.menu.ActiveMenuItem;
import javafx.beans.property.ReadOnlyStringWrapper;

import java.util.Collections;

public class SessionState {
    private final SessionConnectedPhysicalGraph graph;

    public SessionState(final Plugin plugin, final Session session, final SessionInterfaceController controller) {
        this.graph = new SessionConnectedPhysicalGraph(plugin.getConfig(), session);
        final PhysicalVisualization visualization = new ForceDirectedPhysicalVisualization(plugin, this.graph);

        final ActiveMenuItem miDumpSession = new ActiveMenuItem("Dump Physical Graph to Console", event -> {
            final StringBuilder sbOutput = new StringBuilder();
            sbOutput.append("Physical Graph:").append(SessionState.this.graph.toString()).append("\r\nEndpoints:\r\n");
            for(final PhysicalEndpoint endpoint : SessionState.this.graph.getEndpoints()) {
                sbOutput.append(endpoint.toString()).append("\r\n");
            }
            sbOutput.append("Devices:\r\n");
            for(final PhysicalDevice device : SessionState.this.graph.getDevices()) {
                sbOutput.append(device.toString()).append("\r\n");
            }
            sbOutput.append("Connection Trees:\r\n");
            for(final Segment segment : SessionState.this.graph.getSegments()) {
                sbOutput.append(segment.getConnectionTree().toString()).append("\r\n");
            }

            System.out.println(sbOutput.toString());
        });

        controller.createView(new SessionInterfaceController.View(
            new ReadOnlyStringWrapper("Physical Graph"),
            visualization,
            null, //TODO: Tree elements
            null,
            Collections.singletonList(miDumpSession), //TODO: View Menu
            false,
            false
        ), true);
    }

    public void close() {
        //TODO: Clean up, if necessary
    }
}
