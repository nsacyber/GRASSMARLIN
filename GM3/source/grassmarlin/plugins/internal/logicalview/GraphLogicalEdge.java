package grassmarlin.plugins.internal.logicalview;

import grassmarlin.Event;
import grassmarlin.common.fxobservables.FxBooleanProperty;
import grassmarlin.common.fxobservables.FxObservableSet;
import grassmarlin.common.fxobservables.LongAccumulator;
import grassmarlin.session.LogicalConnection;
import grassmarlin.session.PropertyCloud;
import grassmarlin.session.Session;
import grassmarlin.session.graphs.IHasKey;
import grassmarlin.session.pipeline.ILogicalPacketMetadata;
import grassmarlin.session.pipeline.IPacketMetadata;
import grassmarlin.session.pipeline.LogicalAddressMapping;
import grassmarlin.session.serialization.Loader;
import grassmarlin.session.serialization.Writer;
import grassmarlin.session.serialization.XmlSerializable;
import javafx.beans.binding.BooleanExpression;
import javafx.beans.binding.LongBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.ObservableSet;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.stream.events.XMLEvent;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * The GraphLogicalEdge tracks PacketLists between all the LogicalAddressMappings of a pair of GraphLogicalVertices.
 */
public class GraphLogicalEdge extends PropertyCloud implements IHasKey, XmlSerializable {
    /**
     * PacketMetadata objects are the structures found in the PROPERTY_PACKET_LIST collection.
     */
    public final class PacketMetadata implements XmlSerializable {
        private final String file;
        private final long frame;
        private final long time;
        private final short protocol;
        private final long size;
        private final int idxSourceAddress;
        private final int idxDestinationAddress;

        public PacketMetadata(final IPacketMetadata metadata, final int idxSourceAddress, final int idxDestinationAddress) {
            this.file = metadata.getImportSource().getPath().toAbsolutePath().toString();
            this.frame = metadata.getFrame() == null ? -1 : metadata.getFrame();
            this.time = metadata.getTime();
            this.protocol = metadata.getTransportProtocol();
            this.size = metadata.getPacketSize();

            this.idxSourceAddress = idxSourceAddress;
            this.idxDestinationAddress = idxDestinationAddress;
        }

        public PacketMetadata(final XMLStreamReader reader) throws XMLStreamException{
            this.file = reader.getAttributeValue(null, "File");
            this.frame = Long.parseLong(reader.getAttributeValue(null, "Frame"));
            this.time= Long.parseLong(reader.getAttributeValue(null, "Time"));
            this.protocol = Short.parseShort(reader.getAttributeValue(null, "Protocol"));
            this.size = Long.parseLong(reader.getAttributeValue(null, "Size"));
            this.idxSourceAddress = Integer.parseInt(reader.getAttributeValue(null, "Source"));
            this.idxDestinationAddress = Integer.parseInt(reader.getAttributeValue(null, "Destination"));
        }

        @Override
        public void writeToXml(final XMLStreamWriter target) throws XMLStreamException {
            target.writeAttribute("File", this.file);
            target.writeAttribute("Frame", Long.toString(this.frame));
            target.writeAttribute("Time", Long.toString(this.time));
            target.writeAttribute("Protocol", Short.toString(this.protocol));
            target.writeAttribute("Size", Long.toString(this.size));
            target.writeAttribute("Source", Integer.toString(this.idxSourceAddress));
            target.writeAttribute("Destination", Integer.toString(this.idxDestinationAddress));
        }

        public String getFile() {
            return this.file;
        }
        public Long getFrame() {
            return this.frame == -1 ? null : this.frame;
        }
        public long getTime() {
            return this.time;
        }
        public short getTransportProtocol() {
            return this.protocol;
        }
        public long getSize() {
            return this.size;
        }
        public LogicalAddressMapping getSourceAddress() {
            return GraphLogicalEdge.this.lstEndpointAddresses.get(this.idxSourceAddress);
        }
        public LogicalAddressMapping getDestinationAddress() {
            return GraphLogicalEdge.this.lstEndpointAddresses.get(this.idxDestinationAddress);
        }

