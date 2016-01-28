package core.importmodule;

// gm

// dep

import core.Core;
import core.types.LogEmitter;
import org.jnetpcap.Pcap;
import org.jnetpcap.PcapIf;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * BESTDOG - 12/1/2015 - revamped for linux comparability.
 */
public class DeviceList extends ArrayList<LivePCAPImport> {
    public Boolean good = false;
    private int default_device_index = 0;

    public DeviceList populate() {
        try {
            List<PcapIf> devs = new ArrayList<>();
            StringBuilder error = new StringBuilder();

            if (Pcap.findAllDevs(devs, error) != Pcap.ERROR) {
                if (devs.isEmpty()) {
                    this.good = false;
                } else {
                    this.good = true;
                    devs.stream()
                            .map(this::newItem)
                            .forEach(this::add);
                }
            }

            Pcap.freeAllDevs(devs, error);
        } catch (java.lang.UnsatisfiedLinkError ex) {
            good = false;
            Logger.getLogger(DeviceList.class.getName()).log(Level.SEVERE, null, ex);
            LogEmitter.factory.get().emit(this, Core.ALERT.DANGER, "Live capture is unavailable do to insufficient permissions or missing PCAP library.");
        }
        return this;
    }

    private LivePCAPImport newItem(PcapIf pcapif) {
        return new LivePCAPImport(pcapif);
    }

    // can return an empty interface with good = false
    public LivePCAPImport getDefault() {
        if (this.good)
            return get(default_device_index);
        else
            return new LivePCAPImport(null);
    }

    // if the device index is out of range set good = false
    public DeviceList setDefault(int i) {
        if (i >= this.size()) {
            this.good = false;
        } else
            this.default_device_index = i;
        return this;
    }

}
