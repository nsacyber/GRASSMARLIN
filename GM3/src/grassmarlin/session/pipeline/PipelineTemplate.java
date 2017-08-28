package grassmarlin.session.pipeline;

import com.sun.istack.internal.NotNull;
import com.sun.xml.internal.txw2.output.IndentingXMLStreamWriter;
import grassmarlin.Logger;
import grassmarlin.RuntimeConfiguration;
import grassmarlin.plugins.IPlugin;
import grassmarlin.plugins.serialization.PluginSerializableWrapper;

import javax.xml.stream.*;
import java.io.*;
import java.util.*;

public class PipelineTemplate {

    private final String name;

    private List<IPlugin.PipelineStage> stages;
    private List<PipelineStageConnection> connections;
    private Map<String, List<IPlugin.PipelineStage>> entryPoints;
    private Map<String, Serializable> configuration;

    public PipelineTemplate(String name) {
        this.name = name;

        this.stages = new ArrayList<>();
        this.connections = new ArrayList<>();
        this.entryPoints = new HashMap<>();
        this.configuration = new HashMap<>();
    }

    public PipelineTemplate(String name, PipelineTemplate template) {
        this(name);

        this.stages.addAll(template.getStages());
        this.connections.addAll(template.getConnections());
        for (Map.Entry<String, List<IPlugin.PipelineStage>> entry : template.getEntryPoints().entrySet()) {
            List<IPlugin.PipelineStage> copy = new ArrayList<>(entry.getValue());
            this.entryPoints.put(entry.getKey(), copy);
        }
        this.configuration.putAll(template.getConfiguration());
    }

    public String getName() {
        return this.name;
    }

    public boolean addStage(@NotNull final IPlugin.PipelineStage stage) {
        if (stage != null) {
            return this.stages.add(stage);
        } else {
            return false;
        }
    }
    public boolean addStages(final IPlugin.PipelineStage... stages) {
        boolean result = false;
        for(final IPlugin.PipelineStage stage : stages) {
            result |= addStage(stage);
        }
        return result;
    }
    public void addStage(int index, @NotNull final IPlugin.PipelineStage stage) {
        if (stage != null && index >= 0) {
            if (index < this.stages.size()) {
                this.stages.add(index, stage);
            } else {
                for (int i = 0; i < index - this.stages.size(); i++) {
                    this.stages.add(null);
                }
                this.stages.add(stage);
            }
        }
    }


    public boolean addConnection(PipelineStageConnection connection) {
        if (connection != null) {
            if (!stages.contains(connection.getSourceStage())) {
                stages.add(connection.getSourceStage());
            }
            if (!stages.contains(connection.getDestStage())) {
                stages.add(connection.getDestStage());
            }

            return this.connections.add(connection);
        } else {
            return false;
        }
    }

    public boolean addConnection(final int idxSource, final String output, final int idxDestination) {
        return this.addConnection(this.stages.get(idxSource), output, this.stages.get(idxDestination));
    }
    public boolean addConnection(IPlugin.PipelineStage source, String output, IPlugin.PipelineStage dest) {
        return this.addConnection(new PipelineStageConnection(source, output, dest));
    }

    public boolean removeConnection(final PipelineStageConnection connection) {
        return this.connections.remove(connection);
    }

    public PipelineStageConnection removeConnection(final int idxConnection) {
        return this.connections.remove(idxConnection);
    }

    public boolean removeConnection(IPlugin.PipelineStage source, String output, IPlugin.PipelineStage dest) {
        Optional<PipelineStageConnection> connection = this.connections.stream()
                .filter(con -> con.getSourceStage().equals(source) && con.getOutput().equals(output) && con.getDestStage().equals(dest))
                .findFirst();

        if (connection.isPresent()) {
            return this.connections.remove(connection.get());
        } else {
            return false;
        }
    }

    public boolean removeStage(IPlugin.PipelineStage stage) {
        return this.stages.remove(stage);
    }

