package grassmarlin.ui.common.tree;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.TreeItem;

public class SelectableTreeItem<T> extends TreeItem<T> implements ISelectAction {
    private final EventHandler<ActionEvent> handlerSelected;
    private final EventHandler<ActionEvent> handlerDeselected;

    public SelectableTreeItem(final T t, final EventHandler<ActionEvent> handlerSelected) {
        this(t, handlerSelected, null);
    }
    public SelectableTreeItem(final T t, final EventHandler<ActionEvent> handlerSelected, final EventHandler<ActionEvent> handlerDeselected) {
        super(t);

        this.handlerSelected = handlerSelected;
        this.handlerDeselected = handlerDeselected;
    }

    @Override
    public EventHandler<ActionEvent> getOnSelected() {
        return handlerSelected;
    }
    @Override
    public EventHandler<ActionEvent> getOnDeselected() {
        return handlerDeselected;
    }
}
