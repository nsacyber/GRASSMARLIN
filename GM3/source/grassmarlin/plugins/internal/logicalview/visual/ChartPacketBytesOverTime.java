package grassmarlin.plugins.internal.logicalview.visual;

import grassmarlin.plugins.internal.logicalview.GraphLogicalEdge;
import grassmarlin.ui.common.controls.Chart;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class ChartPacketBytesOverTime extends Chart<GraphLogicalEdge.PacketMetadata, Long, Long> {
    public ChartPacketBytesOverTime() {
        //Y Axis should have fixed lower bound of 0; maybe this could be changed via options, but in practice this tends to best match the user's expectations.
        this.setYRange(new Range<>(0L, null));
    }

    @Override
    protected Long calculateXFor(final GraphLogicalEdge.PacketMetadata value) {
        return value.getTime();
    }
    protected Long calculateYFor(final GraphLogicalEdge.PacketMetadata value) {
        return value.getSize();
    }
    protected double normalizeX(final Range<Long> bounds, final Long value) {
        return (double)(value - bounds.getMin()) / (double)(bounds.getMax() - bounds.getMin());
    }
    protected double normalizeY(final Range<Long> bounds, final Long value) {
        return (double)(value - bounds.getMin()) / (double)(bounds.getMax() - bounds.getMin());
    }
    protected List<Long> generateXTicks(final Range<Long> base, final double low, final double high) {
        long tsSpan = base.getMax() - base.getMin();    //The ideal range, actual may be different
        final long tsLow = base.getMin() + (long)(low * (double)tsSpan);
        final long tsHigh = base.getMin() + (long)(high * (double)tsSpan) + 1;  //+1 forces uniqueness and compensates for rounding error

        final long tsDayStart = Instant.ofEpochMilli(tsLow).atZone(ZoneId.of("Z")).toLocalDate().atStartOfDay().toEpochSecond(ZoneOffset.UTC) * 1000;
        tsSpan = tsHigh - tsLow; // Use the actual range for further computation

        //We want to generate "up to about 20" ticks, so pick a tick size based on the time span that makes sense within the overall scale.
        final long tsStep;
        if(tsSpan < 20) {
            tsStep = 1;
        } else if(tsSpan < 100) {
            tsStep = 5;
        } else if(tsSpan < 1000) {
            tsStep = 50;
        } else if(tsSpan < 60000) {
            tsStep = 1000;
        } else if(tsSpan < 3600000) {
            tsStep = 30000;
        } else if(tsSpan < 86400000) {
            tsStep = 1800000;
        } else {
            tsStep = 36000000;
        }

        ArrayList<Long> result = new ArrayList<>(5);
        for(long idx = (tsLow - tsDayStart) / tsStep; idx <= (tsHigh - tsDayStart) / tsStep; idx++) {
            long value = tsDayStart + tsStep * idx;
            if(value > tsLow) {
                result.add(value);
            }
            if(value > tsHigh) {
                break;
            }
        }

        if(result.size() == 1) {
            result.add(result.get(0) + 1);
        }

        return result;
    }
    protected List<Long> generateYTicks(final Range<Long> base, final double low, final double high) {
        //Ignore high/low since we want an exact upper bound if possible.
        final long sizeTick = (base.getMax() - base.getMin() + 3) / 4;
        ArrayList<Long> result = new ArrayList<>(5);
        result.add(base.getMin());
        result.add(base.getMin() + sizeTick);
        result.add(base.getMin() + sizeTick * 2);
        result.add(base.getMin() + sizeTick * 3);
        result.add(base.getMin() + sizeTick * 4);

        return result;
    }
    protected String formatX(final Long x) {
        return Instant.ofEpochMilli(x).atZone(ZoneId.of("Z")).format(DateTimeFormatter.ISO_INSTANT);
    }
    protected String formatY(final Long y) {
        return y.toString();
    }
}
