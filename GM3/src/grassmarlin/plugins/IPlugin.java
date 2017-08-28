package grassmarlin.plugins;

import grassmarlin.session.ImportItem;
import grassmarlin.session.Session;
import grassmarlin.session.pipeline.AbstractStage;
import javafx.scene.control.MenuItem;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Serializable;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.concurrent.BlockingQueue;

/**
 * The Plugin interface is used as the entry point to plugins.
 * A zero-argument constructor is required and will be called during application initialization.
 * The Plugin instance is then tested for the different Plugin sub-interfaces that are defined here.
 */
public interface IPlugin {
    /**
     * Annotation for declaring dependencies on other plugins
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
     * Annotation for declaring active plugins
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.TYPE)
    @interface Active { }

    // == Container classes used by plugins ==


    class PipelineStage {
        private final boolean configurable;
        private String name;
        private final Class<? extends AbstractStage<Session>> stage;
        private final Set<String> outputs;

        public PipelineStage(final boolean configurable, final String name, final Class<? extends AbstractStage<Session>> stage, final Collection<String> outputs) {
            this.name = name;
            this.stage = stage;
            this.configurable = configurable;
            this.outputs = new LinkedHashSet<>();
            if(outputs != null) {
                this.outputs.addAll(outputs);
            }
        }
        public PipelineStage(final boolean configurable, final String name, final Class<? extends AbstractStage<Session>> stage, final String... outputs) {
            this(configurable, name, stage, Arrays.asList(outputs));
        }

        public String getName() {
            return this.name;
        }
        public void setName(String name) {
            this.name = name;
        }
        public Class<? extends AbstractStage<Session>> getStage() {
            return this.stage;
        }
        public Set<String> getOutputs() {
            return this.outputs;
        }
        public boolean isConfigurable() {
            return this.configurable;
        }
    }
    abstract class ImportProcessorWrapper {
        private final String name;
        private final String[] extensions;

        //public ImportProcessorWrapper(final String name, final Class<? extends ImportItem.ImportProcess<?>> processor, final String... extensions) {
        public ImportProcessorWrapper(final String name, final String... extensions) {
            this.name = name;
            this.extensions = extensions;
        }

        public String getName() {
            return this.name;
        }
        public String[] getExtensions() {
            return extensions;
        }
        public abstract Iterator<Object> getProcessor(final ImportItem item, final Session session);
        public abstract boolean itemIsValidTarget(final ImportItem item);

        @Override
        public String toString() {
            return name;
        }
    }

    @FunctionalInterface
    interface FactoryPacketHandler {
        IPacketHandler create(final ImportItem source, final BlockingQueue<Object> packetQueue);
    }
    interface IPacketHandler {
        int handle(final ByteBuffer bufPacket, final long msSinceEpoch, final int idxFrame);
    }

    // == IPlugin behaviors
    interface HasClassFactory extends IPlugin {
        interface ClassFactory<T> {
            String getFactoryName();
            Class<T> getFactoryClass();
            T createInstance(final String text);
            boolean validateText(final String text);
        }
        Collection<ClassFactory<?>> getClassFactories();
    }
    interface HasImportProcessors extends IPlugin {
        Collection<ImportProcessorWrapper> getImportProcessors();
        Map<Integer, FactoryPacketHandler> getPcapHandlerFactories();
    }
    interface SessionEventHooks extends IPlugin {
        void sessionCreated(final grassmarlin.session.Session session, final grassmarlin.ui.common.TabController tabs);
        void sessionClosed(final grassmarlin.session.Session session);
    }
    interface SessionSerialization extends IPlugin {
        @FunctionalInterface
        interface CallbackCreateStream {
            OutputStream getStream(final String name, final boolean buffered) throws IOException;
        }

        @FunctionalInterface
        interface GetChildStream {
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

    /**
     * returns false if the arguments were invalid, true otherwise.  This does not affect the loading of the plugin, but will produce a warning message.
     */
    interface AcceptsCommandLineArguments extends IPlugin {
        boolean processArg(final String arg);
    }

    // == IPlugin.HasVersionInfo
    interface HasVersionInfo extends IPlugin {
        String getVersion();
    }

    // == Core IPlugin
    //There should be a constructor that takes a RuntimeConfiguration as the only parameter.
    String getName();
    Collection<MenuItem> getMenuItems();
}
