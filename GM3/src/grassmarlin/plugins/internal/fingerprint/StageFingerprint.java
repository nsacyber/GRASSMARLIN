package grassmarlin.plugins.internal.fingerprint;

import core.fingerprint3.Fingerprint;
import grassmarlin.RuntimeConfiguration;
import grassmarlin.plugins.internal.fingerprint.processor.FProcessor;
import grassmarlin.plugins.internal.fingerprint.processor.FingerprintState;
import grassmarlin.session.Session;
import grassmarlin.session.pipeline.AbstractStage;
import grassmarlin.session.pipeline.IPacketMetadata;

import java.io.Serializable;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.stream.Collectors;

public class StageFingerprint extends AbstractStage<Session> {
    public static final String NAME = "Fingerprint";
    public static final String FINGERPRINT_PROPERTIES = "Fingerprint Properties";

    private FProcessor processor;
    private List<Fingerprint> fingerprints;

    public StageFingerprint(RuntimeConfiguration config, Session session) {
        super(config, session, new ArrayBlockingQueue<>(1000), IPacketMetadata.class);

        defineOutput(FINGERPRINT_PROPERTIES, FingerprintProperties.class, FingerprintEdgeProperties.class);
        disallowOutputClasses(StageFingerprint.DEFAULT_OUTPUT, FingerprintProperties.class, FingerprintEdgeProperties.class);

        setPassiveMode(true);
    }

    @Override
    public Object process(Object obj) {
        if (processor != null) {
            return processor.process((IPacketMetadata) obj);
        } else {
            return null;
        }
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public void setConfiguration(Serializable config) {
        if (config instanceof List) {
            if ((((List) config).get(0) instanceof FingerprintState)) {
                this.fingerprints = ((List<FingerprintState>)config).stream()
                        .map(state -> state.getFingerprint())
                        .collect(Collectors.toList());
                this.processor = new FProcessor(this.fingerprints);
            }
        }
    }
}
