package grassmarlin.plugins.internal.physical.deviceimport;

import grassmarlin.Logger;
import grassmarlin.RuntimeConfiguration;
import grassmarlin.common.Confidence;
import grassmarlin.plugins.IPlugin;
import grassmarlin.session.HardwareAddress;
import grassmarlin.session.ImportItem;
import grassmarlin.session.LogicalAddress;
import javafx.beans.property.DoubleProperty;
import javafx.geometry.Point2D;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Serializable;
import java.nio.file.Files;
import java.util.Iterator;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

public class Importer {
    private final RuntimeConfiguration config;
    private boolean isParsingComplete;
    private BlockingQueue<Object> queue;
    private final ImportItem source;

    private final boolean haltOnError;

    public Importer(final RuntimeConfiguration config, final ImportItem source) {
        this.config = config;
        this.isParsingComplete = false;
        this.queue = new ArrayBlockingQueue<>(100);
        this.source = source;

        this.haltOnError = false; //TODO: Read from plugin menu

        this.parseFile();
    }

    protected void parseFile() {
        this.isParsingComplete = false;

        final Thread threadParse = new Thread(() -> {
            if(!threadParseFile()) {
                Logger.log(Logger.Severity.ERROR, "The file could not be parsed. (%s)", Importer.this.source.getPath().toAbsolutePath().toString());
            }
        });
        threadParse.setDaemon(true);
        threadParse.setName("grassmarlin.plugins.internal.physical.deviceimport.Importer: " + this.source.getPath().toAbsolutePath().toString());
        threadParse.start();
    }

    private boolean threadParseFile() {
        this.isParsingComplete = false;

        try {
            final BufferedReader reader = Files.newBufferedReader(this.source.getPath());
            String line = reader.readLine();
            while(line != null) {
                if(line.startsWith("#")) {
                    line = reader.readLine();
                    continue;
                }
                switch(line.toUpperCase()) {
                    case "SWITCH":
                        if(!parseSwitch(reader) && haltOnError) {
                            return false;
                        }
                        break;
                    case "ROUTER":
                        if(!parseRouter(reader) && haltOnError) {
                            return false;
                        }
                        break;
                    case "HOST":
                        if(!parseHost(reader) && haltOnError) {
                            return false;
                        }
                        break;
                    default:
                        Logger.log(Logger.Severity.ERROR, "Parse error: unexpected token '%s'", line);
                        return false;
                }
                line = reader.readLine();
            }
        } catch(IOException ex) {
            Logger.log(Logger.Severity.WARNING, "There was an error parsing the file: %s", ex.getMessage());
            return false;
        } finally {
            this.isParsingComplete = true;
            this.source.importCompleteProperty().set(true);
            try {
                this.source.recordProgress(Files.size(source.getPath()));
            } catch(IOException ex) {
                if(this.source.progressProperty() instanceof DoubleProperty) {
                    ((DoubleProperty)this.source.progressProperty()).set(1.0);
                }
            }
        }
        return true;
    }

    private boolean parsePort(final BufferedReader reader, final Switch.Port port) throws IOException {
        String line = reader.readLine();
        while(line != null) {
            switch(line.toUpperCase()) {
                case "END":
                case "END PORT":
                    line = null;
                    continue;
                case "PROPERTY":
                    //TODO: When "Property" is seen, it is a property on the port object.  Any property that goes on the connection has a special name, type checking, is validated to a single value, etc.  Other importers might break these rules, but they will be enforced here.
                    final String[] tokensProperty = reader.readLine().split(":", 2);
                    final Serializable valueProperty = readClass(reader, Serializable.class);
                    if (valueProperty == null) {
                        Logger.log(Logger.Severity.ERROR, "Unable to parse value for %s.", tokensProperty[0]);
                    }

                    if (tokensProperty.length == 2) {
                        //Technically it is wrong for there to be leading whitespace, but it happens often enough that we just remove it.  There is no known format where leading (or trailing) whitespace is relevant.
                        port.addProperty(tokensProperty[0], valueProperty, Confidence.fromString(tokensProperty[1].trim()));
                    } else {
                        port.addProperty(tokensProperty[0], valueProperty);
                    }
                    break;
                case "TRUNK":
                    port.setTrunk(true);
                    break;
                case "ADDRESS": {
                        final HardwareAddress address = readClass(reader, HardwareAddress.class);
                        if (address != null) {
                            port.setAddress(address);
                        }
                    }
                    break;
                case "CONNECTION": {
                        final HardwareAddress address = readClass(reader, HardwareAddress.class);
                        if (address != null) {
                            port.addConnection(address);
                        }
                    }
                    break;
                case "ANGLE":
                    final double degrees = Double.parseDouble(reader.readLine());
                    port.setControlAngle(degrees);
                    break;
                case "POSITION":
                    final double x = Double.parseDouble(reader.readLine());
                    final double y = Double.parseDouble(reader.readLine());
                    port.setPosition(x, y);
                    break;
            }
            line = reader.readLine();
        }

        return port.isValid();
    }

