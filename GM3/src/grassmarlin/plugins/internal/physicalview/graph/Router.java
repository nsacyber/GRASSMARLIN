package grassmarlin.plugins.internal.physicalview.graph;

import grassmarlin.session.HardwareAddress;
import grassmarlin.session.pipeline.LogicalAddressMapping;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Router {
    protected static class Port {
        private final String name;
        private final LogicalAddressMapping mapping;
        private final List<HardwareAddress> addresses;

        public Port(final String name, final LogicalAddressMapping mapping) {
            this.name = name;
            this.mapping = mapping;
            this.addresses = new ArrayList<>();
        }

        public String getName() {
            return this.name;
        }
        public LogicalAddressMapping getMapping() {
            return this.mapping;
        }
        public List<HardwareAddress> getAddresses() {
            return this.addresses;
        }
    }

    private final Map<Port, Segment> ports;

    public Router() {
        this.ports = new HashMap<>();
    }

    public boolean connectSegment(final String name, final LogicalAddressMapping gateway, final Segment segment) {
        if(ports.containsKey(gateway)) {
            if(ports.get(gateway).equals(segment)) {
                return true;
            } else {
                return false;
            }
        } else {
            ports.put(new Port(name, gateway), segment);
            return true;
        }
    }
    public boolean connectSegment(final LogicalAddressMapping gateway, final Segment segment) {
        return this.connectSegment("Unnamed Port", gateway, segment);
    }
}
