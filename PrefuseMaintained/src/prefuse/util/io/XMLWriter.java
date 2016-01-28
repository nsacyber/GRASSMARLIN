/**
 * Copyright (c) 2004-2006 Regents of the University of California.
 * See "license-prefuse.txt" for licensing terms.
 */
package prefuse.util.io;

import java.io.PrintWriter;
import java.util.ArrayList;

/**
 * Utility class for writing XML files. This class provides convenience
 * methods for creating XML documents, such as starting and ending
 * tags, and adding content and comments. This class handles correct
 * XML formatting and will properly escape text to ensure that the
 * text remains valid XML.
 * 
 * <p>To use this class, create a new instance with the desired
 * PrintWriter to write the XML to. Call the {@link #begin()} or
 * {@link #begin(String, int)} method when ready to start outputting
 * XML. Then use the provided methods to generate the XML file.
 * Finally, call either the {@link #finish()} or {@link #finish(String)}
 * methods to signal the completion of the file.</p>
 * 
 * @author <a href="http://jheer.org">jeffrey heer</a>
 */
public class XMLWriter {
    
    private PrintWriter m_out;
    private int m_bias = 0;
    private int m_tab;
    private ArrayList m_tagStack = new ArrayList();
    
    /**
     * Create a new XMLWriter.
     * @param out the print writer to write the XML to
     */
    public XMLWriter(PrintWriter out) {
        this(out, 2);
    }

    /**
     * Create a new XMLWriter.
     * @param out the print writer to write the XML to
     * @param tabLength the number of spaces to use for each
     *  level of indentation in the XML file
     */
    public XMLWriter(PrintWriter out, int tabLength) {
        m_out = out;
        m_tab = 2;
    }
    
    /**
     * Print <em>unescaped</em> text into the XML file. To print
     * escaped text, use the {@link #content(String)} method instead.
     * @param s the text to print. This String will not be escaped.
     */
    public void print(String s) {
        m_out.print(s);
    }

    /**
     * Print <em>unescaped</em> text into the XML file, followed by
     * a newline. To print escaped text, use the {@link #content(String)}
     * method instead.
     * @param s the text to print. This String will not be escaped.
     */
    public void println(String s) {
        m_out.print(s);
        m_out.print("\n");
    }
    
    /**
     * Print a newline into the XML file.
     */
    public void println() {
        m_out.print("\n");
    }
    
    /**
     * Begin the XML document. This must be called before any other
     * formatting methods. This method prints an XML header into
     * the top of the output stream.
     */
    public void begin() {
        m_out.print("<?xml version=\"1.0\" encoding=\"UTF-8\"?>");
        println();
    }
    
    /**
     * Begin the XML document. This must be called before any other
     * formatting methods. This method prints an XML header into
     * the top of the output stream, plus additional header text
     * provided by the client
     * @param header header text to insert into the document
     * @param bias the spacing bias to use for all subsequent indenting
     */
    public void begin(String header, int bias) {
        begin();
        m_out.print(header);
        m_bias = bias;
    }
    
    /**
     * Print a comment in the XML document. The comment will be printed
     * according to the current spacing and followed by a newline.
     * @param comment the comment text
     */
    public void comment(String comment) {
        spacing();
        m_out.print("<!-- ");
        m_out.print(comment);
        m_out.print(" -->");
        println();
    }
    
    /**
     * Internal method for printing a tag with attributes.
     * @param tag the tag name
     * @param names the names of the attributes
     * @param values the values of the attributes
     * @param nattr the number of attributes
     * @param close true to close the tag, false to leave it
     * open and adjust the spacing
     */
    protected void tag(String tag, String[] names, String[] values,
            int nattr, boolean close)
    {
        spacing();
        m_out.print('<');
        m_out.print(tag);
        for ( int i=0; i<nattr; ++i ) {
            m_out.print(' ');
            m_out.print(names[i]);
            m_out.print('=');
            m_out.print('\"');
            escapeString(values[i]);
            m_out.print('\"');
        }
        if ( close ) m_out.print('/');
        m_out.print('>');
        println();
        
        if ( !close ) {
            m_tagStack.add(tag);
        }
    }
    
