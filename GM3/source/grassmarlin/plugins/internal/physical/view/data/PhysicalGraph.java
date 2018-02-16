package grassmarlin.plugins.internal.physical.view.data;

import com.sun.javafx.collections.ObservableListWrapper;
import grassmarlin.Event;
import grassmarlin.RuntimeConfiguration;
import grassmarlin.session.ThreadManagedState;
import javafx.collections.ObservableList;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public abstract class PhysicalGraph {
    //TODO: Rather than dealing with the Physical* elements, we may need to deal with a different set of objects--this has to be evaluated once the rest of the work has been tested
    private final class PhysicalGraphThreadManagedState extends ThreadManagedState {
        private final List<PhysicalEndpoint> pendingEndpoints;
        private final List<PhysicalDevice> pendingDevices;
        private final List<PhysicalWire> pendingWires;

        private final List<PhysicalEndpoint> pendingEndpointRemovals;
        private final List<PhysicalDevice> pendingDeviceRemovals;
        private final List<PhysicalWire> pendingWireRemovals;

        public PhysicalGraphThreadManagedState(final Event.IAsyncExecutionProvider uiProvider) {
            super(RuntimeConfiguration.UPDATE_INTERVAL_MS, "Physical Graph", uiProvider);

            this.pendingEndpoints = new LinkedList<>();
            this.pendingEndpointRemovals = new LinkedList<>();
            this.pendingDevices = new LinkedList<>();
            this.pendingDeviceRemovals = new LinkedList<>();
            this.pendingWires = new LinkedList<>();
            this.pendingWireRemovals = new LinkedList<>();
        }

        @Override
        public void validate() {
            //HACK: Sombra was here
            if(this.hasFlag(this.pendingEndpoints) || this.hasFlag(this.pendingEndpointRemovals)) {
                this.pendingEndpoints.removeAll(PhysicalGraph.this.endpoints);
                PhysicalGraph.this.endpoints.addAll(this.pendingEndpoints);
                PhysicalGraph.this.endpoints.removeAll(this.pendingEndpointRemovals);
                this.pendingEndpoints.clear();
                this.pendingEndpointRemovals.clear();
            }
            if(this.hasFlag(this.pendingDevices) || this.hasFlag(this.pendingDeviceRemovals)) {
                this.pendingDevices.removeAll(PhysicalGraph.this.devices);
                PhysicalGraph.this.devices.addAll(this.pendingDevices);
                PhysicalGraph.this.devices.removeAll(this.pendingDeviceRemovals);
                this.pendingDevices.clear();
                this.pendingDeviceRemovals.clear();
            }
            if(this.hasFlag(this.pendingWires) || this.hasFlag(this.pendingWireRemovals)) {
                this.pendingWires.removeAll(PhysicalGraph.this.wires);
                PhysicalGraph.this.wires.addAll(this.pendingWires);
                PhysicalGraph.this.wires.removeAll(this.pendingWireRemovals);
                this.pendingWires.clear();
                this.pendingWireRemovals.clear();
            }
        }
    }

    private final ObservableList<PhysicalEndpoint> endpoints;
    private final ObservableList<PhysicalDevice> devices;
    private final ObservableList<PhysicalWire> wires;

    private final PhysicalGraphThreadManagedState state;

    protected PhysicalGraph(final Event.IAsyncExecutionProvider uiProvider) {
        this.state = new PhysicalGraphThreadManagedState(uiProvider);

        this.endpoints = new ObservableListWrapper<>(new ArrayList<>());
        this.devices = new ObservableListWrapper<>(new ArrayList<>());
        this.wires = new ObservableListWrapper<>(new ArrayList<>());
    }

    public void waitForValidState() {
        this.state.waitForValid();
    }

    protected void addPhysicalEndpoint(final PhysicalEndpoint endpoint) {
        this.state.invalidate(this.state.pendingEndpoints, () -> {
            boolean result = false;
            result |= PhysicalGraph.this.state.pendingEndpoints.add(endpoint);
            result |= PhysicalGraph.this.state.pendingEndpointRemovals.remove(endpoint);
            return result;
        });
    }
    protected void removePhysicalEndpoint(final PhysicalEndpoint endpoint) {
        this.state.invalidate(this.state.pendingEndpointRemovals, () -> {
            boolean result = false;
            result |= PhysicalGraph.this.state.pendingEndpointRemovals.add(endpoint);
            result |= PhysicalGraph.this.state.pendingEndpoints.remove(endpoint);
            return result;
        });
    }

    protected void addPhysicalDevice(final PhysicalDevice device) {
        this.state.invalidate(this.state.pendingDevices, () -> {
            boolean result = PhysicalGraph.this.state.pendingDevices.add(device);
            result |= PhysicalGraph.this.state.pendingDeviceRemovals.remove(device);
            return result;
        });
    }
    protected void removePhysicalDevice(final PhysicalDevice device) {
        this.state.invalidate(this.state.pendingDeviceRemovals, () -> {
            boolean result = PhysicalGraph.this.state.pendingDeviceRemovals.add(device);
            result |= PhysicalGraph.this.state.pendingDevices.remove(device);
            return result;
        });
    }

    protected void addPhysicalWire(final PhysicalWire wire) {
        this.state.invalidate(this.state.pendingWires, () -> {
            boolean result = PhysicalGraph.this.state.pendingWires.add(wire);
            result |= PhysicalGraph.this.state.pendingWireRemovals.remove(wire);
            return result;
        });
    }
    protected void removePhysicalWire(final PhysicalWire wire) {
        this.state.invalidate(this.state.pendingWireRemovals, () -> {
            boolean result = PhysicalGraph.this.state.pendingWireRemovals.add(wire);
            result |= PhysicalGraph.this.state.pendingWires.remove(wire);
            return result;
        });
    }

    // == Accessors

    public ObservableList<PhysicalEndpoint> getEndpoints() {
        return this.endpoints;
    }
    public ObservableList<PhysicalDevice> getDevices() {
        return this.devices;
    }
    public ObservableList<PhysicalWire> getWires() {
        return this.wires;
    }
}
