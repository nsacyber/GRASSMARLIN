package core.importmodule.parser.cisco;

import core.topology.Mac;
import core.topology.PhysicalNode;
import core.topology.Port;
import core.topology.Vlan;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Mac Address Table (MAT) parser for V12 commands
 */
public class CiscoV12MacAddressTableParse implements SubParser {

    public static boolean debug = false;

    /**
     * There is one line of "----" to skip before the header ends
     */
    private final int SKIP_LINES = 1;
    private final String SKIP_TOKEN = "----";
    private final int VLAN_COL = 2;
    private final int MAC_COL = 0;
    private final int INTERFACE_COL = 3;
    private final String PARSER_NAME = "MAT V12 parser";
    private final int MAX_COLUMN_COUNT = 4;

    @Override
    public boolean apply(CiscoReader r, File f, PhysicalNode data) {
        if (debug) {
            System.out.println(this.getParserName() + " started " + f.getName());
        }

//        List<TraitMap> interfaces = m.getOrCreateList(Trait.INTERFACE_LIST);

//        int initialSize = interfaces.size();

        try (BufferedReader reader = new BufferedReader(new FileReader(f))) {

            skipAhead(reader);

            String line;
            while ((line = reader.readLine()) != null) {

                String[] row = line.trim().split("\\s+");

                if (row.length != getMaxColumnCount()) {
                    continue;
                }
                
                eachRow( row[getInterfaceColumn()], row[getVlanColumn()], row[getMacColumn()], data);
                
            }

        } catch (IOException ex) {
            Logger.getLogger(CiscoV15MacAddressTableParse.class.getName()).log(Level.SEVERE, null, ex);
        }
        
//        if( debug ) {
//            int diff = interfaces.size() - initialSize;
//            System.out.println(String.format("%s parsed %d rows",this.getParserName(), diff));
//        }
        
//        return initialSize < interfaces.size();
        return true;
    }

    protected void eachRow( String ifaText, String vlanText, String macText, PhysicalNode data ) {
        Port p = parseInterface(ifaText, data);
        Mac mac = parseMac(macText, data);
        Vlan vlan = parseVlan(vlanText, data);

        data.macs.add(mac);

        if( p != null && vlan != null ) {
            p.setMac(mac);
            p.add(mac);
            p.vlans.add(vlan);
            vlan.ports.add(p);
            vlan.macs.add(mac);
            data.ports.add(p);
        }
    }
    
    protected Port parseInterface(String iface, PhysicalNode data) {
        return data.getPort( new Port(data, iface, Mac.MissingMac) );
    }

    protected Vlan parseVlan(String vlan, PhysicalNode data) {
        return data.getVlan(Integer.valueOf(vlan));
    }

    protected Mac parseMac(String macText, PhysicalNode data) {
        Mac mac = new Mac(macText);
        data.macs.add(mac);
        return mac;
    }

    protected int getMaxColumnCount() {
        return MAX_COLUMN_COUNT;
    }

    protected String getParserName() {
        return this.PARSER_NAME;
    }

    protected int getVlanColumn() {
        return this.VLAN_COL;
    }

    protected int getMacColumn() {
        return this.MAC_COL;
    }

    protected int getInterfaceColumn() {
        return this.INTERFACE_COL;
    }

    protected int getSkipLines() {
        return SKIP_LINES;
    }

    protected String getSkipToken() {
        return SKIP_TOKEN;
    }

    /**
     * Skips ahead of the table's header.
     *
     * @param reader BufferedReader of the input file.
     */
    protected void skipAhead(BufferedReader reader) {
        String skipToken = getSkipToken();
        String line;
        int lines = getSkipLines();
        try {
            while ((line = reader.readLine()) != null) {
                if (line.startsWith(skipToken)) {
                    if (--lines == 0) {
                        return;
                    }
                }
            }
        } catch (IOException ex) {
            Logger.getLogger(CiscoV15MacAddressTableParse.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

}
