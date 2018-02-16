package grassmarlin.session;

import grassmarlin.Event;
import grassmarlin.session.pipeline.LogicalAddressMapping;
import grassmarlin.session.serialization.Writer;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

public class LogicalVertex extends PropertyContainer {
    private final LogicalAddressMapping mapping;
    private final HardwareVertex base;
    private final Session session;

    public LogicalVertex(final Event.IAsyncExecutionProvider provider, final Session session, final LogicalAddressMapping mapping, final HardwareVertex baseVertex) {
        super(provider);

        this.session = session;
        this.mapping = mapping;
        this.base = baseVertex;

        this.addAncestor(baseVertex);
    }

    public LogicalAddressMapping getLogicalAddressMapping() {
        return this.mapping;
    }
    public LogicalAddress getLogicalAddress() {
        return this.mapping.getLogicalAddress();
    }
    public HardwareVertex getHardwareVertex() {
        return this.base;
    }
    public Session getSession() {
        return this.session;
    }

    @Override
    public String toString() {
        return String.format("%s\n%s", this.mapping, super.toString());
    }

    @Override
    public void writeToXml(final XMLStreamWriter writer) throws XMLStreamException {
        //The HardwareVertex will be written by the Session--it will write a reference to the serialized HardwareVertex
        //Since the HardwareAddress is fixed, we only need the LogicalAddress from the mapping.
        writer.writeStartElement("LogicalAddress");
        Writer.writeObject(writer, mapping.getLogicalAddress());
        writer.writeEndElement();
        writer.writeStartElement("Properties");
        super.writeToXml(writer);
        writer.writeEndElement();
    }
}
