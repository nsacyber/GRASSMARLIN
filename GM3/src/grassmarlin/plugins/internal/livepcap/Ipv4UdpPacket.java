package grassmarlin.plugins.internal.livepcap;

import grassmarlin.session.IAddress;
import grassmarlin.session.ImportItem;
import grassmarlin.session.logicaladdresses.IHasPort;
import grassmarlin.session.pipeline.IUdpPacketData;
import grassmarlin.session.pipeline.LogicalAddressMapping;

import java.nio.ByteBuffer;

public class Ipv4UdpPacket implements IUdpPacketData {
    private final ImportItem item;
    private final long size;
    private final long idxFrame;
    private final long tsRecorded;
    private final short protocol;
    private final LogicalAddressMapping source;
    private final LogicalAddressMapping destination;
    private final int ttl;
    private final int ethertype;
    private final ByteBuffer bufPacket;
    private final ByteBuffer bufPayload;

    public Ipv4UdpPacket(
            final ImportItem item,
            final long size,
            final long idxFrame,
            final LogicalAddressMapping source,
            final LogicalAddressMapping destination,
            final int ttl,
            final int ethertype,
            final ByteBuffer bufPacket,
            final ByteBuffer bufPayload
    ) {
        this.item = item;
        this.size = size;
        this.idxFrame = idxFrame;
        this.tsRecorded = System.currentTimeMillis();
        this.protocol = 17;
        this.source = source;
        this.destination = destination;
        this.ttl = ttl;
        this.ethertype = ethertype;

        this.bufPacket = bufPacket;
        this.bufPayload = bufPayload;
    }


    //IPacketMetadata
    @Override
    public Long getFrame() {
        return this.idxFrame;
    }
    @Override
    public long getTime() {
        return this.tsRecorded;
    }
    @Override
    public short getTransportProtocol() {
        return this.protocol;
    }
    @Override
    public IAddress getSourceAddress() {
        return this.source;
    }
    @Override
    public IAddress getDestAddress() {
        return this.destination;
    }
    @Override
    public int getTtl() {
        return this.ttl;
    }
    @Override
    public int getEtherType() {
        return this.ethertype;
    }
    @Override
    public long getPacketSize() {
        return this.size;
    }

    //IUdpPacketMetadata
    @Override
    public int getSourcePort() {
        if(this.source.getLogicalAddress() instanceof IHasPort) {
            return ((IHasPort) this.source.getLogicalAddress()).getPort();
        } else {
            return -1;
        }
    }
    @Override
    public int getDestinationPort() {
        if(this.destination.getLogicalAddress() instanceof IHasPort) {
            return ((IHasPort)this.destination.getLogicalAddress()).getPort();
        } else {
            return -1;
        }
    }

    //IPacketData
    @Override
    public ByteBuffer getContents() {
        return this.bufPacket;
    }

    //IUdpPacketData
    @Override
    public ByteBuffer getUdpContents() {
        return this.bufPayload;
    }

    //IDeferredProgress - this isn't really needed for live pcap
    @Override
    public ImportItem getImportSource() {
        return this.item;
    }
    @Override
    public long getImportProgress() {
        return 0;
    }
}
