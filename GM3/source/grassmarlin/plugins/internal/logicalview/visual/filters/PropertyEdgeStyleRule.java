package grassmarlin.plugins.internal.logicalview.visual.filters;

import grassmarlin.plugins.internal.logicalview.visual.VisualLogicalEdge;
import grassmarlin.session.Property;
import grassmarlin.session.serialization.Loader;
import grassmarlin.session.serialization.Writer;
import javafx.beans.binding.StringExpression;
import javafx.beans.property.ReadOnlyStringWrapper;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.stream.events.XMLEvent;
import java.io.Serializable;
import java.util.Collections;
import java.util.Set;

public class PropertyEdgeStyleRule extends EdgeStyleRule {

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
            return edge.getEdgeData().getProperties().getOrDefault(propertyName, (Set<Property<?>>)Collections.EMPTY_SET).stream().anyMatch(property -> property.getValue().equals(propertyValue));
        }
    }

    @Override
    public StringExpression descriptionProperty() {
        if(this.propertyValue == null) {
            return new ReadOnlyStringWrapper("Apply ").concat(this.style).concat(" when [").concat(propertyName).concat("] is present.");
        } else {
            return new ReadOnlyStringWrapper("Apply ").concat(this.style).concat(" when [").concat(propertyName).concat("] is [").concat(propertyValue).concat("].");
        }
    }

    @Override
    public String toString() {
        if(propertyValue == null) {
            return String.format("Apply %s when [%s] is present", this.style.get().getName(), propertyName);
        } else {
            return String.format("Apply %s when [%s] is [%s]", this.style.get().getName(), propertyName, propertyValue);
        }
    }

    @Override
    public void readFromXml(final XMLStreamReader source) throws XMLStreamException {
        this.propertyName = source.getAttributeValue(null, "propertyName");
        if(source.nextTag() == XMLEvent.START_ELEMENT && source.getLocalName().equals("PropertyValue")) {
            this.propertyValue = (Serializable) Loader.readObject(source);
        }
    }

    @Override
    public void writeToXml(final XMLStreamWriter writer) throws XMLStreamException {
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
