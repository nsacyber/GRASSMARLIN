package grassmarlin.session.logicaladdresses;

import grassmarlin.session.LogicalAddress;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

public class Ipv4 extends LogicalAddress<Long> {
    public Ipv4(final XMLStreamReader reader) {
        this(Long.parseLong(reader.getAttributeValue(null, "address"), 16));
    }

    public Ipv4(final long address) {
        super(address);
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

    public int getNetmaskSize() {
        for(int idx = 0; idx < 32; idx++) {
            if(((1L << idx) & getRawAddress()) != 0) {
                return 32 - idx;
            }
        }
        return 0;
    }

    @Override
    public void writeToXml(final XMLStreamWriter target) throws XMLStreamException {
        target.writeAttribute("address", Long.toString(getRawAddress(), 16));
    }

    @Override
    public boolean contains(final LogicalAddress<?> child) {
        //An Ipv4 can contain an Ipv4WithPort in addition to the identity.
        if(child instanceof Ipv4WithPort) {
            return (this.getRawAddress() & 0x00000000FFFFFFFFL) == (((Ipv4WithPort)child).getRawAddress() & 0x00000000FFFFFFFFL);
        } else {
            return super.contains(child);
        }
    }

    @Override
    public String toString() {
        final long address = getRawAddress();
        return "" + (0xFF & (address >>> 24)) + "." + (0xFF & (address >>> 16)) + "." + (0xFF & (address >>> 8)) + "." + (0xFF & address);
    }
}
