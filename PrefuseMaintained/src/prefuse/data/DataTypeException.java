package prefuse.data;

/**
 * Exception indicating an incompatible data type assignment.
 * 
 * @author <a href="http://jheer.org">jeffrey heer</a>
 */
public class DataTypeException extends RuntimeException {

    /**
     * Create a new DataTypeException.
     */
    public DataTypeException() {
        super();
    }

    /**
     * Create a new DataTypeException.
     * @param message a descriptive error message
     * @param cause a Throwable (e.g., error or exception) that was the cause
     * for this exception being thrown
     */
    public DataTypeException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Create a new DataTypeException.
     * @param message a descriptive error message
     */
    public DataTypeException(String message) {
        super(message);
    }

    /**
     * Create a new DataTypeException.
     * @param cause a Throwable (e.g., error or exception) that was the cause
     * for this exception being thrown
     */
    public DataTypeException(Throwable cause) {
        super(cause);
    }
    
    /**
     * Create a new DataTypeException.
     * @param type the incompatible data type
     */
    public DataTypeException(Class type) {
        super("Type "+type.getName()+" not supported.");
    }
    
} // end of class DataTypeException
