package core.importmodule.parser.cisco;

import core.topology.Mac;
import core.topology.PhysicalNode;
import core.topology.Port;
import core.topology.Vlan;
import core.types.TriConsumer;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Cisco Running-Config parser. Parses 'running-config' commands.
 *
 * An RC command populates many Trait objects for a device. The format has
 * unique identifiers for each section and a section separator character ('!').
 *
 */
class CiscoRCParse implements SubParser {

    public static boolean debug = false;

    /**
     * This token separates sections within the config file.
     */
    private static final String SECTION_SEPARATOR = "!";

    private static Map<String, TriConsumer<String, BufferedReader, PhysicalNode>> TRIGGERS;
    /**
     * Key designates the start of a 'version' section where
     * {@link Trait#VERSION_NO} can be taken from.
     */
    public static final String VERSION_KEY = "version";
    public static final String HOSTNAME_KEY = "hostname";
    public static final String USERNAME_KEY = "username";
    public static final String INTERFACE_KEY = "interface";
    /**
     * A section that must be skipped, it contains natural language that appears
     * as other patterns recognized by this parser.
     */
    public static final String SKIP_KEY0 = "---------";

    static {
        Map<String, TriConsumer<String, BufferedReader, PhysicalNode>> map = new HashMap<>();

        map.put(INTERFACE_KEY, CiscoRCParse::parseInterface);
        map.put(HOSTNAME_KEY, CiscoRCParse::parseHostname);
        map.put(USERNAME_KEY, CiscoRCParse::parseUsername);
        map.put(VERSION_KEY, CiscoRCParse::parseVersion);
        map.put(SKIP_KEY0, CiscoRCParse::skip0);

        TRIGGERS = Collections.unmodifiableMap(map);
    }

