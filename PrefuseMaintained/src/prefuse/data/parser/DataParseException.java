package prefuse.data.parser;

/**
 * Exception indicating an error occurred during parsing of data values.
 *  
 * @author <a href="http://jheer.org">jeffrey heer</a>
 */
public class DataParseException extends Exception {

    /**
     * Create a new DataParseException.
     */
    public DataParseException() {
        super();
    }
    
    /**
     * Create a new DataParseException.
     * @param message a descriptive error message
     */
    public DataParseException(String message) {
        super(message);
    }

    /**
     * Create a new DataParseException.
     * @param message a descriptive error message
     * @param cause a Throwable (e.g., error or exception) that was the cause
     * for this exception being thrown
     */
    public DataParseException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Create a new DataParseException.
     * @param cause a Throwable (e.g., error or exception) that was the cause
     * for this exception being thrown
     */
    public DataParseException(Throwable cause) {
        super(cause);
    }
    
} // end of class DataParseException
