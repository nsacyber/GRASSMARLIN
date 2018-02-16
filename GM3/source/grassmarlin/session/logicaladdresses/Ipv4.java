package grassmarlin.session.logicaladdresses;

import javax.xml.stream.XMLStreamReader;

public class Ipv4 extends Cidr {
    public Ipv4(final XMLStreamReader reader) {
        super(reader);
    }

    public Ipv4(final long address) {
        super(address, 32);
    }

    public static Ipv4 fromString(final String text) {
        long value = 0;
        for(String token : text.split("\\.", 4)) {
            long valueToken = Long.parseLong(token);
            value <<= 8;
            value += valueToken;
        }

        return new Ipv4(value);
    }

    public byte[] getRawAddressBytes() {
        final long raw = this.getBaseAddress();
        return new byte[] {
                (byte)((raw >> 24) & 0xFF),
                (byte)((raw >> 16) & 0xFF),
                (byte)((raw >>  8) & 0xFF),
                (byte)((raw      ) & 0xFF)
        };
    }

    @Override
    public String toString() {
        final long address = this.getBaseAddress();
        return "" + (0xFF & (address >>> 24)) + "." + (0xFF & (address >>> 16)) + "." + (0xFF & (address >>> 8)) + "." + (0xFF & address);
    }
}
