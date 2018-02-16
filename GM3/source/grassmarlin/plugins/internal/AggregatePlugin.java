package grassmarlin.plugins.internal;

import grassmarlin.plugins.IPlugin;
import grassmarlin.ui.common.SessionInterfaceController;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

/**
 * A plugin class that encapsulates a set of plugin classes.
 */
//TODO: AggregatePlugin needs to provide a console interface, but it can't just be a passthrough, it will need to be functional.
public abstract class AggregatePlugin implements IPlugin, IPlugin.DefinesPipelineStages, IPlugin.HasImportProcessors, IPlugin.SessionEventHooks, IPlugin.HasClassFactory, IPlugin.SessionSerialization {
    private final String name;
    private final List<MenuItem> miSelf;
    private final List<IPlugin> plugins;

    protected AggregatePlugin(final String name, final IPlugin... plugins) {
        this.name = name;
        this.miSelf = new ArrayList<>();

        this.plugins = new ArrayList<>(plugins.length);
        this.plugins.addAll(Arrays.asList(plugins));

        this.initialize();
    }

    /**
     * Any data which will be cached in aggregate should be handled here.  Most methods can call on the fly.
     */
    private void initialize() {
        for(IPlugin plugin : plugins) {
            // IPlugin
            final Collection<MenuItem> itemsChild = plugin.getMenuItems();
            if(itemsChild == null || itemsChild.isEmpty()) {
                continue;
            }
            final Image image = plugin.getImageForSize(14);
            final ImageView graphic;
            if(image == null) {
                graphic = null;
            } else {
                graphic = new ImageView(image);
                graphic.setFitWidth(14.0);
                graphic.setFitHeight(14.0);
                graphic.setPreserveRatio(true);
            }
            final Menu itemChild = new Menu(plugin.getName(), graphic);

            this.miSelf.add(itemChild);

            itemChild.getItems().addAll(itemsChild);
        }
    }

    //Protected since it tends to be used internally.
    protected <T extends IPlugin> Collection<T> getPlugins(Class<T> clazz) {
        return plugins.stream().filter(clazz::isInstance).map(clazz::cast).collect(Collectors.toList());
    }
    //Public because it is expected that plugins that require configuration will have to find the constituent Plugin class to find their configuration.  The AggregatePlugin is handed to Stages, etc., and the internal Plugin is needed.
    public <T extends IPlugin> T getMember(Class<T> clazz) {
        //This should be a hard failure if the plugin isn't valid; invalid class references should never make it to the user, and stack traces should always make it to a developer.
        //noinspection OptionalGetWithoutIsPresent
        return plugins.stream().filter(clazz::isInstance).map(clazz::cast).findFirst().get();
    }

    //<editor-fold desc="IPlugin">
    @Override
    public String getName() {
        return name;
    }
    @Override
    public Collection<MenuItem> getMenuItems() {
        return miSelf;
    }

