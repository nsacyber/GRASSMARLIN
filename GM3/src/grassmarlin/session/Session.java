package grassmarlin.session;

import com.sun.istack.internal.NotNull;
import com.sun.javafx.collections.ObservableListWrapper;
import grassmarlin.Event;
import grassmarlin.Logger;
import grassmarlin.Plugin;
import grassmarlin.RuntimeConfiguration;
import grassmarlin.common.PipelineWatcher;
import grassmarlin.plugins.IPlugin;
import grassmarlin.session.pipeline.*;
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
    public static class AddressPair implements Comparable<AddressPair> {
        protected final IAddress source;
        protected final IAddress destination;

        public AddressPair(final IAddress source, final IAddress destination) {
            this.source = source;
            this.destination = destination;
        }

        @Override
        public int compareTo(final AddressPair other) {
            return this.hashCode() - other.hashCode();
        }

        @Override
        public int hashCode() {
            return source.hashCode() ^ destination.hashCode();
        }

        @Override
        public boolean equals(Object other) {
            if(other != null && other instanceof AddressPair) {
                return this.source.equals(((AddressPair)other).source) && this.destination.equals(((AddressPair)other).destination);
            } else {
                return false;
            }
        }

        @Override
        public String toString() {
            return String.format("%s -> %s", source, destination);
        }
    }
    public static class BidirectionalAddressPair extends AddressPair {
        public BidirectionalAddressPair(final IAddress source, final IAddress destination) {
            super(source, destination);
        }

        @Override
        public boolean equals(Object other) {
            if(other != null && other instanceof BidirectionalAddressPair) {
                return (this.source.equals(((BidirectionalAddressPair)other).source) && this.destination.equals(((BidirectionalAddressPair)other).destination))
                        ||
                        (this.source.equals(((BidirectionalAddressPair)other).destination) && this.destination.equals(((BidirectionalAddressPair)other).source));
            } else {
                return false;
            }
        }


        @Override
        public String toString() {
            return String.format("%s <-> %s", source, destination);
        }
    }
    public class HardwareVertexEvent {
        private final HardwareVertex hardwareVertex;

        public HardwareVertexEvent(final HardwareVertex hardwareVertex) {
            this.hardwareVertex = hardwareVertex;
        }

        public Session getSession() {
            return Session.this;
        }
        public HardwareVertex getHardwareVertex() {
            return this.hardwareVertex;
        }
    }
    public class LogicalVertexEvent {
        private final LogicalVertex logicalVertex;

        public LogicalVertexEvent(final LogicalVertex logicalVertex) {
            this.logicalVertex = logicalVertex;
        }

        public Session getSession() {
            return Session.this;
        }
        public LogicalVertex getLogicalVertex() {
            return logicalVertex;
        }
    }
    public class EdgeEvent {
        private final Edge<?> edge;

        public EdgeEvent(final Edge<?> edge) {
            this.edge = edge;
        }

        public Session getSession() {
            return Session.this;
        }
        public Edge<?> getEdge() {
            return this.edge;
        }
    }
    public class PacketEvent {
        private final Edge<?> edge;
        private final IPacketMetadata packet;

        public PacketEvent(final Edge<?> edge, final IPacketMetadata packet) {
            this.edge = edge;
            this.packet = packet;
        }

        public Edge<?> getEdge() {
            return this.edge;
        }
        public IPacketMetadata getPacket() {
            return this.packet;
        }
    }

    // Events
    public final Event<HardwareVertexEvent> onHardwareVertexCreated;
    public final Event<LogicalVertexEvent> onLogicalVertexCreated;
    public final Event<EdgeEvent> onEdgeCreated;
    public final Event<PacketEvent> onPacketReceived;

    public final Event<List<Network>> onNetworkChange;
    public final Event<Session> onSessionModified;

    //Data Flow / Store
    private final ObservableList<ImportItem> importsAll;
    private final SimpleStringProperty livePcapEntry;
    //HACK: Loading code needs to access the NetworkList directly
    final NetworkList networks;
    private final Map<HardwareAddress, HardwareVertex> lookupHardwareVertices;
    private final Map<LogicalAddressMapping, LogicalVertex> lookupLogicalVertices;
    private final Map<AddressPair, Edge<?>> lookupEdges;

    //Configuration
    private final RuntimeConfiguration config;
    private final Event.IAsyncExecutionProvider providerExec;

    //Pipeline monitoring stuff
    private final PipelineWatcher watcher;

    public Session(RuntimeConfiguration config) {
        this.config = config;
        //All changes within the session will be run through a single worker thread to resolve sync issues.  All events will run from this thread.
        this.providerExec = Event.createThreadQueueProvider();

        //Init Properties
        this.livePcapEntry = new SimpleStringProperty(Pipeline.ENTRY_DEFAULT);

        //State data
        this.networks = new NetworkList(this.providerExec);
        this.lookupHardwareVertices = new ConcurrentHashMap<>();
        this.lookupLogicalVertices = new ConcurrentHashMap<>();
        this.lookupEdges = new ConcurrentHashMap<>();
        this.importsAll = new ObservableListWrapper<>(new ArrayList<>());

        //Events
        this.onHardwareVertexCreated = new Event<>(this.providerExec);
        this.onLogicalVertexCreated = new Event<>(this.providerExec);
        this.onEdgeCreated = new Event<>(this.providerExec);
        this.onPacketReceived = new Event<>(this.providerExec);
        //Events from child objects
        this.onNetworkChange = this.networks.onNetworksUpdated;
        //The OnModified handler can fire in any thread since it will ultimately just trigger an invalidation in the DocumentState.
        this.onSessionModified = new Event<>(Event.PROVIDER_IN_THREAD);

        this.watcher = new PipelineWatcher(this.config);
        this.watcher.startPipelineWatcher();

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
                ex.printStackTrace();
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
            onHardwareVertexCreated.call(new HardwareVertexEvent(hardwareVertex));
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
                onHardwareVertexCreated.call(new HardwareVertexEvent(vertNew));
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
            onLogicalVertexCreated.call(new LogicalVertexEvent(vertex));
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
                final LogicalVertex vertNew = new LogicalVertex(this.providerExec, mapping, vertHw);
                lookupLogicalVertices.put(mapping, vertNew);
                onLogicalVertexCreated.call(new LogicalVertexEvent(vertNew));
                return vertNew;
            }
        }
    }
    public LogicalVertex nonblockingLogicalVertexFor(final LogicalAddressMapping mapping) {
        return lookupLogicalVertices.get(mapping);
    }

    // Edges

    /**
     * addEdge should only be used as part of deserialization; it bypasses the normal redundancy checks.
     * @param edge
     */
    private void addEdge(final Edge<?> edge) {
        lookupEdges.put(new AddressPair(edge.getSource(), edge.getDestination()), edge);
        onEdgeCreated.call(new EdgeEvent(edge));
    }
    public Edge<?> existingEdgeBetween(final IAddress source, final IAddress destination) {
        return existingEdgeBetween(new AddressPair(source, destination));
    }
    public Edge<?> existingEdgeBetween(final AddressPair addresses) {
        return lookupEdges.get(addresses);
    }
    public void createEdgeBetween(final IAddress source, final IAddress destination) {
        createEdgeBetween(new AddressPair(source, destination));
    }
    public void createEdgeBetween(final AddressPair addresses) {
        final Edge<?> edge = new Edge<>(providerExec, addresses.source, addresses.destination);
        if(null == lookupEdges.putIfAbsent(addresses, edge)) {
            onEdgeCreated.call(new EdgeEvent(edge));
        }
    }

    public void addPacket(final Edge<?> edge, final IPacketMetadata packet) {
        //We don't sanity check the edge because it is too expensive an operation to do here.
        onPacketReceived.call(new PacketEvent(edge, packet));
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

    @Override
    public String toString() {
        final StringBuilder result = new StringBuilder();
        result.append(super.toString()).append('\n');
        //Import Items
        for(ImportItem item : importsAll) {
            result.append("  ").append(item).append('\n');
        }

        result.append("== NETWORKS ==\n");
        for(final Network network : networks.getCalculatedNetworksCopy()) {
            result.append(network).append('\n');
        }

        result.append("== HARDWARE VERTICES ==\n");
        for(final HardwareVertex hardwareVertex : lookupHardwareVertices.values()) {
            result.append(hardwareVertex).append('\n');
        }
        result.append("== LOGICAL VERTICES ==\n");
        for(final LogicalVertex logicalVertex : lookupLogicalVertices.values()) {
            result.append(logicalVertex).append('\n');
        }
        result.append("== EDGES ==\n");
        for(final Edge<?> edge : lookupEdges.values()) {
            result.append(edge).append('\n');
        }

        return result.toString();
    }

    // == SERIALIZATION =======================================================

    @Override
    public void writeToXml(final XMLStreamWriter out) throws XMLStreamException {
        out.writeStartElement("session");

        //TODO: Add the pipeline to the session data
        /*
            private final SimpleStringProperty livePcapEntry;
            private final Pipeline pipeline;
         */

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
        final List<Edge<?>> edges = new ArrayList<>(lookupEdges.values());

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

        out.writeStartElement("Edges");
        for(final Edge<?> edge : edges) {
            out.writeStartElement("Edge");

            if(edge.getSource() instanceof HardwareAddress) {
                final int idxSource = hardwareVertices.indexOf(lookupHardwareVertices.get((HardwareAddress)edge.getSource()));
                out.writeAttribute("Source", String.format("HW:%d", idxSource));
            } else if(edge.getSource() instanceof LogicalAddressMapping) {
                final int idxSource = logicalVertices.indexOf(lookupLogicalVertices.get((LogicalAddressMapping)edge.getSource()));
                out.writeAttribute("Source", String.format("L:%d", idxSource));
            }
            if(edge.getDestination() instanceof HardwareAddress) {
                final int idxSource = hardwareVertices.indexOf(lookupHardwareVertices.get((HardwareAddress)edge.getDestination()));
                out.writeAttribute("Destination", String.format("HW:%d", idxSource));
            } else if(edge.getDestination() instanceof LogicalAddressMapping) {
                final int idxSource = logicalVertices.indexOf(lookupLogicalVertices.get((LogicalAddressMapping)edge.getDestination()));
                out.writeAttribute("Destination", String.format("L:%d", idxSource));
            }

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
                                    if(source.getLocalName().equals("Edge")) {
                                        final String[] tokensSource = source.getAttributeValue(null, "Source").split(":");
                                        final String[] tokensDestination = source.getAttributeValue(null, "Destination").split(":");

                                        if(!tokensSource[0].equals(tokensDestination[0])) {
                                            //Invalid edge (hardware to logical); skip it.
                                            break;
                                        }

                                        final Edge<?> edge;
                                        if(tokensSource[0].equals("HW")) {
                                            edge = new Edge<>(this.getExecutionProvider(), lstHardwareVertices.get(Integer.parseInt(tokensSource[1])).getAddress(), lstHardwareVertices.get(Integer.parseInt(tokensDestination[1])).getAddress());
                                        } else if(tokensSource[0].equals("L")) {
                                            edge = new Edge<>(this.getExecutionProvider(), lstLogicalVertices.get(Integer.parseInt(tokensSource[1])).getLogicalAddressMapping(), lstLogicalVertices.get(Integer.parseInt(tokensDestination[1])).getLogicalAddressMapping());
                                        } else {
                                            //Unknown format
                                            break;
                                        }
                                        edge.readFromXml(source);
                                        this.addEdge(edge);
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
        LogicalAddress<?> address = null;
        final String tagName = reader.getLocalName();

        while(reader.hasNext()) {
            final int typeNext = reader.next();
            switch(typeNext) {
                case XMLEvent.START_ELEMENT:
                    if(reader.getLocalName().equals("LogicalAddress")) {
                        final Object obj = Loader.readObject(reader);
                        if(obj instanceof LogicalAddress) {
                            address = (LogicalAddress<?>)obj;
                        }
                    } else if(reader.getLocalName().equals("Properties")) {
                        final LogicalVertex result = new LogicalVertex(this.providerExec, new LogicalAddressMapping(vertexHardware.getAddress(), address), vertexHardware);
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

    private ObjectProperty<PipelineTemplate> pipelineTemplate = null;
    public ObjectProperty<PipelineTemplate> pipelineTemplateProperty() {
        if(this.pipelineTemplate == null) {
            this.pipelineTemplate = new SimpleObjectProperty<>(this.config.getDefaultPipelineTemplate());
        }
        return this.pipelineTemplate;
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

    public ObservableList<PipelineTemplate> getPipelineTemplates() {
        return this.watcher.getPipelineTemplates();
    }

    // == Synchronized access support =========================================

    public void executeWithLock(final Runnable task) {
        synchronized(lookupEdges) {
            synchronized (lookupLogicalVertices) {
                synchronized(lookupHardwareVertices) {
                    task.run();
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
    public void executeOnEdgesWithLock(final Consumer<Edge<?>> task) {
        synchronized(lookupEdges) {
            lookupEdges.values().forEach(task);
        }
    }

    public void waitForSync() {
        this.providerExec.runNow(() -> {});
    }
}
