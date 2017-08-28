package grassmarlin.ui.common.menu;

import javafx.beans.binding.BooleanExpression;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.control.MenuItem;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;

public class ActiveMenuItem extends MenuItem {
    private final static double ICON_SIZE = 16.0;

    public ActiveMenuItem(final String title, final EventHandler<ActionEvent> handler) {
        this(title, null, handler);
    }
    public ActiveMenuItem(final String title, final Node graphic, final EventHandler<ActionEvent> handler) {
        super(title, graphic);
        super.setOnAction(handler);
    }

    public ActiveMenuItem(final ObservableValue<String> title, final EventHandler<ActionEvent> handler) {
        this(title, null, handler);
    }
    public ActiveMenuItem(final ObservableValue<String> title, final Node graphic, final EventHandler<ActionEvent> handler) {
        super(title.getValue(), graphic);
        super.setOnAction(handler);
    }

    public ActiveMenuItem bindEnabled(final BooleanExpression controller) {
        this.disableProperty().bind(controller.not());
        return this;
    }

    public ActiveMenuItem setAccelerator(final KeyCodeCombination.Modifier modifier, final KeyCode key) {
        if(modifier == null) {
            this.setAccelerator(new KeyCodeCombination(key));
        } else {
            this.setAccelerator(new KeyCodeCombination(key, modifier));
        }
        return this;
    }
}
