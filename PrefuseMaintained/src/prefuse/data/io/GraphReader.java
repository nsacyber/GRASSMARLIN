package prefuse.data.io;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import prefuse.data.Graph;

/**
 * interface for classes that read in Graph or Tree data from a particular
 * file format.
 * 
 * @author <a href="http://jheer.org">jeffrey heer</a>
 */
public interface GraphReader {

    /**
     * Read in a graph from the file at the given location. Though
     * not required by this interface, the String is typically resolved
     * using the {@link prefuse.util.io.IOLib#streamFromString(String)} method,
     * allowing URLs, classpath references, and files on the file system
     * to be accessed.
     * @param location the location to read the graph from
     * @return the loaded Graph
     * @throws FileNotFoundException
     * @throws IOException
     */
    public Graph readGraph(String location) throws DataIOException;
    
    /**
     * Read in a graph from the given URL.
     * @param url the url to read the graph from
     * @return the loaded Graph
     * @throws IOException
     */
    public Graph readGraph(URL url) throws DataIOException;
    
    /**
     * Read in a graph from the given File.
     * @param f the file to read the graph from
     * @return the loaded Graph
     * @throws FileNotFoundException
     * @throws IOException
     */
    public Graph readGraph(File f) throws DataIOException;
    
    /**
     * Read in a graph from the given InputStream.
     * @param is the InputStream to read the graph from
     * @return the loaded Graph
     * @throws IOException
     */
    public Graph readGraph(InputStream is) throws DataIOException;

} // end of interface GraphReader
