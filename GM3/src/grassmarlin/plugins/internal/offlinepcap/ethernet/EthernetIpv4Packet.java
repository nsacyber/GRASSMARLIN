package grassmarlin.plugins.internal.offlinepcap.ethernet;

import grassmarlin.session.IAddress;
import grassmarlin.session.ImportItem;
import grassmarlin.session.pipeline.IPacketData;
import grassmarlin.session.pipeline.IPacketMetadata;

import java.nio.ByteBuffer;

public class EthernetIpv4Packet implements IPacketData, IPacketMetadata {
    protected final int cbEthernetHeader;
    protected final ImportItem source;
    protected final long idxFrame;
    protected final long msTime;
    protected final IAddress addrSource;
    protected final IAddress addrDestination;
    protected final long cbSize;
    protected final ByteBuffer payloadEthernet;

    /**
     * This holds onto the ByteBuffer that is passed; it has to be COPIED from the ByteBuffer that is handed around in PacketHandler; .duplicate() is insufficient, the underlying byte array has to be copied into a new array.
     * The efficiency on this is awful, but the buffer will be reused.
     * The contents can be a superset of and reference the same byte array the buffers for higher level protocols use.
     * @param source
     * @param idxFrame
     * @param msTime
     * @param addrSource
     * @param addrDestination
     * @param size
     * @param contents
     */
    public EthernetIpv4Packet(
            final ImportItem source,
            final long idxFrame,
            final long msTime,
            final IAddress addrSource,
            final IAddress addrDestination,
            final long size,
            final ByteBuffer contents) {
        this.source = source;
        this.idxFrame = idxFrame;
        this.msTime = msTime;

        this.addrSource = addrSource;
        this.addrDestination = addrDestination;
        this.cbSize = size;
        //TODO: This can be 18 or 22; need to do more in-depth inspection
        this.cbEthernetHeader = 14;

        this.payloadEthernet = contents;
    }

    // == IDeferredProgress
    @Override
    public ImportItem getImportSource() {
        return this.source;
    }
    @Override
    public long getImportProgress() {
        // There is an ede case not accounted for here.
        // We treat the packet data as deferred progress but, while the amount of deferred progress will be accurate, the pcap file might contain less data than the original packet held.  If the packet was truncated for the pcap file, then the size data will be wrong
        return this.cbSize;
    }

    // == IPacketMetadata
    @Override
    public Long getFrame() {
        return this.idxFrame == -1 ? null : idxFrame;
    }
    @Override
    public long getTime() {
        return this.msTime;
    }
    @Override
    public short getTransportProtocol() {
        return (short)payloadEthernet.get(payloadEthernet.position() + this.cbEthernetHeader + 9);
    }
    @Override
    public IAddress getSourceAddress() {
        return this.addrSource;
    }
    @Override
    public IAddress getDestAddress() {
        return this.addrDestination;
    }
    @Override
    public int getTtl() {
        return (int)payloadEthernet.get(payloadEthernet.position() + this.cbEthernetHeader + 8) & 0x000000FF;
    }
    @Override
    public long getPacketSize() {
        return this.cbSize;
    }
    @Override
    public int getEtherType() {
        //Always the last 2 bytes of the Ethernet header
        return payloadEthernet.getShort(payloadEthernet.position() + this.cbEthernetHeader - 2);
    }

    // == IPacketData
    @Override
    public ByteBuffer getContents() {
        return this.payloadEthernet;
    }
}
