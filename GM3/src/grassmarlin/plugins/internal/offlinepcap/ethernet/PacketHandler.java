package grassmarlin.plugins.internal.offlinepcap.ethernet;

import grassmarlin.Logger;
import grassmarlin.plugins.IPlugin;
import grassmarlin.session.*;
import grassmarlin.session.hardwareaddresses.Mac;
import grassmarlin.session.logicaladdresses.Ipv4;
import grassmarlin.session.logicaladdresses.Ipv4WithPort;
import grassmarlin.session.pipeline.IPacketData;
import grassmarlin.session.pipeline.IPacketMetadata;
import grassmarlin.session.pipeline.LogicalAddressMapping;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;

public class PacketHandler implements IPlugin.IPacketHandler {
    private final ImportItem source;
    private final BlockingQueue<Object> packetQueue;

    private final Map<HardwareAddress, HardwareAddress> addressesHardware;
    private final ProxyMac proxyMac = new ProxyMac();
    private final Map<LogicalAddressMapping, LogicalAddressMapping> addressMappings;
    private final Set<Session.AddressPair> hardwareLinks;

    private final HashMap<Integer, byte[]> fragments = new HashMap<>();

    private final boolean enumerateEphemeralPorts;
    private static class ProxyMac extends Mac {
        public ProxyMac() {
            super(new byte[6]);
        }

        public void setValue(final byte[] value) {
            for(int idx = 5; idx >= 0; idx--) {
                //We can't just cast since that would result in a negative number.
                this.getAddress()[idx] = value[idx] & 0xFF;
            }
        }
    }

    public PacketHandler(final ImportItem source, final BlockingQueue<Object> packetQueue, final boolean enumerateEphemeralPorts) {
        this.source = source;
        this.packetQueue = packetQueue;

        this.addressesHardware = new HashMap<>();
        this.addressMappings = new HashMap<>();
        this.hardwareLinks = new HashSet<>();

        this.enumerateEphemeralPorts = enumerateEphemeralPorts;
    }

    protected final HardwareAddress checkMac(final byte[] mac) {
        proxyMac.setValue(mac);
        final HardwareAddress existing = addressesHardware.get(proxyMac);
        if(existing != null) {
            return existing;
        } else{
            final Mac address = new Mac(proxyMac.getAddress());
            addressesHardware.put(address, address);
            try {
                packetQueue.put(address);
            } catch(InterruptedException ex) {
                //Ignore it.
            }
            return address;
        }
    }
    protected final Ipv4WithPort checkIpv4WithPort(final Ipv4WithPort address) {
        if(enumerateEphemeralPorts) {
            return address;
        } else {
            //49152 (0xC000) is the IANA threshold for Ephemeral Ports.
            //Since the default Ephemeral range varies across OSes and patches, coupled with the configurable nature, this is the best we can really do.
            //We could make this configurable, but it would need to be configurable on a per-host basis, and that just isn't going to happen.
            if(address.getPort() >= 1024) {
                return new Ipv4WithEphemeralPort(address);
            } else {
                return address;
            }
        }
    }
    protected final LogicalAddressMapping checkAddressMapping(final LogicalAddressMapping mapping) {
        final LogicalAddressMapping existing = addressMappings.get(mapping);
        if(existing != null) {
            return existing;
        } else {
            addressMappings.put(mapping, mapping);
            try {
                packetQueue.put(mapping);
            } catch(InterruptedException ex) {
                //Ignore it.
            }
            return mapping;
        }
    }
    protected final void reportHardwareLink(final HardwareAddress src, final HardwareAddress dest) {
        final Session.AddressPair pair = new Session.AddressPair(src, dest);
        if(!hardwareLinks.contains(pair)) {
            hardwareLinks.add(pair);
            try {
                packetQueue.put(pair);
            } catch (InterruptedException ex) {
                //Ignore it.
            }
        }
    }
    protected final void reportPacket(final IPacketMetadata packet) {
        try {
            packetQueue.put(packet);
        } catch(InterruptedException ex) {
            //Ignore it.
        }
    }

