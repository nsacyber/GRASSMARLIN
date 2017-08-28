package grassmarlin.session.serialization;

import grassmarlin.Logger;
import grassmarlin.RuntimeConfiguration;
import grassmarlin.plugins.IPlugin;
import grassmarlin.session.Session;
import grassmarlin.ui.common.tasks.SavingTask;

import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class Writer {
    public static boolean writeSession(final Path pathOut, final Session session, final RuntimeConfiguration config, final SavingTask task) {
        final Path pathTemporary = pathOut.resolveSibling("~" + pathOut.getFileName().toString() + ".zip");
        try(final ZipOutputStream zip = new ZipOutputStream(new BufferedOutputStream(Files.newOutputStream(pathTemporary)))) {
            //TODO: Write-lock the document, hide the temp file
            //Write the manifest
            // We need to know what plugins were loaded and what plugins contain data; if there is a mismatch on load, then ???
            try {
                task.subtaskDescriptionProperty().set("Writing Manifest...");
                zip.putNextEntry(new ZipEntry("manifest.xml"));
                final Manifest manifest = new Manifest(config);
                manifest.write(zip);
            } finally {
                zip.closeEntry();
            }

            try {
                task.subtaskDescriptionProperty().set("Writing Session...");
                zip.putNextEntry(new ZipEntry("session.xml"));

                final XMLStreamWriter writerSession = XMLOutputFactory.newFactory().createXMLStreamWriter(zip);
                writerSession.writeStartDocument();
                session.writeToXml(writerSession);
                writerSession.writeEndDocument();
                writerSession.flush();
            } finally {
                zip.closeEntry();
            }

            task.progressProperty().set(0.0);
            final List<IPlugin.SessionSerialization> plugins = config.enumeratePlugins(IPlugin.SessionSerialization.class);
            for(IPlugin.SessionSerialization plugin : plugins) {
                try {
                    task.subtaskDescriptionProperty().set(String.format("Saving data for Plugins (%s)...", plugin.getName()));
                    final Map<String, ByteArrayOutputStream> streams = new HashMap<>();

                    zip.putNextEntry(new ZipEntry(plugin.getName() + ".xml"));
                    plugin.sessionSaving(session, zip, (nameStream, buffered) -> {
                        if (buffered) {
                            //Return a buffered stream that can be used
                            final ByteArrayOutputStream streamResult = new ByteArrayOutputStream(4096);
                            streams.put(nameStream, streamResult);
                            return streamResult;
                        } else {
                            zip.closeEntry();
                            zip.putNextEntry(new ZipEntry(plugin.getName() + "." + nameStream + ".xml"));
                            return zip;
                        }
                    });

                    for (Map.Entry<String, ByteArrayOutputStream> entry : streams.entrySet()) {
                        zip.closeEntry();
                        zip.putNextEntry(new ZipEntry(plugin.getName() + "." + entry.getKey() + ".xml"));
                        zip.write(entry.getValue().toByteArray());
                    }
                } catch(Exception ex) {
                    ex.printStackTrace();
                } finally {
                    zip.closeEntry();
                    task.progressProperty().set(task.progressProperty().get() + 1.0 / (double)plugins.size());
                }
            }
        } catch(IOException | XMLStreamException ex) {
            ex.printStackTrace();
            //TODO: May not exist?
            try {
                if(Files.exists(pathTemporary)) {
                    Files.delete(pathTemporary);
                }
            } catch(IOException ex2) {
                //We tried.
            }
            return false;
        }

        //TODO: Better handling here; failure means we might lose both files.
        try {
            task.subtaskDescriptionProperty().set("Finalizing file...");
            if(Files.exists(pathOut)) {
                Files.delete(pathOut);
            }
            Files.move(pathTemporary, pathOut);
            return true;
        } catch(IOException ex) {
            return false;
        }
    }
    public static void writeObjectAttributes(final String prefixAttributes, final XMLStreamWriter writer, final Object o) throws XMLStreamException {
        writer.writeAttribute(prefixAttributes + "class", o.getClass().getName());
        //The plugin will be null for core classes, like String.  These classes do
        final String plugin = RuntimeConfiguration.pluginNameFor(o.getClass());
        if(plugin != null) {
            writer.writeAttribute(prefixAttributes + "plugin", plugin);
        }
    }
    public static void writeObject(final OutputStream out, final Object o) throws XMLStreamException {
        final XMLStreamWriter writer = XMLOutputFactory.newFactory().createXMLStreamWriter(out);
        try {
            writeObject(writer, o);
        } finally {
            writer.flush();
        }
    }
    public static void writeSerializableObject(final XMLStreamWriter writer, final Serializable o) throws XMLStreamException, IOException {
        writeObjectAttributes("", writer, o);
        writer.writeAttribute("XmlSerializable", "false");

        ByteArrayOutputStream bytesOut = new ByteArrayOutputStream();
        ObjectOutputStream wrappedOut = new ObjectOutputStream(bytesOut);

        wrappedOut.writeObject(o);
        wrappedOut.close();
        bytesOut.close();

        String serializedString = javax.xml.bind.DatatypeConverter.printHexBinary(bytesOut.toByteArray());

        writer.writeCharacters(serializedString);
    }
    public static void writeObject(final XMLStreamWriter writer, final Object o) throws XMLStreamException {
        writeObjectAttributes("", writer, o);

        if(o instanceof XmlSerializable) {
            ((XmlSerializable)o).writeToXml(writer);
        } else {
            if(RuntimeConfiguration.pluginNameFor(o.getClass()) == null && o instanceof Serializable && !(o instanceof String)) {
                String configAsHex = "";
                try {
                    final ByteArrayOutputStream bytesOut = new ByteArrayOutputStream();
                    ObjectOutputStream objOut = new ObjectOutputStream(bytesOut);
                    objOut.writeObject(o);
                    objOut.close();
                    bytesOut.close();
                    configAsHex = javax.xml.bind.DatatypeConverter.printHexBinary(bytesOut.toByteArray());
                } catch(IOException ex) {
                    Logger.log(Logger.Severity.ERROR, "There was an error serializing a core class (%s): %s", o.getClass().getName(), ex.getMessage());
                }
                writer.writeCharacters(configAsHex);
            } else {
                try {
                    //This will throw a NoSuchMethodException if the constructor doesn't exist.  We don't need the resulting constructor, just the failure path.
                    //Since the save file is regarded as untrusted, there is less interest when loading in identifying API violations--at that point failure is just failure and we aren't going to try to understand what went wrong.
                    //Note: This is only valid when the object is not XmlSerializable, and everything should, as often as possible, be XmlSerializable.
                    o.getClass().getConstructor(String.class);
                } catch (NoSuchMethodException ex) {
                    //Note to anyone (especially myself) that is debugging a stack trace traced to here:  You probably set
                    // a property to a type that doesn't have a (String) constructor and isn't an XmlSerializable object.
                    // You can only save and load objects if one of those two conditions is met.
                    throw new IllegalArgumentException(String.format("The given class (%s) neither implements XmlSerializable nor has a constructor(String) method.", o.getClass().getName()));
                }
                writer.writeCharacters(o.toString());
            }
        }
    }
}
