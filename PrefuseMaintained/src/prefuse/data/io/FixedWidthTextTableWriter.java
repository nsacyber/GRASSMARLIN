package prefuse.data.io;

import java.io.BufferedOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;

import prefuse.data.Table;
import prefuse.util.TypeLib;
import prefuse.util.collections.IntIterator;

/**
 * TableWriter for fixed-width text files, that encode one row of table
 * data per line use a fixed number of characters for each data column.
 * Writing such tables requires use of a schema description that describes
 * the fixed-widths for each individual column.
 * The {@link prefuse.data.io.FixedWidthTextTableSchema} class provides
 * this functionality. A schema description must be written separately into
 * a different file.
 * 
 * @author <a href="http://jheer.org">jeffrey heer</a>
 */
public class FixedWidthTextTableWriter extends AbstractTableWriter {

	// the schema description
	private FixedWidthTextTableSchema m_schema;
	
	/**
	 * Creates a new FixedWidthTextTableWriter using the given schema.
	 * @param schema the schema description of the fixed-width text column lengths
	 */
	public FixedWidthTextTableWriter(FixedWidthTextTableSchema schema) {
		m_schema = schema;
	}
	
	/**
	 * Creates a new FixedWidthTextTableWriter using the schema at
	 * the given location.
	 * @param location a location string (filename, URL, or resource 
	 * locator) for the schema description of the fixed-width text column lengths
	 * @throws DataIOException if an IO exception occurs while loading the schema
	 */
	public FixedWidthTextTableWriter(String location) throws DataIOException {
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
     * @see prefuse.data.io.TableWriter#writeTable(prefuse.data.Table, java.io.OutputStream)
     */
    public void writeTable(Table table, OutputStream os) throws DataIOException {
        try {            
            // get print stream
            PrintStream out = new PrintStream(new BufferedOutputStream(os));
            
            // build array of column padding
            char[] pad = new char[table.getColumnCount()];
            boolean[] pre = new boolean[table.getColumnCount()];
            for (int i=0; i<table.getColumnCount(); ++i ) {
            	Class type = table.getColumnType(i);
            	pre[i] = TypeLib.isNumericType(type);
            	pad[i] = pre[i] ? '0' : ' ';
            }
            
            // write out data
            for ( IntIterator rows = table.rows(); rows.hasNext(); ) {
                int row = rows.nextInt();
                for ( int i=0; i<table.getColumnCount(); ++i ) {
                	out.print(pack(table.getString(row, i), 
                				   m_schema.getColumnLength(i),
                				   pre[i], pad[i]));
                }
                out.println();
            }
            
            // finish up
            out.flush();
        } catch ( Exception e ) {
            throw new DataIOException(e);
        }
    }
    
    /**
     * Pads or truncates a string as necessary to fit within the column length.
     */
    private static String pack(String value, int len, boolean prepend, char pad) {
    	int vlen = value.length();
    	if (vlen < len) {
    		StringBuffer sbuf = new StringBuffer();
    		if (prepend) sbuf.append(value);
    		for (int i=len; i<vlen; ++i)
    			sbuf.append(pad);
    		if (!prepend) sbuf.append(value);
    		return sbuf.toString();
    	} else {
    		return value.substring(0, len);
    	}
    }

} // end of class FixedWidthTextTableWriter
