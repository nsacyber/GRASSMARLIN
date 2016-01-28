package core.importmodule.parser;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.function.BiFunction;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A simple line reader consisting of an internal java.io.BufferedReader.
 * Instead of buffering the stream where "marked" to go back 'n' lines the
 * LineReaderCtx will close then reopen the file and read forward 'n' lines.
 * This is because command parsing may be very "buffer heavy" and will likely
 * not yield an efficient recovery strategy.
 * A parser error will likely have to re-ascend most of the document within these formats.
 * This is generally for reading non-structured syntactical formats.
 */
public class LineReaderCtx {

    private final File file;
    private BufferedReader br;
    private FileReader fr;
    private boolean error;
    private int lineno;
    
    /**
     *
     * @param f - File to create a line-reader context for
     * @throws FileNotFoundException Thrown when the file cannot be found.
     */
    public LineReaderCtx( File f ) throws FileNotFoundException, IOException {
        file = f;
        lineno = -1;
        error = false;
        open();
    }
    
    private void open() throws FileNotFoundException {
        fr = new FileReader( file );
		br = new BufferedReader( fr );
    }
    
    public String getName() {
        return file.getName();
    }
    
    /**
     *
     * @return the line number this reader is on.
     */
    public int lineNo() {
        return lineno;
    }
    
    /**
     * Its implementation specific - but in general - returning true for ARG1 indicates an error
     * and should return to the previous rule by returning true as well.
     * 
     * This follows the typical callback axiom of cb(err,data). So that errors may propagate upward.
     * 
     * @param getline a BiConsumer of (error-flag,line)
     * @return the return of the callback
     */
    public Boolean next(BiFunction<Boolean,String,Boolean> getline) {
        String s = null;
        try {
            s = br.readLine();
            lineno++;
        } catch (IOException ex) {
            Logger.getLogger(LineReaderCtx.class.getName()).log(Level.SEVERE, null, ex);
        }
        return getline.apply(s == null || error, s);
    }
    
    public void back(int lines) {
        int l = lineNo()-lines;
        moveTo( l );
    }

    /**
     *
     * @param lineNo the line number to return to.
     * Useful in recovery situations.
     */
    public void moveTo( int lineNo ) {
        try {
            reset();
            while( (lineNo--) > 0 && br.readLine() != null ){
                lineno++;
            }
        } catch (IOException ex) {
            error = true;
            Logger.getLogger(LineReaderCtx.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    /**
     * WILL attempt to reset the internal reader.
     */
    public void reset() {
        try {
            close();
            open();
            lineno = -1;
        } catch (FileNotFoundException ex) {
            error = true;
            Logger.getLogger(LineReaderCtx.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    /**
     * WILL close the enclosed readers.
     */
    public void close() {
        try {
            br.close();
            fr.close();
        } catch (IOException ex) {
            Logger.getLogger(LineReaderCtx.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
}
