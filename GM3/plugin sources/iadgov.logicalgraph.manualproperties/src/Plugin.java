package iadgov.logicalgraph.manualproperties;

import grassmarlin.RuntimeConfiguration;
import grassmarlin.plugins.IPlugin;
import grassmarlin.plugins.internal.logicalview.ILogicalViewApi;
import javafx.scene.control.MenuItem;
import javafx.scene.image.Image;

import java.util.Collection;

@IPlugin.Uses("grassmarlin")
public class Plugin implements IPlugin {
    public static final String NAME = "Manual Logical Graph Properties";

    public Plugin(final RuntimeConfiguration config) {
        final ILogicalViewApi gm = config.pluginFor("Grassmarlin", grassmarlin.Plugin.class);

        gm.addVertexContextMenuItem((graphLogicalVertex, logicalVertex) -> true, "Edit Properties...", (session, graphLogicalVertex, logicalVertex) -> {
            new DialogManageProperties(graphLogicalVertex).showAndWait();
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

    private final Image iconLarge = new Image(Plugin.class.getClassLoader().getResourceAsStream("plugin.png"));
    @Override
    public Image getImageForSize(final int pixels) {
        return iconLarge;
    }
}