    private boolean parseSwitch(final BufferedReader reader) throws IOException {
        final Switch device = new Switch();
        Point2D ptDefaultPortLocation = new Point2D(0.0, 0.0);
        String line = reader.readLine();
        while(line != null) {
            switch(line.toUpperCase()) {
                case "END":
                case "END SWITCH":
                    line = null;
                    continue;
                case "PROPERTY":
                    final String[] tokensProperty = reader.readLine().split(":", 2);
                    final Serializable valueProperty = readClass(reader, Serializable.class);
                    if(valueProperty == null) {
                        Logger.log(Logger.Severity.ERROR, "Unable to parse value for %s.", tokensProperty[0]);
                    }

                    if(tokensProperty.length == 2) {
                        //Technically it is wrong for there to be leading whitespace, but it happens often enough that we just remove it.  There is no known format where leading (or trailing) whitespace is relevant.
                        device.addProperty(tokensProperty[0], valueProperty, Confidence.fromString(tokensProperty[1].trim()));
                    } else {
                        device.addProperty(tokensProperty[0], valueProperty);
                    }
                    break;
                case "NAME":
                    device.setName(reader.readLine());
                    break;
                case "PORT":
                    //Next line is the name
                    final Switch.Port port = device.new Port(reader.readLine());
                    port.setPosition(ptDefaultPortLocation.getX(), ptDefaultPortLocation.getY());
                    if(parsePort(reader, port)) {
                        device.addPort(port);
                        if(port.getPosition().getX() + 1.0 > ptDefaultPortLocation.getX()) {
                            ptDefaultPortLocation = new Point2D(port.getPosition().getX() + 1.0, port.getPosition().getY());
                        }
                    }
                    break;
            }
            line = reader.readLine();
        }

        if(device.isValid()) {
            this.queue.addAll(device.getEntities());
            return true;
        } else {
            Logger.log(Logger.Severity.WARNING, "Incomplete Device definition");
            return false;
        }
    }
    private boolean parseRouter(final BufferedReader reader) {
        return false;
    }
    private boolean parseHost(final BufferedReader reader) throws IOException {
        final Host host = new Host();
        String line = reader.readLine();
        while(line != null) {
            switch(line.toUpperCase()) {
                case "END":
                case "END HOST":
                    line = null;
                    continue;
                case "PROPERTY":
                    final String[] tokensProperty = reader.readLine().split(":", 2);
                    final Serializable valueProperty = readClass(reader, Serializable.class);
                    if(valueProperty == null) {
                        Logger.log(Logger.Severity.ERROR, "Unable to parse value for %s.", tokensProperty[0]);
                    }

                    if(tokensProperty.length == 2) {
                        //Technically it is wrong for there to be leading whitespace, but it happens often enough that we just remove it.  There is no known format where leading (or trailing) whitespace is relevant.
                        host.addProperty(tokensProperty[0], valueProperty, Confidence.fromString(tokensProperty[1].trim()));
                    } else {
                        host.addProperty(tokensProperty[0], valueProperty);
                    }
                    break;
                case "HARDWARE ADDRESS": {
                        final HardwareAddress address = readClass(reader, HardwareAddress.class);
                        if (address != null) {
                            host.setHardwareAddress(address);
                        }
                    }
                    break;
                case "LOGICAL ADDRESS": {
                        final LogicalAddress address = readClass(reader, LogicalAddress.class);
                        if (address != null) {
                            host.addLogicalAddress(address);
                        }
                    }
                    break;
            }
            line = reader.readLine();
        }

        if(host.isValid()) {
            this.queue.addAll(host.getEntities());
            return true;
        } else {
            Logger.log(Logger.Severity.WARNING, "Incomplete Host definition");
            return false;
        }
    }

    private <T> T readClass(final BufferedReader reader, final Class<? super T> classOutput) throws IOException {
        final String[] tokens = reader.readLine().split(":", 2);
        final String plugin = tokens[0];
        final String nameFactory = tokens[1];
        final String data = reader.readLine();

        if(plugin == null || nameFactory == null || data == null) {
            return null;
        }

        final IPlugin pluginRaw = this.config.pluginFor(plugin);
        if(pluginRaw == null || !(pluginRaw instanceof IPlugin.HasClassFactory)) {
            return null;
        }

        //We don't know for certain that the typecast can be applied, but we'll check later.
        final IPlugin.HasClassFactory.ClassFactory<T> factory = (IPlugin.HasClassFactory.ClassFactory<T>)((IPlugin.HasClassFactory)pluginRaw).getClassFactories().stream().filter(f -> f.getFactoryName().equals(nameFactory)).findAny().orElse(null);
        if(factory == null) {
            return null;
        }

        if(!classOutput.isAssignableFrom(factory.getFactoryClass())) {
            return null;
        }

        return factory.createInstance(data);
    }

    public Iterator<Object> getIterator() {
        return new ContentIterator();
    }

    private final class ContentIterator implements Iterator<Object> {
        @Override
        public boolean hasNext() {
            return !(Importer.this.isParsingComplete && queue.isEmpty());
        }

        @Override
        public Object next() {
            return queue.poll();
        }
    }
}
