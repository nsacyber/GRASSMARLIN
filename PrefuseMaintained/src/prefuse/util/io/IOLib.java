package prefuse.util.io;

import java.awt.Component;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;

import javax.swing.JFileChooser;

import prefuse.data.Graph;
import prefuse.data.Table;
import prefuse.data.io.CSVTableReader;
import prefuse.data.io.DelimitedTextTableReader;
import prefuse.data.io.GraphMLReader;
import prefuse.data.io.GraphReader;
import prefuse.data.io.TableReader;
import prefuse.data.io.TreeMLReader;
import prefuse.util.StringLib;
import prefuse.util.collections.ByteArrayList;

/**
 * Library routines for input/output tasks.
 *  
 * @author <a href="http://jheer.org">jeffrey heer</a>
 */
public class IOLib {

    private IOLib() {
        // disallow instantiation
    }
    
    /**
     * Indicates if a given String is a URL string. Checks to see if the string
     * begins with the "http:/", "ftp:/", or "file:/" protocol strings.
     * @param s the string to check
     * @return true if a url string matching the listed protocols,
     * false otherwise
     */
    public static boolean isUrlString(String s) {
        return s.startsWith("http:/") ||
               s.startsWith("ftp:/")  ||
               s.startsWith("file:/");
    }
    
    /**
     * From a string description, attempt to generate a URL object. The string
     * may point to an Internet location (e.g., http:// or ftp:// URL),
     * a resource on the class path (resulting in a resource URL that points
     * into the current classpath), or a file on the local filesystem
     * (resulting in a file:// URL). The String will be checked in that order
     * in an attempt to resolve it to a valid URL.
     * @param location the location string for which to get a URL object
     * @return a URL object, or null if the location string could not be
     * resolved
     */
    public static URL urlFromString(String location) {
        return urlFromString(location, null, true);
    }
    
    /**
     * From a string description, attempt to generate a URL object. The string
     * may point to an Internet location (e.g., http:// or ftp:// URL),
     * a resource on the class path (resulting in a resource URL that points
     * into the current classpath), or, if the <code>includeFileSystem</code>
     * flag is true, a file on the local filesystem
     * (resulting in a file:// URL). The String will be checked in that order
     * in an attempt to resolve it to a valid URL.
     * @param location the location string for which to get a URL object
     * @param referrer the class to check for classpath resource items, the
     * location string will be resolved against the package/folder containing
     * this class 
     * @param includeFileSystem indicates if the file system should be
     * included in the search to resolve the location String
     * @return a URL object, or null if the location string could not be
     * resolved
     */
    public static URL urlFromString(String location, Class referrer,
                                    boolean includeFileSystem)
    {
        URL url = null;
        if ( isUrlString(location) ) {
            // explicit URL string
            try {
                url = new URL(location);
            } catch ( Exception e ) {
                e.printStackTrace();
            }
        } else {
            // attempt to get a URL pointing into the classpath
            if ( referrer != null )
                url = referrer.getResource(location);
            else
                url = IOLib.class.getResource(location);

            if ( url == null && !location.startsWith("/") )
                url = IOLib.class.getResource("/"+location);
            
            if ( includeFileSystem && url == null ) {
                // if still not found, check the file system
            	File f = new File(location);
            	if ( f.exists() ) {
                    try {
                    	url = f.toURI().toURL();
                    } catch ( Exception e ) {}
                }
            }
        }
        return url;
    }
    
    /**
     * Get an input string corresponding to the given location string. The
     * string will first be resolved to a URL and an input stream will be
     * requested from the URL connection. If this fails, the location will
     * be resolved against the file system. Also, if a gzip file is found,
     * the input stream will also be wrapped by a GZipInputStream. If the
     * location string can not be resolved, a null value is returned
     * @param location the location string
     * @return an InputStream for the resolved location string
     * @throws IOException if an input/ouput error occurs
     */
    public static InputStream streamFromString(String location) 
        throws IOException
    {
        InputStream is = null;
        
        // try to get a working url from the string
        URL url = urlFromString(location, null, false);
        if ( url != null ) {
            is = url.openStream();
        } else {
            // if that failed, try the file system
            File f = new File(location);
            if ( f.exists() )
                is = new FileInputStream(f);
        }
        
        if ( is == null ) {
            return null; // couldn't find it
        } else if ( isGZipFile(location) ) {
            return new GZIPInputStream(is);
        } else {
            return is;
        }
    }
    
    /**
     * Returns the extension for a file or null if there is none
     * @param f the input file
     * @return the file extension, or null if none
     */
    public static String getExtension(File f) {
        return (f != null ? getExtension(f.getName()) : null);
    }
    
    /**
     * Indicates if the given file ends with a file extension of
     * ".gz" or ".Z", indicating a GZip file.
     * @param file a String of the filename or URL of the file
     * @return true if the extension is ".gz" or ".Z", false otherwise
     */
    public static boolean isGZipFile(String file) {
    	String ext = getExtension(file);
        return "gz".equals(ext) || "z".equals(ext);
    }
    
    /**
     * Indicates if the given file ends with a file extension of
     * ".zip", indicating a Zip file.
     * @param file a String of the filename or URL of the file
     * @return true if the extension is ".zip", false otherwise
     */
    public static boolean isZipFile(String file) {
        return "zip".equals(getExtension(file));
    }
    
