package grassmarlin.plugins.internal.logicalview.visual.colorfactories;

import grassmarlin.plugins.internal.logicalview.visual.LogicalVisualization;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.paint.Color;

public class ReferenceColorFactory implements LogicalVisualization.ICalculateColorsForAggregate {
    private final ObjectProperty<LogicalVisualization.ICalculateColorsForAggregate> internal;

    public ReferenceColorFactory(final LogicalVisualization.ICalculateColorsForAggregate initial) {
        internal = new SimpleObjectProperty<>(initial);
    }

    @Override
    public Color getBorderColor(final Object o) {
        return internal.get().getBorderColor(o);
    }
    @Override
    public Color getBackgroundColor(final Object o) {
        return internal.get().getBackgroundColor(o);
    }

    public ObjectProperty<LogicalVisualization.ICalculateColorsForAggregate> valueProperty() {
        return this.internal;
    }
}
