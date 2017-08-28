package grassmarlin.common.fxobservables;

import javafx.application.Platform;
import javafx.beans.property.SimpleDoubleProperty;

/**
 * A DoubleProperty that is used to track progress through a file that fires events in the UI thread, can be updated from any thread, and doesn't create massive change spam.
 *
 * In older revisions, we just went with SimpleDoubleProperty instances, but that had issues when we started using a multithreaded source.
 * The FxDoubleProperty was a huge improvement, but generated so many change events, most of which were meaningless.
 * 3.2 finally used a version of the FxDoubleProperty that limited updates based on passing certain thresholds, and tried to minimize the amount of integer-to-double conversions.
 * The 3.2 solution worked, but the code became a bit of a mess and the early development of 3.3 happened without access to the 3.2 code, so a new solution built on the lessons learned from past implementations was born.
 *
 * For something that is so simple in concept, there is an awful lot of complexity in its use, but part of getting the implementation right is identifying how NOT to do this sort of thing.  Live and learn, right?  That's progress.  Hooray for progress!
 */
public class FxFileParseProgressProperty extends SimpleDoubleProperty {
    private final double progressPerInterval;
    private final long bytesPerInterval;

    private long cbRead;
    private long cntBlocksRead;
    private final long cbTotal;

    public FxFileParseProgressProperty(final long cbTotal) {
        //Add 1 to total size to avoid all sorts of edge-case handling for divide by zero.
        //Also, since we use -1 to represent files that don't exist, if we get here on one of those we have a clear reason for failure.
        this.cbTotal = cbTotal + 1;
        this.cbRead = 1L;
        this.cntBlocksRead = 0;

        if((double)cbTotal * 0.005 < 2.0) {
            bytesPerInterval = 1L;
            progressPerInterval = 1.0 / (double)this.cbTotal;
        } else {
            bytesPerInterval = (long)((double)this.cbTotal * 0.005);
            progressPerInterval = (double)bytesPerInterval / (double)this.cbTotal;
        }
    }

    public FxFileParseProgressProperty(final long cbTotal, final double current) {
        this(cbTotal);

        super.set(current);
    }

    public synchronized void recordProgress(long cbProgress) {
        this.cbRead += cbProgress;
        //System.out.println(String.format("Recording %d (+%d) / %d", this.getProgress(), cbProgress, this.cbTotal));
        if(this.getProgress() == this.cbTotal) {
            //Special case the 100% situation since it is unlikely to be on an even threshold.
            super.set(1.0);
        } else if(this.cbRead > this.bytesPerInterval) {
            //Account for progress that spans multiple blocks at once.
            final long cntBlocks = this.cbRead / this.bytesPerInterval;
            this.cntBlocksRead += cntBlocks;
            this.cbRead -= (cntBlocks * this.bytesPerInterval);
            super.set((double)this.cntBlocksRead * this.progressPerInterval);
        }
    }

    public long getProgress() {
        return (bytesPerInterval * this.cntBlocksRead) + cbRead;
    }

    @Override
    public final void set(double value) {
        throw new IllegalStateException("Cannot manually change the value of a FxFileParseProgressProperty.");
    }

    @Override
    protected void fireValueChangedEvent() {
        try {
            if (!Platform.isFxApplicationThread()) {
                Platform.runLater(this::fireValueChangedEvent);
            } else {
                super.fireValueChangedEvent();
            }
        } catch(IllegalStateException ex) {
            //Fx not initialized--possibly not an Fx UI mode, so just run in-thread
            super.fireValueChangedEvent();
        }
    }

    @Override
    public String toString() {
        return String.format("%d / %d [%d / %d %d-byte (%f) increments (%f)]", getProgress(), this.cbTotal, getProgress() / this.bytesPerInterval, this.cbTotal / this.bytesPerInterval, this.bytesPerInterval, this.progressPerInterval, get());
    }
}
