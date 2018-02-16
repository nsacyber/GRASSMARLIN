package iadgov.siemens;

import grassmarlin.Logger;
import grassmarlin.common.Confidence;
import grassmarlin.plugins.internal.logicalview.GraphLogicalVertex;
import grassmarlin.session.LogicalVertex;
import grassmarlin.session.Property;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Set;

public class S7Comm implements AutoCloseable {
    public static boolean canBeQueried(final GraphLogicalVertex graphVertex, final LogicalVertex vertex) {
        if(vertex == null) {
            return (graphVertex.getRootLogicalAddressMapping().getLogicalAddress() instanceof Ipv4WithRackAndSlot) || graphVertex.getChildAddresses().stream().anyMatch(vertex1 -> vertex1.getLogicalAddress() instanceof Ipv4WithRackAndSlot);
        } else {
            return vertex.getLogicalAddress() instanceof Ipv4WithRackAndSlot;
        }
    }

    public static void queryVertex(final GraphLogicalVertex graphVertex, final LogicalVertex vertex) {
        final Ipv4WithRackAndSlot address;
        if(vertex == null || !(vertex.getLogicalAddress() instanceof Ipv4WithRackAndSlot)) {
            address = (Ipv4WithRackAndSlot) graphVertex.getChildAddresses().stream()
                    .filter(vertex1 -> vertex1.getLogicalAddress() instanceof Ipv4WithRackAndSlot)
                    .findAny()
                    .orElse(null)
                    .getLogicalAddress();
        } else {
            address = (Ipv4WithRackAndSlot)vertex.getLogicalAddress();
        }
        queryVertex(graphVertex, address);
    }
    public static void queryVertex(final GraphLogicalVertex vertex, final Ipv4WithRackAndSlot address) {
        try(final S7Comm s7 = new S7Comm(address)) {
            if (s7.establishConnection()) {
                vertex.getVertex().setProperties(Plugin.NAME, Plugin.PROPERTY_CAN_COMMUNICATE, new Property<>(true, Confidence.USER));

                if (s7.queryConfiguration()) {
                    //TODO: Configurable salt for the hash.
                    final S7Payload configuration = s7.getConfiguration();
                    final MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
                    final byte[] hash = sha256.digest(configuration.getBytes());

                    vertex.getVertex().setProperties(Plugin.NAME, Plugin.PROPERTY_FIRMWARE_HASH_CURRENT, new Property<>(hash, Confidence.USER));
                    final Set<Property<?>> propertiesBaseline = vertex.getVertex().getProperties().get(Plugin.PROPERTY_FIRMWARE_HASH_BASELINE);
                    if(propertiesBaseline == null || propertiesBaseline.isEmpty()) {
                        vertex.getVertex().setProperties(Plugin.NAME, Plugin.PROPERTY_FIRMWARE_HASH_BASELINE, new Property<>(hash, Confidence.USER));
                        vertex.getVertex().setProperties(Plugin.NAME, Plugin.PROPERTY_FIRMWARE_STATE, new Property<>(Plugin.STATE_BASELINE, Confidence.USER));
                    } else {
                        if(propertiesBaseline.stream().anyMatch(baseline -> (baseline.getValue() instanceof byte[]) && Arrays.equals((byte[])baseline.getValue(), hash))) {
                            vertex.getVertex().setProperties(Plugin.NAME, Plugin.PROPERTY_FIRMWARE_STATE, new Property<>(Plugin.STATE_VERIFIED, Confidence.USER));
                        } else {
                            vertex.getVertex().setProperties(Plugin.NAME, Plugin.PROPERTY_FIRMWARE_STATE, new Property<>(Plugin.STATE_CONFLICTED, Confidence.USER));
                        }
                    }
                } else {
                    Logger.log(Logger.Severity.ERROR, "The Firmware of (%s) could not be queried.", vertex.getRootLogicalAddressMapping().getLogicalAddress());
                    vertex.getVertex().setProperties(Plugin.NAME, Plugin.PROPERTY_FIRMWARE_STATE, new Property<>(Plugin.STATE_UNQUERYABLE, Confidence.USER));
                }
            } else {
                vertex.getVertex().setProperties(Plugin.NAME, Plugin.PROPERTY_CAN_COMMUNICATE, new Property<>(false, Confidence.USER));
            }
        } catch(Exception ex) {
            Logger.log(Logger.Severity.ERROR, "There was an error establishing a connection: %s", ex.getMessage());

            ex.printStackTrace();
        }
    }


    private InetAddress address;
    private Socket connection;
    private BufferedOutputStream conOut;
    private BufferedInputStream conIn;

    private static byte[] connectionPacket = new byte[] {0x03, 0x00, 0x00, 0x16, 0x11, (byte)0xE0, 0x00, 0x00, 0x00, 0x01, 0x00, (byte)0xC0, 0x01, 0x0A, (byte)0xC1, 0x02, 0x01, 0x00, (byte)0xC2, 0x02, 0x01, 0x02};

    public S7Comm(final Ipv4WithRackAndSlot ip) {
        try {
            this.address = Inet4Address.getByAddress(ip.getRawAddressBytes());
        } catch (UnknownHostException uhe) {
            throw new IllegalStateException(uhe);
        }
    }

    public boolean establishConnection() {
        try {
            this.connection = new Socket(this.address, 102);
            setConnectionParameters();

            if (this.connection.isConnected()) {
                try {
                    conOut = new BufferedOutputStream(this.connection.getOutputStream());
                    conIn = new BufferedInputStream(this.connection.getInputStream());
                    conOut.write(connectionPacket);
                    conOut.flush();

                    recieveS7Connection();

                } catch (IOException ioe) {
                    Logger.log(Logger.Severity.ERROR, "Unable to establish ISO Connection to PLC at %s: %s", this.address.toString(), ioe.getMessage());
                    return false;
                }
            }

            return true;
        } catch (IOException ioe) {
            Logger.log(Logger.Severity.ERROR, "Unable to connect to PLC at %s on port 102: %s", this.address.toString(), ioe.getMessage());
            return false;
        }
    }

    public boolean queryConfiguration() {
        return false;
    }

    public S7Payload getConfiguration() {
        return null;
    }

    @Override
    public void close() throws IOException {
       this.connection.close();
    }

    private void setConnectionParameters() {
        //TODO: set the TSAP values in the connection packet;
    }

    private void recieveS7Connection() throws IOException {
        //TODO: recieve packet from PLC and check for correct PDU type
    }
}
