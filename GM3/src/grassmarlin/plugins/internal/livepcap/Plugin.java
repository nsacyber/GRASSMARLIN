package grassmarlin.plugins.internal.livepcap;

import grassmarlin.RuntimeConfiguration;
import grassmarlin.plugins.IPlugin;
import grassmarlin.session.ImportItem;
import grassmarlin.session.Session;
import javafx.scene.control.MenuItem;

import java.util.Collection;
import java.util.Iterator;

/**
 * Most live pcap is deeply embedded in the code and can't be implemented as a
 * plugin.
 *
 * The integration in the menu and toolbar, for example, is not made available
 * to plugins, since plugins aren't supposed to know about such things (in the
 * CLI mode they don't exist).  But these are features that have been around
 * for several versions, and we want to keep them there, for familiarity, if
 * not convenience.  While building a well-abstracted framework is important,
 * this is a case where it makes sense to special-purpose implementing code.
 *
 * Given those constraints, there are parts of live pcap that certainly feel
 * like plugin-worthy material, and are implemented as such:
 *   The Importer types that are reported.
 *   Each session needs to have a PcapEngine associated with it, and plugins
 *   already handle that sort of state tracking (to say nothing of appropriate
 *   hooks for unmanaged resource management)
 *
 * So, if you're looking at this because you want to understand how to make a
 * plugin that interacts with the UI like the LivePcap plugin appears to...
 * You can't.
 *
 * The ImportProcessorWrapper is not exposed via plugin interface because the
 * user is never allowed to select it.  We will still use the wrapper, we just
 * can't expose it through the normal mechanisms.  Fortunately, the PcapEngine
 * functions as a static entity, so we don't need all sorts of complicated
 * coupling.
 */
public class Plugin implements IPlugin {
    private final RuntimeConfiguration config;

    private static PcapEngine enginePcap = null;
    public static ImportProcessorWrapper wrapperLivePcap = new ImportProcessorWrapper("Live Pcap") {
        @Override
        public Iterator<Object> getProcessor(final ImportItem item, final Session session) {
            if(item instanceof LivePcapImport){
                return enginePcap.start((LivePcapImport)item, session);
            } else {
                return null;
            }

        }
        @Override
        public boolean itemIsValidTarget(final ImportItem item) {
            return item instanceof LivePcapImport;
        }
    };
    public static PcapEngine getPcapEngine() {
        return enginePcap;
    }

    public Plugin(final RuntimeConfiguration config) {
        this.config = config;

        if(enginePcap == null) {
            enginePcap = new PcapEngine(config);
        }
    }

    @Override
    public String getName() {
        return "Live Pcap";
    }

    @Override
    public Collection<MenuItem> getMenuItems() {
        return null;
    }

}
