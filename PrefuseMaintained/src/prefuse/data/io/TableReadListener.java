package prefuse.data.io;

import prefuse.data.parser.DataParseException;

/**
 * Callback interface used by AbstractTextTableReader instances to be
 * used when a table value is encountered in parsing.
 * 
 * @author <a href="http://jheer.org">jeffrey heer</a>
 */
public interface TableReadListener {

    /**
     * Notification that a text string representing a table value has
     * been read. It is the job of this callback to then appropriately
     * take action, such as parse and store the value.
     * @param line the line of the file at which the value was encountered
     * @param col the table column index at which the value was encountered
     * @param value the text string representing the data value
     * @throws DataParseException if an error occurs while parsing the data
     */
    public void readValue(int line, int col, String value)
        throws DataParseException;
    
} // end of interface TableReadListener
