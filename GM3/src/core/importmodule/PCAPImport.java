/*
 *  Copyright (C) 2011, 2012
 *  This file is part of GRASSMARLIN.
 */
package core.importmodule;

import core.exec.StoringTask;
import core.exec.UDPTask;
import core.fingerprint.ProxyBuffer;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.lang3.ArrayUtils;
import org.jnetpcap.Pcap;
import org.jnetpcap.PcapBpfProgram;
import org.jnetpcap.PcapDumper;
import org.jnetpcap.nio.JBuffer;
import org.jnetpcap.packet.JPacket;
import org.jnetpcap.packet.JPacketHandler;
import org.jnetpcap.protocol.lan.Ethernet;
import org.jnetpcap.protocol.network.Arp;
import org.jnetpcap.protocol.network.Ip4;
import org.jnetpcap.protocol.tcpip.Tcp;
import org.jnetpcap.protocol.tcpip.Udp;
import ui.GrassMarlin;

/**
 * Imports offline PCAP files.
 */
public class PCAPImport extends ImportItem {

    /**
     * Filled with error messages from native JNetPcap code.
     */
    protected final StringBuilder errorBuffer;
    /**
     * Error message for a failed PCAP handle, this can be caused by a
     * non-accessible file
     */
    private static final String HANDLE_FAILED = "FAILED TO SET PCAP HANDLE";
    /**
     * Error message for a failed PCAP filter, caused when the handle failed to
     * set or the filter expression is invalid
     */
    private static final String FILTER_FAILED = "FAILED TO SET PCAP FILTER";
    /**
     * IANA id for TCP protocol.
     */
    private static final int TCP_ID = 6;
    /**
     * IANA id for UDP protocol.
     */
    private static final int UDP_ID = 17;
    /**
     * UNKNOWN protocol.
     */
    private static final int UNKNOWN_ID = -1;
    /**
     * This is a temporary reference to the PCAP handle, its only purpose is to
     * allow another thread to break the pcap loop.
     */
    private Pcap handleReference;

    protected Long frame;

    /** set false on construction, set true onEnd {@link #onEnd(Pcap)} */
    private boolean end;

    /**
     * Constructor for other PCAP imports (PCAPNG and LIVE)
     *
     * @param path Path to device or file.
     * @param type Type of import
     * @param initializeWithTrueCanonicalPath will cause the save canonical path
     * to be set.
     */
    protected PCAPImport(String path, Import type, boolean initializeWithTrueCanonicalPath) {
        super(path, type, initializeWithTrueCanonicalPath);
        errorBuffer = new StringBuilder();
        frame = 0L;
        end = false;
    }

    /**
     * Initialize with {@link #getSafeCanonicalPath() }.
     *
     * @param path Path to .pcap file.
     */
    public PCAPImport(String path) {
        this(path, Import.Pcap, true);
    }

    /**
     * Retrieves a new PCAP handle. May return null if JNetPCAP is not
     * available.
     *
     * @param pathOrDevice The path to the file to open an offline handle to.
     * @return Pcap handle to the pcap file that belongs to this ImportItem.
     */
    protected Pcap getHandle(String pathOrDevice) {
        Pcap handle = null;

        try {
            handle = Pcap.openOffline(pathOrDevice, errorBuffer);
            if( handle == null || errorBuffer.length() > 0 ) {
                String msg = errorBuffer.toString();
                errorBuffer.setLength(0);
                throw new java.lang.IllegalArgumentException(msg);
            }
        } catch (UnsatisfiedLinkError err) {
            Logger.getLogger(PCAPImport.class.getName()).log(Level.SEVERE, "Importing PCAP is disabled.", err);
        } catch( IllegalArgumentException ex ) {
            Logger.getLogger(PCAPImport.class.getName()).log(Level.SEVERE, "Failed to import. Reason: "+ex.getMessage(), ex);
        }

        return handle;
    }

    private boolean filterIsPresent() {
        return this.getImporter().getPreferences().filterIsSet;
    }

