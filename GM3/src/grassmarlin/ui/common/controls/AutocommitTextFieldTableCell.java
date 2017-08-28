package grassmarlin.ui.common.controls;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.event.Event;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.util.Callback;
import javafx.util.StringConverter;
import javafx.util.converter.DefaultStringConverter;

public class AutocommitTextFieldTableCell<S, T> extends TableCell<S, T> {

    /***************************************************************************
     *                                                                         *
     * Static cell factories                                                   *
     *                                                                         *
     **************************************************************************/

    public static <S> Callback<TableColumn<S,String>, TableCell<S,String>> forTableColumn() {
        return forTableColumn(new DefaultStringConverter());
    }

    public static <S,T> Callback<TableColumn<S,T>, TableCell<S,T>> forTableColumn(
            final StringConverter<T> converter) {
        return list -> new AutocommitTextFieldTableCell<S,T>(converter);
    }


    /***************************************************************************
     *                                                                         *
     * Fields                                                                  *
     *                                                                         *
     **************************************************************************/

    private TextField textField;



    /***************************************************************************
     *                                                                         *
     * Constructors                                                            *
     *                                                                         *
     **************************************************************************/

    /**
     * Creates a default TextFieldTableCell with a null converter. Without a
     * {@link StringConverter} specified, this cell will not be able to accept
     * input from the TextField (as it will not know how to convert this back
     * to the domain object). It is therefore strongly encouraged to not use
     * this constructor unless you intend to set the converter separately.
     */
    public AutocommitTextFieldTableCell() {
        this(null);
    }

    /**
     * Creates a TextFieldTableCell that provides a {@link TextField} when put
     * into editing mode that allows editing of the cell content. This method
     * will work on any TableColumn instance, regardless of its generic type.
     * However, to enable this, a {@link StringConverter} must be provided that
     * will convert the given String (from what the user typed in) into an
     * instance of type T. This item will then be passed along to the
     * {@link TableColumn#onEditCommitProperty()} callback.
     *
     * @param converter A {@link StringConverter converter} that can convert
     *      the given String (from what the user typed in) into an instance of
     *      type T.
     */
    public AutocommitTextFieldTableCell(StringConverter<T> converter) {
        this.getStyleClass().add("text-field-table-cell");
        setConverter(converter);
    }



    /***************************************************************************
     *                                                                         *
     * Properties                                                              *
     *                                                                         *
     **************************************************************************/

    // --- converter
    private ObjectProperty<StringConverter<T>> converter =
            new SimpleObjectProperty<StringConverter<T>>(this, "converter");

    /**
     * The {@link StringConverter} property.
     */
    public final ObjectProperty<StringConverter<T>> converterProperty() {
        return converter;
    }

    /**
     * Sets the {@link StringConverter} to be used in this cell.
     */
    public final void setConverter(StringConverter<T> value) {
        converterProperty().set(value);
    }

    /**
     * Returns the {@link StringConverter} used in this cell.
     */
    public final StringConverter<T> getConverter() {
        return converterProperty().get();
    }



    /***************************************************************************
     *                                                                         *
     * Public API                                                              *
     *                                                                         *
     **************************************************************************/

    /** {@inheritDoc} */
    @Override public void startEdit() {
        if (! isEditable()
                || ! getTableView().isEditable()
                || ! getTableColumn().isEditable()) {
            return;
        }
        super.startEdit();

        if (isEditing()) {
            if (this.textField == null) {
                //textField = createTextField(this, getConverter());
                this.textField = new TextField(getItemText(this, this.getConverter()));

                // Use onAction here rather than onKeyReleased (with check for Enter),
                // as otherwise we encounter RT-34685
                textField.setOnAction(event -> {
                    if (converter == null) {
                        throw new IllegalStateException(
                                "Attempting to convert text input into Object, but provided "
                                        + "StringConverter is null. Be sure to set a StringConverter "
                                        + "in your cell factory.");
                    }
                    AutocommitTextFieldTableCell.this.commitEdit(this.getConverter().fromString(AutocommitTextFieldTableCell.this.textField.getText()));
                    event.consume();
                });
                textField.setOnKeyReleased(t -> {
                    if (t.getCode() == KeyCode.ESCAPE) {
                        AutocommitTextFieldTableCell.this.cancelEdit();
                        t.consume();
                    }
                });
                textField.focusedProperty().addListener((observable, oldValue, newValue) -> {
                    //Focus is lost after editing stops, so we can't use commitEdit.
                    if(!newValue) {
                        //This is the relevant part of the commitEdit code.
                        final TableView<S> table = getTableView();
                        if (table != null) {
                            // Inform the TableView of the edit being ready to be committed.
                            TableColumn.CellEditEvent<S, T> editEvent = new TableColumn.CellEditEvent<>(
                                    table,
                                    new TablePosition<>(table, AutocommitTextFieldTableCell.this.getTableRow().getIndex(), AutocommitTextFieldTableCell.this.getTableColumn()),
                                    TableColumn.editCommitEvent(),
                                    getItemValue(AutocommitTextFieldTableCell.this.textField.getText(), AutocommitTextFieldTableCell.this.getConverter())
                            );

                            Event.fireEvent(getTableColumn(), editEvent);
                        }
                    }
                });
            }

            if (textField != null) {
                textField.setText(getItemText(this, this.getConverter()));
            }
            this.setText(null);
            this.setGraphic(textField);

            textField.selectAll();

            // requesting focus so that key input can immediately go into the
            // TextField (see RT-28132)
            textField.requestFocus();
        }
    }

    /** {@inheritDoc} */
    @Override public void cancelEdit() {
        super.cancelEdit();

        this.setText(getItemText(this, this.getConverter()));
        this.setGraphic(null);
    }

    /** {@inheritDoc} */
    @Override public void updateItem(T item, boolean empty) {
        super.updateItem(item, empty);

        if (this.isEmpty()) {
            this.setText(null);
            this.setGraphic(null);
        } else {
            if (this.isEditing()) {
                if (textField != null) {
                    textField.setText(getItemText(this, getConverter()));
                }
                this.setText(null);
                this.setGraphic(textField);
            } else {
                this.setText(getItemText(this, getConverter()));
                this.setGraphic(null);
            }
        }
    }

    protected static <T> String getItemText(Cell<T> cell, StringConverter<T> converter) {
        return converter == null ?
                cell.getItem() == null ? "" : cell.getItem().toString() :
                converter.toString(cell.getItem());
    }
    protected static <T> T getItemValue(final String value, final StringConverter<T> converter) {
        return converter == null ? (T)value : converter.fromString(value);
    }
}
