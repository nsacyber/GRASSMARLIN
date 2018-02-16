package grassmarlin.ui.common.menu;

import grassmarlin.plugins.IPlugin;
import javafx.beans.binding.BooleanExpression;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.control.MenuItem;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;

public class ActiveMenuItem extends MenuItem {
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

    public ActiveMenuItem chainSetAccelerator(final KeyCode key, final KeyCombination.Modifier... modifiers) {
        super.setAccelerator(new KeyCodeCombination(key, modifiers));
        return this;
    }
    public ActiveMenuItem chainSetAccelerator(final KeyCombination keyCombination) {
        super.setAccelerator(keyCombination);
        return this;
    }

    @IPlugin.SafeForConsole
    private class ScriptableMenuItem extends MenuItem {
        public ScriptableMenuItem() {
            this.disableProperty().bind(ActiveMenuItem.this.disableProperty());
            this.visibleProperty().bind(ActiveMenuItem.this.visibleProperty());
            this.onActionProperty().bind(ActiveMenuItem.this.onActionProperty());
            this.textProperty().bind(ActiveMenuItem.this.textProperty());
        }
    }

    public MenuItem safeForScripting() {
        return new ScriptableMenuItem();
    }
}
