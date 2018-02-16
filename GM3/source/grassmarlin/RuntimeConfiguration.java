package grassmarlin;

import com.sun.istack.internal.NotNull;
import com.sun.javafx.collections.ObservableListWrapper;
import grassmarlin.plugins.IPlugin;
import grassmarlin.session.pipeline.PipelineTemplate;
import javafx.beans.binding.BooleanExpression;
import javafx.beans.property.*;
import javafx.beans.value.ObservableValue;
import javafx.collections.ObservableList;
import javafx.scene.control.Dialog;
import javafx.scene.image.Image;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.Window;
import org.jnetpcap.Pcap;

import javax.swing.filechooser.FileSystemView;
import javax.xml.stream.XMLStreamException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.prefs.Preferences;
import java.util.stream.Collectors;

public class RuntimeConfiguration {
    //TODO: Add support for min delay on ThreadManagedState
    public static final int UPDATE_DELAY_MS = 2;
    public static final int UPDATE_INTERVAL_MS = 30;

    @FunctionalInterface
    public interface IDefaultValue {
        String getDefault();
    }

    public interface IPersistedValue {
        String getKey();
        IDefaultValue getFnDefault();
    }

    public enum UiMode {
        SDI,
        CONSOLE,
        CONFIGURATION,
        DIAGNOSTIC
    }

    public static class PersistedColorProperty extends SimpleObjectProperty<Color> {
        private final IPersistedValue field;

        public PersistedColorProperty(@NotNull final IPersistedValue field) {
            super(Color.web(getPersistedString(field)));

            this.field = field;
            addListener(this::Handle_change);
        }

        private void Handle_change(final ObservableValue<? extends Color> observable, final Color oldValue, final Color newValue) {
            setPersistedString(this.field, newValue.toString().substring(2, 8));
        }
    }

    public static class PersistedBooleanProperty extends SimpleBooleanProperty {
        private final IPersistedValue field;

        public PersistedBooleanProperty(@NotNull final IPersistedValue field) {
            super(Boolean.parseBoolean(getPersistedString(field)));

            this.field = field;
            addListener(this::Handle_change);
        }

        private void Handle_change(final ObservableValue<? extends Boolean> observable, final Boolean oldValue, final Boolean newValue) {
            setPersistedString(this.field, newValue.toString());
        }
    }

    public static class PersistedStringProperty extends SimpleStringProperty {
        private final IPersistedValue field;

        public PersistedStringProperty(@NotNull final IPersistedValue field) {
            super(getPersistedString(field));

            this.field = field;
            addListener(this::Handle_change);
        }

        private void Handle_change(final ObservableValue<? extends String> observable, final String oldValue, final String newValue) {
            setPersistedString(this.field, newValue);
        }
    }
    public static class PersistedDoubleProperty extends SimpleDoubleProperty {
        private final IPersistedValue field;

        public PersistedDoubleProperty(@NotNull final IPersistedValue field) {
            super(Double.parseDouble(getPersistedString(field)));

            this.field = field;
            addListener(this::Handle_change);
        }

        private void Handle_change(final ObservableValue<? extends Number> observable, final Number oldValue, final Number newValue) {
            setPersistedString(this.field, Double.toString(newValue.doubleValue()));
        }
    }

