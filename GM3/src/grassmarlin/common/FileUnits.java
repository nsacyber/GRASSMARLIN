package grassmarlin.common;

import javafx.beans.binding.NumberExpression;
import javafx.beans.binding.ObjectBinding;

public class FileUnits {
    public static class FileSize implements Comparable<FileSize> {
        private final long size;

        public FileSize(long size) {
            this.size = size;
        }

        @Override
        public int compareTo(final FileSize other) {
            final long delta = this.size - other.size;
            return delta == 0 ? 0 : (delta < 0 ? -1 : 1);
        }

        @Override
        public String toString() {
            return FileUnits.format(size);
        }
    }

    public static class Binding extends ObjectBinding<FileSize> {
        private final NumberExpression value;

        public Binding(NumberExpression value) {
            super.bind(value);

            this.value = value;
        }

        @Override
        public FileSize computeValue() {
            return new FileSize(value.longValue());
        }
    }

    protected static final FileUnits[] arrUnits = new FileUnits[] {
            new FileUnits(1152921504606846976L, "EB"),  // I hope I live long enough to see this be relevant.
            new FileUnits(1125899906842624L, "PB"),
            new FileUnits(1099511627776L, "TB"),
            new FileUnits(1073741824L, "GB"),
            new FileUnits(1048576L, "MB"),
            new FileUnits(1024L, "KB"),
    };

    private final long threshold;
    private final long sizeUnit;
    private final String units;

    protected FileUnits(long sizeUnit, String units) {
        this.threshold = (long)(sizeUnit * 1.2);
        this.sizeUnit = sizeUnit;
        this.units = " " + units;
    }

    public static String format(final long size) {
        for(FileUnits unit : arrUnits) {
            if(size > unit.threshold) {
                return unit.formatSize(size);
            }
        }
        return size + " B";
    }

    protected String formatSize(final long size) {
        final long tenTimesPrintableSize = 10L * size / this.sizeUnit;
        return (tenTimesPrintableSize / 10L) + "." + (tenTimesPrintableSize % 10) + this.units;
    }
}