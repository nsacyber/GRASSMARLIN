package grassmarlin.plugins.internal.graph;

import grassmarlin.RuntimeConfiguration;
import grassmarlin.session.Session;
import grassmarlin.session.pipeline.AbstractStage;
import grassmarlin.ui.common.dialogs.preferences.PreferenceDialog;

import java.io.Serializable;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * This class generates periodic grassmarlin.plugins.internal.graph.StagePulse.Pulse events.
 *
 * All inputs are ignored.
 *
 * Outputs are generated at regular intervals with sequential IDs.
 *
 * Pulse objects are intended to trigger behaviors in stages receiving them.  For example, StageBroadcast upon receiving a Pulse, outputs every non-Pulse object it has previously received.
 */
public class StagePulse extends AbstractStage<Session> {
    public final static String NAME = "Pulse";

    public static class Configuration implements Cloneable, Serializable {
        @PreferenceDialog.Field(name="Frequency (ms)", accessorName="Frequency", nullable = false)
        private Long msFrequency;

        public Configuration(final long msFrequency) {
            this.msFrequency = msFrequency;
        }

        @Override
        public Object clone() throws CloneNotSupportedException {
            return super.clone();
        }

        public Long getFrequency() {
            return msFrequency;
        }

        public void setFrequency(Long msFrequency) {
            this.msFrequency = msFrequency;
        }
    }

    public static class Pulse {
        private final int id;
        private Pulse(final int id) {
            this.id = id;
        }

        public int getId() {
            return this.id;
        }

        @Override
        public String toString() {
            return String.format("[Pulse:%d]", this.id);
        }
    }

    private Configuration options;
    private final Thread worker;
    private final AtomicBoolean terminate;
    private final AtomicInteger idNext;

    public StagePulse(final RuntimeConfiguration config, final Session session) {
        super(config, session, null, (Collection<Class<?>>)null);

        this.idNext = new AtomicInteger(0);
        //By having no accepted classes and no BlockingQueue the processing thread will be shut down.
        //We're not going to receive anything, we are only going to output Pulses at timed intervals.
        this.terminate = new AtomicBoolean(false);

        this.worker = new Thread(this::Thread_FirePulses);
        this.worker.setDaemon(true);
        this.worker.setName("StagePulse_Worker");
        this.worker.start();
    }

    private void Thread_FirePulses() {
        while(!terminate.get()) {
            try {
                final Configuration options = this.options;
                if(options != null) {
                    Thread.sleep(options.getFrequency());
                } else {
                    Thread.sleep(1000);
                }
                super.processOutput(new Pulse(idNext.getAndIncrement()));
            } catch(InterruptedException ex) {
                //Ignore it
            }
        }
    }

    @Override
    public void setConfiguration(Serializable configuration) {
        if (configuration != null && configuration instanceof Configuration) {
            this.options = (Configuration)configuration;
        }
    }

    @Override
    public Object process(final Object o) {
        //This should never be called
        throw new UnsupportedOperationException("StagePulse should not be able to receive any inputs.");
    }

    @Override
    public void close() {
        this.terminate.set(true);
        this.worker.interrupt();
    }

    @Override
    public String getName() {
        return NAME;
    }
}
