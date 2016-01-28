/*
 *  Copyright (C) 2011, 2012
 *  This file is part of GRASSMARLIN.
 */
package ui.dialog;

import ICSDefines.Category;
import core.Core;
import core.Environment;
import core.Preferences;
import core.types.LogEmitter;
import java.awt.Component;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Platform;
import javafx.embed.swing.JFXPanel;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import ui.GrassMarlin;
import ui.dialog.fingerprintManager.FingerprintManagerDialog;
import ui.icon.Icons;
import ui.menu.MenuAction;
import ui.views.viewfilter.FilterSelectionDialog;

/**
 * Manages the dialog objects
 */
public class DialogManager {

    static ImportDialog importDialog = null;
    static AboutDialog aboutDialog = null;
    static GraphSettingsDialog graphDiag = null;
    static ReportDialog reportDiag = null;
    static HardwareVendorManager hardwareDiag = null;
    static PreferencesDialog preferencesDiag = null;
    static PCAPFilterDialog filterDiag = null;
    static JDialog fingerprintManagerFrame;
    static FingerprintManagerDialog fingerprintManagerEx;
    static SummaryDialog summaryDiag = null;

    static {
        maybeInitializeFingerprintManagerDialog();
    }

    public static void topologyDialog() {
        new TopologyDialog(Category.createVector()).setVisible(true);
    }

    public static void preferences() {
        new PreferencesDialog().setVisible(true);
    }

    public static SummaryDialog summaryDiag(Boolean toggle) {
        if (summaryDiag == null) {
            summaryDiag = new SummaryDialog();
        }
        return (SummaryDialog) openOrClose(summaryDiag, toggle);
    }

    public static ReportDialog reportDiag(Boolean toggle) {
        if (reportDiag == null) {
            reportDiag = new ReportDialog();
        }
        return (ReportDialog) openOrClose(reportDiag, toggle);
    }

    public static GraphSettingsDialog GraphDialog(Boolean toggle) {
        if (graphDiag == null) {
            graphDiag = new GraphSettingsDialog();
            graphDiag.onOpen(() -> {
                graphDiag.cloudCollapse = GrassMarlin.window.preferences.cloudCollapse;
                graphDiag.networkCollapse = GrassMarlin.window.preferences.networkCollapse;
                graphDiag.viewDelayMS = GrassMarlin.window.preferences.viewUpdateDelay;
                /* avoid late initialization */
                graphDiag.onSave(GrassMarlin.window.preferences::save);
            });
            graphDiag.beforeSave(() -> {
                GrassMarlin.window.preferences.setCloudCollapse(graphDiag.cloudCollapse);
                GrassMarlin.window.preferences.setNetworkCollapse(graphDiag.networkCollapse);
                GrassMarlin.window.preferences.setViewUpdateDelay(graphDiag.viewDelayMS);
            });
        }
        return (GraphSettingsDialog) openOrClose(graphDiag, toggle);
    }

    /**
     *
     * @param toggle - toggle may be null to get a reference without a change to
     * UI.
     * @return UIDialogEx - a reference to the requested dialog
     */
    public static ImportDialog ImportDialog(Boolean toggle) {
        if (importDialog == null) {
            importDialog = new ImportDialog();
            importDialog.setStartBtnAction(MenuAction.IMPORT_START.action);
            importDialog.setCancelBtnAction(MenuAction.IMPORT_CANCEL.action);
            importDialog.setActivityIndicator(GrassMarlin.window.importer::importInProgress);
        }
        return (ImportDialog) openOrClose(importDialog, toggle);
    }

    public static FingerprintManagerDialog FMDialog(Boolean toggle) {
        fingerprintManagerEx.setAutoLoad(GrassMarlin.window.manager.mayAutoLoad());
        fingerprintManagerEx.setFpToDisplay(GrassMarlin.window.manager.getActiveFingerprintFileMap());

        fingerprintManagerFrame.setVisible(toggle);
        return fingerprintManagerEx;
    }

    public static void maybeInitializeFingerprintManagerDialog() {
        if (fingerprintManagerFrame == null) {
            fingerprintManagerFrame = new JDialog();
            JFXPanel jfxPanel = new JFXPanel();
            fingerprintManagerFrame.add(jfxPanel);
            fingerprintManagerFrame.setSize(540, 430);
            fingerprintManagerFrame.setTitle("Fingerprint Manager");
            Platform.runLater(() -> {
                initFingerprintManager(jfxPanel);
            });
        }
    }

    private static void initFingerprintManager(JFXPanel panel) {
        try {
            URL url = FingerprintManagerDialog.class.getResource("FingerprintManager.fxml");
            FXMLLoader loader = new FXMLLoader(url);
            Parent root = loader.load();
            fingerprintManagerEx = loader.getController();
            panel.setScene(new Scene(root));
        } catch (IOException ex) {
            Logger.getLogger(DialogManager.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     *
     * @param toggle - opens or closes the dialog
     * @return UIDialog - a reference to the requested dialog
     */
    public static AboutDialog AboutDialog(boolean toggle) {
        if (aboutDialog == null) {
            aboutDialog = new AboutDialog();
        }
        return (AboutDialog) openOrClose(aboutDialog, toggle);
    }

    public static HardwareVendorManager HardwareVendorDialog(boolean toggle) {
        if (hardwareDiag == null) {
            hardwareDiag = new HardwareVendorManager();
        }
        return (HardwareVendorManager) openOrClose(hardwareDiag, toggle);
    }

    private static Component openOrClose(Component dialog, Boolean b) {
        if (b != null) {
            dialog.setVisible(b);
        }
        return dialog;
    }

    public static PCAPFilterDialog filterManager(Boolean toggle) {
        if (filterDiag == null) {
            try {
                File filterFile = Environment.DIR_KNOWLEDGEBASE.getFile(PCAPFilterDialog.DESIRED_FILE_NAME);
                filterDiag = new PCAPFilterDialog(Preferences::testFilter, filterFile);
                filterDiag.put(GrassMarlin.window.preferences.getFilterTitle(), GrassMarlin.window.preferences.getFilterString());
                filterDiag.setActiveFilterSupplier(GrassMarlin.window.preferences::getFilterTitle);
                filterDiag.setApplyFilter((filterTitle, filterString) -> {
                    if (filterString.isEmpty() || filterString.matches("\\s+")) {
                        LogEmitter.factory.get().emit(filterDiag, Core.ALERT.INFO, "PCAP filter was set to allow all packets.");
                    } else {
                        LogEmitter.factory.get().emit(filterDiag, Core.ALERT.INFO, "PCAP filter was set to \"" + filterString + "\"");
                    }
                    GrassMarlin.window.preferences.setActiveFilter(filterTitle, filterString);
                });
            } catch (IOException ex) {
                Logger.getLogger(DialogManager.class.getName()).log(Level.SEVERE, null, ex);
                JOptionPane.showMessageDialog(GrassMarlin.window, "Cannot locate filter file.", "Error", JOptionPane.ERROR_MESSAGE, Icons.Error.getIcon32());
            }
        }
        if (filterDiag != null) {
            return (PCAPFilterDialog) openOrClose(filterDiag, toggle);
        }
        return null;
    }

    public static void showFilterChooser(String name, List possible, List current) {
        Collections.sort(possible);
        FilterSelectionDialog.newDialog(
                name,
                possible,
                current,
                FilterSelectionDialog::show,
                GrassMarlin.window::resetViews
        );

    }

}
