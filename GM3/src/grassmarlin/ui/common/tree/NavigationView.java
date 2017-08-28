package grassmarlin.ui.common.tree;

import javafx.beans.Observable;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.*;
import javafx.stage.WindowEvent;

import java.util.LinkedList;
import java.util.List;

public class NavigationView extends TreeView<Object> {
    private final ContextMenu menu;
    private final MenuItem itemPlaceholder = new MenuItem("Placeholder");

    public NavigationView() {
        this.menu = new ContextMenu();

        initComponents();
    }

    private void initComponents() {
        this.setShowRoot(false);
        this.getSelectionModel().selectedItemProperty().addListener(this::Handle_SelectedItemChanged);

        this.menu.setOnShowing(this::Handle_MenuShowing);
        this.menu.setOnHiding(this::Handle_MenuHiding);
        this.menu.getItems().add(itemPlaceholder);

        this.setContextMenu(this.menu);
    }

    private void Handle_MenuShowing(WindowEvent event) {
        //Remove the placeholder item.
        this.menu.getItems().clear();

        //Focus will be whatever is under the cursor, or what was last under the cursor if the cursor is over a blank row.
        TreeItem<Object> item = this.getFocusModel().getFocusedItem();
        final List<List<MenuItem>> itemGroups = new LinkedList<>();
        // Iterate through parents until reaching root, adding items as appropriate.
        while(item != null) {
            if(item instanceof IContextMenuAction) {
                final List<MenuItem> items = ((IContextMenuAction)item).getMenuItems(false);

                if(items != null && !items.isEmpty()) {
                    itemGroups.add(items);
                }
            }

            item = item.getParent();
        }

        boolean hasPrevious = false;
        for(List<MenuItem> itemGroup : itemGroups) {
            if(hasPrevious) {
                this.menu.getItems().add(new SeparatorMenuItem());
                hasPrevious = true;
            }
            this.menu.getItems().addAll(itemGroup);
        }
    }
    private void Handle_MenuHiding(WindowEvent event) {
        this.menu.getItems().clear();
        this.menu.getItems().add(itemPlaceholder);
    }

    private void Handle_SelectedItemChanged(Observable observable, TreeItem<Object> oldValue, TreeItem<Object> newValue) {
        if(oldValue != null && oldValue instanceof ISelectAction) {
            final EventHandler<ActionEvent> handler = ((ISelectAction)oldValue).getOnDeselected();
            if(handler != null) {
                handler.handle(new ActionEvent(this, oldValue));
            }
        }
        if(newValue != null && newValue instanceof ISelectAction) {
            final EventHandler<ActionEvent> handler = ((ISelectAction)newValue).getOnSelected();
            if(handler != null) {
                handler.handle(new ActionEvent(this, newValue));
            }

        }
    }
}
