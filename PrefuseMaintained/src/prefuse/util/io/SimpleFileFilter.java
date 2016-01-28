package prefuse.util.io;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;

import javax.swing.filechooser.FileFilter;

/**
 * A simple file filter for a particular file extension.
 *  
 * @author <a href="http://jheer.org">jeffrey heer</a>
 */
public class SimpleFileFilter extends FileFilter {
    
    private ArrayList exts = new ArrayList();
    private String desc;
    private Object data;
    
    /**
     * Create a new SimpleFileFilter.
     * @param ext the file extension
     * @param desc a description of the file type
     */
    public SimpleFileFilter(String ext, String desc) {
        addExtension(ext);
        this.desc = desc;
    }
    
    /**
     * Create a new SimpleFileFilter.
     * @param ext the file extension
     * @param desc a description of the file type
     * @param data user-provided attached object
     */
    public SimpleFileFilter(String ext, String desc, Object data) {
        addExtension(ext);
        this.desc = desc;
        this.data = data;
    }
    
    /**
     * Add a file extension to this file filter.
     * @param ext the file extension to add
     */
    public void addExtension(String ext) {
        exts.add(ext.toLowerCase());
    }
    
    /**
     * @see javax.swing.filechooser.FileFilter#accept(java.io.File)
     */
    public boolean accept(File f) {
        if ( f == null )
            return false;
        if ( f.isDirectory() )
            return true;
        String extension = IOLib.getExtension(f);
        if ( extension == null ) return false;

        for ( Iterator iter = exts.iterator(); iter.hasNext(); ) {
            String ext = (String)iter.next();
            if ( ext.equalsIgnoreCase(extension) )
                return true;
        }
        return false;
    }
    
    /**
     * Get a user-provided attached object.
     * @return the user-provided attached object
     */
    public Object getUserData() {
        return data;
    }
    
    /**
     * @see javax.swing.filechooser.FileFilter#getDescription()
     */
    public String getDescription() {
        return desc;
    }
    
    /**
     * Get the first file extension associated with this filter.
     * @return the first file extension associated with this filter
     */
    public String getExtension() {
        return (String)exts.get(0);
    }
    
} // end of class SimpleFileFilter
