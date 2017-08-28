package grassmarlin.plugins.internal.offlinepcap.ethernet;

import grassmarlin.plugins.internal.offlinepcap.PacketDataWrapper;
import grassmarlin.session.IAddress;
import grassmarlin.session.logicaladdresses.IHasPort;
import grassmarlin.session.pipeline.IPacketData;
import grassmarlin.session.pipeline.ITcpPacketData;
import grassmarlin.session.pipeline.ITcpPacketMetadata;
import grassmarlin.session.pipeline.LogicalAddressMapping;

import java.nio.ByteBuffer;

public class TcpPacket extends PacketDataWrapper implements ITcpPacketMetadata, ITcpPacketData {
    protected final ByteBuffer bufTcpPayload;
    protected final int offsetTcpHeader;
    protected final LogicalAddressMapping tcpSource;
    protected final LogicalAddressMapping tcpDest;

    public TcpPacket(final IPacketData packet, final int idxTcpHeaderStart, final int cbTcpHeader, final LogicalAddressMapping addrSource, final LogicalAddressMapping addrDest) {
        super(packet);

        this.offsetTcpHeader = idxTcpHeaderStart;
        this.bufTcpPayload = packet.getContents().duplicate();
        this.bufTcpPayload.position(this.bufTcpPayload.position() + idxTcpHeaderStart + cbTcpHeader);

        this.tcpSource = addrSource;
        this.tcpDest = addrDest;
    }

    // == ITcpPacketMetadata
    @Override
    public int getSourcePort() {
        if(this.tcpSource.getLogicalAddress() instanceof IHasPort) {
            return ((IHasPort)this.tcpSource.getLogicalAddress()).getPort();
        } else {
            return -1;
        }
    }
    @Override
    public int getDestinationPort() {
        if (this.tcpDest.getLogicalAddress() instanceof IHasPort) {
            return ((IHasPort)this.tcpDest.getLogicalAddress()).getPort();
        } else {
            return -1;
        }
    }

    @Override
    public long getAck() {
        return (long)packet.getContents().getInt(packet.getContents().position() + offsetTcpHeader + 8) & 0x00000000FFFFFFFFL;
    }
    @Override
    public int getMss() {
        throw new IllegalArgumentException("TcpPacket.getMss is not yet implemented.");
    }
    @Override
    public long getSeqNum() {
        return (long)packet.getContents().getInt(packet.getContents().position() + offsetTcpHeader + 4) & 0x00000000FFFFFFFFL;
    }
    @Override
    public int getWindowNum() {
        //HACK: This is actually windowSize...  but history says we call it this.
        return (int)packet.getContents().getShort(packet.getContents().position() + offsetTcpHeader + 14) & 0x0000FFFF;
    }
    @Override
    public boolean hasFlag(TcpFlags flag) {
        return ((packet.getContents().getShort(packet.getContents().position() + offsetTcpHeader + 12) & 0x01FF) & flag.getValue()) == flag.getValue();
    }

    // == ITcpPacketData
    @Override
    public ByteBuffer getTcpContents() {
        return bufTcpPayload;
    }

    // == IPacketMetadata
    @Override
    public IAddress getSourceAddress() {
        return this.tcpSource;
    }
    @Override
    public IAddress getDestAddress() {
        return this.tcpDest;
    }

    @Override
    public String toString() {
        return String.format("TCP %s -> %s", tcpSource, tcpDest);
    }
}
