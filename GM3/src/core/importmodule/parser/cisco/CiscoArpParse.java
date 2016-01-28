/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package core.importmodule.parser.cisco;

import core.importmodule.Trait;
import core.topology.Ip;
import core.topology.Mac;
import core.topology.PhysicalNode;
import core.topology.Port;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Parses a Cisco 'show arp' command. The ARP table has 6 columns. We use 4, 0, 1
 * Address(ip), 3 Hardware Addr, 4 Type, 5 Interface. Age & Protocol are
 * ignored.
 *
 * This information fulfills the data required for a {@link Trait#INTERFACE_LIST_ENTRY}.
 */
class CiscoArpParse implements SubParser {

    public static boolean debug = false;
    /**
     * We expect size columns.
     */
    private static final int EXPECTED_COLUMNS = 6;
    private static final int IP_COL = 1;
    private static final int MAC_COL = 3;
    private static final int INTERFACE_COL = 5;
    

    @Override
    public boolean apply(CiscoReader r, File f, PhysicalNode node) {
        if( debug ) {
            System.out.println("ARP parser started " + f.getName());
        }
        
//        int initialSize = m.size();

        try (BufferedReader reader = new BufferedReader(new FileReader(f))) {

            String line;

            while ((line = reader.readLine()) != null) {
                String[] cols = line.split("\\s+"); // split on all white space

                if (cols.length == EXPECTED_COLUMNS) {

                    String ipText = cols[IP_COL];
                    String macText = cols[MAC_COL];
                    String interfaceText = cols[INTERFACE_COL];
                        
                    Ip ip = new Ip.Ip4(ipText);
                    Mac mac = new Mac(macText);

                    Port p = node.getPort( new Port(node, interfaceText, mac));
                    p.add(mac);
                    
                    if( debug ) {
                        System.out.printf("%s\n", p);
                    }
                    
                }

            }

        } catch (IOException ex) {
            Logger.getLogger(CiscoArpParse.class.getName()).log(Level.SEVERE, null, ex);
            r.subParserLog(ex);
        }

//        return initialSize < m.size();
        return true;
    }

}
