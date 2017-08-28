package grassmarlin.plugins.internal.physicalview.graph;

import grassmarlin.Event;
import grassmarlin.plugins.internal.physicalview.StageProcessPhysicalGraphElements;
import grassmarlin.session.Edge;
import grassmarlin.session.HardwareAddress;
import grassmarlin.session.TopLevelLogicalAddressMappingList;
import grassmarlin.session.pipeline.LogicalAddressMapping;

import java.util.*;
import java.util.stream.Collectors;


/**
 * A Segment is a collection of interconnected HardwareAddresses.
 * The addresses may be organized using Switches.
 * HardwareAddresses are only added upon creation or by absorbing another Segment--the base segment contains a single address.
 * LogicalAddressMappings are added to a segment, then assessed to see if a gateway can be identified.
 */
public class Segment {
    private final List<HardwareAddress> addresses;
    private final Set<HardwareAddress> gateways;
    private final Set<Edge<HardwareAddress>> edges;
    private final List<Switch> switches;
    private final Map<HardwareAddress, Set<String>> annotations;

    private final TopLevelLogicalAddressMappingList topLevelLogicalAddresses;

    public class SegmentAddressAddedArgs {
        private final HardwareAddress address;

        public SegmentAddressAddedArgs(final HardwareAddress address) {
            this.address = address;
        }

        public HardwareAddress getAddress() {
            return this.address;
        }
        public Segment getSegment() {
            return Segment.this;
        }
    }

    public class AnnotationAddedArgs {
        private final HardwareAddress address;
        private final String annotation;

        public AnnotationAddedArgs(final HardwareAddress address, final String annotation) {
            this.address = address;
            this.annotation = annotation;
        }

        public HardwareAddress getAddress() {
            return this.address;
        }
        public String getAnnotation() {
            return this.annotation;
        }
        public Segment getSegment() {
            return Segment.this;
        }
    }
    public final Event<SegmentAddressAddedArgs> onAddressAdded;   //Addresses can only ever be added to segments.  Segments can be removed, but addresses are not removed from them.
    public final Event<Segment> onStructureChanged;  //Changing gateways or switches will require a reevaluation of the segment layout, so the specific event is irrelevant, the result is the same.
    public final Event<AnnotationAddedArgs> onAnnotationAdded;
    public final Event<TopLevelLogicalAddressMappingList.LogicalAddressMappingEventArgs> onTopLevelLogicalAddressAdded;
    public final Event<TopLevelLogicalAddressMappingList.LogicalAddressMappingEventArgs> onTopLevelLogicalAddressRemoved;

    private Segment(final Event.IAsyncExecutionProvider executionProvider) {
        this.addresses = new ArrayList<>();
        this.gateways = new HashSet<>();
        this.edges = new HashSet<>();
        this.switches = new ArrayList<>();
        this.annotations = new HashMap<>();

        this.onAddressAdded = new Event<>(executionProvider);
        this.onStructureChanged = new Event<>(executionProvider);
        this.onAnnotationAdded = new Event<>(executionProvider);

        this.topLevelLogicalAddresses = new TopLevelLogicalAddressMappingList(executionProvider);
        this.topLevelLogicalAddresses.onNewTopLevelAddress.addHandler(this.handlerNewTopLevelAddress);
        this.topLevelLogicalAddresses.onRemovedTopLevelAddress.addHandler(this.handlerRemovedTopLevelAddress);

        this.onTopLevelLogicalAddressAdded = this.topLevelLogicalAddresses.onNewTopLevelAddress;
        this.onTopLevelLogicalAddressRemoved = this.topLevelLogicalAddresses.onRemovedTopLevelAddress;
    }

    private void detachHandlers() {
        this.topLevelLogicalAddresses.onNewTopLevelAddress.removeHandler(this.handlerNewTopLevelAddress);
        this.topLevelLogicalAddresses.onRemovedTopLevelAddress.removeHandler(this.handlerRemovedTopLevelAddress);
    }

    public Segment(final Event.IAsyncExecutionProvider executionProvider, final HardwareAddress address) {
        this(executionProvider);

        this.addresses.add(address);
    }

    public Segment(final Event.IAsyncExecutionProvider executionProvider, final Segment... segments) {
        this(executionProvider);

        for(final Segment segment : segments) {
            this.absorbSegment(segment);
        }
    }

    private final Event.EventListener<TopLevelLogicalAddressMappingList.LogicalAddressMappingEventArgs> handlerNewTopLevelAddress = this::handleNewTopLevelAddress;
    private final Event.EventListener<TopLevelLogicalAddressMappingList.LogicalAddressMappingEventArgs> handlerRemovedTopLevelAddress = this::handleRemovedTopLevelAddress;

