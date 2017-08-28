package grassmarlin.ui.common.tree;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.MenuItem;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class ActiveTreeItem<T> extends SelectableTreeItem<T> implements IContextMenuAction {
    public static class ContextItem {
        private final MenuItem menuItem;
        private final boolean includeSelf;
        private final boolean includeOther;

        public ContextItem(final MenuItem item, final boolean self, final boolean other) {
            this.menuItem = item;
            this.includeSelf = self;
            this.includeOther = other;
        }

        public MenuItem getMenuItem() {
            return this.menuItem;
        }
        public boolean isIncludedSelf() {
            return includeSelf;
        }
        public boolean isIncludedOther() {
            return includeOther;
        }
    }

    private final List<ContextItem> items;

    public ActiveTreeItem(final T value, final ContextItem... items) {
        this(value, null, null, items);
    }
    public ActiveTreeItem(final T value, final EventHandler<ActionEvent> handlerSelected, final ContextItem... items) {
        this(value, handlerSelected, null, items);
    }
    public ActiveTreeItem(final T value, final EventHandler<ActionEvent> handlerSelected, final EventHandler<ActionEvent> handlerDeselected, final ContextItem... items) {
        super(value, handlerSelected, handlerDeselected);

        this.items = Arrays.asList(items);

    }

    @Override
    public List<MenuItem> getMenuItems(final boolean wasChildActivated) {
        return items.stream()
                .filter(item -> wasChildActivated ? item.isIncludedOther() : item.isIncludedSelf())
                .map(item -> item.getMenuItem())
                .collect(Collectors.toList());
    }
}
