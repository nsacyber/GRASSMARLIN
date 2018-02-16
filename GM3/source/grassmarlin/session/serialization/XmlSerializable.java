package grassmarlin.session.serialization;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

/**
 * When available, this interface will be used to read/write this class when loading and saving.
 *
 * Any object which can be serialized (e.g. is reported as a property) must either implement
 * this class or have a constructor which takes a single String parameter, which is the inverse
 * of its .toString()
 *
 * When deserializing an XmlSerializable object, the preferred course is to call a constructor
 * that takes an XMLStreamReader as its only argument.  If such a constructor doesn't exist a
 * 0-argument constructor will be called and then readFromXml will be called.
 */
public interface XmlSerializable {
    default void readFromXml(final XMLStreamReader source) throws XMLStreamException {
        //It is expected that most implementations will use the constructor method of handling deserialization.
        throw new UnsupportedOperationException();
    }

    /**
     * @param target The XMLStreamWriter to which the object should be written.
     *               The Stream will be positioned inside an Element that
     *               contains enough metadata to identify the class.
     *               Attributes and Child Elements can be written, but this
     *               initial Element should not be closed.
     */
    void writeToXml(final XMLStreamWriter target) throws XMLStreamException;
}
