package grassmarlin.plugins.internal.physicalview;

import grassmarlin.Logger;
import grassmarlin.RuntimeConfiguration;
import grassmarlin.plugins.IPlugin;
import grassmarlin.plugins.internal.physicalview.visual.TabPhysicalGraph;
import grassmarlin.session.Session;
import grassmarlin.ui.common.ImageDirectoryWatcher;
import javafx.scene.control.MenuItem;
import javafx.scene.image.Image;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class Plugin implements IPlugin, IPlugin.SessionEventHooks, IPlugin.DefinesPipelineStages, IPlugin.SessionSerialization {
    public static final String NAME = "Physical Graph";

    private final static Collection<PipelineStage> STAGES = new ArrayList<PipelineStage>() {
        {
            this.add(new IPlugin.PipelineStage(false, StageProcessPhysicalGraphElements.NAME, StageProcessPhysicalGraphElements.class, StageProcessPhysicalGraphElements.DEFAULT_OUTPUT));
        }
    };


    public class PhysicalGraphState {
        public final SessionConnectedPhysicalGraph graph;
        public final TabPhysicalGraph tab;
        public final Session session;

        public final ImageDirectoryWatcher physicalImageFactory;

        public PhysicalGraphState(final Session session) {
            ImageDirectoryWatcher watcher = null;
            try {
                watcher = new ImageDirectoryWatcher<>(
                        Paths.get(config.getPersistedString(RuntimeConfiguration.PersistableFields.DIRECTORY_APPLICATION), "images", "physical"),
                        config.getUiEventProvider(),
                        path -> new Image("file:" + path.toAbsolutePath().toString())
                );
            } catch(IOException ex) {
                Logger.log(Logger.Severity.ERROR, "Unable to initialize Physical Graph images: %s", ex.getMessage());
                watcher = null;
            } finally {
                this.physicalImageFactory = watcher;
            }

            this.graph = new SessionConnectedPhysicalGraph(session, Plugin.this.config.getUiEventProvider());
            this.tab = new TabPhysicalGraph(this.graph, this.physicalImageFactory);
            this.session = session;
        }

        public Plugin getPlugin() {
            return Plugin.this;
        }
    }

    private final Map<Session, PhysicalGraphState> sessionStates;
    private final RuntimeConfiguration config;

    public Plugin(final RuntimeConfiguration config) {
        this.sessionStates = new HashMap<>();
        this.config = config;
    }

    public PhysicalGraphState stateForSession(final Session session) {
        return sessionStates.get(session);
    }

    // == Core IPlugin
    @Override
    public String getName() {
        return NAME;
    }
    @Override
    public Collection<MenuItem> getMenuItems() {
        return null;
    }

    // == IPlugin.SessionEventHooks
    @Override
    public void sessionCreated(final grassmarlin.session.Session session, final grassmarlin.ui.common.TabController tabs) {
        final PhysicalGraphState state = new PhysicalGraphState(session);
        tabs.addContent(state.tab);
        this.sessionStates.put(session, state);
    }
    // Since serializing a session is going to have to save and load data that belongs to a plugin, there need to be more hooks
    @Override
    public void sessionLoaded(final grassmarlin.session.Session session, final java.io.InputStream stream, final SessionSerialization.GetChildStream fnGetStream) throws IOException {
        //TODO: Load Physical Graph
    }

    @Override
    public void sessionSaving(final grassmarlin.session.Session session, final java.io.OutputStream stream, final SessionSerialization.CallbackCreateStream fnCreateStream) throws IOException {
        //TODO: Save Physical Graph
    }
    @Override
    public void sessionClosed(final grassmarlin.session.Session session) {
        this.sessionStates.remove(session);
        //TODO: Cleanup removed state
    }

    // == IPlugin.DefinesPipelineStages
    @Override
    public Collection<PipelineStage> getPipelineStages() {
        return STAGES;
    }
}
