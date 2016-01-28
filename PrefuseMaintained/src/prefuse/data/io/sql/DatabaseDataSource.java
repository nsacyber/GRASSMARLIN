package prefuse.data.io.sql;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Logger;

import prefuse.data.Schema;
import prefuse.data.Table;
import prefuse.data.io.DataIOException;
import prefuse.data.util.Index;

/**
 * Sends queries to a relational database and processes the results, storing
 * the results in prefuse Table instances. This class should not be
 * instantiated directly. To access a database, the {@link ConnectionFactory}
 * class should be used to retrieve an appropriate instance of this class.
 * 
 * @author <a href="http://jheer.org">jeffrey heer</a>
 */
public class DatabaseDataSource {

    // logger
    private static final Logger s_logger 
        = Logger.getLogger(DatabaseDataSource.class.getName());
    
    protected Connection       m_conn;
    protected Statement        m_stmt;
    protected SQLDataHandler m_handler;
    
    // ------------------------------------------------------------------------
    
    /**
     * Creates a new DatabaseDataSource for reading data from a SQL relational
     * database. This constructor is only package visible and is not intended
     * for use by application level code. Instead, the
     * {@link ConnectionFactory} class should be used to create any number of
     * DatabaseDataSource connections.
     */
    DatabaseDataSource(Connection conn, SQLDataHandler handler) {
        m_conn = conn;
        m_handler = handler;
    }
    
    // ------------------------------------------------------------------------
    // Synchronous Data Retrieval
    
    /**
     * Executes a query and returns the results in a Table instance.
     * @param query the text SQL query to execute
     * @return a Table of the query results
     * @throws DataIOException if an error occurs while executing the query 
     * or adding the query results in a prefuse Table.
     */
    public synchronized Table getData(String query) throws DataIOException {
        return getData(null, query, null);
    }

    /**
     * Executes a query and returns the results in a Table instance.
     * @param query the text SQL query to execute
     * @param keyField the field to treat as a primary key, ensuring that this
     *  field is indexed in the resulting table instance.
     * @return a Table of the query results
     * @throws DataIOException if an error occurs while executing the query 
     * or adding the query results in a prefuse Table.
     */
    public synchronized Table getData(String query, String keyField)
        throws DataIOException
    {
        return getData(null, query, keyField);
    }
    
    /**
     * Executes a query and returns the results in a Table instance.
     * @param t the Table to store the results in. If this value is null, a
     * new table will automatically be created.
     * @param query the text SQL query to execute
     * @return a Table of the query results
     * @throws DataIOException if an error occurs while executing the query 
     * or adding the query results in a prefuse Table.
     */
    public synchronized Table getData(Table t, String query) 
        throws DataIOException
    {
        return getData(t, query, null);
    }
    
    /**
     * Executes a query and returns the results in a Table instance.
     * @param t the Table to store the results in. If this value is null, a
     * new table will automatically be created.
     * @param query the text SQL query to execute
     * @param keyField used to determine if the row already exists in the table
     * @return a Table of the query results
     * @throws DataIOException if an error occurs while executing the query 
     * or adding the query results in a prefuse Table.
     */
    public synchronized Table getData(Table t, String query, String keyField) 
        throws DataIOException
    {
        return getData(t, query, keyField, null);
    }
    
    /**
     * Executes a query and returns the results in a Table instance.
     * @param t the Table to store the results in. If this value is null, a
     * new table will automatically be created.
     * @param query the text SQL query to execute
     * @param keyField used to determine if the row already exists in the table
     * @param lock an optional Object to use as a lock when performing data
     *  processing. This lock will be synchronized on whenever the Table is
     *  modified.
     * @return a Table of the query results
     * @throws DataIOException if an error occurs while executing the query 
     * or adding the query results in a prefuse Table.
     */
    public synchronized Table getData(Table t, String query, 
                                      String keyField, Object lock) 
        throws DataIOException
    {
        ResultSet rs;
        try {
            rs = executeQuery(query);
        } catch ( SQLException e ) {
            throw new DataIOException(e);
        }
        return process(t, rs, keyField, lock);
    }
    
    // ------------------------------------------------------------------------
    // Asynchronous Data Retrieval

    /**
     * Asynchronously executes a query and stores the results in the given 
     * table instance. All data processing is done in a separate thread of
     * execution.
     * @param t the Table in which to store the results
     * @param query the query to execute
     */
    public void loadData(Table t, String query) {
        loadData(t, query, null, null, null);
    }

    /**
     * Asynchronously executes a query and stores the results in the given 
     * table instance. All data processing is done in a separate thread of
     * execution.
     * @param t the Table in which to store the results
     * @param query the query to execute
     * @param keyField the primary key field, comparisons on this field are
     *  performed to recognize data records already present in the table.
     */
    public void loadData(Table t, String query, String keyField) {
        loadData(t, query, keyField, null, null);
    }
    
    /**
     * Asynchronously executes a query and stores the results in the given 
     * table instance. All data processing is done in a separate thread of
     * execution.
     * @param t the Table in which to store the results
     * @param query the query to execute
     * @param lock an optional Object to use as a lock when performing data
     *  processing. This lock will be synchronized on whenever the Table is
     *  modified.
     */
    public void loadData(Table t, String query, Object lock) {
        loadData(t, query, null, lock, null);
    }
    
    /**
     * Asynchronously executes a query and stores the results in the given 
     * table instance. All data processing is done in a separate thread of
     * execution.
     * @param t the Table in which to store the results
     * @param query the query to execute
     * @param keyField the primary key field, comparisons on this field are
     *  performed to recognize data records already present in the table.
     * @param lock an optional Object to use as a lock when performing data
     *  processing. This lock will be synchronized on whenever the Table is
     *  modified.
     */
    public void loadData(Table t, String query, String keyField, Object lock) {
        loadData(t, query, keyField, lock, null);
    }
    
