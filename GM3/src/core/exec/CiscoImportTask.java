/*
 *  Copyright (C) 2011, 2012
 *  This file is part of GRASSMARLIN.
 */

package core.exec;

import core.importmodule.CiscoImport;
import core.importmodule.parser.cisco.CiscoReader;
import core.topology.PhysicalNode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * <pre>
 * 
 * </pre>
 */
public class CiscoImportTask extends ImportTask<CiscoImport> {

    /**
     * Note, we ignore services addresses and the VLAN title for them is "ALL"
     * Service addresses should be excluded from topology discovery because they will be seen
     * as originating from the same address on all networks when in reality they are from different switches on each.
     * Examples are, 
     * Protocol = min-Address / max-Address
     * CDP, UDLD, DTP, VTP, PAGP = 01-00-0C-CC-CC-CC
     * CGMP = 01-00-0C-CC-CC-CD
     * system CPP/BPDU = 01-80-C2-00-00-00 / 01-80-C2-00-00-0F
     * GARP = 01-80-C2-00-00-20 / 01-80-C2-00-00-2F
     * Besides these standard addresses some vendors have their own special service ranges.
     * The management services Vlan key is "ALL", its defaulted to VLAN1 on most / all cisco switches.
     * This Vlan is usually associated with a set of virtual physical addresses used as sources of BPDU traffic.
     */
    public final static Integer DEFAULT_SERVICE_VLAN = -1;
    /**
     * The key for CISCO switches where ports are seen as CPU, meaning its a virtual address usually used for management services & BPDU.
     * This will typically (always) be seen with VLAN=ALL because these are management services.
     */
    public final static Integer DEFAULT_SERVICE_PORT = -1;
    
    /**
     * Text displayed on the entities vendor attribute
     */
    public static final String CISCO_VENDOR_NAME = "Cisco Systems";
    
    public static final String ANON_NAME_ALPHA = "ABCDEFGHIJKMNOPQRSTUVWXYZ1234567890";
    public static final int    ANON_NAME_SIZE = 6;
    
    final Map<String,PhysicalNode> namedDevices;
    final CiscoReader reader;
    
    final Consumer out = System.out::println;
    
    List<PhysicalNode> data;
    
    public CiscoImportTask(CiscoImport item, CiscoReader reader) {
        super(item);
        this.reader = reader;
        this.namedDevices = new HashMap<>();
        data = new ArrayList<>();
    }
    
    public List<PhysicalNode> getSourceData() {
        return data;
    }
    
    @Override
    public void run() {
        if( reader.apply(importItem, data) ) {
            getPipeline().getRawPhysicalData().addAll(data);
        }
        complete();
    }

}
