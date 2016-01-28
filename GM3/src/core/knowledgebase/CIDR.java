/*
 *  Copyright (C) 2011, 2012
 *  This file is part of GRASSMARLIN.
 */

package core.knowledgebase;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;

/**
 * A class to represent IPv4 CIDRs.
 * 
 * @author bwallis
 */
public class CIDR implements Comparable<CIDR>, Serializable {

    final static long maxIP = (long)(((long)1 << 32) - 1); // usign the literal 0xffffffff resulted in a -1 in a long variable.
    final static short maxBits = 32;
    
    private long firstIP; // An integer representation of the first IP in our range.
    private short bits;   // The # of subnet bits.
    
    /**
     * Compute the log2 of a given number. This is also the minimum # of bits to
     * store this value. Requires a positive value for an argument.
     * 
     * If there's a better way, please replace this.
     * 
     * @param arg the number for which the log2 is computed
     * @return the log-base-2 of the argument.
     */
    public static int log2(long arg) {
        if(arg <= 0) throw new IllegalArgumentException("log2() requires positive number!");
        return 63 - Long.numberOfLeadingZeros(arg);
    }
    
    private static String toIP(long ip) {        
        StringBuilder sb = new StringBuilder();
        sb.append(0xff & (ip >>> 24))
          .append('.').append(0xff & (ip >>> 16))
          .append('.').append(0xff & (ip >>> 8))
          .append('.').append(0xff & ip);
        
        return sb.toString();
    }
    
    static private long toIP(String ipStr) {
        String[] parts = ipStr.split("\\.");
        if(parts.length != 4)
            throw new IllegalArgumentException(ipStr + " does not represent an IP Address, the IP has " + parts.length + " parts");
            
        long[] ipParts = new long[4];
        for(int i = 0; i < 4; ++i) {
            ipParts[i] = Long.parseLong(parts[i]);
            if((ipParts[i] < 0) || (ipParts[i] > 255))
                throw new IllegalArgumentException(ipStr + " does not represent an IP address, components exist outside of [0:255]");
        }
        
        return (((((ipParts[0] << 8) + ipParts[1]) << 8) + ipParts[2]) << 8) + ipParts[3];
    }
    
    /**
     * Construct a CIDR from the given string.
     * @param s the string form that we expect holds a CIDR.
     * @return a CIDR object
     * @throws IllegalArgumentException if the string can't be parsed as a CIDR.
     */
    static public CIDR valueOf(String s) {
        return new CIDR(s);
    }
    /**
     * Create a CIDR from a string of the form "A.B.C.D" with an option "/N" suffix.
     * @param cidrOrIP the string form we attempt to parse.
     * @throws IllegalArgumentException if the string is malformed.
     */
    public CIDR(String cidrOrIP) {
        String [] parts = cidrOrIP.split("/");
        if((parts.length < 1) || (parts.length > 2))
            throw new IllegalArgumentException(cidrOrIP + " does not represent an IP address or CIDR");
        
        bits = (1 == parts.length) ? 32 : Short.parseShort(parts[1]);
        if((bits < 1) || (bits > 32))
            throw new IllegalArgumentException(cidrOrIP + " does not represent an IP address or ICDR, /" + bits + " is outside of [1:32]");
        
        this.firstIP = toIP(parts[0]);
    }
    
    /**
     * Create a CIDR from the given IP and number-of-network-bits value.
     * @param ip the long integer form of the ip address.
     * @param bits the # of network bits
     */
    public CIDR(long ip, short bits) {
        if(ip < 0)
            throw new IllegalArgumentException("CIDR(" + ip + "/" + bits + ") cannot accept a negative argument!");
        if(ip > maxIP)
            throw new IllegalArgumentException("CIDR(" + ip + "/" + bits + ") cannot accept an argument > " + maxIP + "!");
        if(bits <= 0)
            throw new IllegalArgumentException("CIDR(" + ip + "/" + bits + ") cannot accept a negative # of bits!");
        if(bits > maxBits)
            throw new IllegalArgumentException("CIDR(" + ip + "/" + bits + ") cannot accept a #bits > " + maxBits + "!");
        this.firstIP = ip;
        this.bits = bits;
    }
    
