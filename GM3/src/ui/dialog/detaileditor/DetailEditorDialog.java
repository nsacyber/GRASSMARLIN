/*
 *  Copyright (C) 2011, 2012
 *  This file is part of GRASSMARLIN.
 */
package ui.dialog.detaileditor;

import java.awt.Dimension;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.layout.GridPane;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import ui.views.tree.visualnode.VisualNode;

/**
 *
 * @author BESTDOG
 * Swing Wrapper class for this JavaFX dialog.
 */
public class DetailEditorDialog extends JFrame {
    
    public DetailEditorDialog(VisualNode node, Consumer<VisualNode> cb) {
        JFXPanel panel = new JFXPanel();
        Platform.runLater(()->{
            try {
                add(panel);
                URL url = DetailEditorDialog.class.getResource("DetailEditor.fxml");
                FXMLLoader loader = new FXMLLoader( url );
                loader.setRoot(new GridPane());
                Parent root = loader.load();
                DetailEditor controller = loader.getController();
                panel.setScene(new Scene(root, 480, 328));
                controller.setNode(node);
                controller.setOnChangeCallback(cb);
                SwingUtilities.invokeLater(()->{
                    setMinimumSize(new Dimension(480, 328));
                    setTitle(node.getName());
                    setIconImage(node.getDetails().image.get());
                    pack();
                    setVisible(true);
                });
            } catch (IOException ex) {
                Logger.getLogger(DetailEditorDialog.class.getName()).log(Level.SEVERE, null, ex);
                System.exit(0);
            }
        });
    }
    
}