        @Override
        public String toString() {
            return String.format("[%s]#%d: 0x%02x, %d bytes, %s -> %s", Instant.ofEpochMilli(this.time).atZone(ZoneId.of("Z")).format(DateTimeFormatter.ISO_DATE_TIME), this.frame, this.protocol, this.size, this.getSourceAddress(), this.getDestinationAddress());
        }
    }

    /**
     * A PacketList stores all packets between two logical endpoints (generally, LogicalAddressMappings to Ipv4WithPorts).
     * The logical graph doesn't particularly care about directionality, so the data is not tracked with directionality, but
     * the session reports it as such.
     */
    public class PacketList implements Comparable<PacketList>, XmlSerializable {
        private final LogicalConnection forwardEdge;
        private LogicalConnection reverseEdge;

        private final LongAccumulator totalPacketSize;
        private final LongAccumulator sentPacketSize;
        private final LongAccumulator recvPacketSize;
        private final LongAccumulator sentPackets;
        private final LongAccumulator recvPackets;
        private final ObservableSet<Short> protocols;

        protected PacketList(final PacketList old) {
            this.forwardEdge = old.forwardEdge;
            this.reverseEdge = old.reverseEdge;

            this.totalPacketSize = old.totalPacketSize;
            this.sentPacketSize = old.sentPacketSize;
            this.recvPacketSize = old.recvPacketSize;
            this.sentPackets = old.sentPackets;
            this.recvPackets = old.recvPackets;
            this.protocols = old.protocols;
        }
        public PacketList(final LogicalConnection edgeForward) {
            this.forwardEdge = edgeForward;
            this.reverseEdge = null;

            this.totalPacketSize = new LongAccumulator(GraphLogicalEdge.this.uiProvider, 0);
            this.sentPacketSize = new LongAccumulator(GraphLogicalEdge.this.uiProvider, 0);
            this.recvPacketSize = new LongAccumulator(GraphLogicalEdge.this.uiProvider, 0);
            this.sentPackets = new LongAccumulator(GraphLogicalEdge.this.uiProvider, 0);
            this.recvPackets = new LongAccumulator(GraphLogicalEdge.this.uiProvider, 0);
            this.protocols = new FxObservableSet<>(GraphLogicalEdge.this.uiProvider);
        }

        public void setReverseEdge(final LogicalConnection edgeReverse) {
            if(this.reverseEdge == edgeReverse) {
                return;
            }
            if(this.reverseEdge != null) {
                throw new IllegalStateException("Reverse Edge is already set.");
            }
            this.reverseEdge = edgeReverse;
            //EXTEND: Process initial state of the new edge.
        }

        public LogicalAddressMapping getSourceAddress() {
            return this.forwardEdge.getSource().getLogicalAddressMapping();
        }
        public LogicalAddressMapping getDestinationAddress() {
            return this.forwardEdge.getDestination().getLogicalAddressMapping();
        }

        public Collection<LogicalConnection> getEdges() {
            if(this.reverseEdge == null) {
                return Collections.singletonList(this.forwardEdge);
            } else {
                return Arrays.asList(this.forwardEdge, this.reverseEdge);
            }
        }

        public void addPacket(final PacketMetadata packet) {
            synchronized(GraphLogicalEdge.this.packetList) {
                GraphLogicalEdge.this.packetList.add(packet);
            }
            this.protocols.add(packet.getTransportProtocol());
            this.totalPacketSize.increment(packet.getSize());

            GraphLogicalEdge.this.totalPacketSize.set(GraphLogicalEdge.this.totalPacketSize.get() + packet.getSize());
            GraphLogicalEdge.this.totalPacketCount.set(GraphLogicalEdge.this.totalPacketCount.get() + 1);
            GraphLogicalEdge.this.protocols.add(packet.getTransportProtocol());

            //In order to add a packet to a logical edge, it has to be between LogicalAddressMappings.
            if (GraphLogicalEdge.this.getSource().getRootLogicalAddressMapping().contains(packet.getSourceAddress())) {
                //Direction matches
                this.sentPackets.increment(1);
                this.sentPacketSize.increment(packet.getSize());
                //TODO: Update sent protocols
                GraphLogicalEdge.this.sentPacketCount.set(GraphLogicalEdge.this.sentPacketCount.get() + 1);
                GraphLogicalEdge.this.sentPacketSize.set(GraphLogicalEdge.this.sentPacketSize.get() + packet.getSize());
            } else {
                //Direction reversed
                this.recvPackets.increment(1);
                this.recvPacketSize.increment(packet.getSize());
                //TODO: Update recv protocols
                GraphLogicalEdge.this.recvPacketCount.set(GraphLogicalEdge.this.recvPacketCount.get() + 1);
                GraphLogicalEdge.this.recvPacketSize.set(GraphLogicalEdge.this.recvPacketSize.get() + packet.getSize());
            }
        }

