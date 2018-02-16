package grassmarlin;

import com.sun.javafx.collections.ObservableMapWrapper;
import javafx.beans.property.ReadOnlyMapWrapper;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public abstract class Version {
    /**
     * The APPLICATION_VERSION is the formal version number associated with this build.
     */
    public static final String APPLICATION_VERSION = "3.3.0Beta3";
    /**
     * The FILE_FORMAT_VERSION is the current version of the Session file format and is the version that will be written when saving.
     * Save logic is not necessarily present for other versions, but loading should have a degree of backwards compatibility.
     */
    public static final String FILE_FORMAT_VERSION = "3.3";
    /**
     * Formal application name, including version number.
     */
    public static final String APPLICATION_TITLE = "GrassMarlin " + APPLICATION_VERSION;
    /**
     *
     */
    public static final int APPLICATION_REVISION = 548;
    public static final String FILENAME_USER_GUIDE = "UserGuide.html";

    public static final boolean IS_BETA = true;

    public static final Map<String, List<String>> PATCH_NOTES = new ReadOnlyMapWrapper<>(new ObservableMapWrapper<>(new LinkedHashMap<String, List<String>>() {
        {
            this.put("3.3.0Beta3", Arrays.asList(
                    "This is BETA build.  This means that:",
                    "1) There is missing functionality.",
                    "2) The functionality that is present may not work.",
                    "3) Even when things appear to work, they might be wrong--we are actively refining algorithms and finding obscure, hard-to-reproduce bugs.",
                    "4) Every command that can be issued through the UI *should* work.",
                    "5) You *should not* see exception and stack traces in the console window, but since things aren't perfected yet, you probably will.",
                    "ImportProcessorWrappers now test a Path for validity rather than an ImportItem (the importitem is constructed only once a valid importer is known--or assumed)",
                    "The Pipeline workflow has been changed.  Most notably, the Pipeline Editor can modify the in-memory default template for a session before an import has started.",
                    "The Logical Graph code was refactored.  There was a slight performance gain measured, but mostly this was an effort to pay off technical debt.",
                    "The TabController system has been changed significantly, forcing updates to how tabs are generated.  The new system adds the following improvements:\n1) Each tab can have Undo/Redo menu items in the edit menu.\n2) Each tab can provide a set of menu items to be displayed in the View menu.\n3) Tabs can be turned on and off from the View menu (each tab can control whether or not it can be so changed).",
                    "Plugins can now report images to the Logical Graph rather than forcing new images to be added to the application directory.  Images on disk replace images provided by plugins.",
                    "Plugins can now provide their own icons.",
                    "The Physical Graph has been rebuilt but isn't complete yet.",
                    "The LogicalGraph reports have been expanded and are mroe responsive and sortable.",
                    "Edge Style Rules are no longer saved across executions.  Edge Style Rules can now change the color of edges (coloring edges must be turned on through the View menu)",
                    "Fingerprinting is much faster, but the more complicated algorithm for processing filters might lead to some incorrect (false-positive or false-negative) actions (nothing is known to have failed but it hasn't been robustly tested either).",
                    "There has been a major revision to the Logical Address class hierarchy."
            ));
            this.put("3.3.0Beta2", Arrays.asList(
                    "This is BETA build.  This means that:",
                    "1) There is missing functionality.",
                    "2) The functionality that is present may not work.",
                    "3) Even when things appear to work, they might be wrong--we are actively refining algorithms and finding obscure, hard-to-reproduce bugs.",
                    "4) Every command that can be issued through the UI *should* work.",
                    "5) You *should not* see exception and stack traces in the console window, but since things aren't perfected yet, you probably will.",
                    "6) The code needs a lot of cleanup work; there are a couple anticipated refactorings and a code review pending.",
                    "This beta version is intended to make available the basic 3.3 framework and allow groups wishing to develop plugins to start down that path.  We're not yet ready to formally support plugin development, but the tools are here--use the existing plugins as examples and remember you need to specify the -allowPlugins command-line argument.  If you are developing, you probably want to enable developer mode with -iacceptallresponsibilityforwhatiamabouttodo.  If working on pipeline stages, you probably want to run with the iadgov.visualpipeline plugin.",
                    "The IPlugin.SessionEventHooks interface was split to allow independent implementation of the load/save handlers; this is a breaking change with respect to plugin implementation.",
                    "Improved Watch Window functionality / performance.",
                    "Limitations of the current beta:",
                    "The physical graph does not process routers at this time.  Routers are now distinct from switches and handled differently--the distinction and the handling are not yet finalized.",
                    "In addition to missing routers, the physical graph is missing many usability-centric features",
                    "The navigation tree for the physical graph does not exist yet.",
                    "EdgeStyleRules are not properly updated/reapplied during certain lunar phases.",
                    "Saving and Loading sessions produces results of unknown value; refactoring/debugging/cleaning up that code the week of BronyCon was probably not the best idea I ever had.  It will take a couple weeks of rigorous testing to have confidence that the serialization code is functioning correctly.  As it stands now, it appears to function correctly, but it is a near-certainty that there is something that is omitted.",
                    "There are many less-noteworthy features present in 3.2 that haven't been implemented in 3.3 yet.  Some are being cut, while others are on the master \"TODO\" list--which are which has not been finalized yet."
            ));
            this.put("3.3.0", Arrays.asList(
                    "Changes:",
                    "The data processing pipeline has been abstracted and is configurable by the user.",
                    "The capability of plugins to interact with GrassMarlin has been greatly expanded since 3.2.0,",
                    "The Physical Graph can now be inferred from packet data.",
                    "The Logical Graph and Physical Graph have been rewritten to handle the flow of data through the pipeline.",
                    "The Logical Graph is no longer tightly coupled to IPv4 traffic; it operates on a graph of abstract Logical and Physical Addresses",
                    "The 'Clear Topology' command has been removed; 'New Session' accomplishes largely the same result and moving to an additive-only model has allowed us to make several improvements to the Logical and Physical Graphs.",
                    "The force-directed layout algorithm used on the Logical Graph has been greatly refined and should produce better results.  Also, it can be run as an animation that is eerily soothing, like watching an aquarium or an Internet-connected VM network of unpatched Windows XP machines that run automated scripts that carry out every security worst-practice activity imaginable.",
                    "Known Issues:",
                    "When expanding or hiding the display of child addresses on the Logical Graph, the edges do not always snap to the correct endpoints.  Moving either endpoint will cause the edge to re-evaluate the endpoints and correct itself."
            ));
            this.put("3.2.1", Arrays.asList(
                    "Following the release of GrassMarlin 3.2.0 we have, with help from a wide base of users, identified and fixed several bugs.",
                    "Importing PcapNg files had some issues that have been addressed, specifically with ARP and files created with a certain Endianness.",
                    "Many bugs were fixed in Fingerprinting.",
                    "Additional Fingerprints have been added, and existing Fingerprints have been updated.",
                    "If Wireshark is not auto-detected (or manually configured) properly, the application will no longer crash.",
                    "Improved support for builds that disable Live Pcap."
            ));
            this.put("3.2.0", Arrays.asList(
                    "KNOWN ISSUES:",
                    "Some context menu items are missing from the Physical Graph tree view; the missing commands are available from the Visualization context menu.",
                    "If a session is associated with a file (it was loaded or saved) and you start saving it again (to the same path), then cancel, the original file will be deleted.",
                    "Some PcapNg files do not contain timestamp information for packets.  These packets will be imported with the current timestamp; this is intentional but can be unexpected.",
                    "Occasionally, attempting to import a pcapng file as a pcap file (or vice versa) does not report an appropriate error message.",
                    "UPDATES:",
                    "The core UI has been rebuilt using the JavaFX UI library.  While the layout and function is similar to the 3.1 interface, the vast majority of the interface had to be rebuilt using JavaFX.  Many minor changes are unavoidable, but this brought increased stability.  These notes should detail the majority of the changes, but do not detail everything.",
                    "Many of the icons used throughout the application have been changed.",
                    "Instead of exporting the data from GrassMarlin as was done in 3.1 and previous builds, the session state can be saved.  The session state is a ZIP containing multiple XML files which contains all the data and the UI state information (It includes all the data generated from reading the imported files, but not hte actual file contents;  a test case of 5.4GB of PCAP yields an approximately 300MB file).  This state can be restored by loading the session.  Starting a new Session will restore many values to defaults, whereas clear Topology will simply un-import files.",
                    "The Fingerprint Editor has been integrated into the GrassMarlin application and has been merged with the functionality of the fingerprint Manager.",
                    "Dropping files onto the main application window will import them, if the file type is recognized.",
                    "A preliminary plugin model is in place; presently the only supported feature of a plugin is to create additional import formats.  An example plugin that imports host data from a CSV is available.",
                    "PcapNg files are no longer converted to Pcap files to be imported.  Most of the functionality of the PcapNg format is not yet supported, but it is feature-complete with respect to 3.1;  future builds should expand on the amount of data parsed from PcapNg files.",
                    "Fingerprints are loaded while the application is loading--there is no longer a separate task that loads them.",
                    "Graphs can now be exported to SVG.  SVG is a widely-used vector graphics format that allows the visualization of a graph to be exported, in full.",
                    "The Print command has been removed in favor of exporting a SVG and printing from an image editing/viewing application.  Supporting a robust, useful, platform-agnostic print interface has proven to be an inefficient use of development resources given the readily available external solutions.",
                    "Reports can be viewed from the relevant menu items in the View menu.",
                    "The network groupings used by the Logical Graph can be changed from the Packet Capture -> Manage Networks menu item.",
                    "By default the logical graph groups elements by Network, however right-clicking on the graph allows the nodes to be grouped by any field which has been matched by a Fingerprint or otherwise added to the node.",
                    "The logical graph now uses a force-directed model to position the groups, and then positions the elements within each group in a circle.  Auto-collapse of groups is no longer an option.",
                    "Watching a node on the Logical Graph will create a new Watch Tab.  The number of degrees of connection to track can be set through the context (right-click) menu.",
                    "The Physical Graph does a better job of identifying and positioning the ports on a switch, allowing the ports to be rendered in various styles.",
                    "The toolbar at the bottom of the visualization has been removed; instead the functionality has been integrated into a context menu on the visualization.",
                    "The context menus on the Tree View and Graph Visualizations reflect operations that can be performed on nodes, groups, edges connected to a node, or to the graph itself.  Consult the user guide for a complete list of commands.",
                    "The Zoom to Fit context menu item will zoom and center the view on the graph, whereas the Fit to Window will reposition the elements in the graph to fill the window.",
                    "The CPU no longer overheats when you hold down spacebar.",
                    "The Scroll Wheel can be used to zoom in or out on the graph.  Holding the Ctrl key while scrolling with the mouse over a group (so the group is highlighted) will expand or contract the elements in the group.",
                    "Nodes cannot be hidden on the primary Logical View; instead, create a new Filtered View from the View menu; nodes can be hidden and shown there.",
                    "GrassMarlin will no longer use the user directory to store documents, images, built-in fingerprints, and related files.",
                    "During startup GrassMarlin will run an integrity check on the GeoIp database, reporting CIDRs that resolve to a country Id that lacks a name as well as country names for which flag files cannot be located.",
                    "During startup GrassMarlin will also output all configurable values.",
                    "Version notes will now display the first time GrassMarlin is run after an update; they may also be viewed through the Help menu."
            ));
            // The User Guide says that 3.1 had a limited release and wasn't made available on GitHub.  This isn't wrong, but does omit a key phrase: "Twitch Programs GrassMarlin"
            this.put("3.1.0", Arrays.asList(
                    "Added Bro2ConnJson parsing.",
                    "Updates to Import dialog.",
                    "Fixed persistence of settings in Preferences dialog."
            ));
        }
    }));

    private Version() {}
}
