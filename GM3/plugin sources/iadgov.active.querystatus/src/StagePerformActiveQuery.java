package iadgov.active.querystatus;

import grassmarlin.RuntimeConfiguration;
import grassmarlin.common.Confidence;
import grassmarlin.session.LogicalAddress;
import grassmarlin.session.Property;
import grassmarlin.session.Session;
import grassmarlin.session.logicaladdresses.Ipv4;
import grassmarlin.session.logicaladdresses.Ipv4WithPort;
import grassmarlin.session.pipeline.AbstractStage;
import grassmarlin.session.pipeline.IHasLogicalVertexProperties;
import grassmarlin.session.pipeline.LogicalAddressMapping;
import grassmarlin.ui.common.dialogs.preferences.PreferenceDialog;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.Serializable;
import java.net.*;
import java.util.*;

public class StagePerformActiveQuery extends AbstractStage<Session> {
    public final static String NAME = "Active Query";

    public class StatusChangeProperty extends HashMap<String, Collection<Property<?>>> implements IHasLogicalVertexProperties {
        private final LogicalAddressMapping mapping;

        public StatusChangeProperty(final LogicalAddressMapping mapping, final String status) {
            super(1);
            this.mapping = mapping;

            this.put(StagePerformActiveQuery.this.configuration.getNameStatusProperty(), Collections.singletonList(new Property<String>(status, StagePerformActiveQuery.this.configuration.getConfidence())));
        }

        @Override
        public String getPropertySource() {
            return NAME;
        }

        @Override
        public LogicalAddressMapping getAddressMapping() {
            return this.mapping;
        }

        @Override
        public Map<String, Collection<Property<?>>> getProperties() {
            return this;
        }
    }

    public static class Configuration implements Cloneable, Serializable {
        public enum Protocol {
            Tcp,
            Udp
            //Http, SSL?
        }

        @PreferenceDialog.Field(name="Protocol", accessorName="Protocol", nullable = false)
        private Protocol protocol;
        @PreferenceDialog.Field(name="Port", accessorName="Port", nullable = false)
        private Integer port;
        @PreferenceDialog.Field(name="Query", accessorName="PayloadQuery", nullable = false)
        private byte[] payloadQuery;
        @PreferenceDialog.Field(name="Compare(lhs, rhs)", accessorName="Script", nullable = false, rows=6)
        private String script;
        @PreferenceDialog.Field(name="Timeout (ms)", accessorName="Timeout", nullable = false)
        private Integer timeout;
        @PreferenceDialog.Field(name="Maximum Response Length (bytes)", accessorName="MaxResponseLength", nullable = false)
        private Integer maxResponseLength;
        @PreferenceDialog.Field(name="Property Name", accessorName="NameStatusProperty", nullable = false)
        private String nameStatusProperty;
        @PreferenceDialog.Field(name="Property Confidence", accessorName="Confidence", nullable = false)
        private Confidence confidence;

        public Configuration(final Protocol protocol, final int port, final byte[] query, final String script, final int timeout, final int maxResponseLength, final String nameStatusProperty, final Confidence confidence) {
            this.protocol = protocol;
            this.port = port;
            this.payloadQuery = query;
            this.script = script;
            this.timeout = timeout;
            this.maxResponseLength = maxResponseLength;
            this.nameStatusProperty = nameStatusProperty;
            this.confidence = confidence;
        }

        @Override
        public Object clone() throws CloneNotSupportedException {
            return super.clone();
        }

        public Protocol getProtocol() {
            return this.protocol;
        }

        public void setProtocol(Protocol protocol) {
            this.protocol = protocol;
        }

        public Integer getPort() {
            return port;
        }

        public void setPort(Integer port) {
            this.port = port;
        }

        public byte[] getPayloadQuery() {
            return payloadQuery;
        }

        public void setPayloadQuery(byte[] payloadQuery) {
            this.payloadQuery = payloadQuery;
        }

        public String getScript() {
            return script;
        }

        public void setScript(String script) {
            this.script = script;
        }

        public Integer getTimeout() {
            return timeout;
        }

        public void setTimeout(Integer timeout) {
            this.timeout = timeout;
        }

        public Integer getMaxResponseLength() {
            return maxResponseLength;
        }

        public void setMaxResponseLength(Integer maxResponseLength) {
            this.maxResponseLength = maxResponseLength;
        }

        public String getNameStatusProperty() {
            return nameStatusProperty;
        }

        public void setNameStatusProperty(String nameStatusProperty) {
            this.nameStatusProperty = nameStatusProperty;
        }

        public Confidence getConfidence() {
            return confidence;
        }

