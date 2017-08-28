package grassmarlin.plugins.internal.offlinepcap;

import grassmarlin.session.IAddress;
import grassmarlin.session.ImportItem;
import grassmarlin.session.pipeline.IPacketData;
import grassmarlin.session.pipeline.IPacketMetadata;

import java.nio.ByteBuffer;

public class PacketDataWrapper implements IPacketMetadata, IPacketData {
    protected final IPacketData packet;

    protected PacketDataWrapper(IPacketData packet) {
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
    public IAddress getSourceAddress() {
        return packet.getSourceAddress();
    }
    @Override
    public IAddress getDestAddress() {
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
