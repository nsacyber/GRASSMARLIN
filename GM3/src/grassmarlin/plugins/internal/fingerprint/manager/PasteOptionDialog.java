package grassmarlin.plugins.internal.fingerprint.manager;

import grassmarlin.RuntimeConfiguration;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.BorderPane;
import javafx.scene.text.Text;
import javafx.scene.text.TextAlignment;

import java.util.Optional;

public class PasteOptionDialog extends Dialog<ButtonType> {

    public static final ButtonType RENAME = new ButtonType("_Rename", ButtonBar.ButtonData.OK_DONE);
    public static final ButtonType OVERWRITE = new ButtonType("_Overwrite", ButtonBar.ButtonData.OK_DONE);
    public static final ButtonType CANCEL = ButtonType.CANCEL;

    private Text message;

    public PasteOptionDialog() {
        super();

        RuntimeConfiguration.setIcons(this);
        this.setTitle("Paste");

        BorderPane content = new BorderPane();
        this.message =  new Text();
        message.setTextAlignment(TextAlignment.CENTER);
        content.setCenter(message);
        this.getDialogPane().setContent(content);

        this.getDialogPane().getButtonTypes().addAll(RENAME, OVERWRITE, CANCEL);
        ((Button)this.getDialogPane().lookupButton(OVERWRITE)).setDefaultButton(false);
        ((Button)this.getDialogPane().lookupButton(RENAME)).setDefaultButton(false);

        this.getDialogPane().getButtonTypes().stream()
                .map(this.getDialogPane()::lookupButton)
                .forEach(button -> button.addEventHandler(KeyEvent.KEY_PRESSED, event -> {
                    if (KeyCode.ENTER.equals(event.getCode()) && event.getTarget() instanceof Button) {
                        ((Button) event.getTarget()).fire();
                    }
                }));
    }

    public Optional<ButtonType> showAndWait(String nameAlreadyExists) {
        this.message.setText(nameAlreadyExists + " already exists.");

        return this.showAndWait();
    }
}
