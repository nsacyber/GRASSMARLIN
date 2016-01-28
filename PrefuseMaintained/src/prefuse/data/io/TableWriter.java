package prefuse.data.io;

import java.io.File;
import java.io.OutputStream;

import prefuse.data.Table;

/**
 * Interface for classes that write Table data to a particular file format.
 * 
 * @author <a href="http://jheer.org">jeffrey heer</a>
 */
public interface TableWriter {

    /**
     * Write a table to the file with the given filename.
     * @param table the Table to write
     * @param filename the file to write the table to
     * @throws DataWriteException
     */
    public void writeTable(Table table, String filename) throws DataIOException;
    
    /**
     * Write a table to the given File.
     * @param table the Table to write
     * @param f the file to write the table to
     * @throws DataWriteException
     */
    public void writeTable(Table table, File f) throws DataIOException;
    
    /**
     * Write a table from the given OutputStream.
     * @param table the Table to write
     * @param os the OutputStream to write the table to
     * @throws DataWriteException
     */
    public void writeTable(Table table, OutputStream os) throws DataIOException;
    
} // end of interface TableWriter
