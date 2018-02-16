package grassmarlin.plugins.internal.offlinepcap;

import grassmarlin.RuntimeConfiguration;
import grassmarlin.plugins.IPlugin;
import grassmarlin.plugins.internal.logicalview.visual.VertexColorAssignment;
import grassmarlin.plugins.internal.offlinepcap.ethernet.Ipv4WithEphemeralPort;
import grassmarlin.plugins.internal.offlinepcap.ethernet.PacketHandler;
import grassmarlin.session.ImportItem;
import grassmarlin.session.Session;
import javafx.beans.property.BooleanProperty;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.MenuItem;

import java.nio.file.Path;
import java.util.*;

public class Plugin implements IPlugin, IPlugin.HasImportProcessors {
    private enum PersistedValues implements RuntimeConfiguration.IPersistedValue {
        COLOR_EPHEMERAL_BACKGROUND("visual.ephemeral.background", () -> "ff00ff"),
        COLOR_EPHEMERAL_TEXT("visual.ephemeral.text", () -> "000000")
        ;

        private final String key;
        private final RuntimeConfiguration.IDefaultValue fnDefault;

        @Override
        public String getKey() {
            return this.key;
        }
        @Override
        public RuntimeConfiguration.IDefaultValue getFnDefault() {
            return this.fnDefault;
        }

        PersistedValues(final String key, final RuntimeConfiguration.IDefaultValue fnDefault) {
            this.key = key;
            this.fnDefault = fnDefault;
        }
    }
    private static final RuntimeConfiguration.PersistedColorProperty colorEphemeralBackground = new RuntimeConfiguration.PersistedColorProperty(PersistedValues.COLOR_EPHEMERAL_BACKGROUND);
    private static final RuntimeConfiguration.PersistedColorProperty colorEphemeralText = new RuntimeConfiguration.PersistedColorProperty(PersistedValues.COLOR_EPHEMERAL_TEXT);

    static {
        VertexColorAssignment.defineColorMapping(Ipv4WithEphemeralPort.class, new VertexColorAssignment.VertexColor(colorEphemeralBackground, colorEphemeralText) { });
    }

    private final Collection<MenuItem> menuPlugin;
    private final RuntimeConfiguration config;
    private final CheckMenuItem miParseNameBlock;
    private final CheckMenuItem miEnumerateEphemeralPorts;

    public Plugin(final RuntimeConfiguration config) {
        this.config = config;
        this.miParseNameBlock = new CheckMenuItem("Extract data from PcapNg Name Resolution Blocks.");
        this.miParseNameBlock.setDisable(true);

        this.miEnumerateEphemeralPorts = new CheckMenuItem("Enumerate Ephemeral Ports");

        this.menuPlugin = new ArrayList<>();
        this.menuPlugin.addAll(Arrays.asList(
                this.miParseNameBlock,
                this.miEnumerateEphemeralPorts
        ));
    }

    public BooleanProperty readNameBlockProperty() {
        return miParseNameBlock.selectedProperty();
    }
    public BooleanProperty enumerateEphemeralPortsProperty() {
        return miEnumerateEphemeralPorts.selectedProperty();
    }

    @Override
    public String getName() {
        return "Offline Pcap";
    }

    @Override
    public Collection<MenuItem> getMenuItems() {
        return this.menuPlugin;
    }

    @Override
    public Collection<ImportProcessorWrapper> getImportProcessors() {
        return Arrays.asList(
                new ImportProcessorWrapper(
                        "Pcap",
                        ".pcap") {
                    @Override
                    public Iterator<Object> getProcessor(final ImportItem item, final Session session) {
                        return new PcapImport(Plugin.this.config, item).getIterator();
                    }

                    @Override
                    public boolean itemIsValidTarget(final Path item) {
                        return PcapImport.validateFileFormat(item, false);
                    }
                },
                new ImportProcessorWrapper("PcapNg", ".pcapng") {
                    @Override
                    public Iterator<Object> getProcessor(final ImportItem item, final Session session) {
                        return new PcapNgImport(Plugin.this.config, item).getIterator();
                    }

                    @Override
                    public boolean itemIsValidTarget(final Path item) {
                        return PcapNgImport.validateFileFormat(item, false);
                    }
                }
        );
    }

    @Override
    public HashMap<Integer, FactoryPacketHandler> getPcapHandlerFactories() {
        final HashMap<Integer, FactoryPacketHandler> result = new HashMap<>();
        result.put(1, (source, packetQueue) -> new PacketHandler(source, packetQueue, Plugin.this.enumerateEphemeralPortsProperty().get()));
        return result;
    }
}
