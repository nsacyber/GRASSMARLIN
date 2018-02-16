package grassmarlin.ui.common.tasks;

import grassmarlin.Logger;
import grassmarlin.RuntimeConfiguration;
import grassmarlin.plugins.IPlugin;
import grassmarlin.session.serialization.Loader;
import grassmarlin.session.serialization.Manifest;
import grassmarlin.ui.common.ActionStackPropertyWrapper;
import grassmarlin.ui.sdi.SingleDocumentState;

import javax.xml.stream.XMLStreamException;
import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.zip.ZipFile;

public class LoadingTask extends AsyncUiTask<LoadingTask> {
    private final RuntimeConfiguration config;
    private final Path path;

    private final SingleDocumentState state;

    public LoadingTask(final RuntimeConfiguration config, final ActionStackPropertyWrapper masterActionStack, final Path path, final AsyncUiTask.TaskCallback<LoadingTask> onSuccess, final AsyncUiTask.TaskCallback<LoadingTask> onFailure) {
        super("Loading", true, onSuccess, onFailure, null);

        this.state = new SingleDocumentState(config, masterActionStack);
        this.config = config;
        this.path = path;
    }

    @Override
    public boolean task(AtomicBoolean shouldStop) {
        try {
            try (final ZipFile zip = new ZipFile(this.path.toAbsolutePath().toString())) {
                // Check the file's manifest against the current environment's
                final Manifest manifestFile = new Manifest(zip.getInputStream(zip.getEntry("manifest.xml")));
                final Manifest manifestCurrent = new Manifest(this.config);
                if(!manifestFile.equals(manifestCurrent)) {
                    //TODO: resolve the conflict with the user--either way we need to know what plugins are loaded, what to skip, etc. (as well as whether or not to abort)
                    if(manifestCurrent.satisfies(manifestFile)) {
                        Logger.log(Logger.Severity.WARNING, "The selected file was saved with a different set of plugins than are currently enabled.  The file will be loaded but may behave differently.");
                    } else {
                        Logger.log(Logger.Severity.ERROR, "The selected file was saved with an incompatible set of plugins and cannot be loaded.");
                        return false;
                    }
                }

                //TODO: We need a mapping of Plugin->Actions to use after here.
                Loader.loadSession(this.state.getSession(), zip);
                this.state.getSession().waitForSync(); //Ensure that all pending events have fired.

                //TODO: Restore plugin-specific data.
                final AtomicBoolean isPluginLoadingSuccessful = new AtomicBoolean(true);
                config.enumeratePlugins(IPlugin.SessionSerialization.class).forEach(plugin -> {
                    try {
                        plugin.sessionLoaded(this.state.getSession(), zip.getInputStream(zip.getEntry(plugin.getName() + ".xml")), (substream) -> {
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
        } finally {
            //This will clear the dirty flag.
            this.state.saved(this.path.toAbsolutePath().toString());
        }
    }

    public SingleDocumentState getDocumentState() {
        return this.state;
    }
}
