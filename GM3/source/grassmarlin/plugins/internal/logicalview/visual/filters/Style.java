package grassmarlin.plugins.internal.logicalview.visual.filters;

import grassmarlin.session.serialization.XmlSerializable;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.paint.Color;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

public class  Style implements XmlSerializable {
    public final static double WEIGHT_MIN = 0f;
    public final static double WEIGHT_MAX = 10f;

    public final static double OPACITY_MIN = 0f;
    public final static double OPACITY_MAX = 1f;

    private final String name;
    private final DoubleProperty weight;
    private final DoubleProperty opacity;
    private final ObjectProperty<Color> color;

    public Style(final String name) {
        this.name = name;
        this.weight = new SimpleDoubleProperty(1.0);
        this.opacity = new SimpleDoubleProperty(1.0);
        this.color = new SimpleObjectProperty<>(Color.BLACK);
    }

    public Style(final XMLStreamReader reader) {
        this(reader.getAttributeValue(null, "Name"));
        this.weight.set(Double.parseDouble(reader.getAttributeValue(null, "Weight")));
        this.opacity.set(Double.parseDouble(reader.getAttributeValue(null, "Opacity")));
        this.color.set(Color.web(reader.getAttributeValue(null, "Color")));
    }

    public DoubleProperty weightProperty() {
        return this.weight;
    }

    public DoubleProperty opacityProperty() {
        return this.opacity;
    }

    public ObjectProperty<Color> colorProperty() {
        return this.color;
    }
    public String getName() {
        return this.name;
    }

    @Override
    public String toString() {
        return this.getName();
    }

    @Override
    public void writeToXml(XMLStreamWriter target) throws XMLStreamException {
        target.writeAttribute("Name", this.name);
        target.writeAttribute("Weight", Double.toString(this.weight.get()));
        target.writeAttribute("Opacity", Double.toString(this.opacity.get()));
        target.writeAttribute("Color", this.color.get().toString().substring(2,8));
    }
}
