package prefuse.data.io;

/**
 * Exception indicating an error occurred during reading or writing data.
 *
 * @author <a href="http://jheer.org">jeffrey heer</a>
 */
public class DataIOException extends Exception {

    /**
     * Create a new DataIOException.
     */
    public DataIOException() {
        super();
    }

    /**
     * Create a new DataIOException.
     * @param message a descriptive error message
     */
    public DataIOException(String message) {
        super(message);
    }

    /**
     * Create a new DataIOException.
     * @param message a descriptive error message
     * @param cause a Throwable (e.g., error or exception) that was the cause
     * for this exception being thrown
     */
    public DataIOException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Create a new DataIOException.
     * @param cause a Throwable (e.g., error or exception) that was the cause
     * for this exception being thrown
     */
    public DataIOException(Throwable cause) {
        super(cause);
    }
    
} // end of class DataIOException
