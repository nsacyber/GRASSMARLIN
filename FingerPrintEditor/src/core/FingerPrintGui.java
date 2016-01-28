package core;

import core.fingerprint.FpPanel;
import javafx.application.Application;
import javafx.embed.swing.JFXPanel;
import javafx.embed.swing.SwingNode;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

import javax.swing.*;

public class FingerPrintGui extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception{
        final SwingNode swingComponents = new SwingNode();
        createAndSetSwingContent(swingComponents);
        StackPane root = new StackPane();
        root.getChildren().add(swingComponents);
        primaryStage.setTitle("FingerPrint Creator");
        primaryStage.setScene(new Scene(root, 800, 600));
        primaryStage.show();
    }

    private void createAndSetSwingContent(final SwingNode swingNode) {
        SwingUtilities.invokeLater(() -> {
            FpPanel fpPanel = new FpPanel(null);
            swingNode.setContent(fpPanel);
        });

    }


    public static void main(String[] args) {
        launch(args);
    }
}
