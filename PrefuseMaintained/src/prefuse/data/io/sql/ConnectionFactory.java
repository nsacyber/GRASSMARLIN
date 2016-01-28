package prefuse.data.io.sql;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * @author <a href="http://jheer.org">jeffrey heer</a>
 */
public class ConnectionFactory {

    /** String constant for the commonly used MySQL JDBC driver */
    public static final String DRIVER_MYSQL = "com.mysql.jdbc.Driver";
    /** String constant for the JDBC/ODBC bridge driver */
    public static final String DRIVER_JDBC_OBDC = "sun.jdbc.odbc.JdbcOdbcDriver";
    
    /** Protocol prefix for JDBC URLs */
    public static final String PROTOCOL_JDBC = "jdbc:";
    /** Sub-protocol prefix for MySQL connections */
    public static final String SUBPROTOCOL_MYSQL = "mysql:";
    /** Sub-protocol prefix for JDBC/ODBC bridge connections */
    public static final String SUBPROTOCOL_JDBC_ODBC = "odbc:";
    
    // ------------------------------------------------------------------------
    
    /**
     * Get an instance of the default SQL data handler.
     * @return an instance of the default SQL data handler
     */
    public static SQLDataHandler getDefaultHandler() {
        return new DefaultSQLDataHandler();
    }
    
    // ------------------------------------------------------------------------
    // Generic Connection Methods
    
    /**
     * Get a new database connection.
     * @param conn the Connection object to the database
     * @param handler the data handler to use
     * @return a DatabaseDataSource for interacting with the database
     * @throws SQLException if an SQL error occurs
     */
    public static DatabaseDataSource getDatabaseConnection(
            Connection conn, SQLDataHandler handler)
        throws SQLException
    {
        return new DatabaseDataSource(conn, handler);
    }    
    
    /**
     * Get a new database connection, using a default handler.
     * @param conn the Connection object to the database
     * @return a DatabaseDataSource for interacting with the database
     * @throws SQLException if an SQL error occurs
     */
    public static DatabaseDataSource getDatabaseConnection(Connection conn)
        throws SQLException
    {
        return getDatabaseConnection(conn, getDefaultHandler());
    }

    /**
     * Get a new database connection.
     * @param driver the database driver to use, must resolve to a valid Java
     * class on the current classpath.
     * @param url the url for the database, of the form
     * "jdbc:<database_sub_protocol>://&lt;hostname&gt;/&lt;database_name&gt;
     * @param user the database username
     * @param password the database password
     * @param handler the sql data handler to use
     * @return a DatabaseDataSource for interacting with the database
     * @throws SQLException
     * @throws ClassNotFoundException
     */
    public static DatabaseDataSource getDatabaseConnection(String driver,
            String url, String user, String password, SQLDataHandler handler)
        throws SQLException, ClassNotFoundException
    {
        Class.forName(driver);
        Connection conn = DriverManager.getConnection(url, user, password);
        return getDatabaseConnection(conn, handler);
    }    
    
    /**
     * Get a new database connection, using a default handler.
     * @param driver the database driver to use, must resolve to a valid Java
     * class on the current classpath.
     * @param url the url for the database, of the form
     * "jdbc:<database_sub_protocol>://&lt;hostname&gt;/&lt;database_name&gt;
     * @param user the database username
     * @param password the database password
     * @return a DatabaseDataSource for interacting with the database
     * @throws SQLException
     * @throws ClassNotFoundException
     */
    public static DatabaseDataSource getDatabaseConnection(String driver,
            String url, String user, String password)
        throws SQLException, ClassNotFoundException
    {
        return getDatabaseConnection(driver, url, user, password, 
                getDefaultHandler());
    }
    
    // ------------------------------------------------------------------------
    // Driver Specific Methods
    
    // -- MySQL ---------------------------------------------------------------
    
    /**
     * Get a new database connection to a MySQL database.
     * @param host the ip address or host name of the database server
     * @param database the name of the particular database to use
     * @param user the database username
     * @param password the database password
     * @param handler the sql data handler to use
     * @return a DatabaseDataSource for interacting with the database
     * @throws SQLException
     * @throws ClassNotFoundException
     */
    public static DatabaseDataSource getMySQLConnection(
            String host, String database, String user, String password,
            SQLDataHandler handler)
        throws SQLException, ClassNotFoundException
    {
        String url = PROTOCOL_JDBC + SUBPROTOCOL_MYSQL 
                   + "//" + host + "/" + database;
        return getDatabaseConnection(DRIVER_MYSQL,url,user,password,handler);
    }

    /**
     * Get a new database connection to a MySQL database, using a default
     * handler.
     * @param host the ip address or host name of the database server
     * @param database the name of the particular database to use
     * @param user the database username
     * @param password the database password
     * @return a DatabaseDataSource for interacting with the database
     * @throws SQLException
     * @throws ClassNotFoundException
     */
    public static DatabaseDataSource getMySQLConnection(
            String host, String database, String user, String password)
        throws SQLException, ClassNotFoundException
    {
        return getMySQLConnection(host, database, user, password, 
                getDefaultHandler());
    }
    
} // end of class ConnectionFactory
