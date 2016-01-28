package prefuse.util.collections;

/**
 * Exception indicating a comparator is incompatible with the data type
 * to be compared.
 * 
 * @author <a href="http://jheer.org">jeffrey heer</a>
 */
public class IncompatibleComparatorException extends Exception {

    /**
     * Create a new IncompatibleComparatorException.
     */
    public IncompatibleComparatorException() {
        super();
    }

    /**
     * Create a new IncompatibleComparatorException.
     * @param message a descriptive error message
     */
    public IncompatibleComparatorException(String message) {
        super(message);
    }

    /**
     * Create a new IncompatibleComparatorException.
     * @param cause a Throwable (e.g., error or exception) that was the cause
     * for this exception being thrown
     */
    public IncompatibleComparatorException(Throwable cause) {
        super(cause);
    }

    /**
     * Create a new IncompatibleComparatorException.
     * @param message a descriptive error message
     * @param cause a Throwable (e.g., error or exception) that was the cause
     * for this exception being thrown
     */
    public IncompatibleComparatorException(String message, Throwable cause) {
        super(message, cause);
    }

} // end of class IncompatibleComparatorException
