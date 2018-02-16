package grassmarlin.session;

import com.sun.istack.internal.NotNull;
import com.sun.javafx.collections.ObservableListWrapper;
import grassmarlin.Event;
import grassmarlin.Logger;
import grassmarlin.Plugin;
import grassmarlin.RuntimeConfiguration;
import grassmarlin.plugins.IPlugin;
import grassmarlin.session.pipeline.LogicalAddressMapping;
import grassmarlin.session.pipeline.Network;
import grassmarlin.session.pipeline.Pipeline;
import grassmarlin.session.pipeline.PipelineTemplate;
import grassmarlin.session.serialization.Loader;
import grassmarlin.session.serialization.Writer;
import grassmarlin.session.serialization.XmlSerializable;
import javafx.beans.InvalidationListener;
import javafx.beans.binding.BooleanExpression;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.ObservableList;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.stream.events.XMLEvent;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

/**
 * WARNING:  It may be necessary to operate on the set of vertices and the set of edges simultaneously, necessitating locking both entities.  When this happens, always nest the lock on the edges within the lock on the vertices.
 * NOTE: If Session is made non-final, there are method references that should be replaced with lambdas and method calls.
 */
public final class Session implements AutoCloseable, XmlSerializable {
    // Support Classes
    private static class AddressPair<T> {
        private final T source;
        private final T destination;

        protected AddressPair(final T source, final T destination) {
            this.source = source;
            this.destination = destination;
        }

        public T getSource() {
            return this.source;
        }
        public T getDestination() {
            return this.destination;
        }

        @Override
        public int hashCode() {
            return this.source.hashCode() ^ this.destination.hashCode();
        }

        @Override
        public boolean equals(Object other) {
            if(other != null && other.getClass() == this.getClass()) {
                return (this.source.equals(((AddressPair)other).source) && this.destination.equals(((AddressPair)other).destination))
                        ||
                        (this.source.equals(((AddressPair)other).destination) && this.destination.equals(((AddressPair)other).source));
            } else {
                return false;
            }
        }


        @Override
        public String toString() {
            return String.format("%s <-> %s", source, destination);
        }
    }
    public static final class LogicalAddressPair extends AddressPair<LogicalAddressMapping> {
        public LogicalAddressPair(final LogicalAddressMapping source, final LogicalAddressMapping destination) {
            super(source, destination);
        }
    }
    public static final class HardwareAddressPair extends AddressPair<HardwareAddress> {
        public HardwareAddressPair(final HardwareAddress source, final HardwareAddress destination) {
            super(source, destination);
        }
    }

    // Events
    public final Event<HardwareVertex> onHardwareVertexCreated;
    public final Event<LogicalVertex> onLogicalVertexCreated;
    public final Event<LogicalConnection> onLogicalConnectionCreated;
    public final Event<PhysicalConnection> onPhysicalConnectionCreated;

    public final Event<List<Network>> onNetworkChange;
    public final Event<Session> onSessionModified;

    //Data Flow / Store
    private final ObservableList<ImportItem> importsAll;
    private final SimpleStringProperty livePcapEntry;
    //HACK: Loading code needs to access the NetworkList directly
    final NetworkList networks;
    private final Map<HardwareAddress, HardwareVertex> lookupHardwareVertices;
    private final Map<LogicalAddressMapping, LogicalVertex> lookupLogicalVertices;
    private final Map<LogicalAddressPair, LogicalConnection> lookupLogicalConnections;
    private final Map<HardwareAddressPair, PhysicalConnection> lookupPhysicalConnections;

    //Configuration
    private final RuntimeConfiguration config;
    private final Event.IAsyncExecutionProvider providerExec;

