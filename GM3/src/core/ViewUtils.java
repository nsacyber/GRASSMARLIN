/*
 *  Copyright (C) 2011, 2012
 *  This file is part of GRASSMARLIN.
 */
package core;

import ICSDefines.Category;
import ICSDefines.PrettyPrintEnum;
import ICSDefines.Role;
import core.knowledgebase.KnowledgeBase;
import core.topology.Ip;
import core.types.ByteTreeItem;
import java.awt.Image;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.SwingUtilities;
import org.apache.commons.lang3.ArrayUtils;
import ui.custom.ProxyImage;
import ui.dialog.SubnetMaskDialog;
import ui.dialog.detaileditor.DetailEditorDialog;
import ui.icon.Icons;
import ui.dialog.ConnectionDialog;
import ui.views.tree.visualnode.HostVisualNode;
import ui.views.tree.visualnode.VisualNode;

/**
 *
 */
public class ViewUtils {

    public static final int[] LARGE_LAN_MASK = {255, 255, 255, 0};
    public static final int LARGE_LAN_CIDR = 24;
    public static final int LARGE_LAN_HASH = -256;
    public static final String WARNING_PROPERTY = "WARNING";

    public static void showSubnetEditor(VisualNode n, BiConsumer<int[], int[]> cb) {
        SubnetMaskDialog smd = new SubnetMaskDialog(cb, n.getSubnet(), ViewUtils.ipString(n.hashCode()));
        smd.setCloseOnCallback(true);
        smd.setAlwaysOnTop(true);
        smd.setVisible(true);
    }
    
    public static void setupCategoryMenu(JMenu menu, VisualNode node) {
        setupMenu( menu, node, ICSDefines.Category.values(), ViewUtils::setCategory );
    }
    
    public static void setupRoleMenu(JMenu menu, VisualNode node) {
        setupMenu( menu, node, ICSDefines.Role.values(), ViewUtils::setCategory );
    }
    
    private static void setupMenu(JMenu menu, VisualNode node, PrettyPrintEnum[] values, BiConsumer<VisualNode, Enum> applyFunction) {
        for (int i = 0; i < values.length; ++i) {
            PrettyPrintEnum value = values[i];
            String text = value.getPrettyPrint();
            JMenuItem item = new JMenuItem(text);
            item.addActionListener(e -> {
                if (node != null) {
                    applyFunction.accept(node, value.get());
                }
            });
            menu.add(item);
        }
    }

    private static void setCategory(VisualNode node, Enum enumValue) {
        if( node.hasDetails() ) {
            ICSDefines.Category category = (Category) enumValue;
            node.getDetails().setCategory(category);
            node.getDetails().image.setIcon(Icons.valueOf(category.iconValue));
        }
    }

    private static void setRole(VisualNode node, Enum enumValue) {
        node.getDetails().setRole((Role) enumValue);
    }
    
    public static int[] ipToInts(String ip) {
        int[] ints = ArrayUtils.EMPTY_INT_ARRAY;
        String[] parts = ip.split("\\D+"); // split on all non-numeral  chars
        final int len = parts.length;
        ints = new int[len];
        for( int i = 0; i < len; i++ ) {
            String part = parts[i];
            if( !part.isEmpty() ) {
                ints[i] = Integer.valueOf(part);
            }
        }
        return ints;
    }
    
    /**
     * Runs the geoip-lookup in the {@link KnowledgeBase} parameter.
     * @param kb KnowledgeBase used to lookup Country name and flag.
     * @param node Node to set the {@link VisualNode#getDetails()} for the return value.
     * @param onSuccess Callback to accept the node on success.
     */
    public static void runGeoIp(KnowledgeBase kb, VisualNode node, Consumer<VisualNode> onSuccess) {
        if( node.hasDetails() ) {
            SwingUtilities.invokeLater(()->{
                String ip = node.getAddress();
                ProxyImage image = node.getDetails().image;
                String country = kb.getCountryNameSync(ip);
                if( country != null ) {
                    node.getDetails().setCountry(country);
                }

                String vendorOUI = kb.getOuiSync(node.getHardwareOui());
                node.getDetails().setHardwareVendor(vendorOUI);
                
//                String vendorType = kb.get ???
                
                Image icon = kb.getCountryFlagSync(ip);
                if( icon != null ) {
                    image.setFlag(icon, country);
                }
                onSuccess.accept(node);
            });
        }
    }
    