        public LongBinding totalPacketSizeProperty() {
            return this.totalPacketSize;
        }
        public LongBinding sentPacketSizeProperty() {
            return this.sentPacketSize;
        }
        public LongBinding recvPacketSizeProperty() {
            return this.recvPacketSize;
        }
        public LongBinding sentPacketCountProperty() {
            return this.sentPackets;
        }
        public LongBinding recvPacketCountProperty() {
            return this.recvPackets;
        }
        public ObservableSet<Short> getProtocols() {
            return this.protocols;
        }

        @Override
        public void readFromXml(final XMLStreamReader reader) throws XMLStreamException {
            //the Source and Destination are expected to either match or not exist in the tream--they are written first so that the code calling this method can identify the correct PacketList instance that loads the remaining content.
            //this.packets.clear();
            this.protocols.clear();
            this.totalPacketSize.set(0);

            while(reader.hasNext()) {
                final int typeNext = reader.next();
                final String tag;
                switch(typeNext) {
                    case XMLEvent.START_ELEMENT:
                        tag = reader.getLocalName();
                        if(tag.equals("AggregateStats")) {
                            this.totalPacketSize.set(Long.parseLong(reader.getAttributeValue(null, "Size")));
                            this.sentPacketSize.set(Long.parseLong(reader.getAttributeValue(null, "SentPacketSize")));
                            this.recvPacketSize.set(Long.parseLong(reader.getAttributeValue(null, "RecvPacketSize")));
                            this.sentPackets.set(Long.parseLong(reader.getAttributeValue(null, "SentPacketCount")));
                            this.recvPackets.set(Long.parseLong(reader.getAttributeValue(null, "RecvPacketCount")));

                            //TODO: Read other aggregated stats
                        } else if(tag.equals("Protocol")) {
                            this.protocols.add(Short.parseShort(reader.getElementText()));
                        }
                        break;
                    case XMLEvent.END_ELEMENT:
                        tag = reader.getLocalName();
                        if(tag.equals("PacketList")) {
                            return;
                        }
                        break;
                }
            }
            throw new IllegalStateException("PacketList did not terminate properly.");
        }

        @Override
        public void writeToXml(final XMLStreamWriter writer) throws XMLStreamException {
            //Source and Destination must be written first so that we can identify the PacketList to which the remaining data belongs.
            writer.writeStartElement("Source");
            Writer.writeObject(writer, forwardEdge.getSource());
            writer.writeEndElement();
            writer.writeStartElement("Destination");
            Writer.writeObject(writer, forwardEdge.getDestination());
            writer.writeEndElement();

            writer.writeStartElement("AggregateStats");
            writer.writeAttribute("Size", Long.toString(totalPacketSize.get()));
            writer.writeAttribute("SentPacketSize", Long.toString(this.sentPacketSize.get()));
            writer.writeAttribute("RecvPacketSize", Long.toString(this.recvPacketSize.get()));
            writer.writeAttribute("SentPacketCount", Long.toString(this.sentPackets.get()));
            writer.writeAttribute("RecvPacketCount", Long.toString(this.recvPackets.get()));
            //TODO: Write other aggregate stats.
            for(short protocol : protocols) {
                writer.writeStartElement("Protocol");
                writer.writeCharacters(Short.toString(protocol));
                writer.writeEndElement();
            }
            writer.writeEndElement();
        }

