package prefuse.util.collections;

/**
 * Abstract base class for a LiteralIterator implementations.
 * 
 * @author <a href="http://jheer.org">jeffrey heer</a>
 */
public abstract class AbstractLiteralIterator implements LiteralIterator {

    /**
     * @see prefuse.util.collections.LiteralIterator#nextInt()
     */
    public int nextInt() {
        throw new UnsupportedOperationException("int type unsupported");
    }

    /**
     * @see prefuse.util.collections.LiteralIterator#nextLong()
     */
    public long nextLong() {
        throw new UnsupportedOperationException("long type unsupported");
    }

    /**
     * @see prefuse.util.collections.LiteralIterator#nextFloat()
     */
    public float nextFloat() {
        throw new UnsupportedOperationException("float type unsupported");
    }

    /**
     * @see prefuse.util.collections.LiteralIterator#nextDouble()
     */
    public double nextDouble() {
        throw new UnsupportedOperationException("double type unsupported");
    }

    /**
     * @see prefuse.util.collections.LiteralIterator#nextBoolean()
     */
    public boolean nextBoolean() {
        throw new UnsupportedOperationException("boolean type unsupported");
    }

    /**
     * @see prefuse.util.collections.LiteralIterator#isBooleanSupported()
     */
    public boolean isBooleanSupported() {
        return false;
    }

    /**
     * @see prefuse.util.collections.LiteralIterator#isDoubleSupported()
     */
    public boolean isDoubleSupported() {
        return false;
    }

    /**
     * @see prefuse.util.collections.LiteralIterator#isFloatSupported()
     */
    public boolean isFloatSupported() {
        return false;
    }

    /**
     * @see prefuse.util.collections.LiteralIterator#isIntSupported()
     */
    public boolean isIntSupported() {
        return false;
    }

    /**
     * @see prefuse.util.collections.LiteralIterator#isLongSupported()
     */
    public boolean isLongSupported() {
        return false;
    }
    
} // end of class AbstractLiteralIterator