    public boolean addEntryPoint(String name, IPlugin.PipelineStage... stageList) {
        List<IPlugin.PipelineStage> stages = this.entryPoints.get(name);
        if (stages == null) {
            stages = new ArrayList<>();
            this.entryPoints.put(name, stages);
        }
        if (stageList != null) {
            return stages.addAll(Arrays.asList(stageList));
        } else {
            return true;
        }
    }

    public List<IPlugin.PipelineStage> removeEntryPoint(String name) {
        return this.entryPoints.remove(name);
    }

    public boolean removeEntryPointConnection(String name, IPlugin.PipelineStage stage) {
        List<IPlugin.PipelineStage> stages = this.entryPoints.get(name);
        if (stages != null) {
            return stages.remove(stage);
        } else {
            return false;
        }
    }

    public List<IPlugin.PipelineStage> getStages() {
        return Collections.unmodifiableList(this.stages);
    }

    public List<PipelineStageConnection> getConnections() {
        return Collections.unmodifiableList(this.connections);
    }

    public Map<String, List<IPlugin.PipelineStage>> getEntryPoints() {
        return Collections.unmodifiableMap(this.entryPoints);
    }

    public Map<String, Serializable> getConfiguration() {
        return this.configuration;
    }

    public Serializable getStageConfiguration(String stageName) {
        return this.configuration.get(stageName);
    }

    public void setConfiguration(String stageName, Serializable configuration) {
        this.configuration.put(stageName, configuration);
    }

    /**
     * This method checks to see if there are any stages with the given name, and if there are not,
     * deletes the configuration associated with that name.
     *
     * @param stageName The name of the stage that was renamed or deleted
     */
    public void cleanConfiguration(String stageName) {
        Optional<IPlugin.PipelineStage> found = this.stages.stream()
                .filter(stage -> stage.getName().equals(stageName))
                .findFirst();

        if (!found.isPresent()) {
            this.configuration.remove(stageName);
        }
    }

    public static class PipelineElements {
        public static final String TEMPLATE = "template";
        public static final String STAGES = "stages";
        public static final String STAGE = "stage";
        public static final String CONFIGURATION = "configuration";
        public static final String CONFIGURABLE = "configurable";
        public static final String INDEX = "index";
        public static final String NAME = "name";
        public static final String PLUGIN = "plugin";
        public static final String CLASS = "class";
        public static final String OUTPUTS = "outputs";
        public static final String OUTPUT = "output";
        public static final String CONNECTIONS = "connections";
        public static final String CONNECTION = "connection";
        public static final String SOURCE = "source";
        public static final String DEST = "destination";
        public static final String ENTRYPOINTS = "entrypoints";
        public static final String ENTRYPOINT = "entrypoint";
    }

