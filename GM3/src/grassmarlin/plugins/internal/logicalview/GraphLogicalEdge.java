package grassmarlin.plugins.internal.logicalview;

import com.sun.javafx.collections.ObservableListWrapper;
import com.sun.javafx.collections.ObservableSetWrapper;
import grassmarlin.Event;
import grassmarlin.RuntimeConfiguration;
import grassmarlin.common.fxobservables.FxBooleanProperty;
import grassmarlin.session.*;
import grassmarlin.session.graphs.IHasKey;
import grassmarlin.session.pipeline.IPacketMetadata;
import grassmarlin.session.pipeline.LogicalAddressMapping;
import grassmarlin.session.serialization.Loader;
import grassmarlin.session.serialization.Writer;
import grassmarlin.session.serialization.XmlSerializable;
import javafx.beans.binding.BooleanExpression;
import javafx.beans.binding.LongExpression;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.LongProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleLongProperty;
import javafx.collections.ObservableList;
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
     * A PacketList stores all packets between two logical endpoints (generally, LogicalAddressMappings to Ipv4WithPorts).
     * The logical graph doesn't particularly care about directionality, so the data is not tracked with directionality, but
     * the session reports it as such.
     */
    public class PacketList implements XmlSerializable {
        /**
         * PacketMetadata objects are the structures found in the PROPERTY_PACKET_LIST collection.
         */
        public final class PacketMetadata implements XmlSerializable {
            private final String file;
            private final long frame;
            private final long time;
            private final short protocol;
            private final long size;
            private final boolean isMatchedDirection;

            public PacketMetadata(final IPacketMetadata metadata) {
                this.file = metadata.getImportSource().getPath().toAbsolutePath().toString();
                this.frame = metadata.getFrame() == null ? -1 : metadata.getFrame();
                this.time = metadata.getTime();
                this.protocol = metadata.getTransportProtocol();
                this.size = metadata.getImportProgress();
                this.isMatchedDirection = (metadata.getSourceAddress().equals(PacketList.this.getSourceAddress()));
            }

            public PacketMetadata(final XMLStreamReader reader, final boolean readDirection) throws XMLStreamException{
                this.file = reader.getAttributeValue(null, "File");
                this.frame = Long.parseLong(reader.getAttributeValue(null, "Frame"));
                this.time= Long.parseLong(reader.getAttributeValue(null, "Time"));
                this.protocol = Short.parseShort(reader.getAttributeValue(null, "Protocol"));
                this.size = Long.parseLong(reader.getAttributeValue(null, "Size"));
                if(readDirection) {
                    this.isMatchedDirection = reader.getAttributeValue(null, "Direction").equals("forward");
                } else {
                    this.isMatchedDirection = !reader.getAttributeValue(null, "Direction").equals("forward");
                }
            }

            @Override
            public void writeToXml(final XMLStreamWriter target) throws XMLStreamException {
                target.writeAttribute("File", this.file);
                target.writeAttribute("Frame", Long.toString(this.frame));
                target.writeAttribute("Time", Long.toString(this.time));
                target.writeAttribute("Protocol", Short.toString(this.protocol));
                target.writeAttribute("Size", Long.toString(this.size));
                if(this.isMatchedDirection) {
                    target.writeAttribute("Direction", "forward");
                } else {
                    target.writeAttribute("Direction", "reverse");
                }
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
            public IAddress getSourceAddress() {
                return this.isMatchedDirection ? PacketList.this.getSourceAddress() : PacketList.this.getDestinationAddress();
            }
            public IAddress getDestinationAddress() {
                return !this.isMatchedDirection ? PacketList.this.getSourceAddress() : PacketList.this.getDestinationAddress();
            }

            @Override
            public String toString() {
                return String.format("[%s]#%d: 0x%02x, %d bytes, %s -> %s", Instant.ofEpochMilli(this.time).atZone(ZoneId.of("Z")).format(DateTimeFormatter.ISO_DATE_TIME), this.frame, this.protocol, this.size, this.getSourceAddress(), this.getDestinationAddress());
            }
        }

        private final Edge<LogicalAddressMapping> forwardEdge;
        private Edge<LogicalAddressMapping> reverseEdge;

        private final ObservableList<PacketMetadata> packets;
        private final LongProperty totalPacketSize;
        private final ObservableSet<Short> protocols;

        private final LinkedList<PacketMetadata> pendingPackets;
        private final LinkedList<Short> pendingProtocols;
        private long pendingSize;

        private final PacketListState state;

        private final class PacketListState extends ThreadManagedState {
            public PacketListState(final Event.IAsyncExecutionProvider uiProvider) {
                super(RuntimeConfiguration.UPDATE_INTERVAL_MS, "PacketListState", uiProvider);
            }

            @Override
            public void validate() {
                if(this.hasFlag(PacketList.this.pendingPackets)) {
                    synchronized(PacketList.this.pendingPackets) {
                        PacketList.this.packets.addAll(PacketList.this.pendingPackets);
                        PacketList.this.pendingPackets.clear();
                    }
                }
                if(this.hasFlag(PacketList.this.pendingProtocols)) {
                    synchronized(PacketList.this.pendingProtocols) {
                        PacketList.this.protocols.addAll(PacketList.this.pendingProtocols);
                        synchronized(GraphLogicalEdge.this.protocols) {
                            GraphLogicalEdge.this.protocols.addAll(PacketList.this.pendingProtocols);
                        }
                        PacketList.this.pendingProtocols.clear();
                    }
                }
                if(this.hasFlag(PacketList.this.totalPacketSize)) {
                    synchronized(PacketList.this.totalPacketSize) {
                        PacketList.this.totalPacketSize.set(PacketList.this.totalPacketSize.get() + PacketList.this.pendingSize);
                        synchronized(GraphLogicalEdge.this.totalPacketSize) {
                            GraphLogicalEdge.this.totalPacketSize.set(GraphLogicalEdge.this.totalPacketSize.get() + PacketList.this.pendingSize);
                        }
                        PacketList.this.pendingSize = 0;
                    }
                }
            }
        }

        public PacketList(final Edge<LogicalAddressMapping> edgeForward, final Event.IAsyncExecutionProvider uiProvider) {
            this.forwardEdge = edgeForward;
            this.reverseEdge = null;

            this.packets = new ObservableListWrapper<>(new ArrayList<>());
            this.totalPacketSize = new SimpleLongProperty(0);
            this.protocols = new ObservableSetWrapper<>(new HashSet<>());

            this.pendingPackets = new LinkedList<>();
            this.pendingProtocols = new LinkedList<>();
            this.pendingSize = 0;

            this.state = new PacketListState(uiProvider);
        }

        public void setReverseEdge(final Edge<LogicalAddressMapping> edgeReverse) {
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
            return this.forwardEdge.getSource();
        }
        public LogicalAddressMapping getDestinationAddress() {
            return this.forwardEdge.getDestination();
        }

        public void addPacket(final PacketMetadata packet) {
            this.state.invalidate(this.pendingPackets, () -> this.pendingPackets.add(packet));
            this.state.invalidate(this.pendingProtocols, () -> this.pendingProtocols.add(packet.getTransportProtocol()));
            this.state.invalidate(this.totalPacketSize, () -> pendingSize += packet.getSize());
        }

        public ObservableList<PacketMetadata> getPackets() {
            return this.packets;
        }
        public LongProperty totalPacketSizeProperty() {
            return this.totalPacketSize;
        }
        public ObservableSet<Short> getProtocols() {
            return this.protocols;
        }

        //Hack for handling loading of edges where directionality is reversed.
        private boolean readDirection = true;
        public void setReadDirection(final boolean direction) {
            this.readDirection = direction;
        }

        @Override
        public void readFromXml(final XMLStreamReader reader) throws XMLStreamException {
            //the Source and Destination are expected to either match or not exist in the tream--they are written first so that the code calling this method can identify the correct PacketList instance that loads the remaining content.
            this.packets.clear();
            this.protocols.clear();
            this.totalPacketSize.set(0);

            boolean hasForward = false;
            boolean hasReverse = false;

            while(reader.hasNext()) {
                final int typeNext = reader.next();
                final String tag;
                switch(typeNext) {
                    case XMLEvent.START_ELEMENT:
                        tag = reader.getLocalName();
                        if(tag.equals("AggregateStats")) {
                            this.totalPacketSize.set(Long.parseLong(reader.getAttributeValue(null, "Size")));
                        } else if(tag.equals("Protocol")) {
                            this.protocols.add(Short.parseShort(reader.getElementText()));
                        } else if(tag.equals("Metadata")) {
                            //We bypass the default addPacket method and run it out-of-thread since there is no way that the changes made here can affect anything outside this object (we're in the constructor, after all)
                            final PacketMetadata metadata = new PacketMetadata(reader, this.readDirection);
                            this.packets.add(metadata);

                            hasForward |= metadata.isMatchedDirection;
                            hasReverse |= !metadata.isMatchedDirection;
                        }
                        break;
                    case XMLEvent.END_ELEMENT:
                        tag = reader.getLocalName();
                        if(tag.equals("PacketList")) {
                            if(hasForward && !this.readDirection || hasReverse && this.readDirection) {
                                GraphLogicalEdge.this.mapHasEndpointReceivedData.get(this.getSourceAddress()).set(true);
                            }
                            if(hasForward && this.readDirection || hasReverse && !this.readDirection) {
                                GraphLogicalEdge.this.mapHasEndpointReceivedData.get(this.getDestinationAddress()).set(true);
                            }
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
            for(short protocol : protocols) {
                writer.writeStartElement("Protocol");
                writer.writeCharacters(Short.toString(protocol));
                writer.writeEndElement();
            }
            writer.writeEndElement();

            writer.writeStartElement("Packets");
            for(final PacketMetadata packet : packets) {
                writer.writeStartElement("Metadata");
                packet.writeToXml(writer);
                writer.writeEndElement();
            }
            writer.writeEndElement();
        }
    }

    private GraphLogicalVertex source;
    private GraphLogicalVertex destination;

    private final String key;
    private final String keyReverse;

    private final Map<Edge<LogicalAddressMapping>, PacketList> lookupPacketListsByEdge;
    private final Map<Session.BidirectionalAddressPair, PacketList> lookupPacketListsByEndpoints;

    private final LinkedList<Edge<LogicalAddressMapping>> edgesPending;
    private final LinkedList<Session.PacketEvent> packetsPending;
    private final LinkedList<PacketList> packetListsPending;

    private final ObservableList<PacketList> packetLists;
    private final SimpleLongProperty totalPacketSize;
    private final SimpleLongProperty totalPacketCount;
    private final ObservableSet<Short> protocols;

    private final Map<LogicalAddressMapping, BooleanProperty> mapHasEndpointReceivedData;

    private class GraphLogicalEdgeThreadManagedState extends ThreadManagedState {
        public GraphLogicalEdgeThreadManagedState(final String title, final Event.IAsyncExecutionProvider uiProvider) {
            super(RuntimeConfiguration.UPDATE_INTERVAL_MS * 4, title, uiProvider);
        }

        @Override
        public void validate() {
            if(this.hasFlag(GraphLogicalEdge.this.edgesPending)) {
                final ArrayList<Edge<LogicalAddressMapping>> edgesNew;
                synchronized(GraphLogicalEdge.this.edgesPending) {
                    edgesNew = new ArrayList<>(GraphLogicalEdge.this.edgesPending);
                    GraphLogicalEdge.this.edgesPending.clear();
                }

                LinkedList<PacketList> listsNew = new LinkedList<>();
                for(final Edge<LogicalAddressMapping> edge : edgesNew) {
                    GraphLogicalEdge.this.addAncestor(edge);
                    final Session.BidirectionalAddressPair pairEdge = new Session.BidirectionalAddressPair(edge.getSource(), edge.getDestination());
                    //The PacketList will bind to the appropriate events.
                    final PacketList listExisting = lookupPacketListsByEndpoints.get(pairEdge);
                    if (listExisting != null) {
                        //The list exists, so this must be the reverse edge
                        listExisting.setReverseEdge(edge);
                        lookupPacketListsByEdge.put(edge, listExisting);
                    } else {
                        //The list doesn't exist, so we are creating a new list with this as the forward edge.
                        final PacketList listNew = new PacketList(edge, this.executionProvider);
                        listsNew.add(listNew);
                        lookupPacketListsByEndpoints.put(pairEdge, listNew);
                        lookupPacketListsByEdge.put(edge, listNew);
                    }
                }
                packetLists.addAll(listsNew);
            }
            if(this.hasFlag(GraphLogicalEdge.this.packetsPending)) {
                final ArrayList<Session.PacketEvent> packets;
                synchronized(GraphLogicalEdge.this.packetsPending) {
                    packets = new ArrayList<>(GraphLogicalEdge.this.packetsPending);
                    GraphLogicalEdge.this.packetsPending.clear();
                }

                long cbTotal = 0;
                final Set<Short> protocols = new HashSet<>();
                for(final Session.PacketEvent event : packets) {
                    cbTotal += event.getPacket().getImportProgress();
                    protocols.add(event.getPacket().getTransportProtocol());
                    final PacketList list = GraphLogicalEdge.this.lookupPacketListsByEdge.get(event.getEdge());
                    list.addPacket(list.new PacketMetadata(event.getPacket()));
                }
                GraphLogicalEdge.this.protocols.addAll(protocols);
                GraphLogicalEdge.this.totalPacketSize.set(GraphLogicalEdge.this.totalPacketSize.get() + cbTotal);
                GraphLogicalEdge.this.totalPacketCount.set(GraphLogicalEdge.this.totalPacketCount.get() + packets.size());
            }
            if(this.hasFlag(GraphLogicalEdge.this.packetListsPending)) {
                //this is only invalidated in load code; we aren't worried about race conditions in this code.
                GraphLogicalEdge.this.packetLists.addAll(GraphLogicalEdge.this.packetListsPending);
                for(final PacketList list : GraphLogicalEdge.this.packetListsPending) {
                    GraphLogicalEdge.this.protocols.addAll(list.getProtocols());
                    GraphLogicalEdge.this.totalPacketSize.set(GraphLogicalEdge.this.totalPacketSize.get() + list.totalPacketSize.get());
                    GraphLogicalEdge.this.totalPacketCount.set(GraphLogicalEdge.this.totalPacketCount.get() + list.getPackets().size());
                }
                GraphLogicalEdge.this.packetListsPending.clear();
            }
        }
    }
    private final ThreadManagedState state;

    public GraphLogicalEdge(final GraphLogicalVertex source, final GraphLogicalVertex destination, final Event.IAsyncExecutionProvider uiProvider) {
        super(uiProvider);

        this.source = source;
        this.destination = destination;

        this.key = String.format("[%s->%s]", source.getKey(), destination.getKey());
        this.keyReverse = String.format("[%s->%s]", destination.getKey(), source.getKey());

        this.state = new GraphLogicalEdgeThreadManagedState(this.toString(), uiProvider);

        this.lookupPacketListsByEdge = new HashMap<>();
        this.lookupPacketListsByEndpoints = new HashMap<>();

        this.edgesPending = new LinkedList<>();
        this.packetsPending = new LinkedList<>();
        this.packetListsPending = new LinkedList<>();

        this.packetLists = new ObservableListWrapper<>(new ArrayList<>());
        this.totalPacketSize = new SimpleLongProperty(0);
        this.totalPacketCount = new SimpleLongProperty(0);
        this.protocols = new ObservableSetWrapper<>(new HashSet<>());

        this.mapHasEndpointReceivedData = new HashMap<>();
    }

    public void trackEdge(final Edge<LogicalAddressMapping> edge) {
        this.state.invalidate(this.edgesPending, () -> this.edgesPending.add(edge));
    }

    public void addPacket(final Session.PacketEvent packet) {
        synchronized(this.mapHasEndpointReceivedData) {
            final IAddress dest = packet.getPacket().getDestAddress();
            if(dest instanceof LogicalAddressMapping) {
                final LogicalAddressMapping addr = (LogicalAddressMapping)dest;
                if (!this.mapHasEndpointReceivedData.containsKey(addr)) {
                    this.mapHasEndpointReceivedData.put(addr, new FxBooleanProperty(true));
                } else {
                    this.mapHasEndpointReceivedData.get(addr).set(true);
                }

                if(this.getSource().getRootLogicalAddressMapping().contains(addr)) {
                    final BooleanProperty propRoot = this.mapHasEndpointReceivedData.get(this.getSource().getRootLogicalAddressMapping());
                    if(propRoot != null) {
                        propRoot.set(true);
                    } else {
                        this.mapHasEndpointReceivedData.put(this.getSource().getRootLogicalAddressMapping(), new FxBooleanProperty(true));
                    }
                } else {
                    //Assume destination match
                    final BooleanProperty propRoot = this.mapHasEndpointReceivedData.get(this.getDestination().getRootLogicalAddressMapping());
                    if(propRoot != null) {
                        propRoot.set(true);
                    } else {
                        this.mapHasEndpointReceivedData.put(this.getDestination().getRootLogicalAddressMapping(), new FxBooleanProperty(true));
                    }
                }
            }
        }
        this.state.invalidate(this.packetsPending, () -> this.packetsPending.add(packet));
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

    public ObservableList<PacketList> getPacketLists() {
        return this.packetLists;
    }

    public LongExpression totalPacketSizeProperty() {
        return this.totalPacketSize;
    }

    public LongExpression totalPacketCountProperty() {
        return this.totalPacketCount;
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
        this.state.waitForValid();
        while(reader.hasNext()) {
            final int typeNext = reader.next();
            final String tag;
            switch(typeNext) {
                case XMLEvent.START_ELEMENT:
                    tag = reader.getLocalName();
                    if(tag.equals("AggregateStats")) {
                        GraphLogicalEdge.this.totalPacketCount.set(Integer.parseInt(reader.getAttributeValue(null, "Count")));
                        GraphLogicalEdge.this.totalPacketSize.set(Integer.parseInt(reader.getAttributeValue(null, "Size")));
                    } else if(tag.equals("Protocol")) {
                        GraphLogicalEdge.this.protocols.add(Short.parseShort(reader.getElementText()));
                    } else if(tag.equals("PacketList")) {
                        //The PacketLists have already been restored from loading edges earlier--we now need to identify each PacketList to an edge and load the content
                        //A PacketList writes the source/destination first, so we can extract those to identify the existing PacketList, then load the content to the PacketList
                        final LogicalAddressMapping source = Loader.readNextObject(reader, LogicalAddressMapping.class);
                        final LogicalAddressMapping destination = Loader.readNextObject(reader, LogicalAddressMapping.class);
                        this.mapHasEndpointReceivedData.putIfAbsent(source, new FxBooleanProperty(false));
                        this.mapHasEndpointReceivedData.putIfAbsent(destination, new FxBooleanProperty(false));

                        final PacketList listExisting = lookupPacketListsByEndpoints.get(new Session.BidirectionalAddressPair(source, destination));
                        listExisting.setReadDirection(listExisting.getSourceAddress().equals(source));
                        listExisting.readFromXml(reader);
                    } else if(tag.equals("PropertyCloud")) {
                        super.readFromXml(reader);
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
        //The key will match the source/destination, so we don't need to track those.
        writer.writeAttribute("key", this.getKey());

        writer.writeStartElement("AggregateStats");
        writer.writeAttribute("Size", Long.toString(totalPacketSize.get()));
        writer.writeAttribute("Count", Long.toString(totalPacketCount.get()));
        for(short protocol : protocols) {
            writer.writeStartElement("Protocol");
            writer.writeCharacters(Short.toString(protocol));
            writer.writeEndElement();
        }
        writer.writeEndElement();

        for(final PacketList packetList : packetLists) {
            writer.writeStartElement("PacketList");
            packetList.writeToXml(writer);
            writer.writeEndElement();
        }

        writer.writeStartElement("PropertyCloud");
        super.writeToXml( writer);
        writer.writeEndElement();
    }
}
