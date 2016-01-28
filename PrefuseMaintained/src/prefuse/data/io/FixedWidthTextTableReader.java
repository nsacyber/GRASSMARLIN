package prefuse.data.io;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;

import prefuse.data.parser.DataParseException;

/**
 * TableReader for fixed-width text files, that encode one row of table
 * data per line use a fixed number of characters for each data column.
 * Reading such tables requires use of a schema description that describes
 * the fixed-widths for each individual column.
 * The {@link prefuse.data.io.FixedWidthTextTableSchema} class provides
 * this functionality.
 * 
 * @author <a href="http://jheer.org">jeffrey heer</a>
 */
public class FixedWidthTextTableReader extends AbstractTextTableReader {

	// the schema description
	private FixedWidthTextTableSchema m_schema;
	
	/**
	 * Creates a new FixedWidthTextTableReader using the given schema.
	 * @param schema the schema description of the fixed-width text column lengths
	 */
	public FixedWidthTextTableReader(FixedWidthTextTableSchema schema) {
		super.setHasHeader(false);
		m_schema = schema;
	}
	
	/**
	 * Creates a new FixedWidthTextTableReader using the schema at
	 * the given location.
	 * @param location a location string (filename, URL, or resource locator)
	 * for the schema description of the fixed-width text column lengths
	 * @throws DataIOException if an IO exception occurs while loading the schema
	 */
	public FixedWidthTextTableReader(String location) throws DataIOException {
		this(FixedWidthTextTableSchema.load(location));
	}
	
	// ------------------------------------------------------------------------    
    
    /**
     * Get the schema description describing the data columns' fixed widths
     * @return the fixed-width table schema description
     */
    public FixedWidthTextTableSchema getFixedWidthSchema() {
        return m_schema;
    }

    /**
     * Set the schema description describing the data columns' fixed widths
     * @param schema the fixed-width table schema description
     */
    public void setFixedWidthSchema(FixedWidthTextTableSchema schema) {
        m_schema = schema;
    }
    
	// ------------------------------------------------------------------------
    
	/**
     * @see prefuse.data.io.AbstractTextTableReader#read(java.io.InputStream, prefuse.data.io.TableReadListener)
     */
    protected void read(InputStream is, TableReadListener trl)
            throws IOException, DataParseException
    {
        String line;
        int lineno = 0;
        
        BufferedReader br = new BufferedReader(new InputStreamReader(is));
        while ( (line=br.readLine()) != null ) {
            // increment the line number
            ++lineno;
            
            // split on tab character
            for (int i=0; i<m_schema.getColumnCount(); ++i) {
            	int idx0 = m_schema.getColumnStart(i);
            	int idx1 = m_schema.getColumnEnd(i);
            	trl.readValue(lineno, i+1, line.substring(idx0, idx1));
            }
        }
    }

    /**
     * @see prefuse.data.io.AbstractTextTableReader#getColumnNames()
     */
    protected ArrayList getColumnNames() {
    	ArrayList names = new ArrayList();
    	for (int i=0; i<m_schema.getColumnCount(); ++i) {
    		names.add(m_schema.getColumnName(i));
    	}
    	return names;
    }
    
} // end of class FixedWidthTextTableReader
