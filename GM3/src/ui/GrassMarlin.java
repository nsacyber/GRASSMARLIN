/*
 *  Copyright (C) 2011, 2012
 *  This file is part of GRASSMARLIN.
 */
package ui;

// core

import ICSDefines.Lookup;
import core.*;
import core.exec.Task;
import core.exec.TaskDispatcher;
import core.fingerprint.FManager;
import core.importmodule.DeviceList;
import core.importmodule.ImportItem;
import core.importmodule.Importer;
import core.knowledgebase.KnowledgeBase;
import core.topology.Entities;
import core.topology.PhysicalNode;
import core.types.LogEmitter;
import jnwizard.JNWizard;
import org.apache.commons.io.FileUtils;
import org.jnetpcap.Pcap;
import ui.dialog.DialogManager;
import ui.dialog.EntityEditor;
import ui.dialog.MacAddressTable;
import ui.icon.Icons;
import ui.log.UILog;
import ui.menu.Menu;
import ui.menu.MenuBar;
import ui.menu.SubMenu;
import ui.platform.Footer;
import ui.platform.SplitPane;
import ui.views.graph.GraphView;
import ui.views.tree.TreeView;
import ui.views.tree.visualnode.VisualNode;
import ui.views.viewfilter.ViewFilterList;
import ui.views.viewfilter.viewfilters.CategoryViewFilter;
import ui.views.viewfilter.viewfilters.CountryViewFilter;
import ui.views.viewfilter.viewfilters.FingerprintViewFilter;

import javax.swing.*;
import javax.swing.Timer;
import java.awt.*;
import java.io.File;
import java.util.*;
import java.util.List;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * GrassMarlin is the application class for this project.
 * core components MUST be initialized before ui components
 */
public class GrassMarlin extends JFrame {

    // ui components
    /**
     * the main GM window
     */
    public static GrassMarlin window;
    /**
     * menu
     */
    public final MenuBar menu;
    /**
     * tree view for holding the data
     */
    public final TreeView tree;
    /**
     * prefuse logical graph
     */
    public final GraphView graph;
    /**
     * footer ui element contains status and progress
     */
    public final Footer footer;
    /**
     * sub menus
     */
    public final SubMenu subMenu;
    /**
     * controls the ui log panel
     */
    public final UILog log;
    /**
     * worker
     */
    public final Timer appWorker;
    /**
     * gm core
     */
    public final Core core;
    /**
     * data and execution pipeline
     */
    public final Pipeline pipeline;
    /**
     * sets visibility flags on view data
     */
    public final ViewFilterList filterList;
    /**
     * fingerprint manager
     */
    public final FManager manager;
    /**
     * pcap importer
     */
    public final Importer importer;
    /**
     * user prefs
     */
    public final Preferences preferences;

    /**
     * Constructs a new instance of GrassMArlin, initializes ui elements and
     * opens main panel contains main
     */
    protected GrassMarlin() {
        System.out.println("Grassmarlin 3.0.1-r2402f");

        core = new Core();
        /* set plaf */
        GrassMarlin.trySetLook();

        /* setup major back-end components */
        this.pipeline = new Pipeline();

        this.filterList = new ViewFilterList(pipeline)
                .add(new FingerprintViewFilter())
                .add(new CategoryViewFilter())
                .add(new CountryViewFilter());

        this.preferences = new Preferences();
        loadMiscSettings();
        validatePcap();

        this.manager = new FManager();

        /* create the device list for live-capture, this will fail without permission */
        if (this.preferences.pcapAvailable) {
            this.subMenu = new SubMenu().useList(new DeviceList().populate());
        } else {
            this.subMenu = new SubMenu();
            this.subMenu.enabled(false);
        }

        this.importer = new Importer(this.pipeline.taskDispatcher(), this.pipeline, this.preferences);
        initializeKnowledgeBase();
        initializeByteFunctions();
        
        /* build UI */
        this.log = new UILog();
        this.menu = new MenuBar();
        this.tree = new TreeView(this.filterList);
        this.graph = new GraphView()
                .setCloudCollapseLimit(this.preferences.cloudCollapse)
                .setNetworkCollapseLimit(this.preferences.networkCollapse);
        this.footer = new Footer();

        initializeProgressBars();
        initializeMenu();
        initializeUiBuilder();
        attachCallbacks();
        open();

        /** appWorker drives all view updates */
        this.appWorker = new Timer(preferences.viewUpdateDelay, e -> {
            try {
                if (!this.subMenu.autoUpdateEnabled()) {
                    return;
                }
                refresh();
            } catch (Exception ex) {
                Logger.getLogger(TreeView.class.getName()).log(Level.SEVERE, "Worker stopped unexpectedly.", ex);
            }
        });
        this.appWorker.start();
    }