    public static void saveTemplate(RuntimeConfiguration config, PipelineTemplate template, OutputStream out) throws XMLStreamException{
        XMLOutputFactory xof = XMLOutputFactory.newInstance();
        XMLStreamWriter writer = new IndentingXMLStreamWriter(xof.createXMLStreamWriter(out));

        writer.writeStartDocument();

        //start template
        writer.writeStartElement(PipelineElements.TEMPLATE);
        writer.writeAttribute(PipelineElements.NAME, template.getName());

        //start configuration
        writer.writeStartElement(PipelineElements.CONFIGURATION);
        Map<String, PluginSerializableWrapper> wrapperMap = new HashMap<>();
        for (Map.Entry<String, Serializable> entry : template.getConfiguration().entrySet()) {
            wrapperMap.put(entry.getKey(), new PluginSerializableWrapper(RuntimeConfiguration.pluginNameFor(entry.getValue().getClass()), entry.getValue()));
        }
        ByteArrayOutputStream configOut = new ByteArrayOutputStream();
        try {
            ObjectOutputStream objOut = new ObjectOutputStream(configOut);
            objOut.writeObject(wrapperMap);
            objOut.close();
            configOut.close();
            String configAsHex = javax.xml.bind.DatatypeConverter.printHexBinary(configOut.toByteArray());
            writer.writeCharacters(configAsHex);
        } catch (IOException ioe) {
            Logger.log(Logger.Severity.ERROR, "Unable to save configuration for template %s", template.getName());
        }

        //end configuration
        writer.writeEndElement();

        //start stage list
        writer.writeStartElement(PipelineElements.STAGES);
        int i;
        IPlugin.PipelineStage stage;
        for (i = 0; i < template.getStages().size(); i++) {
            stage = template.getStages().get(i);

            //start stage
            writer.writeStartElement(PipelineElements.STAGE);
            writer.writeAttribute(PipelineElements.INDEX, Integer.toString(i));
            writer.writeAttribute(PipelineElements.NAME, stage.getName());
            writer.writeAttribute(PipelineElements.PLUGIN, RuntimeConfiguration.pluginNameFor(stage.getStage()));
            writer.writeAttribute(PipelineElements.CLASS, stage.getStage().getCanonicalName());
            writer.writeAttribute(PipelineElements.CONFIGURABLE, Boolean.toString(stage.isConfigurable()));

            //start output list
            writer.writeStartElement(PipelineElements.OUTPUTS);
            for (String output : stage.getOutputs()) {
                //start output
                writer.writeStartElement(PipelineElements.OUTPUT);
                writer.writeCharacters(output);
                //end output
                writer.writeEndElement();
            }
            //end output list
            writer.writeEndElement();
            //end stage
            writer.writeEndElement();
        }
        //end stage list
        writer.writeEndElement();

        //start entry point list
        writer.writeStartElement(PipelineElements.ENTRYPOINTS);

        for (Map.Entry<String, List<IPlugin.PipelineStage>> entry : template.entryPoints.entrySet()) {
            //start entry point
            writer.writeStartElement(PipelineElements.ENTRYPOINT);
            writer.writeAttribute(PipelineElements.NAME, entry.getKey());

            //start connected stage list
            writer.writeStartElement(PipelineElements.STAGES);
            for (IPlugin.PipelineStage ps : entry.getValue()) {
                //start connected stage
                writer.writeStartElement(PipelineElements.STAGE);
                writer.writeAttribute(PipelineElements.INDEX, Integer.toString(template.getStages().indexOf(ps)));
                writer.writeAttribute(PipelineElements.PLUGIN, RuntimeConfiguration.pluginNameFor(ps.getStage()));
                writer.writeAttribute(PipelineElements.CLASS, ps.getStage().getCanonicalName());
                //end connected stage
                writer.writeEndElement();
            }
            //end connected stage list
            writer.writeEndElement();
            //end entry point
            writer.writeEndElement();
        }
        //end entry point list
        writer.writeEndElement();

        //start connection list
        writer.writeStartElement(PipelineElements.CONNECTIONS);
        for (PipelineStageConnection connection : template.getConnections()) {
            //start connection
            writer.writeStartElement(PipelineElements.CONNECTION);
            //start source
            writer.writeStartElement(PipelineElements.SOURCE);
            IPlugin.PipelineStage source = connection.getSourceStage();
            writer.writeAttribute(PipelineElements.INDEX, Integer.toString(template.getStages().indexOf(source)));
            writer.writeAttribute(PipelineElements.NAME, source.getName());
            writer.writeAttribute(PipelineElements.PLUGIN, RuntimeConfiguration.pluginNameFor(source.getStage()));
            writer.writeAttribute(PipelineElements.CLASS, source.getStage().getCanonicalName());
            //end source
            writer.writeEndElement();

            //start dest
            writer.writeStartElement(PipelineElements.DEST);
            IPlugin.PipelineStage dest = connection.getDestStage();
            writer.writeAttribute(PipelineElements.INDEX, Integer.toString(template.getStages().indexOf(dest)));
            writer.writeAttribute(PipelineElements.NAME, dest.getName());
            writer.writeAttribute(PipelineElements.PLUGIN, RuntimeConfiguration.pluginNameFor(dest.getStage()));
            writer.writeAttribute(PipelineElements.CLASS, dest.getStage().getCanonicalName());
            //end dest
            writer.writeEndElement();

            writer.writeStartElement(PipelineElements.OUTPUT);
            writer.writeCharacters(connection.getOutput());
            writer.writeEndElement();

            //end connection
            writer.writeEndElement();
        }
        //end connection list
        writer.writeEndElement();

        //end template
        writer.writeEndElement();

        writer.writeEndDocument();

        writer.close();
    }

