package core.importmodule;

import core.Core;
import core.Environment;
import core.types.LogEmitter;
import java.io.File;
import java.io.IOException;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import org.jnetpcap.Pcap;
import org.jnetpcap.PcapDumper;
import org.jnetpcap.PcapIf;
import ui.GrassMarlin;

/**
 * Extends Inner classes PcapIf and overloads isgood methods to work specifically with PcapReader.
 * This is a special import item for capturing from a device instead of a file.
 * 
 * This "specialty" import will open gui popups and ask the user if he/she would like a drumpfile created
 * and if so will create an automatic dump file in the {@link core.Environment#DIR_LIVE_CAPTURE} folder.
*/
public class LivePCAPImport extends PCAPImport {
	private final PcapIf device;
    private PcapDumper dumper;
    private boolean isUsingDumpFile;
    
	public LivePCAPImport(PcapIf ifa) {
        super(ifa.getName()+ifa.getDescription(), Import.LIVE, false);
		this.device = ifa;
        isUsingDumpFile = false;
	}

    /**
     * We ignore the argument and open the {@link #device#getName() } instead.
     * @param pathOrDevice Argument ignored.
     * @return Pcap handle to the NIC/device that belongs to this ImportItem.
     */
    @Override
    protected Pcap getHandle(String pathOrDevice) {
        Pcap handle = null;

        int snaplen = getImporter().getPreferences().snaplen;
        int mode = getImporter().getPreferences().mode;
        int timeout = getImporter().getPreferences().timeout;
        
        try {
            handle = Pcap.openLive(device.getName(), snaplen, mode, timeout, errorBuffer);
        } catch (UnsatisfiedLinkError err) {
            Logger.getLogger(PCAPImport.class.getName()).log(Level.SEVERE, "Importing PCAP is disabled.", err);
        }

        return handle;
    }
    
    @Override
    public String getName() {
        String name;
        if(Objects.isNull(device) ) {
            name = "Error.";
        } else {
            name = device.getName();
        }
        return name;
    }
    
    @Override
    public String getCanonicalPath() throws IOException {
        if( device == null ) throw new IOException("Device not found");
        return device.getName();
    }
    
	@Override
	public String toString() {
		return this.getName();
	}
    
    @Override
    public boolean isGood() {
        return true;
    }
    
    @Override
    public Boolean isComplete() {
        return false;
    }

    @Override
    public Boolean isIncluded() {
        return true;
    }

    /**
     * Ask use if they would like a dump file, if so, create one.
     * @param pcap Reference to the PcapHandle.
     */
    @Override
    protected boolean beforeStart(Pcap pcap) {
        super.beforeStart(pcap);
        int option = JOptionPane.showConfirmDialog(GrassMarlin.window, "Create a dumpfile?");
        if( option == JOptionPane.OK_OPTION  ) {
            try {
            
                String filename = System.currentTimeMillis()+"_dump.pcap";
                File f = new File( Environment.DIR_LIVE_CAPTURE.getPath()+File.separator + filename );

                JFileChooser fc = new JFileChooser();
                fc.setSelectedFile(f);

                int i = fc.showSaveDialog(GrassMarlin.window.getContentPane());
                
                if( i == JFileChooser.APPROVE_OPTION ) {
                    dumper = pcap.dumpOpen(fc.getSelectedFile().getCanonicalPath());
                }
            
            } catch( Exception ex ) {
                Logger.getLogger(LivePCAPImport.class.getName()).log(Level.SEVERE, "Failed to set dumpfile.", ex);
            }
        } else if( option == JOptionPane.CANCEL_OPTION ) {
            return false;
        }
        isUsingDumpFile = dumper != null;
        return true;
    }
    
    @Override
    protected void onEnd(Pcap pcap) {
        super.onEnd(pcap);
        LogEmitter.factory.get().emit(this, Core.ALERT.INFO, "Live capture has ended.");
    }
    
    @Override
    public boolean saveToDumpfile() {
        return isUsingDumpFile;
    }
    
    @Override
    public PcapDumper getPcapDumper() {
        return dumper;
    }
    
}
