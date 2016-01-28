package prefuse.data.io;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import prefuse.data.parser.DataParseException;
import prefuse.data.parser.ParserFactory;

/**
 * TableReader for delimited text files, such as tab-delimited or
 * pipe-delimited text files. Such files typically list one row of table
 * data per line of the file, using a designated character such as a tab
 * (\t) or pipe (|) to demarcate different data columns. This class
 * allows you to select any regular expression as the column
 * delimiter.
 * 
 * @author <a href="http://jheer.org">jeffrey heer</a>
 */
public class DelimitedTextTableReader extends AbstractTextTableReader {

    private String m_delim;
   
    /**
     * Create a new DelimitedTextTableReader for reading tab-delimited files
     * using a default parser factory.
     */
    public DelimitedTextTableReader() {
        this("\t");
    }
    
    /**
     * Create a new DelimitedTextTableReader for reading tab-delimited files.
     * @param parserFactory the ParserFactory to use for parsing text strings
     * into table values.
     */
    public DelimitedTextTableReader(ParserFactory parserFactory) {
        this("\t", parserFactory);
    }
    
    /**
     * Create a new DelimitedTextTableReader using a default parser factory.
     * @param delimiterRegex a regular expression string indicating the
     * delimiter to use to separate column values
     */
    public DelimitedTextTableReader(String delimiterRegex) {
        m_delim = delimiterRegex;
    }
    
    /**
     * Create a new DelimitedTextTableReader.
     * @param delimiterRegex a regular expression string indicating the
     * delimiter to use to separate column values
     * @param pf the ParserFactory to use for parsing text strings
     * into table values.
     */
    public DelimitedTextTableReader(String delimiterRegex, ParserFactory pf) {
        super(pf);
        m_delim = delimiterRegex;
    }
    
    // ------------------------------------------------------------------------
    
    /**
     * @see prefuse.data.io.AbstractTextTableReader#read(java.io.InputStream, prefuse.data.io.TableReadListener)
     */
    protected void read(InputStream is, TableReadListener trl)
            throws IOException, DataParseException
    {
        String line;
        int lineno   = 0;
        
        BufferedReader br = new BufferedReader(new InputStreamReader(is));
        while ( (line=br.readLine()) != null ) {
            // increment the line number
            ++lineno;
            
            // split on tab character
            String[] cols = line.split(m_delim);
            for ( int i=0; i<cols.length; ++i ) {
                trl.readValue(lineno, i+1, cols[i]);
            }
        }
    }

} // end of class DelimitedTextTableReader
