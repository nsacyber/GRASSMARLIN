package grassmarlin.session.hardwareaddresses;

import grassmarlin.session.HardwareAddress;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;
import java.util.Arrays;

public class Mac extends HardwareAddress {
    public Mac(final String text) {
        super(Arrays.stream(text.split(":")).mapToInt(token -> Integer.parseInt(token, 16)).toArray());
    }
    public Mac(final int[] mac) {
        super(new int[6]);
        System.arraycopy(mac, 0, this.getAddress(), 0, 6);
    }

    public Mac(final byte[] mac) {
        super(mac);
    }

    public Mac(final XMLStreamReader reader) throws XMLStreamException {
        this(reader.getElementText());
    }

    @Override
    public void writeToXml(final XMLStreamWriter target) throws XMLStreamException {
        target.writeCharacters(this.toString());
    }

    @Override
    public String toString() {
        return HardwareAddress.formatAddress(getAddress(), ":", "", "");
    }
}
