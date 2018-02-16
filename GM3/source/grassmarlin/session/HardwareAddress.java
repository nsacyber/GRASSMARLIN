package grassmarlin.session;

import grassmarlin.session.serialization.XmlSerializable;

import java.io.Serializable;
import java.util.Arrays;
import java.util.stream.Collectors;

public abstract class HardwareAddress implements Comparable<HardwareAddress>, XmlSerializable, Serializable {
    private final int[] address;

    protected HardwareAddress(final int[] address) {
        this.address = address;
    }
    protected HardwareAddress(final byte[] address) {
        this.address = new int[address.length];
        for(int idx = address.length - 1; idx >= 0; idx--) {
            //We can't just cast since that would result in a negative number.
            this.address[idx] = address[idx] & 0xFF;
        }
    }

    public int[] getAddress() {
        return this.address;
    }

    @Override
    public int compareTo(final HardwareAddress other) {
        if(this == other) {
            return 0;
        }
        final int[] addressOther = other.address;
        if(address.length != addressOther.length) {
            return address.length < addressOther.length ? -1 : 1;
        }
        for(int idx = 0; idx < address.length; idx++) {
            if(address[idx] != addressOther[idx]) {
                return address[idx] < addressOther[idx] ? -1 : 1;
            }
        }
        return 0;
    }

    @Override
    public String toString() {
        return formatAddress(address, "-", "", "");
    }

    @Override
    public boolean equals(Object other) {
        if(other == null || !(other instanceof HardwareAddress)) {
            return false;
        }
        return this.compareTo((HardwareAddress)other) == 0;
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(address);
    }

    protected static String formatAddress(final int[] bytes, final String delimiter, final String start, final String end) {
        return Arrays.stream(bytes)
                .mapToObj(val -> "0" + Integer.toHexString(val))
                .map(str -> str.length() == 3 ? str.substring(1, 3) : str)
                .collect(Collectors.joining(delimiter, start, end));
    }
}
