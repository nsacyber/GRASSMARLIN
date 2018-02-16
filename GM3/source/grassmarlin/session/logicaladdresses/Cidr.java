package grassmarlin.session.logicaladdresses;

import grassmarlin.session.LogicalAddress;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

public class Cidr extends LogicalAddress {
    private final long ipBase;
    private final long mask;

    public Cidr(final XMLStreamReader reader) {
        this(Long.parseLong(reader.getAttributeValue(null, "address"), 16), Integer.parseInt(reader.getAttributeValue(null, "bits")));
    }

    public Cidr(final long address, final int bits) {
        super();

        this.mask = calculateMask(bits);
        this.ipBase = address & this.mask;
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
        if(bits > 31 || bits < 0) {
            return 0x00000000FFFFFFFFL;
        } else {
            return (0x00000000FFFFFFFFL << (32 - bits)) & 0x00000000FFFFFFFFL;
        }
    }

    public long getBaseAddress() {
        return this.ipBase;
    }
    public long getMask() {
        return this.mask;
    }
    public int getMaskSize() {
        for(int i = 0; i < 32; i++) {
            if((this.mask & (1 << i)) != 0) {
                return 32 - i;
            }
        }
        return 0;
    }

    @Override
    public boolean contains(final LogicalAddress other) {
        if(super.contains(other)) {
            final Cidr rhs = (Cidr)other;
            if(rhs.mask >= this.mask) {
                //The other mask has at least as many bits of precision, so it might be contained within this.
                return (((Cidr)other).ipBase & this.mask) == this.ipBase;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }

    @Override
    public int hashCode() {
        return (int)this.ipBase;
    }

    @Override
    public void writeToXml(final XMLStreamWriter target) throws XMLStreamException {
        target.writeAttribute("address", Long.toString(this.getBaseAddress(), 16));
        target.writeAttribute("bits", Integer.toString(this.getMaskSize()));
    }

    @Override
    public String toString() {
        return "" + (0xFF & (this.ipBase >>> 24)) + "." + (0xFF & (this.ipBase >>> 16)) + "." + (0xFF & (this.ipBase >>> 8)) + "." + (0xFF & this.ipBase) + "/" + this.getMaskSize();
    }
}
