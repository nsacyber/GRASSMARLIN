package iadgov.bro2connparser;

import grassmarlin.Logger;
import grassmarlin.RuntimeConfiguration;
import grassmarlin.session.ImportItem;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.concurrent.BlockingQueue;

/*  TODO: Maybe utilize the org.json import to parse through json files (easy mode?)
 *      -- Nah bruh, we'll just make our own
 *
 * import org.json.simple.JSONArray;
 * import org.json.simple.JSONObject;
 * import org.json.simple.JSONParser;
 **/

/**
 *  Parses Bro2Conn Log files
 *  mostly rewritten for v3.3, partially ported from v3.2
 *
 *  This section was developed by an intern with caffeine...
 *  DevHash: 446562202d20434e4f4450204744502032303139
 *
 */

public class Bro2ConnJsonParser implements Bro2ConnImport.IBroParser {
    private final ImportItem source;
    private final RuntimeConfiguration config;
    private boolean done;

    public Bro2ConnJsonParser(final RuntimeConfiguration config, final ImportItem source) {
        this.source = source;
        this.config = config;
        this.done = false;

        if(!validateFileFormat(this.source.getPath())) {
            throw new IllegalArgumentException("File failed validation.");
        }
    }

    //TODO use and track conn_state and history
    public enum conn_state_enum {
        S0,     // Connection attempt seen, no reply
        S1,     // Connection established, not terminated
        SF,     // Normal establishment and termination, will have byte counts
        REJ,    // Connection attempt rejected
        S2,     // Connection established and close attempt by originator seen
        S3,     // Connection established and close attempt by responder seen
        RSTO,   // Connection establishedm originator aborted (sent RST)
        RSTR,   // Responder sent a RST
        RSTOS0, // Originator sent a SYN followed by a RST, no SYN-ACK seen
        RSTRH,  // Responder sent a SYN followed by a RST, no SYN-ACK seen
        SH,     // Originator sent a SYN followed by a FIN, never saw SYN-ACK (half open)
        SHR,    // Responder sent a SYN-ACK followed by a FIN, never saw SYN from originator
        OTH     // no SYN seen, just midstream traffic
    }

    public enum history_enum {
        S,      // SYN bit set
        h,      // SYN-ACK bits set
        a,      // ACK bit set
        d,      // packet payload with data
        f,      // packet with FIN bit set
        r,      // packet with RST bit set
        c,      // packet with bad checksum
        t,      // packet with retransmitted payload
        i,      // inconsistent packet (FIN + RST bits set)
        q       // multi-flag packet (SYN + FIN or SYN + RST bits set)
    }

    private static boolean validateFileFormat(final Path path) {
        //TODO: Actually implement file validation for Bro2 Conn JSON logs...
        if(path != null) { return true; }
        else {
            Logger.log(Logger.Severity.ERROR, "Error! Unable to process import of bro2 conn.log file, path is null!");
            return false;
        }
    }

    @Override
    public int parseFile(final BufferedReader file, BlockingQueue<Object> packetQueue) throws IOException {
        int result = -1;

        try(BufferedReader bufferReader = file) {
            String line;
            while((line = bufferReader.readLine()) != null) {
                if(!line.startsWith("#")) {
                    result = logPacketMaker(line, packetQueue);
                }
            }
            bufferReader.close();
        } catch(IOException ex) {
            Logger.log(Logger.Severity.ERROR, "Unable to process import of bro2 conn.log; failed on error: " + ex.toString());
        } finally { return result; }
    }

    private static int logPacketMaker(String logLine, final BlockingQueue<Object> packetQueue) {
        LogPacket logPacket = null;

        try {
            int packetCount = 0;
            if(logLine == "") {return -1;}
            HashMap<String, Object> lineParsed = JsonParser.readObject(new StringParser(logLine));

            String[] timeParse = ((String)lineParsed.get("ts")).split("\\.");
            String timeString = timeParse[0] + (timeParse.length > 1 ? (timeParse[1] + "000").substring(0,3) : "");
            long time = Long.parseLong(timeString);
            String uid = (String)lineParsed.get("uid");
            String srcIP = (String)lineParsed.get("id.orig_h");
            int srcPort = Integer.parseInt((String)lineParsed.get("id.orig_p"));
            String dstIP = (String)lineParsed.get("id.resp_h");
            int dstPort = Integer.parseInt((String)lineParsed.get("id.resp_p"));
            String proto = (String)lineParsed.get("proto");

            int srcBytes = 0;
            if(lineParsed.containsKey("orig_bytes")) {
                srcBytes = Integer.parseInt((String)lineParsed.get("orig_bytes"));
            }
            int dstBytes = 0;
            if(lineParsed.containsKey("resp_bytes")) {
                dstBytes = Integer.parseInt((String)lineParsed.get("resp_bytes"));
            }

            //TODO Track to prevent duplicate packetQueue item reporting
            if(!srcIP.contains(":") && !dstIP.contains(":")) {
                logPacket = new LogPacket(time, uid, srcIP, srcPort, dstIP, dstPort, proto, srcBytes, dstBytes, logLine.length());
                try {
                    packetQueue.put(logPacket.getMac());
                    packetQueue.put(logPacket.getLogicalSourceAddress());
                    packetQueue.put(logPacket.getLogicalDestAddress());
                    packetQueue.put(logPacket.getAddressPair());
                } catch (InterruptedException e) {
                    Logger.log(Logger.Severity.ERROR, "Unable to report bro2 conn.log JSON packet; failed on error: " + e.toString());
                }
            } else if(srcIP.contains(":") || dstIP.contains(":")){
                //else we have an IPv6 packet, and we don't do those... yet...
                Logger.log(Logger.Severity.INFORMATION, "Processing of IPv6 not currently supported in bro2 conn.log;");
            } else {
                Logger.log(Logger.Severity.ERROR, "Something very bad happened in logPacketMaker(); You should never see this...");
            }
        } catch (Exception e) {
            Logger.log(Logger.Severity.ERROR, "Error making packet in logPacketMaker(); failed on error: " + e.toString());
        } finally {
            if(logPacket != null) {
                return 1;
            } else {
                return -1;
            }
        }
    }
}
