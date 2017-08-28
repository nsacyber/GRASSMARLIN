package grassmarlin.session;

import grassmarlin.Event;
import grassmarlin.session.pipeline.LogicalAddressMapping;
import grassmarlin.session.serialization.Writer;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import java.util.Map;
import java.util.Set;

public class LogicalVertex extends PropertyContainer {
    public final static String SOURCE_INHERITED_PROPERTIES = "Hardware";

    private final LogicalAddressMapping mapping;
    private final HardwareVertex base;

    public LogicalVertex(final Event.IAsyncExecutionProvider provider, final LogicalAddressMapping mapping, final HardwareVertex baseVertex) {
        super(provider);

        this.mapping = mapping;
        this.base = baseVertex;

        baseVertex.onPropertyChanged.addHandler(this.handlerBasePropertyChanged);

        for(Map.Entry<String, Set<Property<?>>> entry : this.base.getProperties().entrySet()) {
            this.addProperties(SOURCE_INHERITED_PROPERTIES, entry.getKey(), entry.getValue());
        }
    }

    public LogicalVertex(final Event.IAsyncExecutionProvider provider, final LogicalAddressMapping mapping, final HardwareVertex baseVertex, final PropertyContainer properties) {
        super(provider, properties);

        this.mapping = mapping;
        this.base = baseVertex;

        baseVertex.onPropertyChanged.addHandler(this.handlerBasePropertyChanged);
    }

    private final Event.EventListener<PropertyContainer.PropertyEventArgs> handlerBasePropertyChanged = this::handle_BasePropertyChanged;

    private void handle_BasePropertyChanged(final Event<PropertyContainer.PropertyEventArgs> event, final PropertyContainer.PropertyEventArgs args) {
        //HACK: Because we're using computed properties, we don't know the source--so we just use the "Hardware" source for all inherited properties since we don't have a proper inheritance model.
        if(args.isAdded()) {
            this.addProperties(SOURCE_INHERITED_PROPERTIES, args.getName(), args.getProperty());
        } else {
            this.removeProperties(SOURCE_INHERITED_PROPERTIES, args.getName(), args.getProperty());
        }
    }

    public LogicalAddressMapping getLogicalAddressMapping() {
        return this.mapping;
    }
    public LogicalAddress<?> getLogicalAddress() {
        return this.mapping.getLogicalAddress();
    }
    public HardwareVertex getHardwareVertex() {
        return this.base;
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
