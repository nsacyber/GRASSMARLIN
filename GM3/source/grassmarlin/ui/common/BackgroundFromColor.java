package grassmarlin.ui.common;

import javafx.beans.binding.ObjectBinding;
import javafx.beans.binding.ObjectExpression;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.geometry.Insets;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.paint.Paint;

public class BackgroundFromColor extends ObjectBinding<Background> {
    private final ObjectExpression<? extends Paint> color;
    private final ObjectExpression<? extends CornerRadii> radii;
    private final ObjectExpression<? extends Insets> insets;

    public BackgroundFromColor(ObjectExpression<? extends Paint> color) {
        //Cast is necessary because otherwise the call becomes ambiguous.
        //noinspection RedundantCast
        this(color, (CornerRadii)null, (Insets)null);
    }
    public BackgroundFromColor(ObjectExpression<? extends Paint> color, final CornerRadii radii, final Insets insets) {
        super.bind(color);

        this.color = color;
        this.radii = new ReadOnlyObjectWrapper<>(radii);
        this.insets = new ReadOnlyObjectWrapper<>(insets);
    }
    public BackgroundFromColor(ObjectExpression<? extends Paint> color, final ObjectExpression<? extends CornerRadii> radii, final ObjectExpression<? extends Insets> insets) {
        super.bind(color, radii, insets);

        this.color = color;
        this.radii = radii == null ? new ReadOnlyObjectWrapper<>(null) : radii;
        this.insets = insets == null ? new ReadOnlyObjectWrapper<>(null) : insets;
    }

    @Override
    public Background computeValue() {
        return new Background(new BackgroundFill(color.get(), radii.get(), insets.get()));
    }
}
