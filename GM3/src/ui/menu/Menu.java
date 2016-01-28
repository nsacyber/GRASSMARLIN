/*
 *  Copyright (C) 2011, 2012
 *  This file is part of GRASSMARLIN.
 */
package ui.menu;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.TreeMap;

import static ui.menu.Menu.DropDown.*;

/**
 *
 */
public enum Menu {

    /**
     * Set up menu bar
     * <pre>
     * attributes:
     * 1 - dropdown menu item
     * 2 - handle
     * 3 - display text
     * 4 - followed by a separator
     * 5 - is checkable
     * 6 - is a submenu
     * 7 - parent menu item (null if parent is a dropdown item)
     * </pre>
     */
    IMPORT(FILE, "import", "Import", true),
    CLEAR_TOPOLOGY(FILE, "clear", "Clear Topology", true),
    EXPORT(FILE, "export", "Export", true, false, true),
    EXPORT_TOPOLOGY(FILE, "export-topology", "Topology", false, false, true, Menu.EXPORT),
    EXPORT_LOGICAL(FILE, "export-topology-logical", "Logical View", false, false, false, Menu.EXPORT_TOPOLOGY),
    EXPORT_PHYSICAL(FILE, "export-topology-physical", "Physical View", false, false, false, Menu.EXPORT_TOPOLOGY),
    EXPORT_BOTH(FILE, "export-topology-both", "Both Views", false, false, false, Menu.EXPORT_TOPOLOGY),
    EXPORT_DATA(FILE, "export-data", "Data", false, false, false, Menu.EXPORT),
    EXPORT_SHARE(FILE, "export-share", "Share", false, false, false, Menu.EXPORT),
    QUIT(FILE, "quit", "Quit", false),
    LOGFILE(VIEW, "log-file", "Log File", true),
    LIVE_START(PACKET_CAPTURE, "start-live", "Start Live Capture", false),
    LIVE_STOP(PACKET_CAPTURE, "stop-live", "Halt Live Capture", true),
    LIVE_SHOW(PACKET_CAPTURE, "show-live", "Open Capture Folder", false),
    EVENTS(WINDOW, "EVENTS", "Events", false, true),
    NETWORKMAP(WINDOW, "NETWORKMAP", "Network Tree Map", false, true),
    NETWORKTOPOLOGY(WINDOW, "NETWORKTOPOLOGY", "Network Topology", false, true),
    REDRAW(WINDOW, "REDRAW", "Redraw Network Views", false),
    MANAGER_FILTER(OPTIONS, "filter-manager", "Filter Manager", false),
    MANAGER_VENDOR(OPTIONS, "hardware-vendor-manager", "Hardware Vendor Manager", false),

    MANAGER_FINGERPRINT(
            OPTIONS,
            "fingerprint-manager",
            "Fingerprint Manager",
            false
    ),
    MANAGER_FINGERPRINT_EDITOR(
            OPTIONS,
            "fingerprint-editor",
            "Create New Fingerprint",
            false
    ),
    PREFERENCES(OPTIONS, "preferences", "Preferences", false),

    CONNECTION_REPORT(REPORT, "xxx", "Connections", false),
    SUMMARY_REPORT(REPORT, "xxx", "Summary", false),

    USER_GUIDE(HELP, "User-guide", "User Guide", false),
    TOPOLOGY_KEY(HELP, "Topology-key", "Topology Key", false),
    ABOUT(HELP, "about", "About", false);

    public static TreeMap<DropDown, ArrayList<Menu>> MenuGroups;

    static {
        MenuGroups = new TreeMap<>();
        Arrays.asList(DropDown.values()).forEach((DropDown menu) -> {
            MenuGroups.put(menu, new ArrayList<>());
        });

        Arrays.asList(Menu.values()).forEach((Menu item) -> {
            MenuGroups.get(item.dropdown).add(item);
        });
    }

    public final String text;
    public final String handle;
    public final DropDown dropdown;
    public final Menu parent;
    public final boolean sep;
    public final boolean isSub;
    public final boolean toggle;
    public JMenuItem item;

    Menu(DropDown dropdown, String handle, String text, boolean sep) {
        this(dropdown, handle, text, sep, false);
    }

    Menu(DropDown dropdown, String handle, String text, boolean sep, boolean toggle) {
        this(dropdown, handle, text, sep, toggle, false);
    }

    Menu(DropDown dropdown, String handle, String text, boolean sep, boolean toggle, boolean isSub) {
        this(dropdown, handle, text, sep, toggle, isSub, null);
    }

    Menu(DropDown dropdown, String handle, String text, boolean sep, boolean toggle, boolean isSub, Menu parent) {
        this.text = text;
        this.handle = handle;
        this.dropdown = dropdown;
        this.sep = sep;
        this.isSub = isSub;
        this.parent = parent;
        this.toggle = toggle;
    }

    public enum DropDown {

        FILE("File"),
        VIEW("View"),
        WINDOW("Windows"),
        PACKET_CAPTURE("Packet Capture"),
        OPTIONS("Options"),
        REPORT("Report", true),
        HELP("Help");

        public final String displayText;
        public final boolean enabled;

        public ui.menu.DropDown item;

        DropDown(String displayText) {
            this.displayText = displayText;
            this.enabled = true;
        }

        DropDown(String displayText, boolean enabled) {
            this.displayText = displayText;
            this.enabled = enabled;
        }

    }
}
