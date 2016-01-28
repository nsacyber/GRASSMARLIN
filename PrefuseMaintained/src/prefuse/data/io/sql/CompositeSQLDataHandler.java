package prefuse.data.io.sql;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.HashMap;

import prefuse.data.Table;

/**
 * SQLDataHandler that allows multiple handlers to be grouped together. This
 * class supports a map that allows specific data field / column names to
 * use a custom SQLDataHandler implementation, while maintaining a default
 * handler for any data fields without an entry in the map.
 * 
 * @author <a href="http://jheer.org">jeffrey heer</a>
 */
public class CompositeSQLDataHandler implements SQLDataHandler {

    private SQLDataHandler m_default;
    private HashMap m_overrides;
    
    // ------------------------------------------------------------------------

    /**
     * Create a new CompositeSQLDataHandler. Uses a
     * {@link DefaultSQLDataHandler} as the default handler.
     */
    public CompositeSQLDataHandler() {
        this(new DefaultSQLDataHandler());
    }
    
    /**
     * Create a new CompositeSQLDataHandler.
     * @param defaultHandler the default data handler to use
     */
    public CompositeSQLDataHandler(SQLDataHandler defaultHandler) {
        m_default = defaultHandler;
    }
    
    // ------------------------------------------------------------------------
    
    /**
     * Add a custom data handler for a given column name.
     * @param columnName the data field / column name
     * @param handler the data handler to use for the field
     */
    public void addHandler(String columnName, SQLDataHandler handler) {
        if ( m_overrides == null )
            m_overrides = new HashMap(3);
        m_overrides.put(columnName, handler);
    }
    
    /**
     * Remove a custom data handler for a given column name. Subsequent
     * to this method, the column will use the default handler.
     * @param columnName the data field / column name
     * @return true if a handler was successfully removed, false if
     * no such custom handler was found.
     */
    public boolean removeHandler(String columnName) {
        if ( m_overrides == null )
            return false;
        else
            return m_overrides.remove(columnName) != null;
    }
    
    // ------------------------------------------------------------------------

    /**
     * @see prefuse.data.io.sql.SQLDataHandler#process(prefuse.data.Table, int, java.sql.ResultSet, int)
     */
    public void process(Table t, int trow, ResultSet rset, int rcol)
            throws SQLException
    {
        SQLDataHandler handler = m_default;
        if ( m_overrides != null && m_overrides.size() > 0 ) {
            ResultSetMetaData metadata = rset.getMetaData();
            String name = metadata.getColumnName(rcol);
            SQLDataHandler h = 
                (SQLDataHandler)m_overrides.get(name);
            if ( h != null )
                handler = h;
        }

        handler.process(t, trow, rset, rcol);
    }

    /**
     * @see prefuse.data.io.sql.SQLDataHandler#getDataType(java.lang.String, int)
     */
    public Class getDataType(String columnName, int sqlType) {
        SQLDataHandler handler = m_default;
        if ( m_overrides != null && m_overrides.size() > 0 ) {
            SQLDataHandler h = (SQLDataHandler)m_overrides.get(columnName);
            if ( h != null ) handler = h;
        }

        return handler.getDataType(columnName, sqlType);
    }

} // end of class CompositeSQLDataValueHandler