    public enum PersistableFields implements IPersistedValue {
        PATH_WIRESHARK("path.wireshark", () -> {
            final String nameOs = System.getProperty("os.name");
            final Path path;
            if(nameOs == null) {
                //This is almost guaranteed to be wrong, but we have no idea what OS we're on, so it is (marginally) better than nothing.
                path = Paths.get(".").toAbsolutePath();
            } else if(nameOs.startsWith("Windows")) {
                path = Paths.get("C:", "Program Files", "Wireshark", "Wireshark.exe");
            } else {
                //TODO: More intelligent guessing of wireshark path.
                path = Paths.get(".").toAbsolutePath();
            }

            if(path != null && Files.exists(path)) {
                return path.toString();
            } else {
                return null;
            }
        }),
        PATH_MANUFACTURER_DB("path.wireshark.manuf", () -> {
            final String nameOs = System.getProperty("os.name");
            final Path path;
            if(nameOs == null) {
                return null;
            } else if(nameOs.startsWith("Windows")) {
                //On windows, we can check the directory used by wireshark.
                return Paths.get(Paths.get(RuntimeConfiguration.getPersistedString(PATH_WIRESHARK)).getParent().toString(), "manuf").toString();
            } else {
                //On non-windows, guess--the wireshark path is probably to a symlink
                return "/usr/share/wireshark/manuf";
            }
        }),
        // The App, AppData, and UserData paths all have a special-case local directory that overrides the default values.
        // These local directories exist in the development environment and serve to sandbox file system access.
        // These overrides can be present in release situations, but are not expected to be used.
        // Since these are defaults, they are only used if there is no saved configuration value
        DIRECTORY_APPLICATION("path.app", () -> {
            if (Files.exists(Paths.get("dir_application"))) {
                return Paths.get("dir_application").toAbsolutePath().toString();
            } else {
                return Paths.get(".").toAbsolutePath().toString();
            }
        }),
        DIRECTORY_USER_DATA("path.userdata", () -> {
            if(Files.exists(Paths.get("dir_user_data"))) {
                return Paths.get("dir_user_data").toAbsolutePath().toString();
            } else {
                return Paths.get(FileSystemView.getFileSystemView().getDefaultDirectory().getPath(), "GrassMarlin").toAbsolutePath().toString();
            }
        }),
        DIRECTORY_APP_DATA("path.appdata", () -> {
            if(Files.exists(Paths.get("dir_app_data"))) {
                return Paths.get("dir_app_data").toAbsolutePath().toString();
            } else {
                if(System.getProperty("os.name").startsWith("Windows")) {
                    final String pathAppData = System.getenv("APPDATA");
                    if(pathAppData != null) {
                        return Paths.get(pathAppData, "GrassMarlin").toAbsolutePath().toString();
                    }
                }
                //If we don't have any better ideas, use USER_DATA--the directory needs to be writable.
                return getPersistedString(PersistableFields.DIRECTORY_USER_DATA);
            }
        }),
        DIRECTORY_LIVE_CAPTURE("dir.misc.livecaptures", () -> Paths.get(getPersistedString(PersistableFields.DIRECTORY_USER_DATA), "pcap").toAbsolutePath().toString()),
        DIRECTORY_PIPELINES("dir.misc.pipelines", () -> Paths.get(getPersistedString(PersistableFields.DIRECTORY_APP_DATA), "pipelines").toAbsolutePath().toString()),

        // == COLORS ==
        //  - Pipeline Editor -
        COLOR_PIPELINE_BACKGROUND("pipeline.color.background", () -> "d2691e"),
        COLOR_PIPELINE_TEXT("pipeline.color.text", () -> "ffffff"),
        COLOR_PIPELINE_TITLE_BACKGROUND("pipeline.color.title.background", () -> "f0ffff"),
        COLOR_PIPELINE_TITLE_TEXT("pipeline.color.title.text", () -> "ff0000"),
        COLOR_PIPELINE_SELECTOR("pipeline.color.selector", () -> "008000"),
        COLOR_PIPELINE_LINE("pipeline.color.line", () -> "006400"),
        COLOR_PIPELINE_WINDOW("pipeline.color.window", () -> "f5f5dc"),

        //  - Logical Graph -
        COLOR_GRAPH_BACKGROUND("graph.color.background", () -> "000000"),
        COLOR_GRAPH_TEXT("graph.color.text", () -> "ffffff"),

        COLOR_GRAPH_TCP_BACKGROUND("graph.color.tcp.background", () -> "ff0000"),
        COLOR_GRAPH_TCP_TEXT("graph.color.tcp.text", () -> "ffffff"),
        COLOR_GRAPH_UDP_BACKGROUND("graph.color.udp.background", () -> "00ff00"),
        COLOR_GRAPH_UDP_TEXT("graph.color.udp.text", () -> "ffffff"),
        COLOR_GRAPH_IPV4_PORT_BACKGROUND("graph.color.ipv4withport.background", () -> "0000ff"),
        COLOR_GRAPH_IPV4_PORT_TEXT("graph.color.ipv4withport.text", () -> "ffffff"),
        COLOR_GRAPH_IPV4_BACKGROUND("graph.color.ipv4.background", () -> "000000"),
        COLOR_GRAPH_IPV4_TEXT("graph.color.ipv4.text", () -> "ffffff"),



        // == APPLICATIONS ==
        TEXT_EDITOR_EXEC("path.texteditor", () -> {
            String path;
            if(System.getProperty("os.name").startsWith("Windows")) {
                //Again, pulling the application associated with .txt files would be preferable, but registry access from Java is both complicated and bad form.
                path = "C:\\Windows\\System32\\Notepad.exe";
            } else {
                //This could probably be done, but we will not weigh in on the emacs/vi/vim/butterfly debate, so null is the safest bet.
                path = null;
            }
            if(path != null && new File(path).exists()) {
                return path;
            } else {
                return null;
            }
        }),

        // == User Preferences ==
        PREF_LAST_RUN_VERSION("last_version", () -> ""),
        PREF_SUPPRESS_UNCHANGED_VERSION_NOTES("suppress_version_notes", () -> "true")
        ;

        private final String key;
        private final IDefaultValue fnDefault;

        @Override
        public String getKey() {
            return this.key;
        }
        @Override
        public IDefaultValue getFnDefault() {
            return this.fnDefault;
        }