    /**
     * Create a CIDR from a numeric equivalent of an IP address.
     * @param ip the long integer form of the ip address.
     */
    public CIDR(long ip) {
        if(ip < 0)
            throw new IllegalArgumentException("CIDR(" + ip + ") cannot accept a negative argument!");
        if(ip > maxIP)
            throw new IllegalArgumentException("CIDR(" + ip + ") cannot accept an argument > " + maxIP + "!");
        
        this.firstIP = ip;
        this.bits = 32;
    }
    
    
    @Override
    public String toString() {
        return toIP(this.firstIP) + "/" + bits;
    }
    
    static private int compare(long diff) {
        return (0 == diff) ? 0 : (diff > 0 ? 1 : -1);
    }
    
    @Override
    public int compareTo(CIDR cidr) {
        int ipComp = compare(this.firstIP - cidr.firstIP);
        
        // If a subnet starts on the same IP but is smaller, then it should go ahead of a larger subnet.
        // So we invert the meaning of "difference" with bits.
        return (0 != ipComp) ? ipComp : compare((long)cidr.bits - (long)this.bits);
    }
    
    @Override
    public boolean equals(Object obj) {
        
        if ((obj == null)|| (obj.getClass() != getClass()))
            return false;
            
        final CIDR cidr = (CIDR) obj;
        return (this.firstIP == cidr.firstIP) && (this.bits == cidr.bits);
    }
    
    @Override
    public int hashCode() {
        return (int)this.firstIP;
    }
    
    
    //
    // From here to the bottom of the ifle, we have functions to suport:
    // * the changing of an IP range into a minimal list of CIDRs
    // * the merging of adjacent/overlapping CIDRs into a minimal list of CIDRs
    //
    
    
    // Produce a 32-bit bitmask with the leftmost N bits set.
    private static int imask(int leftBitsToSet) {
        if((leftBitsToSet < 1) || (leftBitsToSet > 32))
            throw new IllegalArgumentException("CIDR.imask(" + leftBitsToSet + ")? Arg must be in [1:32]");
        return (int)((long)Math.pow(2, 32) - (long)Math.pow(2, (32 - leftBitsToSet)));
    }
    
    // Given a numeric IP, how many left-set bits are required in a 32-bit mask?
    private static short maxBlock(long ip) {
        if((0 == ip) || (ip > maxIP))
            throw new IllegalArgumentException("CIDR.maxBlock(" + ip + ")? Arg must be valid ip integer.");
        
        short result = 32;
        for(int mask = imask(31); (ip & mask) == ip; mask <<= 1) {
            --result;
        }
        return result;
    }
    
    public static List<CIDR> toCidrs(String start, String end) {
        return toCidrs(toIP(start), toIP(end));
    }
    
    public static List<CIDR> toCidrs(long firstIp, long lastIp) {
        
        List<CIDR> result = new ArrayList<>();
        
        while(lastIp > firstIp) {
            short maxsize = maxBlock(firstIp); // The # of subnet bits
            short x = (short)log2(lastIp - firstIp + 1);
            short maxdiff = (short)(32 - x);
            maxsize = (short)Math.max(maxsize, maxdiff);
            CIDR cidr = new CIDR(firstIp, maxsize);
            result.add(cidr);
            
            firstIp += Math.pow(2, (32 - maxsize));
        }
        if(lastIp == firstIp) { // What if we have a single /32 left over?
            CIDR cidr = new CIDR(firstIp, (short)32);
            result.add(cidr);
        }
        
        return result;
    }
    
    private static List<CIDR> do_coalesce(Collection<CIDR> cidrs) {
        List<CIDR> result = new ArrayList<>();
        if(cidrs.isEmpty())
            return result;
        
        Iterator<CIDR> i = cidrs.iterator();
        CIDR prior = i.next();
        while(i.hasNext()) {
            CIDR current = i.next();
            if(current.equals(prior)) {
                prior = current;       // de-duping
            } else if( prior.overlaps(current)) {
                if( current.contains(prior)) {
                    prior = current;
                } else if(prior.contains(current)) {
                    // Nothing to do, we ignore the current CIDR
                } else { // we simply overlap
                    // First, add the total range as CIDRs
                    long firstIP = Math.min(prior.firstIP, current.firstIP);
                    long lastIP = Math.max(prior.lastIP(), current.lastIP());
                    result.addAll(toCidrs(firstIP, lastIP));
                    
                    // Second, grab the next one and store it in 'prior'.
                    prior = i.hasNext() ? i.next() : null;
                }
            } else if(prior.isAdjacent(current)) {
                result.add(new CIDR(prior.firstIP, (short)(prior.bits-1)));
                prior = i.hasNext() ? i.next() : null;
            } else {
                result.add(prior);
                prior = current;
            }
        }
        
        // If there was an odd CIDR on the end, just add it.
        if(null != prior) {
            result.add(prior);
        }
        
        return result;
    }
    
