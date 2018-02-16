package grassmarlin.session.logicaladdresses;

import grassmarlin.session.LogicalAddress;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

public class RouterLogicalAddress extends LogicalAddress {
    public RouterLogicalAddress() {
        super();
    }

    @Override
    public boolean contains(final LogicalAddress other) {
        return true;
    }

    @Override
    public String toString() {
        return "Router Address";
    }

    @Override
    public void readFromXml(XMLStreamReader source) throws XMLStreamException {
        return;
    }

    @Override
    public void writeToXml(XMLStreamWriter target) throws XMLStreamException {
        return;
    }
}
