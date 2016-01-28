package sample;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import java.util.logging.Level;
import java.util.logging.Logger;

public class Main extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception{
        FXMLLoader loader = new FXMLLoader(getClass().getResource("sample.fxml"));
        primaryStage.getIcons().add(new Image(getClass().getResourceAsStream("Grassmarlincircleicon.png")));
        primaryStage.getIcons().add(new Image(getClass().getResourceAsStream("Grassmarlincirclesmall.png")));
        Parent root = loader.load();
        Controller controller = loader.getController();
        controller.setStage(primaryStage);
        controller.setArgs(getParameters().getNamed());
        //Parent root = FXMLLoader.load(getClass().getResource("sample.fxml"));
        primaryStage.setTitle("FingerPrint Creator");
        primaryStage.setScene(new Scene(root));
        primaryStage.show();
    }
    public static void main(String[] args) {
        launch(args);
    }
}
