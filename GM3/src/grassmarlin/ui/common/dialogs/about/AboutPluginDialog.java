package grassmarlin.ui.common.dialogs.about;

import grassmarlin.RuntimeConfiguration;
import grassmarlin.plugins.IPlugin;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.layout.GridPane;
import javafx.scene.text.Font;
import javafx.scene.text.Text;

import java.net.URLClassLoader;

public class AboutPluginDialog extends Dialog<ButtonType> {
    private final RuntimeConfiguration config;
    private final SimpleObjectProperty<IPlugin> plugin;

    private final Text lblPluginName;
    private final Text lblPluginPath;

    public AboutPluginDialog(final RuntimeConfiguration config) {
        this.config = config;
        this.plugin = new SimpleObjectProperty<>();

        this.lblPluginName = new Text();
        this.lblPluginPath = new Text();

        initComponents();
    }

    private void initComponents() {
        RuntimeConfiguration.setIcons(this);

        this.plugin.addListener(this.handler_pluginChanged);

        lblPluginName.setFont(Font.font(lblPluginName.getFont().getFamily(), lblPluginName.getFont().getSize() + 4.0));

        final GridPane layout = new GridPane();

        //TODO: Add a plugin-specific icon at 0,0
        layout.add(this.lblPluginName, 1, 0);
        layout.add(this.lblPluginPath, 1, 1);

        this.getDialogPane().setContent(layout);
        this.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
    }

    private final ChangeListener<IPlugin> handler_pluginChanged = this::handle_PluginChanged;

    private void handle_PluginChanged(final ObservableValue<? extends IPlugin> observable, final IPlugin oldValue, final IPlugin newValue) {
        if(newValue == null) {
            lblPluginName.setText("");
            lblPluginPath.setText("");
        } else {
            if(newValue instanceof IPlugin.HasVersionInfo) {
                lblPluginName.setText(newValue.getName() + " v" + ((IPlugin.HasVersionInfo) newValue).getVersion());
            } else {
                lblPluginName.setText(newValue.getName());
            }
            this.setTitle("About " + newValue.getName());
            try {
                lblPluginPath.setText(((URLClassLoader) newValue.getClass().getClassLoader()).getURLs()[0].getFile());
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
