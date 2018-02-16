package grassmarlin.plugins.internal.livepcap;

import com.sun.istack.internal.NotNull;
import com.sun.javafx.collections.ObservableListWrapper;
import grassmarlin.Logger;
import grassmarlin.RuntimeConfiguration;
import grassmarlin.session.Session;
import grassmarlin.session.hardwareaddresses.Mac;
import grassmarlin.session.pipeline.Pipeline;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.ObservableList;
import org.jnetpcap.Pcap;
import org.jnetpcap.PcapIf;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CopyOnWriteArrayList;

public class PcapEngine {
    public static class Device {
        private final PcapIf device;

        public Device(final PcapIf device) {
            this.device = device;
        }

        public PcapIf getDevice() {
            return this.device;
        }

        @Override
        public boolean equals(Object other) {
            if(other instanceof Device) {
                return this.device.equals(((Device) other).device);
            } else if(other instanceof PcapIf) {
                return this.device.equals(other);
            } else {
                return false;
            }
        }

        @Override
        public String toString() {
            if(this.device == null) {
                return "[null]";
            } else {
                //Various failovers for different JNetPcap implementations.
                String text = this.device.getDescription();
                if(text == null || text.isEmpty()) {
                    text = this.device.getName();
                }
                if(text == null || text.isEmpty()) {
                    try {
                        text = new Mac(this.device.getHardwareAddress()).toString();
                    } catch(IOException ex) {
                        text = null;
                    }
                }
                if(text == null || text.isEmpty()) {
                    text = "[Unnamed Device]";
                }

                return text;
            }
        }
    }

    private final RuntimeConfiguration config;
    private final ObservableList<Device> devices;
    private final BooleanProperty isLivePcapAvailable;
    private final BooleanProperty isLivePcapRunning;
    private final StringProperty entryName;
    private final StringProperty filter;
    private final BlockingQueue<Object> queue;

    public PcapEngine(final RuntimeConfiguration config) {
        this.config = config;
        this.devices = new ObservableListWrapper<>(new CopyOnWriteArrayList<>());
        this.isLivePcapAvailable = new SimpleBooleanProperty(false);
        this.isLivePcapRunning = new SimpleBooleanProperty(false);
        this.entryName = new SimpleStringProperty(Pipeline.ENTRY_DEFAULT);
        this.filter = new SimpleStringProperty();
        this.queue = new ArrayBlockingQueue<>(100);

        if(this.config.allowLivePcapProperty().get()) {
            this.enumerateDevices();
            this.isLivePcapAvailable.set(!this.devices.isEmpty());
        }
    }

    private void enumerateDevices() {
        final List<PcapIf> rawDevices = new ArrayList<>();
        final StringBuilder error = new StringBuilder();

        try {
            if(Pcap.findAllDevs(rawDevices, error) != Pcap.ERROR) {
                this.devices.removeIf(device -> !rawDevices.contains(device.getDevice()));
                for(PcapIf rawDevice : rawDevices) {
                    final Device device = new Device(rawDevice);
                    this.devices.add(device);
                }
            }
        } catch(java.lang.UnsatisfiedLinkError ex) {
            Logger.log(Logger.Severity.ERROR, "Live Capture is unavailable due to insufficient permissions or a missing PCAP library: %s", ex.getMessage());
        } finally {
            Pcap.freeAllDevs(rawDevices, error);
        }
    }

    protected static Path getDumpFileName(final Device device) {
        return Paths.get(
                RuntimeConfiguration.getPersistedString(RuntimeConfiguration.PersistableFields.DIRECTORY_USER_DATA),
                "pcap",
                String.format("capture_%s_%s.pcap", device.toString(), Instant.now().toString()).replaceAll("[^a-zA-Z0-9_\\.]", "_")
        ).toAbsolutePath();
    }

    private LivePcapImport importCurrent;
    public Iterator<Object> start(@NotNull final LivePcapImport importCurrent, @NotNull final Session session) {
        if(this.importCurrent != null) {
            //HACK: Right now we can only track one import at a time--we should be tracking one import per device.
            return null;
        }
        this.importCurrent = importCurrent;

        //HACK:  There is no good reason why we should only support Live Pcap on a single device at any given point in time.  We do, however, impose this restriction because I am being lazy right now and not writing it to work that way.  A slightly better reason is that we need to revisit UI conventions and implementation to effectively support multiple live pcaps as a feature, but that isn't a particularly good reason considering the depth of revision 3.3 already represents.
        //To move away from the singleton live pcap, the shared queue/iterator needs to be revised, either via multiple iterators or reference counting.
        if(isLivePcapRunning.get()) {
            Logger.log(Logger.Severity.WARNING, "Unable to start Live Pcap while Live Pcap is running.");
            return null;
        } else if(!isLivePcapAvailable.get()) {
            //Not a warning since the UI shouldn't have allowed this in the first place; we're refusing to try rather than trying and producing an unexpected result.
            Logger.log(Logger.Severity.INFORMATION, "Live PCAP is disabled.");
            return null;
        } else {
            try {
                //Note:  There is a race condition present in how this is handled.  The old
                // iterator will not be removed from the BufferedAggregator until it is empty
                // and live pcap has stopped.  A new one can be added as soon as it has been
                // stopped.  Since they share the same queue (which should have been cleared
                // anyway) both will exist until the end condition has been met again.  This
                // will cause a bias towards processing Live Pcap and waste a few cycles/bytes
                // of RAM, but should have no significant impact.
                //TODO: Modify the iterator to respond to the change in isLivePcapRunning by flipping a switch, removing the above race condition.
                importCurrent.start(this.filter.get(), this.queue);
                isLivePcapRunning.set(true);
                return iterator;
            } catch (Exception ex) {
                ex.printStackTrace();
                isLivePcapRunning.set(false);
                return null;
            }
        }
    }

    protected final Iterator<Object> iterator = new Iterator<Object>() {
        @Override
        public Object next() {
            return queue.poll();
        }
        @Override
        public boolean hasNext() {
            return !queue.isEmpty() || PcapEngine.this.isLivePcapRunning.get();
        }
    };

    public void stop() {
        if(isLivePcapRunning.get()) {
            if(this.importCurrent != null) {
                this.importCurrent.stop();
                this.importCurrent = null;
            }
            this.queue.clear();
        }
        isLivePcapRunning.set(false);
    }

    public ObservableList<Device> getDeviceList() {
        return this.devices;
    }
    public BooleanProperty pcapAvailableProperty() {
        return this.isLivePcapAvailable;
    }
    public BooleanProperty pcapRunningProperty() {
        return this.isLivePcapRunning;
    }
    public StringProperty entryNameProperty() {
        return this.entryName;
    }
}
