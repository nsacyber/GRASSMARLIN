package grassmarlin.plugins.internal.physicalview.graph;

import grassmarlin.session.HardwareAddress;
import grassmarlin.session.PropertyContainer;

import java.util.*;

public class Switch extends PropertyContainer {
    public interface IPortGroupMapper {
        PortVisualSettings settingsFor(final int index);
    }

    public static class Port {
        private final HardwareAddress address;
        private final String name;
        private final List<HardwareAddress> connectedAddresses;
        private boolean enabled = false;
        private boolean errors = false;
        private boolean warnings = false;
        private boolean trunk = false;

        public Port(final HardwareAddress address, final String name) {
            this.address = address;
            this.name = name;
            this.connectedAddresses = new ArrayList<>();
        }

        public String getName() {
            return this.name;
        }

        public HardwareAddress getAddress() {
            return this.address;
        }

        public void setEnabled(final boolean enabled) {
            this.enabled = enabled;
        }
        public boolean isEnabled() {
            return this.enabled;
        }

        public void setErrors(final boolean errors) {
            this.errors = errors;
        }
        public boolean isErrors() {
            return this.errors;
        }

        public void setWarnings(final boolean warnings) {
            this.warnings = warnings;
        }
        public boolean isWarnings() {
            return this.warnings;
        }

        public void setTrunk(final boolean trunk) {
            this.trunk = trunk;
        }
        public boolean isTrunk() {
            return this.trunk;
        }

        public void addConnectedAddresses(final HardwareAddress... addresses) {
            this.addConnectedAddresses(Arrays.asList(addresses));
        }
        public void addConnectedAddresses(final Collection<HardwareAddress> addresses) {
            this.connectedAddresses.addAll(addresses);
        }
        public List<HardwareAddress> getConnectedAddresses() {
            return this.connectedAddresses;
        }
    }

    public static class PortVisualSettings {
        private final int x;
        private final int y;
        private final boolean inverted;

        public PortVisualSettings(final int x, final int y, final boolean inverted) {
            this.x = x;
            this.y = y;
            this.inverted = inverted;
        }

        public int getX() {
            return this.x;
        }
        public int getY() {
            return this.y;
        }
        public boolean isInverted() {
            return this.inverted;
        }
    }

    public static class PortGroup {
        private final Map<Port, PortVisualSettings> ports;
        private final String name;
        private IPortGroupMapper mapper = null;

        public PortGroup(final String name) {
            this.name = name;
            this.ports = new LinkedHashMap<>();
        }

        public String getName() {
            return this.name;
        }
        public Map<Port, PortVisualSettings> getPorts() {
            return this.ports;
        }
        public void setMapper(final IPortGroupMapper mapper) {
            this.mapper = mapper;
        }

        public void addPort(final Port port, final int index) {
            if(this.mapper == null) {
                ports.put(port, null);
            } else {
                ports.put(port, this.mapper.settingsFor(index));
            }
        }
    }

    private final List<PortGroup> groups;
    private final String name;

    public Switch(final String name) {
        this.groups = new ArrayList<>();
        this.name = name;
    }

    public void addGroup(final PortGroup group) {
        this.groups.add(group);
    }

    public List<PortGroup> getPortGroups() {
        return this.groups;
    }
}
