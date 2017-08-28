package grassmarlin.plugins.internal.logicalview.visual.filters;

import com.sun.javafx.collections.ObservableListWrapper;
import grassmarlin.RuntimeConfiguration;
import grassmarlin.plugins.internal.logicalview.visual.VisualLogicalEdge;
import grassmarlin.session.serialization.XmlSerializable;
import javafx.beans.Observable;
import javafx.beans.property.*;
import javafx.collections.ObservableList;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.stream.Collectors;

public abstract class EdgeStyleRule implements XmlSerializable {
    public final static RuntimeConfiguration.IPersistedValue RULE_LIST = new RuntimeConfiguration.IPersistedValue() {
        @Override
        public String getKey() {
            return "grassmarlin.plugins.logicalview.stylerules";
        }

        @Override
        public RuntimeConfiguration.IDefaultValue getFnDefault() {
            return () -> "";
        }
    };
    private final static RuntimeConfiguration.IPersistedValue STYLE_LIST = new RuntimeConfiguration.IPersistedValue() {
        @Override
        public String getKey() {
            return "grassmarlin.plugins.logicalview.styles";
        }

        @Override
        public RuntimeConfiguration.IDefaultValue getFnDefault() {
            return () -> "Bold|Suppressed";
        }
    };

    public final static ObservableList<Style> styles = new ObservableListWrapper<>(new ArrayList<>());
    static {
        styles.addAll(Arrays.stream(RuntimeConfiguration.getPersistedString(STYLE_LIST).split("|")).map(nameStyle -> Style.getStyle(nameStyle)).collect(Collectors.toList()));
        styles.addListener(EdgeStyleRule::HandleStylesChanged);
    }

    private static void HandleStylesChanged(Observable obs) {
        RuntimeConfiguration.setPersistedString(STYLE_LIST, styles.stream().map(style -> style.getName()).collect(Collectors.joining("|")));
    }

    public static EdgeStyleRule hasProperty(final String propertyName) {
        return hasProperty(propertyName, null);
    }
    public static EdgeStyleRule hasProperty(final String name, final Serializable value) {
        return new PropertyEdgeStyleRule(name, value);
    }

    protected final StringProperty styleNameProperty;
    protected final ObjectProperty<Style> style;
    protected final DoubleProperty weight;
    protected final DoubleProperty opacity;

    protected EdgeStyleRule() {
        this.styleNameProperty = new SimpleStringProperty();
        this.style = new SimpleObjectProperty<>();
        this.weight = new SimpleDoubleProperty();
        this.opacity = new SimpleDoubleProperty();

        this.style.addListener((observable, oldValue, newValue) -> {
            if(newValue == null) {
                EdgeStyleRule.this.weight.unbind();
                EdgeStyleRule.this.weight.set(1.0);
                EdgeStyleRule.this.opacity.unbind();
                EdgeStyleRule.this.opacity.set(1.0);
                EdgeStyleRule.this.styleNameProperty.set("");
            } else {
                EdgeStyleRule.this.weight.bind(newValue.weightProperty());
                EdgeStyleRule.this.opacity.bind(newValue.opacityProperty());
                EdgeStyleRule.this.styleNameProperty.set(newValue.getName());
            }
        });
    }

    public ObjectProperty<Style> styleProperty() {
        return this.style;
    }

    public abstract boolean applies(final VisualLogicalEdge edge);

    @Override
    public void readFromXml(final XMLStreamReader source) throws XMLStreamException {
        this.styleProperty().set(Style.getStyle(source.getAttributeValue(null, "style")));
    }

    @Override
    public void writeToXml(final XMLStreamWriter writer) throws XMLStreamException {
        writer.writeAttribute("style", this.styleNameProperty.get());
    }
}
