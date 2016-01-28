package prefuse.data.io;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import prefuse.data.Table;
import prefuse.data.parser.DataParseException;
import prefuse.data.parser.DataParser;
import prefuse.data.parser.ParserFactory;
import prefuse.data.parser.TypeInferencer;
import prefuse.util.collections.ByteArrayList;
import prefuse.util.io.IOLib;

/**
 * Abstract base class for TableReader instances that read in a table
 * from a textual data file.
 * 
 * @author <a href="http://jheer.org">jeffrey heer</a>
 */
public abstract class AbstractTextTableReader extends AbstractTableReader {

    private ParserFactory m_pfactory;
    private boolean m_hasHeader;
    
    /**
     * Create a new AbstractTextTableReader using a default ParserFactory.
     */
    public AbstractTextTableReader() {
        this(ParserFactory.getDefaultFactory());
    }

    /**
     * Create a new AbstractTextTableReader.
     * @param parserFactory the ParserFactory to use for parsing text strings
     * into table values.
     */
    public AbstractTextTableReader(ParserFactory parserFactory) {
        m_pfactory = parserFactory;
        m_hasHeader = true;
    }
    
    /**
     * Set whether or not the table data file includes a header row.
     * @param hasHeaderRow true if the the data file includes a header row,
     * false otherwise.
     */
    public void setHasHeader(boolean hasHeaderRow) {
        m_hasHeader = hasHeaderRow;
    }
    
    /**
     * @see prefuse.data.io.AbstractTableReader#readTable(java.io.InputStream)
     */
    public Table readTable(InputStream is) throws DataIOException {
        
        // determine input stream capabilities
        // if we can't reset the stream, we read in all the bytes
        // and make our own local stream
        ByteArrayList buf = null;
        if ( is.markSupported() ) {
            // mark the stream to our reset point
            is.mark(Integer.MAX_VALUE);
        } else {
            // load in the entirety of the input stream
            try {
                buf = IOLib.readAsBytes(is);
            } catch ( IOException ioe ) {
                throw new DataIOException(ioe);
            }
            // create our own input stream
            is = buf.getAsInputStream();
        }
        
        final TypeInferencer di = new TypeInferencer(m_pfactory);
        final ArrayList headers = getColumnNames();
        final int[] dim = new int[] { 0, 0 };
        
        TableReadListener scanner = new TableReadListener() {
            int prevLine = -1;
            public void readValue(int line, int col, String value)
                throws DataParseException
            {
                // sample value to determine data type
                if ( line > 1 || !m_hasHeader ) {
                    di.sample(col-1, value);
                    
                    // update num rows
                    if ( line != prevLine ) {
                        prevLine = line;
                        dim[0]++;
                    }
                } else if ( line == 1 && m_hasHeader ) {
                    headers.add(value);
                }
                
                // update num cols
                if ( col > dim[1] )
                    dim[1] = col;
            }
        };
        
        // do a scan of the stream, collecting length and type data
        try {
            read(is, scanner);
        } catch ( IOException ioe ) {
            throw new DataIOException(ioe);
        } catch ( DataParseException de ) {
            // can't happen
        }
        
        // create the table
        int nrows = dim[0];
        int ncols = dim[1];
        final Table table = new Table(nrows, ncols);
        
        // create the table columns
        for ( int i=0; i < ncols; ++i ) {
            String header;
            if ( m_hasHeader || i < headers.size() ) {
                header = (String)headers.get(i);
            } else {
                header = getDefaultHeader(i);
            }
            table.addColumn(header, di.getType(i));
            table.getColumn(i).setParser(di.getParser(i));
        }
        
        // reset dim array, will hold row/col indices
        dim[0] = dim[1] = -1;
        
        TableReadListener parser = new TableReadListener() {
            int prevLine = -1;
            public void readValue(int line, int col, String value)
                throws DataParseException
            {
                // early exit on header value
                if ( line == 1 && m_hasHeader )
                    return;
                if ( line != prevLine ) {
                    prevLine = line;
                    ++dim[0];
                }
                dim[1] = col-1;
                
                // XXX NOTE-2005.08.29-jheer
                // For now we use generic routines for filling column values.
                // This results in the autoboxing of primitive types, slowing
                // performance a bit and possibly triggering avoidable garbage
                // collections. If this proves to be a problem down the road,
                // we can add more nuance later.
                DataParser dp = di.getParser(dim[1]);
                table.set(dim[0], dim[1], dp.parse(value));
            }
        };
        
        // read the data into the table
        try {
            // prepare the input stream
            if ( is.markSupported() ) {
                is.reset();
            } else {
                is = buf.getAsInputStream();
            }
            // read the data
            read(is, parser);
        } catch ( IOException ioe ) {
            throw new DataIOException(ioe);
        } catch ( DataParseException de ) {
            throw new DataIOException("Parse exception for column "
                    + '\"' + dim[1] + '\"' + " at row: " + dim[0], de);
        }
        
        return table;
    }
    
    /**
     * Subclasses can override this to provide column names through
     * a custom mechanism.
     * @return an ArrayList of String instances indicating the column names
     */
    protected ArrayList getColumnNames() {
    	return new ArrayList();
    }
    
    /**
     * Returns default column header names of the type "A", "B", ...,
     * "Z", "AA", "AB", etc.
     * @param idx the index of the column header
     * @return a default column header name for the given index.
     */
    public static String getDefaultHeader(int idx) {
        if ( idx == 0 ) return "A";
        int len = ((int)(Math.log(idx) / Math.log(26))) + 1;
        char[] h = new char[len];
        int p = len;
        
        h[--p] = (char)('A'+(idx%26));
        idx = idx / 26;
        
        while ( idx > 26 ) {
            h[--p] = (char)('A'+(idx%26));
            idx = idx/26;
        }
        if ( idx > 0 ) {
            h[--p] = (char)('A'+((idx-1)%26));
        }

        return new String(h, p, len);
    }
    
    /**
     * Scans the input stream, making call backs for each encountered entry
     * on the provided TextReadListener.
     * @param is the InputStream to read
     * @param trl the TextReadListener that will receive callbacks
     * @throws IOException
     * @throws DataParseException
     */
    protected abstract void read(InputStream is, TableReadListener trl)
        throws IOException, DataParseException;

} // end of abstract class AbstractTextTableReader
