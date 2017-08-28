package iadgov.logicalgraph.manualproperties;

import grassmarlin.RuntimeConfiguration;
import grassmarlin.plugins.IPlugin;
import grassmarlin.plugins.internal.logicalview.ILogicalViewApi;
import javafx.scene.control.MenuItem;

import java.util.Collection;

@IPlugin.Uses("grassmarlin")
public class Plugin implements IPlugin {
    public static final String NAME = "Manual Logical Graph Properties";

    public Plugin(final RuntimeConfiguration config) {
        final ILogicalViewApi gm = config.pluginFor("grassmarlin", grassmarlin.Plugin.class);

        gm.addVertexContextMenuItem(graphLogicalVertex -> true, "Edit Properties...", graphLogicalVertex -> {
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
}
