package grassmarlin.session.hardwareaddresses;

import grassmarlin.session.HardwareAddress;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * The natural ordering of the Unique HardwareAddress is inconsistent with .equals()
 *
 * Two different Unique values will have a compareTo of 0, but will not be .equal.
 */
public class Unique extends HardwareAddress {
    private static final AtomicInteger idNext = new AtomicInteger(0);

    public Unique() {
        super(new int[] {idNext.getAndIncrement()});
    }
    public Unique(final XMLStreamReader reader) {
        super(new int[] {idNext.getAndIncrement()});
    }

    @Override
    public boolean equals(final Object other) {
        //We need reference equality to be enforced, but otherwise this will never match anything.
        return this == other;
    }

    @Override
    public void writeToXml(XMLStreamWriter target) throws XMLStreamException {
        //There is nothing to write
    }

    @Override
    public String toString() {
        return String.format("(Unique:%04d)", super.getAddress()[0]);
    }
}
