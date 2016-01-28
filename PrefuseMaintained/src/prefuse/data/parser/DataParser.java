package prefuse.data.parser;


/**
 * Interface for data parsers, which parse data values from text Strings
 * and generated formatted text Strings for data values.
 *
 * @author <a href="http://jheer.org">jeffrey heer</a>
 */
public interface DataParser {
    
    /**
     * Get the data type for the values parsed by this parser.
     * @return the parsed data type for this parser as a Java Class instance
     */
    public Class getType();
    
    
    /**
     * Get a String representation for the given value.
     * @param value the object value to format
     * @return a formatted String representing the input value
     */
    public String format(Object value);
    
    /**
     * Indicates if the given text string can be successfully parsed by
     * this parser.
     * @param text the text string to check for parsability
     * @return true if the string can be successfully parsed into this
     * parser's data type, false otherwise
     */
    public boolean canParse(String text);
    
    /**
     * Parse the given text string to a data value.
     * @param text the text string to parse
     * @return the parsed data value, which will be an instance of the
     * Class returned by the {@link #getType()} method
     * @throws DataParseException if an error occurs during parsing
     */
    public Object parse(String text) throws DataParseException; 
   
} // end of interface DataParser
