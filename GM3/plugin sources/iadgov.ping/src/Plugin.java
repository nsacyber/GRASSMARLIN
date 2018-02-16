package iadgov.ping;

import grassmarlin.RuntimeConfiguration;
import grassmarlin.common.Confidence;
import grassmarlin.plugins.IPlugin;
import grassmarlin.plugins.internal.logicalview.ILogicalViewApi;
import grassmarlin.session.Property;
import grassmarlin.session.logicaladdresses.Ipv4;
import grassmarlin.session.pipeline.AbstractStage;
import grassmarlin.ui.common.dialogs.preferences.PreferenceDialog;
import javafx.scene.control.ButtonType;
import javafx.scene.control.MenuItem;
import javafx.scene.image.Image;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;

/**
 * This is an ACTIVE plugin that adds the ability to ICMP ping an IPv4 host from the Logical Graph.  Response information is added as properties.
 * The Inet4Address.isReachable() implementation is supposed to attempt a ping, with fallback operations available, however that is an implementation detail and is not guaranteed.
 */
@IPlugin.Uses("grassmarlin")
@IPlugin.Active
public class Plugin implements IPlugin, IPlugin.DefinesPipelineStages {
    public static final String NAME = "Ping";
    public static final String PROPERTY_REACHABLE = "Reachable";

    private final RuntimeConfiguration config;

    public Plugin(final RuntimeConfiguration config) {
        this.config = config;
        final ILogicalViewApi api = config.pluginFor("Grassmarlin", grassmarlin.Plugin.class);
        api.addVertexContextMenuItem(
                (graphLogicalVertex, logicalVertex) -> logicalVertex.getLogicalAddress() instanceof Ipv4,
                "Ping",
                (session, graphLogicalVertex, logicalVertex) -> {
                    final Thread threadPing = new Thread(() -> {
                            logicalVertex.setProperties(Plugin.NAME, Plugin.PROPERTY_REACHABLE, new Property<>(Ping.ping((Ipv4)logicalVertex.getLogicalAddress(), 5000), Confidence.HIGH));
                    });
                    threadPing.setDaemon(true);
                    threadPing.setName("iadgov.ping: " + logicalVertex.getLogicalAddress());
                    threadPing.start();
                });
        api.addMappedImage("Reachable", "true", new Image(Plugin.class.getClassLoader().getResourceAsStream("true.png")));
        api.addMappedImage("Reachable", "false", new Image(Plugin.class.getClassLoader().getResourceAsStream("false.png")));
    }

    @Override
    public Collection<PipelineStage> getPipelineStages() {
        return Collections.singleton(
                new PipelineStage(true, "Ping", StagePing.class, AbstractStage.DEFAULT_OUTPUT)
        );
    }

    @Override
    public Serializable getConfiguration(PipelineStage stage, Serializable configuration) {
        if(stage.getStage().equals(StagePing.class) && configuration instanceof ConfigStage) {
            final PreferenceDialog<ConfigStage> dlg = new PreferenceDialog<>(config, (ConfigStage)configuration);
            if(dlg.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
                return dlg.getPreferences();
            } else {
                return configuration;
            }
        } else {
            return null;
        }
    }

    @Override
    public Serializable getDefaultConfiguration(PipelineStage stage) {
        if(stage.getStage() == StagePing.class) {
            return new ConfigStage();
        } else {
            return null;
        }
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
