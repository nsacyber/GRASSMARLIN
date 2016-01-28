package prefuse.data.parser;

/**
 * DataParser instance that parses long values from a text string. Long
 * values can be explicitly coded for by using an 'L' at the end of a
 * number. For example "42" could parse as an int or a long, but
 * "42L" will only parse as a long.
 *  
 * @author <a href="http://jheer.org">jeffrey heer</a>
 */
public class LongParser implements DataParser {
    
    /**
     * Returns long.class.
     * @see prefuse.data.parser.DataParser#getType()
     */
    public Class getType() {
        return long.class;
    }
    
    /**
     * @see prefuse.data.parser.DataParser#format(java.lang.Object)
     */
    public String format(Object value) {
        if ( value == null ) return null;
        if ( !(value instanceof Number) )
            throw new IllegalArgumentException(
              "This class can only format Objects of type Number.");
        return String.valueOf(((Number)value).longValue())+"L";
    }
    
    /**
     * @see prefuse.data.parser.DataParser#canParse(java.lang.String)
     */
    public boolean canParse(String text) {
        try {
            parseLong(text);
            return true;
        } catch ( DataParseException e ) {
            return false;
        }
    }
    
    /**
     * @see prefuse.data.parser.DataParser#parse(java.lang.String)
     */
    public Object parse(String text) throws DataParseException {
        return new Long(parseLong(text));
    }
    
    /**
     * Parse a long value from a text string.
     * @param text the text string to parse
     * @return the parsed long value
     * @throws DataParseException if an error occurs during parsing
     */
    public static long parseLong(String text) throws DataParseException {
        try {
            // allow trailing 'L' characters to signify a long
            if ( text.length() > 0 ) {
                char c = text.charAt(text.length()-1);
                if ( c == 'l' || c == 'L' )
                    text = text.substring(0,text.length()-1);
            }
            // parse the string
            return Long.parseLong(text);
        } catch ( NumberFormatException e ) {
            throw new DataParseException(e);
        }
    }
    
} // end of class LongParser