    private PcapBpfProgram getFilter(Pcap pcap) {
        if (pcap == null) {
            return null;
        }

        String filterString = getImporter().getPreferences().getFilterString();
        int optimize = getImporter().getPreferences().optimize;
        int netmask = getImporter().getPreferences().netmask;
        PcapBpfProgram program = null;

        try {

            program = new PcapBpfProgram();
            int result = pcap.compile(program, filterString, optimize, netmask);

            if (result == Pcap.OK) {
                program = null;
            }

        } catch (UnsatisfiedLinkError err) {
            Logger.getLogger(PCAPImport.class.getName()).log(Level.SEVERE, "Importing PCAP is disabled.", err);
        }

        return program;
    }

    /**
     * Called before the PCAP loop is entered.
     *
     * @param pcap Reference to the PCAP handle used.
     * @return if true import will continue, else canceled when false.
     */
    protected boolean beforeStart(Pcap pcap) {
        this.end = false;
        GrassMarlin.window.addIndicator(this::indicate);
        return true;
    }

    private String indicate() {
        String msg = "";
        if( !end && this.frame != 0 ) {
            msg = "Frames ".concat(this.frame.toString());
        } else if( end ) {
            msg = "";
        } else {
            msg = "Starting new import...";
        }
        return msg;
    }

    /**
     * Called after the PCAP loop exits.
     *
     * @param pcap Reference to the PCAP handle used.
     */
    protected void onEnd(Pcap pcap) {
        this.end = true;
    }

    public PcapDumper getPcapDumper() {
        throw new java.lang.UnsupportedOperationException("PCAPImport.java is for offline .pcap files only.");
    }

    public boolean saveToDumpfile() {
        return false;
    }

    @Override
    public void run() {
        /* pcap handle for interfacing with the pcap loop. */
        Pcap pcap = null;
        PcapBpfProgram filter = null;
        int captureLimit = getImporter().getPreferences().captureLimit;

        if ((pcap = getHandle(getSafeCanonicalPath())) != null) {

            if (filterIsPresent()) {
                if ((filter = getFilter(pcap)) == null) {
                    fail(FILTER_FAILED);
                }
            }

            if( !beforeStart(pcap) ) {
                handleReference = null;
                onEnd(pcap);
                return;
            }

            handleReference = pcap;

            if (getImporter().fingerprintsAvailable()) {
                pcap.loop(captureLimit, newWithFingerprintOffline(), this);
            } else {
                pcap.loop(captureLimit, newNoFingerprintOffline(), this);
            }

            handleReference = null;

            onEnd(pcap);

        } else {
            fail(HANDLE_FAILED);
        }
    }

    @Override
    public void cancel() {
        if( handleReference != null ) {
            this.handleReference.breakloop();
        }
    }