    /**
     * Returns the extension for a file or null if there is none
     * @param filename the input filename
     * @return the file extension, or null if none
     */
    public static String getExtension(String filename) {
        int i = filename.lastIndexOf('.');
        if ( i>0 && i<filename.length()-1 ) {
            return filename.substring(i+1).toLowerCase();
        } else {
            return null;
        }
    }
    
    /**
     * Reads an input stream into a list of byte values.
     * @param is the input stream to read
     * @return a ByteArrayList containing the contents of the input stream
     * @throws IOException if an input/ouput error occurs
     */
    public static ByteArrayList readAsBytes(InputStream is) throws IOException {
        ByteArrayList buf = new ByteArrayList();
        byte[] b = new byte[8192];
        int nread = -1;
        while ( (nread=is.read(b)) >= 0 ) {
            buf.add(b, 0, nread);
        }
        return buf;
    }
    
    /**
     * Reads an input stream into a single String result.
     * @param is the input stream to read
     * @return a String containing the contents of the input stream
     * @throws IOException if an input/ouput error occurs
     */
    public static String readAsString(InputStream is) throws IOException {
        StringBuffer buf = new StringBuffer();
        byte[] b = new byte[8192];
        int nread = -1;
        while ( (nread=is.read(b)) >= 0 ) {
            String s = new String(b, 0, nread);
            buf.append(s);
        }
        return buf.toString();
    }
    
    /**
     * Reads data pulled from the given location string into a single String
     * result. The method attempts to retrieve an InputStream using the
     * {@link #streamFromString(String)} method, then read the input stream
     * into a String result.
     * @param location the location String
     * @return a String with the requested data
     * @throws IOException if an input/ouput error occurs
     * @see #streamFromString(String)
     */
    public static String readAsString(String location) throws IOException {
        return readAsString(streamFromString(location));
    }
    
    // ------------------------------------------------------------------------
    
    /**
     * Present a file chooser dialog for loading a Table data set.
     * @param c user interface component from which the request is being made
     * @return a newly loaded Table, or null if not found or action canceled
     */
    public static Table getTableFile(Component c) {
        JFileChooser jfc = new JFileChooser();
        jfc.setDialogType(JFileChooser.OPEN_DIALOG);
        jfc.setDialogTitle("Open Table File");
        jfc.setAcceptAllFileFilterUsed(false);
        
        SimpleFileFilter ff;
        
        // TODO: have this generate automatically
        // tie into PrefuseConfig??
        
        // CSV
        ff = new SimpleFileFilter("csv",
                "Comma Separated Values (CSV) File (*.csv)",
                new CSVTableReader());
        ff.addExtension("gz");
        jfc.setFileFilter(ff);

        // Pipe-Delimited
        ff = new SimpleFileFilter("txt",
                "Pipe-Delimited Text File (*.txt)",
                new DelimitedTextTableReader("|"));
        ff.addExtension("gz");
        jfc.setFileFilter(ff);

        // Tab-Delimited
        ff = new SimpleFileFilter("txt",
                "Tab-Delimited Text File (*.txt)",
                new DelimitedTextTableReader());
        ff.addExtension("gz");
        jfc.setFileFilter(ff);
        
        int retval = jfc.showOpenDialog(c);
        if (retval != JFileChooser.APPROVE_OPTION)
            return null;
        
        File f = jfc.getSelectedFile();
        ff = (SimpleFileFilter)jfc.getFileFilter();
        TableReader tr = (TableReader)ff.getUserData();
        
        try {
            return tr.readTable(streamFromString(f.getAbsolutePath()));
        } catch ( Exception e ) {
            Logger.getLogger(IOLib.class.getName()).warning(
                e.getMessage() + "\n" + StringLib.getStackTrace(e));
            return null;
        }
    }
    
    /**
     * Present a file chooser dialog for loading a Graph or Tree data set.
     * @param c user interface component from which the request is being made
     * @return a newly loaded Graph, or null if not found or action canceled
     */
    public static Graph getGraphFile(Component c) {
        JFileChooser jfc = new JFileChooser();
        jfc.setDialogType(JFileChooser.OPEN_DIALOG);
        jfc.setDialogTitle("Open Graph or Tree File");
        jfc.setAcceptAllFileFilterUsed(false);
        
        SimpleFileFilter ff;
        
        // TODO: have this generate automatically
        // tie into PrefuseConfig??

        // TreeML
        ff = new SimpleFileFilter("xml",
                "TreeML File (*.xml, *.treeml)",
                new TreeMLReader());
        ff.addExtension("treeml");
        ff.addExtension("gz");
        jfc.setFileFilter(ff);
        
        // GraphML
        ff = new SimpleFileFilter("xml",
                "GraphML File (*.xml, *.graphml)",
                new GraphMLReader());
        ff.addExtension("graphml");
        ff.addExtension("gz");
        jfc.setFileFilter(ff);
        
        int retval = jfc.showOpenDialog(c);
        if (retval != JFileChooser.APPROVE_OPTION)
            return null;
        
        File f = jfc.getSelectedFile();
        ff = (SimpleFileFilter)jfc.getFileFilter();
        GraphReader gr = (GraphReader)ff.getUserData();
        
        try {
            return gr.readGraph(streamFromString(f.getAbsolutePath()));
        } catch ( Exception e ) {
            Logger.getLogger(IOLib.class.getName()).warning(
                e.getMessage() + "\n" + StringLib.getStackTrace(e));
            return null;
        }
    }
    
} // end of class IOLib
