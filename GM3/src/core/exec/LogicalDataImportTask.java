package core.exec;

import TemplateEngine.Data.FilterData;
import core.importmodule.ImportItem;
import core.types.ByteTreeItem;
import org.jnetpcap.packet.JPacket;

/**
 * A generic importTask for importing any data that results in logical topology.
 * Generally better to run this before fingerprinting tasks occur.
 * 
 * 2015.8.24 - BESTDOG - Removed unused constructor / added docs.
 * 2015.9.9 - BESTDOG - Renamed to LogicalDataImportTask because it doesn't only import data from PCAP
 * @param <T> The type of ImportItems this Task will use.
 */
public abstract class LogicalDataImportTask<T extends ImportItem> extends ImportTask<T> implements FilterData {
    /** Object where the record of some source address is stored. */
    public ByteTreeItem srcNode;
    /** Object where the record of some destination address is stored. */
    public ByteTreeItem dstNode;
    /** Four byte source IP address. */
    public Byte[] src_ip;
    /** Four byte destination IP address. */
    public Byte[] dst_ip;
    /** Six byte source MAC address. */
    public Byte[] src_mac;
    /** Six byte destination MAC address */
    public Byte[] dst_mac;
    /** ACK flag string {@link org.jnetpcap.protocol.tcpip.Tcp#flagsCompactString() } method. */
    public String flags;
    /** The Long value of the ACK field of a TCP packet as a String since Long cannot be used in a switch. */
    public String ack;
    /** The Long value of the SEQ field of a TCP packet as a String since Long cannot be used in a switch. */
    public String seq;
    /** Source port */
    public int src;
    /** destination port */
    public int dst;
    /** Ethertype */
    public int eth;
    /** IPv4 ttl */
    public int ttl;
    /** IANA protocol number, either six or seven */
    public int proto;
    /** The size of the datagram, {@link org.jnetpcap.packet.JPacket#getPacketWirelen() } method. */
    public int dsize;
    /** The TCP window field */
    public int window;
    /** The packet size, used to notify the amount of bytes read for input, {@link org.jnetpcap.packet.JPacket#size() } method. */
    public int size;
    /** The frame from the pcap file, {@link  org.jnetpcap.packet.JPacket#getFrameNumber() } method. */
    public Long frame;
    /** The entire original packet buffer. */
    protected JPacket packet;

    /**
     * Creates a PCAPImportTask with the default fields set.
     * @param importItem
     */
    public LogicalDataImportTask(T importItem) {
        super(importItem);
        this.flags = "";
        this.ack = "";
        this.seq = "";
        this.src = -1;
        this.dst = -1;
        this.eth = -1;
        this.ttl = -1;
        this.proto = -1;
        this.dsize = -1;
        this.window = -1;
        this.size = 0;
    }

    /**
     * Create a new task with the agnostic mapping information.
     * @param parent The previous PCAPImportTask that was processing the {@link core.exec.PCAPImportTask#importItem }.
     */
    public LogicalDataImportTask( LogicalDataImportTask<T> parent ) {
        this(parent.importItem);
        this.size = parent.size;
        this.setMappingData(parent.src_ip, parent.dst_ip, parent.src_mac, parent.dst_mac, parent.src, parent.dst);
    }

    /**
     * The original constructor for the chain of Tasks used to parse a JPacket buffer.
     * @param importItem ImportItem where this data comes from.
     * @param p Packet buffer for original frame.
     */
    public LogicalDataImportTask( T importItem, JPacket p ) {
        this(importItem);
        this.packet = p;
    }

    /**
     * Sets each of the important mapping fields which are required to create topologies of a network.
     * @param src_ip Source IP of this frame.
     * @param dst_ip Destination IP of this frame.
     * @param src_mac Source MAC of this frame.
     * @param dst_mac Destination MAC of this frame.
     * @param src_port Source port of this frame.
     * @param dst_port Destination port of this frame.
     */
    public final void setMappingData( Byte[] src_ip, Byte[] dst_ip, Byte[] src_mac, Byte[] dst_mac, int src_port, int dst_port) {
        this.src_ip = src_ip;
        this.dst_ip = dst_ip;
        this.src_mac = src_mac;
        this.dst_mac = dst_mac;
        this.src = src_port;
        this.dst = dst_port;
    }
  
    public int getSrc(){
        return this.src;
    }
    public int getDst(){
        return this.dst;
    }
    public int getEth(){
        return this.eth;
    }
    public int getTtl(){
        return this.ttl;
    }
    public int getProto(){
        return this.proto;
    }
    public int getDsize(){
        return this.dsize;
    }
    public int getWindow(){
        return this.window;
    }
    public int size(){
        return this.size;
    }
    public Long getFrame(){
        return this.frame;
    }
    public String getFlags(){
        return this.flags;
    }
    public String getAck(){
        return this.ack;
    }
    public String getSeq(){
        return this.seq;
    }

    
}
