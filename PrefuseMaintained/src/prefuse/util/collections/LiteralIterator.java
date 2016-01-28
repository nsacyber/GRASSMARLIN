package prefuse.util.collections;

import java.util.Iterator;

/**
 * @author <a href="http://jheer.org">jeffrey heer</a>
 */
public interface LiteralIterator extends Iterator {
    
    public int nextInt();
    public boolean isIntSupported();
    
    public long nextLong();
    public boolean isLongSupported();
    
    public float nextFloat();
    public boolean isFloatSupported();
    
    public double nextDouble();
    public boolean isDoubleSupported();
    
    public boolean nextBoolean();
    public boolean isBooleanSupported();
    
} // end of interface LiteralIterator
