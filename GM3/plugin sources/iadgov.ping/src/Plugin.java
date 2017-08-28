package iadgov.ping;

import grassmarlin.RuntimeConfiguration;
import grassmarlin.plugins.IPlugin;
import grassmarlin.plugins.internal.logicalview.ILogicalViewApi;
import grassmarlin.session.Property;
import grassmarlin.session.logicaladdresses.Ipv4;
import grassmarlin.session.pipeline.AbstractStage;
import grassmarlin.ui.common.dialogs.preferences.PreferenceDialog;
import javafx.scene.control.ButtonType;
import javafx.scene.control.MenuItem;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collection;

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
        final ILogicalViewApi api = config.pluginFor("grassmarlin", grassmarlin.Plugin.class);
        api.addVertexContextMenuItem(
                vertex -> vertex.getRootLogicalAddressMapping().getLogicalAddress() instanceof Ipv4,
                "Ping",
                vertex -> {
                    final Thread threadPing = new Thread(() -> {
                            vertex.getVertex().setProperties(Plugin.NAME, Plugin.PROPERTY_REACHABLE, new Property<>(Ping.ping((Ipv4)vertex.getRootLogicalAddressMapping().getLogicalAddress(), 5000), 1));
                    });
                    threadPing.setDaemon(true);
                    threadPing.setName("iadgov.ping: " + vertex.getRootLogicalAddressMapping().getLogicalAddress());
                    threadPing.start();
                });
    }

    @Override
    public Collection<PipelineStage> getPipelineStages() {
        return Arrays.asList(
                new PipelineStage(true, "Ping", StagePing.class, AbstractStage.DEFAULT_OUTPUT)
        );
    }

    @Override
    public Serializable getConfiguration(PipelineStage stage, Serializable configuration) {
        if(stage.getStage().isAssignableFrom(StagePing.class) && configuration instanceof ConfigStage) {
            final PreferenceDialog<ConfigStage> dlg = new PreferenceDialog<>(config, (ConfigStage)configuration);
            if(dlg.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
                return dlg.getPreferences();
            } else {
                return configuration;
            }
        } else {
            return configuration;
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
}