        PersistableFields(final String key, final IDefaultValue fnDefault) {
            this.key = key;
            this.fnDefault = fnDefault;
        }
    }

    private UiMode uiMode = UiMode.DIAGNOSTIC;
    private final Map<String, IPlugin> plugins;
    private static final Map<String, ClassLoader> loaderForPlugin;
    private final BooleanProperty allowPlugins;
    private final BooleanProperty allowActiveScanning;
    private final BooleanProperty allowLivePcap;
    private final static BooleanProperty isLivePcapAvailable;
    private final BooleanProperty isDeveloperMode;
    private static AtomicBoolean isJnetPcapLoaded = new AtomicBoolean(false);
    private Event.IAsyncExecutionProvider uiEventProvider = Event.PROVIDER_IN_THREAD;

    private final Map<String, Set<String>> pluginDependenciesFromArguments;
    private final Map<String, String> pluginArguments;
    private final ObservableList<PipelineTemplate> pipelineTemplates;

    //All persisted properties should be static to minimize sync issues.
    private static final PersistedColorProperty colorPipelineBackground = new PersistedColorProperty(PersistableFields.COLOR_PIPELINE_BACKGROUND);
    private static final PersistedColorProperty colorPipelineText = new PersistedColorProperty(PersistableFields.COLOR_PIPELINE_TEXT);
    private static final PersistedColorProperty colorPipelineTitleBackground = new PersistedColorProperty(PersistableFields.COLOR_PIPELINE_TITLE_BACKGROUND);
    private static final PersistedColorProperty colorPipelineTitleText = new PersistedColorProperty(PersistableFields.COLOR_PIPELINE_TITLE_TEXT);
    private static final PersistedColorProperty colorPipelineSelector = new PersistedColorProperty(PersistableFields.COLOR_PIPELINE_SELECTOR);
    private static final PersistedColorProperty colorPipelineLine = new PersistedColorProperty(PersistableFields.COLOR_PIPELINE_LINE);
    private static final PersistedColorProperty colorPipelineWindow = new PersistedColorProperty(PersistableFields.COLOR_PIPELINE_WINDOW);

    private static final PersistedColorProperty colorGraphElementBackground = new PersistedColorProperty(PersistableFields.COLOR_GRAPH_BACKGROUND);
    private static final PersistedColorProperty colorGraphElementText = new PersistedColorProperty(PersistableFields.COLOR_GRAPH_TEXT);

    private static final PersistedColorProperty colorGraphElementIpv4Background = new PersistedColorProperty(PersistableFields.COLOR_GRAPH_IPV4_BACKGROUND);
    private static final PersistedColorProperty colorGraphElementIpv4Text = new PersistedColorProperty(PersistableFields.COLOR_GRAPH_IPV4_TEXT);
    private static final PersistedColorProperty colorGraphElementIpv4WithPortBackground = new PersistedColorProperty(PersistableFields.COLOR_GRAPH_IPV4_PORT_BACKGROUND);
    private static final PersistedColorProperty colorGraphElementIpv4WithPortText = new PersistedColorProperty(PersistableFields.COLOR_GRAPH_IPV4_PORT_TEXT);
    private static final PersistedColorProperty colorGraphElementIpv4TcpBackground = new PersistedColorProperty(PersistableFields.COLOR_GRAPH_TCP_BACKGROUND);
    private static final PersistedColorProperty colorGraphElementIpv4TcpText = new PersistedColorProperty(PersistableFields.COLOR_GRAPH_TCP_TEXT);
    private static final PersistedColorProperty colorGraphElementIpv4UdpBackground = new PersistedColorProperty(PersistableFields.COLOR_GRAPH_UDP_BACKGROUND);
    private static final PersistedColorProperty colorGraphElementIpv4UdpText = new PersistedColorProperty(PersistableFields.COLOR_GRAPH_UDP_TEXT);

    private static final PersistedBooleanProperty suppressUnchangedVersionNotes = new PersistedBooleanProperty(PersistableFields.PREF_SUPPRESS_UNCHANGED_VERSION_NOTES);

    private static final PersistedStringProperty lastLoadedVersion = new PersistedStringProperty(PersistableFields.PREF_LAST_RUN_VERSION);

    static {
        isLivePcapAvailable = new SimpleBooleanProperty(false);
        loaderForPlugin = new HashMap<>();
        loaderForPlugin.put(Plugin.NAME, RuntimeConfiguration.class.getClassLoader());

        try {
            //This will throw an UnsatisfiedLinkError exception if the library is missing.
            System.loadLibrary("jnetpcap");
            //Test that it loaded successfully.
            Pcap.libVersion();
            isLivePcapAvailable.set(true);
        } catch(UnsatisfiedLinkError noLib) {
            //This can happen for two reasons.  Either a developer fail put a path separator in the loadLibrary parameter, or the library can't be found within the search path.
            Logger.log(Logger.Severity.WARNING, "The JNetPcap Native Library appears to be missing; Live PCAP will be unavailable.");
            isLivePcapAvailable.set(false);
        } catch(Error | Exception ex) {
            //This should result from a fail on Pcap.libVersion if the Pcap class was not initialized.  We do a blanket catch because it doesn't really matter what went wrong, we're just going to make Pcap unavailable and move on.
            Logger.log(Logger.Severity.WARNING, "The JNetPcap Library could not be initialized; Live PCAP will be unavailable.");
            isLivePcapAvailable.set(false);
        }
    }

