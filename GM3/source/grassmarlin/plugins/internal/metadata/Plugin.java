package grassmarlin.plugins.internal.metadata;

import grassmarlin.RuntimeConfiguration;
import grassmarlin.plugins.IPlugin;
import grassmarlin.session.hardwareaddresses.Mac;
import grassmarlin.session.logicaladdresses.Ipv4;
import grassmarlin.ui.common.dialogs.preferences.PreferenceDialog;
import javafx.scene.control.ButtonType;
import javafx.scene.control.MenuItem;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;

public class Plugin implements IPlugin, IPlugin.DefinesPipelineStages {
    public final static String NAME = "Metadata";
    private static Collection<PipelineStage> stages = new ArrayList<PipelineStage>() {
        {
            this.add(new PipelineStage(true, StageApplyMetadata.NAME, StageApplyMetadata.class, StageApplyMetadata.DEFAULT_OUTPUT, StageApplyMetadata.OUTPUT_MANUFACTURER, StageApplyMetadata.OUTPUT_GEOLOCATION));
        }
    };

    private final RuntimeConfiguration config;

    private static Ipv4GeoIp geoIpv4;
    private static MacManufacturer macs;

    public Plugin(final RuntimeConfiguration config) {
        this.config = config;
    }

    @Override
    public Collection<PipelineStage> getPipelineStages() {
        return grassmarlin.plugins.internal.metadata.Plugin.stages;
    }

    @Override
    public Serializable getConfiguration(PipelineStage stage, Serializable configuration) {
        if(stage.getStage().equals(StageApplyMetadata.class) && configuration instanceof Configuration) {
            final PreferenceDialog<Configuration> dlg = new PreferenceDialog<>(config, (Configuration)configuration);
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
        if(stage.getStage() == StageApplyMetadata.class) {
            return new Configuration();
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

    public static String countryNameFor(final Ipv4 ip) {
        synchronized(Plugin.class){
            if (geoIpv4 == null) {
                geoIpv4 = new Ipv4GeoIp();
            }
        }
        return geoIpv4.match(ip);
    }
    public static String manufacturerFor(final Mac mac) {
        synchronized(Plugin.class) {
            if(macs == null) {
                macs = new MacManufacturer();
            }
        }
        return macs.forMac(mac);
    }
}
