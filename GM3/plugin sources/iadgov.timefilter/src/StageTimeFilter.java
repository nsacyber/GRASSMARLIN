package iadgov.timefilter;

import grassmarlin.RuntimeConfiguration;
import grassmarlin.session.Session;
import grassmarlin.session.pipeline.AbstractStage;
import grassmarlin.session.pipeline.IPacketMetadata;

import java.io.Serializable;
import java.util.Arrays;
import java.util.function.Consumer;

public class StageTimeFilter extends AbstractStage<Session> {

    public static final String NAME = "Time Filter";
    public static final String REJECTED = "Rejected";

    private Timespan span;

    Plugin plugin;

    public StageTimeFilter(RuntimeConfiguration config, Session session) {
        super(config, session, Object.class);
        plugin = (Plugin)config.pluginFor(this.getClass());

        // Objects will be sent here explicitly by the process method
        this.defineOutput(REJECTED, null, Arrays.asList(Object.class));
    }

    @Override
    public Object process(Object obj) {
        if (obj instanceof IPacketMetadata) {
            long packetTime = ((IPacketMetadata) obj).getTime();
            if (packetTime >= span.start.toEpochMilli() && packetTime < span.end.toEpochMilli()) {
                return obj;
            } else {
                final Consumer<Object> target = this.targetOf(REJECTED);
                if (target != null) {
                    target.accept(obj);
                }
                return null;
            }
        } else {
            return obj;
        }
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public void setConfiguration(Serializable span) {
        if (span != null && span instanceof Timespan) {
            this.span = ((Timespan) span);
        }
    }

}
