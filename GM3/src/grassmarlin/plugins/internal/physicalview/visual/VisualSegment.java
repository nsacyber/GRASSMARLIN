package grassmarlin.plugins.internal.physicalview.visual;

import grassmarlin.Event;
import grassmarlin.plugins.internal.physicalview.graph.Segment;
import grassmarlin.plugins.internal.physicalview.graph.Switch;
import grassmarlin.session.HardwareAddress;
import grassmarlin.session.TopLevelLogicalAddressMappingList;
import grassmarlin.ui.common.ImageDirectoryWatcher;
import grassmarlin.ui.common.ZoomableScrollPane;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.image.Image;

import java.util.*;
import java.util.stream.Collectors;

public class VisualSegment implements ZoomableScrollPane.IMultiLayeredNode {
    private final Segment segment;
    private final ImageDirectoryWatcher<Image> watcher;

    // Endpoint devices within this segment.
    private final Group endpoints;
    // Defined devices that reduce the effective group of endpoints by removing things known to be part of switches.
    private final Group devices;
    private final Group wires;

    private final List<VisualSwitch> visualSwitches;

    private final Map<HardwareAddress, IHasControlPoint> lookupEndpoints;
    private final List<Wire> wireList;

    public VisualSegment(final Segment segment, final ImageDirectoryWatcher<Image> watcher) {
        this.segment = segment;
        this.watcher = watcher;

        this.endpoints = new Group();
        this.devices = new Group();
        this.wires = new Group();

        this.visualSwitches = new ArrayList<>();

        this.lookupEndpoints = new HashMap<>();
        this.wireList = new ArrayList<>();

        this.segment.onStructureChanged.addHandler(this.handlerStructureChanged);
        this.segment.onAddressAdded.addHandler(this.handlerAddressAdded);
        this.segment.onTopLevelLogicalAddressAdded.addHandler(this.handlerMappingsChanged);
        this.segment.onTopLevelLogicalAddressRemoved.addHandler(this.handlerMappingsChanged);
        this.segment.onAnnotationAdded.addHandler(this.handlerAnnotationAdded);

        for(final HardwareAddress address : this.segment.getAddresses()) {
            this.addAddress(address);
        }
    }

    private final Event.EventListener<Segment> handlerStructureChanged = this::handleStructureChanged;
    private final Event.EventListener<Segment.SegmentAddressAddedArgs> handlerAddressAdded = this::handleAddressAdded;
    private final Event.EventListener<TopLevelLogicalAddressMappingList.LogicalAddressMappingEventArgs> handlerMappingsChanged = this::handleMappingsChanged;
    private final Event.EventListener<Segment.AnnotationAddedArgs> handlerAnnotationAdded = this::handleAnnotationAdded;

    void detachHandlers() {
        this.segment.onStructureChanged.removeHandler(this.handlerStructureChanged);
        this.segment.onAddressAdded.removeHandler(this.handlerAddressAdded);
        this.segment.onTopLevelLogicalAddressAdded.removeHandler(this.handlerMappingsChanged);
        this.segment.onTopLevelLogicalAddressRemoved.removeHandler(this.handlerMappingsChanged);
        this.segment.onAnnotationAdded.removeHandler(this.handlerAnnotationAdded);
    }

    private void handleAnnotationAdded(final Event<Segment.AnnotationAddedArgs> event, final Segment.AnnotationAddedArgs args) {
        //TODO: Annotations are being replaced with properties, I think.
        //lookupEndpoints.get(args.getAddress()).updateAnnotations(args.getSegment().annotationsFor(args.getAddress()));
    }

    private void handleMappingsChanged(final Event<TopLevelLogicalAddressMappingList.LogicalAddressMappingEventArgs> event, final TopLevelLogicalAddressMappingList.LogicalAddressMappingEventArgs args) {
        //TODO: The lookup no longer operates on VisualEndpoints alone; this may need a re-evaluation for actions to take when dealing with other types.
        final IHasControlPoint target = lookupEndpoints.get(args.getMapping().getHardwareAddress());
        if(target instanceof VisualEndpoint) {
            final VisualEndpoint endpoint = (VisualEndpoint)target;
            if (event == this.segment.onTopLevelLogicalAddressAdded) {
                endpoint.addMapping(args.getMapping());
            } else {
                endpoint.removeMapping(args.getMapping());
            }
        }
    }

