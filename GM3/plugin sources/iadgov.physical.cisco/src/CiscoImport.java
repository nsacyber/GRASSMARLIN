package iadgov.physical.cisco;

import grassmarlin.RuntimeConfiguration;
import grassmarlin.session.ImportItem;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Iterator;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class CiscoImport {
    private final ImportItem source;
    private final BlockingQueue<Object> deviceQueue;
    private final RuntimeConfiguration config;
    private boolean done;

    public CiscoImport(final RuntimeConfiguration config, final ImportItem source) {
        this.source = source;
        this.deviceQueue = new ArrayBlockingQueue<>(1); //Each file should produce a single device (or none)
        this.config = config;
        this.done = false;

        parseSource();
    }

    protected void parseSource() {
        done = false;

        final Thread loop = new Thread(() -> {
            try(BufferedReader reader = Files.newBufferedReader(source.getPath())) {
                Object result = CiscoParser.parseFile(reader);
                if(result != null) {
                    deviceQueue.add(result);
                }
            } catch(IOException ex) {
                ex.printStackTrace();
            } finally {
                done = true;
            }
        }, "CiscoLoop/" + this.source.toString());
        loop.setDaemon(true);
        loop.start();
    }

    public Iterator<Object> getIterator() {
        return new Iterator<Object>() {
            @Override
            public boolean hasNext() {
                return !(CiscoImport.this.done && CiscoImport.this.deviceQueue.isEmpty());
            }

            @Override
            public Object next() {
                return CiscoImport.this.deviceQueue.poll();
            }

            @Override
            public String toString() {
                return CiscoImport.this.source.toString();
            }
        };
    }

    @Override
    public String toString() {
        return String.format("%s, (%s%f%%)", this.source, this.done ? "COMPLETE / " : "", 100.0 * this.source.progressProperty().doubleValue());
    }
}
