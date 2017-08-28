package grassmarlin.plugins.internal.physicalview;

import grassmarlin.Event;
import grassmarlin.Launcher;
import grassmarlin.Logger;
import grassmarlin.RuntimeConfiguration;
import grassmarlin.plugins.internal.physicalview.graph.Router;
import grassmarlin.plugins.internal.physicalview.graph.Segment;
import grassmarlin.plugins.internal.physicalview.graph.Switch;
import grassmarlin.session.Edge;
import grassmarlin.session.HardwareAddress;
import grassmarlin.session.ThreadManagedState;
import grassmarlin.session.pipeline.LogicalAddressMapping;

import java.util.*;
import java.util.stream.Collectors;

public abstract class PhysicalGraph {
    //Primary data storage
    private final List<Segment> segments;
    private final List<Router> routers;

    public class SegmentEventArgs {
        private final Segment segment;
        private final boolean isValid;

        public SegmentEventArgs(final Segment segment, final boolean isValid) {
            this.segment = segment;
            this.isValid = isValid;
        }

        public Segment getSegment() {
            return this.segment;
        }
        public boolean isValid() {
            return this.isValid;
        }
        public PhysicalGraph getGraph() {
            return PhysicalGraph.this;
        }
    }
    public class RouterEventArgs {
        private final Router router;
        private final boolean isValid;

        public RouterEventArgs(final Router router, final boolean isValid) {
            this.router = router;
            this.isValid = isValid;
        }

        public Router getRouter() {
            return this.router;
        }
        public boolean isValid() {
            return this.isValid;
        }
        public PhysicalGraph getGraph() {
            return PhysicalGraph.this;
        }
    }

    private class PhysicalGraphState extends ThreadManagedState {
        private final Set<Switch> pendingSwitches = new HashSet<>();
        private final Set<HardwareAddress> pendingAddresses = new HashSet<>();
        private final List<Edge<HardwareAddress>> pendingEdges = new LinkedList<>();
        private final List<LogicalAddressMapping> pendingMappings = new LinkedList<>();
        private final Map<HardwareAddress, Set<String>> pendingAnnotations = new HashMap<>();

        public PhysicalGraphState(Event.IAsyncExecutionProvider executionProvider) {
            super(RuntimeConfiguration.UPDATE_INTERVAL_MS, "PhysicalGraph", executionProvider);
        }

        public void validate() {
            try {
                if (hasFlag(pendingAddresses) || hasFlag(pendingAnnotations)) {
                    for (HardwareAddress address : pendingAddresses) {
                        if (!segmentForHardwareAddress.containsKey(address)) {
                            final Segment newSegment = new Segment(this.executionProvider);
                            newSegment.getAddresses().add(address);
                            segments.add(newSegment);
                            PhysicalGraph.this.onSegmentsModified.call(new SegmentEventArgs(newSegment, true));
                            segmentForHardwareAddress.put(address, newSegment);
                        }
                    }
                    pendingAddresses.clear();
                }
                if (hasFlag(pendingEdges)) {
                    for (final Edge<HardwareAddress> edge : pendingEdges) {
                        final Segment first = segmentForHardwareAddress.get(edge.getSource());
                        final Segment second = segmentForHardwareAddress.get(edge.getDestination());

                        //If the edge crosses a segment boundary, we need to merge the segments
                        if (first != second) {
                            final Segment remaining;
                            final Segment replaced;
                            if (first.getAddresses().size() > second.getAddresses().size()) {
                                remaining = first;
                                replaced = second;
                            } else {
                                remaining = second;
                                replaced = first;
                            }
                            remaining.absorbSegment(replaced);
                            for (final HardwareAddress key : replaced.getAddresses()) {
                                segmentForHardwareAddress.put(key, remaining);
                            }
                            segments.remove(replaced);
                            PhysicalGraph.this.onSegmentsModified.call(new SegmentEventArgs(replaced, false));

                            //Now that the segments have the right set of nodes, add the new edge
                            remaining.addEdge(edge);
                        }
                    }
                    pendingEdges.clear();
                }
                if (hasFlag(pendingMappings)) {
                    for (final LogicalAddressMapping mapping : pendingMappings) {
                        segmentForHardwareAddress.get(mapping.getHardwareAddress()).addMapping(mapping);
                    }
                    pendingMappings.clear();
                }
                if(hasFlag(pendingAnnotations)) {
                    for(final Map.Entry<HardwareAddress, Set<String>> entry : pendingAnnotations.entrySet()) {
                        PhysicalGraph.this.segmentForHardwareAddress.get(entry.getKey()).addAnnotations(entry.getKey(), entry.getValue());
                    }
                    pendingAnnotations.clear();
                }
                if(hasFlag(pendingSwitches)) {
                    for(final Switch sWitch : pendingSwitches) {
                        //Get the segment for the first port of the first port group; everything in the switch should belong to the same segment and for any valid switch this will exist.
                        final Segment segment = PhysicalGraph.this.segmentForHardwareAddress.get(sWitch.getPortGroups().get(0).getPorts().keySet().iterator().next().getAddress());
                        segment.addSwitch(sWitch);
                    }
                    pendingSwitches.clear();
                }
            } catch(Exception ex) {
                Logger.log(Logger.Severity.PEDANTIC_DEVELOPER_SPAM, "Breakpoint triggered: %s", ex);
                if(Launcher.getConfiguration().isDeveloperModeProperty().get()) {
                    ex.printStackTrace();
                }
            }
        }
    }
    private final PhysicalGraphState state;