    public Session(RuntimeConfiguration config) {
        this.config = config;
        //All changes within the session will be run through a single worker thread to resolve sync issues.  All events will run from this thread.
        this.providerExec = Event.PROVIDER_IN_THREAD;

        //Init Properties
        this.livePcapEntry = new SimpleStringProperty(Pipeline.ENTRY_DEFAULT);

        //State data
        this.networks = new NetworkList(this.providerExec);
        this.lookupHardwareVertices = new ConcurrentHashMap<>();
        this.lookupLogicalVertices = new ConcurrentHashMap<>();
        this.lookupLogicalConnections = new ConcurrentHashMap<>();
        this.lookupPhysicalConnections = new ConcurrentHashMap<>();
        this.importsAll = new ObservableListWrapper<>(new ArrayList<>());

        //Events
        this.onHardwareVertexCreated = new Event<>(this.providerExec);
        this.onLogicalVertexCreated = new Event<>(this.providerExec);
        this.onLogicalConnectionCreated = new Event<>(this.providerExec);
        this.onPhysicalConnectionCreated = new Event<>(this.providerExec);
        //Events from child objects
        this.onNetworkChange = this.networks.onNetworksUpdated;
        //The OnModified handler can fire in any thread since it will ultimately just trigger an invalidation in the SingleDocumentState.
        this.onSessionModified = new Event<>(Event.PROVIDER_IN_THREAD);

        this.importsAll.addListener(new InvalidationListener() {
            @Override
            public void invalidated(javafx.beans.Observable observable) {
                if(Session.this.pipelineProperty().get() == null) {
                    Session.this.pipelineProperty().set(Pipeline.buildPipelineFromTemplate(Session.this.config, Session.this, Session.this.pipelineTemplateProperty().get()));
                }
            }
        });
    }

    public Event.IAsyncExecutionProvider getExecutionProvider() {
        return this.providerExec;
    }

    public void markAsModified() {
        onSessionModified.call(this);
    }

    // == IMPORT ==============================================================

    public void processImport(final ImportItem source) {
        if(!importsAll.contains(source)) {
            //If we're trying to process something that is not already in the list, then add it.
            importsAll.add(source);
        }
        if(source.importStartedProperty().get()) {
            //It is already being processed, so there is nothing to do here.
        } else {
            try {
                final String entry = source.pipelineEntryProperty().get();
                if(!getPipelineEntryPoints().contains(entry)) {
                    Logger.log(Logger.Severity.WARNING, "Import failed: Entry '%s' is not defined.", entry);
                    return;
                }

                final IPlugin.ImportProcessorWrapper importer;
                if(source.importerPluginNameProperty().get().equals(Plugin.NAME) && source.importerFunctionNameProperty().get().equals(grassmarlin.plugins.internal.livepcap.Plugin.wrapperLivePcap.getName())) {
                    importer = grassmarlin.plugins.internal.livepcap.Plugin.wrapperLivePcap;
                } else {
                    importer = ((IPlugin.HasImportProcessors) config.pluginFor(source.importerPluginNameProperty().get())).getImportProcessors().stream().filter(wrapper -> wrapper.getName().equals(source.importerFunctionNameProperty().get())).findAny().orElse(null);
                }

                // While typing this section, I mis-typed "iterator" as "oterator" several times.  I mention this only because I think the world needs to see the Terminator franchise re-imaged with otters.  Internet, please make this happen.
                final Iterator<Object> iterator = importer.getProcessor(source, this);

                if(iterator != null) {
                    pipelineProperty.get().startImport(entry, iterator);
                } else {
                    Logger.log(Logger.Severity.ERROR, "There was an error starting the import of [%s]", source);
                }
            } catch(Exception ex) {
                //We expect that an error has already been logged.
            }
        }
    }

    public List<String> getPipelineEntryPoints() {
        if(this.pipelineTemplateProperty().get() == null) {
            return new ArrayList<>();
        } else {
            if(this.pipelineTemplateProperty().get() != null) {
                return new ArrayList<>(this.pipelineTemplateProperty().get().getEntryPoints().keySet());
            } else {
                return new ArrayList<>();
            }
        }
    }
    public String getDefaultPipelineEntry() {
        final Collection<String> entries = getPipelineEntryPoints();
        if(entries.isEmpty() || entries.contains(Pipeline.ENTRY_DEFAULT)) {
            return Pipeline.ENTRY_DEFAULT;
        } else {
            return entries.iterator().next();
        }
    }

