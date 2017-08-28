package grassmarlin.plugins.internal.logicalview.visual;

import grassmarlin.plugins.internal.logicalview.FilteredLogicalGraph;
import grassmarlin.ui.common.menu.DynamicSubMenu;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.MenuItem;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.stream.Collectors;

public class SetGroupingMenuItem extends DynamicSubMenu implements DynamicSubMenu.IGetMenuItems {
    private final FilteredLogicalGraph graph;
    private final HashMap<String, CheckMenuItem> cache;
    private final CheckMenuItem miNothing;

    public SetGroupingMenuItem(final FilteredLogicalGraph graph) {
        super("Group By", null);

        this.graph = graph;
        this.cache = new HashMap<>();

        this.miNothing = new CheckMenuItem("(None)");
        this.miNothing.setOnAction(event -> {
            SetGroupingMenuItem.this.graph.groupingProperty().set(null);
        });
    }

    @Override
    public Collection<MenuItem> getDynamicItems() {
        final ArrayList<MenuItem> items = new ArrayList<>();
        items.add(miNothing);
        this.miNothing.setSelected(this.graph.groupingProperty().get() == null);

        items.addAll(this.graph.getGroupings().stream().map(title -> {
            final MenuItem existing = SetGroupingMenuItem.this.cache.get(title);
            if(existing != null) {
                ((CheckMenuItem)existing).setSelected(title.equals(graph.groupingProperty().get()));
                return existing;
            } else {
                final CheckMenuItem ck = new CheckMenuItem(title);
                ck.setOnAction(event -> {
                    SetGroupingMenuItem.this.graph.groupingProperty().set(title);
                });
                ck.setSelected(title.equals(graph.groupingProperty().get()));
                SetGroupingMenuItem.this.cache.put(title, ck);
                return ck;
            }
        }).collect(Collectors.toList()));
        return items;
    }
}