    //Events
    public final Event<SegmentEventArgs> onSegmentsModified;
    public final Event<RouterEventArgs> onRoutersModified;

    //Caching/lookup
    private final Map<HardwareAddress, Segment> segmentForHardwareAddress;

    protected PhysicalGraph(final Event.IAsyncExecutionProvider executionProvider) {
        this.segments = new LinkedList<>();
        this.routers = new LinkedList<>();
        this.segmentForHardwareAddress = new HashMap<>();

        this.onSegmentsModified = new Event<>(executionProvider);
        this.onRoutersModified = new Event<>(executionProvider);

        this.state = new PhysicalGraphState(executionProvider);
    }

    public void addDevice(final Switch sWitch) {
        state.invalidate(state.pendingSwitches, () -> state.pendingSwitches.add(sWitch));
    }
    public void addHardwareAddress(final HardwareAddress address) {
        state.invalidate(state.pendingAddresses, () -> state.pendingAddresses.add(address));
    }
    public void addHardwareAddressConnection(final Edge<HardwareAddress> edge) {
        state.invalidate(state.pendingEdges, () -> {
            if(!state.pendingAddresses.contains(edge.getSource()) && !segmentForHardwareAddress.containsKey(edge.getSource())) {
                return false;
            }
            if(!state.pendingAddresses.contains(edge.getDestination()) && !segmentForHardwareAddress.containsKey(edge.getDestination())) {
                return false;
            }

            return state.pendingEdges.add(edge);
        });
    }
    public void addLogicalAddressMapping(final LogicalAddressMapping mapping) {
        state.invalidate(state.pendingMappings, () -> {
            if(!state.pendingAddresses.contains(mapping.getHardwareAddress()) && !segmentForHardwareAddress.containsKey(mapping.getHardwareAddress())) {
                return false;
            }
            return state.pendingMappings.add(mapping);
        });
    }
    public void addAnnotation(final HardwareAddress address, final String annotation) {
        state.invalidate(state.pendingAnnotations, () -> {
            if(!state.pendingAddresses.contains(address) && !segmentForHardwareAddress.containsKey(address)) {
                return false;
            }

            if(state.pendingAnnotations.containsKey(address)) {
                boolean result = state.pendingAnnotations.get(address).add(annotation);
                result |= state.pendingAddresses.add(address);
                return result;
            } else {
                final Set<String> container = new HashSet<>();
                container.add(annotation);
                state.pendingAnnotations.put(address, container);
                state.pendingAddresses.add(address);
                return true;
            }
        });
    }

    @Override
    public String toString() {
        final StringBuilder result = new StringBuilder();

        result.append("[PHYSICAL GRAPH ");
        result.append(this.segments.stream().map(segment -> segment.toString()).collect(Collectors.joining(", ", "Segments:{", "}")));
        result.append(this.routers.stream().map(router -> router.toString()).collect(Collectors.joining(", ", "Routers:{", "}]")));

        return result.toString();
    }

    public List<Segment> getSegments() {
        return new ArrayList<>(this.segments);
    }
}
