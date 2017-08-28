package grassmarlin.plugins.internal.livepcap;

import grassmarlin.Logger;
import grassmarlin.RuntimeConfiguration;
import grassmarlin.session.HardwareAddress;
import grassmarlin.session.ImportItem;
import grassmarlin.session.Session;
import grassmarlin.session.hardwareaddresses.Mac;
import grassmarlin.session.logicaladdresses.Ipv4;
import grassmarlin.session.logicaladdresses.Ipv4WithPort;
import grassmarlin.session.pipeline.IPacketMetadata;
import grassmarlin.session.pipeline.LogicalAddressMapping;
import javafx.beans.binding.NumberExpression;
import javafx.beans.property.ReadOnlyDoubleWrapper;
import org.jnetpcap.Pcap;
import org.jnetpcap.PcapBpfProgram;
import org.jnetpcap.PcapDumper;
import org.jnetpcap.packet.PcapPacket;
import org.jnetpcap.protocol.lan.Ethernet;
import org.jnetpcap.protocol.network.Ip4;
import org.jnetpcap.protocol.tcpip.Tcp;
import org.jnetpcap.protocol.tcpip.Udp;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;

public class LivePcapImport extends ImportItem {
    private final PcapEngine.Device device;
    private final Pcap pcap;
    private final PcapDumper dumper;

    //Options
    //TODO: Make these configurable in some way
    private final int snapLen = 0;
    private final int mode = -1;
    private final int timeout = 30000;
    private final StringBuilder errors = new StringBuilder();

    //State
    private final Map<HardwareAddress, HardwareAddress> addressesHardware;
    private final Map<LogicalAddressMapping, LogicalAddressMapping> addressMappings;
    private final Map<Session.AddressPair, Session.AddressPair> addressPairs;

    protected final Ethernet eth = new Ethernet();
    protected final Ip4 ip4 = new Ip4();
    protected final Tcp tcp = new Tcp();
    protected final Udp udp = new Udp();

    /**
     * Auto-create a path for the dump file.
     * @param entryPoint
     * @param device
     */
    public LivePcapImport(final String entryPoint, final PcapEngine.Device device) {
        this(PcapEngine.getDumpFileName(device), entryPoint, device, true);
    }

    public LivePcapImport(final Path pathDumpFile, final String entryPoint, final PcapEngine.Device device, final boolean createDumpFile) {
        super(pathDumpFile, entryPoint);

        this.addressesHardware = new HashMap<>();
        this.addressMappings = new HashMap<>();
        this.addressPairs = new HashMap<>();

        this.device = device;

        try {
            this.size.set(Files.size(pathDumpFile));
        } catch (IOException ex) {
            this.size.set(-1);
        }

        this.pcap = Pcap.openLive(device.getDevice().getName(), this.snapLen, this.mode, this.timeout, this.errors);
        if(createDumpFile) {
            this.dumper = this.pcap.dumpOpen(pathDumpFile.toString());
        } else {
            this.dumper = null;
        }

        this.importerPluginNameProperty().set(RuntimeConfiguration.pluginNameFor(Plugin.class));
        this.importerFunctionNameProperty().set(Plugin.wrapperLivePcap.getName());
    }

    public PcapEngine.Device getDevice() {
        return this.device;
    }

    @Override
    public NumberExpression progressProperty() {
        return new ReadOnlyDoubleWrapper(-1.0);
    }

    @Override
    public void recordProgress(final long progress) {
        try {
            this.size.set(Files.size(path));
        } catch (IOException ex) {
            this.size.set(-1);
        }
    }

    public void start(final String textFilter, BlockingQueue<Object> queue) {
        // Set up filtering
        if (textFilter != null && !textFilter.isEmpty()) {
            final PcapBpfProgram progFilter = new PcapBpfProgram();

            //TODO: 4th parameter appears to be a mask of some form, not sure what is expected or if it is necessary.
            if (pcap.compile(progFilter, textFilter, 1, -1) != Pcap.OK) {
                Logger.log(Logger.Severity.WARNING, "Unable to initialize PCAP filter for '%s' (%s).  Filtering will not be performed.", textFilter, pcap.getErr());
            } else {
                pcap.setFilter(progFilter);
                Logger.log(Logger.Severity.INFORMATION, "Using PCAP filter '%s'", textFilter);
            }
        }

        final Thread thread_pcap = new Thread(() -> {
            LivePcapImport.this.importStartedProperty().set(true);
            LivePcapImport.this.pcap.loop(Pcap.LOOP_INFINITE, this::handle_Packet, queue);
        });
        thread_pcap.setDaemon(true);
        thread_pcap.start();
    }

    public void stop() {
        this.pcap.breakloop();
        this.pcap.close();
        this.importCompleteProperty().set(true);
    }