    private final Image iconSmall = new Image(AggregatePlugin.class.getClassLoader().getResourceAsStream("resources/images/grassmarlin_16.png"));
    private final Image iconLarge = new Image(AggregatePlugin.class.getClassLoader().getResourceAsStream("resources/images/grassmarlin_64.png"));
    @Override
    public Image getImageForSize(final int pixels) {
        if(pixels <= 24) {
            return iconSmall;
        } else {
            return iconLarge;
        }
    }
    //</editor-fold>
    //<editor-fold desc="IPlugin.HasLogicalAddressFactory">
    @Override
    public Collection<ClassFactory<?>> getClassFactories() {
        return getPlugins(IPlugin.HasClassFactory.class).stream().flatMap(plugin -> plugin.getClassFactories().stream()).collect(Collectors.toList());
    }
    //</editor-fold>
    //<editor-fold desc="IPlugin.DefinesPipelineStages">
    @Override
    public Collection<PipelineStage> getPipelineStages() {
        return getPlugins(IPlugin.DefinesPipelineStages.class).stream().flatMap(plugin -> plugin.getPipelineStages().stream()).collect(Collectors.toList());
    }
    @Override
    public Serializable getConfiguration(PipelineStage stage, Serializable config) {
        Serializable newConfig = null;
        // Try to get a new config from the plugins within this plugin, just try until it works
        for (DefinesPipelineStages plugin : this.getPlugins(DefinesPipelineStages.class)) {
            Serializable test = plugin.getConfiguration(stage, config);
            if (test != null) {
                newConfig = test;
                break;
            }
        }

        return newConfig;
    }
    @Override
    public Serializable getDefaultConfiguration(PipelineStage stage) {
        Serializable config = null;
        for (DefinesPipelineStages plugin : this.getPlugins(DefinesPipelineStages.class)) {
            Serializable test = plugin.getDefaultConfiguration(stage);
            if (test != null) {
                config = test;
                break;
            }
        }

        return config;
    }
    //</editor-fold>
    //<editor-fold desc="IPlugin.HasImportProcessors">
    @Override
    public Collection<ImportProcessorWrapper> getImportProcessors() {
        return getPlugins(IPlugin.HasImportProcessors.class).stream().flatMap(plugin -> plugin.getImportProcessors().stream()).collect(Collectors.toList());
    }
    @Override
    public HashMap<Integer, FactoryPacketHandler> getPcapHandlerFactories() {
        final HashMap<Integer, IPlugin.FactoryPacketHandler> result = new HashMap<>();
        for(final Map.Entry<Integer, IPlugin.FactoryPacketHandler> entry : getPlugins(IPlugin.HasImportProcessors.class).stream().map(plugin -> plugin.getPcapHandlerFactories()).filter(map -> map != null).flatMap(map -> map.entrySet().stream()).collect(Collectors.toList())) {
            result.put(entry.getKey(), entry.getValue());
        }
        return result;
    }
    //</editor-fold>
    //<editor-fold desc="IPlugin.SessionEventHooks">
    @Override
    public void sessionCreated(final grassmarlin.session.Session session, final SessionInterfaceController tabs) {
        getPlugins(IPlugin.SessionEventHooks.class).forEach(plugin -> plugin.sessionCreated(session, tabs));
    }
    @Override
    public void sessionSaving(final grassmarlin.session.Session session, final java.io.OutputStream stream, final CallbackCreateStream fnCreateStream) throws IOException {
        // Write file containing list of child plugins
        OutputStreamWriter writer = new OutputStreamWriter(stream, StandardCharsets.UTF_8);
        writer.write("<AggregatePlugin>\n");
        for(IPlugin.SessionEventHooks plugin : getPlugins(IPlugin.SessionEventHooks.class)) {
            writer.write("\t<Plugin name='");
            writer.write(plugin.getName());
            writer.write("'/>\n");
        }
        writer.write("</AggregatePlugin>");
        writer.flush();

        //Call the Saving method on each plugin that implements it, passing a plugin-specific stream to each.
        for(IPlugin.SessionSerialization plugin : getPlugins(IPlugin.SessionSerialization.class)) {
            plugin.sessionSaving(session, fnCreateStream.getStream(plugin.getName(), false), (name, buffered) -> fnCreateStream.getStream(plugin.getName() + "." + name, buffered));
        }
    }
    @Override
    public void sessionLoaded(final grassmarlin.session.Session session, final java.io.InputStream stream, final GetChildStream fnGetStream) throws IOException {
        for(final IPlugin.SessionSerialization plugin : getPlugins(IPlugin.SessionSerialization.class)) {
            plugin.sessionLoaded(session, fnGetStream.getStream(plugin.getName()), (name) -> fnGetStream.getStream(plugin.getName() + "." + name));
        }
    }
    @Override
    public void sessionClosed(final grassmarlin.session.Session session) {
        getPlugins(IPlugin.SessionEventHooks.class).forEach(plugin -> plugin.sessionClosed(session));
    }
    //</editor-fold>
}