    private JPacketHandler<PCAPImport> newWithFingerprintOffline() {
        return new JPacketHandler<PCAPImport>() {
            //<editor-fold defaultstate="collapsed" desc="JpacketHandler for offline pcap reading that does produce fingerprint tasks">
            final Ethernet eth = new Ethernet();
            final Ip4 ipv4 = new Ip4();
            final Arp arp = new Arp();
            final Tcp tcp = new Tcp();
            final Udp udp = new Udp();

            @Override
            public void nextPacket(JPacket p, PCAPImport item) {
                item.frame++;
                if (!p.hasHeader(ipv4)) {
                    return;
                }
                if (!p.hasHeader(eth)) {
                    return;
                }

                // copy of mac(eth) and ip(ipv4) so the buffer can be freed ASAP when we don't recognize the proto
                Byte[] dst_mac = Arrays.copyOf(ArrayUtils.toObject(eth.destination()), 6);
                Byte[] src_mac = Arrays.copyOf(ArrayUtils.toObject(eth.source()), 6);
                Byte[] dst_ip = Arrays.copyOf(ArrayUtils.toObject(ipv4.destination()), 4);
                Byte[] src_ip = Arrays.copyOf(ArrayUtils.toObject(ipv4.source()), 4);

                StoringTask store = new StoringTask(item);
                store.frame = p.getFrameNumber();
                store.size = p.size();
                store.time = p.getCaptureHeader().timestampInMillis();

                if (p.hasHeader(tcp)) {
                    store.proto = PCAPImport.TCP_ID;

                    UDPTask t = new UDPTask(item, store);
                    t.proto = PCAPImport.TCP_ID; // JNetPcap is not compliant with IEEE numbers. this is hard coded
                    t.src = tcp.source();
                    t.dst = tcp.destination();
                    t.eth = 2048;
                    t.ttl = ipv4.ttl();
                    t.dsize = p.getPacketWirelen();
                    JBuffer temp = new JBuffer(tcp.getPayloadLength() + 1);
                    p.transferTo(temp, tcp.getPayloadOffset(), tcp.getPayloadLength(), 0);
                    t.payload = new ProxyBuffer(temp);
                    store.setMappingData(src_ip, dst_ip, src_mac, dst_mac, t.src, t.dst);

                } else if (p.hasHeader(udp)) {
                    store.proto = PCAPImport.UDP_ID;

                    UDPTask t = new UDPTask(item, store);
                    t.proto = PCAPImport.UDP_ID; // JNetPcap is not compliant with IEEE numbers. this is hard coded
                    t.src = udp.source();
                    t.dst = udp.destination();
                    t.eth = 2048;
                    t.ttl = ipv4.ttl();
                    t.dsize = p.getPacketWirelen();

                    JBuffer temp = new JBuffer(udp.getPayloadLength() + 1);
                    p.transferTo(temp, udp.getPayloadOffset(), udp.getPayloadLength(), 0);
                    t.payload = new ProxyBuffer(temp);
                    store.setMappingData(src_ip, dst_ip, src_mac, dst_mac, t.src, t.dst);

                } else {
                    store.proto = PCAPImport.UNKNOWN_ID;
                    store.setMappingData(src_ip, dst_ip, src_mac, dst_mac, -1, -1);
                }

                if (item.saveToDumpfile()) {
                    item.getPcapDumper().dump(p);
                }

                item.getImporter().run(store);
            }
            //</editor-fold>
        };
    }

    private JPacketHandler<PCAPImport> newNoFingerprintOffline() {
        return new JPacketHandler<PCAPImport>() {
            //<editor-fold defaultstate="collapsed" desc="JPacket handler for offline pcap reading without producing fingerprinting tasks">

            final Ethernet eth = new Ethernet();
            final Ip4 ipv4 = new Ip4();
            final Arp arp = new Arp();
            final Tcp tcp = new Tcp();
            final Udp udp = new Udp();

            @Override
            public void nextPacket(JPacket p, PCAPImport item) {
                item.frame++;
                if (!p.hasHeader(ipv4)) {
                    return;
                }
                if (!p.hasHeader(eth)) {
                    return;
                }

                // copy of mac(eth) and ip(ipv4) so the buffer can be freed ASAP when we don't recognize the proto
                Byte[] dst_mac = Arrays.copyOf(ArrayUtils.toObject(eth.destination()), 6);
                Byte[] src_mac = Arrays.copyOf(ArrayUtils.toObject(eth.source()), 6);
                Byte[] dst_ip = Arrays.copyOf(ArrayUtils.toObject(ipv4.destination()), 4);
                Byte[] src_ip = Arrays.copyOf(ArrayUtils.toObject(ipv4.source()), 4);

                StoringTask store = new StoringTask(item);
                store.frame = p.getFrameNumber();
                store.size = p.size();
                store.time = p.getCaptureHeader().timestampInMillis();

                if (p.hasHeader(tcp)) {
                    store.proto = PCAPImport.TCP_ID;
                    store.setMappingData(src_ip, dst_ip, src_mac, dst_mac, tcp.source(), tcp.destination());
                } else if (p.hasHeader(udp)) {
                    store.proto = PCAPImport.UDP_ID;
                    store.setMappingData(src_ip, dst_ip, src_mac, dst_mac, udp.source(), udp.destination());
                } else {
                    store.proto = PCAPImport.UNKNOWN_ID;
                    store.setMappingData(src_ip, dst_ip, src_mac, dst_mac, -1, -1);
                }

                if (item.saveToDumpfile()) {
                    item.getPcapDumper().dump(p);
                }

                item.getImporter().run(store);
            }
            //</editor-fold>
        };
    }
}
