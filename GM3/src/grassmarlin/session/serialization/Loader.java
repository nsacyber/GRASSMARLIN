package grassmarlin.session.serialization;

import grassmarlin.Logger;
import grassmarlin.RuntimeConfiguration;
import grassmarlin.plugins.serialization.CustomObjectInputStream;
import grassmarlin.session.Session;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class Loader {
    public static Session loadSession(final RuntimeConfiguration config, final Session session, final ZipFile zip) throws IOException, XMLStreamException {
        final ZipEntry entrySession = zip.getEntry("session.xml");

        final XMLStreamReader readerXml = XMLInputFactory.newInstance().createXMLStreamReader(zip.getInputStream(entrySession));
        session.readFromXml(readerXml);

        return null;
    }

    public static <T> T readNextObject(final XMLStreamReader reader, final Class<? super T> clazz) throws XMLStreamException {
        reader.nextTag();
        final Object result = readObject(reader);
        if(clazz.isAssignableFrom(result.getClass())) {
            //noinspection unchecked
            return (T)result;
        } else {
            throw new XMLStreamException("The given object type could not be read.");
        }
    }
    public static Object readObject(final InputStream in) throws XMLStreamException {
        XMLStreamReader reader = XMLInputFactory.newFactory().createXMLStreamReader(in);
        //Advance to the first tag found
        reader.nextTag();
        return readObject(reader);
    }

    public static Serializable readSerializableObject(final XMLStreamReader reader) throws XMLStreamException, IOException, ClassNotFoundException {
        final String namePlugin = reader.getAttributeValue(null, "plugin");

        final ClassLoader loader = RuntimeConfiguration.getLoader(namePlugin);
        if (loader != null) {
            byte[] bytes = javax.xml.bind.DatatypeConverter.parseHexBinary(reader.getElementText());
            ByteArrayInputStream bytesIn = new ByteArrayInputStream(bytes);
            CustomObjectInputStream objIn = new CustomObjectInputStream(bytesIn, loader);

            return (Serializable)objIn.readObject();
        } else {
            throw new ClassNotFoundException("ClassLoader for Plugin " + namePlugin + " is null");
        }
    }

    public static Object readObject(final XMLStreamReader reader) throws XMLStreamException {
        final String nameClass = reader.getAttributeValue(null, "class");
        final String namePlugin = reader.getAttributeValue(null, "plugin");

        if(namePlugin == null) {
            // This is a core class and we need special handling.
            switch(nameClass) {
                case "java.lang.String":
                    //Strings are special cased to support text search through files.
                    return reader.getElementText();
                default:
                    try {
                        if(Serializable.class.isAssignableFrom(Class.forName(nameClass))) {
                            final String textObject = reader.getElementText();
                            final byte[] bytesObject = javax.xml.bind.DatatypeConverter.parseHexBinary(textObject);
                            ByteArrayInputStream streamObject = new ByteArrayInputStream(bytesObject);

                            ObjectInputStream objIn = new ObjectInputStream(streamObject);
                            return objIn.readObject();
                        } else {
                            Logger.log(Logger.Severity.ERROR, "Unable to load core class (not Serializable) %s", nameClass);
                            return null;
                        }
                    } catch (IOException | ClassNotFoundException e) {
                        Logger.log(Logger.Severity.ERROR, "Unable to load core class %s (%s)", nameClass, e.getMessage());
                        return null;
                    }

            }
        } else {
            try {
                final Class<?> classObject = RuntimeConfiguration.getLoader(namePlugin).loadClass(nameClass);
                if(XmlSerializable.class.isAssignableFrom(classObject)) {
                    try {
                        // First choice: constructor that takes an XMLStreamReader
                        return classObject.getConstructor(XMLStreamReader.class).newInstance(reader);
                    } catch(NoSuchMethodException exNoReaderConstructor) {
                        //Ignore it; we'll have returned from the try or failed to here.
                    } catch(InstantiationException | IllegalAccessException | InvocationTargetException ex) {
                        throw new IllegalArgumentException(String.format("Unable to deserialize (%s) from plugin (%s) using XmlSerializable (constructor).", nameClass, namePlugin), ex);
                    }

                    try {
                        // Second choice: 0-argument constructor then call XmlSerializable.readFromXml(...)
                        final Object result = classObject.getConstructor().newInstance();
                        ((XmlSerializable)result).readFromXml(reader);
                        return result;
                    } catch(NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException ex) {
                        throw new IllegalArgumentException(String.format("Unable to deserialize (%s) from plugin (%s) using XmlSerializable (instance).", nameClass, namePlugin), ex);
                    }
                } else {
                    try {
                        //Call a constructor that takes a single string.  If this doesn't exist, fail.
                        return classObject.getConstructor(String.class).newInstance(reader.getElementText());
                    } catch(NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException ex) {
                        throw new IllegalArgumentException(String.format("Unable to deserialize (%s) from plugin (%s) using text fallback.", nameClass, namePlugin), ex);
                    }
                }
            } catch(ClassNotFoundException ex) {
                throw new IllegalArgumentException(String.format("The class (%s) does not exist in the plugin (%s)", nameClass, namePlugin));
            }
        }
    }
}
