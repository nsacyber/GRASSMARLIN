package grassmarlin.ui.common;

import javafx.beans.binding.ObjectBinding;
import javafx.beans.binding.ObjectExpression;
import javafx.scene.paint.Paint;

/**
 * Hack class to get around binding ObjectProperty&lt;Paint&gt;
 */
public class PaintBindingForColor extends ObjectBinding<Paint> {
    private final ObjectExpression<? extends Paint> base;

    public PaintBindingForColor(final ObjectExpression<? extends Paint> base) {
        this.base = base;

        super.bind(base);
    }

    @Override
    protected Paint computeValue() {
        return base.get();
    }
}