    public RuntimeConfiguration() {
        this.plugins = new LinkedHashMap<>();

        this.allowPlugins = new SimpleBooleanProperty(false);
        this.allowActiveScanning = new SimpleBooleanProperty(false);
        this.allowLivePcap = new SimpleBooleanProperty(false);

        this.isDeveloperMode = new SimpleBooleanProperty(false);

        this.pluginArguments = new HashMap<>();
        this.pipelineTemplates = new ObservableListWrapper<>(new ArrayList<>());

        this.pluginDependenciesFromArguments = new HashMap<>();
    }

    final void parseArgs(String[] args) {
        for(int idxArg = 0; idxArg < args.length; idxArg++) {
            final String arg = args[idxArg];
            switch(arg) {
                case "-allowPlugins":
                    this.allowPlugins.set(true);
                    break;
                case "-allowActiveScanning":
                    this.allowActiveScanning.set(true);
                    break;
                case "-allowLivePcap":
                    this.allowLivePcap.set(true);
                    break;
                case "-ui":
                    try {
                        this.uiMode = UiMode.valueOf(args[++idxArg]);
                    } catch(IndexOutOfBoundsException ex) {
                        System.out.println("Expected parameter: UI Mode (SDI|CONSOLE|CONFIGURATION|DIAGNOSTIC)");
                    } catch(Exception ex) {
                        System.out.println("Unable to set UI Mode (Invalid Value): " + args[idxArg]);
                    }
                    break;
                case "-iacceptallresponsibilityforwhatiamabouttodo":
                    isDeveloperMode.set(true);
                    break;
                case "-configurePlugin":
                case "-P":
                    //Either <plugin name>:<configuration> or <plugin name> <configuration> should follow this parameter
                    final String[] parametersForPlugin = args[++idxArg].split(":", 2);
                    if(parametersForPlugin.length == 1) {
                        pluginArguments.put(parametersForPlugin[0], args[++idxArg]);
                    } else {
                        pluginArguments.put(parametersForPlugin[0], parametersForPlugin[1]);
                    }
                    break;
                case "-dependencies":
                case "-pluginOrder":
                case "-O":
                    //Given a list of plugin names separated by commas, each plugin will depend on all plugins that come before it.
                    final String[] pluginOrder = args[++idxArg].split(",");
                    for(String plugin : pluginOrder) {
                        plugin = plugin.trim();
                        this.pluginDependenciesFromArguments.put(plugin, new HashSet<>(this.pluginDependenciesFromArguments.keySet()));
                    }
                    break;
            }
        }

        //TODO: Sanity check wireshark location
    }