    /**
     * Copies all Object members into the Details 'common' map.
     * @param n Node to have Details updated.
     */
    public static void prepareDetails(VisualNode n) {
//        Map<String,Set<String>> common = n.getDetails().getCommon();
//        put(common, "Name", n.getDisplayText());
//        put(common, "Comments", "");
//        put(common, "Subnet Mask", n.getSubnet());
//        if( n.isHost() ) {
//            put(common, "Confidence", n.getDetails().getConfidence());
//            put(common, "Category", n.getDetails().getCategory());
//            put(common, "Role", n.getDetails().getRole());
//            put(common, "# of Connections", n.getChildCount());
//            put(common, "NIC OUI", n.getDetails().getOUI());
//            put(common, "NIC Vendor", n.vendor);
//            put(common, "NIC Vendor Type", "");
//            put(common, "Hardware Vendor", n.vendor);
//            put(common, "Primary Address", n.getIp());
//            put(common, "Number of Interfaces", 1); // this field is leftover from 2.0's merge of both trees
//            n.getDetails().eachExtract( (k, v) -> {
//                put(common, k, v);
//            });
    //        put(common, "Hardware", null);
    //        put(common, "Serial Number", "");
    //        put(common, "Location", null);
    //        put(common, "OS Vendor", null);
    //        put(common, "OS Version", null);
//        }
    }

    private static void put( Map<String,Set<String>> common, String key, Object value ) {
        if( value != null ) {
            Set<String> set = common.get(key);
            if( set == null ) {
                set = new HashSet<>();
                common.put( key, set );
            }
            if( set.contains(value.toString()) ) {

            } else {
                set.clear();
                set.add(value.toString());
            }
        }
    }
    
    /**
     * Creates a list of ByteTreeItems from a TreeViewNode of a network.
     *
     * @param network Network to create the list from.
     * @return List of ByteTreeItems or empty is the TreeViewNode was not a
     * network.
     */
    public static List<ByteTreeItem> allNodes(VisualNode network) {
        if (!network.isNetwork()) {
            return new ArrayList<>();
        }
        return network.getChildren().stream()
                .map(VisualNode::getData)
                .collect(Collectors.toList());
    }

    /**
     * Creates a 4 byte bit-mask based on the amount of bits provided shifted to
     * MSB
     *
     * @param bits Bits to mask from the MSB
     * @return byte[] byte mask.
     */
    public static int[] createMSBMask(int bits) {
        int m = 0, orig = 32 - bits;
        while (bits-- > 0) {
            m |= 1;
            m <<= 1;
        }
        while (--orig > 0) {
            m <<= 1;
        }
        return new int[]{(m >> 24) & 0xFF, (m >> 16) & 0xFF, (m >> 8) & 0xFF, m & 0xFF};
    }
    
    public static int[] intsFromString(String ip) {
        int[] ints = new int[4];
        String[] strs = ip.trim().split("\\D+");
        int len = strs.length < 4 ? strs.length : 4;
        for( int i = 0; i < len; ++i ) {
            ints[i] = Integer.valueOf(strs[i]);
        }
        return ints;
    }
    
    public static int parseIp(String ip) {
        return Ip.parse(ip);
    }

    public static String subnetMask(int bits) {
        return ipString(createMSBMask(bits), null);
    }

    public static Integer intHash(ByteTreeItem n, int treeDepth) {
        return intFromBytes(getBytes(n, treeDepth));
    }

    public static Integer intHash(int[] ints) {
        return (ints[0] << 24) + (ints[1] << 16) + (ints[2] << 8) + ints[3];
    }