    @Override
    public void close() throws Exception {
        pipelineProperty().get().close();
    }

    // == Session manipulation ==
    // Hardware Vertices

    /**
     * addHardwareVertex should only be used as part of deserialization; it bypasses the normal redundancy checks.
     * @param hardwareVertex
     */
    private void addHardwareVertex(@NotNull final HardwareVertex hardwareVertex) {
        synchronized(lookupHardwareVertices) {
            lookupHardwareVertices.put(hardwareVertex.getAddress(), hardwareVertex);
            onHardwareVertexCreated.call(hardwareVertex);
        }
    }

    public HardwareVertex hardwareVertexFor(final HardwareAddress address) {
        synchronized(lookupHardwareVertices) {
            final HardwareVertex vertExisting = lookupHardwareVertices.get(address);
            if(vertExisting != null) {
                return vertExisting;
            } else {
                final HardwareVertex vertNew = new HardwareVertex(this.providerExec, address);
                lookupHardwareVertices.put(address, vertNew);
                onHardwareVertexCreated.call(vertNew);
                return vertNew;
            }
        }
    }

    //Logical Vertices (which are derived from LogicalAddressMappings)

    /**
     * Add avoids performing integrity checks since it is intended for use in load methods, not the pipeline.
     */
    private void addLogicalVertex(final LogicalVertex vertex) {
        synchronized(lookupLogicalVertices) {
            lookupLogicalVertices.put(vertex.getLogicalAddressMapping(), vertex);
            onLogicalVertexCreated.call(vertex);
        }
    }

    public LogicalVertex logicalVertexFor(final LogicalAddressMapping mapping) {
        synchronized(lookupLogicalVertices) {
            final HardwareVertex vertHw;
            synchronized (lookupHardwareVertices) {
                vertHw = lookupHardwareVertices.get(mapping.getHardwareAddress());
            }
            if (vertHw == null) {
                throw new IllegalArgumentException(String.format("Unknown Hardware Address (%s)", mapping.getHardwareAddress()));
            }

            final LogicalVertex vertExisting = lookupLogicalVertices.get(mapping);
            if(vertExisting != null) {
                return vertExisting;
            } else {
                final LogicalVertex vertNew = new LogicalVertex(this.providerExec, this, mapping, vertHw);
                lookupLogicalVertices.put(mapping, vertNew);
                onLogicalVertexCreated.call(vertNew);
                return vertNew;
            }
        }
    }
    public LogicalVertex nonblockingLogicalVertexFor(final LogicalAddressMapping mapping) {
        return lookupLogicalVertices.get(mapping);
    }

    //Logical connections
    private void addLogicalConnection(final LogicalConnection connection) {
        lookupLogicalConnections.put(new LogicalAddressPair(connection.getSource().getLogicalAddressMapping(), connection.getDestination().getLogicalAddressMapping()), connection);
        onLogicalConnectionCreated.call(connection);
    }

    public LogicalConnection existingEdgeBetween(final LogicalAddressMapping source, final LogicalAddressMapping destination) {
        return lookupLogicalConnections.get(new LogicalAddressPair(source, destination));
    }
    public LogicalConnection existingEdgeBetween(final LogicalAddressPair connection) {
        return lookupLogicalConnections.get(connection);
    }
    public void createEdgeBetween(final LogicalAddressMapping source, final LogicalAddressMapping destination) {
        final LogicalConnection edgeNew = new LogicalConnection(this.providerExec, lookupLogicalVertices.get(source), lookupLogicalVertices.get(destination));
        if(null == lookupLogicalConnections.putIfAbsent(new LogicalAddressPair(source, destination), edgeNew)) {
            onLogicalConnectionCreated.call(edgeNew);
        }
    }
    public void createEdgeBetween(final LogicalAddressPair addresses) {
        final LogicalConnection edgeNew = new LogicalConnection(this.providerExec, lookupLogicalVertices.get(addresses.getSource()), lookupLogicalVertices.get(addresses.getDestination()));
        if(null == lookupLogicalConnections.putIfAbsent(addresses, edgeNew)) {
            onLogicalConnectionCreated.call(edgeNew);
        }
    }

