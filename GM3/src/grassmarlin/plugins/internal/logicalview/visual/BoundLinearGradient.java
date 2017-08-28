package grassmarlin.plugins.internal.logicalview.visual;

import javafx.beans.binding.NumberExpression;
import javafx.beans.binding.ObjectBinding;
import javafx.beans.binding.ObjectExpression;
import javafx.scene.paint.*;

public class BoundLinearGradient extends ObjectBinding<Paint> {
    private final NumberExpression x1;
    private final NumberExpression y1;
    private final NumberExpression x2;
    private final NumberExpression y2;
    private final ObjectExpression<Color> c1;
    private final ObjectExpression<Color> c2;

    public BoundLinearGradient(final NumberExpression x1, final NumberExpression y1, final NumberExpression x2, final NumberExpression y2, final ObjectExpression<Color> c1, final ObjectExpression<Color> c2) {
        super.bind(x1, x2, y1, y2, c1, c2);

        this.x1 = x1;
        this.y1 = y1;
        this.x2 = x2;
        this.y2 = y2;

        this.c1 = c1;
        this.c2 = c2;
    }

    @Override
    public Paint computeValue() {
        //If the endpoints are the same color, we don't need a gradient.
        if(c1.get().equals(c2.get())) {
            return c1.get();
        }

        return new LinearGradient(x1.doubleValue(), y1.doubleValue(), x2.doubleValue(), y2.doubleValue(), false, CycleMethod.NO_CYCLE, new Stop(0.0, (Color)c1.get()), new Stop(1.0, (Color)c2.get()));
    }
}
