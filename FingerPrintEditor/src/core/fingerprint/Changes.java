package core.fingerprint;

import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;

/**
 * 08.05.2015 - CC - New...
 */
public class Changes extends Dialog<String> {
    private static final String CHANGE_LOG = "" +
            "Fingerprint Editor Version 1.2(alpha)\n" +
            "-Load and save now go back to the last place you loaded or saved from\n" +
            "-Added header text for transport protocol indicating users should choose 6 or 17\n" +
            "-Payload items will now populate their edit dialogs with current values\n" +
            "-Added ability to copy Filter Groups by right clicking\n" +
            "-Added ability to edit filters by right clicking\n" +
            "-Added tooltip text to filter groups and filters explaining functionality\n" +
            "-Limited filter groups so they will not accept 2 filters of the same type\n" +
            "-Added this changes dialog\n" +
            "\n" +
            "Fingerprint Editor Version 1.1(alpha)\n" +
            "-New payload tabs start out with a filter group already present\n" +
            "-Added close functionality to file menu drop down choice\n" +
            "-Added new check for save feature which will prompt the user to save their filter before closing if changes have been made since the last save\n" +
            "-Made Author a mandatory field\n" +
            "-Added a description pop out for better editing of lengthy description text\n" +
            "-Added more tool tip text\n" +
            "\n" +
            "Fingerprint Editor Version 1.0(alpha)\n" +
            "-Initial deployment for testing\n" +
            "-Added splash image and icon images";

    public Changes() {
        super();
        setTitle("Change Log");
        setHeaderText("Recent changes");

        Label label = new Label("Changes: ");
        TextArea textArea = new TextArea(CHANGE_LOG);
        textArea.setEditable(false);
        textArea.setWrapText(true);
        textArea.setMaxWidth(Double.MAX_VALUE);
        textArea.setMaxHeight(Double.MAX_VALUE);
        GridPane.setVgrow(textArea, Priority.ALWAYS);
        GridPane.setHgrow(textArea, Priority.ALWAYS);

        GridPane expContent = new GridPane();
        expContent.setMaxWidth(Double.MAX_VALUE);
        expContent.add(label, 0, 0);
        expContent.add(textArea,0,1);
        getDialogPane().setContent(expContent);

        getDialogPane().getButtonTypes().addAll(ButtonType.CLOSE);
    }

}