    public static PipelineTemplate loadTemplate(RuntimeConfiguration config, InputStream in) throws XMLStreamException, ClassNotFoundException{
        PipelineTemplate template = null;
        XMLInputFactory xif = XMLInputFactory.newFactory();
        XMLStreamReader reader = xif.createXMLStreamReader(in);

        while (reader.hasNext() ) {
            switch (reader.next()) {
                case XMLStreamReader.START_ELEMENT:
                    switch (reader.getLocalName()) {
                        case PipelineElements.TEMPLATE:
                            String name = reader.getAttributeValue(null, PipelineElements.NAME);
                            template = new PipelineTemplate(name);
                            break;
                        case PipelineElements.CONFIGURATION:
                            readConfiguration(template, reader);
                            break;
                        case PipelineElements.STAGES:
                            readStages(config, template, reader);
                            break;
                        case PipelineElements.ENTRYPOINTS:
                            readEntryPoints(config, template, reader);
                            break;
                        case PipelineElements.CONNECTIONS:
                            readConnections(config, template, reader);
                            break;
                        default:
                            throw new XMLStreamException("Unexpected Element " + reader.getLocalName());
                    }
                    break;
                case XMLStreamReader.END_ELEMENT:
                    switch (reader.getLocalName()) {
                        case PipelineElements.TEMPLATE:
                            break;
                        default:
                            throw new XMLStreamException("Unexpected End of Element " + reader.getLocalName());
                    }
                    break;
            }
        }

        reader.close();

        return template;
    }

    private static void readStages(RuntimeConfiguration config, PipelineTemplate template, XMLStreamReader reader) throws XMLStreamException, ClassNotFoundException {
        while (reader.hasNext()) {
            switch (reader.next()) {
                case XMLStreamReader.START_ELEMENT:
                    switch (reader.getLocalName()) {
                        case PipelineElements.STAGE:
                            readStage(config, template, reader);
                            break;
                        default:
                            throw new XMLStreamException("Unexpected Element " + reader.getLocalName());
                    }
                    break;
                case XMLStreamReader.END_ELEMENT:
                    switch (reader.getLocalName()) {
                        case PipelineElements.STAGES:
                            return;
                        case PipelineElements.STAGE:
                            break;
                        default:
                            throw new XMLStreamException("Unexpected End of Element " + reader.getLocalName());
                    }
            }
        }
    }