    private long lastIP() {
        return this.firstIP + (1 << (32 - this.bits)) - 1;
    }
    
    public boolean contains(CIDR cidr) {
        // Trival case, we start at the same IP and go further.
        if((this.firstIP == cidr.firstIP) && (this.bits > cidr.bits))
            return true;
        
        return (this.firstIP <= cidr.firstIP) && (this.lastIP() >= cidr.lastIP());
    }
    
    private boolean overlaps(CIDR cidr) {
        return !( (this.firstIP > cidr.lastIP()) || (this.lastIP() < cidr.firstIP) );
    }
    
    private boolean isAdjacent(CIDR cidr) {
        int oldCidrRange = 1 << (32 - this.bits);
        int newCidrRange = oldCidrRange * 2;
        return (this.bits == cidr.bits) &&
            (cidr.firstIP - this.firstIP == oldCidrRange) &&
            (0 == (this.firstIP % newCidrRange));
    }
    
    static public List<CIDR> coalesce(Collection<CIDR> cidrs) {
        HashSet<CIDR> set = new HashSet<>(cidrs);
        List<CIDR> sorted = new ArrayList<>(set);
        Collections.sort(sorted);
        cidrs = sorted;
        
        List<CIDR> result = do_coalesce(cidrs);
        
        if(result.size() != cidrs.size()) { // If any pairs were coalesced...
            List<CIDR> new_result = do_coalesce(result);
            
            // Keep going as long as we join subnets together...
            while(new_result.size() != result.size()) {
                result = new_result;
                new_result = do_coalesce(result);
            }
        }
        
        return result;
    }
    
    // Given an ip represented as a long, and an octet number (1.2.3.4), return
    // the requested octect as a short
    //
    // Not the lack of error handling - improperly calling this can produce UB.
    private static short getOctet(long ip, int which) {
        final int nBitShift = 8 * (4 - which);
        long mask = 255 << nBitShift;
        long masked = ip & mask;
        return (short)(masked >>> nBitShift);
    }
    
    /**
     * Is the given IP routable?
     * The unroutable networks are:
     *   1) 10.0.0.0 - 10.255.255.255     10.0.0.8/8
     *   2) 172.16.0.0 - 172.31.255.255   172.16.0.0/12
     *   3) 192.168.0.0 - 192.168.255.255 192.168.0.0/16
     *   4) 100.46.0.0 - 100.127.255.255  100.64.0.0/10
     * 
     *   The first 3 were established by RFC 1918 as the original
     *   private network ranges. The 4th was allocated by IANA
     *   for carrier-grade NAT deployments.
     * 
     * @param ipStr the ip in string form
     * @return true if the IP is not in an unroutable range, false otherwise
     */
    public static boolean isRoutable(String ipStr) {
        return isRoutable(toIP(ipStr));
    }
    public static boolean isRoutable(long ip) {
        boolean result = true;
        
        switch(getOctet(ip, 1)) {
            case 10:
                result = false;
                break;
                
            case 100: {
                // For 100.64.0.0/10, we need to check that the top 2 bits of
                // the 2nd octet are 01; the mask will be 0x40 0000 or 4194304
                final long mask = 4194304;
                if(mask == (ip & mask))
                    result = false;
            }
            break;
                
            case 172: {
                short octetTwo = getOctet(ip, 2);
                if((octetTwo >= 16) && (octetTwo <= 31))
                    result = false;
            }
            break;
                
            case 192: {
                short octetTwo = getOctet(ip, 2);
                if(octetTwo == 168)
                    result = false;
            }
            break;
                
            default: break;
        }
        
        return result;
    }
}