        @Override
        public int compareTo(PacketList o) {
            final int cmpSource = this.getSourceAddress().compareTo(o.getSourceAddress());
            if(cmpSource == 0) {
                return this.getDestinationAddress().compareTo(o.getDestinationAddress());
            } else {
                return cmpSource;
            }
        }
    }

    private final GraphLogicalVertex source;
    private final GraphLogicalVertex destination;

    private final String key;
    private final String keyReverse;

    private final Map<LogicalConnection, PacketList> lookupPacketListsByEdge;
    private final Map<Session.LogicalAddressPair, PacketList> lookupPacketListsByEndpoints;

    private final List<PacketMetadata> packetList;
    private final ObservableSet<PacketList> packetLists;
    private final LongAccumulator totalPacketSize;
    private final LongAccumulator totalPacketCount;
    private final LongAccumulator sentPacketSize;
    private final LongAccumulator sentPacketCount;
    private final LongAccumulator recvPacketSize;
    private final LongAccumulator recvPacketCount;
    private final ObservableSet<Short> protocols;
    private final ObservableSet<Short> sentProtocols;
    private final ObservableSet<Short> recvProtocols;

    private final Map<LogicalAddressMapping, BooleanProperty> mapHasEndpointReceivedData;

    private final List<LogicalAddressMapping> lstEndpointAddresses;

    public GraphLogicalEdge(final Event.IAsyncExecutionProvider ui, final GraphLogicalVertex source, final GraphLogicalVertex destination, final Collection<GraphLogicalEdge> edges) {
        super(ui);

        this.source = source;
        this.destination = destination;

        this.key = String.format("[%s->%s]", source.getKey(), destination.getKey());
        this.keyReverse = String.format("[%s->%s]", destination.getKey(), source.getKey());

        this.lstEndpointAddresses = new LinkedList<>();

        this.lookupPacketListsByEdge = new HashMap<>();
        this.lookupPacketListsByEndpoints = new HashMap<>();

        this.packetLists = new FxObservableSet<>(this.uiProvider);
        this.packetList = new ArrayList<>();

        this.totalPacketSize = new LongAccumulator(this.uiProvider, 0);
        this.totalPacketCount = new LongAccumulator(this.uiProvider, 0);
        this.sentPacketSize = new LongAccumulator(this.uiProvider, 0);
        this.sentPacketCount = new LongAccumulator(this.uiProvider, 0);
        this.recvPacketSize = new LongAccumulator(this.uiProvider, 0);
        this.recvPacketCount = new LongAccumulator(this.uiProvider, 0);
        this.protocols = new FxObservableSet<>(this.uiProvider);
        this.sentProtocols = new FxObservableSet<>(this.uiProvider);
        this.recvProtocols = new FxObservableSet<>(this.uiProvider);

        this.mapHasEndpointReceivedData = new HashMap<>();

        for(final GraphLogicalEdge edge : edges) {
            for(final PropertyCloud ancestor : edge.getAncestors()) {
                super.addAncestor(ancestor);
                if(ancestor instanceof LogicalConnection) {
                    this.trackEdge((LogicalConnection)ancestor);
                }
            }
            this.mapHasEndpointReceivedData.putAll(edge.mapHasEndpointReceivedData);
            this.protocols.addAll(edge.protocols);
            this.totalPacketSize.increment(edge.totalPacketSize.get());
            this.totalPacketCount.increment(edge.totalPacketCount.get());

            this.packetList.addAll(edge.packetList);

            //Because PacketList is an inner class we have to create new PacketList objects wit hthe same values
            final Map<PacketList, PacketList> mappingPacketLists = new HashMap<>();
            for(final PacketList listOld : edge.packetLists) {
                final PacketList listNew = new PacketList(listOld);
                mappingPacketLists.put(listOld, listNew);
            }
            this.packetLists.addAll(mappingPacketLists.values());

            for(final Map.Entry<LogicalConnection, PacketList> entryOld : edge.lookupPacketListsByEdge.entrySet()) {
                this.lookupPacketListsByEdge.put(entryOld.getKey(), mappingPacketLists.get(entryOld.getValue()));
            }
            for(final Map.Entry<Session.LogicalAddressPair, PacketList> entryOld : edge.lookupPacketListsByEndpoints.entrySet()) {
                this.lookupPacketListsByEndpoints.put(entryOld.getKey(), mappingPacketLists.get(entryOld.getValue()));
            }

            this.sentPacketSize.increment(edge.sentPacketSize.get());
            this.sentPacketCount.increment(edge.sentPacketCount.get());
            this.recvPacketSize.increment(edge.recvPacketSize.get());
            this.recvPacketCount.increment(edge.recvPacketCount.get());

            //this.sentProtocols = new ObservableSetWrapper<>(new HashSet<>());
            //this.recvProtocols = new ObservableSetWrapper<>(new HashSet<>());
        }

    }
    public GraphLogicalEdge(final Event.IAsyncExecutionProvider ui, final LogicalConnection edge, final GraphLogicalVertex source, final GraphLogicalVertex destination) {
        super(ui);

        this.source = source;
        this.destination = destination;

        this.lstEndpointAddresses = new LinkedList<>();

        super.addAncestor(edge);

        this.key = String.format("[%s->%s]", source.getKey(), destination.getKey());
        this.keyReverse = String.format("[%s->%s]", destination.getKey(), source.getKey());

        this.lookupPacketListsByEdge = new HashMap<>();
        this.lookupPacketListsByEndpoints = new HashMap<>();

        this.packetLists = new FxObservableSet<>(this.uiProvider);
        this.packetList = new ArrayList<>();
        
        this.totalPacketSize = new LongAccumulator(this.uiProvider, 0);
        this.totalPacketCount = new LongAccumulator(this.uiProvider, 0);
        this.sentPacketSize = new LongAccumulator(this.uiProvider, 0);
        this.sentPacketCount = new LongAccumulator(this.uiProvider, 0);
        this.recvPacketSize = new LongAccumulator(this.uiProvider, 0);
        this.recvPacketCount = new LongAccumulator(this.uiProvider, 0);
        this.protocols = new FxObservableSet<>(this.uiProvider);
        this.sentProtocols = new FxObservableSet<>(this.uiProvider);
        this.recvProtocols = new FxObservableSet<>(this.uiProvider);

        this.mapHasEndpointReceivedData = new HashMap<>();

        this.trackEdge(edge);
    }

