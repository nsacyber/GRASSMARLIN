package grassmarlin.ui.common.controls;

import javafx.event.Event;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.util.Callback;

public class DoubleFieldTableCell<S> extends TableCell<S, Double> {
    private double min;
    private double max;

    /***************************************************************************
     *                                                                         *
     * Static cell factories                                                   *
     *                                                                         *
     **************************************************************************/

    /**
     * Provides a {@link TextField} that allows editing of the cell content when
     * the cell is double-clicked, or when
     * {@link TableView#edit(int, javafx.scene.control.TableColumn) } is called.
     *
     * @return A {@link Callback} that can be inserted into the
     *      {@link TableColumn#cellFactoryProperty() cell factory property} of a
     *      TableColumn, that enables textual editing of the content.
     */
    public static <S> Callback<TableColumn<S, Double>, TableCell<S, Double>> forTableColumn(final double min, final double max) {
        return list -> new DoubleFieldTableCell<>(min, max);
    }


    /***************************************************************************
     *                                                                         *
     * Fields                                                                  *
     *                                                                         *
     **************************************************************************/

    private DoubleField doubleField;



    /***************************************************************************
     *                                                                         *
     * Constructors                                                            *
     *                                                                         *
     **************************************************************************/
    public DoubleFieldTableCell(final double min, final double max) {
        this.min = min;
        this.max = max;
        this.getStyleClass().add("text-field-table-cell");
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
            if (doubleField == null) {
                doubleField = new DoubleField(this.min, this.max, getItem() == null ? this.min : getItem());

                doubleField.setOnAction(event -> {
                    // onAction fires before the text is parsed into the Value, so we have to parse it here to commit the new value, rather than the original value.
                    DoubleFieldTableCell.this.commitEdit(new Double(doubleField.textProperty().get()));
                    event.consume();
                });
                doubleField.focusedProperty().addListener((observable, oldValue, newValue) -> {
                    //Focus is lost after editing stops, so we can't use commitEdit.
                    if(!newValue) {
                        //This is the relevant part of the commitEdit code.
                        final TableView<S> table = getTableView();
                        if (table != null) {
                            // Inform the TableView of the edit being ready to be committed.
                            TableColumn.CellEditEvent<S, Double> editEvent = new TableColumn.CellEditEvent<>(
                                    table,
                                    new TablePosition<>(table, DoubleFieldTableCell.this.getTableRow().getIndex(), DoubleFieldTableCell.this.getTableColumn()),
                                    TableColumn.editCommitEvent(),
                                    new Double(doubleField.textProperty().get())
                            );

                            Event.fireEvent(getTableColumn(), editEvent);
                        }
                    }
                });
                doubleField.setOnKeyReleased(t -> {
                    if (t.getCode() == KeyCode.ESCAPE) {
                        DoubleFieldTableCell.this.cancelEdit();
                        t.consume();
                    }
                });
            }

            doubleField.setText(DoubleFieldTableCell.this.getItem().toString());

            DoubleFieldTableCell.this.setText(null);
            DoubleFieldTableCell.this.setGraphic(doubleField);

            doubleField.selectAll();
            doubleField.requestFocus();
        }
    }

    /** {@inheritDoc} */
    @Override
    public void cancelEdit() {
        super.cancelEdit();
        this.setText(this.getItem().toString());
        this.setGraphic(null);
    }

    /** {@inheritDoc} */
    @Override
    public void updateItem(Double item, boolean empty) {
        super.updateItem(item, empty);
        if (this.isEmpty()) {
            this.setText(null);
            this.setGraphic(null);
        } else {
            if (this.isEditing()) {
                this.setText(null);
                this.setGraphic(doubleField);
            } else {
                this.setText(item.toString());
                this.setGraphic(null);
            }
        }
    }
}
