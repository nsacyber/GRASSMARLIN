package grassmarlin.session.serialization;

import grassmarlin.Logger;
import grassmarlin.RuntimeConfiguration;
import grassmarlin.plugins.IPlugin;
import grassmarlin.session.ImportItem;
import grassmarlin.session.pipeline.Pipeline;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

public class Quicklist {
    private final List<ImportItem> items;

    public Quicklist() {
        this.items = new ArrayList<>();
    }
    public Quicklist(final Collection<ImportItem> items) {
        this.items = new ArrayList<>(items);
    }

    public List<ImportItem> getItems() {
        return this.items;
    }
    public boolean readFromStream(final RuntimeConfiguration config, final InputStream stream) {
        items.clear();

        try(final BufferedReader reader = new BufferedReader(new InputStreamReader(stream))) {
            //TODO: Check for earlier quicklist formats.
            final String lineFirst = reader.readLine();
            if(lineFirst.equals("#Version:3.2.0")) {
                return readFromStream_3_2_0(config, stream);
            } else if(lineFirst.equals("#Version:3.3.0")) {
                return readFromStream_3_3_0(config, reader);
            } else {
                Logger.log(Logger.Severity.ERROR, "Unable to read Quicklist: Unknown Format");
                return false;
            }
        } catch(final IOException ex) {
            Logger.log(Logger.Severity.ERROR, "Unable to read Quicklist: %s", ex.getMessage());
            return false;
        }
    }

    /**
     * The 3.2 quicklist format is based on the java.util.Properties class.
     *
     * @param config The RuntimeConfiguration that will be used to resolve plugins.
     * @param stream The stream from which to read.
     * @return Always returns true.
     * @throws IOException
     */
    private boolean readFromStream_3_2_0(final RuntimeConfiguration config, final InputStream stream) throws IOException {
        final Properties contents = new Properties();
        contents.load(stream);
        for(final Map.Entry<Object, Object> entry : contents.entrySet()) {
            final Path path = Paths.get(entry.getKey().toString());
            final IPlugin.ImportProcessorWrapper wrapper = config.importerForName(entry.getValue().toString());
            final IPlugin plugin = config.pluginFor(wrapper.getClass());

            if(plugin instanceof IPlugin.HasImportProcessors) {
                final ImportItem.FromPlugin temp = new ImportItem.FromPlugin(path, Pipeline.ENTRY_DEFAULT);
                temp.importerPluginNameProperty().set(config.pluginNameFor(wrapper.getClass()));
                temp.importerFunctionNameProperty().set(wrapper.getName());
                items.add(temp);
            } else {
                Logger.log(Logger.Severity.WARNING, "There was an error loading import type '%s' from plugin '%s'", entry.getValue(), plugin.getName());
            }
        }
        return true;
    }

    private boolean readFromStream_3_3_0(final RuntimeConfiguration config, final BufferedReader reader) throws IOException {
        //We've already read the version line
        String line;
        while((line = reader.readLine()) != null) {
            final String[] b64Tokens = line.split(",");
            final Path path = Paths.get(new String(Base64.getDecoder().decode(b64Tokens[0]), StandardCharsets.UTF_8));
            final IPlugin plugin = config.pluginFor(new String(Base64.getDecoder().decode(b64Tokens[1]), StandardCharsets.UTF_8));
            final IPlugin.ImportProcessorWrapper wrapper = config.importerForName(new String(Base64.getDecoder().decode(b64Tokens[2]), StandardCharsets.UTF_8));
            final String entryPoint = new String(Base64.getDecoder().decode(b64Tokens[3]), StandardCharsets.UTF_8);

            //Integrity check: if the plugin that was chosen for the name is not the plugin for the processor, we have an error.
            if(config.pluginFor(wrapper.getClass()) != plugin) {
                Logger.log(Logger.Severity.ERROR, "Plugin mismatch while loading quicklist.");
                return false;
            }
            if(plugin instanceof IPlugin.HasImportProcessors) {
                final ImportItem.FromPlugin temp = new ImportItem.FromPlugin(path, entryPoint);
                temp.importerPluginNameProperty().set(config.pluginNameFor(wrapper.getClass()));
                temp.importerFunctionNameProperty().set(wrapper.getName());

                items.add(temp);
            } else {
                Logger.log(Logger.Severity.WARNING, "There was an error loading import type '%s' from plugin '%s'", new String(Base64.getDecoder().decode(b64Tokens[2]), StandardCharsets.UTF_8), plugin.getName());
            }
        }

        return true;
    }

    public void writeToStream(final OutputStream stream) throws IOException {
        try(BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(stream))) {
            writer.write("#Version:3.3.0");

            for(final ImportItem item : this.items) {
                final String b64Path = Base64.getEncoder().encodeToString(item.getPath().toAbsolutePath().toString().getBytes(StandardCharsets.UTF_8));
                //HACK: Need to fix this
                /*
                final String b64Plugin = Base64.getEncoder().encodeToString(item.getPlugin().getClass().getPackage().getName().getBytes(StandardCharsets.UTF_8));
                final String b64Processor = Base64.getEncoder().encodeToString(item.processorWrapperProperty().get().getName().getBytes(StandardCharsets.UTF_8));
                final String b64Entry = Base64.getEncoder().encodeToString(item.getPipelineEntry().getBytes(StandardCharsets.UTF_8));
                writer.write(String.format("\n%s,%s,%s,%s", b64Path, b64Plugin, b64Processor, b64Entry));
                */
            }

            if(this.items.isEmpty() || this.items.contains(null)) {
                Logger.log(Logger.Severity.ERROR, "Quicklist was empty or contained one or more null entries.");
            }
        }
    }
}