    /**
     * Converts each byte in an integer to an integer, places them in MSB order.
     *
     * @param val Integer to separate.
     * @return Array of shifted bytes as an int[].
     */
    public static int[] toInts(int val) {
        int[] ints = new int[4];
        for (int i = 0; i < 4; i++) {
            ints[3 - i] = 0xFF & val;
            val >>= 8;
        }
        return ints;
    }

    /**
     * Prints a int[] as a string separated by ".", if the string is to short
     * the default is "0.0.0.0"
     *
     * @param b int[] to print the first four indexes of.
     * @return The formatted string or default value.
     */
    public static String intsToString(int[] b) {
        if (b.length < 4) {
            return "0.0.0.0";
        }
        return String.format("%d.%d.%d.%d", b[0], b[1], b[2], b[3]);
    }

    public static byte[] getBytes(ByteTreeItem n, int treeDepth) {
        byte[] bytes = {0, 0, 0, 0};
        while (n.hasParent()) {
            bytes[treeDepth--] = n.getValue();
            n = n.getParent();
        }
        return bytes;
    }

    /**
     * Creates a hash and IP string of the network of the given Terminal node by
     * its details.mask CIDR
     *
     * @param item Item to find network IP-String and IP-Hash of.
     * @param cb Callback to accept the IP-String and IP-Hash in that order
     * @return The network's IP-hash, null if the item argument is not terminal.
     */
    public static Integer networkOf(ByteTreeItem item, BiConsumer<String, Integer> cb) {
        if (!item.isTerminal()) {
            return null;
        }

        if( item.hasNetworkId() ) {
            Integer id = item.getNetworkId();
            String ipString = ViewUtils.ipString(id);
            cb.accept(ipString, id);
            return id;
        }
        
        if (item.details.hasExplicitNetwork()) {
            if (cb != null) {
                String ipString = ViewUtils.ipString(item.details.getNetworkId());
                cb.accept(ipString, item.details.getNetworkId());
            }
            return item.details.getNetworkId();
        }

        int[] mask = item.details.cidr() == 24 ? LARGE_LAN_MASK : createMSBMask(item.details.cidr());
        byte[] bytes = getBytes(item, 3);

        for (int i = 0; i < 4; i++) {
            bytes[i] &= mask[i];
        }

        Integer i = intFromBytes(bytes);
        if (cb != null) {
            cb.accept(ipString(i), i);
        }
        return i;
    }

    public static String ipString(int intVal) {
        return ipString(bytesFromInt(intVal), null);
    }

    public static String ipString(int intVal, Integer CIDR) {
        return ipString(bytesFromInt(intVal), CIDR);
    }

    public static String ipString(ByteTreeItem b) {
        return ipString(getBytes(b, b.depth()), null);
    }

    public static String ipString(byte[] b, Integer CIDR) {
        StringBuilder sb = new StringBuilder();
        sb.append(b[0] & 0xFF);
        sb.append('.');
        sb.append(b[1] & 0xFF);
        sb.append('.');
        sb.append(b[2] & 0xFF);
        sb.append('.');
        sb.append(b[3] & 0xFF);
        if (CIDR != null) {
            sb.append('/');
            sb.append(CIDR);
        }
        return sb.toString();
    }
    
    public static String ipString(int[] b, Integer CIDR) {
        StringBuilder sb = new StringBuilder();
        sb.append(b[0] & 0xFF);
        sb.append('.');
        sb.append(b[1] & 0xFF);
        sb.append('.');
        sb.append(b[2] & 0xFF);
        sb.append('.');
        sb.append(b[3] & 0xFF);
        if (CIDR != null) {
            sb.append('/');
            sb.append(CIDR);
        }
        return sb.toString();
    }

    public static int intFromBytes(byte[] b) {
        return (b[0] << 24) | (b[1] & 0xFF) << 16 | (b[2] & 0xFF) << 8 | (b[3] & 0xFF);
    }

    public static byte[] bytesFromInt(int intVal) {
        return ByteBuffer.allocate(4).putInt(intVal).array();
    }

