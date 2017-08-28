package grassmarlin.session.logicaladdresses;

import grassmarlin.session.LogicalAddress;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

public class Cidr extends LogicalAddress<Long> {
    private final long mask;

    public Cidr(final XMLStreamReader reader) {
        this(Long.parseLong(reader.getAttributeValue(null, "address"), 16), Integer.parseInt(reader.getAttributeValue(null, "bits")));
    }

    public Cidr(final long address, final int bits) {
        super(address & calculateMask(bits) | ((long)bits << 32));

        this.mask = calculateMask(bits);
    }

    public static Cidr fromString(final String text) {
        final String[] tokens = text.split("/");
        if(tokens.length != 2) {
            return null;
        }

        final int bits = Integer.parseInt(tokens[1]);
        long address = 0;
        for(String token : tokens[0].split("\\.")) {
            address <<= 8;
            address += Long.parseLong(token);
        }

        return new Cidr(address, bits);
    }

    private static long calculateMask(final int bits) {
        return (0x00000000FFFFFFFFL << (32 - (bits & 0x1F))) & 0x00000000FFFFFFFFL;
    }

    public long getBaseAddress() {
        return getRawAddress() & 0x00000000FFFFFFFFL;
    }
    public int getBits() {
        return (int)((getRawAddress() & 0x000000FF00000000L) >> 32);
    }

    @Override
    public void writeToXml(final XMLStreamWriter target) throws XMLStreamException {
        target.writeAttribute("address", Long.toString(getBaseAddress(), 16));
        target.writeAttribute("bits", Integer.toString(getBits()));
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean contains(final LogicalAddress<?> child) {
        if(child instanceof Ipv4WithPort || child instanceof Ipv4) {
            return (this.getRawAddress() & this.mask) == (((LogicalAddress<Long>)child).getRawAddress() & this.mask);
        } else if(child instanceof Cidr) {
            return (this.getRawAddress() & this.mask) == (((Cidr)child).getRawAddress() & this.mask) && (this.mask <= ((Cidr)child).mask);
        } else {
            return false;
        }
    }

    @Override
    public String toString() {
        final long address = getRawAddress();
        return "" + (0xFF & (address >>> 24)) + "." + (0xFF & (address >>> 16)) + "." + (0xFF & (address >>> 8)) + "." + (0xFF & address) + "/" + (0x1F & (address >>> 32));
    }
}
