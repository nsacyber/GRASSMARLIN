package grassmarlin.ui.common;

import javafx.beans.binding.DoubleBinding;
import javafx.beans.binding.NumberExpression;

public class StrokeWidthFromByteCountBinding extends DoubleBinding {
    private final NumberExpression bytes;

    public StrokeWidthFromByteCountBinding(final NumberExpression bytes) {
        super.bind(bytes);

        this.bytes = bytes;
    }

    @Override
    public double computeValue() {
        return Math.max(0.5, Math.log10(bytes.doubleValue()) - 2.0 );
    }
}
