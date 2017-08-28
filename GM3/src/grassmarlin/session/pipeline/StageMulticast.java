package grassmarlin.session.pipeline;

import grassmarlin.RuntimeConfiguration;
import grassmarlin.session.Session;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

public class StageMulticast extends AbstractStage<Session>{

    public static String NAME = "Multicast";

    final private AtomicInteger index;

    public StageMulticast(RuntimeConfiguration config, Session session) {
        super(config, session, (BlockingQueue<Object>)null);

        index = new AtomicInteger(0);
    }

    @Override
    public Object process(Object obj) {
        return obj;
    }

    @Override
    public String getName() {
        return NAME;
    }

    public void connectOutput(String name, Consumer<Object>... stages) {
        for (Consumer<Object> consumer : stages) {
            int i = index.getAndIncrement();
            this.defineOutput(name + i);
            this.connectOutput(name + i, consumer);
        }
    }
}
