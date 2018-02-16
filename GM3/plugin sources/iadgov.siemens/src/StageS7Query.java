package iadgov.siemens;

import grassmarlin.RuntimeConfiguration;
import grassmarlin.session.Session;
import grassmarlin.session.pipeline.AbstractStage;

import java.io.Serializable;

public class StageS7Query extends AbstractStage<Session> {
    public static class Configuration implements Cloneable, Serializable {
        private long msTimeout = 500;
        private boolean allowRetryOnFailedQueryTargets = false;

        public Configuration() {

        }
    }

    private Configuration options;

    public StageS7Query(final RuntimeConfiguration config, final Session session) {
        super(config, session, Ipv4WithRackAndSlot.class);

        this.options = new Configuration();
    }

    @Override
    public void setConfiguration(final Serializable configuration) {
        if(configuration instanceof Configuration) {
            this.options = (Configuration)configuration;
        }
    }

    public Configuration getConfiguration() {
        return this.options;
    }

    @Override
    public Object process(Object obj) {
        if(obj instanceof Ipv4WithRackAndSlot) {
            final Ipv4WithRackAndSlot target = (Ipv4WithRackAndSlot)obj;

            //TODO: Execute query, taking into consideration the options.
        }

        return obj;
    }

    @Override
    public String getName() {
        return "Siemens Status Query";
    }
}
