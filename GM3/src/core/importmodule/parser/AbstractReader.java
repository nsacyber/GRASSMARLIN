/*
 *  Copyright (C) 2011, 2012
 *  This file is part of GRASSMARLIN.
 */

package core.importmodule.parser;

import core.Core;
import core.importmodule.ImportItem;
import core.types.LogEmitter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The intent of the AbstractReader is to handle two types of errors. Controlled
 * and not controlled Exceptions. 
 * 
 * For example - if we are looking a Document with a header and there is no
 * header, we can call the malformedInputHandler instead of the Component's
 * normal error handling for unknown exceptions. (IO errors and such)
 * 
 * This implementation is reports verbosely by default.
 * @param <T> The data type this Reader produces
 */
public abstract class AbstractReader<T extends ImportItem> {

    protected Consumer<Exception> malformedInputHandler;
    protected Boolean verbose;
    private int lineCount;
    LogEmitter logEmitter;
    
    public AbstractReader() {
        super();
        verbose = true;
        lineCount = 0;
        malformedInputHandler = this::handleError;
    }
    
    public int getLineCount() {
        return lineCount;
    }
    
    public LogEmitter getLogEmitter() {
        return logEmitter;
    }

    public void setLogEmitter(LogEmitter log) {
        this.logEmitter = log;
    }
    
    protected void handleError(Exception ex) {
        handleError( this.getClass(), null, ex);
    }
    
    protected void handleError(Class c, String s, Exception ex) {
        Logger.getLogger(c.getClass().getName()).log(Level.SEVERE, s, ex);
        logEmitter.emit(this, Core.ALERT.DANGER, s==null?ex.getMessage():s);
    }
    
    public boolean isVerbose() {
        return verbose;
    }
    
    public abstract boolean test(String path);
    public abstract boolean test(String path, Boolean verbose);
    
    /**
     *
     * @param verbose will log parser exceptions if true, else false and silent
     */
    public void setVerbose( Boolean verbose ) {
        this.verbose = verbose;
    }
    
    public void setMalformedInputCB(Consumer<Exception> cb) {
        malformedInputHandler = cb;
    }

    public static int countLines(ImportItem t) {
        int lines = 0;
        try ( InputStream is = new FileInputStream(t) ) {
            byte[] c = new byte[1024];
            int count = 0;
            int readChars = 0;
            boolean empty = true;
            while((readChars = is.read(c)) != -1 ) {
                empty = false;
                for( int i = 0; i < readChars; ++i ) {
                    if(c[i] == '\n') {
                        ++count;
                    }
                }
            }
            lines = count == 0 && !empty ? 1 : count;
        } catch (FileNotFoundException ex) {
            Logger.getLogger(AbstractReader.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(AbstractReader.class.getName()).log(Level.SEVERE, null, ex);
        }
        return lines;
    }
    
}
