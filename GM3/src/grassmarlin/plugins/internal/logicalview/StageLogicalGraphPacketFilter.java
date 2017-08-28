package grassmarlin.plugins.internal.logicalview;

import grassmarlin.RuntimeConfiguration;
import grassmarlin.session.Session;
import grassmarlin.session.pipeline.AbstractStage;
import grassmarlin.session.pipeline.IIcmpPacketData;
import grassmarlin.session.pipeline.IPacketData;

import java.util.function.Consumer;

public class StageLogicalGraphPacketFilter extends AbstractStage<Session> {
    public static final String NAME = "Logical Graph PacketFilter";
    public static final String OUTPUT_REJECTED_BY_FILTER = "Rejected";

    public StageLogicalGraphPacketFilter(final RuntimeConfiguration config, final Session session) {
        super(config, session, IPacketData.class);

        // We're going to redirect rejected packets directly in the process method, so the setup for the rejected path is a bit atypical.
        super.defineOutput(OUTPUT_REJECTED_BY_FILTER);
        super.disallowOutputClasses(OUTPUT_REJECTED_BY_FILTER, Object.class);
    }

    @Override
    public Object process(final Object o) {
        boolean reject = false;

        if(o instanceof IIcmpPacketData) {
            // Reject if this is not a ping reply.
            reject = (((IIcmpPacketData)o).getIcmpType() != 0);
        }

        if(reject) {
            final Consumer<Object> target = targetOf(OUTPUT_REJECTED_BY_FILTER);
            if(target != null) {
                target.accept(o);
            }
            return null;
        } else {
            return o;
        }
    }

    @Override
    public String getName() {
        return NAME;
    }
}
