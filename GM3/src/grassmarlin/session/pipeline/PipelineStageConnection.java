package grassmarlin.session.pipeline;

import grassmarlin.plugins.IPlugin;

public class PipelineStageConnection {

    private IPlugin.PipelineStage sourceStage;
    private String output;
    private IPlugin.PipelineStage destStage;

    public PipelineStageConnection(IPlugin.PipelineStage source, String output, IPlugin.PipelineStage dest) {
        this.sourceStage = source;
        this.output = output;
        this.destStage = dest;
    }

    public IPlugin.PipelineStage getSourceStage() {
        return this.sourceStage;
    }

    public String getOutput() {
        return this.output;
    }

    public IPlugin.PipelineStage getDestStage() {
        return this.destStage;
    }
}
