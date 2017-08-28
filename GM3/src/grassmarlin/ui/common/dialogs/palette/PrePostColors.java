package grassmarlin.ui.common.dialogs.palette;

import javafx.beans.property.ObjectProperty;
import javafx.scene.layout.HBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

public class PrePostColors extends HBox {
    public PrePostColors(final ObjectProperty<Color> initial, final ObjectProperty<Color> selected) {
        final Rectangle rectPre = new Rectangle(40, 60);
        rectPre.fillProperty().bind(initial);

        final Rectangle rectPost = new Rectangle(40, 60);
        rectPost.fillProperty().bind(selected);

        this.getChildren().addAll(rectPre, rectPost);
    }
}
