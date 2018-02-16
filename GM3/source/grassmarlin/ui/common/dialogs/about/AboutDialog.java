package grassmarlin.ui.common.dialogs.about;

import grassmarlin.RuntimeConfiguration;
import grassmarlin.Version;
import grassmarlin.plugins.IPlugin;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

public class AboutDialog extends Dialog<ButtonType> {
    private final RuntimeConfiguration config;

    public AboutDialog(final RuntimeConfiguration config) {
        this.config = config;

        this.initComponents();
    }

    private void initComponents() {
        this.setTitle("About " + Version.APPLICATION_TITLE);
        RuntimeConfiguration.setIcons(this);

        final HBox layout = new HBox(2.0);
        final ImageView imageIconLarge = new ImageView(new Image(AboutDialog.class.getResourceAsStream("/resources/images/grassmarlin_256.png")));
        imageIconLarge.setPreserveRatio(true);
        imageIconLarge.setFitWidth(256.0);
        imageIconLarge.setFitHeight(256.0);

        final GridPane paneDetails = new GridPane();
        final Label textTitle = new Label(Version.APPLICATION_TITLE);
        final Label textVersionHeader = new Label("Version");
        textVersionHeader.setFont(Font.font(textVersionHeader.getFont().getFamily(), FontWeight.BOLD, textVersionHeader.getFont().getSize()));
        final Label textVersionValue = new Label(Version.APPLICATION_VERSION + "r" + Version.APPLICATION_REVISION);

        paneDetails.add(textTitle, 0, 0, 2, 1);
        paneDetails.add(textVersionHeader, 0, 1);
        paneDetails.add(textVersionValue, 1, 1);

        int idxRow = 2;
        if(config.isDeveloperModeProperty().get()) {
            paneDetails.add(new Label("DEVELOPER MODE is enabled."), 0, idxRow++, 2, 1);
        }
        paneDetails.add(new Label(String.format("ACTIVE PLUGINS are %s.", config.allowActiveScanningProperty().get() ? "enabled" : "disabled")), 0, idxRow++, 2, 1);
        paneDetails.add(new Label(String.format("LIVE PCAP is %s.", config.allowLivePcapProperty().get() ? "enabled" : "disabled")), 0, idxRow++, 2, 1);
        paneDetails.add(new Label(String.format("PLUGIN LOADING is %s.", config.allowPluginsProperty().get() ? "enabled" : "disabled")), 0, idxRow++, 2, 1);

        final Label textPlugins = new Label("Plugins");
        textTitle.setFont(textTitle.getFont());
        paneDetails.add(textPlugins, 0, idxRow++, 2, 1);
        boolean hasPlugins = false;
        for(IPlugin plugin : config.enumeratePlugins(IPlugin.class)) {
            //Skip the internal plugin
            if(plugin == config.pluginFor(AboutDialog.class)) {
                continue;
            }

            paneDetails.add(new Label(plugin.getName()), 0, idxRow);
            if(plugin instanceof IPlugin.HasVersionInfo) {
                paneDetails.add(new Label(((IPlugin.HasVersionInfo) plugin).getVersion()), 1, idxRow);
            }

            idxRow++;
            hasPlugins = true;
        }

        paneDetails.add(new Label("GrassMarlin is a product of the United States Department of Defense and is licensed for use and distribution under the GNU Lesser General Public License v3.0."), 0, idxRow++, 2, 1);
        if(hasPlugins) {
            paneDetails.add(new Label("Plugins are the property of their respective authors and may be subject to different copyright and licensing restrictions."), 0, idxRow++, 2, 1);
        }

        layout.getChildren().addAll(
                imageIconLarge,
                paneDetails
        );

        this.getDialogPane().setContent(layout);

        this.getDialogPane().getButtonTypes().addAll(ButtonType.CLOSE);
    }
}
