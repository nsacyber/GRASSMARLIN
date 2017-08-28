package grassmarlin.ui.common;

import javafx.beans.binding.NumberExpression;
import javafx.beans.binding.ObjectBinding;
import javafx.beans.binding.ObjectExpression;
import javafx.scene.paint.Color;
import javafx.scene.paint.Paint;

public class TranslucentColorExpression extends ObjectBinding<Paint> {
    private final ObjectExpression<Color> colorBase;
    private final NumberExpression opacity;

    public TranslucentColorExpression(final ObjectExpression<Color> colorBase, final NumberExpression opacity ) {
        this.colorBase = colorBase;
        this.opacity = opacity;

        super.bind(colorBase, opacity);
    }

    @Override
    public Paint computeValue() {
        final Color color = colorBase.get();
        // get methods return [0.0, 1.0], rgb method expects [0-255]
        return Color.rgb(
                (int)(color.getRed() * 255.0),
                (int)(color.getGreen() * 255.0),
                (int)(color.getBlue() * 255.0),
                opacity.doubleValue());
    }
}
