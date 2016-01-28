package prefuse.data.io;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import prefuse.data.Graph;
import prefuse.util.io.IOLib;


/**
 * Abstract base class implementation of the GraphReader interface. Provides
 * implementations for all but the
 * {@link prefuse.data.io.GraphReader#readGraph(InputStream)} method. 
 * 
 * @author <a href="http://jheer.org">jeffrey heer</a>
 */
public abstract class AbstractGraphReader implements GraphReader {

    /**
     * @see prefuse.data.io.GraphReader#readGraph(java.lang.String)
     */
    public Graph readGraph(String location) throws DataIOException
    {
        try {
            InputStream is = IOLib.streamFromString(location);
            if ( is == null )
                throw new DataIOException("Couldn't find " + location
                    + ". Not a valid file, URL, or resource locator.");
            return readGraph(is);
        } catch ( IOException e ) {
            throw new DataIOException(e);
        }   
    }

    /**
     * @see prefuse.data.io.GraphReader#readGraph(java.net.URL)
     */
    public Graph readGraph(URL url) throws DataIOException {
        try {
            return readGraph(url.openStream());
        } catch ( IOException e ) {
            throw new DataIOException(e);
        }
    }

    /**
     * @see prefuse.data.io.GraphReader#readGraph(java.io.File)
     */
    public Graph readGraph(File f) throws DataIOException {
        try {
            return readGraph(new FileInputStream(f));
        } catch ( FileNotFoundException e ) {
            throw new DataIOException(e);
        }
    }
    
    /**
     * @see prefuse.data.io.GraphReader#readGraph(java.io.InputStream)
     */
    public abstract Graph readGraph(InputStream is) throws DataIOException;

} // end of class AbstractGraphReader
