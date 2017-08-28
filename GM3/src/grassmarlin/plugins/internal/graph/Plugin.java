package grassmarlin.plugins.internal.graph;

import grassmarlin.RuntimeConfiguration;
import grassmarlin.plugins.IPlugin;
import grassmarlin.session.logicaladdresses.Cidr;
import javafx.scene.control.MenuItem;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class Plugin implements IPlugin, IPlugin.DefinesPipelineStages, IPlugin.HasClassFactory {
    private static List<PipelineStage> stages = new ArrayList<PipelineStage>() {
        {
            this.add(new PipelineStage(false, StageBuildGraph.NAME, StageBuildGraph.class, StageBuildGraph.DEFAULT_OUTPUT, StageBuildGraph.OUTPUT_HARDWAREADDRESSES, StageBuildGraph.OUTPUT_LOGICALADDRESSMAPPINGS));
            this.add(new PipelineStage(false, StageSetProperties.NAME, StageSetProperties.class, StageSetProperties.DEFAULT_OUTPUT));
            this.add(new PipelineStage(false, StageRecordCompletion.NAME, StageRecordCompletion.class, StageSetProperties.DEFAULT_OUTPUT));
        }
    };
    private static List<ClassFactory<?>> addressFactories = new ArrayList<ClassFactory<?>>() {
        {
            this.add(new ClassFactory() {
                @Override
                public String getFactoryName() {
                    return "Cidr";
                }
                @Override
                public Class<?> getFactoryClass() {
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
        }
    };

    public Plugin(final RuntimeConfiguration config) {
        //No initialization required.
    }

    @Override
    public Collection<PipelineStage> getPipelineStages() {
        return Plugin.stages;
    }

    @Override
    public Collection<ClassFactory<?>> getClassFactories() {
        return addressFactories;
    }

    @Override
    public String getName() {
        return "Graph";
    }
    @Override
    public Collection<MenuItem> getMenuItems() {
        return null;
    }
}