    //Physical Connections
    private void addPhysicalConnection(final PhysicalConnection connection) {
        lookupPhysicalConnections.put(new HardwareAddressPair(connection.getSource().getAddress(), connection.getDestination().getAddress()), connection);
        onPhysicalConnectionCreated.call(connection);
    }

    public PhysicalConnection existingEdgeBetween(final HardwareAddress source, final HardwareAddress destination) {
        return lookupPhysicalConnections.get(new HardwareAddressPair(source, destination));
    }
    public PhysicalConnection existingEdgeBetween(final HardwareAddressPair connection) {
        return lookupPhysicalConnections.get(connection);
    }
    public void createEdgeBetween(final HardwareAddress source, final HardwareAddress destination) {
        final PhysicalConnection edgeNew = new PhysicalConnection(this.providerExec, lookupHardwareVertices.get(source), lookupHardwareVertices.get(destination));
        if(null == lookupPhysicalConnections.putIfAbsent(new HardwareAddressPair (source, destination), edgeNew)) {
            onPhysicalConnectionCreated.call(edgeNew);
        }
    }
    public void createEdgeBetween(final HardwareAddressPair addresses) {
        final PhysicalConnection edgeNew = new PhysicalConnection(this.providerExec, lookupHardwareVertices.get(addresses.getSource()), lookupHardwareVertices.get(addresses.getDestination()));
        if(null == lookupPhysicalConnections.putIfAbsent(addresses, edgeNew)) {
            onPhysicalConnectionCreated.call(edgeNew);
        }
    }

    // Networks
    public void addNetwork(final String source, final Network network) {
        this.networks.addNetwork(source, network);
    }
    @Deprecated
    /**
     * See notes for {@code removeNetwork} in {@link grassmarlin.session.NetworkList}
     */
    public void removeNetwork(final String source, final Network network) {
        //noinspection deprecation
        this.networks.removeNetwork(source, network);
    }
    public List<Network> getNetworks() {
        return networks.getCalculatedNetworksCopy();
    }

    public NetworkList getRawNetworkList() {
        return this.networks;
    }

    //Imports
    public ObservableList<ImportItem> allImportsProperty() {
        return importsAll;
    }
    public StringProperty livePcapEntryPointProperty() {
        return this.livePcapEntry;
    }

    // Path / Serialization
    public BooleanExpression isBusy() {
        return pipelineProperty.get().busyProperty();
    }

    // == SERIALIZATION =======================================================

