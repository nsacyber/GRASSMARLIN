package grassmarlin.plugins.internal.logicalview.visual.colorfactories;

import grassmarlin.plugins.internal.logicalview.visual.LogicalVisualization;
import javafx.scene.paint.Color;

public class HashColorFactory implements LogicalVisualization.ICalculateColorsForAggregate {
    public HashColorFactory() {

    }

    protected int computeRgb444(final Object value) {
        return (value.hashCode() & 0x0FFF) ^ ((value.hashCode() >>> 12) & 0x0FFF) ^ ((value.hashCode() >>> 24) & 0x03FC);
    }

    @Override
    public Color getBorderColor(final Object o) {
        final int rgb444 = computeRgb444(o);
        final int r = (rgb444 >>> 4) & 0xF0;
        final int g = rgb444 & 0xF0;
        final int b = (rgb444 << 4) & 0xF0;

        return Color.rgb(r, g, b);
    }
    @Override
    public Color getBackgroundColor(final Object o) {
        final int rgb444 = computeRgb444(o);
        final int r = (rgb444 >>> 4) & 0xF0;
        final int g = rgb444 & 0xF0;
        final int b = (rgb444 << 4) & 0xF0;

        return Color.rgb(r, g, b, 0.4);
    }
}
