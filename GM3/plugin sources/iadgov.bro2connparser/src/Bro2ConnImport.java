package iadgov.bro2connparser;

import grassmarlin.Logger;
import grassmarlin.RuntimeConfiguration;
import grassmarlin.session.ImportItem;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Iterator;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

/**
 *  Imports Bro2Conn Log files for parsing
 *  rewritten for v3.3 instead of ported from v3.2
 *
 *  This section was developed by an intern with caffeine...
 *  DevHash: 446562202d20434e4f4450204744502032303139
 */



public class Bro2ConnImport {
    public interface IBroParser {
        int parseFile(final BufferedReader file, BlockingQueue<Object> packetQueue) throws IOException;
    }

    private final ImportItem source;
    private final BlockingQueue<Object> packetQueue;
    private final RuntimeConfiguration config;
    private boolean done;

    public Bro2ConnImport(final RuntimeConfiguration config, final ImportItem source, IBroParser parser) {
        this.source = source;
        this.packetQueue = new ArrayBlockingQueue<>(100);
        this.config = config;
        this.done = false;

        parseSource(parser);
    }

    public void parseSource(final IBroParser parser) {
        done = false;

        final Thread loop = new Thread(() -> {
            try(BufferedReader reader = Files.newBufferedReader(source.getPath())) {
                final int result = parser.parseFile(reader, packetQueue);
                if(result < 1) {
                    Logger.log(Logger.Severity.ERROR, "Error! Unable to report bro2 conn.log packet! Int \"result\" returned less than 1.");
                }
            } catch(IOException ex) {
                Logger.log(Logger.Severity.ERROR, "Unable to process import of bro2 conn.log; failed on error: " + ex.toString());
            } finally {
                Logger.log(Logger.Severity.COMPLETION, "Completed import of Bro log file.");
                done = true;
            }
        }, "Bro2ConLog/" + this.source.toString());
        loop.setDaemon(true);
        loop.start();
    }

    public Iterator<Object> getIterator() {
        return new Iterator<Object>() {
            @Override
            public boolean hasNext() {
                return !(done && packetQueue.isEmpty());
            }

            @Override
            public Object next() {
                return packetQueue.poll();
            }

            @Override
            public String toString() { return Bro2ConnImport.this.source.toString(); }
        };
    }
}