    private static void readStage(RuntimeConfiguration config, PipelineTemplate template, XMLStreamReader reader) throws XMLStreamException, ClassNotFoundException {
        String name;
        String className;
        String plugin;
        List<String> outputs;
        boolean configurable = false;

        int index = Integer.parseInt(reader.getAttributeValue(null, PipelineElements.INDEX));
        name = reader.getAttributeValue(null, PipelineElements.NAME);

        plugin = reader.getAttributeValue(null, PipelineElements.PLUGIN);
        className = reader.getAttributeValue(null, PipelineElements.CLASS);
        configurable = Boolean.parseBoolean(reader.getAttributeValue(null, PipelineElements.CONFIGURABLE));

        outputs = new ArrayList<>();

        while (reader.hasNext()) {
            switch (reader.next()) {
                case XMLStreamReader.START_ELEMENT:
                    switch (reader.getLocalName()) {
                        case PipelineElements.OUTPUTS:
                            readOutputs(outputs, reader);
                            break;
                        default:
                            throw new XMLStreamException("Unexpected Element " + reader.getLocalName());
                    }
                    break;
                case XMLStreamReader.END_ELEMENT:
                    switch (reader.getLocalName()) {
                        case PipelineElements.STAGE:
                            if (name != null && className != null) {
                                IPlugin.PipelineStage passedStage = testStage(config, className, plugin, outputs);
                                if (passedStage != null) {
                                    Class clazz = config.getLoader(plugin).loadClass(passedStage.getStage().getCanonicalName());

                                    IPlugin.PipelineStage stage = new IPlugin.PipelineStage(configurable, name, clazz, outputs);
                                    if (configurable) {
                                        if (template.getStageConfiguration(name) == null) {
                                            template.setConfiguration(name, ((IPlugin.DefinesPipelineStages) config.pluginFor(clazz)).getDefaultConfiguration(stage));
                                        }
                                    }

                                    template.addStage(index, stage);
                                }
                            }

                            return;
                        default:
                            throw new XMLStreamException("Unexpected End of Element " + reader.getLocalName());
                    }
            }
        }
    }

    private static void readConfiguration(PipelineTemplate template, XMLStreamReader reader) throws XMLStreamException{
        String configAsHex = readCharacters(reader);

        byte[] configAsBytes = javax.xml.bind.DatatypeConverter.parseHexBinary(configAsHex);
        ByteArrayInputStream configIn = new ByteArrayInputStream(configAsBytes);

        try {
            ObjectInputStream objIn = new ObjectInputStream(configIn);
            Map<String, PluginSerializableWrapper> configuration = ((Map<String, PluginSerializableWrapper>) objIn.readObject());
            for (Map.Entry<String, PluginSerializableWrapper> entry : configuration.entrySet()) {
                template.setConfiguration(entry.getKey(), entry.getValue().getWrapped());
            }
        } catch (IOException | ClassNotFoundException e) {
            Logger.log(Logger.Severity.ERROR, "Unable to load configuration for template %s", template.getName());
        }

    }

    private static void readOutputs(List<String> outputs, XMLStreamReader reader) throws XMLStreamException{
        while (reader.hasNext()) {
            switch (reader.next()) {
                case XMLStreamReader.START_ELEMENT:
                    switch (reader.getLocalName()) {
                        case PipelineElements.OUTPUT:
                            outputs.add(readCharacters(reader));
                            break;
                        default:
                            throw new XMLStreamException("Unexpected Element " + reader.getLocalName());
                    }
                    break;
                case XMLStreamReader.END_ELEMENT:
                    switch (reader.getLocalName()) {
                        case PipelineElements.OUTPUTS:
                            return;
                        default:
                            throw new XMLStreamException("Unexpected End of Element " + reader.getLocalName());
                    }
            }
        }
    }

    private static String readCharacters(XMLStreamReader reader) throws XMLStreamException{
        StringBuilder result = new StringBuilder();
        while (reader.hasNext()) {
            switch (reader.next()) {
                case XMLStreamReader.CHARACTERS:
                    result.append(reader.getText());
                    break;
                case XMLStreamReader.END_ELEMENT:
                    return result.toString();
            }
        }

        throw new XMLStreamException("Unexpected End of File");
    }

    private static void readEntryPoints(RuntimeConfiguration config, PipelineTemplate template, XMLStreamReader reader) throws XMLStreamException {
        while (reader.hasNext()) {
            switch (reader.next()) {
                case XMLStreamReader.START_ELEMENT:
                    switch (reader.getLocalName()) {
                        case PipelineElements.ENTRYPOINT:
                            readEntryPoint(config, template, reader);
                            break;
                        default:
                            throw new XMLStreamException("Unexpected Element " + reader.getLocalName());
                    }
                    break;
                case XMLStreamReader.END_ELEMENT:
                    switch (reader.getLocalName()) {
                        case PipelineElements.ENTRYPOINTS:
                            return;
                        default:
                            throw new XMLStreamException("Unexpected End of Element " + reader.getLocalName());
                    }
            }
        }
    }

