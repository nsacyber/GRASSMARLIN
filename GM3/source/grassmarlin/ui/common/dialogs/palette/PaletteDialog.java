package grassmarlin.ui.common.dialogs.palette;

import grassmarlin.RuntimeConfiguration;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.layout.GridPane;
import javafx.scene.paint.Color;

public class PaletteDialog extends Dialog<ButtonType> {
    protected final SimpleObjectProperty<Color> colorInitial;
    protected final SimpleObjectProperty<Color> colorSelected;

    public PaletteDialog() {
        colorInitial = new SimpleObjectProperty<Color>(Color.BLACK) {
            @Override
            public void set(final Color newValue) {
                /* Hack: We need the change event to fire even if the color isn't changed.
                 *   Since all the better ways to make this happen don't work due to private
                 *   and/or final implementations, we're stuck setting the color to a
                 *   known-different value before setting the new value.  Null doesn't work
                 *   since the sliders need to extract the RGB values.
                 */
                super.set(newValue.invert());
                super.set(newValue);
            }
        };
        colorSelected = new SimpleObjectProperty<>(Color.BLACK);

        initComponents();
    }

    private void initComponents() {
        colorInitial.addListener(this::Handle_ColorChanged);

        this.setTitle("Select New Color...");
        RuntimeConfiguration.setIcons(this);

        final ColorSelections colorsDefault = new ColorSelections(6, 8,
                Color.rgb(255, 128, 128),
                Color.rgb(255, 255, 128),
                Color.rgb(128, 255, 128),
                Color.rgb(0, 255, 128),
                Color.rgb(128, 255, 255),
                Color.rgb(0, 128, 255),
                Color.rgb(255, 128, 192),
                Color.rgb(255, 128, 255),

                Color.rgb(255, 0, 0),
                Color.rgb(255, 255, 0),
                Color.rgb(128, 255, 0),
                Color.rgb(0, 255, 64),
                Color.rgb(0, 255, 255),
                Color.rgb(0, 128, 192),
                Color.rgb(128, 128, 192),
                Color.rgb(255, 0, 255),

                Color.rgb(128, 64, 64),
                Color.rgb(255, 128, 64),
                Color.rgb(0, 255, 0),
                Color.rgb(0, 128, 128),
                Color.rgb(0, 64, 128),
                Color.rgb(128, 128, 255),
                Color.rgb(128, 0, 64),
                Color.rgb(255, 0, 128),

                Color.rgb(128, 0, 0),
                Color.rgb(255, 128, 0),
                Color.rgb(0, 128, 0),
                Color.rgb(0, 128, 64),
                Color.rgb(0, 0, 255),
                Color.rgb(0, 0, 160),
                Color.rgb(128, 0, 128),
                Color.rgb(128, 0, 255),

                Color.rgb(64, 0, 0),
                Color.rgb(128, 64, 0),
                Color.rgb(0, 64, 0),
                Color.rgb(0, 64, 64),
                Color.rgb(0, 0, 128),
                Color.rgb(0, 0, 64),
                Color.rgb(64, 0, 64),
                Color.rgb(64, 0, 128),

                Color.rgb(0, 0, 0),
                Color.rgb(128, 128, 0),
                Color.rgb(128, 128, 64),
                Color.rgb(128, 128, 128),
                Color.rgb(64, 128, 128),
                Color.rgb(192, 192, 192),
                Color.rgb(64, 0, 32),
                Color.rgb(255, 255, 255)
                );
        colorsDefault.selectedColorProperty().bindBidirectional(colorSelected);

        final RgbSliders rgbSliders = new RgbSliders(colorSelected);
        final PrePostColors colorPreview = new PrePostColors(colorInitial, colorSelected);

        final GridPane layout = new GridPane();
        layout.add(colorsDefault, 0, 0);
        layout.add(rgbSliders, 1, 0);
        layout.add(colorPreview, 1, 1);

        getDialogPane().setContent(layout);
        getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
    }

    protected void Handle_ColorChanged(final ObservableValue<? extends Color> observable, final Color oldValue, final Color newValue) {
        colorSelected.set(newValue);
    }

    public ObjectProperty<Color> initialColorProperty() {
        return colorInitial;
    }
    public ObjectProperty<Color> selectedColorProperty() {
        return colorSelected;
    }
}
