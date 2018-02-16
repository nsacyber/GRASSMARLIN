package iadgov.siemens;

public class S7Payload {
    private final byte[] bytes;

    public S7Payload(final byte[] bytes) {
        this.bytes = bytes;
    }

    public byte[] getBytes() {
        return this.bytes;
    }
}