    public int handle(final ByteBuffer bufPacket, final long msSinceEpoch, final int idxFrame) {
        final ByteBuffer bufRawPacket = bufPacket.duplicate();
        final int cbPacket = bufPacket.limit() - bufPacket.position();

        // == Ethernet Header
        final byte[] macDestination = new byte[6];
        final byte[] macSource = new byte[6];
        for(int idxByte = 0; idxByte < 6; idxByte++) {
            //noinspection PointlessArithmeticExpression
            macDestination[idxByte] = bufPacket.get(bufPacket.position() + 0 + idxByte);
            macSource[idxByte] = bufPacket.get(bufPacket.position() + 6 + idxByte);
        }
        final HardwareAddress hwaddrSource = checkMac(macSource);
        final HardwareAddress hwaddrDest = checkMac(macDestination);
        reportHardwareLink(hwaddrSource, hwaddrDest);

        final int etherType = (0xFF & bufPacket.get(bufPacket.position() + 12)) << 8 | (0xFF & bufPacket.get(bufPacket.position() + 13));
        //TODO: The length is not a given at 14 bytes; there can be up to 2 VLAN fields that change it to 16 or 18.
        bufPacket.position(bufPacket.position() + 14);

        //TODO: Validate this and expand.
        if(etherType == 0x0800) {
            return handleIp(bufPacket, msSinceEpoch, idxFrame, bufRawPacket, hwaddrSource, hwaddrDest);
        } else {
            //Unknown
            //Logger.log(Logger.Severity.INFORMATION, 100, "Ethertype of %d", etherType);
            return 0;
        }
    }

    public int handleIp(final ByteBuffer bufPacket, final long msSinceEpoch, final int idxFrame, final ByteBuffer bufEthernetFrame, final HardwareAddress hwAddrSource, final HardwareAddress hwAddrDest) {
        final byte ipVersionAndHeaderSize = bufPacket.get(bufPacket.position());
        if((ipVersionAndHeaderSize & 0xF0) == 0x40) {
            final int cbHeader = (ipVersionAndHeaderSize & 0x0F) * 4;
            return handleIpv4(bufPacket, msSinceEpoch, idxFrame, bufEthernetFrame, hwAddrSource, hwAddrDest, cbHeader);
        } else if((ipVersionAndHeaderSize & 0xF0) == 0x60) {
            //TODO: Support Ipv6
            return 0;
        } else {
            return 0;
        }
    }

