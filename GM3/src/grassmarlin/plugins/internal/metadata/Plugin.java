package grassmarlin.plugins.internal.metadata;

import grassmarlin.RuntimeConfiguration;
import grassmarlin.plugins.IPlugin;
import grassmarlin.session.hardwareaddresses.Mac;
import grassmarlin.session.logicaladdresses.Ipv4;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

public class Plugin implements IPlugin, IPlugin.DefinesPipelineStages, IPlugin.HasClassFactory {
    private static Collection<PipelineStage> stages = new ArrayList<PipelineStage>() {
        {
            this.add(new PipelineStage(false, StageApplyMetadata.NAME, StageApplyMetadata.class, StageApplyMetadata.DEFAULT_OUTPUT, StageApplyMetadata.OUTPUT_MANUFACTURER, StageApplyMetadata.OUTPUT_GEOLOCATION));
        }
    };
    private static Collection<IPlugin.HasClassFactory.ClassFactory<?>> factories = new ArrayList<IPlugin.HasClassFactory.ClassFactory<?>>() {
        {
            this.add(new ClassFactory<String>() {
                public String getFactoryName() {
                    return "Text";
                }
                public Class<String> getFactoryClass() {
                    return String.class;
                }
                public String createInstance(final String text) {
                    return text;
                }
                public boolean validateText(final String text) {
                    return true;
                }
            });
            this.add(new ClassFactory<Double>() {
                public String getFactoryName() {
                    return "Double-Precision Number";
                }
                public Class<Double> getFactoryClass() {
                    return Double.class;
                }
                public Double createInstance(final String text) {
                    return new Double(text);
                }
                public boolean validateText(final String text) {
                    try {
                        Double.parseDouble(text);
                        return true;
                    } catch(NumberFormatException ex) {
                        return false;
                    }
                }
            });
            this.add(new ClassFactory<Long>() {
                public String getFactoryName() {
                    return "64-bit Signed Integer";
                }
                public Class<Long> getFactoryClass() {
                    return Long.class;
                }
                public Long createInstance(final String text) {
                    return new Long(text);
                }
                public boolean validateText(final String text) {
                    try {
                        Long.parseLong(text);
                        return true;
                    } catch(NumberFormatException ex) {
                        return false;
                    }
                }
            });
        }
    };

    private final Menu menuPlugin;
    private final CheckMenuItem miGeoIpv4;
    private final CheckMenuItem miGeoIpv6;
    private final CheckMenuItem miMacManufacturer;

    private final Ipv4GeoIp geoIpv4;
    private final MacManufacturer macs;

    public boolean isIpv4GeoIpEnabled() {
        return this.miGeoIpv4.isSelected();
    }
    public boolean isIpv6GeoIpEnabled() {
        return this.miGeoIpv6.isSelected();
    }
    public boolean isMacLookupEnabled() {
        return this.miMacManufacturer.isSelected();
    }

    public Plugin(final RuntimeConfiguration config) {
        this.menuPlugin = new Menu("Metadata");
        this.miGeoIpv4 = new CheckMenuItem("Ipv4 Geolocation");
        this.miGeoIpv4.setSelected(true);
        this.miGeoIpv6 = new CheckMenuItem("Ipv6 Geolocation");
        this.miGeoIpv6.setSelected(true);
        this.miMacManufacturer = new CheckMenuItem("MAC Manufacturer");
        this.miMacManufacturer.setSelected(true);
        this.menuPlugin.getItems().addAll(this.miGeoIpv4, this.miGeoIpv6, this.miMacManufacturer);

        this.geoIpv4 = new Ipv4GeoIp();
        this.macs = new MacManufacturer();
    }

    @Override
    public Collection<PipelineStage> getPipelineStages() {
        return grassmarlin.plugins.internal.metadata.Plugin.stages;
    }

    @Override
    public Collection<ClassFactory<?>> getClassFactories() {
        return factories;
    }

    @Override
    public String getName() {
        return "Metadata";
    }
    @Override
    public Collection<MenuItem> getMenuItems() {
        return Arrays.asList(menuPlugin);
    }

    public String countryNameFor(final Ipv4 ip) {
        return geoIpv4.match(ip);
    }
    public String manufacturerFor(final Mac mac) {
        return macs.forMac(mac);
    }
}
