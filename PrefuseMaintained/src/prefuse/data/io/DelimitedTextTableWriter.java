package prefuse.data.io;

import java.io.BufferedOutputStream;
import java.io.OutputStream;
import java.io.PrintStream;

import prefuse.data.Table;
import prefuse.util.collections.IntIterator;

/**
 * TableWriter that writes out a delimited text table, using a designated
 * character string to demarcate data columns. By default, a header row
 * containing the column names is included in the output.
 * 
 * @author <a href="http://jheer.org">jeffrey heer</a>
 */
public class DelimitedTextTableWriter extends AbstractTableWriter {

    private String  m_delim;
    private boolean m_printHeader;
    
    /**
     * Create a new DelimitedTextTableWriter that writes tab-delimited
     * text files.
     */
    public DelimitedTextTableWriter() {
        this("\t");
    }
    
    /**
     * Create a new DelimitedTextTableWriter.
     * @param delimiter the delimiter string to use between columns
     */
    public DelimitedTextTableWriter(String delimiter) {
        this(delimiter, true);
    }
    
    /**
     * Create a new DelimitedTextTableWriter.
     * @param delimiter the delimiter string to use between columns
     * @param printHeader indicates if a header row should be printed
     */
    public DelimitedTextTableWriter(String delimiter, boolean printHeader) {
        m_delim = delimiter;
        m_printHeader = printHeader;
    }

    // ------------------------------------------------------------------------    
    
    /**
     * Get the delimiter used to separate data fields.
     * @return the delimiter string
     */
    public String getDelimiter() {
        return m_delim;
    }

    /**
     * Set the delimiter used to separate data fields.
     * @param delimiter the delimiter string
     */
    public void setDelimeter(String delimiter) {
        m_delim = delimiter;
    }

    /**
     * Indicates if this writer will write a header row with the column names.
     * @return true if a header row will be printed, false otherwise
     */
    public boolean isPrintHeader() {
        return m_printHeader;
    }

    /**
     * Sets if this writer will write a header row with the column names.
     * @param printHeader true to print a header row, false otherwise
     */
    public void setPrintHeader(boolean printHeader) {
        m_printHeader = printHeader;
    }    
    
    // ------------------------------------------------------------------------

    /**
     * @see prefuse.data.io.TableWriter#writeTable(prefuse.data.Table, java.io.OutputStream)
     */
    public void writeTable(Table table, OutputStream os) throws DataIOException {
        try {            
            // get print stream
            PrintStream out = new PrintStream(new BufferedOutputStream(os));
            
            // write out header row
            if ( m_printHeader ) {
                for ( int i=0; i<table.getColumnCount(); ++i ) {
                    if ( i>0 ) out.print(m_delim);
                    out.print(table.getColumnName(i));
                }
                out.println();
            }
            
            // write out data
            for ( IntIterator rows = table.rows(); rows.hasNext(); ) {
                int row = rows.nextInt();
                for ( int i=0; i<table.getColumnCount(); ++i ) {
                    if ( i>0 ) out.print(m_delim);
                    out.print(table.getString(row, table.getColumnName(i)));
                }
                out.println();
            }
            
            // finish up
            out.flush();
        } catch ( Exception e ) {
            throw new DataIOException(e);
        }
    }

} // end of class DelimitedTextTableWriter
