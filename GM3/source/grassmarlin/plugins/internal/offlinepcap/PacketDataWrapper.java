package grassmarlin.plugins.internal.offlinepcap;

import grassmarlin.session.ImportItem;
import grassmarlin.session.pipeline.ILogicalPacketData;
import grassmarlin.session.pipeline.ILogicalPacketMetadata;
import grassmarlin.session.pipeline.LogicalAddressMapping;

import java.nio.ByteBuffer;

public class PacketDataWrapper implements ILogicalPacketMetadata, ILogicalPacketData {
    protected final ILogicalPacketData packet;

    protected PacketDataWrapper(ILogicalPacketData packet) {
        this.packet = packet;
    }

    // == IDeferredProgress
    @Override
    public ImportItem getImportSource() {
        return packet.getImportSource();
    }
    @Override
    public long getImportProgress() {
        return packet.getImportProgress();
    }


    // == IPacketMetadata
    @Override
    public Long getFrame() {
        return packet.getFrame();
    }
    @Override
    public long getTime() {
        return packet.getTime();
    }
    @Override
    public short getTransportProtocol() {
        return packet.getTransportProtocol();
    }
    @Override
    public LogicalAddressMapping getSourceAddress() {
        return packet.getSourceAddress();
    }
    @Override
    public LogicalAddressMapping getDestAddress() {
        return packet.getDestAddress();
    }
    @Override
    public int getTtl() {
        return packet.getTtl();
    }
    @Override
    public int getEtherType() {
        return packet.getEtherType();
    }
    @Override
    public long getPacketSize() {
        return packet.getPacketSize();
    }

    // == IPacketData
    @Override
    public ByteBuffer getContents() {
        return packet.getContents();
    }

}