    final void loadPlugins() {
        final Map<Class<? extends IPlugin>, Set<String>> dependencies = new HashMap<>();

        //Clean up existing plugins, if necessary.
        plugins.values().forEach(plugin -> {
            if(plugin instanceof AutoCloseable) {
                try {
                    ((AutoCloseable) plugin).close();
                } catch(Exception ex) {
                    Logger.log(Logger.Severity.ERROR, "The plugin %s encountered an error while closing: %s", plugin.getName(), ex.getMessage());
                }
            }
            Logger.log(Logger.Severity.INFORMATION, "The plugin %s has been unloaded.", plugin.getName());
        });
        plugins.clear();

        //Always load the core plugin
        Plugin corePlugin = new Plugin(this);
        plugins.put(corePlugin.getName(), corePlugin);

        if(allowPlugins.get()) {
            synchronized(loaderForPlugin) {
                try {
                    //Identify plugin classes to attempt to load.
                    final Map<String, Class<? extends IPlugin>> pluginClasses = new HashMap<>();
                    for (Path pathPlugin : Files.newDirectoryStream(Paths.get(getPersistedString(PersistableFields.DIRECTORY_APPLICATION), "plugins"))) {
                        if (pathPlugin.toString().endsWith(".jar")) {
                            try {
                                final String filenamePlugin = pathPlugin.getFileName().toString();
                                final String namePlugin = filenamePlugin.substring(0, filenamePlugin.length() - 4);

                                final ClassLoader loaderPlugin;
                                if (loaderForPlugin.containsKey(namePlugin)) {
                                    loaderPlugin = loaderForPlugin.get(namePlugin);
                                } else {
                                    loaderPlugin = new URLClassLoader(new URL[]{pathPlugin.toUri().toURL()});
                                    loaderForPlugin.put(namePlugin, loaderPlugin);
                                }

                                Class<?> classPlugin = loaderPlugin.loadClass(namePlugin + ".Plugin");
                                if (IPlugin.class.isAssignableFrom(classPlugin)) {
                                    if (classPlugin.isAnnotationPresent(IPlugin.Active.class) && !allowActiveScanning.get()) {
                                        Logger.log(Logger.Severity.WARNING, "Unable to load plugin '%s': Active plugins are disabled.", namePlugin);
                                    } else {
                                        pluginClasses.put(namePlugin, (Class<? extends IPlugin>) classPlugin);

                                        if(this.pluginDependenciesFromArguments.containsKey(namePlugin)) {
                                            dependencies.put((Class<? extends IPlugin>)classPlugin, new HashSet<>(this.pluginDependenciesFromArguments.get(namePlugin)));
                                        }

                                        if (classPlugin.isAnnotationPresent(IPlugin.Uses.class)) {
                                            dependencies.computeIfAbsent((Class<? extends IPlugin>)classPlugin, key -> new HashSet<>()).addAll(Arrays.asList(classPlugin.getAnnotation(IPlugin.Uses.class).value()));
                                        }
                                    }
                                } else {
                                    Logger.log(Logger.Severity.ERROR, "'%s' is not a valid Plugin.", filenamePlugin);
                                }
                            } catch (Exception ex) {
                                Logger.log(Logger.Severity.WARNING, "There was an error loading the Plugin file '%s': %s", pathPlugin.toString(), ex.getMessage());
                            }
                        }
                    }
                    //We have all the plugin classes (and can infer their loaders), so now we have to sort them based on dependencies.
                    final List<Class<? extends IPlugin>> pluginsToLoad = new ArrayList<>();
                    final List<Class<? extends IPlugin>> pluginsToSort = new LinkedList<>();
                    pluginsToSort.addAll(pluginClasses.values());
                    pluginsToSort.sort((p1, p2) -> p1.getName().compareTo(p2.getName()));

                    //Currently the core plugin is identified by "Grassmarlin" however some older plugins use "grassmarlin".
                    for(final Set<String> values : dependencies.values()) {
                        values.remove("grassmarlin");
                        values.remove("Grassmarlin");
                    }

                    while (!pluginsToSort.isEmpty()) {
                        //Start by finding plugins that have all their dependencies met.
                        final List<Class<? extends IPlugin>> pluginsToMove = new ArrayList<>();
                        for (final Class<? extends IPlugin> classPlugin : pluginsToSort) {
                            final Set<String> dependenciesUnfulfilled = this.pluginDependenciesFromArguments.get(classPlugin.getPackage().getName());
                            if(dependenciesUnfulfilled == null || dependenciesUnfulfilled.isEmpty()) {
                                pluginsToMove.add(classPlugin);
                            } else {
                                //There are unmet dependencies so we will skip it for now.
                                continue;
                            }
                        }
                        //if pluginstoMove is empty, then there are no plugins with all dependencies met; we need a tiebreaker.
                        if (pluginsToMove.isEmpty()) {
                            pluginsToSort.sort((l, r) -> {
                                //If a plugin has enough unfulfilled dependencies to overflow a 32-bit integer, the data loss on typecast is the least of your problems.
                                final int cntLeft = l.isAnnotationPresent(IPlugin.Uses.class) ? (int) Arrays.stream(l.getAnnotation(IPlugin.Uses.class).value()).filter(name -> pluginsToSort.contains(pluginClasses.get(name))).count() : 0;
                                final int cntRight = r.isAnnotationPresent(IPlugin.Uses.class) ? (int) Arrays.stream(r.getAnnotation(IPlugin.Uses.class).value()).filter(name -> pluginsToSort.contains(pluginClasses.get(name))).count() : 0;

                                if (cntLeft == cntRight) {
                                    return l.getCanonicalName().compareTo(r.getCanonicalName());
                                } else {
                                    return cntLeft - cntRight;
                                }
                            });
                            pluginsToMove.add(pluginsToSort.get(0));
                            pluginsToSort.remove(0);
                            Logger.log(Logger.Severity.WARNING, "Dependency deadlock detected: '%s' is being loaded first to resolve the deadlock.", pluginsToMove.get(0).getPackage().getName());
                        }
                        pluginsToLoad.addAll(pluginsToMove);
                        pluginsToSort.removeAll(pluginsToMove);
                        //Remove any newly fulfilled dependencies from the list.
                        final Collection<String> namesToMove = pluginClasses.entrySet().stream().filter(entry -> pluginsToMove.contains(entry.getValue())).map(entry -> entry.getKey()).collect(Collectors.toList());
                        for(final Class<? extends IPlugin> classPlugin : pluginsToMove) {
                            dependencies.computeIfAbsent(classPlugin, key -> Collections.EMPTY_SET).removeAll(namesToMove);
                        }
                    }

                    for (final Class<? extends IPlugin> classPlugin : pluginsToLoad) {
                        try {
                            final IPlugin plugin = classPlugin.getConstructor(RuntimeConfiguration.class).newInstance(this);
                            final String namePlugin = pluginNameFor(classPlugin);

                            this.plugins.put(namePlugin, plugin);
                            if (plugin instanceof IPlugin.AcceptsCommandLineArguments && pluginArguments.containsKey(namePlugin)) {
                                if (!((IPlugin.AcceptsCommandLineArguments) plugin).processArg(pluginArguments.get(namePlugin))) {
                                    Logger.log(Logger.Severity.WARNING, "The arguments for plugin [%s] were rejected by the plugin. (%s)", namePlugin, pluginArguments.get(namePlugin));
                                }
                            }
                            Logger.log(Logger.Severity.COMPLETION, "Loaded Plugin: %s", namePlugin);
                        } catch (NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException ex) {
                            Logger.log(Logger.Severity.ERROR, "Unable to load Plugin '%s': %s", pluginNameFor(classPlugin), Coalesce.of(ex.getMessage(), ex.getCause()));
                        }
                    }
                } catch (IOException ex) {
                    Logger.log(Logger.Severity.ERROR, "Unable to enumerate plugins: %s", ex.getMessage());
                }
            }
        }

    }

