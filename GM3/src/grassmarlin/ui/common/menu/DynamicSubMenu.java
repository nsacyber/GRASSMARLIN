package grassmarlin.ui.common.menu;

import javafx.beans.binding.BooleanExpression;
import javafx.event.Event;
import javafx.scene.Node;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;

import java.util.Collection;

public class DynamicSubMenu extends Menu {
    private final MenuItem disabled = new MenuItem("No Options Available");

    @FunctionalInterface
    public interface IGetMenuItems {
        Collection<MenuItem> getDynamicItems();
    }

    private final IGetMenuItems fnGetItems;

    protected DynamicSubMenu(final String title, final Node graphic) {
        super(title, graphic);
        if(this instanceof IGetMenuItems) {
            this.fnGetItems = (IGetMenuItems)this;
        } else {
            throw new IllegalArgumentException("Constructor can only be called from a subclass that implements IGetMenuItems.");
        }

        initComponents();
    }
    public DynamicSubMenu(final String title, final Node graphic, final IGetMenuItems fnGetMenuItems) {
        super(title, graphic);
        this.fnGetItems = fnGetMenuItems;

        initComponents();
    }

    private void initComponents() {
        getItems().add(disabled);

        setOnShowing(this::Handle_showing);
        disabled.setDisable(true);
    }

    protected void Handle_showing(Event evt) {
        getItems().clear();
        final Collection<MenuItem> items = fnGetItems.getDynamicItems();
        if(items == null || items.isEmpty()) {
            getItems().add(disabled);
        } else {
            getItems().addAll(items);
        }
    }

    public DynamicSubMenu bindEnabled(final BooleanExpression controller) {
        this.disableProperty().bind(controller.not());
        return this;
    }
}