    private void handle_Packet(final PcapPacket jPacket, final BlockingQueue<Object> queue) {
        dumper.dump(jPacket);

        if(!jPacket.hasHeader(ip4) || !jPacket.hasHeader(eth)) {
            return;
        }

        //Get MACs from Ethernet header
        final HardwareAddress macSrc = checkHardwareAddress(new Mac(eth.source()), queue);
        final HardwareAddress macDest = checkHardwareAddress(new Mac(eth.destination()), queue);
        reportHardwareLink(macSrc, macDest, queue);

        final Ipv4 ipSrc = new Ipv4(new BigInteger(1, ip4.source()).longValue());
        final Ipv4 ipDest = new Ipv4(new BigInteger(1, ip4.destination()).longValue());

        checkAddressMapping(new LogicalAddressMapping(macSrc, ipSrc), queue);
        checkAddressMapping(new LogicalAddressMapping(macDest, ipDest), queue);

        // Store the entire packet in a ByteBuffer
        final ByteBuffer bufPacket = ByteBuffer.allocate(jPacket.size());
        jPacket.transferTo(bufPacket);

        if(jPacket.hasHeader(tcp)) {
            final Ipv4WithPort endpointSource = new Ipv4WithPort.Ipv4WithTcpPort(ipSrc, tcp.source());
            final Ipv4WithPort endpointDestination = new Ipv4WithPort.Ipv4WithTcpPort(ipDest, tcp.destination());
            final LogicalAddressMapping mappingSource = new LogicalAddressMapping(macSrc, endpointSource);
            final LogicalAddressMapping mappingDestination = new LogicalAddressMapping(macDest, endpointDestination);
            checkAddressMapping(mappingSource, queue);
            checkAddressMapping(mappingDestination, queue);

            final ByteBuffer bufPayload = bufPacket.duplicate();
            bufPayload.position(tcp.getPayloadOffset());
            bufPayload.limit(bufPayload.position() + tcp.getPayloadLength());

            //TODO: packet.getCaptureHeader().timestampInMillis() for the timestamp?
            reportPacket(new Ipv4TcpPacket(
                    this,
                    jPacket.size(),
                    jPacket.getFrameNumber(),
                    mappingSource,
                    mappingDestination,
                    ip4.ttl(),
                    2048,
                    bufPacket,
                    bufPayload,
                    tcp.ack(),
                    -1, //TODO: Restore line: tcp.hasSubHeader(mssHeader) ? mssHeader.mss() : -1,
                    tcp.seq(),
                    tcp.windowScaled(),
                    (short)tcp.flags()
            ), queue);
        } else if(jPacket.hasHeader(udp)) {
            final Ipv4WithPort endpointSource = new Ipv4WithPort.Ipv4WithUdpPort(ipSrc, udp.source());
            final Ipv4WithPort endpointDestination = new Ipv4WithPort.Ipv4WithUdpPort(ipDest, udp.destination());
            final LogicalAddressMapping mappingSource = new LogicalAddressMapping(macSrc, endpointSource);
            final LogicalAddressMapping mappingDestination = new LogicalAddressMapping(macDest, endpointDestination);
            checkAddressMapping(mappingSource, queue);
            checkAddressMapping(mappingDestination, queue);

            final ByteBuffer bufPayload = bufPacket.duplicate();
            bufPayload.position(udp.getPayloadOffset());
            bufPayload.limit(bufPayload.position() + udp.getPayloadLength());

            //TODO: packet.getCaptureHeader().timestampInMillis() for the timestamp?
            reportPacket(new Ipv4UdpPacket(
                    this,
                    jPacket.size(),
                    jPacket.getFrameNumber(),
                    mappingSource,
                    mappingDestination,
                    ip4.ttl(),
                    2048,
                    bufPacket,
                    bufPayload
            ), queue);
        }
    }

    //Reporting support methods
    protected final HardwareAddress checkHardwareAddress(final HardwareAddress address, final BlockingQueue<Object> packetQueue) {
        final HardwareAddress existing = addressesHardware.get(address);
        if(existing != null) {
            return existing;
        } else{
            addressesHardware.put(address, address);
            try {
                packetQueue.put(address);
            } catch(InterruptedException ex) {
                //Ignore it.
            }
            return address;
        }
    }
    protected final LogicalAddressMapping checkAddressMapping(final LogicalAddressMapping mapping, final BlockingQueue<Object> packetQueue) {
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
    protected final void reportHardwareLink(final HardwareAddress src, final HardwareAddress dest, final BlockingQueue<Object> packetQueue) {
        final Session.AddressPair pair = new Session.AddressPair(src, dest);
        final Session.AddressPair existing = addressPairs.get(pair);
        if(existing == null) {
            addressPairs.put(pair, pair);
            try {
                packetQueue.put(pair);
            } catch(InterruptedException ex) {
                //Ignore it.
            }
        }
    }
    protected final void reportPacket(final IPacketMetadata packet, final BlockingQueue<Object> packetQueue) {
        final Session.AddressPair pair = new Session.AddressPair(packet.getSourceAddress(), packet.getDestAddress());
        final Session.AddressPair existing = addressPairs.get(pair);
        if(existing == null) {
            addressPairs.put(pair, pair);
            try {
                packetQueue.put(pair);
            } catch(InterruptedException ex) {
                //Ignore it.
            }
        }
        try {
            packetQueue.put(packet);
        } catch(InterruptedException ex) {
            //Ignore it.
        }
    }
}
