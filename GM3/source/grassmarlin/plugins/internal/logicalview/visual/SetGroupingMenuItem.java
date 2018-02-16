package grassmarlin.plugins.internal.logicalview.visual;

import grassmarlin.plugins.internal.logicalview.LogicalGraph;
import grassmarlin.ui.common.menu.DynamicSubMenu;
import javafx.beans.property.StringProperty;
import javafx.scene.control.MenuItem;
import javafx.scene.control.RadioMenuItem;
import javafx.scene.control.ToggleGroup;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.stream.Collectors;

public class SetGroupingMenuItem extends DynamicSubMenu implements DynamicSubMenu.IGetMenuItems {
    private final LogicalGraph graph;
    private final HashMap<String, RadioMenuItem> cache;
    private final ToggleGroup groupToggle;
    private final RadioMenuItem miNothing;
    private final StringProperty groupProperty;

    public SetGroupingMenuItem(final LogicalGraph graph, final StringProperty groupProperty) {
        super("Group By", null);

        this.graph = graph;
        this.groupProperty = groupProperty;
        this.cache = new HashMap<>();
        this.groupToggle = new ToggleGroup();

        this.miNothing = new RadioMenuItem("(None)");
        this.miNothing.setOnAction(event -> {
            SetGroupingMenuItem.this.groupProperty.set(null);
        });
        this.miNothing.setToggleGroup(this.groupToggle);
    }

    @Override
    public Collection<MenuItem> getDynamicItems() {
        final String currentValue = this.groupProperty.get();
        final ArrayList<MenuItem> items = new ArrayList<>();
        //"No grouping" is always available.
        items.add(miNothing);
        this.miNothing.setSelected(currentValue == null);

        final ArrayList<String> groupings = new ArrayList<>();
        this.graph.getGroupings(groupings);
        groupings.sort(String::compareToIgnoreCase);

        items.addAll(groupings.stream().map(title -> {
            final MenuItem existing = SetGroupingMenuItem.this.cache.get(title);
            if(existing != null) {
                ((RadioMenuItem)existing).setSelected(title.equals(currentValue));
                return existing;
            } else {
                final RadioMenuItem ck = new RadioMenuItem(title);
                ck.setToggleGroup(SetGroupingMenuItem.this.groupToggle);
                ck.setOnAction(event -> {
                    SetGroupingMenuItem.this.groupProperty.set(title);
                });
                ck.setSelected(title.equals(currentValue));
                SetGroupingMenuItem.this.cache.put(title, ck);
                return ck;
            }
        }).collect(Collectors.toList()));
        return items;
    }
}
