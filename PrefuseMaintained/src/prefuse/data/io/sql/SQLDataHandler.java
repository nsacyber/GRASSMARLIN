package prefuse.data.io.sql;

import java.sql.ResultSet;
import java.sql.SQLException;

import prefuse.data.Table;

/**
 * Interface for taking a value in a SQL ResultSet and translating it into
 * a Java data value for use in a prefuse Table.
 * 
 * @author <a href="http://jheer.org">jeffrey heer</a>
 */
public interface SQLDataHandler {

    /**
     * Process a data value from a ResultSet, translating it into a
     * Java data value and storing it in a Table.
     * @param t the Table in which to store the result value
     * @param trow the Table row to add to
     * @param rset the ResultSet to read the SQL value from, assumed
     * to be set to the desired row
     * @param rcol the column index of the data value in the row set.
     * This is also used to look up the column name, which is used
     * to access the correct data field of the Table.
     * @throws SQLException if an error occurs accessing the ResultSet
     */
    public void process(Table t, int trow, ResultSet rset, int rcol)
        throws SQLException;
    
    /**
     * Return the Java data type for the given data field name and
     * its sql data type.
     * @param columnName the name of data field / column
     * @param sqlType the field's sql data type, one of the constants
     * in the {@link java.sql.Types} class.
     * @return the Java Class data type
     */
    public Class getDataType(String columnName, int sqlType);
    
} // end of interface SQLDataValueHandler
