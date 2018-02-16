package grassmarlin.session;

import grassmarlin.Event;
import grassmarlin.session.serialization.Writer;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

public class HardwareVertex extends PropertyContainer {
    private final HardwareAddress address;

    public HardwareVertex(final Event.IAsyncExecutionProvider provider, final HardwareAddress address) {
        super(provider);

        this.address = address;
    }
    public HardwareVertex(final Event.IAsyncExecutionProvider provider, final HardwareAddress address, final PropertyContainer properties) {
        super(provider, properties);

        this.address = address;
    }

    public HardwareAddress getAddress() {
        return this.address;
    }

    @Override
    public int hashCode() {
        return this.address.hashCode();
    }

    @Override
    public boolean equals(final Object other) {
        return other != null && other instanceof HardwareVertex && ((HardwareVertex) other).address.equals(this.address);
    }

    @Override
    public String toString() {
        return String.format("HardwareVertex(%s)\n%s", address, super.toString());
    }

    @Override
    public void writeToXml(final XMLStreamWriter target) throws XMLStreamException {
        target.writeStartElement("HardwareAddress");
        Writer.writeObject(target, address);
        target.writeEndElement();
        target.writeStartElement("Properties");
        super.writeToXml(target);
        target.writeEndElement();
    }
}
