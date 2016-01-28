package prefuse.data.util;

import java.util.HashSet;

import prefuse.data.column.Column;

/**
 * ColumnProjection instance that includes or excludes columns based on
 * the column name.
 * 
 * @author <a href="http://jheer.org">jeffrey heer</a>
 */
public class NamedColumnProjection extends AbstractColumnProjection {

    private HashSet m_names;
    private boolean m_include;

    /**
     * Create a new NamedColumnProjection.
     * @param name the name to filter on
     * @param include true to include the given names (and exclude all others),
     * false to exclude them (and include all others)
     */
    public NamedColumnProjection(String name, boolean include) {
        m_names = new HashSet();
        m_names.add(name);
        m_include = include;
    }
    
    /**
     * Create a new NamedColumnProjection.
     * @param names the names to filter on
     * @param include true to include the given names (and exclude all others),
     * false to exclude them (and include all others)
     */
    public NamedColumnProjection(String[] names, boolean include) {
        m_names = new HashSet();
        for ( int i=0; i<names.length; ++i )
            m_names.add(names[i]);
        m_include = include;
    }
    
    /**
     * Add a column name to this projection.
     * @param name the column name to add
     */
    public void addName(String name) {
        m_names.add(name);
    }
    
    /**
     * Remove a column name from this projection
     * @param name the column name to remove
     * @return true if the name was succesffuly removed, false otherwise
     */
    public boolean removeName(String name) {
        return m_names.remove(name);
    }
    
    /**
     * @see prefuse.data.util.ColumnProjection#include(prefuse.data.column.Column, java.lang.String)
     */
    public boolean include(Column col, String name) {
        return !(m_include ^ m_names.contains(name));
    }

} // end of class NamedColumnProjection