    /**
     * Print a closed tag with attributes. The tag will be followed by a
     * newline.
     * @param tag the tag name
     * @param names the names of the attributes
     * @param values the values of the attributes
     * @param nattr the number of attributes
     */
    public void tag(String tag, String[] names, String[] values, int nattr)
    {
        tag(tag, names, values, nattr, true);
    }
    
    /**
     * Print a start tag with attributes. The tag will be followed by a
     * newline, and the indentation level will be increased.
     * @param tag the tag name
     * @param names the names of the attributes
     * @param values the values of the attributes
     * @param nattr the number of attributes
     */
    public void start(String tag, String[] names, String[] values, int nattr)
    {
        tag(tag, names, values, nattr, false);
    }
    
    /**
     * Internal method for printing a tag with a single attribute.
     * @param tag the tag name
     * @param name the name of the attribute
     * @param value the value of the attribute
     * @param close true to close the tag, false to leave it
     * open and adjust the spacing
     */
    protected void tag(String tag, String name, String value, boolean close) {
        spacing();
        m_out.print('<');
        m_out.print(tag);
        m_out.print(' ');
        m_out.print(name);
        m_out.print('=');
        m_out.print('\"');
        escapeString(value);
        m_out.print('\"');
        if ( close ) m_out.print('/');
        m_out.print('>');
        println();
        
        if ( !close ) {
            m_tagStack.add(tag);
        }
    }
    
    /**
     * Print a closed tag with one attribute. The tag will be followed by a
     * newline.
     * @param tag the tag name
     * @param name the name of the attribute
     * @param value the value of the attribute
     */
    public void tag(String tag, String name, String value)
    {
        tag(tag, name, value, true);
    }
    
    /**
     * Print a start tag with one attribute. The tag will be followed by a
     * newline, and the indentation level will be increased.
     * @param tag the tag name
     * @param name the name of the attribute
     * @param value the value of the attribute
     */
    public void start(String tag, String name, String value)
    {
        tag(tag, name, value, false);
    }
    
    /**
     * Internal method for printing a tag with attributes.
     * @param tag the tag name
     * @param names the names of the attributes
     * @param values the values of the attributes
     * @param nattr the number of attributes
     * @param close true to close the tag, false to leave it
     * open and adjust the spacing
     */
    protected void tag(String tag, ArrayList names, ArrayList values,
            int nattr, boolean close)
    {
        spacing();
        m_out.print('<');
        m_out.print(tag);
        for ( int i=0; i<nattr; ++i ) {
            m_out.print(' ');
            m_out.print((String)names.get(i));
            m_out.print('=');
            m_out.print('\"');
            escapeString((String)values.get(i));
            m_out.print('\"');
        }
        if ( close ) m_out.print('/');
        m_out.print('>');
        println();
        
        if ( !close ) {
            m_tagStack.add(tag);
        }
    }
    
    /**
     * Print a closed tag with attributes. The tag will be followed by a
     * newline.
     * @param tag the tag name
     * @param names the names of the attributes
     * @param values the values of the attributes
     * @param nattr the number of attributes
     */
    public void tag(String tag, ArrayList names, ArrayList values, int nattr)
    {
        tag(tag, names, values, nattr, true);
    }
    
    /**
     * Print a start tag with attributes. The tag will be followed by a
     * newline, and the indentation level will be increased.
     * @param tag the tag name
     * @param names the names of the attributes
     * @param values the values of the attributes
     * @param nattr the number of attributes
     */
    public void start(String tag, ArrayList names, ArrayList values, int nattr)
    {
        tag(tag, names, values, nattr, false);
    }
    
    /**
     * Print a start tag without attributes. The tag will be followed by a
     * newline, and the indentation level will be increased.
     * @param tag the tag name
     */
    public void start(String tag) {
        tag(tag, (String[])null, null, 0, false);
    }

