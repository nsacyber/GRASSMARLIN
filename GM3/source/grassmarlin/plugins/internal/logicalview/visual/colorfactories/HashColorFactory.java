package grassmarlin.plugins.internal.logicalview.visual.colorfactories;

import grassmarlin.plugins.internal.logicalview.ILogicalViewApi;
import javafx.scene.paint.Color;

public class HashColorFactory implements ILogicalViewApi.ICalculateColorsForAggregate {
    public HashColorFactory() {

    }

    protected Color computeRgb(final Object value, final double opacity) {
        final int hash = value.hashCode() ^ 0xDEADBEEF;

        int r = 0;
        int g = 0;
        int b = 0;

        r |= (hash >> 24) & 0x00C0;
        g |= (hash >> 22) & 0x00E0;
        b |= (hash >> 19) & 0x00E0;

        r |= (hash >> 18) & 0x0038;
        g |= (hash >> 16) & 0x001C;
        b |= (hash >> 13) & 0x0018;

        r |= (hash >> 13) & 0x0007;
        g |= (hash >> 11) & 0x0003;
        b |= (hash >> 8) & 0x0007;

        return Color.rgb(r, g, b, opacity);
    }

    @Override
    public Color getBorderColor(final Object o) {
        return computeRgb(o, 1.0);
    }
    @Override
    public Color getBackgroundColor(final Object o) {
        return computeRgb(o, 0.4);
    }
}