    public int handleIpv4(final ByteBuffer bufPacket, final long msSinceEpoch, final int idxFrame, ByteBuffer bufEthernetFrame, final HardwareAddress hwAddrSource, final HardwareAddress hwAddrDest, final int cbHeader) {
        final ByteBuffer bufPayload;

        final byte protocol = bufPacket.get(bufPacket.position() + 9);
        final LogicalAddress<?> ipSource = new Ipv4(((long)bufPacket.getInt(bufPacket.position() + 12)) & 0x00000000FFFFFFFFL);
        final LogicalAddress<?> ipDest = new Ipv4(((long)bufPacket.getInt(bufPacket.position() + 16)) & 0x00000000FFFFFFFFL);
        final LogicalAddressMapping mappingSourceIpToMac = checkAddressMapping(new LogicalAddressMapping(hwAddrSource, ipSource));
        final LogicalAddressMapping mappingDestIpToMac = checkAddressMapping(new LogicalAddressMapping(hwAddrDest, ipDest));
        final int cbIp = (int)bufPacket.getShort(bufPacket.position() + 2) & 0x0000FFFF;
        final int idxLastIpByte = bufPacket.position() + cbIp;

        final int wFragment = bufPacket.getShort(bufPacket.position() + 6);
        final boolean hasMoreFragments = (wFragment & 0x2000) == 0x2000;
        final int offsetFragment = (wFragment & 0x1FFF) << 3;

        if(hasMoreFragments || offsetFragment != 0) {
            final int idFragment = (int)bufPacket.getShort(bufPacket.position() + 6);
            final int idxLastByteInThisFragment = offsetFragment + cbIp - cbHeader;
            final byte[] bufNew;
            final byte[] bufOld = fragments.get(idFragment);
            if(bufOld != null) {
                //Resize fragment, if needed
                if(bufOld.length < idxLastByteInThisFragment) {
                    bufNew = new byte[idxLastByteInThisFragment];
                    System.arraycopy(bufOld, 0, bufNew, 0, bufOld.length);
                    fragments.put(idFragment, bufNew);
                } else {
                    bufNew = bufOld;
                }
            } else {
                bufNew = new byte[idxLastByteInThisFragment];
                fragments.put(idFragment, bufNew);
            }

            for(int idx = offsetFragment; idx < idxLastByteInThisFragment; idx++) {
                bufNew[idx] = bufPacket.get(bufPacket.position() + cbHeader + idx - offsetFragment);
            }

            //TODO: We need better fragment tracking; if they are observed out-of-order we'll be missing a section from the middle.
            if(hasMoreFragments) {
                //TODO: Determine the correct breakdown in how to handle the progress tracking.
                return 0;
            }

            //We have a complete packet now.  Well, a complete payload...
            //HACK: bufPacket and bufEthernetFrame are known to be related.  bufEthernetFrame was a duplicate of bufPacket, taken before updating the position in bufPacket for various headers.  Therefore the relevant header size is the difference between the two.
            final byte[] bufCompletePacket = new byte[bufPacket.position() - bufEthernetFrame.position() + bufNew.length];
            final int cbPreviousHeaders = bufPacket.position() - bufEthernetFrame.position();
            for(int idx = 0; idx < cbPreviousHeaders; idx++) {
                bufCompletePacket[idx] = bufEthernetFrame.get(bufEthernetFrame.position() + idx);
            }
            System.arraycopy(bufNew, 0, bufCompletePacket, cbPreviousHeaders, bufNew.length);
            bufEthernetFrame = ByteBuffer.wrap(bufCompletePacket);
            bufPayload = bufEthernetFrame.duplicate();
            //TODO: The logic in the else block was wrong with respect to identifying the buffer offsets, so this is probably wrong too, but we lack a test case at the moment.
            bufPayload.position(cbPreviousHeaders);
            fragments.remove(idFragment);
        } else {
            //We need bufPacket and bufPayload to point to new, local byte arrays.
            final byte[] bufCompletePacket = new byte[bufEthernetFrame.limit() - bufEthernetFrame.position()];
            for(int idx = 0; idx < bufCompletePacket.length; idx++) {
                bufCompletePacket[idx] = bufEthernetFrame.get(bufEthernetFrame.position() + idx);
            }
            bufEthernetFrame = ByteBuffer.wrap(bufCompletePacket);
            bufPayload = bufEthernetFrame.duplicate();
            //bufPacket and bufEthernetFrame wrap the same array.
            // the position in bufPacket identifies the start of the IP header
            // the position in bufEthernetFrame identifies the start of the ethernet header.
            //We removed the part of bufEthernetFrame before the ethernet frame, so we shift the start of the payload accordingly, and we otherwise inherit the start of the IP header, then add the size of the IP header.
            bufPayload.position(bufPacket.position() - bufEthernetFrame.position() + cbHeader);
        }

        //TODO: Verify correct size
        EthernetIpv4Packet packetEthernet = new EthernetIpv4Packet(source, idxFrame, msSinceEpoch, mappingSourceIpToMac, mappingDestIpToMac, bufEthernetFrame.limit() - bufEthernetFrame.position(), bufEthernetFrame);
        //Now that we have constructed packetEthernet, we can parse the tcp/udp/whatever payload and report a corresponding packet object.  The packet objects for TCP/UDP/etc. will encapsulate packetEthernet and share its ByteBuffer.
        //At this point, bufPayload MUST reference the same array as the ByteBuffer that was passed to the constructor for packetEthernet (bufEthernetFrame).
        switch(protocol) {
            case 6:
                return handleTcp(packetEthernet, bufPayload, hwAddrSource, hwAddrDest);
            case 17:
                return handleUdp(packetEthernet, bufPayload, hwAddrSource, hwAddrDest);
            case 1:
                return handleIcmp(packetEthernet, bufPayload);
            case 2:
                //TODO: IGMP
            default:
                //Default case is to report whatever we have.
                reportPacket(packetEthernet);
                return (int)packetEthernet.getImportProgress();

        }
    }

