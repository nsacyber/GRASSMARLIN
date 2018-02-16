package iadgov.siemens;

import grassmarlin.RuntimeConfiguration;
import grassmarlin.plugins.IPlugin;
import grassmarlin.plugins.internal.logicalview.ILogicalViewApi;
import javafx.scene.control.MenuItem;
import javafx.scene.image.Image;

import java.util.Collection;

@IPlugin.Uses("grassmarlin")
@IPlugin.Active
public class Plugin implements IPlugin {
    public final static String NAME = "Siemens";
    public final static String PROPERTY_CAN_COMMUNICATE = "S7Server Status";
    public final static String PROPERTY_FIRMWARE_STATE = "Firmware State";
    public final static String PROPERTY_FIRMWARE_HASH_CURRENT = "Firmware Hash (Current)";
    public final static String PROPERTY_FIRMWARE_HASH_BASELINE = "Firmware Hash (Baseline)";

    public final static String STATE_UNQUERYABLE = "Cannot Query";
    public final static String STATE_BASELINE = "Baseline";
    public final static String STATE_VERIFIED = "Current Matches Baseline";
    public final static String STATE_CONFLICTED = "Current Does NOT Match Baseline";

    public Plugin(final RuntimeConfiguration config) {
        final ILogicalViewApi api = config.pluginFor("Grassmarlin", grassmarlin.Plugin.class);
        api.addVertexContextMenuItem(
                S7Comm::canBeQueried,
                "Query Firmware",
                (session, graphLogicalVertex, logicalVertex) -> {
                    final Thread threadQuery = new Thread(() -> {
                        S7Comm.queryVertex(graphLogicalVertex, logicalVertex);
                    });
                    threadQuery.setDaemon(true);
                    threadQuery.setName("iadgov.siemens: " + logicalVertex.getLogicalAddress());
                    threadQuery.start();
                });
        api.addMappedImage(PROPERTY_FIRMWARE_STATE, STATE_UNQUERYABLE, new Image(Plugin.class.getClassLoader().getResourceAsStream("Unknown.png")));
        api.addMappedImage(PROPERTY_FIRMWARE_STATE, STATE_VERIFIED, new Image(Plugin.class.getClassLoader().getResourceAsStream("Verified.png")));
        api.addMappedImage(PROPERTY_FIRMWARE_STATE, STATE_CONFLICTED, new Image(Plugin.class.getClassLoader().getResourceAsStream("Conflicted.png")));
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
