package iadgov.bro2connparser;

import grassmarlin.Logger;
import grassmarlin.RuntimeConfiguration;
import grassmarlin.session.ImportItem;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Path;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.concurrent.BlockingQueue;

/**
 *  Parses Bro2Conn Log files
 *  rewritten for v3.3 instead of ported from v3.2
 *
 *  This section was developed by an intern with caffeine...
 *  DevHash: 446562202d20434e4f4450204744502032303139
 *
 */

public class Bro2ConnLogParser implements Bro2ConnImport.IBroParser {
    private final ImportItem source;
    private final RuntimeConfiguration config;
    private boolean done;


    public Bro2ConnLogParser(final RuntimeConfiguration config, final ImportItem source) {
        this.source = source;
        this.config = config;
        this.done = false;

        if(!validateFileFormat(this.source.getPath())) {
            throw new IllegalArgumentException("File failed validation.");
        }
    }

    private static boolean validateFileFormat(final Path path) {
        //TODO: Actually implement file validation for Bro2 Conn logs...
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
        } finally {
            return result;
        }
    }

    private static int logPacketMaker(String logLine, BlockingQueue<Object> packetQueue) {
        LogPacket logPacket = null;

        try {
            int packetCount = 0;
            if(logLine == "") {return -1;}
            final String[] tokens = logLine.split("\t");

            if(tokens.length != 21) {
                Logger.log(Logger.Severity.ERROR, "Error! Invalid number of recorded items in log file!; failed on error: logPacketMaker()");
                return -1;
            }

            // time formatting should not be modified from the input logs, this is to better accommodate
            // all different types of bro time reporting
            NumberFormat format = NumberFormat.getInstance(Locale.US);
            Number num = format.parse(tokens[0]);
            long time = num.longValue();
            String uid = tokens[1];
            String src_ip = tokens[2];
            int src_prt = Integer.parseInt(tokens[3]);
            String dst_ip = tokens[4];
            int dst_prt = Integer.parseInt(tokens[5]);
            String proto = tokens[6];

            //Format source and destination bytes sent, if value is '-' or "(empty)", format to be 0
            if(tokens[9].equals("-") || tokens[9].equals("(empty)")) {
                tokens[9] = "0";
            }
            if(tokens[10].equals("-") || tokens[10].equals("(empty)")) {
                tokens[10] = "0";
            }
            int src_byt = Integer.parseInt(tokens[9]);
            int dst_byt = Integer.parseInt(tokens[10]);

            //TODO Track to prevent duplicate packetQueue item reporting
            //IPv4 Packet Implementation
            if(!src_ip.contains(":") && !dst_ip.contains(":")) {
                logPacket = new LogPacket(time, uid, src_ip, src_prt, dst_ip, dst_prt, proto, src_byt, dst_byt, logLine.length());
                try {
                    packetQueue.put(logPacket.getMac());
                    packetQueue.put(logPacket.getLogicalSourceAddress());
                    packetQueue.put(logPacket.getLogicalDestAddress());
                    packetQueue.put(logPacket.getAddressPair());
                } catch (InterruptedException e) {
                    Logger.log(Logger.Severity.ERROR, "Unable to report bro2 conn.log IPv4 packet; failed on error: " + e.toString());
                }

            } else if(src_ip.contains(":") || dst_ip.contains(":")) {
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
                return 0;
            }
        }
    }
}
