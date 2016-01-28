/*
 *  Copyright (C) 2011, 2012
 *  This file is part of GRASSMARLIN.
 */
package core.fingerprint;

import TemplateEngine.Data.PseudoBuffer;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang3.ArrayUtils;
import org.jnetpcap.nio.JBuffer;

/**
 * A buffer to proxy JNetPcaps JBuffer.
 * This is necessary to allow fingerPrinting based solely port info and
 * to avoid issues with non pcap imports (bro) and safely execute the same
 * port-only protocol identification with a corrupt or missing JNetPcap native library.
 * 
 * See {@link org.jnetpcap.nio.JBuffer} for actual method documentation.
 * See {@link org.jnetpcap.nio.JMemory} for actual method documentation.
 * 
 */
public class ProxyBuffer implements PseudoBuffer {
    
    public static final ProxyBuffer EMPTY_BUFFER = new ProxyBuffer() {
        @Override
        public byte[] getByteArray(int index, byte[] array, int offset, int length) {
            return ArrayUtils.EMPTY_BYTE_ARRAY;
        }
        
        @Override
        public int size() {
            return -1;
        }
    };
    
    JBuffer buffer;
    
    private ProxyBuffer() {
        
    }
    
    public ProxyBuffer(byte[] bytes) {
        this.buffer = new JBuffer(bytes);
    }
    
    public ProxyBuffer(JBuffer buffer) {
        this.buffer = buffer;
    }
    
    public byte[] getByteArray(int index, byte[] array, int offset, int length, boolean flip) {
        byte[] ary = buffer.getByteArray(index, array, offset, length);
        if( flip ) { /// flip to little endian
            ArrayUtils.reverse(array);
        }
        return ary;
    }
    
    public byte[] getByteArray(int index, byte[] array, int offset, int length) {
        return buffer.getByteArray(index, array, offset, length);
    }
    
    public byte[] getByteArray(int offset, int length) {
        byte[] bytes;
        if( buffer.size() > offset + length ) {
            bytes = buffer.getByteArray(offset, length);
        } else {
            bytes = ArrayUtils.EMPTY_BYTE_ARRAY;
        }
        return bytes;
    }
    /**
     * @return Last available index in the buffer.
     */
    public int end() {
        return buffer.size()-1;
    }
    
    public String getString(int offset, int length) {
        return new String( getByteArray(offset, length) );
    }
    
    public String match(Pattern pattern) {
        return match(pattern, 0, this.size()-1);
    }
    
    public String match(Pattern pattern, int offset, int length) {
        String match = null;
        String string = new String(getByteArray(offset, length));
        Matcher m = pattern.matcher(string);
        if( m.matches() && m.start() != m.end() ) {
            match = m.group();
        }
        return match;
    }
    
    /**
     * Locates the bytes within given length from the offset in the buffer.
     * @param search Bytes to search for.
     * @param offset Offset to start search at.
     * @param length Length to search within.
     * @return Location of the start of the matched sequence on success, else -1 on failure.
     */
    public int match(byte[] search, int offset, int length) {
        int ret = -1;
        int searchLength = search.length;
        if( searchLength > 0 ) {
            int limit = Math.min(offset + length, buffer.size()) - searchLength - offset;
            byte byte0 = search[0];
            for( int start = offset; start <= limit; ++start ) {
                if( buffer.getByte(start) == byte0 ) {
                    int i = 0;
                    for( ; i < searchLength; ++i ) {
                        if( search[i] != buffer.getByte(start+i) ) {
                            break;
                        }
                    }
                    if( i == searchLength ) {
                        ret = start;
                        break;
                    }
                }
            }
        }
        return ret;
    }
    
    @Override
    public byte[] extract(final int from, int to, final int length) {
        byte[] bytes;
        if( from > end() ) {
            bytes = ArrayUtils.EMPTY_BYTE_ARRAY;
        } else {
            bytes = buffer.getByteArray(from, to - from);
        }
        return bytes;
    }
    
    public byte[] extractLittle(final int from, final int to, final int length) {
        byte[] bytes = extract(from, to, length);
        ArrayUtils.reverse(bytes);
        return bytes;
    }
    
    public boolean match(int offset, int compareTo) {
        return buffer.getInt(offset) == compareTo;
    }
    
    public int getInt(final int offset, boolean isNetByteOrder) {
        return getInt(offset, Integer.BYTES, isNetByteOrder);
    }
    
    public int getInt(final int offset, int length, boolean isNetByteOrder) {
        int val;
        length = Math.min(Integer.BYTES, length);
        byte[] bytes = new byte[length];
        if( offset >= 0 && length > 0 ) {
            buffer.getByteArray(offset, bytes);
            if( !isNetByteOrder ) {
                ArrayUtils.reverse(bytes);
            }
            val = 0;
            for( int i = 0; i< length; ++i ) {
                val <<= 8;
                val |= bytes[i];
            }
        } else {
            val = 0;
        }
        return val;
    }
    
    public String toHexdump() {
        return buffer.toHexdump();
    }
    
    public byte getByte(int index) {
        return buffer.getByte(index);
    }
    
    public int size() {
        return buffer.size();
    }
    
}
