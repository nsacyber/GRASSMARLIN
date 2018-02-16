package grassmarlin.plugins.internal.graph;

import grassmarlin.RuntimeConfiguration;
import grassmarlin.session.Session;
import grassmarlin.session.pipeline.AbstractStage;
import grassmarlin.ui.common.dialogs.preferences.PreferenceDialog;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

public class StageBroadcast extends AbstractStage<Session> {
    public static final String NAME = "Broadcast";
    public static final String PULSE_OUTPUT = "Pulse";

    public static class Configuration implements Serializable, Cloneable {
        @PreferenceDialog.Field(name="Forget after broadcast", accessorName="ForgetAfterBroadcast", nullable = false)
        private Boolean forgetAfterBroadcast;

        // We store the cache of remembered objects in the configuration so that it is saved.  This has a side effect of sharing the lists between identically-named instances, but preserving the list through serialization is worth it.
        private final Set<Object> memory;

        public Configuration(final boolean forgetAfterBroadcast) {
            this.forgetAfterBroadcast = forgetAfterBroadcast;
            this.memory = new HashSet<>();
        }

        @Override
        public Object clone() throws CloneNotSupportedException {
            return super.clone();
        }

        public Boolean getForgetAfterBroadcast() {
            return this.forgetAfterBroadcast;
        }
        public void setForgetAfterBroadcast(final Boolean value) {
            this.forgetAfterBroadcast = value;
        }

        Set<Object> getMemory() {
            return this.memory;
        }
    }

    private Configuration options;

    public StageBroadcast(final RuntimeConfiguration config, final Session session) {
        super(config, session, Object.class);

        //Pulses get rebroadcast on a dedicated output, but not the default.
        super.disallowOutputClasses(DEFAULT_OUTPUT, StagePulse.Pulse.class);
        super.defineOutput(PULSE_OUTPUT, StagePulse.Pulse.class);
    }

    @Override
    public Object process(Object o) {
        if(o instanceof StagePulse.Pulse) {
            if(options != null && options.getForgetAfterBroadcast()) {
                synchronized(this.options.getMemory()) {
                    final Collection result = new ArrayList<>(this.options.getMemory());
                    this.options.getMemory().clear();
                    return result;
                }
            } else  {
                synchronized(this.options.getMemory()) {
                    return new ArrayList<>(this.options.getMemory());
                }
            }
        } else {
            synchronized(this.options.getMemory()) {
                this.options.getMemory().add(o);
            }
            return null;
        }
    }

    @Override
    public void setConfiguration(final Serializable options) {
        if(options != null && options instanceof Configuration) {
            this.options = (Configuration)options;
        }
    }

    @Override
    public String getName() {
        return NAME;
    }
}
