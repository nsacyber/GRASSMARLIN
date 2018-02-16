package grassmarlin.plugins.internal.logicalview;

import grassmarlin.plugins.internal.logicalview.visual.colorfactories.HashColorFactory;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.control.Menu;
import javafx.scene.control.RadioMenuItem;
import javafx.scene.control.Toggle;
import javafx.scene.control.ToggleGroup;

import java.util.Map;
import java.util.function.Supplier;

public class ColorFactoryMenuItem extends Menu {
    private final ObjectProperty<ILogicalViewApi.ICalculateColorsForAggregate> factory;
    private final ToggleGroup group;

    public ColorFactoryMenuItem(final Plugin plugin) {
        super("Set Group Color Theme");

        this.factory = new SimpleObjectProperty<>(null);
        this.group = new ToggleGroup();

        for(Map.Entry<String, Supplier<ILogicalViewApi.ICalculateColorsForAggregate>> entry : plugin.getColorFactoryFactories().entrySet()) {
            final ILogicalViewApi.ICalculateColorsForAggregate factory = entry.getValue().get();
            final RadioMenuItem mi = new RadioMenuItem(entry.getKey());
            mi.setToggleGroup(group);
            mi.setOnAction(event -> {
                ColorFactoryMenuItem.this.factory.set(factory);
            });

            this.getItems().add(mi);

            if(factory instanceof HashColorFactory) {
                this.factory.set(factory);
                group.selectToggle(mi);
            }
        }

        this.getItems().sort((o1, o2) -> o1.getText().compareTo(o2.getText()));
    }

    public ILogicalViewApi.ICalculateColorsForAggregate getFactory() {
        return this.factory.get();
    }
    public String getFactoryName() {
        return ((RadioMenuItem)this.group.getSelectedToggle()).getText();
    }
    public void setFactoryByName(final String name) {
        final Toggle factory = this.group.getToggles().stream().filter(toggle -> toggle instanceof RadioMenuItem).filter(toggle -> ((RadioMenuItem)toggle).getText().equals(name)).findAny().orElse(null);
        if(factory != null) {
            this.group.selectToggle(factory);
        }

    }
}
