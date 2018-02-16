package grassmarlin.plugins;

import grassmarlin.session.ImportItem;
import grassmarlin.session.Session;
import grassmarlin.session.pipeline.AbstractStage;
import grassmarlin.ui.common.SessionInterfaceController;
import javafx.scene.control.MenuItem;
import javafx.scene.image.Image;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.BlockingQueue;

/**
 * The Plugin interface is used as the entry point to plugins.
 * A zero-argument constructor is required and will be called during application initialization.
 * The Plugin instance is then tested for the different Plugin sub-interfaces that are defined here.
 */
public interface IPlugin {
    /**
     * Annotation for declaring dependencies on other plugins.
     *
     * Using this annotation suggests to GrassMarlin that other Plugins should be loaded before this Plugin is loaded, but there is no guarantee that this is done.
     *
     * This affects only the loading of the plugins themselves, not the order of SessionLoaded calls made after initialization has concluded, nor does it affect any other operation.
     *
     * The internal (grassmarlin.Plugin) Plugin will always be loaded first.
     * Everything without dependencies are loaded next.
     * When a Plugin is loaded, we remove it from the list of dependencies of all other Plugins.
     * This repeats until there are no plugins left to load or all plugins have at least one unfulfilled dependency.
     * If there are unfulfilled dependencies, one Plugin will be chosen at random (*not random, but the actual method is an implementation detail subject to change) to be loaded and the cycle repeat.
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    @interface Uses {
        /**
         * One or more plugin names that should be loaded before this plugin.  The grassmarlin internal plugin will always be loaded before every other plugin.
         * @return
         */
        String[] value() default {};
    }

    /**
     * Annotation for declaring active plugins.
     *
     * If a plugin class has the @grassmarlin.IPlugin.Active annotation, it will be ignored unless active plugins are permitted.
     *
     * For a Plugin which contains only active components, this is the recommended safeguard.
     *
     * If a Plugin contains active and non-active components, then the access to active content should be gated with appropriate checks against the current RuntimeConfiguration.
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    @interface Active { }

    /**
     * Annotation for indicating menu items which can be executed through the console.
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    @interface SafeForConsole { }

    // == Container classes used by plugins ==

    /**
     * Metadata associated with a Pipeline Stage.
     */
    class PipelineStage {
        private final boolean configurable;
        private String name;
        private final Class<? extends AbstractStage<Session>> stage;
        private final Set<String> outputs;

        /**
         * Constructor that sets all members to the values given as parameters.
         * @param configurable    Enables configuration of this Stage in the Pipeline Editor.  This Plugin must support configuration through the getConfiguration and getDefaultConfiguration methods.
         * @param name            The name of the Stage, which will be displayed to the user.
         * @param stage           The class object for the Stage.
         * @param outputs         A list of the names of all valid outputs from this Stage.  At this time there is no support for individual Stage instances changing their valid outputs.
         */
        public PipelineStage(final boolean configurable, final String name, final Class<? extends AbstractStage<Session>> stage, final Collection<String> outputs) {
            this.name = name;
            this.stage = stage;
            this.configurable = configurable;
            this.outputs = new LinkedHashSet<>();
            if(outputs != null) {
                this.outputs.addAll(outputs);
            }
        }

        /**
         * Constructor that sets all members to the values given as parameters.
         * @param configurable    Enables configuration of this Stage in the Pipeline Editor.  This Plugin must support configuration through the getConfiguration and getDefaultConfiguration methods.
         * @param name            The name of the Stage, which will be displayed to the user.
         * @param stage           The class object for the Stage.
         * @param outputs         A list of the names of all valid outputs from this Stage.  At this time there is no support for individual Stage instances changing their valid outputs.
         */
        public PipelineStage(final boolean configurable, final String name, final Class<? extends AbstractStage<Session>> stage, final String... outputs) {
            this(configurable, name, stage, Arrays.asList(outputs));
        }

        /**
         * @return Returns the display name of the Stage.
         */
        public String getName() {
            return this.name;
        }

        /**
         * This method is used to rename Stage instances in the Pipeline Editor.  It should not be used elsewhere.
         * This is a hack for configuring Stages and will be refactored in a future release.
         * @param name    The name of this instance of the Stage.
         */
        @Deprecated
        public void setName(String name) {
            this.name = name;
        }

        /**
         * @return Returns the Class for this Stage.
         */
        public Class<? extends AbstractStage<Session>> getStage() {
            return this.stage;
        }

        /**
         * @return Returns a list of all valid outputs from this Stage.
         */
        public Set<String> getOutputs() {
            return this.outputs;
        }

        /**
         * @return Returns true if this type of Stage is configurable.
         */
        public boolean isConfigurable() {
            return this.configurable;
        }
    }

    /**
     * Describes an Import capability.
     */
    abstract class ImportProcessorWrapper {
        private final String name;
        private final String[] extensions;

        /**
         *
         * @param name
         * @param extensions
         */
        public ImportProcessorWrapper(final String name, final String... extensions) {
            this.name = name;
            this.extensions = extensions;
        }

        /**
         * @return Returns the display Name of this Processor.
         */
        public String getName() {
            return this.name;
        }

        /**
         * @return Returns an array of file extensions (that should be in the ".extension" format) that are associated with this Processor.  These are only used to provide filters in the Open dialog.
         */
        public String[] getExtensions() {
            return extensions;
        }
        public abstract Iterator<Object> getProcessor(final ImportItem item, final Session session);

        /**
         * Method to test whether this Processor can be applied to a given item.
         * @param item The Path to the item to be imported.
         * @return Returns true if the item can be imported, false if it cannot.  If uncertain, true should be returned.
         */
        public abstract boolean itemIsValidTarget(final Path item);

        @Override
        public String toString() {
            return name;
        }
    }

    /**
     *   Interface used to construct IPacketHandler instances.
     */
    @FunctionalInterface
    interface FactoryPacketHandler {
        /**
         * Creates an instance of an IPacketHandler to handle the contents of the given item.
         * @param source         The source of the data to process.
         * @param packetQueue    The queue into which generated objects should be placed.
         * @return Returns an IPacketHandler that will parse the contents of source into the packetQueue.
         */
        IPacketHandler create(final ImportItem source, final BlockingQueue<Object> packetQueue);
    }

    /**
     * Interface that can parse packet contents into Objects to be placed in the Pipeline.
     */
    interface IPacketHandler {
        /**
         * Processes a single packet.
         *
         * The queue into which the packet's contents should be placed is given to the create method of the FactoryPacketHandler that created this instance.
         * @param bufPacket       A ByteBuffer containing the packet contents, including all headers.
         * @param msSinceEpoch    The timestamp, in milliseconds from epoch, when this packet was captured.
         * @param idxFrame        The frame number within the Pcap file.
         * @return Returns the number of bytes to advance the file parsing progress.  Progress that will be recorded later (by the Record Deferred Progress Stage, for example) should be subtracted from the size of the packet.
         */
        int handle(final ByteBuffer bufPacket, final long msSinceEpoch, final int idxFrame);
    }

    // == IPlugin behaviors

    /**
     * The HasClassFactory interface allows a Plugin to provide GrassMarlin with the means to construct typed objects from a String.
     * This is similar to StringConverter but contains more metadata to better present the user with information.
     */
    interface HasClassFactory extends IPlugin {
        /**
         * The ClassFactory<T> interface specifies how to convert a String into an object of type T while maintaining metadata to present this conversion to the user.
         * @param <T> The type that is constructed by this instance.
         */
        interface ClassFactory<T> {
            /**
             * @return The display name of the class constructed by this instance.
             */
            String getFactoryName();

            /**
             * @return The Class associated with this instance.
             */
            Class<T> getFactoryClass();

            /**
             * @param text The string to convert into a T
             * @return An instance of a T, as described by the given text.
             */
            T createInstance(final String text);

            /**
             * @param text The string to test for validity.
             * @return True if the given text can be converted into an object of type T.
             */
            boolean validateText(final String text);
        }

        /**
         * @return Every ClassFactory this Plugin provides.
         */
        Collection<ClassFactory<?>> getClassFactories();
    }

    /**
     * The HasImportProcessors allows the Plugin to provide the means to process files and/or contents of PCAP files.
     */
    interface HasImportProcessors extends IPlugin {
        /**
         *
         * @return All methods of importing files that are made available by this Plugin, or null if none are available.
         */
        Collection<ImportProcessorWrapper> getImportProcessors();

        /**
         *
         * @return A mapping between pcap network types and the FactoryPacketHandler instance to handle that type.  Null is treated as equivalent to an empty map.
         */
        Map<Integer, FactoryPacketHandler> getPcapHandlerFactories();
    }

    /**
     * The SessionEventHooks interface allows the Plugin to respond to the creation and destruction of a Session.
     */
    interface SessionEventHooks extends IPlugin {
        /**
         * This will be called every time a Session is created.  Session creation happens both when a blank Session is opened as well as when a Session is loaded.
         *
         * This will be called in the UI thread of the current interface.
         * @param session    The Session that was just created.
         * @param tabs       If there is a JavaFx interface, this is the SessionInterfaceController that governs the content area of that interface.
         */
        void sessionCreated(final grassmarlin.session.Session session, final SessionInterfaceController tabs);

        /**
         * This will be called whenever a session is destroyed.  In the case of SDI, creating a new session will result in the destruction of the prior session.
         * This will be called in the UI thread of the current interface.
         * There are no guarantees regarding the order of operations when a single user action simultaneously creates one session while destroying another (e.g. New Session with SDI).
         * @param session    The Session that was closed.
         */
        void sessionClosed(final grassmarlin.session.Session session);
    }

    /**
     * The SessionSerialization interface allows a Plugin to save and load data during Session serialization.
     *
     * GrassMarlin Sessions are saved as ZIP files containing multiple XML documents.
     *
     * Nothing compels the use of XML, however it is strongly encouraged to simplify integration with other applications.
     */
    interface SessionSerialization extends IPlugin {
        @FunctionalInterface
        interface CallbackCreateStream {
            /**
             * Creates a new child stream into which data can be written.
             * @param name        The name of the child stream.  This must be unique within this Plugin.
             * @param buffered    If a stream is buffered, it will be written into memory and committed to the file after returning from sessionSaving.  Non-buffered streams are all written through the same interface, so opening a new non-buffered stream terminates the previous non-buffered stream.
             * @return Returns a stream to which data can be written.
             * @throws IOException
             */
            OutputStream getStream(final String name, final boolean buffered) throws IOException;
        }

        @FunctionalInterface
        interface GetChildStream {
            /**
             * Gets a child stream that was previously written.
             * @param name    The name of the child stream.
             * @return Returns the stream requested.
             * @throws IOException
             */
            InputStream getStream(final String name) throws IOException;
        }

        // Since serializing a session is going to have to save and load data that belongs to a plugin, there need to be more hooks
        void sessionLoaded(final grassmarlin.session.Session session, final java.io.InputStream stream, final SessionSerialization.GetChildStream fnGetStream) throws IOException;

        /**
         * Called in each plugin when a session is being saved.  Each plugin should save enough state information to restore itself when sessionLoaded is called.
         * @param session The session being saved.
         * @param stream The default stream for this plugin.  This stream should NOT be closed by the plugin, so avoid try-with-resources for creating specialized stream writers.  Also, when wrapping this with some form of writer, remember to flush the wrapping stream.
         * @param fnCreateStream    A method that can be called to create a new, named sub-stream.
         * @throws IOException
         */
        void sessionSaving(final grassmarlin.session.Session session, final java.io.OutputStream stream, final SessionSerialization.CallbackCreateStream fnCreateStream) throws IOException;

    }

    /**
     * The DefinesPipelineStages interface allows a Pipeline to define one or more Pipeline Stages.
     */
    interface DefinesPipelineStages extends IPlugin {
        Collection<PipelineStage> getPipelineStages();

        /**
         * This method is called to configure a <code>PipelineStage</code> when modifying a <code>PipelineTemplate</code>
         *
         * @param stage The stage to get the configuration for
         * @param configuration The current configuration of the stage
         * @return The new configuration for the stage
         */
        default Serializable getConfiguration(PipelineStage stage, Serializable configuration) {
            return null;
        }

        /**
         * This method is used to get the default configuration for a <code>PipelineStage</code>
         *
         * @param stage The stage to get the default configuration for
         * @return The default configuration for the stage
         */
        default Serializable getDefaultConfiguration(PipelineStage stage) {
            return null;
        }
    }

    interface AcceptsCommandLineArguments extends IPlugin {
        /**
         * Arguments on the command line can be passed to Plugins through this mechanism.
         *
         * A single string argument can be passed to each Plugin.
         *
         * @param arg   The argument to process.
         * @return returns false if the arguments were invalid, true otherwise.  This does not affect the loading of the plugin, but will produce a warning message.
         */
        boolean processArg(final String arg);
    }

    // == IPlugin.HasVersionInfo

    /**
     * The HasVersionInfo interface allows a Plugin to report a version number.  This has no meaning outside display to the user.
     */
    interface HasVersionInfo extends IPlugin {
        /**
         * @return Returns a String representation of this Plugin's version.  There is no behavior attached to this value, so the format doesn't matter.
         */
        String getVersion();
    }

    // == IPlugin.ConsoleInput

    interface ConsoleInput extends IPlugin {
        void processConsoleCommand(final String line, final SessionInterfaceController controller);
    }

    // == Core IPlugin
    //There should be a constructor that takes a RuntimeConfiguration as the only parameter.

    /**
     * @return Returns the Display Name of this Plugin.
     */
    String getName();

    /**
     * The MenuItems returned from this method will be added to a Menu in the Plugin Menu.
     *
     * Often the MenuItems are used to configure the Plugin, but remember that the scope of the Plugin is not necessarily the scope of a single Session--it generally has Application scope.  MenuItems should therefore be operating at application scope.
     *
     * @return Returns all menu items associated with a Plugin, or null if there are none.
     */
    Collection<MenuItem> getMenuItems();

    /**
     * Allows a Plugin to provide its own Image for display in menus and the About dialog.
     *
     * This method does not need to return an image of the given size, merely an image that can be displayed at that size.  The caller will scale the image appropriately.
     *
     * The About dialog requests a 64px image.  The icon for a Menu asks for a 14px image.
     *
     * @param pixels The size (width and height) that will be used to display the image.
     * @return Returns null if there is no image, or an Image that can be scaled to the given size.
     */
    default Image getImageForSize(final int pixels) {
        return null;
    }
}
