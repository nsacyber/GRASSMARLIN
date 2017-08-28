package iadgov.timefilter;

import java.io.Serializable;
import java.time.Instant;
import java.time.ZoneOffset;

public class Timespan implements Serializable {
    private static final long serialVersionUID = 1L;

    public final Instant start;
    public final Instant end;
    public final ZoneOffset offset;

    public Timespan(Instant start, Instant end, ZoneOffset offset) {
        this.start = start;
        this.end = end;
        this.offset = offset;
    }

    public String toString() {
        return start + " , " + end + " : " + offset;
    }
}
