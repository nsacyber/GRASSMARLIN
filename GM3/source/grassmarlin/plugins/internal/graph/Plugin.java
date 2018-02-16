package grassmarlin.plugins.internal.graph;

import grassmarlin.RuntimeConfiguration;
import grassmarlin.plugins.IPlugin;
import grassmarlin.session.hardwareaddresses.Mac;
import grassmarlin.session.hardwareaddresses.Unique;
import grassmarlin.session.logicaladdresses.Cidr;
import grassmarlin.session.logicaladdresses.Ipv4;
import grassmarlin.ui.common.dialogs.preferences.PreferenceDialog;
import javafx.scene.control.ButtonType;
import javafx.scene.control.MenuItem;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class Plugin implements IPlugin, IPlugin.DefinesPipelineStages, IPlugin.HasClassFactory {
    private static List<PipelineStage> stages = new ArrayList<PipelineStage>() {
        {
            this.add(new PipelineStage(false, StageBuildGraph.NAME, StageBuildGraph.class, StageBuildGraph.DEFAULT_OUTPUT, StageBuildGraph.OUTPUT_HARDWAREADDRESSES, StageBuildGraph.OUTPUT_LOGICALADDRESSMAPPINGS, StageBuildGraph.OUTPUT_PACKETS_RECORDABLE, StageBuildGraph.OUTPUT_PACKETS_QUESTIONABLE));
            this.add(new PipelineStage(false, StageSetProperties.NAME, StageSetProperties.class, StageSetProperties.DEFAULT_OUTPUT));
            this.add(new PipelineStage(false, StageAddProperties.NAME, StageAddProperties.class, StageAddProperties.DEFAULT_OUTPUT));
            this.add(new PipelineStage(false, StageRecordCompletion.NAME, StageRecordCompletion.class, StageSetProperties.DEFAULT_OUTPUT));
            this.add(new PipelineStage(true, StagePulse.NAME, StagePulse.class, StagePulse.DEFAULT_OUTPUT));
            this.add(new PipelineStage(true, StageBroadcast.NAME, StageBroadcast.class, StageBroadcast.DEFAULT_OUTPUT, StageBroadcast.PULSE_OUTPUT));
        }
    };
    private static List<ClassFactory<?>> addressFactories = new ArrayList<ClassFactory<?>>() {
        {
            this.add(new ClassFactory<String>() {
                @Override
                public String getFactoryName() {
                    return "Text";
                }
                @Override
                public Class<String> getFactoryClass() {
                    return String.class;
                }
                @Override
                public String createInstance(final String text) {
                    return text;
                }
                @Override
                public boolean validateText(final String text) {
                    return true;
                }
            });
            this.add(new ClassFactory<Double>() {
                @Override
                public String getFactoryName() {
                    return "Double-Precision Number";
                }
                @Override
                public Class<Double> getFactoryClass() {
                    return Double.class;
                }
                @Override
                public Double createInstance(final String text) {
                    return new Double(text);
                }
                @Override
                public boolean validateText(final String text) {
                    try {
                        //We just need to make sure it can be parsed.
                        //noinspection ResultOfMethodCallIgnored
                        Double.parseDouble(text);
                        return true;
                    } catch(NumberFormatException ex) {
                        return false;
                    }
                }
            });
            this.add(new ClassFactory<Long>() {
                @Override
                public String getFactoryName() {
                    return "64-bit Signed Integer";
                }
                @Override
                public Class<Long> getFactoryClass() {
                    return Long.class;
                }
                @Override
                public Long createInstance(final String text) {
                    return new Long(text);
                }
                @Override
                public boolean validateText(final String text) {
                    try {
                        //We just need to make sure it can be parsed.
                        //noinspection ResultOfMethodCallIgnored
                        Long.parseLong(text);
                        return true;
                    } catch(NumberFormatException ex) {
                        return false;
                    }
                }
            });
            this.add(new ClassFactory<Cidr>() {
                @Override
                public String getFactoryName() {
                    return "Cidr";
                }
                @Override
                public Class<Cidr> getFactoryClass() {
                    return Cidr.class;
                }
                @Override
                public Cidr createInstance(final String text) {
                    return Cidr.fromString(text);
                }
                @Override
                public boolean validateText(final String text) {
                    return Cidr.fromString(text) != null;
                }
            });
            this.add(new ClassFactory<Mac>() {
                @Override
                public String getFactoryName() {
                    return "Mac";
                }
                @Override
                public Class<Mac> getFactoryClass() {
                    return Mac.class;
                }
                @Override
                public Mac createInstance(String text) {
                    return new Mac(text);
                }
                @Override
                public boolean validateText(String text) {
                    return text.matches("^([A-Fa-f0-9]{2}:){5}([A-Fa-f0-9]{2})$");
                }
            });
            this.add(new ClassFactory<Ipv4>() {
                @Override
                public String getFactoryName() {
                    return "Ipv4";
                }
                @Override
                public Class<Ipv4> getFactoryClass() {
                    return Ipv4.class;
                }
                @Override
                public Ipv4 createInstance(String text) {
                    return Ipv4.fromString(text);
                }
                @Override
                public boolean validateText(String text) {
                    //TODO: Better Ipv4 Regex
                    return text.matches("\\d+\\.\\d+\\.\\d+\\.\\d+");
                }
            });
            this.add(new ClassFactory<Unique>() {
                @Override
                public String getFactoryName() {
                    return "Unique";
                }
                @Override
                public Class<Unique> getFactoryClass() {
                    return Unique.class;
                }
                @Override
                public Unique createInstance(String text) {
                    return new Unique();
                }
                @Override
                public boolean validateText(String text) {
                    return true;
                }
            });
        }
    };

    private final RuntimeConfiguration config;

    public Plugin(final RuntimeConfiguration config) {
        this.config = config;
    }

    @Override
    public Collection<PipelineStage> getPipelineStages() {
        return Plugin.stages;
    }

    @Override
    public Serializable getConfiguration(PipelineStage stage, Serializable configuration) {
        if(stage.getStage().equals(StagePulse.class) && configuration instanceof StagePulse.Configuration) {
            final PreferenceDialog<StagePulse.Configuration> dlg = new PreferenceDialog<>(this.config, (StagePulse.Configuration) configuration);
            if (dlg.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
                return dlg.getPreferences();
            } else {
                return configuration;
            }
        } else if(stage.getStage().equals(StageBroadcast.class) && configuration instanceof StageBroadcast.Configuration) {
            final PreferenceDialog<StageBroadcast.Configuration> dlg = new PreferenceDialog<>(this.config, (StageBroadcast.Configuration) configuration);
            if (dlg.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
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
        if(stage.getStage().equals(StagePulse.class)) {
            return new StagePulse.Configuration(1000);
        } else if(stage.getStage().equals(StageBroadcast.class)) {
            return new StageBroadcast.Configuration(false);
        } else {
            return null;
        }
    }

    @Override
    public Collection<ClassFactory<?>> getClassFactories() {
        return addressFactories;
    }

    @Override
    public String getName() {
        return "Pipeline Stages";
    }
    @Override
    public Collection<MenuItem> getMenuItems() {
        return null;
    }
}