    public int handleTcp(final IPacketData packet, final ByteBuffer bufPayload, final HardwareAddress hwAddrSource, final HardwareAddress hwAddrDest) {
        final int portSource = bufPayload.getShort(bufPayload.position()) & 0x0000FFFF;
        final int portDest = bufPayload.getShort(bufPayload.position() + 2) & 0x0000FFFF;

        final IAddress source = packet.getSourceAddress();
        final IAddress destination = packet.getDestAddress();
        final LogicalAddress<?> logicalSource;
        final LogicalAddress<?> logicalDestination;

        if(source instanceof LogicalAddressMapping) {
            final LogicalAddressMapping mappingSource = (LogicalAddressMapping)source;
            if(mappingSource.getLogicalAddress() instanceof Ipv4) {
                logicalSource = checkIpv4WithPort(new Ipv4WithPort.Ipv4WithTcpPort((Ipv4) mappingSource.getLogicalAddress(), portSource));
            } else {
                //TODO: Fail better
                throw new IllegalArgumentException("Unrecognized source address format");
            }
        } else {
            //TODO: Fail better
            throw new IllegalArgumentException("Unrecognized source address format");
        }
        if(destination instanceof LogicalAddressMapping) {
            final LogicalAddressMapping mappingDestination = (LogicalAddressMapping)destination;
            if(mappingDestination.getLogicalAddress() instanceof Ipv4) {
                logicalDestination = checkIpv4WithPort(new Ipv4WithPort.Ipv4WithTcpPort((Ipv4) mappingDestination.getLogicalAddress(), portDest));
            } else {
                //TODO: Fail better
                throw new IllegalArgumentException("Unrecognized source address format");
            }
        } else {
            //TODO: Fail better
            throw new IllegalArgumentException("Unrecognized source address format");
        }

        final LogicalAddressMapping mappingSource = new LogicalAddressMapping(hwAddrSource, logicalSource);
        final LogicalAddressMapping mappingDestination = new LogicalAddressMapping(hwAddrDest, logicalDestination);
        checkAddressMapping(mappingSource);
        checkAddressMapping(mappingDestination);

        final int cbTcpHeaders = 0x3C & (bufPayload.get(bufPayload.position() + 12) >>> 2);

        try {
            reportPacket(new TcpPacket(packet, bufPayload.position(), cbTcpHeaders, mappingSource, mappingDestination));
        } catch(Exception ex) {
            Logger.log(Logger.Severity.ERROR, "Malformed TCP packet: %s:%d", packet.getImportSource().getPath(), packet.getFrame());
            reportPacket(packet);
        }

        return (int)packet.getImportProgress();
    }

    public int handleUdp(final IPacketData packet, final ByteBuffer bufPayload, final HardwareAddress hwAddrSource, final HardwareAddress hwAddrDest) {
        final int portSource = bufPayload.getShort(bufPayload.position()) & 0x0000FFFF;
        final int portDest = bufPayload.getShort(bufPayload.position() + 2) & 0x0000FFFF;

        final IAddress source = packet.getSourceAddress();
        final IAddress destination = packet.getDestAddress();
        final LogicalAddress<?> logicalSource;
        final LogicalAddress<?> logicalDestination;

        if(source instanceof LogicalAddressMapping) {
            final LogicalAddressMapping mappingSource = (LogicalAddressMapping)source;
            if(mappingSource.getLogicalAddress() instanceof Ipv4) {
                logicalSource = checkIpv4WithPort(new Ipv4WithPort.Ipv4WithUdpPort((Ipv4) mappingSource.getLogicalAddress(), portSource));
            } else {
                //TODO: Fail better
                throw new IllegalArgumentException("Unrecognized source address format");
            }
        } else {
            //TODO: Fail better
            throw new IllegalArgumentException("Unrecognized source address format");
        }
        if(destination instanceof LogicalAddressMapping) {
            final LogicalAddressMapping mappingDestination = (LogicalAddressMapping)destination;
            if(mappingDestination.getLogicalAddress() instanceof Ipv4) {
                logicalDestination = checkIpv4WithPort(new Ipv4WithPort.Ipv4WithUdpPort((Ipv4) mappingDestination.getLogicalAddress(), portDest));
            } else {
                //TODO: Fail better
                throw new IllegalArgumentException("Unrecognized source address format");
            }
        } else {
            //TODO: Fail better
            throw new IllegalArgumentException("Unrecognized source address format");
        }

        final LogicalAddressMapping mappingSource = new LogicalAddressMapping(hwAddrSource, logicalSource);
        final LogicalAddressMapping mappingDestination = new LogicalAddressMapping(hwAddrDest, logicalDestination);
        checkAddressMapping(mappingSource);
        checkAddressMapping(mappingDestination);

        try {
            reportPacket(new UdpPacket(packet, bufPayload.position(), mappingSource, mappingDestination));
        } catch(Exception ex) {
            Logger.log(Logger.Severity.ERROR, "Malformed UDP packet: %s:%d", packet.getImportSource().getPath(), packet.getFrame());
            reportPacket(packet);
        }


        return (int)packet.getImportProgress();
    }

    public int handleIcmp(final IPacketData packet, final ByteBuffer bufPayload) {
        // In previous implementations we would only process ping responses.
        // This has changed because we now filter ICMP traffic in the pipeline to prevent the logical graph from processing other ICMP traffic, which still allows the physical graph to extract relevant bits from the other icmp traffic.
        final IcmpPacket icmp = new IcmpPacket(packet, bufPayload.position());
        reportPacket(icmp);
        return (int)packet.getImportProgress();
    }
}
