package grassmarlin.ui.common.controls;

import com.sun.istack.internal.NotNull;
import javafx.application.Platform;
import javafx.beans.InvalidationListener;
import javafx.beans.Observable;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.control.ComboBox;

public class UnselectingComboBox<T> extends ComboBox<T> {

    private InvalidationListener itemListListener;
    private InvalidationListener nullSelectionListener;
    private ObservableList<T> items;
    private T nullObject;

    public UnselectingComboBox(@NotNull T nullObject) {
        super();
        if (nullObject == null) {
            throw new IllegalArgumentException("Argument to Constructor can not be null");
        }
        this.itemListListener = this::handleItemListInvalidation;
        this.nullSelectionListener = this::handleNullSelection;
        this.nullObject = nullObject;

        this.getItems().addListener(this.itemListListener);
        this.itemsProperty().addListener(this.itemListListener);
        this.getSelectionModel().selectedItemProperty().addListener(this.nullSelectionListener);
    }

    /**
     * It would be a lot simpler if I could have just used null to represent a null selection
     * but JavaFX throws an exception if null is selected in a combo box, so the null Object
     * can not and must not in actuality be null.
     *
     * @param list The list of selectable values
     * @param nullObject The object that represents a selection of null (i.e. clear selection)
     */
    public UnselectingComboBox(@NotNull ObservableList<T> list, @NotNull T nullObject) {
        this(nullObject);
        this.setItems(list);
    }

    private void handleItemListInvalidation(Observable observable) {
        this.getItems().removeListener(this.itemListListener);
        this.itemsProperty().removeListener(this.itemListListener);
        this.items = FXCollections.observableArrayList(this.getItems());
        this.setItems(this.items);
        if (!this.getItems().contains(nullObject)) {
            this.getItems().add(nullObject);
        }
        this.getItems().addListener(this.itemListListener);
        this.itemsProperty().addListener(this.itemListListener);
    }

    private void handleNullSelection(Observable observable) {
        if (this.getSelectionModel().getSelectedItem() == nullObject) {
            Platform.runLater(() -> this.getSelectionModel().clearSelection());
        }
    }
}