    /**
     * Expects the below block, interface GigabitEthernet0/0/0 description link
     * to Router3 ip address 10.1.3.1 255.255.255.0 negotiation auto
     *
     * @param line Line this was entered on.
     * @param reader BufferedReader of the input file.
     * @param map TraitMap to populate.
     */
    static void parseInterface(String line, BufferedReader reader, PhysicalNode data) {
        /* local keys for this subsection */
        String addressKey = "ip address";
        String descriptionKey = "description";
        /* key for " switchport trunk allowed vlan 10,20,30" */
        String switchportKey = "switchport";
        String switchportVlanKey = "vlan";
        String subInterfaceDelimeter = ".";
        String bitBucket = "Null";

        line = SubParser.after(line, INTERFACE_KEY);

        if (line.startsWith(bitBucket)) {
            return;
        }

        /**
         * a shutdown interface can be seen as having no port identifiers; as in
         * "Ethernet0" with no slash. This can however also be an ACE
         * vlan-interface so we must try the latter else assume the former.
         */
        if (!SubParser.isPhysicalPort(line)) {
            parseVirtualInterface(line, reader, data);
            return;
        }

        Port p;
        
        if( line.contains(subInterfaceDelimeter) ) {
            int pos = line.indexOf(subInterfaceDelimeter);
            String interfaceText = line.substring(0, pos);
            line = line.substring(pos+subInterfaceDelimeter.length());
            pos = SubParser.locate(line, Character::isWhitespace);
            line = line.substring(0, pos);
            p = data.getPort( new Port(data, interfaceText, Mac.MissingMac) );
            
            Integer subInterface = Integer.valueOf(line);
            p.addSubInterface(subInterface);
        } else {
            p = data.getPort( new Port(data, line, Mac.MissingMac) );
        }

        if( p == null ) {
            System.out.println(  );
        }

        try {
            while ((line = reader.readLine()) != null && !line.startsWith(SECTION_SEPARATOR)) {
                line = line.trim();
                if (line.startsWith(addressKey)) {
                    String ip = SubParser.getFirstWord(SubParser.after(line, addressKey));
//                    ifa.setIp( new Ip.Ip4(ip) );
                } else if (line.startsWith(descriptionKey)) {
                    String description = SubParser.after(line, descriptionKey);
                    p.setDescription( description );
                } else if (line.startsWith(switchportKey)) {
                    if (line.contains(switchportVlanKey)) {
                        /* get the last contiguous charsequence in the line, should look like 10,20,30, 20-25 */
                        String vlans = SubParser.getLastWord(line);
                        SubParser.parseNumericList(vlans).forEach( id -> {
                            Vlan vlan = data.getVlan(id);
                            p.addVlan(vlan);
                            vlan.ports.add(p);
                        });
                    }
                }

            }
        } catch (IOException ex) {
            Logger.getLogger(CiscoRCParse.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Parses an 'interface' section that often appears as 'interface VLAN30'.
     *
     * NOTE! this expects the interface portion removed.
     *
     * @param line Line the rule was entered on with the beginning ''interfaces'
     * removed.
     * @param reader BufferedReader for this input file.
     * @param map Map to be have a {@link Trait#VLAN_LIST} populated.
     */
    static void parseVirtualInterface(String line, BufferedReader reader, PhysicalNode data) {
        /* local keys for this subsection */
        String addressKey = "ip address";
        String shutdownKey = "shutdown";
        String noIpKey = "no ip address";

        try {
            int pos = SubParser.locateFromEnd(line, Character::isAlphabetic);
            String vlanName = line;
            String vlanId = line.substring(pos);
            while ((line = reader.readLine()) != null) {
                line = line.trim();

                if (line.startsWith(shutdownKey) || line.startsWith(noIpKey) || line.startsWith(SECTION_SEPARATOR)) {
                    break;
                } else if (line.startsWith(addressKey)) {
                    
                    /* unusued
                    String ip = SubParser.getFirstWord(SubParser.after(line, addressKey));
                    String subnet = SubParser.getLastWord(line);
                    */
                    Vlan vlan = data.getVlan( Integer.valueOf(vlanId) );
                    
                    if (debug) {
                        System.out.println(String.format("\tiface '%s' is vlan", vlanName));
                    }
                }
            }
        } catch (Exception ex) {
            Logger.getLogger(CiscoRCParse.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Parses a simple simple line "version 12.4", setting
     * {@link Trait#VERSION_NO}.
     *
     * @param line Line containing a version number.
     * @param reader argument ignored.
     * @param map TraitMap to be populated with {@link Trait#VERSION_NO}.
     */
    static void parseVersion(String line, BufferedReader reader, PhysicalNode data) {
        String ver = SubParser.after(line, VERSION_KEY);
        data.getVersion().version = ver;
    }

    /**
     * Parses a simple single line "hostname Router1", setting
     * {@link Trait#HOST_NAME}.
     *
     * @param line Line containing a host name.
     * @param reader argument ignored.
     * @param map TraitMap to be populated with {@link Trait#HOST_NAME}.
     */
    static void parseHostname(String line, BufferedReader reader, PhysicalNode data) {
        String hostname = SubParser.after(line, HOSTNAME_KEY);
        data.setName(hostname);
    }

    /**
     * Skips a section with natural language delimited by lines of "-----"
     * dashes.
     *
     * @param line Line this was entered on.
     * @param reader BufferedReader of the input file which will attempt to read
     * several lines until the section is over.
     * @param map argument ignored.
     */
    static void skip0(String line, BufferedReader reader, PhysicalNode map) {
        try {
            while ((line = reader.readLine()) != null && !line.startsWith(SKIP_KEY0)) {
                /* skip */
            }
        } catch (IOException ex) {
            Logger.getLogger(CiscoRCParse.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Parses a simple single line "username test password 0 test" creating or
     * adding to a {@link Trait#USERNAME_LIST}. This will ignore secret keys as
     * they are often useless.
     *
     * @param line Line containing a host name and (optional) password.
     * @param reader argument ignored.
     * @param map TraitMap to be populated with a {@link Trait#USERNAME_LIST}
     * which will contain at least one new {@link Trait#USERNAME_ENTRY}.
     */
    static void parseUsername(String line, BufferedReader reader, PhysicalNode data) {
        /* local token for this section */
        String passwordToken = "password";

        String username = SubParser.getFirstWord(SubParser.after(line, USERNAME_KEY));
        
        data.getUserInfo().addUser(username);
        
        if (line.startsWith(passwordToken)) {
            String password = SubParser.getLastWord(line);
            data.getUserInfo().addPassword(username, password);
        }
    }


    @Override
    public boolean apply(CiscoReader r, File f, PhysicalNode data) {
        if (debug) {
            System.out.println("Running-config parser started " + f.getName());
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(f))) {
            String line;
            while ((line = reader.readLine()) != null) {

                for (String key : TRIGGERS.keySet()) {
                    if (line.startsWith(key)) {
                        if (debug) {
                            System.out.println(String.format("Key(%s) found: %s", key, line));
                        }
                        TRIGGERS.get(key).accept(line, reader, data);
                        break;
                    }
                }
            }

        } catch (IOException ex) {
            Logger.getLogger(CiscoRCParse.class.getName()).log(Level.SEVERE, null, ex);
        }
        return true;
    }

}
