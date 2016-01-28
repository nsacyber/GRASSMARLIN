/*
 *  Copyright (C) 2011, 2012
 *  This file is part of GRASSMARLIN.
 */
package ui.menu;

import core.Core;
import core.Environment;
import core.exportmodule.ExportDataTask;
import core.exportmodule.ExportShareTask;
import core.exportmodule.TopologyExporter;
import core.importmodule.ImportItem;
import core.importmodule.LivePCAPImport;
import core.types.LogEmitter;
import org.apache.commons.io.FileUtils;
import ui.GrassMarlin;
import ui.dialog.DialogManager;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 */
public enum MenuAction {

    OpenImportAct(Menu.IMPORT, new AbstractAction() {
        @Override
        public void actionPerformed(ActionEvent e) {
            DialogManager.ImportDialog(true);
        }
    }),
    IMPORT_START(null, new AbstractAction() {
        @Override
        public void actionPerformed(ActionEvent e) {
            GrassMarlin.window.preferences.setOffline();
            GrassMarlin.window.importer.importStartAsynch(
                    DialogManager.ImportDialog(null).getImportItems()
            );
        }
    }),
    IMPORT_CANCEL(null, new AbstractAction() {
        @Override
        public void actionPerformed(ActionEvent e) {
            GrassMarlin.window.importer.importCancel();
        }
    }),
    EndLiveCapture(Menu.LIVE_STOP, new AbstractAction() {
        @Override
        public void actionPerformed(ActionEvent e) {
            GrassMarlin.window.importer.getActiveItems().stream().forEach(item -> {
                if (item instanceof LivePCAPImport) {
                    ((LivePCAPImport) item).cancel();
                }
            });
            GrassMarlin.window.importer.importCancel();
        }
    }),
    RefreshViews(null, new AbstractAction() {
        @Override
        public void actionPerformed(ActionEvent e) {
            GrassMarlin.window.refresh();
        }
    }),
    StartLiveCapture(Menu.LIVE_START, new AbstractAction() {
        @Override
        public void actionPerformed(ActionEvent e) {
            try {
                if (GrassMarlin.window.preferences.pcapAvailable) {
                    Object device = GrassMarlin.window.subMenu.getLiveDevice();
                    if (device instanceof LivePCAPImport) {
                        ArrayList<ImportItem> list = new ArrayList<>();
                        list.add((LivePCAPImport) device);
                        GrassMarlin.window.preferences.setLive();
                        GrassMarlin.window.importer.importStartAsynch(list);
                    } else {
                        throw new ClassCastException(device.toString());
                    }
                } else {
                    Logger.getLogger(MenuAction.class.getName()).log(Level.SEVERE, null, new Exception("PCAP capture is unavailable"));
                }
            } catch (ClassCastException ex) {
                Logger.getLogger(MenuAction.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    }),
    FilterManagerAct(Menu.MANAGER_FILTER, new AbstractAction() {
        @Override
        public void actionPerformed(ActionEvent e) {
            DialogManager.filterManager(true);
        }
    }),
    ToggleNetworksInView(null, new AbstractAction() {
        boolean networks_enabled;

        {
            this.networks_enabled = false;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (e.getSource() instanceof JCheckBox) {
                GrassMarlin.window.graph.toggleNetworkVisibility(((JCheckBox) e.getSource()).isSelected());
            }
        }
    }),
    OpenCaptureFile(Menu.LIVE_SHOW, new AbstractAction() {
        @Override
        public void actionPerformed(ActionEvent e) {
            try {
                Desktop.getDesktop().open(Environment.DIR_LIVE_CAPTURE.getDir());
            } catch (IOException ex) {
                Logger.getLogger(MenuAction.class.getName()).log(Level.SEVERE, null, ex);
                LogEmitter.factory.get().emit(Desktop.class, Core.ALERT.DANGER, "Cannot open live capture folder.");
            }
        }
    }),
    OpenPreferencesAct(Menu.PREFERENCES, new AbstractAction() {
        @Override
        public void actionPerformed(ActionEvent e) {
            DialogManager.preferences();
        }
    }),
    ClearTopologyAct(Menu.CLEAR_TOPOLOGY, new AbstractAction() {
        @Override
        public void actionPerformed(ActionEvent e) {
            GrassMarlin.window.pipeline.clear();
            GrassMarlin.window.tree.clear();
            GrassMarlin.window.graph.clear();
            DialogManager.ImportDialog(null).reset();
        }
    }),
    OpenHardwareVendorAct(Menu.MANAGER_VENDOR, new AbstractAction() {
        @Override
        public void actionPerformed(ActionEvent e) {
            DialogManager.HardwareVendorDialog(true);
        }
    }),
    ExportLogicalAct(Menu.EXPORT_LOGICAL, new AbstractAction() {
        @Override
        public void actionPerformed(ActionEvent e) {
            GrassMarlin.window.run(new TopologyExporter(GrassMarlin.window.graph.getLogical()::exportAll));
        }
    }),
    ExportPhysicalAct(Menu.EXPORT_PHYSICAL, new AbstractAction() {
        @Override
        public void actionPerformed(ActionEvent e) {
            GrassMarlin.window.run(new TopologyExporter(GrassMarlin.window.graph.getPhysical()::exportAll));
        }
    }),
    ExportBothAct(Menu.EXPORT_BOTH, new AbstractAction() {
        @Override
        public void actionPerformed(ActionEvent e) {
            TopologyExporter exporter = new TopologyExporter()
                    .add(GrassMarlin.window.graph.getPhysical()::exportAll)
                    .add(GrassMarlin.window.graph.getLogical()::exportAll);
            GrassMarlin.window.run(exporter);
        }
    }),
    OpenLogInTextFile(Menu.LOGFILE, new AbstractAction() {
        @Override
        public void actionPerformed(ActionEvent e) {
            Process proc = null;
            if (GrassMarlin.window.log.isLogfileAvailable()) {
                String path = GrassMarlin.window.log.getLogfilePath();
                String exec = Environment.TEXT_EDITOR_EXEC.getPath();
                String command = String.format("%s \"%s\"", exec, path);

                if (exec == null) {
                    LogEmitter.factory.get().emit("", Core.ALERT.DANGER, "Cannot open LogFile, please check user preferences.");
                } else {
                    try {
                        proc = Runtime.getRuntime().exec(command);
                    } catch (IOException ex) {
                        Logger.getLogger(MenuAction.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }

            }
            if (proc == null) {
                LogEmitter.factory.get().emit("", Core.ALERT.DANGER, "Could not open LogFile in text editor.");
            }
        }
    }),
    QuitAct(Menu.QUIT, new AbstractAction() {
        @Override
        public void actionPerformed(ActionEvent e) {
            System.exit(0);
        }
    }),
    OpenFManagerAct(Menu.MANAGER_FINGERPRINT, new AbstractAction() {
        @Override
        public void actionPerformed(ActionEvent e) {
            DialogManager.FMDialog(true);
        }
    }),
    OpenFEditorAct(Menu.MANAGER_FINGERPRINT_EDITOR, new AbstractAction() {
        @Override
        public void actionPerformed(ActionEvent e) {
            GrassMarlin.window.openFingerprintEditor(null);
        }
    }),
    OpenUserGuideAct(Menu.USER_GUIDE, new AbstractAction() {
        @Override
        public void actionPerformed(ActionEvent e) {
            File[] misc = Environment.DIR_MISC.getDir().listFiles();
            Process proc = null;
            for (File misc1 : misc) {
                String name = misc1.getName();
                if (name.contains("uide") && name.endsWith("pdf")) {
                    String path = misc1.getPath();
                    String exec = Environment.PDF_VIEWER_EXEC.getPath();
                    String command = String.format("%s \"%s\"", exec, path);
                    try {
                        proc = Runtime.getRuntime().exec(command);
                    } catch (IOException ex) {
                        Logger.getLogger(MenuAction.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    break;
                }
            }
            if (proc == null) {
                LogEmitter.factory.get().emit(proc, Core.ALERT.DANGER, "Cannot open user guide.");
            }
        }
    }),
    OpenTopologyKeyAct(Menu.TOPOLOGY_KEY, new AbstractAction() {
        @Override
        public void actionPerformed(ActionEvent e) {
            DialogManager.topologyDialog();
        }
    }),
    OpenAboutAct(Menu.ABOUT, new AbstractAction() {
        @Override
        public void actionPerformed(ActionEvent e) {
            DialogManager.AboutDialog(true);
        }
    }),
    TOGGLE_EVENT(Menu.EVENTS, new AbstractAction() {
        @Override
        public void actionPerformed(ActionEvent e) {
            GrassMarlin.window.log.setVisible(!GrassMarlin.window.log.isVisible());
            GrassMarlin.window.fixToggleVisibility();
        }
    }),
    TOGGLE_NETWORKMAP(Menu.NETWORKMAP, new AbstractAction() {
        @Override
        public void actionPerformed(ActionEvent e) {
            GrassMarlin.window.tree.setVisible(!GrassMarlin.window.tree.isVisible());
            GrassMarlin.window.fixToggleVisibility();
        }
    }),
    TOGGLE_NETWORKTOPOLOGY(Menu.NETWORKTOPOLOGY, new AbstractAction() {
        @Override
        public void actionPerformed(ActionEvent e) {
            GrassMarlin.window.graph.setVisible(!GrassMarlin.window.graph.isVisible());
            GrassMarlin.window.fixToggleVisibility();
        }
    }),
    REDRAW(Menu.REDRAW, new AbstractAction() {
        @Override
        public void actionPerformed(ActionEvent e) {
            GrassMarlin.window.resetViews(null);
        }
    }),
    ExportShareAct(Menu.EXPORT_SHARE, new AbstractAction() {
        @Override
        public void actionPerformed(ActionEvent e) {
            ExportShareTask est = new ExportShareTask();
            File tempFile = FileUtils.getFile(FileUtils.getTempDirectory(), "GM_DataExport.xml");
            try {
                FileUtils.forceDeleteOnExit(tempFile);
            } catch (IOException ex) {
                tempFile.deleteOnExit();
                Logger.getLogger(MenuAction.class.getName()).log(Level.SEVERE, null, ex);
            }
            new ExportDataTask(est::setDataFile).saveToFile(tempFile);
            est.run();
        }
    }),
    EXPORT_DATA(Menu.EXPORT_DATA, new AbstractAction() {
        @Override
        public void actionPerformed(ActionEvent e) {
            new ExportDataTask().run();
        }
    }
    ),
    OPEN_SUMMARY_DIAG(Menu.SUMMARY_REPORT, new AbstractAction() {
        @Override
        public void actionPerformed(ActionEvent e) {
            DialogManager.summaryDiag(true).refresh();
        }
    }),
    OPEN_REPORTS_DIAG(Menu.CONNECTION_REPORT, new AbstractAction() {
        @Override
        public void actionPerformed(ActionEvent e) {
            DialogManager.reportDiag(true);
        }
    });
    public final static HashMap<Menu, AbstractAction> actionMap = new HashMap<>();

    static {
        for (MenuAction ma : MenuAction.values()) {
            if (ma.menu == null) {
                continue;
            }
            MenuAction.actionMap.put(ma.menu, ma.action);

        }
    }

    public final AbstractAction action;
    private final Menu menu;

    /**
     * @param menu   - a menu item
     * @param action - action assigned to the menu item
     */
    MenuAction(Menu menu, AbstractAction action) {
        this.menu = menu;
        this.action = action;
    }

}
