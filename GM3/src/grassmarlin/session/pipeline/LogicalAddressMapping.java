package grassmarlin.session.pipeline;

import grassmarlin.session.HardwareAddress;
import grassmarlin.session.IAddress;
import grassmarlin.session.LogicalAddress;
import grassmarlin.session.serialization.Loader;
import grassmarlin.session.serialization.Writer;
import grassmarlin.session.serialization.XmlSerializable;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.stream.events.XMLEvent;

public final class LogicalAddressMapping implements IAddress, Comparable<LogicalAddressMapping>, XmlSerializable {
    private final HardwareAddress hardware;
    private final LogicalAddress<?> logical;

    public LogicalAddressMapping(final HardwareAddress hardware, final LogicalAddress<?> logical) {
        this.hardware = hardware;
        this.logical = logical;
    }
    public LogicalAddressMapping(final XMLStreamReader reader) throws XMLStreamException {
        HardwareAddress hw = null;
        LogicalAddress<?> la = null;
        final String terminalTag = reader.getLocalName();

        while(reader.hasNext()) {
            final int typeNext = reader.nextTag();
            final String tag;
            switch(typeNext) {
                case XMLEvent.START_ELEMENT:
                    tag = reader.getLocalName();
                    if(tag.equals("HardwareAddress")) {
                        hw = (HardwareAddress) Loader.readObject(reader);
                    } else if(tag.equals("LogicalAddress")) {
                        la = (LogicalAddress<?>)Loader.readObject(reader);
                    }
                    break;
                case XMLEvent.END_ELEMENT:
                    tag = reader.getLocalName();
                    if(tag.equals(terminalTag)) {
                        if(hw == null || la == null) {
                            throw new IllegalStateException("Incomplete LogicalAddressMapping");
                        }
                        this.hardware = hw;
                        this.logical = la;
                        return;
                    }
            }
        }

        throw new IllegalStateException("Invalid LogicalAddressMapping");
    }

    public HardwareAddress getHardwareAddress() {
        return this.hardware;
    }
    public LogicalAddress<?> getLogicalAddress() {
        return this.logical;
    }

    public boolean contains(final LogicalAddressMapping other) {
        return this.hardware.equals(other.hardware) && this.logical.contains(other.logical);
    }

    @Override
    public int compareTo(final LogicalAddressMapping other) {
        final int logicalResult = this.logical.compareTo(other.logical);
        if(logicalResult == 0) {
            return this.hardware.compareTo(other.hardware);
        } else {
            return logicalResult;
        }
    }

    @Override
    public int hashCode() {
        return hardware.hashCode() ^ logical.hashCode();
    }

    @Override
    public boolean equals(Object other) {
        if(other instanceof LogicalAddressMapping) {
            final LogicalAddressMapping o = (LogicalAddressMapping)other;
            return this.hardware.equals(o.hardware) && this.logical.equals(o.logical);
        } else {
            return false;
        }
    }

    @Override
    public String toString() {
        return String.format("%s(%s)", hardware, logical);
    }

    @Override
    public void writeToXml(final XMLStreamWriter writer) throws XMLStreamException {
        writer.writeStartElement("HardwareAddress");
        Writer.writeObject(writer, this.hardware);
        writer.writeEndElement();

        writer.writeStartElement("LogicalAddress");
        Writer.writeObject(writer, this.logical);
        writer.writeEndElement();
    }
}