    public void trackEdge(final LogicalConnection edge) {
        final Session.LogicalAddressPair pairEdge = new Session.LogicalAddressPair(edge.getSource().getLogicalAddressMapping(), edge.getDestination().getLogicalAddressMapping());

        synchronized(this.lookupPacketListsByEdge) {
            //Adding as an ancestor enables Property inheritance.
            this.addAncestor(edge);

            // Start by looking for an existing PacketList that matches the endpoints.  We may have gotten into a situation where the edge direction was flipped, and so the edge won't match.
            synchronized(this.packetLists) {
                final PacketList listExisting = lookupPacketListsByEndpoints.get(pairEdge);

                if (listExisting != null) {
                    //The list exists, so this must be the reverse edge
                    //Alternatively, it might be a duplicate--this happens with router addresses.
                    if (edge != listExisting.forwardEdge && edge != listExisting.reverseEdge) {
                        listExisting.setReverseEdge(edge);
                        lookupPacketListsByEdge.put(edge, listExisting);
                    }
                } else {
                    //The list doesn't exist, so we are creating a new list with this as the forward edge.
                    final PacketList listNew = new PacketList(edge);
                    this.lookupPacketListsByEndpoints.put(pairEdge, listNew);
                    this.lookupPacketListsByEdge.put(edge, listNew);
                    this.packetLists.add(listNew);
                }
            }
        }
    }

