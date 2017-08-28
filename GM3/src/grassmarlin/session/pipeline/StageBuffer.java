package grassmarlin.session.pipeline;

import grassmarlin.RuntimeConfiguration;
import grassmarlin.session.Session;

public class StageBuffer extends AbstractStage<Session> {
    public final static String NAME = "Buffer";

    public StageBuffer(final RuntimeConfiguration config, final Session container) {
        super(config, container);

        super.setPassiveMode(true);
    }

    @Override
    public Object process(final Object object) {
        return null;
    }

    @Override
    public String getName() {
        return NAME;
    }
}
