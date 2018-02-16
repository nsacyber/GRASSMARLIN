package grassmarlin.plugins.internal.physical.deviceimport;

import grassmarlin.RuntimeConfiguration;
import grassmarlin.plugins.IPlugin;
import grassmarlin.session.ImportItem;
import grassmarlin.session.Session;
import javafx.scene.control.MenuItem;

import java.nio.file.Path;
import java.util.*;

public class Plugin implements IPlugin, IPlugin.HasImportProcessors {
    public static final String NAME = "Physical Device Descriptions";
    private final List<ImportProcessorWrapper> processorWrappers = new ArrayList<>(1);

    public Plugin(final RuntimeConfiguration config) {
        this.processorWrappers.add(new ImportProcessorWrapper("Physical Devices", ".txt") {
            @Override
            public Iterator<Object> getProcessor(ImportItem item, Session session) {
                return new Importer(config, item).getIterator();
            }

            @Override
            public boolean itemIsValidTarget(Path item) {
                return true;
            }
        });
    }

    // == HasImportProcessors

    @Override
    public Collection<ImportProcessorWrapper> getImportProcessors() {
        return this.processorWrappers;
    }

    @Override
    public Map<Integer, FactoryPacketHandler> getPcapHandlerFactories() {
        return null;
    }


    // == IPlugin

    @Override
    public String getName() {
        return Plugin.NAME;
    }

    @Override
    public Collection<MenuItem> getMenuItems() {
        return null;
    }
}