    /**
     * Close the most recently opened tag. The tag will be followed by a
     * newline, and the indentation level will be decreased.
     */
    public void end() {
        String tag = (String)m_tagStack.remove(m_tagStack.size()-1);
        spacing();
        m_out.print('<');
        m_out.print('/');
        m_out.print(tag);
        m_out.print('>');
        println();
    }
    
    /**
     * Print a new content tag with a single attribute, consisting of an
     * open tag, content text, and a closing tag, all on one line.
     * @param tag the tag name
     * @param name the name of the attribute
     * @param value the value of the attribute, this text will be escaped
     * @param content the text content, this text will be escaped
     */
    public void contentTag(String tag, String name, String value, 
                           String content)
    {
        spacing();
        m_out.print('<'); m_out.print(tag); m_out.print(' ');
        m_out.print(name); m_out.print('=');
        m_out.print('\"'); escapeString(value); m_out.print('\"');
        m_out.print('>');    
        escapeString(content);
        m_out.print('<'); m_out.print('/'); m_out.print(tag); m_out.print('>');
        println();
    }
    
    /**
     * Print a new content tag with no attributes, consisting of an
     * open tag, content text, and a closing tag, all on one line.
     * @param tag the tag name
     * @param content the text content, this text will be escaped
     */
    public void contentTag(String tag, String content) {
        spacing();
        m_out.print('<'); m_out.print(tag); m_out.print('>');
        escapeString(content);
        m_out.print('<'); m_out.print('/'); m_out.print(tag); m_out.print('>');
        println();
    }
    
    /**
     * Print content text.
     * @param content the content text, this text will be escaped
     */
    public void content(String content) {
        escapeString(content);
    }
    
    /**
     * Finish the XML document.
     */
    public void finish() {
        m_bias = 0;
        m_out.flush();
    }
    
    /**
     * Finish the XML document, printing the given footer text at the
     * end of the document.
     * @param footer the footer text, this will not be escaped
     */
    public void finish(String footer) {
        m_bias = 0;
        m_out.print(footer);
        m_out.flush();
    }
    
    /**
     * Print the current spacing (determined by the indentation level)
     * into the document. This method is used by many of the other
     * formatting methods, and so should only need to be called in
     * the case of custom text printing outside the mechanisms
     * provided by this class.
     */
    public void spacing() {
        int len = m_bias + m_tagStack.size() * m_tab;
        for ( int i=0; i<len; ++i )
            m_out.print(' ');
    }
    
    // ------------------------------------------------------------------------
    // Escape Text
    
    // unicode ranges and valid/invalid characters
    private static final char   LOWER_RANGE = 0x20;
    private static final char   UPPER_RANGE = 0x7f;
    private static final char[] VALID_CHARS = { 0x9, 0xA, 0xD };
    
    private static final char[] INVALID = { '<', '>', '"', '\'', '&' };
    private static final String[] VALID = 
        { "&lt;", "&gt;", "&quot;", "&apos;", "&amp;" };
    
    /**
     * Escape a string such that it is safe to use in an XML document.
     * @param str the string to escape
     */
    protected void escapeString(String str) {
        if ( str == null ) {
            m_out.print("null");
            return;
        }
        
        int len = str.length();
        for (int i = 0; i < len; ++i) {
            char c = str.charAt(i);
            
            if ( (c < LOWER_RANGE     && c != VALID_CHARS[0] && 
                  c != VALID_CHARS[1] && c != VALID_CHARS[2]) 
                 || (c > UPPER_RANGE) )
            {
                // character out of range, escape with character value
                m_out.print("&#");
                m_out.print(Integer.toString(c));
                m_out.print(';');
            } else {
                boolean valid = true;
                // check for invalid characters (e.g., "<", "&", etc)
                for (int j=INVALID.length-1; j >= 0; --j )
                {
                    if ( INVALID[j] == c) {
                        valid = false;
                        m_out.print(VALID[j]);
                        break;
                    }
                }
                // if character is valid, don't escape
                if (valid) {
                    m_out.print(c);
                }
            }
        }
    }
    
} // end of class XMLWriter
