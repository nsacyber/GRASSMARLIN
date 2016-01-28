package prefuse.data.io;

import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;

import prefuse.util.io.IOLib;

/**
 * Helper class that stores character length counts for each column in
 * a fixed width text table. This class is needed for reading and writing
 * fixed width data tables. A schema definition can either be created
 * manually using the {@link #addColumn(String, int)} method or loaded from
 * a tab-delimited text file using the {@link #load(String)} method.
 * 
 * @author <a href="http://jheer.org">jeffrey heer</a>
 */
public class FixedWidthTextTableSchema {
	
	private String[] names = new String[0];
	private int[] cols = new int[1];
	
	/**
	 * Creates a new, initially empty FixedWidthTextTableSchema.
	 */
	public FixedWidthTextTableSchema() {
		
	}
	
	private void ensureCapacity(int cap) {
		String[] nnames = new String[names.length+1];
		System.arraycopy(names, 0, nnames, 0, names.length);
		names = nnames;
		
		int[] ncols = new int[cols.length+1];
		System.arraycopy(cols, 0, ncols, 0, cols.length);
		cols = ncols;
	}
	
	/**
	 * Adds a column to this schema description.
	 * @param name the name of this column
	 * @param length the length, in text characters, of this column in a data file
	 */
	public void addColumn(String name, int length) {
		int idx = names.length;
		ensureCapacity(idx+1);
		names[idx] = name;
		cols[idx+1] = cols[idx]+length;
	}
	
	/**
	 * Returns the number of columns in this schema.
	 * @return the numner of columns
	 */
	public int getColumnCount() {
		return names.length;
	}
	
	/**
	 * Gets the name of the requested column
	 * @param idx the index of the column
	 * @return the name of the column
	 */
	public String getColumnName(int idx) {
		return names[idx];
	}
	
	/**
	 * Gets the character length of the given column
	 * @param idx the index of the column
	 * @return the character length of the column in the fixed-width format
	 */
	public int getColumnLength(int idx) {
		return cols[idx+1]-cols[idx];
	}
	
	/**
	 * Gets the starting character number for the given column index
	 * @param idx the index of the column
	 * @return the text character position at which this column starts on a
	 * line
	 */
	public int getColumnStart(int idx) {
		return cols[idx];
	}
	
	/**
	 * Sets the ending character number for the given column index. This value
	 * is one greater than the last character position for the column.
	 * @param idx the index of the column
	 * @return one greater than the last text character position at which this
	 * column ends on a line
	 */
	public int getColumnEnd(int idx) {
		return cols[idx+1];
	}
	
	/**
	 * Writes this schema description to a file with the given name.
	 * @param filename the name of the file
	 * @throws DataIOException if an IO exception occurs
	 */
	public void write(String filename) throws DataIOException {
		try {
            write(new FileOutputStream(filename));
        } catch ( FileNotFoundException e ) {
            throw new DataIOException(e);
        }
	}
	
	/**
	 * Writes this schema description to the given output stream.
	 * @param os the output stream
	 * @throws DataIOException if an IO exception occurs
	 */
	public void write(OutputStream os) throws DataIOException {
		try {
			PrintStream out = new PrintStream(new BufferedOutputStream(os));
			for (int i = 0; i < names.length; ++i) {
				out.print(names[i]);
				out.print('\t');
				out.print(cols[i+1]-cols[i]);
				out.println();
			}
		} catch ( Exception e ) {
			throw new DataIOException(e);
		}
	}
	
	/**
	 * Loads a schema description from the given location.
	 * @param loc a location string representing a filename, URL, or resource locator
	 * @return the loaded schema description
	 * @throws DataIOException if an IO exception occurs
	 */
	public static FixedWidthTextTableSchema load(String loc) throws DataIOException {
		try {
			InputStream is = IOLib.streamFromString(loc);
			if (is == null) return null;
			FixedWidthTextTableSchema fws = new FixedWidthTextTableSchema();
			BufferedReader br = new BufferedReader(new InputStreamReader(is));
			String line;
			
			while ((line=br.readLine()) != null) {
				String[] tok = line.split("\t");
				fws.addColumn(tok[0], Integer.parseInt(tok[1]));
			}
			
			return fws;
		} catch ( Exception e ) {
			throw new DataIOException(e);
		}
	}
	
} // end of class FixedWidthTextTableReader