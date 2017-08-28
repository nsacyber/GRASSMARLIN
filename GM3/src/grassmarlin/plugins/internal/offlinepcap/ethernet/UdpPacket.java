package grassmarlin.plugins.internal.offlinepcap.ethernet;

import grassmarlin.plugins.internal.offlinepcap.PacketDataWrapper;
import grassmarlin.session.IAddress;
import grassmarlin.session.logicaladdresses.IHasPort;
import grassmarlin.session.pipeline.IPacketData;
import grassmarlin.session.pipeline.IUdpPacketData;
import grassmarlin.session.pipeline.IUdpPacketMetadata;
import grassmarlin.session.pipeline.LogicalAddressMapping;

import java.nio.ByteBuffer;

public class UdpPacket extends PacketDataWrapper implements IUdpPacketData, IUdpPacketMetadata {
    protected final ByteBuffer bufUdpPayload;
    protected final int offsetUdpHeader;
    protected final LogicalAddressMapping udpSource;
    protected final LogicalAddressMapping udpDest;

    public UdpPacket(final IPacketData packet, final int idxUdpHeaderStart, final LogicalAddressMapping addrSource, final LogicalAddressMapping addrDestination) {
        super(packet);
        this.offsetUdpHeader = idxUdpHeaderStart;
        this.bufUdpPayload = packet.getContents().duplicate();
        this.bufUdpPayload.position(this.bufUdpPayload.position() + idxUdpHeaderStart + 8);

        this.udpSource = addrSource;
        this.udpDest = addrDestination;
    }

    // == IUdpPacketMetadata
    @Override
    public int getSourcePort() {
        if(this.udpSource.getLogicalAddress() instanceof IHasPort) {
            return ((IHasPort)this.udpSource.getLogicalAddress()).getPort();
        } else {
            return -1;
        }
    }
    @Override
    public int getDestinationPort() {
        if (this.udpDest.getLogicalAddress() instanceof IHasPort) {
            return ((IHasPort)this.udpDest.getLogicalAddress()).getPort();
        } else {
            return -1;
        }
    }

    // == IUdpPacketdata
    @Override
    public ByteBuffer getUdpContents() {
        return bufUdpPayload;
    }

    // == IPacketMetadata
    @Override
    public IAddress getSourceAddress() {
        return this.udpSource;
    }
    @Override
    public IAddress getDestAddress() {
        return this.udpDest;
    }

    @Override
    public String toString() {
        return String.format("UDP %s -> %s", udpSource, udpDest);
    }
}
