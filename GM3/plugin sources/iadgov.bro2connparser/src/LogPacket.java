package iadgov.bro2connparser;

import grassmarlin.session.ImportItem;
import grassmarlin.session.Session;
import grassmarlin.session.hardwareaddresses.Mac;
import grassmarlin.session.logicaladdresses.Ipv4;
import grassmarlin.session.logicaladdresses.Ipv4WithPort;
import grassmarlin.session.pipeline.ILogicalPacketMetadata;
import grassmarlin.session.pipeline.LogicalAddressMapping;

/**
 *  LogPacket Class to be used with the Bro2Conn Log file parser
 *  mostly rewritten for v3.3, partially ported from v3.2
 *
 *  This section was developed by an intern with caffeine...
 *  DevHash: 446562202d20434e4f4450204744502032303139
 *
 */

public class LogPacket implements ILogicalPacketMetadata {
    private final LogicalAddressMapping srcAddrMap;
    private final LogicalAddressMapping dstAddrMap;
    //TODO check & grab real hardware addresses if they exist in the bro log
    //All Hardware addresses will default to 00:01:01:00:00:00:00:00 "private" to prevent vendor favoritism...
    private final Mac hwAddr = new Mac("00:01:01:00:00:00");
    private final Long Time;
    private final String Uid;
    private final String ipSrc;
    private final int portSrc;
    private final String ipDst;
    private final int portDst;
    private final String proto;
    private final int SRC_BYT;
    private final int DST_BYT;
    private final Session.LogicalAddressPair addressPair;
    private final long lengthLine;

    protected LogPacket(final Long time,
                        final String uid,
                        final String src_ip,
                        final int src_prt,
                        final String dst_ip,
                        final int dst_prt,
                        final String proto,
                        final int src_byt,
                        final int dst_byt,
                        final long lengthLine) {
        this.Time = time;
        this.Uid = uid;
        this.ipSrc = src_ip;
        this.portSrc = src_prt;
        this.ipDst = dst_ip;
        this.portDst = dst_prt;
        this.proto = proto;
        this.SRC_BYT = src_byt;
        this.DST_BYT = dst_byt;
        this.srcAddrMap = new LogicalAddressMapping(hwAddr, genIPv4WithPort(src_ip,src_prt));
        this.dstAddrMap = new LogicalAddressMapping(hwAddr, genIPv4WithPort(dst_ip,dst_prt));
        this.addressPair = new Session.LogicalAddressPair(srcAddrMap, dstAddrMap);
        this.lengthLine = lengthLine;
    }

    public ImportItem getImportSource() { return null;}  //This probably should not be null...
    @Override
    public long getImportProgress() { return this.lengthLine; }     //We report each packet size of 1, for 1 packet/line of log

    @Override
    public Long getFrame() { return null; }    //Result is optional, so this is nullable.
    @Override
    public long getTime() { return this.Time; }
    public short getTransportProtocol() {
        switch(this.proto.toLowerCase()) {
            case ("icmp"):
                return 1;
            case ("tcp"):
                return 6;
            case ("udp"):
                return 17;
            //You might not see these, but they have been included just for fun...
            case ("rdp"):
                return 27;
            case ("gre"):
                return 47;
            case ("ipsec-ah"):
                return 50;
            case ("ipsec-esp"):
                return 51;
            default:
                return 0;
        }
    }

    // Note: These methods are not implicitly declared as package-private
    // This enables them to be accessible to this Class, Package and Subclasses
    Mac getMac() { return hwAddr; }
    int getSourceBytes() { return SRC_BYT; }
    int getDestBytes() { return DST_BYT; }
    Session.LogicalAddressPair getAddressPair() { return addressPair; }
    LogicalAddressMapping getLogicalSourceAddress() { return srcAddrMap; }
    LogicalAddressMapping getLogicalDestAddress() { return dstAddrMap; }

    @Override
    public LogicalAddressMapping getSourceAddress() { return srcAddrMap; }
    @Override
    public LogicalAddressMapping getDestAddress() { return dstAddrMap; }
    @Override
    public int getTtl() { return 128; }      //Bro logging does not track ttl, so...
    @Override
    public int getEtherType() { return 14; } //Copy from internal.offlinepcap.ethernet.EthernetIpv4Packet.java
    @Override
    public long getPacketSize() {
        //There isn't a 1:1 correlation between log lines and packets, so this is the best we can to.
        return this.SRC_BYT + DST_BYT;
    }

    @Override
    public String toString() {
        String output = "";
        output += "Packet\nTime:\t\t" + this.Time + "\n";
        output += "Uid:\t\t" + this.Uid + "\n";
        output += "ipSrc:\t\t" + this.ipSrc + "\n";
        output += "portSrc:\t" + this.portSrc + "\n";
        output += "ipDst:\t\t" + this.ipDst + "\n";
        output += "portDst:\t" + this.portDst + "\n";
        output += "proto:\t\t" + this.proto + "\n";
        output += "SENT_BYT:\t" + this.SRC_BYT + "\n";
        output += "RECV_BYT:\t" + this.DST_BYT + "\n";
        return output;
    }

    public static Ipv4WithPort genIPv4WithPort(final String address, final int port) {
        long value = 0;
        for(String token : address.split("\\.", 4)) {
            long valueToken = Long.parseLong(token);
            value <<= 8;
            value += valueToken;
        }

        Ipv4 src = new Ipv4(value);
        final Ipv4WithPort tempIPv4 = new Ipv4WithPort(src, port);
        return tempIPv4;
    }
}
