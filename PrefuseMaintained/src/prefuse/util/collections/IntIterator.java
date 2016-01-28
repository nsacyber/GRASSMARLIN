package prefuse.util.collections;

/**
 * Abstract LiteralIterator implementation that supports an iteration over
 * int values. Subclasses need only implement the {@link #nextInt()} method.
 * The {@link #nextLong()}, {@link #nextFloat()}, and {@link #nextDouble()}
 * methods all simply cast the output of {@link #nextInt()}. The
 * {@link #next()} method simply wraps the output of {@link #nextInt()} in
 * an {@link java.lang.Integer} object.
 * 
 * @author <a href="http://jheer.org">jeffrey heer</a>
 */
public abstract class IntIterator extends AbstractLiteralIterator {

    /**
     * @see java.util.Iterator#next()
     */
    public Object next() {
        return new Integer(nextInt());
    }

    /**
     * @see prefuse.util.collections.LiteralIterator#isDoubleSupported()
     */
    public boolean isDoubleSupported() {
        return true;
    }

    /**
     * @see prefuse.util.collections.LiteralIterator#isFloatSupported()
     */
    public boolean isFloatSupported() {
        return true;
    }

    /**
     * @see prefuse.util.collections.LiteralIterator#isIntSupported()
     */
    public boolean isIntSupported() {
        return true;
    }

    /**
     * @see prefuse.util.collections.LiteralIterator#isLongSupported()
     */
    public boolean isLongSupported() {
        return true;
    }

    /**
     * @see prefuse.util.collections.LiteralIterator#nextDouble()
     */
    public double nextDouble() {
        return nextInt();
    }

    /**
     * @see prefuse.util.collections.LiteralIterator#nextFloat()
     */
    public float nextFloat() {
        return nextInt();
    }

    /**
     * @see prefuse.util.collections.LiteralIterator#nextLong()
     */
    public long nextLong() {
        return nextInt();
    }

    /**
     * @see prefuse.util.collections.LiteralIterator#nextInt()
     */
    public abstract int nextInt();
    
} // end of abstract class IntIterator