    public void loadPipelines() {
        Path pipelinePath = Paths.get(getPersistedString(PersistableFields.DIRECTORY_PIPELINES));

        try {
            Files.newDirectoryStream(pipelinePath, path -> path.toString().endsWith(".pt")).forEach((file) -> {
                try (InputStream in = Files.newInputStream(file)) {
                    final PipelineTemplate templateNew = PipelineTemplate.loadTemplate(this, in);
                    this.pipelineTemplates.add(templateNew);
                } catch (XMLStreamException xse) {
                    Logger.log(Logger.Severity.WARNING, "Unable to load malformed pipeline: " + file.getFileName());
                } catch (ClassNotFoundException cne) {
                    Logger.log(Logger.Severity.WARNING, "Plugin required by pipeline " + file.getFileName() + " not loaded");
                } catch (IOException ioe) {
                    Logger.log(Logger.Severity.WARNING, "Unable to read pipeline file " + file.getFileName());
                }
            });
        } catch (IOException ioe) {
            Logger.log(Logger.Severity.WARNING, "Unable to load pipelines, " + pipelinePath + " doesn't exist");
        }
    }

    // Misc support methods
    public void createAppDataDirectories() throws Exception {
        final String rootAppData = getPersistedString(PersistableFields.DIRECTORY_APP_DATA);
        if(rootAppData == null) {
            throw new Exception("App Data directory undefined.");
        }
        Files.createDirectories(Paths.get(rootAppData, "pipelines"));
        Files.createDirectories(Paths.get(rootAppData, "logs"));
    }

    public void createUserDataDirectories() throws Exception {
        final String rootUserData = getPersistedString(PersistableFields.DIRECTORY_USER_DATA);
        Files.createDirectories(Paths.get(rootUserData, "quicklists"));
        Files.createDirectories(Paths.get(rootUserData, "pcap"));
    }

    // == Accessors and utility functions that other classes care about ==
    public UiMode getUiMode() {
        return this.uiMode;
    }

    private static Image imgApplication64;
    private static Image imgApplication32;
    private static Image imgApplication16;
    public static synchronized Image getApplicationIcon64() {
        if(imgApplication64 == null) {
            imgApplication64 = new Image(RuntimeConfiguration.class.getResourceAsStream("/resources/images/grassmarlin_64.png"));
        }
        return imgApplication64;
    }
    public static synchronized Image getApplicationIcon32() {
        if(imgApplication32 == null) {
            imgApplication32 = new Image(RuntimeConfiguration.class.getResourceAsStream("/resources/images/grassmarlin_32.png"));
        }
        return imgApplication32;
    }
    public static synchronized Image getApplicationIcon16() {
        if(imgApplication16 == null) {
            imgApplication16 = new Image(RuntimeConfiguration.class.getResourceAsStream("/resources/images/grassmarlin_16.png"));
        }
        return imgApplication16;
    }
    public static void setIcons(final Stage stage) {
        stage.getIcons().add(getApplicationIcon64());
        stage.getIcons().add(getApplicationIcon32());
        stage.getIcons().add(getApplicationIcon16());
    }
    public static void setIcons(final Dialog<?> dialog) {
        final Window stage = dialog.getDialogPane().getScene().getWindow();
        if(stage instanceof Stage) {
            setIcons((Stage)stage);
        }
    }