    @Override
    public void writeToXml(final XMLStreamWriter out) throws XMLStreamException {
        out.writeStartElement("session");

        // Pipeline -- This needs to appear before the Imports in the file, since one the imports are modified we're locked out from changing the pipeline.
        final PipelineTemplate pipeline = this.pipelineTemplateProperty().get();
        if(pipeline != null) {
            out.writeStartElement("Pipeline");
            out.writeAttribute("Instantiated", Boolean.toString(this.pipelineProperty != null && this.pipelineProperty.get() != null));

            PipelineTemplate.saveTemplate(pipeline, out);

            out.writeEndElement();
        }

        //Imports
        out.writeStartElement("Imports");
        for(final ImportItem item: importsAll) {
            out.writeStartElement("Item");
            Writer.writeObject(out, item);
            out.writeEndElement();
        }
        out.writeEndElement();

        out.writeStartElement("Networks");
        networks.writeToXml( out);
        out.writeEndElement();

        //Take snapshots, in case we need to reference ordering.
        final List<HardwareVertex> hardwareVertices = new ArrayList<>(lookupHardwareVertices.values());
        final List<LogicalVertex> logicalVertices = new ArrayList<>(lookupLogicalVertices.values());
        final List<LogicalConnection> edgesLogical = new ArrayList<>(lookupLogicalConnections.values());
        final List<PhysicalConnection> edgesPhysical = new ArrayList<>(lookupPhysicalConnections.values());

        out.writeStartElement("HardwareVertices");
        for(final HardwareVertex hardwareVertex : hardwareVertices) {
            out.writeStartElement("HardwareVertex");
            hardwareVertex.writeToXml( out);
            out.writeEndElement();
        }
        out.writeEndElement();

        out.writeStartElement("LogicalVertices");
        for(final LogicalVertex vertex : logicalVertices) {
            out.writeStartElement("LogicalVertex");
            out.writeAttribute("refHardwareVertex", Integer.toString(hardwareVertices.indexOf(vertex.getHardwareVertex())));

            vertex.writeToXml(out);
            out.writeEndElement();
        }
        out.writeEndElement();

        out.writeStartElement("PhysicalEdges");
        for(final PhysicalConnection edge : edgesPhysical) {
            out.writeStartElement("PhysicalInterconnection");

            out.writeAttribute("Source", String.format("%d", hardwareVertices.indexOf(lookupHardwareVertices.get(edge.getSource().getAddress()))));
            out.writeAttribute("Destination", String.format("%d", hardwareVertices.indexOf(lookupHardwareVertices.get(edge.getDestination().getAddress()))));

            edge.writeToXml(out);
            out.writeEndElement();
        }
        out.writeEndElement();

        out.writeStartElement("LogicalEdges");
        for(final LogicalConnection edge : edgesLogical) {
            out.writeStartElement("LogicalEdge");

            out.writeAttribute("Source", String.format("%d", logicalVertices.indexOf(lookupLogicalVertices.get(edge.getSource().getLogicalAddressMapping()))));
            out.writeAttribute("Destination", String.format("%d", logicalVertices.indexOf(lookupLogicalVertices.get(edge.getDestination().getLogicalAddressMapping()))));

            edge.writeToXml(out);
            out.writeEndElement();
        }
        out.writeEndElement();


        out.writeEndElement();
    }