    public static void trySetLook() {
        try {
            UIManager.getDefaults().put("TabbedPane.contentBorderInsets", new Insets(0, 0, 0, 0));
            UIManager.getDefaults().put("TabbedPane.tabsOverlapBorder", false);
        } catch (Exception | Error ex) {
            System.err.println("Failed to set some UI elements.");
        }
        try {
            for (javax.swing.UIManager.LookAndFeelInfo info : javax.swing.UIManager.getInstalledLookAndFeels()) {
                if (System.getProperty("sun.desktop").equals(info.getName().toLowerCase())) {
                    try {
                        javax.swing.UIManager.setLookAndFeel(info.getClassName());
                    } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | UnsupportedLookAndFeelException ex) {
                        Logger.getLogger(GrassMarlin.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    break;
                }
            }
        } catch (Exception ex) {
            /** typically thrown on linux systems, and ignored */
            System.err.println("Using default PLAF.");
        }
    }

    public static void main(String[] args) {
        checkJNetPcapNativeAssembly();

        window = new GrassMarlin();

        /* create another thread to handle fingerprint loading */
        Objects.nonNull(window.manager);

        if (!Environment.WIRESHARK_EXEC.isAvailable()) {
            LogEmitter.factory.get().emit(args, Core.ALERT.WARNING, "Cannot locate Wireshark.");
        }

        window.manager.maybeAutoLoadFingerprints();
    }

    private void initializeMenu() {
        /* allows view to work with data from the logical view */
        Menu.DropDown.VIEW.item.onHover(graph.getLogical()::getVisibilityMenuItems);

        JMenu menu = new JMenu("Filter Views");
        menu.setIcon(Icons.Filter.getIcon());
        Menu.DropDown.VIEW.item.add(menu);
        Menu.DropDown.VIEW.item.addSeparator();

        filterList.forEach(filter ->
                        menu.add(new JMenuItem(filter.getName(), filter.getIcon())).addActionListener(e -> {
                            try {
                                filter.update();
                            } catch (Exception ex) {
                                String msg = "Cannot open filter dialog.";
                                LogEmitter.factory.get().emit(e, Core.ALERT.DANGER, msg);
                                Logger.getLogger(getClass().getName()).log(Level.SEVERE, msg, ex);
                            }
                        })
        );

        setIcon(Menu.IMPORT, Icons.Download);
        setIcon(Menu.EXPORT_DATA, Icons.Save_document);
        setIcon(Menu.EXPORT_LOGICAL, Icons.Save_image);
        setIcon(Menu.EXPORT_PHYSICAL, Icons.Save_image);
        setIcon(Menu.EXPORT_BOTH, Icons.Save_image);
        setIcon(Menu.EXPORT_SHARE, Icons.Save_data);
        setIcon(Menu.QUIT, Icons.Cross);
        setIcon(Menu.CLEAR_TOPOLOGY, Icons.Recycle);
        setIcon(Menu.MANAGER_FILTER, Icons.Filter);
        setIcon(Menu.LOGFILE, Icons.Document);
        setIcon(Menu.LIVE_START, Icons.Start);
        setIcon(Menu.LIVE_STOP, Icons.Stop);
        setIcon(Menu.LIVE_SHOW, Icons.Folder);
        setIcon(Menu.MANAGER_FINGERPRINT,Icons.Fingerprint);
        setIcon(Menu.MANAGER_FINGERPRINT_EDITOR,Icons.Fingerprint_add);
        setIcon(Menu.PREFERENCES,Icons.Cog);
        setIcon(Menu.MANAGER_VENDOR, Icons.Form);
        setIcon(Menu.CONNECTION_REPORT,Icons.Connection);
        setIcon(Menu.SUMMARY_REPORT,Icons.Summary);
        setIcon(Menu.USER_GUIDE, Icons.Pdf);
        setIcon(Menu.TOPOLOGY_KEY, Icons.Legend);
    }

    /**
     * Adds a JLabel which will be updated on each tick of {@link Footer#run()}.
     * If the Supplier returns a null or empty String the Supplier will be removed.
     * See {@link Footer#addIndicator(Supplier)}.
     * @param messageSupplier Supplier of a message which will appear in the UI.
     */
    public void addIndicator(Supplier<String> messageSupplier) {
        this.footer.addIndicator(messageSupplier);
    }

    private void setIcon(Menu item, Icons icon) {
        try {
            item.item.setIcon(icon.getIcon());
        } catch (Exception ex) {
            Logger.getLogger(getClass().getName()).log(Level.FINEST, "Failed to setIcon {0}", icon);
        }
    }

    /**
     * Places all UI components in the proper layout.
     */
    private void initializeUiBuilder() {
        UIBuilder builder = new UIBuilder();
        builder.setReference(this)
                .setSubMenu(this.subMenu)
                .setFooter(this.footer)
                .setTreeView(this.tree)
                .setGraph(this.graph)
                .setMenu(this.menu)
                .setLog(this.log)
                .build();
    }

    private void initializeProgressBars() {
        this.footer.setLoadingProgress(manager::getProgress);
        /* footer has an activity indicator and progress bars */
        this.manager.addActivityObserver((Observable o, Object arg) -> {
            if (!(arg instanceof String)) {
                return;
            }
            switch ((String) arg) {
                case FManager.BUSY:
                    LogEmitter.factory.get().emit(this, Core.ALERT.INFO, "Reloading fingerprints.");
                    this.footer.setLoadingProgress(manager::getProgress);
                    break;
                case FManager.IDLE:
                case FManager.COMPLETE:
                    this.footer.setLoadingProgress(null);
                    break;
                case FManager.FAILURE:
                    LogEmitter.factory.get().emit(this, Core.ALERT.DANGER, "Failed to load fingerprints.");
            }
        });
    }

    private void initializeKnowledgeBase() {
        /* load knowledge base */
        if (!Core.kb.isPresent()) {
            KnowledgeBase kb = KnowledgeBase.newInstance(Environment.DIR_PROPERTIES.getPath());
            Core.kb = Optional.ofNullable(kb);
        }
    }

    private void validatePcap() {
        String libPath = getLibPath();
        if (!this.preferences.pcapAvailable) {
            try {
                File libFolder = new File(libPath);
                if (libFolder.isFile()) {
                    libFolder = libFolder.getParentFile();
                }
                boolean exists = Arrays.asList(libFolder.list())
                        .stream().anyMatch(name -> name.contains("jnetpcap"));
                String msg;

                if (libFolder.exists() && exists) {
                    msg = "PCAP module " + libPath + "  failed to load. Reason: Wrong version";
                } else {

                    msg = "PCAP module failed to load. Reason: Missing library " + libPath + "/" + Pcap.LIBRARY;
                }
                System.err.println(msg);
                LogEmitter.factory.get().emit(preferences, Core.ALERT.DANGER, msg);
                System.loadLibrary("jnetpcap");
            } catch (Exception | Error ex) {
                Logger.getAnonymousLogger().log(Level.SEVERE, "Failed to load.", ex);
            }
        }
    }

    private String getLibPath() {
        String libPath = System.getProperty("java.library.path");
        libPath = new File(libPath).getAbsolutePath();
        return libPath;
    }

    private void loadMiscSettings() {
        Properties misc = Environment.PROP_MISC_PROPERTIES.getProperties();
        if (misc != null) {
            preferences.loadFromProperties(Environment.PROP_MISC_PROPERTIES.getDir(), misc);
            preferences.loadFromProperties(System.getProperties());
            preferences.save();
            /* reload after updating from system properties to parse vars properly */
            preferences.loadFromProperties(Environment.PROP_MISC_PROPERTIES.getDir(), misc);
        }
    }

    public final void refresh() {
        if (!tree.busy()) {
            tree.update();
        }
    }

    public final void attachCallbacks() {
        importer.getLogEmitter().addEvent(Importer.IMPORT_COMPLETE_TAG, () -> {
            footer.showSpinner(false);
            DialogManager.ImportDialog(null).notifyImportComplete();
            SwingUtilities.invokeLater(graph.getPhysical()::requestUpdate);
        });

        importer.getLogEmitter().addEvent(Importer.IMPORT_CANCEL_TAG, () ->
                        DialogManager.ImportDialog(null).notifyImportCancellation()
        );

        importer.getLogEmitter().addEvent(Importer.IMPORT_START_TAG, footer::showSpinner);

        /* pipeline expects to be told when there the filter object is available */
        manager.addFilterChangeObserver(pipeline);

        pipeline.fingerprintNameSupplier = manager::getFingerprintNames;
//        /* sidebar expects to get the data available in the tree */
//        tree.getPollObserver().addObserver(sidebar);
        /* graph expects to get the data availalbe in the tree */
        tree.getPollObserver().addObserver(graph);
        /* the graph expects to get updates when node's data change, such as name, icon, or category */
        tree.getNodeUpdateObserver().addObserver(graph);
        /* when the tree 'finds in view' a node the graph will center on it */
        tree.getNodeFocusObserver().addObserver(graph.getLogical()::observeFocus);

        List<Observer> physicalObervers = new ArrayList<>();

        physicalObervers.add(graph.getPhysical());
        physicalObervers.add(tree.getPhysicalTree());

        /* lets the physical graph control when it updates */
        graph.getPhysical().getUpdateRequest().addObserver((invoker, arg)
                        -> pipeline.runTopologyDiscover(physicalObervers)
        );

        graph.getTabChangedObserver().addObserver((invoker, arg) -> {
            if (graph.logicalActive()) {
                tree.showLogical();
            } else {
                tree.showPhysical();
            }
        });

        /** the tree will focus on an item when the graph requests it */
        graph.getFocusRequest().addObserver((invoker, arg) -> {
            if (arg instanceof VisualNode) {
                tree.focusItem((VisualNode) arg);
            }
        });

        /** when a subnet change occurs */
        Observer subnetchanged = (invoker, arg) -> {
            if (arg instanceof VisualNode) {
                VisualNode n = (VisualNode) arg;
                if (n.isNetwork()) {

                    ViewUtils.showSubnetEditor(n, (newMaskInts, newIPInts) -> {
                        int newMask = ViewUtils.intHash(newMaskInts);
                        window.resetViews(()
                                        -> n.getChildren().forEach(host -> {
                                    int newNetworkId = host.hashCode() & newMask;
                                    host.getData().updateNetwork(newNetworkId, newMask);
                                })
                        );
                    });

                }
            }
        };

        /** when a subnet is requested to be edited */
        graph.getSubnetChangeRequest().addObserver(subnetchanged);

        /** when a subnet is requested to be edited */
        tree.getSubnetChangeRequest().addObserver(subnetchanged);

        /**
         * To be run when the graph settings menu has changed settings.
         */
        DialogManager.GraphDialog(null).getUpdate().addObserver((invoker, arg) -> {
            appWorker.setDelay(this.preferences.viewUpdateDelay);
            graph.setNetworkCollapseLimit(this.preferences.networkCollapse);
            graph.setCloudCollapseLimit(this.preferences.cloudCollapse);
        });

        /**
         * To be run when a Physical view node wishes to have its {@link MacAddressTable} open.
         */
        graph.getPhysical().getViewCamTableRequest().addObserver((inv, obj) -> {
            if (obj instanceof PhysicalNode) {
                MacAddressTable mat = new MacAddressTable((PhysicalNode) obj);
                mat.setVisible(true);
            }
        });

        /**
         * To be run when a host is requested to be viewed from the physical view.
         */
        graph.getPhysical().getViewHostRequest().addObserver((inv, obj) -> {
            if (obj instanceof Entities) {
                EntityEditor editor = new EntityEditor((Entities) obj);
                editor.setVisible(true);
            }
        });

        /**
         * To be run when an entity is requested to be found in the tree view.
         */
        graph.getPhysical().getFindRequest().addObserver((inv, obj) -> {
            tree.getPhysicalTree().find(obj);
        });

        /**
         * To be run when a NIC detail editor is requested to be shown.
         */
        graph.getPhysical().getViewNICRequest().addObserver((inv, obj) -> {
            if (obj instanceof Entities) {
                EntityEditor editor = new EntityEditor((Entities) obj);
                editor.setVisible(true);
            }
        });

        LogEmitter.factory.get().addObserver(log.observer);
    }

    /**
     * Resets the views (Graphs and Trees) and logs a event message.
     */
    public void resetViews() {
        resetViews(null);
    }

    /**
     * Resets the views (Graphs and Trees) and logs a event message.
     * @param before Will run this action after updated are locked and the view data is cleared but before updates are resumed.
     */
    public void resetViews(Runnable before) {
        this.resetViews(before, true);
    }

    /**
     * Resets the views (Graphs and Trees) and logs a event message.
     * @param before Will run this action after updated are locked and the view data is cleared but before updates are resumed.
     * @param alert If true will alert the "View is resetting" message, else false and silent.
     */
    public void resetViews(Runnable before, boolean alert) {
        if( alert ) {
            LogEmitter.factory.get().emit(this, Core.ALERT.INFO, "View is resetting.");
        }
        tree.lockUpdates();
        tree.clear();
        graph.clear();
        graph.getLogical().vis().runLater(() -> {
            if (before != null) {
                before.run();
            }
            pipeline.resetVisualRows();
            tree.unlockUpdates();
        });
    }

    /**
     * Shows the application window.
     */
    public final void open() {
        this.setVisible(true);
        this.log.log(Core.ALERT.MESSAGE, "Grassmarlin is ready.");
        this.resetViews(null, false);
    }

    /**
     * Will auto-resize the window dimensions when a window is hidden or shown.
     */
    public void fixToggleVisibility() {
        int lim = window.getContentPane().getComponentCount();
        for (int i = 0; i < lim; i++) {
            Component c = window.getContentPane().getComponent(i);
            if (c instanceof SplitPane) {
                ((SplitPane) c).sizeDefault();
            }
        }
    }

    /**
     * @return List of all complete import items.
     */
    public List<ImportItem> getImports() {
        return DialogManager.ImportDialog(null).getCompletedItems();
    }

    /**
     * @param t Task to be ran in the {@link #window#pipeline}'s {@link TaskDispatcher}.
     */
    public void run(Task t) {
        this.pipeline.taskDispatcher().accept(t);
    }

    /**
     * initializes byte functions used to late-bind data as a result of
     * fingerprinting. Late binding is preferred because it reduces "at cost"
     * operations, and allows the data to be processed and become available upon
     * request to print, write, or access it.
     */
    private void initializeByteFunctions() {
        this.core.withKnowledgebase(kb -> {
            Lookup.BACNET.setSupplier(kb::getBacnetVendor);
            Lookup.ENIPDEVICE.setSupplier(kb::getEnipDevice);
            Lookup.ENIPVENDOR.setSupplier(kb::getEnipVendor);
        });
    }

    public String getJavaExecutable() {
        String javaHome = System.getProperty("java.home");
        String javaExec = javaHome + File.separator + "bin" + File.separator + "java";

        if (System.getProperty("os.name").contains("Windows")) {
            javaExec = javaExec.concat(".exe");
        } else {
            javaExec = javaExec.concat("");
        }

        return javaExec;
    }

    /**
     * Checks that the native assembly required to use JNetPcap is present on the FS.
     * If it is not then the assembly was not loaded upon launch and must be restarted.
     */
    private static void checkJNetPcapNativeAssembly() {
        File parent = FileUtils.getUserDirectory();
        File file = FileUtils.getFile(parent,  Environment.APPLICATION_NAME);
        file.mkdir();

        JNWizard jnw = new JNWizard();

        boolean firstRun = jnw.libSize() == 0L;
        if (!jnw.isPresent()) {
            jnw.run();
            JNWizard.errors.stream().map(Throwable::getMessage).forEach(System.err::println);
            if (firstRun) {
                JOptionPane.showMessageDialog(
                        null,
                        "Grassmarlin is done being configured. Please restart the application.",
                        "Restart Required",
                        JOptionPane.INFORMATION_MESSAGE
                );
                System.exit(0);
            }
        }
    }

    /**
     * Opens the Fingerprint editor from the same JRE running this instance of Grassmarlin.
     *
     * @param loadFile Absolute path to the file to open, or null to open a blank fingerprint.
     */
    public void openFingerprintEditor(String loadFile) {
        String command;
        String pathArg;
        String loadArg;
        String javaPath;
        String jarPath;
        String splashArg;
        String s = File.separator;
        String editorPath = Environment.DIR_MISC.getPath() + s + "Fingerprint_Editor" + s;

        if (loadFile != null && !loadFile.isEmpty()) {
            loadArg = String.format("--loadFile=\"%s\"", loadFile);
        } else {
            loadArg = "";
        }

        pathArg = String.format("--path=\"%s\"", Environment.DIR_FPRINT_USER.getPath());

        javaPath = getJavaExecutable();
        jarPath = editorPath + "lib" + s + "GM_FP.jar";

        splashArg = "-splash:" + editorPath + "images" + s + "splash.png";

        command = String.format("%s %s -jar %s %s %s", javaPath, splashArg, jarPath, pathArg, loadArg);

        try {
            Process proc = Runtime.getRuntime().exec(command);
        } catch (Exception | Error ex) {
            String msg = "Failed to open editor. Reason: " + ex.getCause().getMessage();
            Logger.getAnonymousLogger().log(Level.SEVERE, msg, ex);
            LogEmitter.factory.get().emit(command, Core.ALERT.DANGER, msg);
        }

    }

}
