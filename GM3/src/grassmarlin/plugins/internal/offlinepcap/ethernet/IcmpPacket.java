package grassmarlin.plugins.internal.offlinepcap.ethernet;

import grassmarlin.plugins.internal.offlinepcap.PacketDataWrapper;
import grassmarlin.session.pipeline.IIcmpPacketData;
import grassmarlin.session.pipeline.IIcmpPacketMetadata;
import grassmarlin.session.pipeline.IPacketData;

import java.nio.ByteBuffer;

public class IcmpPacket extends PacketDataWrapper implements IIcmpPacketMetadata, IIcmpPacketData {
    protected final ByteBuffer bufIcmpPayload;
    protected final int offsetIcmpHeader;

    public IcmpPacket(final IPacketData packet, final int idxIcmpHeaderStart) {
        super(packet);

        this.offsetIcmpHeader = idxIcmpHeaderStart;
        this.bufIcmpPayload = packet.getContents().duplicate();
        this.bufIcmpPayload.position(this.bufIcmpPayload.position() + idxIcmpHeaderStart + 8);  //Icmp header is a fixed length of 8 bytes
    }

    @Override
    public ByteBuffer getIcmpContents() {
        return this.bufIcmpPayload;
    }

    @Override
    public int getIcmpType() {
        return 0x000000FF & (int)this.bufIcmpPayload.get(this.offsetIcmpHeader);
    }
    @Override
    public int getIcmpCode() {
        return 0x000000FF & (int)this.bufIcmpPayload.get(this.offsetIcmpHeader + 1);
    }
}
