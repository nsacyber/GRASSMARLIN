package grassmarlin.plugins.internal.logicalview.visual.filters;

import grassmarlin.plugins.internal.logicalview.visual.VisualLogicalEdge;
import grassmarlin.session.serialization.XmlSerializable;
import javafx.beans.binding.StringExpression;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.paint.Color;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

public abstract class EdgeStyleRule implements XmlSerializable {
    protected final ObjectProperty<Style> style;
    protected final DoubleProperty weight;
    protected final DoubleProperty opacity;
    protected final ObjectProperty<Color> color;

    protected EdgeStyleRule() {
        this.style = new SimpleObjectProperty<>();
        this.weight = new SimpleDoubleProperty();
        this.opacity = new SimpleDoubleProperty();
        this.color = new SimpleObjectProperty<>();

        this.style.addListener((observable, oldValue, newValue) -> {
            if(newValue == null) {
                EdgeStyleRule.this.weight.unbind();
                EdgeStyleRule.this.weight.set(1.0);
                EdgeStyleRule.this.opacity.unbind();
                EdgeStyleRule.this.opacity.set(1.0);
                EdgeStyleRule.this.color.unbind();
                EdgeStyleRule.this.color.set(Color.BLACK);
            } else {
                EdgeStyleRule.this.weight.bind(newValue.weightProperty());
                EdgeStyleRule.this.opacity.bind(newValue.opacityProperty());
                EdgeStyleRule.this.color.bind(newValue.colorProperty());
            }
        });
    }

    public ObjectProperty<Style> styleProperty() {
        return this.style;
    }

    public abstract boolean applies(final VisualLogicalEdge edge);

    public abstract StringExpression descriptionProperty();

    @Override
    public void readFromXml(final XMLStreamReader source) throws XMLStreamException {

    }

    @Override
    public void writeToXml(final XMLStreamWriter writer) throws XMLStreamException {

    }
}
