package grassmarlin.plugins.internal.offlinepcap.serial;

import grassmarlin.plugins.IPlugin;
import grassmarlin.session.ImportItem;

import java.nio.ByteBuffer;
import java.util.concurrent.BlockingQueue;

public class RtacHandler implements IPlugin.IPacketHandler {
    private final ImportItem source;
    private final BlockingQueue<Object> queue;

    public RtacHandler(final ImportItem source, final BlockingQueue<Object> packetQueue) {
        this.source = source;
        this.queue = packetQueue;
    }

    @Override
    public int handle(final ByteBuffer bufPacket, final long msSinceEpoch, final int idxFrame) {
        //TODO: RTAC parsing--need examples, support modbus and DNP3, noting that there are other unsupported formats.
        throw new UnsupportedOperationException();
    }
}
