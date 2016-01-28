package core.fingerprint;

import com.sun.javafx.scene.control.skin.resources.ControlResources;
import javafx.application.Platform;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.util.Pair;

/**
 * Created by CC on 7/13/2015.
 */
public class PairTextInputDialog extends Dialog<Pair<String,String>> {
    private final GridPane grid;
    private final Label label;
    //private final TextField textField;
    private final Pair<TextField, TextField> textFields;
    private final Pair<String, String> defaultValues;

    public PairTextInputDialog() {
        this(new Pair<>("",""));
    }

    public PairTextInputDialog(Pair<String, String> defaultValues) {
        final DialogPane dialogPane = getDialogPane();
        textFields = new Pair<>(new TextField(defaultValues.getKey()),new TextField(defaultValues.getValue()));
        textFields.getKey().setMaxWidth(Double.MAX_VALUE);
        textFields.getValue().setMaxWidth(Double.MAX_VALUE);
        // -- textfield
//        this.textField = new TextField(defaultValue);
//        this.textField.setMaxWidth(Double.MAX_VALUE);
        GridPane.setHgrow(textFields.getKey(), Priority.ALWAYS);
        GridPane.setFillWidth(textFields.getKey(), true);
        GridPane.setHgrow(textFields.getValue(), Priority.ALWAYS);
        GridPane.setFillWidth(textFields.getValue(), true);

        // -- label
        label = createContentLabel(dialogPane.getContentText());
        label.setPrefWidth(Region.USE_COMPUTED_SIZE);
        label.textProperty().bind(dialogPane.contentTextProperty());

        this.defaultValues = defaultValues;

        this.grid = new GridPane();
        this.grid.setHgap(10);
        this.grid.setMaxWidth(Double.MAX_VALUE);
        this.grid.setAlignment(Pos.CENTER_LEFT);

        dialogPane.contentTextProperty().addListener(o -> updateGrid());

        setTitle(ControlResources.getString("Dialog.confirm.title"));
        dialogPane.setHeaderText(ControlResources.getString("Dialog.confirm.header"));
        dialogPane.getStyleClass().add("text-input-dialog");
        dialogPane.getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        updateGrid();

        setResultConverter((dialogButton) -> {
            ButtonBar.ButtonData data = dialogButton == null ? null : dialogButton.getButtonData();
            return data == ButtonBar.ButtonData.OK_DONE ? new Pair<>(textFields.getKey().getText(),textFields.getValue().getText()) : null;
        });
    }

    public final Pair<TextField, TextField> getEditors() {
        return this.textFields;
    }

    /**
     * Creates a Label node that works well within a Dialog.
     * @param text The text to display
     */
    static Label createContentLabel(String text) {
        Label label = new Label(text);
        label.setMaxWidth(Double.MAX_VALUE);
        label.setMaxHeight(Double.MAX_VALUE);
        label.getStyleClass().add("content");
        label.setWrapText(true);
        label.setPrefWidth(360);
        return label;
    }

    private void updateGrid() {
        grid.getChildren().clear();

        grid.add(label, 0, 0);
        grid.add(textFields.getKey(), 1, 0);
        grid.add(textFields.getValue(), 2, 0);
        getDialogPane().setContent(grid);

        Platform.runLater(() -> textFields.getKey().requestFocus());
    }
}
