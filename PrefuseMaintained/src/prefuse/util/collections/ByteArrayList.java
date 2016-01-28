package prefuse.util.collections;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

/**
 * A resizable array that maintains a list of byte values.
 * 
 * @author <a href="http://jheer.org">jeffrey heer</a>
 */
public class ByteArrayList {

    private byte[] m_bytes;
    private int m_size;
    
    public ByteArrayList() {
        this(4096);
    }
    
    public ByteArrayList(int capacity) {
        m_bytes = new byte[capacity];
        m_size = 0;
    }
    
    private void rangeCheck(int i) {
        if ( i < 0 || i >= m_size ) 
            throw new IndexOutOfBoundsException(
                    "Index: "+i+" Size: " + m_size);
    }
    
    private void ensureCapacity(int cap) {
        if ( m_bytes.length < cap ) {
            int capacity = Math.max((3*m_bytes.length)/2 + 1, cap);
            byte[] nbytes = new byte[capacity];
            System.arraycopy(m_bytes, 0, nbytes, 0, m_size);
            m_bytes = nbytes;
        }
    }
    
    public byte get(int i) {
        rangeCheck(i);
        return m_bytes[i];
    }
    
    public void set(int i, byte b) {
        rangeCheck(i);
        m_bytes[i] = b;
    }
    
    public int size() {
        return m_size;
    }
    
    public void add(byte b) {
        ensureCapacity(m_size+1);
        m_bytes[m_size++] = b;
    }
    
    public void add(byte[] b, int start, int len) {
        ensureCapacity(m_size+len);
        System.arraycopy(b,start,m_bytes,m_size,len);
        m_size += len;
    }
    
    public InputStream getAsInputStream() {
        return new ByteArrayInputStream(m_bytes,0,m_size);
    }
    
    public byte[] toArray() {
        byte[] b = new byte[m_size];
        System.arraycopy(m_bytes,0,b,0,m_size);
        return b;
    }
}
