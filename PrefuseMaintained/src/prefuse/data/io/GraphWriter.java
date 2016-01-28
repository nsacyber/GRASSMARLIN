/**
 * Copyright (c) 2004-2006 Regents of the University of California.
 * See "license-prefuse.txt" for licensing terms.
 */
package prefuse.data.io;

import java.io.File;
import java.io.OutputStream;

import prefuse.data.Graph;

/**
 * Interface for classes that write Graph data to a particular file format.
 * 
 * @author <a href="http://jheer.org">jeffrey heer</a>
 */
public interface GraphWriter {

    /**
     * Write a graph to the file with the given filename.
     * @param graph the Graph to write
     * @param filename the file to write the graph to
     * @throws DataWriteException
     */
    public void writeGraph(Graph graph, String filename) throws DataIOException;
    
    /**
     * Write a graph to the given File.
     * @param graph the Graph to write
     * @param f the file to write the graph to
     * @throws DataWriteException
     */
    public void writeGraph(Graph graph, File f) throws DataIOException;
    
    /**
     * Write a graph from the given OutputStream.
     * @param graph the Graph to write
     * @param os the OutputStream to write the graph to
     * @throws DataWriteException
     */
    public void writeGraph(Graph graph, OutputStream os) throws DataIOException;
    
} // end of interface GraphWriter
