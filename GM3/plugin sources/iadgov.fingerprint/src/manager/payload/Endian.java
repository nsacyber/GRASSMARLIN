package iadgov.fingerprint.manager.payload;

public enum Endian {
    BIG,
    LITTLE;

    public static Endian getDefault() {
        return BIG;
    }
}
