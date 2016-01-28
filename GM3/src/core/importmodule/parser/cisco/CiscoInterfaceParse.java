/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package core.importmodule.parser.cisco;

import core.Core;
import core.importmodule.Trait;
import core.importmodule.TraitMap;
import core.topology.IfaEx;
import core.topology.Ip;
import core.topology.Mac;
import core.topology.PhysicalNode;
import core.topology.Port;
import core.topology.TopologySource;
import core.topology.Vlan;
import core.topology.VlanEx;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;

/**
 * Parse a Cisco 'show interfaces' command into a {@link Trait#INTERFACE_LIST}.
 * The first line is the user-command, and so it may be ignored.
 */
class CiscoInterfaceParse implements SubParser {

    public static boolean debug = false;

    @Override
    public boolean apply(CiscoReader r, File file, PhysicalNode data) {
        if (debug) {
            System.out.println("interfaces parser started " + file.getName());
        }
//        int initialSize = root.size();

        IfaEx thisInterface = null;

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {

            String line = reader.readLine();

            if (line == null) {
                return false;
            }

            while ((line = reader.readLine()) != null && !line.isEmpty()) {
                /* new interface groups start with NO white space, so we skip the slac-lines */
                char textChar = line.charAt(0);
                if (Character.isWhitespace(textChar) || line.endsWith("down")) {

                } else {
                    
                    if (SubParser.isPhysicalPort(line)) {
                        parsePhysical(reader, line, data);
                    } else {
                        if( line.startsWith("Vlan") ) {
                            parseVirtual(reader, line, data);
                        }
                    }
                }
            }

        } catch (Exception ex) {
            r.subParserLog(ex);
        }

        return true;
    }

    /**
     * Parses a physical interface, expects to find a mac address and interface.
     *
     * Sample, ( set brackets delimit important data )
     * <pre>
     * {{FastEthernet0/3}} is down,
     *   line protocol is down (notconnect) Hardware is Fast Ethernet, address is {{000e.831e.7283}} (bia 000e.831e.7283)
     * </pre>
     *
     * @param r Reader for the open input file.
     * @param line Line such as above.
     * @param data TopologySource data for the entire import.
     */
    private void parsePhysical(BufferedReader r, String line, PhysicalNode data) throws IOException {
        String ifaText = SubParser.getFirstWord(line);
        
        /** next line expects '  Hardware is EtherSVI, address is 000e.831e.7280 (bia 000e.831e.7280)' */
        line = r.readLine().replaceAll("^.*?address.is.", "");
        String macText = SubParser.getFirstWord(line);
        Mac mac = new Mac(macText);
        
        Port p = data.getPort( new Port(data, ifaText, mac) );
        
    }

    /**
     * Parsed a vlan, expects to find, mac, ip, and vlan id.
     *
     * Sample, ( set brackets delimit important data )
     * <pre>
     * Vlan{{1}} is up, line
     *   protocol is down Hardware is EtherSVI, address is {{000e.831e.7280}} (bia 000e.831e.7280)
     *   Internet address is {{192.168.0.3}}/24
     * </pre>
     * 
     * @param r Reader for the open input file.
     * @param line Line such as above.
     * @param data TopologySource data for the entire import.
     */
    private void parseVirtual(BufferedReader r, String line, PhysicalNode data) throws IOException {
        String vlanId = SubParser.getFirstWord(line.replaceAll("^\\D+", ""));
        
        Vlan vlan = data.getVlan( Integer.valueOf(vlanId) );
        
        /** next line expects '  Hardware is EtherSVI, address is 000e.831e.7280 (bia 000e.831e.7280)' */
        line = r.readLine().replaceAll("^.*?address.is.", "");
        String macText = SubParser.getFirstWord(line);
        Mac mac = new Mac(macText);
        
        line = r.readLine().replaceAll("^.*?address.is.", "");
        String ipText = SubParser.getFirstWord(line);
        Ip ip = new Ip.Ip4(ipText);
        
        vlan.macs.add(mac);
    }

}
