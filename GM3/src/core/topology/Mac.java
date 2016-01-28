/*
 *  Copyright (C) 2011, 2012
 *  This file is part of GRASSMARLIN.
 */
package core.topology;

import java.io.Serializable;

/**
 * Contains a MAC address hashed to a long.
 */
public class Mac implements Serializable {
    public static final Mac MissingMac = new Mac(-1) {
        @Override
        public boolean isMissing() {
            return true;
        }
        @Override
        public String toString() {
            return "?-?-?-?-?-?";
        }
    };
    
    public static final char SEPARATOR_CHAR = '-';
    public static final String SEPARATOR_STRING = "-";
    public static final String[] MISSING_OUI = { "00", "00", "00" };
    
    Long hash;
    boolean portMac;
    
    public Mac(long hash, boolean portMac) {
        this.hash = hash;
        this.portMac = portMac;
    }
    
    public Mac(long hash) {
        this(hash, false);
    }
    
    public void setPortMac(boolean portMac) {
        this.portMac = portMac;
    }
    
    public Mac(String macAddress) {
        this.hash = Mac.strToLong(macAddress);
    }

    public boolean isPortMac() {
        return portMac;
    }
    
    public boolean isMissing() {
        return false;
    }
   
    public long toLong() {
        return hash;
    }
    
    public String getOUI() {
        return Mac.getOUI(this);
    }
    
    public static String toString(Byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (Byte b : bytes) {
            sb.insert(0, Integer.toHexString(b & 0b00001111).substring(0, 1) );
            sb.insert(0, Integer.toHexString(b & 0b11110000).substring(0, 1) );
            sb.insert(0, '-');
        }
        return sb.toString().substring(1).toUpperCase();
    }

    public static Byte[] toBytes( String macAddress ) {
        Long l = Mac.strToLong(macAddress);
        Byte[] buffer = new Byte[6];
        for( int i = 0 ; i < 6; ++i ) {
            buffer[i] = l.byteValue();
            l >>= 8;
        }
        return buffer;
    }
    
    @Override
    public String toString() {
        return Mac.longToMac(this.hash);
    }
    
    @Override
    public int hashCode() {
        return hash.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Mac other = (Mac) obj;

        long aHash = other.hash;
        long bHash = this.hash;
        
        return aHash == bHash;
    }
    
    //<editor-fold defaultstate="collapsed" desc="Static Helper Methods">
    public static String getOUI( Mac mac ) {
        String[] parts = mac.toString().split(SEPARATOR_STRING);
        if( parts.length < 3 ) {
            parts = Mac.MISSING_OUI;
        }
        return parts[0].concat(parts[1].concat(parts[2]));
    }
    
    public static final Long strToLong(String macAddress) {
        Long hash = 0L;
        macAddress = macAddress.replaceAll("[^\\p{XDigit}]", "");
        for( int i = 0; i < macAddress.length(); i++ ) {
            byte b = (byte)Character.digit(macAddress.charAt(i), 16);
            hash <<= 4;
            hash  += b;
        }
        return hash;
    }
    
    public static final String longToMac( long l ) {
        StringBuilder sb = new StringBuilder();
        for( int i = 0 ; i < 6; i++ ) {
            byte b = (byte) l;
            sb.insert(0, Integer.toHexString(b & 0b00001111).substring(0, 1) );
            sb.insert(0, Integer.toHexString(b & 0b11110000).substring(0, 1) );
            sb.insert(0, Mac.SEPARATOR_CHAR);
            l >>= 8;
        }
        return sb.toString().substring(1);
    }
    //</editor-fold>
}
