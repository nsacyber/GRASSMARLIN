package grassmarlin.plugins.internal.logicalview.visual.filters;

import grassmarlin.plugins.internal.logicalview.visual.VisualLogicalEdge;
import grassmarlin.session.Property;
import grassmarlin.session.serialization.Loader;
import grassmarlin.session.serialization.Writer;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.stream.events.XMLEvent;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

public class PropertyEdgeStyleRule extends EdgeStyleRule {
    private final static Set<Property<?>> EMPTY_SET = new HashSet<>();

    private String propertyName;
    private Serializable propertyValue;

    public PropertyEdgeStyleRule() {
        this(null, null);
    }
    public PropertyEdgeStyleRule(final String name, final Serializable value) {
        this.propertyName = name;
        this.propertyValue = value;
    }

    @Override
    public boolean applies(VisualLogicalEdge edge) {
        if(propertyValue == null) {
            return edge.getEdgeData().getProperties().containsKey(propertyName);
        } else {
            return edge.getEdgeData().getProperties().getOrDefault(propertyName, EMPTY_SET).stream().anyMatch(property -> property.getValue().equals(propertyValue));
        }
    }

    @Override
    public String toString() {
        if(propertyValue == null) {
            return String.format("Apply %s when [%s] is present", this.styleNameProperty.get(), propertyName);
        } else {
            return String.format("Apply %s when [%s] is [%s]", this.styleNameProperty.get(), propertyName, propertyValue);
        }
    }

    @Override
    public void readFromXml(final XMLStreamReader source) throws XMLStreamException {
        super.readFromXml(source);

        this.propertyName = source.getAttributeValue(null, "propertyName");
        if(source.nextTag() == XMLEvent.START_ELEMENT && source.getLocalName().equals("PropertyValue")) {
            this.propertyValue = (Serializable) Loader.readObject(source);
        }
    }

    @Override
    public void writeToXml(final XMLStreamWriter writer) throws XMLStreamException {
        super.writeToXml(writer);

        writer.writeAttribute("propertyName", propertyName);
        if(propertyValue != null) {
            writer.writeStartElement("PropertyValue");
            Writer.writeObject(writer, propertyValue);
            writer.writeEndElement();
        }
    }

    public String getPropertyName() {
        return this.propertyName;
    }
}
