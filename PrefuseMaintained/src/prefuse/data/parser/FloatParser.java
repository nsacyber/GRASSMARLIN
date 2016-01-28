package prefuse.data.parser;

/**
 * DataParser instance that parses float values from a text string. Float
 * values can be explicitly coded for by using a 'f' at the end of a
 * number. For example "1.0" could parse as a double or a float, but
 * "1.0f" will only parse as a float.
 *  
 * @author <a href="http://jheer.org">jeffrey heer</a>
 */
public class FloatParser implements DataParser {
    
    /**
     * Returns float.class.
     * @see prefuse.data.parser.DataParser#getType()
     */
    public Class getType() {
        return float.class;
    }
    
    /**
     * @see prefuse.data.parser.DataParser#format(java.lang.Object)
     */
    public String format(Object value) {
        if ( value == null ) return null;
        if ( !(value instanceof Number) )
            throw new IllegalArgumentException(
              "This class can only format Objects of type Number.");
        return String.valueOf(((Number)value).floatValue())+"f";
    }
    
    /**
     * @see prefuse.data.parser.DataParser#canParse(java.lang.String)
     */
    public boolean canParse(String text) {        
        try {
            Float.parseFloat(text);
            return true;
        } catch ( NumberFormatException e ) {
            return false;
        }
    }

    /**
     * @see prefuse.data.parser.DataParser#parse(java.lang.String)
     */
    public Object parse(String text) throws DataParseException {
        return new Float(parseFloat(text));
    }
    
    /**
     * Parse a float value from a text string.
     * @param text the text string to parse
     * @return the parsed float value
     * @throws DataParseException if an error occurs during parsing
     */
    public static float parseFloat(String text) throws DataParseException {
        try {
            return Float.parseFloat(text);
        } catch ( NumberFormatException e ) {
            throw new DataParseException(e);
        }
    }
    
} // end of class FloatParser
