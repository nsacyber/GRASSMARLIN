package grassmarlin.plugins.internal.graph;

import grassmarlin.RuntimeConfiguration;
import grassmarlin.session.ImportItem;
import grassmarlin.session.Session;
import grassmarlin.session.pipeline.AbstractStage;
import grassmarlin.session.pipeline.IDeferredProgress;

import java.util.concurrent.ArrayBlockingQueue;

public class StageRecordCompletion extends AbstractStage<Session> {
    public static final String NAME = "Record Deferred Progress";

    public StageRecordCompletion(final RuntimeConfiguration config, final Session session) {
        super(config, session, new ArrayBlockingQueue<>(250), IDeferredProgress.class);
    }

    public Object process(final Object obj) {
        if(obj instanceof IDeferredProgress) {
            final IDeferredProgress progress = (IDeferredProgress) obj;
            final ImportItem source = progress.getImportSource();
            final long amount = progress.getImportProgress();

            // Source is null when working with live pcap.  Conceivably, it could also be null from other sources.
            // Amounts should not be negative, and an amount of 0 requires no action.
            if (source != null && amount > 0) {
                source.recordProgress(amount);
            }
        }
        return obj;
    }

    public String getName() {
        return NAME;
    }
}