    public void absorbSegment(final Segment segment) {
        segment.detachHandlers();

        this.addresses.addAll(segment.addresses);
        for(final HardwareAddress address : segment.addresses) {
            this.onAddressAdded.call(new SegmentAddressAddedArgs(address));
        }
        this.gateways.addAll(segment.gateways);
        this.switches.addAll(segment.switches);

        for(final LogicalAddressMapping mapping : segment.topLevelLogicalAddresses.getAllMappings()) {
            topLevelLogicalAddresses.addMapping(mapping);
        }
        for(final HardwareAddress address : segment.annotations.keySet()) {
            this.annotations.put(address, segment.annotations.get(address));
            for(final String annotation : segment.annotations.get(address)) {
                this.onAnnotationAdded.call(new AnnotationAddedArgs(address, annotation));
            }
        }

        this.onStructureChanged.call(this);
    }

    public void addMapping(final LogicalAddressMapping mapping) {
        topLevelLogicalAddresses.addMapping(mapping);
        this.onStructureChanged.call(this);
    }

    public void addEdge(final Edge<HardwareAddress> edge) {
        if(edges.add(edge)) {
            onStructureChanged.call(this);
        }
    }

    public void addSwitch(final Switch sWitch) {
        if(switches.add(sWitch)) {
            onStructureChanged.call(this);
        }
    }

    public void addAnnotation(final HardwareAddress address, final String annotation) {
        Set<String> annotations = this.annotations.get(address);
        if(annotations == null) {
            annotations = new HashSet<>();
            this.annotations.put(address, annotations);
        }

        if(annotations.add(annotation)) {
            onAnnotationAdded.call(new AnnotationAddedArgs(address, annotation));
        }
    }

    public void addAnnotations(final HardwareAddress address, final Collection<String> annotationsNew) {
        Set<String> annotations = this.annotations.get(address);
        if(annotations == null) {
            annotations = new HashSet<>();
            this.annotations.put(address, annotations);
        }

        for(final String annotation : annotationsNew) {
            if (annotations.add(annotation)) {
                onAnnotationAdded.call(new AnnotationAddedArgs(address, annotation));
            }
        }
    }

    private void handleNewTopLevelAddress(final Event<TopLevelLogicalAddressMappingList.LogicalAddressMappingEventArgs> event, final TopLevelLogicalAddressMappingList.LogicalAddressMappingEventArgs args) {
        if(this.topLevelLogicalAddresses.topLevelMappingsFor(args.getMapping().getHardwareAddress()).size() > 1) {
            this.gateways.add(args.getMapping().getHardwareAddress());
            addAnnotation(args.getMapping().getHardwareAddress(), StageProcessPhysicalGraphElements.ANNOTATION_GATEWAY);
        }
    }
    private void handleRemovedTopLevelAddress(final Event<TopLevelLogicalAddressMappingList.LogicalAddressMappingEventArgs> event, final TopLevelLogicalAddressMappingList.LogicalAddressMappingEventArgs args) {
        if(this.topLevelLogicalAddresses.topLevelMappingsFor(args.getMapping().getHardwareAddress()).size() <= 1) {
            this.gateways.remove(args.getMapping().getHardwareAddress());
        }
    }

    public List<HardwareAddress> getAddresses() {
        return this.addresses;
    }
    public Set<HardwareAddress> getGateways() {
        return this.gateways;
    }
    public List<Switch> getSwitches() {
        return this.switches;
    }
    public Collection<Edge<HardwareAddress>> getEdges() {
        return this.edges;
    }

    public Set<String> annotationsFor(final HardwareAddress address) {
        return this.annotations.get(address);
    }

    @Override
    public boolean equals(final Object other) {
        if(other instanceof Segment) {
            final List<HardwareAddress> otherAddresses = ((Segment)other).addresses;

            return this.topLevelLogicalAddresses.equals(((Segment) other).topLevelLogicalAddresses) &&
                    this.addresses.size() == otherAddresses.size() &&
                    this.addresses.containsAll(otherAddresses);
        } else {
            return false;
        }
    }

    @Override
    public String toString() {
        final StringBuilder result = new StringBuilder();

        result.append("[Segment");
        result.append(this.addresses.stream().map(address -> address.toString()).collect(Collectors.joining(", ", " Addresses:{", "}")));
        result.append(this.gateways.stream().map(gateway -> gateway.toString()).collect(Collectors.joining(", ", " Gateways:{", "}")));
        result.append(this.switches.stream().map(device -> device.toString()).collect(Collectors.joining(", ", " Switches:{", "}")));

        return result.toString();
    }
}
