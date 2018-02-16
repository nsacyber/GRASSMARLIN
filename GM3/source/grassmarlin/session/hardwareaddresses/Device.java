package grassmarlin.session.hardwareaddresses;

import grassmarlin.session.HardwareAddress;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import java.io.UnsupportedEncodingException;

/**
 * The Device class gets special handling in the Physical Graph.
 * A Device HardwareAddress identifies the properties of and ports of a Device--anything connected to this address is a port on this switch.  The properties of the coresponding HardwareVertex are the properties of the switch.
 */
public class Device extends HardwareAddress {
    private final String name;
    public Device(final String name) throws UnsupportedEncodingException {
        super(name.getBytes("UTF8"));

        this.name = name;
    }

    public Device(final XMLStreamReader reader) throws XMLStreamException, UnsupportedEncodingException {
        this(reader.getElementText());
    }

    @Override
    public void writeToXml(final XMLStreamWriter target) throws XMLStreamException {
        target.writeCharacters(this.name);
    }

    @Override
    public String toString() {
        return name;
    }
}
