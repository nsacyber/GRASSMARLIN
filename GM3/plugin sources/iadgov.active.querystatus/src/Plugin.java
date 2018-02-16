package iadgov.active.querystatus;

import grassmarlin.RuntimeConfiguration;
import grassmarlin.common.Confidence;
import grassmarlin.plugins.IPlugin;
import grassmarlin.ui.common.dialogs.preferences.PreferenceDialog;
import javafx.scene.control.ButtonType;
import javafx.scene.control.MenuItem;
import javafx.scene.image.Image;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

@IPlugin.Active
public class Plugin implements IPlugin, IPlugin.DefinesPipelineStages {
    public static final String NAME = "Query Status";

    private static final List<PipelineStage> stages = Collections.singletonList(
            new PipelineStage(true, StagePerformActiveQuery.NAME, StagePerformActiveQuery.class, StagePerformActiveQuery.DEFAULT_OUTPUT)
    );

    private final RuntimeConfiguration config;

    public Plugin(final RuntimeConfiguration config) {
        this.config = config;
    }


    // == IPlugin
    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public Collection<MenuItem> getMenuItems() {
        return null;
    }

    private final Image iconLarge = new Image(Plugin.class.getClassLoader().getResourceAsStream("plugin.png"));
    @Override
    public Image getImageForSize(final int pixels) {
        return iconLarge;
    }

    // == IPlugin.DefinesPipelineStages
    @Override
    public Collection<PipelineStage> getPipelineStages() {
        return stages;
    }

    @Override
    public Serializable getConfiguration(PipelineStage stage, Serializable configuration) {
        if(stage.getStage().equals(StagePerformActiveQuery.class) && configuration instanceof StagePerformActiveQuery.Configuration) {
            final PreferenceDialog<StagePerformActiveQuery.Configuration> dlg = new PreferenceDialog<>(this.config, (StagePerformActiveQuery.Configuration)configuration);
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
        if(stage.getStage().equals(StagePerformActiveQuery.class)) {
            return new StagePerformActiveQuery.Configuration(
                    StagePerformActiveQuery.Configuration.Protocol.Udp, 7,
                    "Quack!".getBytes(),
                    //Validation Script:
                    "if(lhs == null) return -1;\r\n" +              //If there is no prior state, update the current but don't identify a change.
                    "if(lhs.length != rhs.length) return 1;\r\n" +  //If the lengths or contents don't match then there is a change and the new should become the baseline.
                    "for(var i=lhs.length;i>=0;i--){\r\n" +
                    "  if(lhs[i] != rhs[i]) return 1;\r\n" +
                    "}\r\n" +
                    "return 0;",                                    //The status is identical
                    100,        // Timeout (ms)
                    32,         // Max Response size
                    "Echo", Confidence.HIGH   //Property name, confidence
            );
        } else {
            return null;
        }
    }
}