        public void setConfidence(Confidence confidence) {
            this.confidence = confidence;
        }
    }

    private Configuration configuration = null;
    private CompareScript comparison = null;


    public StagePerformActiveQuery(final RuntimeConfiguration config, final Session session) {
        super(config, session, LogicalAddressMapping.class);

        this.setPassiveMode(true);
    }

    @Override
    public void setConfiguration(final Serializable config) {
        if(config instanceof Configuration) {
            this.configuration = (Configuration)config;
            this.comparison = new CompareScript(this.configuration.getScript());
        }
    }

    @Override
    public Object process(final Object o) {
        //If we don't have a comparison we can just stop now.
        if(this.comparison == null) {
            return null;
        }
        if(o instanceof LogicalAddressMapping) {
            LogicalAddress address = ((LogicalAddressMapping) o).getLogicalAddress();

            if(address instanceof Ipv4) {
                try {
                    SocketAddress target = new InetSocketAddress(Inet4Address.getByAddress(((Ipv4) address).getRawAddressBytes()), configuration.getPort());
                    return processResponse((LogicalAddressMapping)o, performQuery(target));
                } catch(IOException ex) {
                    return null;
                }
            } else if(address instanceof Ipv4WithPort) {
                try {
                    SocketAddress target = new InetSocketAddress(Inet4Address.getByAddress(((Ipv4WithPort)address).getAddressWithoutPort().getRawAddressBytes()), configuration.getPort());
                    return processResponse((LogicalAddressMapping)o, performQuery(target));
                } catch(IOException ex) {
                    return null;
                }
            } else {
                //TODO: Add Ipv6 support
                return null;
            }
        } else {
            return null;
        }
    }

    @Override
    public String getName() {
        return NAME;
    }

    private byte[] performQueryTcp(final SocketAddress address) throws IOException {
        final Socket sock = new Socket();

        sock.connect(address, configuration.getTimeout());

        if(sock.isConnected()) {
            sock.getOutputStream().write(configuration.getPayloadQuery());

            BufferedInputStream input = new BufferedInputStream(sock.getInputStream());
            final byte[] responseBuffer = new byte[configuration.getMaxResponseLength()];
            int cbResult = 0;
            int cbRead = 0;
            while((cbRead = input.read(responseBuffer, cbResult, configuration.getMaxResponseLength() - cbRead)) != -1) {
                cbResult += cbRead;
                if(cbResult == configuration.getMaxResponseLength()) {
                    break;
                }
            }

            final byte[] response;
            if(cbRead != configuration.getMaxResponseLength()) {
                response = Arrays.copyOf(responseBuffer, cbRead);
            } else {
                response = responseBuffer;
            }

            return response;
        } else {
            return null;
        }
    }

    private byte[] performQueryUdp(final SocketAddress address) throws IOException {
        final DatagramSocket sock = new DatagramSocket();

        sock.send(new DatagramPacket(configuration.getPayloadQuery(), configuration.getPayloadQuery().length, address));
        final DatagramPacket response = new DatagramPacket(new byte[configuration.getMaxResponseLength()], configuration.getMaxResponseLength());
        sock.receive(response);

        if(response.getLength() != configuration.getMaxResponseLength()) {
            return Arrays.copyOf(response.getData(), response.getLength());
        } else {
            return response.getData();
        }
    }

    protected byte[] performQuery(final SocketAddress address) throws IOException {
        switch (configuration.getProtocol()) {
            case Udp:
                return performQueryUdp(address);
            case Tcp:
                return performQueryTcp(address);
            default:
                return null;
        }
    }

    protected Object processResponse(final LogicalAddressMapping mapping, final byte[] response) {
        if(response == null) {
            //TODO: Error?
        }
        if(this.responseCache.containsKey(mapping)) {
            //TODO: This should set the property directly--returning a property would be additive, not replace.
            switch(comparison.compare(responseCache.get(mapping), response)) {
                case Error:
                    return new StatusChangeProperty(mapping, "Error");
                case Identical:
                    return new StatusChangeProperty(mapping, "Stable");
                case UpdateChanged:
                    this.responseCache.put(mapping, response);
                    return new StatusChangeProperty(mapping, "Changed");
                case UpdateUnchanged:
                    this.responseCache.put(mapping, response);
                    return new StatusChangeProperty(mapping, "Stable");
                default:
                    return null;
            }
        } else {
            this.responseCache.put(mapping, response);
            //TODO: This should maybe output the address as added to the cache?
            return null;
        }
    }

    private Map<LogicalAddressMapping, byte[]> responseCache = new HashMap<>();
}
