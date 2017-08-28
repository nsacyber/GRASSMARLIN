package grassmarlin.session.logicaladdresses;

import grassmarlin.session.LogicalAddress;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

public class Ipv4WithPort extends LogicalAddress<Long> implements IHasPort {
    public static class Ipv4WithUdpPort extends Ipv4WithPort {
        public Ipv4WithUdpPort(final XMLStreamReader reader) {
            super(reader);
        }
        public Ipv4WithUdpPort(final Ipv4 ip, final int port) {
            super(ip, port);
        }
        public Ipv4WithUdpPort(final long address, final int port) {
            super(address, port);
        }
    }
    public static class Ipv4WithTcpPort extends Ipv4WithPort {
        public Ipv4WithTcpPort(final XMLStreamReader reader) {
            super(reader);
        }
        public Ipv4WithTcpPort(final Ipv4 ip, final int port) {
            super(ip, port);
        }
        public Ipv4WithTcpPort(final long address, final int port) {
            super(address, port);
        }
    }

    public Ipv4WithPort(final XMLStreamReader reader) {
        this(Long.parseLong(reader.getAttributeValue(null, "address"), 16), Integer.parseInt(reader.getAttributeValue(null, "port")));
    }
    public Ipv4WithPort(final Ipv4 ip, final int port) {
        this(ip.getRawAddress(), port);
    }
    public Ipv4WithPort(final long address, final int port) {
        super(address + ((long)port << 40));
    }

    public long getBaseAddress() {
        return getRawAddress() & 0x00000000FFFFFFFFL;
    }

    @Override
    public void writeToXml(final XMLStreamWriter target) throws XMLStreamException {
        target.writeAttribute("address", Long.toString(getBaseAddress(), 16));
        target.writeAttribute("port", Integer.toString(getPort()));
    }

    @Override
    public int getPort() {
        return (int)(getRawAddress() >>> 40) & 0xFFFF;
    }
    @Override
    public LogicalAddress<?> getAddressWithoutPort() {
        return new Ipv4(getBaseAddress());
    }

    @Override
    public String toString() {
        final long address = getRawAddress();
        return "" + (0xFF & (address >>> 24)) + "." + (0xFF & (address >>> 16)) + "." + (0xFF & (address >>> 8)) + "." + (0xFF & address) + ":" + getPort();
    }
}
