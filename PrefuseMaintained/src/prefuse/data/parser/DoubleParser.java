package prefuse.data.parser;

/**
 * DataParser instance that parses double values from a text string.
 *
 * @author <a href="http://jheer.org">jeffrey heer</a>
 */
public class DoubleParser implements DataParser {
    
    private boolean m_blockExplicitFloats = true;
    
    /**
     * Returns double.class.
     * @see prefuse.data.parser.DataParser#getType()
     */
    public Class getType() {
        return double.class;
    }
    
    /**
     * @see prefuse.data.parser.DataParser#format(java.lang.Object)
     */
    public String format(Object value) {
        if ( value == null ) return null;
        if ( !(value instanceof Number) )
            throw new IllegalArgumentException(
              "This class can only format Objects of type Number.");
        return String.valueOf(((Number)value).doubleValue());
    }
    
    /**
     * @see prefuse.data.parser.DataParser#canParse(java.lang.String)
     */
    public boolean canParse(String text) {
        try {
            if ( m_blockExplicitFloats && text.endsWith("f") ) {
                // don't try to convert floats
                return false;
            }
            Double.parseDouble(text);
            return true;
        } catch ( NumberFormatException e ) {
            return false;
        }
    }
    
    /**
     * @see prefuse.data.parser.DataParser#parse(java.lang.String)
     */
    public Object parse(String text) throws DataParseException {
        return new Double(parseDouble(text));
    }
    
    /**
     * Parse a double value from a text string.
     * @param text the text string to parse
     * @return the parsed double value
     * @throws DataParseException if an error occurs during parsing
     */
    public static double parseDouble(String text) throws DataParseException {
        try {
            return Double.parseDouble(text);
        } catch ( NumberFormatException e ) {
            throw new DataParseException(e);
        }
    }
    
} // end of class DoubleParser