    /**
     * Asynchronously executes a query and stores the results in the given 
     * table instance. All data processing is done in a separate thread of
     * execution.
     * @param t the Table in which to store the results
     * @param query the query to execute
     * @param keyField the primary key field, comparisons on this field are
     *  performed to recognize data records already present in the table.
     *  A null value will result in no key checking.
     * @param lock an optional Object to use as a lock when performing data
     *  processing. This lock will be synchronized on whenever the Table is
     *  modified. A null value will result in no locking.
     * @param listener an optional listener that will provide notifications
     *  before the query has been issued and after the query has been 
     *  processed. This is most useful for post-processing operations.
     */
    public void loadData(Table t, String query, String keyField, 
                         Object lock, DataSourceWorker.Listener listener) {
        DataSourceWorker.Entry e = new DataSourceWorker.Entry(
                this, t, query, keyField, lock, listener);
        DataSourceWorker.submit(e);
    }
    
    // ------------------------------------------------------------------------
    
    /**
     * Execute a query and return the corresponding result set
     * @param query the text SQL query to execute
     * @return the ResultSet of the query
     * @throws SQLException if an error occurs issuing the query
     */
    private ResultSet executeQuery(String query) throws SQLException {
        if ( m_stmt == null )
            m_stmt = m_conn.createStatement();
        
        // clock in
        long timein = System.currentTimeMillis();
        
        s_logger.info("Issuing query: "+query);
        ResultSet rset = m_stmt.executeQuery(query);
        
        // clock out
        long time = System.currentTimeMillis()-timein;
        s_logger.info("External query processing completed: "
                + (time/1000) + "." + (time%1000) + " seconds.");
        
        return rset;
    }
    
    // ------------------------------------------------------------------------
    
    /**
     * Process the results of a SQL query, putting retrieved data into a
     * Table instance. If a null table is provided, a new table with the
     * appropriate schema will be created.
     * @param t the Table to store results in
     * @param rset the SQL query result set
     * @return a Table containing the query results
     */
    protected Table process(Table t, ResultSet rset, String key, Object lock)
        throws DataIOException
    {
        // clock in
        int count = 0;
        long timein = System.currentTimeMillis();

        try {
            ResultSetMetaData metadata = rset.getMetaData();
            int ncols = metadata.getColumnCount();
    
            // create a new table if necessary
            if ( t == null ) {
                t = getSchema(metadata, m_handler).instantiate();
                if ( key != null ) {
                    try {
                        t.index(key);
                        s_logger.info("Indexed field: "+key);
                    } catch ( Exception e ) {
                        s_logger.warning("Error indexing field: "+key);
                    }
                }
            }

            // set the lock, lock on the table itself if nothing else provided
            lock = (lock==null ? t : lock);
            
            // process the returned rows
            while ( rset.next() )
            {
                synchronized ( lock ) {
                    // determine the table row index to use
                    int row = getExistingRow(t, rset, key);
                    if ( row < 0 ) {
                        row = t.addRow();
                    }
                    
                    //process each value in the current row
                    for ( int i=1; i<=ncols; ++i ) {
                        m_handler.process(t, row, rset, i);
                    }
                }
                
                // increment row count
                ++count;
            }
        } catch ( SQLException e ) {
            throw new DataIOException(e);
        }
        
        // clock out
        long time = System.currentTimeMillis()-timein;
        s_logger.info("Internal query processing completed: "+count+" rows, "
                + (time/1000) + "." + (time%1000) + " seconds.");
        
        return t;
    }
    
    /**
     * See if a retrieved database row is already represented in the given
     * Table.
     * @param t the prefuse Table to check for an existing row
     * @param rset the ResultSet, set to a particular row, which may or
     * may not have a matching row in the prefuse Table
     * @param keyField the key field to look up to check for an existing row
     * @return the index of the existing row, or -1 if no match is found
     * @throws SQLException
     */
    protected int getExistingRow(Table t, ResultSet rset, String keyField)
        throws SQLException
    {
        // check if we have a keyField, bail if not
        if ( keyField == null )
            return -1;
        
        // retrieve the column data type, bail if column is not found
        Class type = t.getColumnType(keyField);
        if ( type == null )
            return -1;
        
        // get the index and perform the lookup
        Index index = t.index(keyField);
        if ( type == int.class ) {
            return index.get(rset.getInt(keyField));
        } else if ( type == long.class ) {
            return index.get(rset.getLong(keyField));
        } else if ( type == float.class ) {
            return index.get(rset.getFloat(keyField));
        } else if ( type == double.class ) {
            return index.get(rset.getDouble(keyField));
        } else if ( !type.isPrimitive() ) {
            return index.get(rset.getObject(keyField));
        } else {
            return -1;
        }
    }
    
    /**
     * Given the metadata for a SQL result set and a data value handler for that
     * result set, returns a corresponding schema for a prefuse table.
     * @param metadata the SQL result set metadata
     * @param handler the data value handler
     * @return the schema determined by the metadata and handler
     * @throws SQLException if an error occurs accessing the metadata
     */
    public Schema getSchema(ResultSetMetaData metadata, SQLDataHandler handler)
        throws SQLException
    {
        int ncols = metadata.getColumnCount();
        Schema schema = new Schema(ncols);
        
        // determine the table schema
        for ( int i=1; i<=ncols; ++i ) {
            String name = metadata.getColumnName(i);
            int sqlType = metadata.getColumnType(i);
            Class type = handler.getDataType(name, sqlType);
            if ( type != null )
                schema.addColumn(name, type);
        }
        
        return schema;
    }

} // end of class DatabaseDataSource
