package prefuse.util;

import java.awt.FontMetrics;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StreamTokenizer;
import java.io.StringReader;
import java.io.StringWriter;
import java.lang.reflect.Array;
import java.util.Hashtable;

/**
 * Library of utility routines pertaining to Strings.
 * 
 * @author <a href="http://jheer.org">jeffrey heer</a>
 */
public class StringLib {

    private StringLib() {
        // prevent instantiation
    }

    /**
     * Given an array object, create a String showing the contents
     * of the array using a "[a[0], a[1], ..., a[a.length-1]]" format.
     * @param a the array object
     * @return the array string
     */
    public static final String getArrayString(Object a) {
        StringBuffer sbuf = new StringBuffer();
        sbuf.append('[');
        int size = Array.getLength(a);
        for ( int i=0; i<size; ++i ) {
            if ( i>0 ) sbuf.append(", ");
            sbuf.append(Array.get(a, i));
        }
        sbuf.append(']');
        return sbuf.toString();
    }
    
    /**
     * Format the given number as a String, including the given number of
     * decimal places.
     * @param number the number to format
     * @param decimalPlaces the number of decimal places to include
     * @return the formatted String
     */
    public static String formatNumber(double number, int decimalPlaces) {
        String s = String.valueOf(number);
        int idx1 = s.indexOf('.');
        if ( idx1 == -1 ) {
            return s;
        } else {
            int idx2 = s.indexOf('E');        
            int dp = decimalPlaces + (idx2>=0 ? 0 : 1);
            String t = s.substring(0, Math.min(idx1+dp, s.length()));
            if ( idx2 >= 0 )
                t += s.substring(idx2);
            return t;
        }
    }
    
    /**
     * Capitalize all letters preceded by whitespace, and lower case
     * all other letters. 
     * @param s the String to capitalize
     * @return the capitalized string
     */
    public static String capitalizeFirstOnly(String s) {
        if ( s == null )
            return null;
        if ( s.length() == 0 )
            return s;
        
        StringBuffer sbuf = new StringBuffer();
        char c = s.charAt(0);
        sbuf.append(Character.toUpperCase(c));
        boolean space = Character.isWhitespace(c);
        for ( int i=1; i<s.length(); ++i ) {
            c = s.charAt(i);
            if ( Character.isWhitespace(c) ) {
                space = true;
            } else if ( space ) {
                c = Character.toUpperCase(c);
                space = false;
            } else {
                c = Character.toLowerCase(c);
            }
            sbuf.append(c);
        }
        return sbuf.toString();
    }
    
    /**
     * Get the stack trace of the given Throwable as a String.
     * @param t the Throwable
     * @return the stack trace of the Throwable
     */
    public static String getStackTrace(Throwable t) {
        StringWriter sw = new StringWriter();
        PrintWriter  pw = new PrintWriter(sw);
        t.printStackTrace(pw);
        pw.close();
        return sw.toString();
    }
    
    // ------------------------------------------------------------------------
    // Abbreviation Methods
    
    private static final String SUFFIX = "suffix";
    private static final String PREFIX = "prefix";
    private static Hashtable prefixSuffixT = new Hashtable();
    static {
        prefixSuffixT.put( "mr",    PREFIX );
        prefixSuffixT.put( "mr.",   PREFIX );
        prefixSuffixT.put( "dr",    PREFIX );
        prefixSuffixT.put( "dr.",   PREFIX );
        prefixSuffixT.put( "lt",    PREFIX );
        prefixSuffixT.put( "lt.",   PREFIX );
        prefixSuffixT.put( "gen",   PREFIX );
        prefixSuffixT.put( "gen.",  PREFIX );
        prefixSuffixT.put( "sgt",   PREFIX );
        prefixSuffixT.put( "sgt.",  PREFIX );
        prefixSuffixT.put( "cmdr",  PREFIX );
        prefixSuffixT.put( "cmdr.", PREFIX );
        prefixSuffixT.put( "cpt",   PREFIX );
        prefixSuffixT.put( "cpt.",  PREFIX );
        prefixSuffixT.put( "ii",    SUFFIX );
        prefixSuffixT.put( "iii",   SUFFIX );
        prefixSuffixT.put( "iv",    SUFFIX );
        prefixSuffixT.put( "jr",    SUFFIX );
        prefixSuffixT.put( "jr.",   SUFFIX );
        prefixSuffixT.put( "sr",    SUFFIX );
        prefixSuffixT.put( "sr.",   SUFFIX );
    }
    
    /**
     * Abbreviate a String by simply truncating it.
     * @param str the String to abbreviate
     * @param fm the FontMetrics for measuring the String length
     * @param width the maximum string width, in pixels
     * @return an abbreviated String
     */
    public static String abbreviate(String str, FontMetrics fm, int width) {
        int lastblank = 0, nchars = 0, cumx = 0;
        while ( cumx < width &&  nchars < str.length() ) {
        if ( Character.isWhitespace(str.charAt(nchars)) ) {
            lastblank = nchars;
        }
        cumx += fm.charWidth(str.charAt(nchars));
        nchars++;
        }
        if ( nchars < str.length() && lastblank > 0 ) { nchars = lastblank; }
        return ( nchars > 0 ? str.substring(0, nchars) : str );
    }

    /**
     * Abbreviate a String as a given name.
     * @param str the String to abbreviate
     * @param fm the FontMetrics for measuring the String length
     * @param width the maximum string width, in pixels
     * @return an abbreviated String
     */
    public static String abbreviateName(String str, FontMetrics fm, int width)
    {
        if (fm.stringWidth(str) > width) str = abbreviateName(str, false);
        if (fm.stringWidth(str) > width) str = abbreviateName(str, true);
        return str;
    }
    
    /**
     * String abbreviation helper method for name strings.
     * @param inString the String to abbreviate
     * @param lastOnly true to include only the last name, false otherwise
     * @return an abbreviated name
     */
    private static String abbreviateName(String inString, boolean lastOnly) {
        StringReader in = new StringReader(inString);
        StreamTokenizer p = new StreamTokenizer(in);
        p.wordChars('&', '&');
        p.wordChars('@', '@');
        p.wordChars(':', ':');
        p.ordinaryChar(',');
        p.ordinaryChar('-');
        int c;
        String lastNameHold = null;
        String lastInitialHold = null;
        StringBuffer outString = new StringBuffer();
        try {
        out:
            while (true) {
            c = p.nextToken();
            switch (c) {
            case StreamTokenizer.TT_EOF:
                break out;
            case StreamTokenizer.TT_EOL:
                System.err.println("warning: unexpected EOL token"); break;
            case StreamTokenizer.TT_NUMBER:
                break;
            case ',':
                break out;
            case StreamTokenizer.TT_WORD:
                if (p.sval.endsWith(":")) outString.append(p.sval + " ");
                else if (prefixSuffixT.get(p.sval.toLowerCase()) == null) {
                    if (!lastOnly) {
                        if (lastInitialHold != null) outString.append(lastInitialHold);
                        lastInitialHold = p.sval.substring(0,1)+". ";
                    }
                    lastNameHold = p.sval;
                }
                break;
            default:
                break;
            }
            }
            outString.append(lastNameHold);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return outString.toString();
    }
    
} // end of class StringLib