    private static void readEntryPoint(RuntimeConfiguration config, PipelineTemplate template, XMLStreamReader reader) throws XMLStreamException {
        String name = reader.getAttributeValue(null, PipelineElements.NAME);

        List<IPlugin.PipelineStage> connectedStages = Collections.emptyList();
        while (reader.hasNext()) {
            switch (reader.next()) {
                case XMLStreamReader.START_ELEMENT:
                    switch (reader.getLocalName()) {
                        case PipelineElements.STAGES:
                            connectedStages = readConnectedStages(config, template, reader);
                            break;
                        default:
                            throw new XMLStreamException("Unexpected Element " + reader.getLocalName());
                    }
                    break;
                case XMLStreamReader.END_ELEMENT:
                    switch (reader.getLocalName()) {
                        case PipelineElements.ENTRYPOINT:
                            template.addEntryPoint(name, connectedStages.toArray(new IPlugin.PipelineStage[connectedStages.size()]));
                            return;
                        default:
                            throw new XMLStreamException("Unexpected End of Element " + reader.getLocalName());
                    }
            }
        }
    }

    private static List<IPlugin.PipelineStage> readConnectedStages(RuntimeConfiguration config, PipelineTemplate template, XMLStreamReader reader) throws XMLStreamException {
        List<IPlugin.PipelineStage> stages = new ArrayList<>();

        while (reader.hasNext()) {
            switch (reader.next()) {
                case XMLStreamReader.START_ELEMENT:
                    switch (reader.getLocalName()) {
                        case PipelineElements.STAGE:
                            IPlugin.PipelineStage stage = readConnectedStage(config, template, reader);
                            if (stage != null) {
                                stages.add(stage);
                            }
                            break;
                        default:
                            throw new XMLStreamException("Unexpected Element " + reader.getLocalName());
                    }
                    break;
                case XMLStreamReader.END_ELEMENT:
                    switch (reader.getLocalName()) {
                        case PipelineElements.STAGE:
                            break;
                        case PipelineElements.STAGES:
                            return stages;
                        default:
                            throw new XMLStreamException("Unexpected End of Element " + reader.getLocalName());
                    }
                    break;
            }
        }

        return Collections.emptyList();
    }

    private static IPlugin.PipelineStage readConnectedStage(RuntimeConfiguration config, PipelineTemplate template, XMLStreamReader reader) throws XMLStreamException {
        int index = Integer.parseInt(reader.getAttributeValue(null, PipelineElements.INDEX));
        String pluginName = reader.getAttributeValue(null, PipelineElements.PLUGIN);
        String className = reader.getAttributeValue(null, PipelineElements.CLASS);

        IPlugin.PipelineStage stage = null;
        if (index < template.getStages().size()) {
            stage = template.getStages().get(index);
        }

        if (stage != null && RuntimeConfiguration.pluginNameFor(stage.getStage()).equals(pluginName) && stage.getStage().getCanonicalName().equals(className)) {
            return stage;
        } else {
            Logger.log(Logger.Severity.WARNING, "Could not reconnect Entry Point to " + className + " in Plugin " + pluginName);
            return null;
        }
    }

    private static void readConnections(RuntimeConfiguration config, PipelineTemplate template, XMLStreamReader reader) throws XMLStreamException {
        while (reader.hasNext()) {
            switch (reader.next()) {
                case XMLStreamReader.START_ELEMENT:
                    switch (reader.getLocalName()) {
                        case PipelineElements.CONNECTION:
                            readConnection(config, template, reader);
                            break;
                        default:
                            throw new XMLStreamException("Unexpected Element " + reader.getLocalName());
                    }
                    break;
                case XMLStreamReader.END_ELEMENT:
                    switch (reader.getLocalName()) {
                        case PipelineElements.CONNECTIONS:
                            return;
                        default:
                            throw new XMLStreamException("Unexpected End of Element " + reader.getLocalName());
                    }
            }
        }
    }