    public void addPacket(final ILogicalPacketMetadata packet) {
        final LogicalAddressMapping dest = packet.getDestAddress();

        synchronized(this.mapHasEndpointReceivedData) {
            //The destination has received data, so we will flag this as such.
            final BooleanProperty existingDestinationHasReceivedData = this.mapHasEndpointReceivedData.get(dest);
            if(existingDestinationHasReceivedData == null) {
                this.mapHasEndpointReceivedData.put(dest, new FxBooleanProperty(true));
            } else {
                existingDestinationHasReceivedData.set(true);
            }

            //The top-level source or destination will also have received data, so we determine which is the endpoint and mark it, too.
            if (this.getSource().getRootLogicalAddressMapping().contains(dest)) {
                final BooleanProperty propRoot = this.mapHasEndpointReceivedData.get(this.getSource().getRootLogicalAddressMapping());
                if (propRoot != null) {
                    propRoot.set(true);
                } else {
                    this.mapHasEndpointReceivedData.put(this.getSource().getRootLogicalAddressMapping(), new FxBooleanProperty(true));
                }
            } else {
                //Assume destination match
                final BooleanProperty propRoot = this.mapHasEndpointReceivedData.get(this.getDestination().getRootLogicalAddressMapping());
                if (propRoot != null) {
                    propRoot.set(true);
                } else {
                    this.mapHasEndpointReceivedData.put(this.getDestination().getRootLogicalAddressMapping(), new FxBooleanProperty(true));
                }
            }
        }

        final PacketList list;
        final int idxSourceAddress;
        final int idxDestinationAddress;
        synchronized(this.packetLists) {
            list = this.lookupPacketListsByEndpoints.get(new Session.LogicalAddressPair(packet.getSourceAddress(), packet.getDestAddress()));
            if(!this.lstEndpointAddresses.contains(packet.getSourceAddress())) {
                this.lstEndpointAddresses.add(packet.getSourceAddress());
            }
            idxSourceAddress = this.lstEndpointAddresses.indexOf(packet.getSourceAddress());
            if(!this.lstEndpointAddresses.contains(packet.getDestAddress())) {
                this.lstEndpointAddresses.add(packet.getDestAddress());
            }
            idxDestinationAddress = this.lstEndpointAddresses.indexOf(packet.getDestAddress());
        }
        //noinspection SynchronizationOnLocalVariableOrMethodParameter
        synchronized(list) {
            list.addPacket(new PacketMetadata(packet, idxSourceAddress, idxDestinationAddress));
        }
    }

    public BooleanExpression getHasEndpointReceivedData(final LogicalAddressMapping destination) {
        synchronized(this.mapHasEndpointReceivedData) {
            final BooleanExpression existing = this.mapHasEndpointReceivedData.get(destination);
            if(existing != null) {
                return existing;
            } else {
                final SimpleBooleanProperty propNew = new FxBooleanProperty(false);
                this.mapHasEndpointReceivedData.put(destination, propNew);
                return propNew;
            }
        }
    }

    public GraphLogicalVertex getSource() {
        return this.source;
    }
    public GraphLogicalVertex getDestination() {
        return this.destination;
    }

    public void getPacketList(final List<PacketMetadata> packets) {
        synchronized(this.packetList) {
            packets.addAll(this.packetList);
        }
    }
    public ObservableSet<PacketList> getPacketLists() {
        return this.packetLists;
    }

    public LongBinding totalPacketSizeProperty() {
        return this.totalPacketSize;
    }
    public LongBinding totalPacketCountProperty() {
        return this.totalPacketCount;
    }
    public LongBinding sentPacketSizeProperty() {
        return this.sentPacketSize;
    }
    public LongBinding recvPacketSizeProperty() {
        return this.recvPacketSize;
    }
    public LongBinding sentPacketCountProperty() {
        return this.sentPacketCount;
    }
    public LongBinding recvPacketCountProperty() {
        return this.recvPacketCount;
    }

    public ObservableSet<Short> protocolsProperty() {
        return this.protocols;
    }

    @Override
    public int hashCode() {
        return source.hashCode() ^ destination.hashCode();
    }

    @Override
    public final String getKey() {
        return this.key;
    }

    public final String getReverseKey() {
        return this.keyReverse;
    }

