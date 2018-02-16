package grassmarlin.ui.common.menu;

import javafx.beans.property.BooleanProperty;
import javafx.scene.control.CheckMenuItem;

public class BoundCheckMenuItem extends CheckMenuItem {
    public BoundCheckMenuItem(final String name, final BooleanProperty value) {
        super(name);

        this.selectedProperty().bindBidirectional(value);
    }
}
