/*
 *  Copyright (C) 2011, 2012
 *  This file is part of GRASSMARLIN.
 */
package core.importmodule;

import core.Core;
import core.Environment;
import core.types.LogEmitter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.io.FileUtils;

/**
 * An PCAPNG import uses Wireshark to convert the file for this ImportItem into a .pcap
 * and stores it in the {@link core.Environment#DIR_CONVERTED_PCAP} directory.
 * 
 * It also added a shutdown hook to delete the newly created file on shutdown.
 * 
 * The new converted files path can be found post-run by calling {@link #getSafeCanonicalPath() }.
 * 
 * The {@link PCAPImport#run() } method is called once the conversion is complete.
 */
public class PCAPNGImport extends PCAPImport {
    /**
     * Message displayed when this import is created.
     */
    private static final String START_MESSAGE = "PCAPNG requires WireShark, this import will be converted automatically.";
    /**
     * The global logger.
     */
    private static final LogEmitter emitter = LogEmitter.factory.get();
    
    private String targetPcapFileName;
    
    public PCAPNGImport(String path) {
        super(path);
        emitter.emit(PCAPNGImport.class, Core.ALERT.INFO, START_MESSAGE);
    }

    /**
     * Created the modified path to the to-be .pcap file.
     * @return Path to the new .pcap file.
     */
    private String getNewFilePath() {
        String name = getName();
        name = name.replaceAll("(\\..*$)", "");
        return Environment.DIR_CONVERTED_PCAP.getPath() + File.separator + name.concat(".pcapng");
    }

    /**
     * Report some unknown issue where the PCAPNG fails to convert.
     */
    void genericFailure() {
        emitter.emit(this, Core.ALERT.INFO, "Conversion failed for " + getName() + ".");
        fail();
    }
    
    /**
     * From the Wireshark directory gets the executable that converts pcapng to pcap.
     * @param name Name of the file to locate.
     * @param wiresharkDir The directory containing the wireshark executable.
     * @return Path to the file if found, else throws.
     * @throws FileNotFoundException Thrown when no such file is found.
     */
    private String locateFilebyName(String name, String wiresharkDir) throws FileNotFoundException {
        File f = new File(wiresharkDir);
        String[] files = f.list();
        for( int i = 0; i < files.length; i++ ) {
            if( files[i].contains(name) ) {
                return files[i];
            }
        }
        throw new java.io.FileNotFoundException(wiresharkDir);
    }

    @Override
    public String getSafeCanonicalPath() {
        return this.targetPcapFileName;
    }
    
    @Override
    public void run() {
        
        File source = this;
        File target = new File( getNewFilePath() );
        
        emitter.emit(this, Core.ALERT.INFO, String.format("Converting \"%s\" to \"%s\"", source.getName(), target.getName()));
        
        try {
            String wiresharkDir = null;
            try {
                wiresharkDir = Environment.WIRESHARK_EXEC.getDir().getParentFile().getCanonicalPath();
            } catch( NullPointerException ex ) {
                String msg = "Could not convert PCAPNG file, is the Wireshark path set?";
                fail(msg);
                return;
            }
            String srcPath = source.getCanonicalPath();
            String dstPath = target.getCanonicalPath();
            String execName = locateFilebyName( "editcap", wiresharkDir );
            String command = String.format("\"%s%s%s\" -F libpcap \"%s\" \"%s\"", wiresharkDir, File.separator, execName, srcPath, dstPath);
            
            Process proc = Runtime.getRuntime().exec(command);
            proc.waitFor();
            
            if( target.exists() ) {
                FileUtils.forceDeleteOnExit(target);
                this.targetPcapFileName = target.getCanonicalPath();
                emitter.emit(this, Core.ALERT.INFO, "Conversion succeedes for " + target.getName() + ".");
                super.run();
            } else {
                genericFailure();
            }
            
        }catch (InterruptedException | IOException ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, null, ex);
            genericFailure();
        }
        
    }
    
}
