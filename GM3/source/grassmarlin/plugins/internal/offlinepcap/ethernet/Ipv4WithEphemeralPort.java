package grassmarlin.plugins.internal.offlinepcap.ethernet;

import grassmarlin.session.logicaladdresses.Ipv4WithPort;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

public class Ipv4WithEphemeralPort extends Ipv4WithPort {
    public Ipv4WithEphemeralPort(final Ipv4WithPort base) {
        super(base.getBaseAddress(), -1);
    }
    public Ipv4WithEphemeralPort(final XMLStreamReader reader) throws XMLStreamException {
        this(new Ipv4WithPort(Long.parseLong(reader.getAttributeValue(null, "address"), 16), Integer.parseInt(reader.getAttributeValue(null, "port"))));
    }

    @Override
    public int getPort() {
        return -1;
    }

    @Override
    public String toString() {
        final long address = getBaseAddress();
        return "" + (0xFF & (address >>> 24)) + "." + (0xFF & (address >>> 16)) + "." + (0xFF & (address >>> 8)) + "." + (0xFF & address) + ":Ephemeral";
    }
}
