/*
 *  Copyright (C) 2011, 2012
 *  This file is part of GRASSMARLIN.
 */
package core.importmodule.parser.cisco;

import core.topology.PhysicalNode;
import core.topology.TopologySourceVersion;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 */
public class CiscoVersionParse implements SubParser {

    /**
     * The first text seen on the line where the version is found.
     */
    private final String V12_KEY = "IOS";
    private final String V12_SOFTWARE_KEY = "System image file is";
    private final int V12_VERSION_NO_INDEX = 1;
    
    private final String V12dot2_KEY = "Cisco IOS";
    private final int V12dot2_VERSION_NO_INDEX = 2;
    
    private final String MODEL_NUMBER_KEY = "Model number";
    private final String SERIAL_NUMBER_KEY = "System serial number";
    private final String SERIAL_NUMBER_ALT_KEY = "Motherboard serial number";
    
    Map<String,BiConsumer<String,TopologySourceVersion>> parseMethods;
    
    public CiscoVersionParse() {
        parseMethods = new HashMap<>();
        parseMethods.put(V12_KEY, this::parseV12);
        parseMethods.put(V12dot2_KEY, this::parseV12dot2);
        parseMethods.put(V12_SOFTWARE_KEY, this::parseSoftwareVersion);
        parseMethods.put(MODEL_NUMBER_KEY, this::parseModelNumber);
        parseMethods.put(SERIAL_NUMBER_KEY, this::parseSerialNumber);
        parseMethods.put(SERIAL_NUMBER_ALT_KEY, this::parseSerialNumber);
    }
    
    
    @Override
    public boolean apply(CiscoReader r, File f, PhysicalNode m) {
        
        TopologySourceVersion ver = m.getVersion();
        
        try ( BufferedReader reader = new BufferedReader(new FileReader(f))) {
            String line;
            while( (line = reader.readLine())!= null ) {
                for( String key : parseMethods.keySet() ) {
                    if( line.startsWith(key) ) {
                        parseMethods.get(key).accept(line, ver);
                    }
                }
            }
            
        } catch( IOException ex) {
            Logger.getLogger(CiscoVersionParse.class.getName()).log(Level.SEVERE, "Cannot parse version file.", ex);
        }
        
        return true;
    }

    
    
    private void parseV12dot2(String line, TopologySourceVersion version) {
        String[] parts = line.split(",");
        String number = parts[V12dot2_VERSION_NO_INDEX];
        version.version = number;
    }
    
    private void parseV12(String line, TopologySourceVersion version) {
        String[] parts = line.split(",");
        String number = parts[V12_VERSION_NO_INDEX];
        version.version = number;
    }
    
    private void parseSoftwareVersion(String line, TopologySourceVersion version) {
        line = SubParser.after(line, V12_SOFTWARE_KEY);
        String softwareVer = line.replace("\"", "");
        version.software = softwareVer;
    }
    
    private void parseModelNumber(String line, TopologySourceVersion version) {
        String modelNo = SubParser.getLastWord(line);
        version.model = modelNo;
    }
    
    private void parseSerialNumber(String line, TopologySourceVersion version) {
        line = SubParser.after(line, SERIAL_NUMBER_KEY);
        String serialNo = SubParser.getLastWord(line);
        version.serial = serialNo;
    }
    
}