    private static void readConnection(RuntimeConfiguration config, PipelineTemplate template, XMLStreamReader reader) throws XMLStreamException{
        IPlugin.PipelineStage source = null;
        IPlugin.PipelineStage dest = null;
        String output = null;

        while(reader.hasNext()) {
            switch (reader.next()) {
                case XMLStreamReader.START_ELEMENT:
                    switch (reader.getLocalName()) {
                        case PipelineElements.SOURCE:
                            int sourceIndex = Integer.parseInt(reader.getAttributeValue(null, PipelineElements.INDEX));
                            if (sourceIndex < template.getStages().size()) {
                                source = template.getStages().get(sourceIndex);
                            }

                            break;
                        case PipelineElements.DEST:
                            int destIndex = Integer.parseInt(reader.getAttributeValue(null, PipelineElements.INDEX));
                            if (destIndex < template.getStages().size()) {
                                dest = template.getStages().get(destIndex);
                            }

                            break;
                        case PipelineElements.OUTPUT:
                            output = readCharacters(reader);
                            break;
                        default:
                            throw new XMLStreamException("Unexpected Element " + reader.getLocalName());
                    }
                    break;
                case XMLStreamReader.END_ELEMENT:
                    switch (reader.getLocalName()) {
                        case PipelineElements.SOURCE:
                        case PipelineElements.DEST:
                            break;
                        case PipelineElements.CONNECTION:
                            if (source != null && dest != null && output != null && source.getOutputs().contains(output)) {
                                template.addConnection(source, output, dest);
                            } else if (source != null && output != null && !source.getOutputs().contains(output)) {
                                Logger.log(Logger.Severity.WARNING, "Stage %s no longer contains output %s", source.getName(), output);
                            }
                            return;
                        default:
                            throw new XMLStreamException("Unexpected End of Element " + reader.getLocalName());
                    }
                    break;
            }
        }
    }

    private static IPlugin.PipelineStage testStage(final RuntimeConfiguration config, final String className, final String pluginName, final Collection<String> outputs) {
        IPlugin.PipelineStage passedStage = null;

        IPlugin plugin = config.pluginFor(pluginName);
        if (plugin != null) {
            if (plugin instanceof IPlugin.DefinesPipelineStages) {
                Optional<IPlugin.PipelineStage> stageOptionalClass = ((IPlugin.DefinesPipelineStages) plugin).getPipelineStages().stream()
                        .filter(stage -> stage.getStage().getCanonicalName().equals(className))
                        .findFirst();

               if (stageOptionalClass.isPresent()) {
                    try {
                        RuntimeConfiguration.getLoader(pluginName).loadClass(className);
                        if (!stageOptionalClass.get().getOutputs().containsAll(outputs)) {
                            Logger.log(Logger.Severity.WARNING, "Stage defined by class %s no longer contains the same outputs", className);
                        }
                        passedStage = stageOptionalClass.get();
                    } catch (ClassNotFoundException e) {
                        Logger.log(Logger.Severity.ERROR, "Could not load class %s from plugin %s: %s", className, pluginName, e.getMessage());
                    }
                } else {
                    Logger.log(Logger.Severity.ERROR, "Plugin %s no longer defines a stage with class %s", pluginName, className);
                }
            } else {
                Logger.log(Logger.Severity.ERROR, "Plugin %s no longer defines any Pipeline Stages", pluginName);
            }
        } else {
            Logger.log(Logger.Severity.ERROR, "No such Plugin: %s", pluginName);
        }
        return passedStage;
    }

}
