package grassmarlin.plugins.internal.physical.deviceimport;

import grassmarlin.common.Confidence;
import grassmarlin.plugins.internal.physical.view.IPhysicalViewApi;
import grassmarlin.session.HardwareAddress;
import grassmarlin.session.Property;
import grassmarlin.session.Session;
import grassmarlin.session.hardwareaddresses.Device;
import grassmarlin.session.pipeline.IHasHardwareVertexProperties;
import grassmarlin.session.pipeline.IHasPhysicalConnectionProperties;
import javafx.geometry.Point2D;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.util.*;
import java.util.stream.Collectors;

public class Switch {
    public class Port {
        private HardwareAddress address;
        private final String name;
        private Point2D position;
        private double degreesControl;
        private boolean isTrunk;
        private Collection<HardwareAddress> connectedTo;
        private final Map<String, Collection<Property<?>>> properties;

        public Port(final String name) {
            this.name = name;
            this.address = null;
            this.position = new Point2D(0.0, 0.0);
            this.degreesControl = Double.NaN;
            this.isTrunk = false;
            this.connectedTo = new LinkedHashSet<>();
            this.properties = new HashMap<>();
        }

        public void setAddress(final HardwareAddress address) {
            this.address = address;
        }

        public void setPosition(final double x, final double y) {
            this.position = new Point2D(x, y);
        }
        public Point2D getPosition() {
            return this.position;
        }

        public void setControlAngle(final double degrees) {
            this.degreesControl = degrees;
        }

        public void setTrunk(final boolean trunk) {
            this.isTrunk = trunk;
        }

        public void addConnection(final HardwareAddress address) {
            this.connectedTo.add(address);
        }

        public void addProperty(final String property, final Serializable value) {
            this.addProperty(property, value, CONFIDENCE);
        }

        public void addProperty(final String property, final Serializable value, final Confidence confidence) {
            this.properties.computeIfAbsent(property, p -> new HashSet<>()).add(new Property<>(value, confidence));
        }

        public boolean isValid() {
            return this.address != null && this.name != null && this.position != null;
        }

        public Collection<Object> getEntities() {
            final List<Object> result = new ArrayList<>(this.connectedTo.size() * 2 + 2);
            result.add(this.address);
            result.addAll(this.connectedTo);
            result.addAll(this.connectedTo.stream().map(endpoint -> new Session.HardwareAddressPair(this.address, endpoint)).collect(Collectors.toList()));
            final Map<String, Collection<Property<?>>> propertiesVertex = new HashMap<>(this.properties);
            propertiesVertex.put("Name", Collections.singletonList(new Property<>(this.name, Switch.CONFIDENCE)));
            //TODO: Trunk flag should be on the connection rather than the vertex.  Not sure how much of the downstream processing is ready for that, though.
            propertiesVertex.put("Trunk", Collections.singletonList(new Property<>(this.isTrunk, Switch.CONFIDENCE)));

            result.add(new IHasHardwareVertexProperties() {
                @Override
                public String getPropertySource() {
                    return Plugin.NAME;
                }

                @Override
                public HardwareAddress getHardwareAddress() {
                    return Port.this.address;
                }

                @Override
                public Map<String, Collection<Property<?>>> getProperties() {
                    return propertiesVertex;
                }
            });



            return result;
        }
    }

    public static Confidence CONFIDENCE = Confidence.MEDIUM_HIGH;

    private final Collection<Port> ports;
    private String name;
    private final Map<String, Collection<Property<?>>> properties;

    public Switch() {
        this.ports = new LinkedHashSet<>();
        this.name = null;
        this.properties = new HashMap<>();
    }

    public boolean isValid() {
        //Has 2+ valid ports and a name
        return this.name != null && this.ports.stream().filter(port -> port.isValid()).count() > 1;
    }

    public void setName(final String name) {
        this.name = name;
        this.properties.put("Name", Collections.singletonList(new Property<>(name, Confidence.MEDIUM_HIGH)));
    }

    public void addPort(final Port port) {
        this.ports.add(port);
    }

    public void addProperty(final String property, final Serializable value) {
        this.addProperty(property, value, CONFIDENCE);
    }

    public void addProperty(final String property, final Serializable value, final Confidence confidence) {
        this.properties.computeIfAbsent(property, k -> new HashSet<>()).add(new Property<>(value, confidence));
    }

    public Collection<Object> getEntities() {
        final List<Object> result = new ArrayList<>(this.ports.size() * 2 + 2);
        final Device deviceSelf;
        try {
             deviceSelf = new Device(this.name);
        } catch(UnsupportedEncodingException ex) {
            return null;
        }

        result.add(deviceSelf);
        if(!properties.isEmpty()) {
            result.add(new IHasHardwareVertexProperties() {
                @Override
                public String getPropertySource() {
                    return Plugin.NAME;
                }

                @Override
                public HardwareAddress getHardwareAddress() {
                    return deviceSelf;
                }

                @Override
                public Map<String, Collection<Property<?>>> getProperties() {
                    return Switch.this.properties;
                }
            });
        }
        result.addAll(this.ports.stream().flatMap(port -> port.getEntities().stream()).collect(Collectors.toList()));
        for(final Port port : this.ports) {
            final Session.HardwareAddressPair edge = new Session.HardwareAddressPair(deviceSelf, port.address);
            result.add(edge);

            //The edge from the switch to the port contains properties that are used to depict the visual layout.
            final Map<String, Collection<Property<?>>> propertiesPort = new HashMap<>();
            propertiesPort.put(IPhysicalViewApi.PROPERTY_PORT_POSITION_X, Collections.singletonList(new Property<>(port.position.getX(), Confidence.MEDIUM_HIGH)));
            propertiesPort.put(IPhysicalViewApi.PROPERTY_PORT_POSITION_Y, Collections.singletonList(new Property<>(port.position.getY(), Confidence.MEDIUM_HIGH)));
            if(!Double.isNaN(port.degreesControl)) {
                propertiesPort.put(IPhysicalViewApi.PROPERTY_PORT_CONTROL_ANGLE, Collections.singletonList(new Property<>(port.degreesControl, Confidence.MEDIUM_HIGH)));
            }
            result.add(new IHasPhysicalConnectionProperties() {
                @Override
                public String getPropertySource() {
                    return Plugin.NAME;
                }

                @Override
                public Session.HardwareAddressPair getEndpoints() {
                    return edge;
                }

                @Override
                public Map<String, Collection<Property<?>>> getProperties() {
                    return propertiesPort;
                }
            });
        }
        return result;
    }
}
