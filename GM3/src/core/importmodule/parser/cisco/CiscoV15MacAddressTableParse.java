package core.importmodule.parser.cisco;

import core.importmodule.Trait;
import core.topology.Mac;
import core.topology.PhysicalNode;
import core.topology.Port;
import core.topology.Vlan;
import java.io.File;

/**
 * Parses the four column Mac Address Table.
 *
 * Parses a MAT with four columns as shown below,
 * <pre>
 * Vlan Mac Address    Type     Ports
 * ---- -----------    -------- -----
 * All  0100.0ccc.cccc STATIC   CPU
 * </pre>
 *
 * The reality of this is we have a list of
 * {@link Trait#MAC_ADDR},  {@link Trait#INTERFACE_MODULE}, {@link Trait#INTERFACE_SLOT}, {@link Trait#INTERFACE_ID}
 * , and {@link Trait#VLAN_ID}, which is enough to fill up an interface list.
 * {@link Trait#INTERFACE_LIST} is the highest valued vector for complete AFT
 * data.
 *
 * Not, this is where {@link Trait#MANAGEMENT_INTERFACE_ENTRY}s are seen.
 *
 */
public class CiscoV15MacAddressTableParse extends CiscoV12MacAddressTableParse {

    /**
     * This key is seen in the {@link #VLAN_COL} column.
     *
     * Key for VLAN's, usually for management services, which is accessible to
     * all VLANS. This wont parse. We call this VLAN -1 since vlans are usually
     * unsigned and we still need to be able to compare it to other VLAN ids.
     */
    public static final String VLAN_ALL_KEY = "ALL";
    /**
     * This key is seen in the {@link #INTERFACE_COL} column.
     *
     * It means that the associated MAC address is a special service address and
     * it not an actual port MAC address. It is replaced with the special
     */
    public static final String CPU_KEY = "CPU";

    private final String PARSER_NAME = "MAT v15 parser";
    private final int SKIP_LINES = 2;
    private final int VLAN_COL = 0;
    private final int MAC_COL = 1;
    private final int INTERFACE_COL = 3;

    @Override
    public boolean apply(CiscoReader r, File f, PhysicalNode m) {
        return super.apply(r, f, m);
    }

    @Override
    protected void eachRow(String ifaText, String vlanText, String macText, PhysicalNode data) {

        boolean isPhysical = valid(vlanText);
        
        Mac mac = new Mac(macText);
        
        if( isPhysical ) {
            Vlan vlan = data.getVlan( valid(vlanText) ? Integer.valueOf(vlanText) : -1  );
            Port p = data.getPort( new Port(data, ifaText, Mac.MissingMac) );
            
            if( p.mac.isMissing() ) {
                System.out.println( p );
            }
            
            p.add(mac);
            vlan.ports.add(p);
            vlan.macs.add(mac);
            data.vlans.add(vlan);
            data.ports.add(p);
        }
        
        data.macs.add(mac);
    }
    
    /**
     * Tests if a string is a valid vlan id 
     * @param s String to test.
     * @return True is the first character is non-alpha, else false.
     */
    private boolean valid(String s) {
        return !Character.isAlphabetic(s.charAt(0));
    }

    @Override
    protected String getParserName() {
        return PARSER_NAME;
    }

    @Override
    protected int getSkipLines() {
        return SKIP_LINES;
    }

    @Override
    protected int getVlanColumn() {
        return VLAN_COL;
    }

    @Override
    protected int getMacColumn() {
        return MAC_COL;
    }

    @Override
    protected int getInterfaceColumn() {
        return INTERFACE_COL;
    }

}
