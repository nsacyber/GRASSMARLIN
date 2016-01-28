package prefuse.data.io;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import prefuse.data.parser.DataParseException;
import prefuse.data.parser.ParserFactory;

/**
 * TableReader for Comma Separated Value (CSV) files. CSV files list
 * each row of a table on a line, separating each data column by a line.
 * Typically the first line of the file is a header row indicating the
 * names of each data column.
 * 
 * For a more in-depth description of the CSV format, please see this
 * <a href="http://www.creativyst.com/Doc/Articles/CSV/CSV01.htm">
 *  CSV reference web page</a>.
 * 
 * @author <a href="http://jheer.org">jeffrey heer</a>
 */
public class CSVTableReader extends AbstractTextTableReader {

    /**
     * Create a new CSVTableReader.
     */
    public CSVTableReader() {
        super();
    }
    
    /**
     * Create a new CSVTableReader.
     * @param parserFactory the ParserFactory to use for parsing text strings
     * into table values.
     */
    public CSVTableReader(ParserFactory parserFactory) {
        super(parserFactory);
    }
    
    /**
     * @see prefuse.data.io.AbstractTextTableReader#read(java.io.InputStream, prefuse.data.io.TableReadListener)
     */
    public void read(InputStream is, TableReadListener trl)
        throws IOException, DataParseException
    {
        String line;
        StringBuffer sbuf = new StringBuffer();
        
        boolean inRecord = false;
        int inQuote  = 0;
        int lineno   = 0;
        int col      = 0;
        
        BufferedReader br = new BufferedReader(new InputStreamReader(is));
        while ( (line=br.readLine()) != null ) {
            // increment the line number
            ++lineno;
            
            // extract the character array for quicker processing
            char[] c = line.toCharArray();
            int last = c.length-1;
            
            // iterate through current line
            for ( int i=0; i<=last; ++i ) {
                if ( !inRecord ) {
                    // not currently processing a record
                    if ( Character.isWhitespace(c[i]) )
                    {
                        continue;
                    }
                    else if ( c[i] == '\"' )
                    {
                        inRecord = true;
                        inQuote  = 1;
                    }
                    else if ( c[i] == ',' )
                    {
                        String s = sbuf.toString().trim();
                        trl.readValue(lineno, ++col, s);
                        sbuf.delete(0, sbuf.length());
                    }
                    else
                    {
                        inRecord = true;
                        sbuf.append(c[i]);
                    }
                } else {
                    // in the midst of a record
                    if ( inQuote == 1 ) {
                        if ( c[i]=='\"' && (i==last || c[i+1] != '\"') )
                        {
                            // end of quotation
                            inQuote = 2;
                        }
                        else if ( c[i]=='\"' )
                        {
                            // double quote so skip one ahead
                            sbuf.append(c[i++]);
                        }
                        else
                        {
                            sbuf.append(c[i]);
                        }
                    } else {
                        if ( Character.isWhitespace(c[i]) )
                        {
                            sbuf.append(c[i]);
                        }
                        else if ( c[i] != ',' && inQuote == 2 )
                        {
                            throw new IllegalStateException(
                                "Invalid data format. " + 
                                "Error at line " + lineno + ", col " + i);
                        }
                        else if ( c[i] != ',' )
                        {
                            sbuf.append(c[i]);
                        }
                        else
                        {
                            String s = sbuf.toString().trim();
                            trl.readValue(lineno, ++col, s);
                            sbuf.delete(0, sbuf.length());
                            inQuote = 0;
                            inRecord = false;
                        }
                    }
                }
            }
            if ( inQuote != 1 ) {
                String s = sbuf.toString().trim();
                trl.readValue(lineno, ++col, s);
                sbuf.delete(0, sbuf.length());
                inQuote = 0;
                inRecord = false;
            }
            if ( !inRecord && col > 0 ) {
                col = 0;
            }
        }
    }
    
} // end of class CSVTableReader