    public static synchronized boolean isPersistedStringSet(@NotNull final IPersistedValue field) {
        final String tokenPlugin;
        if(field.getClass().getClassLoader() == RuntimeConfiguration.class.getClassLoader()) {
            tokenPlugin = "";
        } else {
            tokenPlugin = "[" + field.getClass().getPackage().getName() + "]" + File.separator;
        }
        final String persisted = Preferences.userRoot().get("GrassMarlin" + File.separator + tokenPlugin + field.getKey(), null);
        return (persisted != null);
    }
    public static synchronized String getPersistedString(@NotNull final IPersistedValue field) {
        final String tokenPlugin;
        if(field.getClass().getClassLoader() == RuntimeConfiguration.class.getClassLoader()) {
            tokenPlugin = "";
        } else {
            tokenPlugin = "[" + field.getClass().getPackage().getName() + "]" + File.separator;
        }
        final String persisted = Preferences.userRoot().get("GrassMarlin" + File.separator + tokenPlugin + field.getKey(), null);
        if(persisted == null) {
            if(field.getFnDefault() != null) {
                return field.getFnDefault().getDefault();
            } else {
                return null;
            }
        } else {
            return persisted;
        }
    }
    public static synchronized void setPersistedString(final IPersistedValue field, final String value) {
        Preferences.userRoot().put("GrassMarlin" + File.separator + field.getKey(), value);
    }
    public static synchronized void clearPersistedString(@NotNull final IPersistedValue field) {
        final String tokenPlugin;
        if(field.getClass().getClassLoader() == RuntimeConfiguration.class.getClassLoader()) {
            tokenPlugin = "";
        } else {
            tokenPlugin = "[" + field.getClass().getPackage().getName() + "]" + File.separator;
        }

        Preferences.userRoot().remove("GrassMarlin" + File.separator + tokenPlugin + field.getKey());
    }

    public <T extends IPlugin> List<T> enumeratePlugins(final Class<T> clazz) {
        return plugins.values().stream()
                .filter(plugin -> clazz.isAssignableFrom(plugin.getClass()))
                .map(clazz::cast)
                .collect(Collectors.toList());
    }

    public final IPlugin pluginFor(final Class<?> clazz) {
        return pluginFor(pluginNameFor(clazz));
    }
    public final IPlugin pluginFor(final String name) {
        return plugins.get(name);
    }
    public final <T extends IPlugin> T pluginFor(final String name, final Class<? extends T> clazz) {
        final IPlugin plugin = plugins.get(name);
        if(plugin != null && clazz.isInstance(plugin)) {
            return (T)plugin;
        }
        return null;
    }
    public static String pluginNameFor(final Class<?> clazz) {
        if(clazz.getClassLoader() == null) {
            //This happens for things like String.
            return null;
        }
        for(Map.Entry<String, ClassLoader> entry : loaderForPlugin.entrySet()) {
            if(entry.getValue() == clazz.getClassLoader()) {
                return entry.getKey();
            }
        }
        return null;
    }

    public static ClassLoader getLoader(final String namePlugin) {
        if(namePlugin == null || namePlugin.trim().equals("")) {
            return RuntimeConfiguration.class.getClassLoader();
        } else {
            synchronized(loaderForPlugin) {
                return loaderForPlugin.get(namePlugin);
            }
        }
    }

    public static void setLoader(final String namePlugin, final ClassLoader loader) {
        loaderForPlugin.put(namePlugin, loader);
    }

    public final IPlugin.ImportProcessorWrapper importerForFile(final Path pathFile) {
        final String nameFile = pathFile.getFileName().toString();
        for(IPlugin.HasImportProcessors plugin : enumeratePlugins(IPlugin.HasImportProcessors.class)) {
            for(IPlugin.ImportProcessorWrapper wrapper : plugin.getImportProcessors()) {
                if(wrapper.itemIsValidTarget(pathFile)) {
                    return wrapper;
                }
            }
        }
        return null;
    }

    public final IPlugin.ImportProcessorWrapper importerForName(final String name) {
        for(IPlugin.HasImportProcessors plugin : enumeratePlugins(IPlugin.HasImportProcessors.class)) {
            for(IPlugin.ImportProcessorWrapper wrapper : plugin.getImportProcessors()) {
                if(wrapper.getName().equals(name)) {
                    return wrapper;
                }
            }
        }
        return null;
    }

