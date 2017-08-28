package grassmarlin.ui.common.controls;

import com.sun.javafx.collections.ObservableListWrapper;
import com.sun.javafx.scene.control.skin.ComboBoxListViewSkin;
import javafx.application.Platform;
import javafx.beans.Observable;
import javafx.beans.value.ChangeListener;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.event.EventHandler;
import javafx.geometry.Point2D;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.util.StringConverter;

import java.util.ArrayList;
import java.util.List;

public class FilteredComboBox<T> extends ComboBox<T> {
    private ObservableList<T> baseList;
    private FilteredList<T> filteredList;

    private final ChangeListener<String> typingListener = this::handleTyping;
    private final EventHandler<KeyEvent> navigationListener = this::handleNavigation;
    private final EventHandler<MouseEvent> mousesListener = this::handleMouse;

    private final boolean caseSensitive = false;

    public FilteredComboBox() {
        super();

        this.setBaseList(new ArrayList<>());

        this.setEditable(true);

        final ComboBoxListViewSkin<T> skin = new ComboBoxListViewSkin<>(this);
        this.setSkin(skin);
        final ListView<T> listView = skin.getListView();

        listView.setOnKeyPressed(navigationListener);
        listView.setOnMouseClicked(mousesListener);
        listView.setOnMousePressed(mousesListener);
    }

    private void handleNavigation(KeyEvent event) {
        TextField editor = this.getEditor();
        switch (event.getCode()) {
            case BACK_SPACE:
                // delete the selection, then delete the character immediately before the selection
                // this essentially treats the selection as if it's not actually there
                editor.textProperty().removeListener(typingListener);
                int caret = editor.getCaretPosition();
                editor.deleteText(editor.getSelection());
                editor.deselect();
                editor.positionCaret(caret);
                editor.textProperty().addListener(typingListener);
                editor.deletePreviousChar();

                event.consume();

                break;
            case UP:
                // update selection using up and down while maintaining the typed text so far
                if (this.getSelectionModel().getSelectedIndex() > 0) {
                    caret = editor.getCaretPosition();
                    editor.textProperty().removeListener(typingListener);
                    this.getSelectionModel().selectPrevious();
                    Platform.runLater(() -> {
                        editor.selectRange(editor.textProperty().length().get(), caret);

                        editor.textProperty().addListener(typingListener);
                    });
                }
                event.consume();
                break;
            case DOWN:
                if (this.getSelectionModel().getSelectedIndex() < this.itemsProperty().get().size()) {
                    caret = editor.getCaretPosition();
                    editor.textProperty().removeListener(typingListener);
                    this.getSelectionModel().selectNext();
                    Platform.runLater(() -> {
                        editor.selectRange(editor.textProperty().length().get(), caret);

                        editor.textProperty().addListener(typingListener);
                    });
                }

                event.consume();
                break;
        }
    }

    private void handleTyping(Observable observable, String oldValue, String newValue) {
        if (newValue != null) {
            // only update if new and old are different (which since we're in the change handler should be always)
            if (!(caseSensitive ? newValue.equals(oldValue) : newValue.equalsIgnoreCase(oldValue))) {
                // don't want recursive calls to the change handler because we're going to be changing the text field contents
                this.getEditor().textProperty().removeListener(typingListener);
                if (newValue.equals("")) {
                    // if the text field is now empty than reset everything
                    filteredList.setPredicate(null);
                    this.hide();
                    this.show();
                    this.getEditor().deselect();
                    Platform.runLater(() -> this.getSelectionModel().clearSelection());
                    Platform.runLater(() -> {
                        this.getEditor().textProperty().removeListener(typingListener);
                        this.getEditor().setText(newValue);
                        this.getEditor().textProperty().addListener(typingListener);
                    });
                } else if (baseList.stream().anyMatch(name -> this.getConverter().toString(name).equalsIgnoreCase(newValue))) {
                    // this is a check for if a value was selected by clicking. If using clicking leave whole list available
                    if (caseSensitive) {
                        if (baseList.stream().noneMatch(name-> this.getConverter().toString(name).equals(newValue))) {
                            return;
                        }
                    } else {
                        Platform.runLater(() -> {
                            this.getEditor().selectRange(this.getEditor().getText().length(), this.getEditor().getText().length());
                        });
                    }
                } else if (baseList.stream().anyMatch(name -> this.getConverter().toString(name).regionMatches(!caseSensitive, 0, newValue, 0, newValue.length()))) {
                    // This is the filtering magic, filter the list based on what has been typed so far
                    filteredList.setPredicate(name -> this.getConverter().toString(name).regionMatches(!caseSensitive, 0, newValue, 0, newValue.length()));
                    this.hide();
                    this.show();
                    // select the first thing in the filtered list as a guess
                    this.getSelectionModel().selectFirst();
                    Platform.runLater(() -> {
                        // select the text in the text box that has not been entered by the user
                        this.getEditor().textProperty().removeListener(typingListener);
                        this.getEditor().setText(this.getConverter().toString(this.getSelectionModel().getSelectedItem()));
                        this.getEditor().selectRange(this.getEditor().getText().length(), newValue.length());
                        this.getEditor().textProperty().addListener(typingListener);
                    });
                } else {
                    // no matches to typed text are in the current list, show the entire list and don't highlight any text
                    this.getEditor().deselect();
                    Platform.runLater(() -> {
                        this.getSelectionModel().clearSelection();

                        Platform.runLater(() -> {
                            this.getEditor().textProperty().removeListener(typingListener);
                            this.getEditor().setText(newValue);
                            this.getEditor().positionCaret(newValue.length());
                            this.getEditor().textProperty().addListener(typingListener);
                        });
                    });
                }
                this.getEditor().textProperty().addListener(typingListener);
            }
        }
    }

    private void handleMouse(MouseEvent event) {
        if (this.localToScreen(this.getBoundsInLocal()).contains(new Point2D(event.getScreenX(), event.getScreenY()))) {
            System.out.println("Clicked");

            event.consume();
        }
    }

    public void setItems(List<T> items) {
        this.getEditor().textProperty().removeListener(typingListener);
        this.baseList.clear();
        this.baseList.addAll(items);

        StringConverter<T> converter = this.getConverter();

        if (caseSensitive) {
            this.baseList.sort((item, other) -> converter.toString(item).compareTo(converter.toString(other)));
        } else {
            this.baseList.sort((item, other) -> converter.toString(item).compareToIgnoreCase(converter.toString(other)));
        }

        this.filteredList = new FilteredList<>(new ObservableListWrapper<>(new ArrayList(baseList)));

        this.setItems(this.filteredList);

        Platform.runLater(() -> this.getEditor().textProperty().addListener(typingListener));
    }

    public void setBaseList(final ObservableList<T> list) {
        this.baseList = list;
        this.filteredList = new FilteredList<>(this.baseList);

        this.getEditor().textProperty().removeListener(typingListener);
        this.getSelectionModel().clearSelection();
        this.setItems(this.filteredList);
        this.getEditor().textProperty().addListener(typingListener);
    }
    public void setBaseList(final List<T> list) {
        this.setBaseList(new ObservableListWrapper<>(list));
    }
    public List<T> getAllItems() {
        return this.baseList;
    }
    public List<T> getFilteredItems() {
        return new ArrayList<>(filteredList);
    }
}