    public void readFromXml(final XMLStreamReader source) throws XMLStreamException {
        final List<HardwareVertex> lstHardwareVertices = new ArrayList<>();
        final List<LogicalVertex> lstLogicalVertices = new ArrayList<>();

        while(source.hasNext()) {
            if(source.next() == XMLEvent.START_ELEMENT) {
                switch(source.getLocalName()) {
                    case "Imports":
                        this.readImportsFromXml(source);
                        break;
                    case "Pipeline":
                        final boolean instantiated = Boolean.parseBoolean(source.getAttributeValue(null, "Instantiated"));
                        this.templateDefault = PipelineTemplate.loadTemplate(this.config, source);
                        if(instantiated) {
                            this.pipelineProperty.set(Pipeline.buildPipelineFromTemplate(this.config, this, this.templateDefault));
                        }
                        break;
                    case "Networks":
                        this.networks.readFromXml(source);
                        break;
                    case "HardwareVertices":
                        findHardwareVertices:
                        while(source.hasNext()) {
                            final int typeNext = source.next();
                            switch(typeNext) {
                                case XMLEvent.START_ELEMENT:
                                    if(source.getLocalName().equals("HardwareVertex")) {
                                        final HardwareVertex hardwareVertex = processHardwareVertex(source);
                                        if(hardwareVertex != null) {
                                            lstHardwareVertices.add(hardwareVertex);
                                            this.addHardwareVertex(hardwareVertex);
                                            hardwareVertex.reannounceProperties();
                                        }
                                    }
                                    break;
                                case XMLEvent.END_ELEMENT:
                                    if(source.getLocalName().equals("HardwareVertices")) {
                                        break findHardwareVertices;
                                    }
                                    break;
                            }
                        }
                        break;
                    case "LogicalVertices":
                        findLogicalVertices:
                        while(source.hasNext()) {
                            final int typeNext = source.next();
                            switch(typeNext) {
                                case XMLEvent.START_ELEMENT:
                                    if(source.getLocalName().equals("LogicalVertex")) {
                                        final int idxHardwareVertex = Integer.parseInt(source.getAttributeValue(null, "refHardwareVertex"));
                                        final LogicalVertex vertex = processLogicalVertex(source, lstHardwareVertices.get(idxHardwareVertex));
                                        if(vertex != null) {
                                            lstLogicalVertices.add(vertex);
                                            this.addLogicalVertex(vertex);
                                            vertex.reannounceProperties();
                                        }
                                    }
                                    break;
                                case XMLEvent.END_ELEMENT:
                                    if(source.getLocalName().equals("LogicalVertices")) {
                                        break findLogicalVertices;
                                    }
                                    break;
                            }
                        }
                        break;
                    case "Edges":
                        findEdges:
                        while(source.hasNext()) {
                            final int typeNext = source.next();
                            switch(typeNext) {
                                case XMLEvent.START_ELEMENT:
                                    if(source.getLocalName().equals("PhysicalInterconnection")) {
                                        final PhysicalConnection edge = new PhysicalConnection(this.getExecutionProvider(), lstHardwareVertices.get(Integer.parseInt(source.getAttributeValue(null, "Source"))), lstHardwareVertices.get(Integer.parseInt(source.getAttributeValue(null, "Destination"))));
                                        edge.readFromXml(source);
                                        this.addPhysicalConnection(edge);
                                    } else if(source.getLocalName().equals("LogicalEdge")) {
                                        final LogicalConnection edge = new LogicalConnection(this.getExecutionProvider(), lstLogicalVertices.get(Integer.parseInt(source.getAttributeValue(null, "Source"))), lstLogicalVertices.get(Integer.parseInt(source.getAttributeValue(null, "Destination"))));
                                        edge.readFromXml(source);
                                        this.addLogicalConnection(edge);
                                    }
                                    break;
                                case XMLEvent.END_ELEMENT:
                                    if(source.getLocalName().equals("Edges")) {
                                        break findEdges;
                                    }
                            }
                        }
                        break;
                }
            }
        }
    }

    protected void readImportsFromXml(final XMLStreamReader reader) throws XMLStreamException {
        while(reader.hasNext()) {
            final int typeNext = reader.next();
            switch(typeNext) {
                case XMLEvent.START_ELEMENT:
                    if(reader.getLocalName().equals("Item")) {
                        final Object obj = Loader.readObject(reader);
                        if(obj instanceof ImportItem) {
                            this.importsAll.add((ImportItem)obj);
                        } else {
                            Logger.log(Logger.Severity.WARNING, "An invalid object was found in the Import list and cannot be loaded: %s", obj);
                        }
                    }
                    break;
                case XMLEvent.END_ELEMENT:
                    if(reader.getLocalName().equals("Imports")) {
                        return;
                    }
                    break;
            }
        }
    }

    protected HardwareVertex processHardwareVertex(final XMLStreamReader reader) throws XMLStreamException {
        // reader is pointing to the start element for the vertex
        HardwareAddress address = null;
        final String tagName = reader.getLocalName();

        while(reader.hasNext()) {
            final int typeNext = reader.next();
            switch(typeNext) {
                case XMLEvent.START_ELEMENT:
                    switch(reader.getLocalName()) {
                        case "HardwareAddress":
                            final Object objAddress = Loader.readObject(reader);
                            if(objAddress instanceof HardwareAddress) {
                                address = (HardwareAddress)objAddress;
                            }
                            break;
                        case "Properties":
                            final HardwareVertex result = new HardwareVertex(this.providerExec, address);
                            result.readFromXml(reader);
                            return result;
                    }
                    break;
                case XMLEvent.END_ELEMENT:
                    if(reader.getLocalName().equals(tagName)) {
                        return null;
                    }
                    break;
            }
        }
        return null;
    }
    protected LogicalVertex processLogicalVertex(final XMLStreamReader reader, final HardwareVertex vertexHardware) throws XMLStreamException {
        LogicalAddress address = null;
        final String tagName = reader.getLocalName();

        while(reader.hasNext()) {
            final int typeNext = reader.next();
            switch(typeNext) {
                case XMLEvent.START_ELEMENT:
                    if(reader.getLocalName().equals("LogicalAddress")) {
                        final Object obj = Loader.readObject(reader);
                        if(obj instanceof LogicalAddress) {
                            address = (LogicalAddress)obj;
                        }
                    } else if(reader.getLocalName().equals("Properties")) {
                        final LogicalVertex result = new LogicalVertex(this.providerExec, this, new LogicalAddressMapping(vertexHardware.getAddress(), address), vertexHardware);
                        result.readFromXml(reader);
                        return result;
                    }
                    break;
                case XMLEvent.END_ELEMENT:
                    if(reader.getLocalName().equals(tagName)) {
                        return null;
                    }
                    break;
            }
        }
        return null;
    }