    @Override
    public void readFromXml(final XMLStreamReader reader) throws XMLStreamException {
        while(reader.hasNext()) {
            final int typeNext = reader.next();
            final String tag;
            switch(typeNext) {
                case XMLEvent.START_ELEMENT:
                    tag = reader.getLocalName();
                    if(tag.equals("AggregateStats")) {
                        this.totalPacketCount.set(Long.parseLong(reader.getAttributeValue(null, "Count")));
                        this.totalPacketSize.set(Long.parseLong(reader.getAttributeValue(null, "Size")));
                    } else if(tag.equals("Protocol")) {
                        this.protocols.add(Short.parseShort(reader.getElementText()));
                    } else if(tag.equals("PacketList")) {
                        //The PacketLists have already been restored from loading edges earlier--we now need to identify each PacketList to an edge and load the content
                        //A PacketList writes the source/destination first, so we can extract those to identify the existing PacketList, then load the content to the PacketList
                        final LogicalAddressMapping source = Loader.readNextObject(reader, LogicalAddressMapping.class);
                        final LogicalAddressMapping destination = Loader.readNextObject(reader, LogicalAddressMapping.class);
                        this.mapHasEndpointReceivedData.computeIfAbsent(source, k -> new FxBooleanProperty(false));
                        this.mapHasEndpointReceivedData.computeIfAbsent(destination, k -> new FxBooleanProperty(false));

                        final PacketList listExisting = lookupPacketListsByEndpoints.get(new Session.LogicalAddressPair(source, destination));
                        //TODO: Revisit reversed direction packetlists
                        //listExisting.setReadDirection(listExisting.getSourceAddress().equals(source));
                        listExisting.readFromXml(reader);
                    } else if(tag.equals("Address")) {
                        this.lstEndpointAddresses.add((LogicalAddressMapping)Loader.readObject(reader));
                    } else if(tag.equals("PropertyCloud")) {
                        super.readFromXml(reader);
                    } else if(tag.equals("Metadata")) {
                        //We bypass the default addPacket method and run it out-of-thread since there is no way that the changes made here can affect anything outside this object (we're in the constructor, after all)
                        final PacketMetadata metadata = new PacketMetadata(reader);
                        GraphLogicalEdge.this.packetList.add(metadata);

                        final BooleanProperty existing = this.mapHasEndpointReceivedData.get(metadata.getDestinationAddress());
                        if(existing != null) {
                            existing.set(true);
                        } else {
                            this.mapHasEndpointReceivedData.put(metadata.getDestinationAddress(), new SimpleBooleanProperty(true));
                        }
                        this.mapHasEndpointReceivedData.computeIfAbsent(metadata.getSourceAddress(), k -> new SimpleBooleanProperty(false));
                    }
                    break;
                case XMLEvent.END_ELEMENT:
                    tag = reader.getLocalName();
                    if(tag.equals("Edge")) {
                        return;
                    }
                    break;
            }
        }
    }

    @Override
    public void writeToXml(final XMLStreamWriter writer) throws XMLStreamException {
        writer.writeStartElement("AggregateStats");
        writer.writeAttribute("Size", Long.toString(totalPacketSize.get()));
        writer.writeAttribute("Count", Long.toString(totalPacketCount.get()));
        for(short protocol : protocols) {
            writer.writeStartElement("Protocol");
            writer.writeCharacters(Short.toString(protocol));
            writer.writeEndElement();
        }
        writer.writeEndElement();

        for(LogicalAddressMapping mapping : this.lstEndpointAddresses) {
            writer.writeStartElement("Address");
            Writer.writeObject(writer, mapping);
            writer.writeEndElement();
        }

        for(final PacketList packetList : packetLists) {
            writer.writeStartElement("PacketList");
            packetList.writeToXml(writer);
            writer.writeEndElement();
        }

        synchronized(this.packetList) {
            writer.writeStartElement("Packets");
            for (final PacketMetadata packet : packetList) {
                writer.writeStartElement("Metadata");
                packet.writeToXml(writer);
                writer.writeEndElement();
            }
            writer.writeEndElement();
        }


        writer.writeStartElement("PropertyCloud");
        super.writeToXml(writer);
        writer.writeEndElement();
    }
}
