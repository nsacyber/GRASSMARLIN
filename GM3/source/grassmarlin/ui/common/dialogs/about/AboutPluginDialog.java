package grassmarlin.ui.common.dialogs.about;

import grassmarlin.Plugin;
import grassmarlin.RuntimeConfiguration;
import grassmarlin.plugins.IPlugin;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.text.Font;
import javafx.scene.text.Text;

import java.net.URLClassLoader;
import java.nio.file.Paths;
import java.time.Instant;

public class AboutPluginDialog extends Dialog<ButtonType> {
    private final SimpleObjectProperty<IPlugin> plugin;

    private final Text lblPluginName;
    private final Text lblPluginPath;
    private final Text lblPluginModified;
    private final ImageView imgPlugin;

    public AboutPluginDialog() {
        this.plugin = new SimpleObjectProperty<>();

        this.lblPluginName = new Text();
        this.lblPluginPath = new Text();
        this.lblPluginModified = new Text();
        this.imgPlugin = new ImageView();

        initComponents();
    }

    private void initComponents() {
        RuntimeConfiguration.setIcons(this);

        this.plugin.addListener(this.handler_pluginChanged);

        lblPluginName.setFont(Font.font(lblPluginName.getFont().getFamily(), lblPluginName.getFont().getSize() + 4.0));

        this.imgPlugin.setPreserveRatio(true);
        this.imgPlugin.setFitHeight(64.0);
        this.imgPlugin.setFitWidth(64.0);

        final GridPane layout = new GridPane();
        layout.setHgap(8.0);
        layout.setVgap(2.0);

        layout.add(this.imgPlugin, 0, 0, 1, 4);
        layout.add(this.lblPluginName, 1, 0, 2, 1);
        layout.add(new Label("Path: "), 1, 1);
        layout.add(this.lblPluginPath, 2, 1);
        layout.add(new Label("Modified: "), 1, 2);
        layout.add(this.lblPluginModified, 2, 2);

        this.getDialogPane().setContent(layout);
        this.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
    }

    private final ChangeListener<IPlugin> handler_pluginChanged = this::handle_PluginChanged;

    private void handle_PluginChanged(final ObservableValue<? extends IPlugin> observable, final IPlugin oldValue, final IPlugin newValue) {
        if(newValue == null) {
            this.lblPluginName.setText("");
            this.lblPluginPath.setText("");
            this.lblPluginModified.setText("");
            this.imgPlugin.setImage(null);
        } else {
            if(newValue instanceof IPlugin.HasVersionInfo) {
                lblPluginName.setText(newValue.getName() + " v" + ((IPlugin.HasVersionInfo) newValue).getVersion());
            } else {
                lblPluginName.setText(newValue.getName());
            }

            this.imgPlugin.setImage(newValue.getImageForSize(64));

            this.setTitle("About " + newValue.getName());
            try {
                if(newValue.getClass() == Plugin.class) {
                    lblPluginPath.setText("Internal Plugin");
                    lblPluginModified.setText("");
                } else {
                    lblPluginPath.setText(Paths.get(((URLClassLoader) newValue.getClass().getClassLoader()).getURLs()[0].toURI()).toFile().toString());
                    lblPluginModified.setText(Instant.ofEpochMilli(Paths.get(((URLClassLoader) newValue.getClass().getClassLoader()).getURLs()[0].toURI()).toFile().lastModified()).toString());
                }
            } catch(Exception ex) {
                //There are a lot of reasons why the try block could fail.  It doesn't really matter why it failed, everything uses the same fallback.
                lblPluginPath.setText(RuntimeConfiguration.pluginNameFor(newValue.getClass()));
            }
        }
    }

    public ObjectProperty<IPlugin> pluginProperty() {
        return this.plugin;
    }
}
