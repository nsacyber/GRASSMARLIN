package grassmarlin.ui.common.controls;

import grassmarlin.ui.common.dialogs.palette.PaletteDialog;
import javafx.beans.binding.When;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.util.Callback;

public class ColorFieldTableCell<S> extends TableCell<S, Color> {
    public static <S> Callback<TableColumn<S, Color>, TableCell<S, Color>> forTableColumn(final Color initialColor) {
        return list -> new ColorFieldTableCell<>(initialColor);
    }

    private final static PaletteDialog dlgPalette = new PaletteDialog();
    private final Rectangle rectSample;

    protected ColorFieldTableCell(final Color colorDefault) {
        this.rectSample = new Rectangle(14.0, 14.0);
        this.rectSample.setStroke(Color.BLACK);
        this.rectSample.setStrokeWidth(1.0);
        this.rectSample.fillProperty().bind(new When(this.itemProperty().isNull()).then(Color.BLACK).otherwise(itemProperty()));
    }

    /** {@inheritDoc} */
    @Override
    public void startEdit() {
        if (! isEditable()
                || ! getTableView().isEditable()
                || ! getTableColumn().isEditable()) {
            return;
        }
        super.startEdit();

        if (isEditing()) {
            dlgPalette.initialColorProperty().set(getItem() == null ? Color.BLACK : getItem());
            if(dlgPalette.showAndWait().orElse(null) == ButtonType.OK) {
                commitEdit(dlgPalette.selectedColorProperty().get());
            }
            cancelEdit();
        }
    }

    /** {@inheritDoc} */
    @Override
    public void updateItem(Color item, boolean empty) {
        super.updateItem(item, empty);
        if (this.isEmpty()) {
            this.setText(null);
            this.setGraphic(null);
        } else {
            this.setGraphic(this.rectSample);
            this.setText(null);
        }
    }
}
