package grassmarlin.ui.common.tree;

import javafx.scene.control.MenuItem;

import java.util.List;

public interface IContextMenuAction {
    /**
     * Called to generate a context menu for a tree item.
     * @param wasChildActivated    If the context menu was activated for a child of this node, then this will be set to true.  False if this node was clicked directly.
     * @return A list of MenuItems to add to the context menu.
     */
    List<MenuItem> getMenuItems(boolean wasChildActivated);
}