    public static void openPcapFile(final String pathPcap, final long idxFrame) {
        final String pathWireshark = getPersistedString(PersistableFields.PATH_WIRESHARK);
        if(pathWireshark == null) {
            Logger.log(Logger.Severity.ERROR, "Unable to open Wireshark:  Path not set.");
            return;
        }

        final File fileWireshark = new File(pathWireshark);
        if(!fileWireshark.exists()) {
            Logger.log(Logger.Severity.ERROR, "Unable to open Wireshark:  The specified file (%s) could not be found.", pathWireshark);
            return;
        }

        try {
            Runtime.getRuntime().exec(new String[] {pathWireshark, "-g", Long.toString(idxFrame), "-r", pathPcap});
        } catch(IOException ex) {
            Logger.log(Logger.Severity.ERROR, "Unable to open pcap in Wireshark: %s", ex.getMessage());
            return;
        }

        Logger.log(Logger.Severity.INFORMATION, "Opened [%s] in Wireshark.", pathPcap);
    }

    public Event.IAsyncExecutionProvider getUiEventProvider() {
        return this.uiEventProvider;
    }
    public void setUiEventProvider(final Event.IAsyncExecutionProvider provider) {
        this.uiEventProvider = provider;
    }

    public BooleanExpression allowPluginsProperty() {
        return allowPlugins;
    }
    public BooleanExpression allowLivePcapProperty() {
        return this.allowLivePcap.and(RuntimeConfiguration.isLivePcapAvailable);
    }
    public BooleanExpression allowActiveScanningProperty() {
        return allowActiveScanning;
    }

    public BooleanExpression isDeveloperModeProperty() {
        return isDeveloperMode;
    }

    public static BooleanProperty suppressUnchangedVersionNotesProperty() {
        return suppressUnchangedVersionNotes;
    }

    public StringProperty lastLoadedVersionProperty() {
        return lastLoadedVersion;
    }

    public ObjectProperty<Color> colorPipelineBackgroundProperty() {
        return colorPipelineBackground;
    }
    public ObjectProperty<Color> colorPipelineTextProperty() {
        return colorPipelineText;
    }
    public ObjectProperty<Color> colorPipelineTitleBackgroundProperty() {
        return colorPipelineTitleBackground;
    }
    public ObjectProperty<Color> colorPipelineTitleTextProperty() {
        return colorPipelineTitleText;
    }
    public ObjectProperty<Color> colorPipelineSelectorProperty() {
        return colorPipelineSelector;
    }
    public ObjectProperty<Color> colorPipelineLineProperty() {
        return colorPipelineLine;
    }
    public ObjectProperty<Color> colorPipelineWindowProperty() {
        return colorPipelineWindow;
    }

    public static ObjectProperty<Color> colorGraphElementBackgroundProperty() {
        return colorGraphElementBackground;
    }
    public static ObjectProperty<Color> colorGraphElementTextProperty() {
        return colorGraphElementText;
    }

    public static ObjectProperty<Color> colorGraphElementIpv4BackgroundProperty() {
        return colorGraphElementIpv4Background;
    }
    public static ObjectProperty<Color> colorGraphElementIpv4TextProperty() {
        return colorGraphElementIpv4Text;
    }
    public static ObjectProperty<Color> colorGraphElementIpv4WithPortBackgroundProperty() {
        return colorGraphElementIpv4WithPortBackground;
    }
    public static ObjectProperty<Color> colorGraphElementIpv4WithPortTextProperty() {
        return colorGraphElementIpv4WithPortText;
    }
    public static ObjectProperty<Color> colorGraphElementIpv4TcpBackgroundProperty() {
        return colorGraphElementIpv4TcpBackground;
    }
    public static ObjectProperty<Color> colorGraphElementIpv4TcpTextProperty() {
        return colorGraphElementIpv4TcpText;
    }
    public static ObjectProperty<Color> colorGraphElementIpv4UdpBackgroundProperty() {
        return colorGraphElementIpv4UdpBackground;
    }
    public static ObjectProperty<Color> colorGraphElementIpv4UdpTextProperty() {
        return colorGraphElementIpv4UdpText;
    }

    @Override
    public String toString() {
        final StringBuilder result = new StringBuilder();

        for(PersistableFields field : PersistableFields.values()) {
            final String value = getPersistedString(field);
            result.append("  ").append(field.key).append(": ").append(value == null ? "[null]" : value).append('\n');
        }

        return result.toString();
    }

    public IPlugin.FactoryPacketHandler factoryForNetworkType(final int network) {
        return this.enumeratePlugins(IPlugin.HasImportProcessors.class).stream()
                .filter(plugin -> plugin.getPcapHandlerFactories() != null)
                .flatMap(plugin -> plugin.getPcapHandlerFactories().entrySet().stream())
                .filter(entry -> entry.getKey().equals(network))
                .map(entry -> entry.getValue())
                .findFirst().orElse(null);
    }

    public ObservableList<PipelineTemplate> getPipelineTemplates() {
        return this.pipelineTemplates;
    }

    public List<Logger.Message> getStartupMessages() {
        List<Logger.Message> messages = new ArrayList<>(Logger.getStartupMessages());
        Logger.clearStartupMessages();

        return messages;
    }
}
