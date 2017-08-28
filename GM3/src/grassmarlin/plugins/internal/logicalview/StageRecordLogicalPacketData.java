package grassmarlin.plugins.internal.logicalview;

import grassmarlin.RuntimeConfiguration;
import grassmarlin.session.Edge;
import grassmarlin.session.Session;
import grassmarlin.session.pipeline.*;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

public class StageRecordLogicalPacketData extends AbstractStage<Session> {
    public static final String NAME = "Record Logical Packet Data";
    protected final static int NUM_THREADS = 1; //TODO: This seems to become unstable when multithreaded, with apparent deadlocks happening more frequently as it increases.

    public static final String OUTPUT_TCP = "Tcp";
    public static final String OUTPUT_UDP = "Udp";
    public static final String OUTPUT_PCAP = "Pcap";

    public StageRecordLogicalPacketData(final RuntimeConfiguration config, final Session session) {
        //We can process several types of data, but all inherit from IPacketMetadata
        super(config, session, new ArrayBlockingQueue<>(1000), IPacketMetadata.class);

        defineOutput(OUTPUT_TCP, ITcpPacketMetadata.class);
        defineOutput(OUTPUT_UDP, IUdpPacketMetadata.class);
        defineOutput(OUTPUT_PCAP, IPacketData.class);

        final int sizeThreadPool = NUM_THREADS - 1;
        if(sizeThreadPool > 0) {
            final Executor pool = Executors.newFixedThreadPool(sizeThreadPool, runnable -> {
                Thread t = new Thread(runnable);
                t.setDaemon(true);
                return t;
            });

            for(int idx = 0; idx < sizeThreadPool; idx++) {
                pool.execute(this::Thread_Process);
            }
        }

        this.setPassiveMode(true);
    }

    @Override
    public Object process(final Object obj) {
        //We only accept IPacketMetadata
        final IPacketMetadata packet = (IPacketMetadata) obj;
        //We don't actually do any processing, we just want to copy packets to different paths based on the type of content they have.
        final Edge<?> edge;
        edge = getContainer().existingEdgeBetween(packet.getSourceAddress(), packet.getDestAddress());
        getContainer().addPacket(edge, packet);
        return null;
    }

    @Override
    public String getName() {
        return NAME;
    }
}