    public static String proto(Integer proto) {
        switch (proto) {
            case 6:
                return "TCP";
            case 17:
                return "UDP";
            default:
                return "?";
        }
    }

    public static boolean isRoutableIP(int ip) {
        if (ip >= 167772160 && ip <= 184549375) {
            return false; // CLASS A between 10.0.0.0 & 10.255.255.255
        }
        if (ip >= -1408237568 && ip <= -1407123457) {
            return false; // CLASS B between 172.16.0.0 & 172.32.255.255
        }
        if (ip >= -1062731776 && ip <= -1062666241) {
            return false; // CLASS C between 192.168.0.0 & 192.168.255.255
        }
        if (ip >= -268435456 && ip <= -251658241) {
            return false; // CLASS E (RESEARCH) between 240.0.0.0 & 240.255.255.255
        }
        return true;
    }

    public static void showDetailEditor(VisualNode n, Consumer<VisualNode> onChange) {
        SwingUtilities.invokeLater(()->{
            DetailEditorDialog detailsDialog = new DetailEditorDialog( n, onChange );
        });
    }

    public static void showConnectionDialog(VisualNode node) {
        if( node.isHost() ) {
            ConnectionDialog dialog = new ConnectionDialog((HostVisualNode) node);
            dialog.setVisible(true);
        }
    }
    
    public static Long getMAC_ADDR(String s) {
        Long hash = 0L;
        s = s.replaceAll("[^\\p{XDigit}]", "");
        for (int i = 0; i < s.length(); i++) {
            byte b = (byte) Character.digit(s.charAt(i), 16);
            hash <<= 4;
            hash += b;
        }
        return hash;
    }

    public static String getMACString(long l) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 6; i++) {
            byte b = (byte) l;
            sb.insert(0, Integer.toHexString(b & 0b00001111).substring(0, 1));
            sb.insert(0, Integer.toHexString(b & 0b11110000).substring(0, 1));
            sb.insert(0, '-');
            l >>= 8;
        }
        return sb.toString().substring(1);
    }

    public static boolean isMACMulticast(long l) {
        return ((byte) (l >> 40) & 0b00000001) == 1;
    }

    public static boolean isMACUnicast(long l) {
        return !isMACMulticast(l);
    }

    public static boolean isMACLocallyAdministered(long l) {
        return ((byte) (l >> 40) & 0b00000010) == 2;
    }

    public static boolean isMACGUID(long l) {
        return !isMACLocallyAdministered(l);
    }

    /**
     * Gets CIDR from an IP hash. Inverts, and counts high bits.
     *
     * @param hash IP hash to count cidr bits from.
     * @return cidr, will zero if MSB is zero.
     */
    public static int getCIDR(int hash) {
        int cidr = 0;
        hash = ~hash;
        while ((hash & 0b1) == 1) {
            cidr++;
            hash >>= 1;
        }
        return 32 - cidr;
    }

    public static int sort(final VisualNode n1, final VisualNode n2) {
        int i1 = n1.hashCode();
        int i2 = n2.hashCode();
        return octetSort(i1, i2);
    }

    /**
     * Calculates the sort order of two ints by their MSB, so 255.0.0.0 is a
     * higher number than 0.0.0.0 even though 255.0.0.0 has the a high sign bit.
     *
     * @param i1 left int to compare.
     * @param i2 right int to compare.
     * @return 1 if i2 is greater than i1 else -1 if i2 is less than i1, else 0
     * if equal.
     */
    public static int octetSort(int i1, int i2) {
       int[] ints1 = ViewUtils.toInts(i1);
       int[] ints2 = ViewUtils.toInts(i2);
       for( int i = 0; i < 4; i++ ) {
           if( ints1[i] < ints2[i] ) {
               return -1;
           }
       }
       return 0;
    }

    /**
     * Create an ip string followed by a CIDR
     *
     * @param hash ip hash to generate string and CIDR from
     * @return Network formatted string in the format 0.0.0.0/0
     */
    public static String networkString(int hash) {
        return String.format("%s/%d", ipString(hash), getCIDR(hash));
    }
}