    private void buildSwitches() {
        //Results of the algorithm:
        final List<Wire> wires = new LinkedList<>();
        final List<VisualSwitch> switches = new LinkedList<>();
        final List<VisualCloud> clouds = new LinkedList<>();

        //State-tracking for the algorithm
        final Map<HardwareAddress, Switch.Port> lookupPorts = new HashMap<>();
        final Map<HardwareAddress, Set<HardwareAddress>> subsegments = new HashMap<>();

        //Step 0: Clear old data.
        //HACK: We do a scorched-earth approach here.  We really need to do a more elegant replace-the-minimum-necessary so that visual state information isn't lost.
        this.devices.getChildren().clear();
        this.wires.getChildren().clear();

        //Step 1: Add all addresses that belong to this segment to the subsegment list as single-node subsegments.
        for(final HardwareAddress address : this.segment.getAddresses()) {
            final Set<HardwareAddress> set = new HashSet<>();
            set.add(address);
            subsegments.put(address, set);
        }

        /*
        System.out.println(" == Step 1 ==");
        System.out.println("Addresses:");
        for(final HardwareAddress address : this.segment.getAddresses()) {
            System.out.println(address);
        }
        System.out.println("\nSubsegments:");
        for(final Map.Entry<HardwareAddress, Set<HardwareAddress>> entry : subsegments.entrySet()) {
            System.out.println(entry.getKey() + " -> " + entry.getValue());
        }
        */

        //Step 1.5: Build the switch objects and the lookup for address-to-port
        for(final Switch sWitch : segment.getSwitches()) {
            final VisualSwitch visual = new VisualSwitch(sWitch);
            switches.add(visual);

            //Since the addresses were added via addAddress, we have to clear the VisualEndpoints that were automatically created, but we only do this for things belonging to the switch itself.
            for(final Switch.Port port : sWitch.getPortGroups().stream().flatMap(group -> group.getPorts().keySet().stream()).collect(Collectors.toList())) {
                final HardwareAddress address = port.getAddress();
                lookupPorts.put(address, port);
                this.endpoints.getChildren().remove(this.lookupEndpoints.put(address, visual.getPortMapping().get(port)));
            }
        }

        //Step 2: Process non-trunk ports.
        // The Mac Address Table on a switch will contain every sender's address for every packet received by the corresponding interface (well, ignoring expiration, but with a complete picture, this should be only one device per switch port, so it becomes moot)
        final List<HardwareAddress> addressesForTrunkPorts = segment.getSwitches().stream()
                .flatMap(dev -> dev.getPortGroups().stream())
                .flatMap(group -> group.getPorts().keySet().stream())
                .filter(port -> port.isTrunk())
                .map(port -> port.getAddress())
                .collect(Collectors.toList());
        final List<HardwareAddress> addressesForNonTrunkPorts = segment.getSwitches().stream()
                .flatMap(dev -> dev.getPortGroups().stream())
                .flatMap(group -> group.getPorts().keySet().stream())
                .filter(port -> !port.isTrunk())
                .map(port -> port.getAddress())
                .collect(Collectors.toList());
        final List<HardwareAddress> addressesForAllSwitchPorts = segment.getSwitches().stream()
                .flatMap(dev -> dev.getPortGroups().stream())
                .flatMap(group -> group.getPorts().keySet().stream())
                .map(port -> port.getAddress())
                .collect(Collectors.toList());

        for(final HardwareAddress addressNonTrunk : addressesForNonTrunkPorts) {
            mergeLists(subsegments, addressNonTrunk, lookupPorts.get(addressNonTrunk).getConnectedAddresses());
        }

        /*
        System.out.println(" == Step 2 ==");
        System.out.println("Addresses for Trunk Ports:");
        for(final HardwareAddress address : addressesForTrunkPorts) {
            System.out.println(address);
        }
        System.out.println("\nAddresses for Non-Trunk Ports:");
        for(final HardwareAddress address : addressesForNonTrunkPorts) {
            System.out.println(address);
        }
        System.out.println("\nAddresses for switch Ports:");
        for(final HardwareAddress address : addressesForAllSwitchPorts) {
            System.out.println(address);
        }
        System.out.println("\nSubsegments:");
        for(final Map.Entry<HardwareAddress, Set<HardwareAddress>> entry : subsegments.entrySet()) {
            System.out.println(entry.getKey() + " -> " + entry.getValue());
        }
        */

        //Step 3: Merge trunk ports with connected switch (not necessarily trunk) ports.
        // We connect to all switch ports since misconfiguration of trunk ports where one side is a trunk and the other isn't is rather common, and this generally doesn't have an adverse impact on the graph
        for(final HardwareAddress addressTrunk : addressesForTrunkPorts) {
            mergeLists(subsegments, addressTrunk, lookupPorts.get(addressTrunk).getConnectedAddresses().stream()
                    .filter(connected -> addressesForAllSwitchPorts.contains(connected))
                    .collect(Collectors.toList())
            );
        }

        /*
        System.out.println(" == Step 3 ==");
        System.out.println("\nSubsegments:");
        for(final Map.Entry<HardwareAddress, Set<HardwareAddress>> entry : subsegments.entrySet()) {
            System.out.println(entry.getKey() + " -> " + entry.getValue());
        }
        */

        //Step 4A: Find all sets that contain single nodes...
        final List<HardwareAddress> addressesIsolated = subsegments.entrySet().stream().filter(entry -> entry.getValue().size() == 1).map(entry -> entry.getKey()).collect(Collectors.toList());
        // And look for trunk ports where they can be inserted.
        for(final HardwareAddress addressIsolated : addressesIsolated) {
            mergeLists(subsegments, addressIsolated, addressesForTrunkPorts.stream()
                    .map(address -> lookupPorts.get(address))
                    .filter(port -> port.getConnectedAddresses().contains(addressIsolated))
                    .flatMap(port -> subsegments.get(port.getAddress()).stream())
                    .distinct()
                    .collect(Collectors.toList()));
        }

        /*
        System.out.println(" == Step 4A ==");
        System.out.println("Isolated Addresses:");
        for(final HardwareAddress address : addressesIsolated) {
            System.out.println(address);
        }
        System.out.println("\nSubsegments:");
        for(final Map.Entry<HardwareAddress, Set<HardwareAddress>> entry : subsegments.entrySet()) {
            System.out.println(entry.getKey() + " -> " + entry.getValue());
        }
        */

        //Step 4B: If there are any remaining nodes, connect them according to the defined edges.
        // These are nodes that would have originated from pcap.  As such, we ignore anything that is a switch port.
        final List<HardwareAddress> addressesIsolated2 = subsegments.entrySet().stream().filter(entry -> entry.getValue().size() == 1 && !addressesForAllSwitchPorts.contains(entry.getKey())).map(entry -> entry.getKey()).collect(Collectors.toList());
        for(final HardwareAddress addressIsolated : addressesIsolated2) {
            //Find all the endpoints
            final List<HardwareAddress> otherEndpoints = this.segment.getEdges().stream().filter(edge -> edge.getSource().equals(addressIsolated) || edge.getDestination().equals(addressIsolated)).map(edge -> edge.getSource().equals(addressIsolated) ? edge.getDestination() : edge.getSource()).distinct().collect(Collectors.toList());
            final Set<HardwareAddress> allCloudAddresses = new HashSet<>();
            for(final HardwareAddress address : otherEndpoints) {
                allCloudAddresses.addAll(subsegments.get(address));
            }
            mergeLists(subsegments, addressIsolated, allCloudAddresses);
        }

        /*
        System.out.println(" == Step 4B ==");
        System.out.println("Isolated Addresses:");
        for(final HardwareAddress address : addressesIsolated2) {
            System.out.println(address);
        }
        System.out.println("\nSubsegments:");
        for(final Map.Entry<HardwareAddress, Set<HardwareAddress>> entry : subsegments.entrySet()) {
            System.out.println(entry.getKey() + " -> " + entry.getValue());
        }
        */

        //Step 5: Turn the subsegment list into a list of wires and a list of clouds.
        for(final Set<HardwareAddress> subsegment : subsegments.values().stream().distinct().collect(Collectors.toList())) {
            if(subsegment.size() < 2) {
                //In theory this is a problem, but in reality, it happens all the time while building the graph. (address added before connection)
                //Logger.log(Logger.Severity.WARNING, "A segment of the physical graph was unexpectedly isolated: %s", subsegment);
            } else if(subsegment.size() == 2) {
                final Iterator<HardwareAddress> i = subsegment.iterator();
                final HardwareAddress source = i.next();
                final HardwareAddress destination = i.next();
                wires.add(new Wire(lookupEndpoints.get(source), lookupEndpoints.get(destination)));
            } else {
                final VisualCloud cloud = new VisualCloud();
                clouds.add(cloud);
                final List<VisualEndpoint> endpoints = new LinkedList<>();
                for(final HardwareAddress address : subsegment) {
                    final IHasControlPoint point = lookupEndpoints.get(address);
                    wires.add(new Wire(point, cloud));
                    if(point instanceof VisualEndpoint) {
                        endpoints.add((VisualEndpoint)point);
                    }
                }
                cloud.setConnectedEndpoints(endpoints);
            }
        }

        //Add the visual entities to the display and clean up the existing elements.
        // Clean up any existing clouds.
        final Iterator<Node> i = this.endpoints.getChildren().iterator();
        while(i.hasNext()) {
            final Node next = i.next();
            if(next instanceof VisualCloud) {
                i.remove();
            }
        }
        this.endpoints.getChildren().addAll(clouds);

        this.devices.getChildren().clear();
        this.devices.getChildren().addAll(switches);
        this.visualSwitches.clear();
        this.visualSwitches.addAll(switches);
        // Clean up any existing wires.
        this.wireList.clear();
        this.wireList.addAll(wires);
        this.wires.getChildren().clear();
        this.wires.getChildren().addAll(wires);

        for(final VisualSwitch sWitch : switches) {
            final Collection<VisualSwitchPort> ports = sWitch.getPortMapping().values();
            sWitch.setConnectedEndpoints(wires.stream()
                    .filter(wire -> ports.contains(wire.getStart()) || ports.contains(wire.getStop()))
                    .map(wire -> ports.contains(wire.getStart()) ? wire.getStop() : wire.getStart())
                    .filter(hascontrolpoint -> hascontrolpoint instanceof VisualEndpoint)
                    .map(hascontrolpoint -> (VisualEndpoint)hascontrolpoint)
                    .collect(Collectors.toList()));
        }
    }

