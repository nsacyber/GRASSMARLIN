package prefuse.data.parser;

import prefuse.util.ColorLib;

/**
 * DataParser instance the parses int color values from a
 * hexadecimal text string.
 *  
 * @author <a href="http://jheer.org">jeffrey heer</a>
 */
public class ColorIntParser implements DataParser {
    
    /**
     * Returns int.class.
     * @see prefuse.data.parser.DataParser#getType()
     */
    public Class getType() {
        return int.class;
    }
    
    /**
     * @see prefuse.data.parser.DataParser#format(java.lang.Object)
     */
    public String format(Object value) {
        if ( value == null ) return null;
        if ( !(value instanceof Number) )
            throw new IllegalArgumentException(
              "This class can only format Objects of type Number.");
        return String.valueOf(((Number)value).intValue());
    }
    
    /**
     * @see prefuse.data.parser.DataParser#canParse(java.lang.String)
     */
    public boolean canParse(String text) {
        try {
            if ( text.charAt(0) == ColorLib.HEX_PREFIX ) {
                parseInt(text);
            } else {
                return false;
            }
            return true;
        } catch ( Exception e ) {
            return false;
        }
    }
    
    /**
     * @see prefuse.data.parser.DataParser#parse(java.lang.String)
     */
    public Object parse(String text) throws DataParseException {
        return new Integer(parseInt(text));
    }
    
    /**
     * Parse an int value from a text string.
     * @param text the text string to parse
     * @return the parsed int value
     * @throws DataParseException if an error occurs during parsing
     */
    public static int parseInt(String text) throws DataParseException {
        try {
            return ColorLib.hex(text);
        } catch ( Exception e ) {
            throw new DataParseException(e);
        }
    }
    
} // end of class IntParser
