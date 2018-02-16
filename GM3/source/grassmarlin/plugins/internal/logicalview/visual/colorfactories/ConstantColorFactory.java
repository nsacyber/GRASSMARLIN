package grassmarlin.plugins.internal.logicalview.visual.colorfactories;

import grassmarlin.plugins.internal.logicalview.ILogicalViewApi;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.paint.Color;

public class ConstantColorFactory implements ILogicalViewApi.ICalculateColorsForAggregate {
    private final ObjectProperty<Color> color;

    public ConstantColorFactory() {
        this.color = new SimpleObjectProperty<>(Color.RED);
    }

    public ObjectProperty<Color> colorProperty() {
        return this.color;
    }

    @Override
    public Color getBorderColor(final Object o) {
        return this.color.get();
    }
    @Override
    public Color getBackgroundColor(final Object o) {
        return Color.rgb(
                (int)((this.color.get()).getRed() * 255.0),
                (int)((this.color.get()).getGreen() * 255.0),
                (int)((this.color.get()).getBlue() * 255.0),
                0.4
        );
    }
}
