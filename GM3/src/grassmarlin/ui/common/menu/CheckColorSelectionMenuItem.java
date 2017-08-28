package grassmarlin.ui.common.menu;

import grassmarlin.ui.common.dialogs.palette.PaletteDialog;
import javafx.beans.property.ObjectProperty;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;

import java.util.Optional;

public class CheckColorSelectionMenuItem extends CheckMenuItem {
    protected static PaletteDialog dlgPalette;

    private static class ColorPreview extends Rectangle
    {
        public ColorPreview(final ObjectProperty<Color> fillColor) {
            super(14.0, 14.0);

            setStrokeWidth(1.0);
            setStroke(Color.BLACK);
            fillProperty().bind(fillColor);
        }
    }

    public CheckColorSelectionMenuItem(final ObjectProperty<Color> color, final String text) {
        this(color, text, null);
    }

    public CheckColorSelectionMenuItem(final ObjectProperty<Color> color, final String text, final Runnable fnOnChange) {
        super(text, new CheckColorSelectionMenuItem.ColorPreview(color));

        super.setOnAction(event -> {
            //We defer the creation of the dialog until we know we're in the UI Thread.
            if(dlgPalette == null) {
                dlgPalette = new PaletteDialog();
            }

            dlgPalette.initialColorProperty().set(color.get());
            final Optional<ButtonType> result = dlgPalette.showAndWait();
            if(result.isPresent() && result.get() == ButtonType.OK) {
                color.set(dlgPalette.selectedColorProperty().get());
                if(fnOnChange != null) {
                    fnOnChange.run();
                }
            } else {
                //Canceled--Do nothing
            }
        });
    }
}