    // == PIPELINE ============================================================

    // The default template is the session-local pipeline template.  This is what is modified when the pipeline editor opens by default, this is what will be used when th
    private PipelineTemplate templateDefault;

    private ObjectProperty<PipelineTemplate> pipelineTemplate = null;

    /**
     * If the pipelineTemplate is set to null then it will return the Session's default template.
     * @return
     */
    public ObjectProperty<PipelineTemplate> pipelineTemplateProperty() {
        if(this.pipelineTemplate == null) {
            this.pipelineTemplate = new SimpleObjectProperty<PipelineTemplate>() {
                @Override
                public PipelineTemplate get() {
                    final PipelineTemplate result = super.get();
                    if(result == null) {
                        return Session.this.getSessionDefaultTemplate();
                    } else {
                        return result;
                    }
                }
            };
        }
        return this.pipelineTemplate;
    }

    public PipelineTemplate getSessionDefaultTemplate() {
        if(this.templateDefault == null) {
            try {
                this.templateDefault = PipelineTemplate.loadTemplate(Session.this.config, RuntimeConfiguration.class.getResourceAsStream("/resources/default.pte"));
            } catch(Exception ex) {
                Logger.log(Logger.Severity.ERROR, "There was an error loading the default Pipeline Template: %s", ex.getMessage());
            }
        }
        return this.templateDefault;
    }


    private ObjectProperty<Pipeline> pipelineProperty = null;
    public ObjectProperty<Pipeline> pipelineProperty() {
        if(this.pipelineProperty == null) {
            this.pipelineProperty = new SimpleObjectProperty<Pipeline>(null) {
                @Override
                public void set(final Pipeline newValue) {
                    if(Session.this.canSetPipeline().get()) {
                        if(get() != null) {
                            get().close();
                        }
                        super.set(newValue);
                    } else {
                        throw new IllegalStateException("Cannot change pipeline after adding an import.");
                    }
                }
            };
        }
        return this.pipelineProperty;
    }

    public BooleanExpression canSetPipeline() {
        return Session.this.pipelineProperty().isNull();
    }

    // == Synchronized access support =========================================

    public void executeWithLock(final Runnable task) {
        synchronized(lookupPhysicalConnections) {
            synchronized(lookupLogicalConnections) {
                synchronized (lookupLogicalVertices) {
                    synchronized (lookupHardwareVertices) {
                        task.run();
                    }
                }
            }
        }
    }

    public void executeOnHardwareVerticesWithLock(final Consumer<HardwareVertex> task) {
        synchronized(lookupHardwareVertices) {
            lookupHardwareVertices.values().forEach(task);
        }
    }
    public void executeOnLogicalVerticesWithLock(final Consumer<LogicalVertex> task) {
        synchronized(lookupLogicalVertices) {
            lookupLogicalVertices.values().forEach(task);
        }
    }
    public void executeOnLogicalEdgesWithLock(final Consumer<LogicalConnection> task) {
        synchronized(lookupLogicalConnections) {
            lookupLogicalConnections.values().forEach(task);
        }
    }

    public void waitForSync() {
        this.providerExec.runNow(() -> {});
    }
}
