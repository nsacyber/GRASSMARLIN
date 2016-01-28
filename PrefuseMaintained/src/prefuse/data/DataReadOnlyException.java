package prefuse.data;

/**
 * Exception indicating an attempt to write to a read-only data value was made.
 * @author <a href="http://jheer.org">jeffrey heer</a>
 */
public class DataReadOnlyException extends RuntimeException {

    /**
     * Create a new DataReadOnlyException.
     */
    public DataReadOnlyException() {
        super();
    }

    /**
     * Create a new DataReadOnlyException.
     * @param message a descriptive error message
     * @param cause a Throwable (e.g., error or exception) that was the cause
     * for this exception being thrown
     */
    public DataReadOnlyException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Create a new DataReadOnlyException.
     * @param message a descriptive error message
     */
    public DataReadOnlyException(String message) {
        super(message);
    }

    /**
     * Create a new DataReadOnlyException.
     * @param cause a Throwable (e.g., error or exception) that was the cause
     * for this exception being thrown
     */
    public DataReadOnlyException(Throwable cause) {
        super(cause);
    }

} // end of class DataReadOnlyException
