package iadgov.diff;

import grassmarlin.RuntimeConfiguration;
import grassmarlin.plugins.IPlugin;
import grassmarlin.session.Property;
import grassmarlin.session.Session;
import grassmarlin.session.pipeline.AbstractStage;
import grassmarlin.ui.common.TabController;
import grassmarlin.ui.common.dialogs.preferences.PreferenceDialog;
import grassmarlin.ui.common.menu.ActiveMenuItem;
import javafx.scene.control.ButtonType;
import javafx.scene.control.MenuItem;

import java.io.Serializable;
import java.time.Instant;
import java.util.*;

public class Plugin implements IPlugin, IPlugin.SessionEventHooks, IPlugin.DefinesPipelineStages {
    public static final String NAME = "Diff";

    private static final PipelineStage STAGE_ASSIGN_PROPERTIES = new PipelineStage(true, StageAssignProperties.NAME, StageAssignProperties.class, AbstractStage.DEFAULT_OUTPUT, StageAssignProperties.OUTPUT_GENERATED_PROPERTIES);

    private final RuntimeConfiguration config;
    protected final List<Session> sessions;
    protected final List<MenuItem> miPlugin;

    public Plugin(final RuntimeConfiguration config) {
        this.config = config;
        this.sessions = new LinkedList<>();
        this.miPlugin = new ArrayList<>();
        this.miPlugin.addAll(Arrays.asList(new ActiveMenuItem("_Mark as Baseline", event -> {
            final Instant now = Instant.now();
            for(final Session session : Plugin.this.sessions) {
                session.executeWithLock(() -> {
                    session.executeOnHardwareVerticesWithLock(hw -> {
                        hw.addProperties(NAME, "Diff Baseline", new Property<>(now, 0));
                    });
                    session.executeOnLogicalVerticesWithLock(logical -> {
                        logical.addProperties(NAME, "Diff Baseline", new Property<>(now, 0));
                    });
                    session.executeOnEdgesWithLock(edge -> {
                        edge.addProperties(NAME, "Diff Baseline", new Property<>(now, 0));
                    });
                });
            }
        })));
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public Collection<MenuItem> getMenuItems() {
        return this.miPlugin;
    }

    // == IPlugin.SessionEventHooks ==

    @Override
    public void sessionCreated(final Session session, final TabController tabs) {
        this.sessions.add(session);
    }

    @Override
    public void sessionClosed(final Session session) {
        this.sessions.remove(session);
    }

    // == IPlugin.DefinesPipelineStages

    @Override
    public Collection<PipelineStage> getPipelineStages() {
        return Arrays.asList(STAGE_ASSIGN_PROPERTIES);
    }

    @Override
    public Serializable getConfiguration(PipelineStage stage, Serializable configuration) {
        if(stage.getStage().equals(StageAssignProperties.class)) {
            final PreferenceDialog<StageAssignProperties.Configuration> dlg = new PreferenceDialog<>(this.config, (StageAssignProperties.Configuration)configuration);
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
        if(stage.getStage().equals(StageAssignProperties.class)) {
            return new StageAssignProperties.Configuration();
        }
        return null;
    }
}