    private static void mergeLists(final Map<HardwareAddress, Set<HardwareAddress>> subsegments, final HardwareAddress address, final Collection<HardwareAddress>... lists) {
        final Collection<HardwareAddress>[] array = Arrays.copyOf(lists, lists.length + 1);
        array[array.length - 1] = Arrays.asList(address);
        mergeLists(subsegments, array);
    }

    private static void mergeLists(final Map<HardwareAddress, Set<HardwareAddress>> subsegments, final Collection<HardwareAddress>... lists) {
        final Set<HardwareAddress> setNew = new HashSet<>();
        final List<HardwareAddress> addresses = Arrays.stream(lists).flatMap(list -> list.stream()).distinct().collect(Collectors.toList());

        for(final HardwareAddress address : addresses) {
            setNew.addAll(subsegments.put(address, setNew));
        }
    }

    private void handleStructureChanged(final Event<Segment> event, final Segment segment) {
        this.buildSwitches();
    }

    private void handleAddressAdded(final Event<Segment.SegmentAddressAddedArgs> event, final Segment.SegmentAddressAddedArgs args) {
        this.addAddress(args.getAddress());
    }

    public void addAddress(final HardwareAddress address) {
        final VisualEndpoint visualEndpoint = new VisualEndpoint(this.segment, address, watcher);
        this.endpoints.getChildren().add(visualEndpoint);
        this.lookupEndpoints.put(address, visualEndpoint);
    }

    @Override
    public Node nodeForLayer(final String layer) {
        switch(layer) {
            case PhysicalVisualization.LAYER_DEVICES:
                return this.devices;
            case PhysicalVisualization.LAYER_ENDPOINTS:
                return this.endpoints;
            case PhysicalVisualization.LAYER_WIRES:
                return this.wires;
            default:
                return null;
        }
    }

    public List<VisualSwitch> getVisualSwitches() {
        return new ArrayList<>(this.visualSwitches);
    }

    public IHasControlPoint endpointFor(final HardwareAddress address) {
        return this.lookupEndpoints.get(address);
    }

    public Collection<Wire> wiresConnectedTo(final HardwareAddress address) {
        return wiresConnectedTo(lookupEndpoints.get(address));

    }
    public Collection<Wire> wiresConnectedTo(final IHasControlPoint endpoint) {
        if(endpoint == null) {
            return new ArrayList<>();
        }
        return this.wireList.stream().filter(wire -> wire.connectsTo(endpoint)).collect(Collectors.toList());
    }
}
