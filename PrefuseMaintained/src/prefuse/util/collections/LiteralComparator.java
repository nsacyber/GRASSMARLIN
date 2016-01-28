package prefuse.util.collections;

import java.util.Comparator;

/**
 * @author <a href="http://jheer.org">jeffrey heer</a>
 */
public interface LiteralComparator extends Comparator {

    int compare(byte x1, byte x2);
    int compare(int x1, int x2);
    int compare(long x1, long x2);
    int compare(float x1, float x2);
    int compare(double x1, double x2);
    int compare(boolean x1, boolean x2);
    
} // end of interface LiteralComparator
