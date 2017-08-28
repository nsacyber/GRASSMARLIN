package grassmarlin.ui.common.tasks;

import grassmarlin.Logger;
import grassmarlin.RuntimeConfiguration;
import grassmarlin.plugins.IPlugin;
import grassmarlin.session.Session;
import grassmarlin.session.serialization.Loader;
import grassmarlin.session.serialization.Manifest;
import grassmarlin.ui.common.TabController;

import javax.xml.stream.XMLStreamException;
import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.zip.ZipFile;

public class LoadingTask extends AsyncUiTask<LoadingTask> {
    private final RuntimeConfiguration config;
    private final TabController tabs;
    private final Session session;
    private final Path path;

    public LoadingTask(final RuntimeConfiguration config, final Path path, final AsyncUiTask.TaskCallback<LoadingTask> onSuccess, final AsyncUiTask.TaskCallback<LoadingTask> onFailure) {
        super("Loading", true, onSuccess, onFailure, null);

        this.config = config;
        this.tabs = new TabController();
        this.session = new Session(config);
        this.path = path;
    }

    @Override
    public boolean task(AtomicBoolean shouldStop) {
        try {
            // Announce new session to plugins (so that initialization can be done)
            // This is run in the Fx thread, but we have to wait for it to complete.

            //TODO: This should be based on an Event.IAsyncExecutionProvider; SynchronousPlatform was the precusor to IAsyncExecutionProvider.  Despite the various improvements it offers over the precursor class, IAsyncExecutionProvider doesn't launch rainbow-trailing unicorns or enable you to leave glowing rainbow horseshoeprints in your wake, but if you want to work with arbitrary ui models, it has you covered.
            config.enumeratePlugins(IPlugin.SessionEventHooks.class).forEach(plugin -> {
                SynchronousPlatform.runNow(() -> plugin.sessionCreated(this.session, this.tabs));
            });

            try (final ZipFile zip = new ZipFile(this.path.toAbsolutePath().toString())) {
                // Check the file's manifest against the current environment's
                final Manifest manifestFile = new Manifest(zip.getInputStream(zip.getEntry("manifest.xml")));
                final Manifest manifestCurrent = new Manifest(this.config);
                if(!manifestFile.equals(manifestCurrent)) {
                    //TODO: resolve the conflict with the user--either way we need to know what plugins are loaded, what to skip, etc. (as well as whether or not to abort)
                    Logger.log(Logger.Severity.ERROR, "The selected file was saved with an incompatible set of plugins and cannot be loaded.");
                    return false;
                }

                //TODO: We need a mapping of Plugin->Actions to use after here.
                Loader.loadSession(this.config, this.session, zip);
                this.session.waitForSync(); //Ensure that all pending events have fired.

                //TODO: Restore plugin-specific data.
                final AtomicBoolean isPluginLoadingSuccessful = new AtomicBoolean(true);
                config.enumeratePlugins(IPlugin.SessionSerialization.class).forEach(plugin -> {
                    try {
                        plugin.sessionLoaded(this.session, zip.getInputStream(zip.getEntry(plugin.getName() + ".xml")), (substream) -> {
                            return zip.getInputStream(zip.getEntry(plugin.getName() + "." + substream + ".xml"));
                        });
                    } catch(IOException ex) {
                        isPluginLoadingSuccessful.set(false);
                    }
                });

                if(!isPluginLoadingSuccessful.get()) {
                    Logger.log(Logger.Severity.WARNING, "At least one plugin failed to load data correctly.");
                }
                return true;
            }
        } catch(IOException | XMLStreamException ex) {
            Logger.log(Logger.Severity.ERROR, "An error occurred while loading '%s': %s", path.getFileName(), ex.getMessage());
            return false;
        }
    }

    public Session getSession() {
        return this.session;
    }

    public TabController getTabController() {
        return this.tabs;
    }
}
