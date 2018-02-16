package grassmarlin.ui.common.menu;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.Node;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;

public class EnumSelectionMenuItem<TEnum extends Enum<TEnum>> extends Menu {
    private final SimpleObjectProperty<TEnum> selectedValue;

    public EnumSelectionMenuItem(final String prompt, final TEnum initialValue) {
        this(prompt, null, initialValue);
    }
    public EnumSelectionMenuItem(final String prompt, final Node graphic, final TEnum initialValue) {
        super(prompt, graphic);

        selectedValue = new SimpleObjectProperty<>(initialValue);

        for(final TEnum value : initialValue.getDeclaringClass().getEnumConstants()) {
            final CheckMenuItem miValue = new CheckMenuItem(value.toString());
            miValue.setOnAction(event -> selectedValue.set(value));
            this.getItems().add(miValue);
        }

        this.setOnShowing(event -> {
            for(final MenuItem child : this.getItems()) {
                if(child instanceof CheckMenuItem) {
                    ((CheckMenuItem)child).setSelected(child.getText().equals(selectedValue.get().toString()));
                }
            }
        });
    }

    public ObjectProperty<TEnum> selectedValueProperty() {
        return this.selectedValue;
    }
}
