package iadgov.logicalgraph.groupedports;

import grassmarlin.RuntimeConfiguration;
import grassmarlin.plugins.IPlugin;
import grassmarlin.plugins.internal.logicalview.ILogicalViewApi;
import grassmarlin.session.LogicalVertex;
import grassmarlin.session.logicaladdresses.RouterLogicalAddress;
import grassmarlin.session.pipeline.LogicalAddressMapping;
import javafx.scene.control.MenuItem;
import javafx.scene.image.Image;

import java.util.Collection;

public class Plugin implements IPlugin {
    public final static String NAME = "Grouped Ports";
    private final static Image IMAGE_PLUGIN = new Image(Plugin.class.getClassLoader().getResourceAsStream("plugin.png"));

    public Plugin(final RuntimeConfiguration config) {
        final ILogicalViewApi api = config.pluginFor("Grassmarlin", grassmarlin.Plugin.class);

        api.addVertexContextMenuItem(
                (graphLogicalVertex, logicalVertex) -> !(graphLogicalVertex.getRootLogicalAddressMapping().getLogicalAddress() instanceof RouterLogicalAddress),
                "Convert to Router Port",
                (session, graphLogicalVertex, logicalVertex) -> {
                    final RouterLogicalAddress address = new RouterLogicalAddress();
                    final LogicalAddressMapping mappingNew = new LogicalAddressMapping(graphLogicalVertex.getVertex().getHardwareVertex().getAddress(), address);

                    final LogicalVertex vertexLogical = graphLogicalVertex.getVertex().getSession().logicalVertexFor(mappingNew);
                    //TODO: Set properties here?
                });
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public Collection<MenuItem> getMenuItems() {
        return null;
    }

    @Override
    public Image getImageForSize(int pixels) {
        return IMAGE_PLUGIN;
    }
}
