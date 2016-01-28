/*
 *  Copyright (C) 2011, 2012
 *  This file is part of GRASSMARLIN.
 */
package core.importmodule;

import core.topology.Ip;
import core.topology.Mac;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

/**
 * <pre>
 * The trait enum links multiple data-entities found in the imported flatfiles (such as Bro logs) with
 * identafiable constructs within grassmarlin.
 *
 * {@link TraitMap} provides a type checked way to populate a safe data-vector with known entities.
 * </pre>
 */
public enum Trait {
    /**
     * The root of the vector, all flatfiles should produce a TraitMap created
     * from a single call to {@link #newRoot() }.
     */
    ROOT(),
    /**
     * {@link List} of {@link TraitMap}s each map MUST contain
     * {@link #PORT_DEPRICATE}, {@link #INTERFACE_NAME}, and {@link #MAC_ADDR}.
     * Required for completed info, {@link #MAC_TABLE} This is a list of
     * interfaces and is only partial information, {@link #SWITCH_INTERFACES}
     * and {@link #MAC_TABLE} make for a complete device data set.
     */
    SWITCH_INTERFACES(List.class),
    /**
     * Differed from an Interface list in that it contains
     * {@link #SUB_INTERFACE_ENTRY}s.
     */
    SUB_INTERFACE_LIST(List.class),
    /**
     * Same as {@link #INTERFACE_LIST_ENTRY} but contains an
     * {@link #SUB_INTERFACE_ID}.
     */
    SUB_INTERFACE_ENTRY(TraitMap.class),
    SUB_INTERFACE_ID(Integer.class, Integer::valueOf),
    INTERFACE_LIST(List.class),
    /**
     * A port is the actual plug on a switch. {@link #INTERFACE_MODULE}
     * (optional) is the module slot the {@link #INTERFACE_SLOT}. AKA
     * motherboard-slot, motherboard-module. {@link #INTERFACE_SLOT} (required)
     * is the hardware NIC this port is mounted on. AKA slot, sub-slot,
     * interface. {@link #INTERFACE_ID} (required) is the number assigned to
     * this port by the switch. AKA port-id, port. {@link #INTERFACE_NAME}
     * (optional) is the type of port, ex. Serial, FastEthernet, etc...
     */
    INTERFACE_LIST_ENTRY(TraitMap.class),
    /**
     * 0/0/0 = {0}/0/0
     */
    INTERFACE_MODULE(Integer.class, Integer::valueOf),
    /**
     * 0/0/0 = 0/{0}/0
     */
    INTERFACE_SLOT(Integer.class, Integer::valueOf),
    /**
     * 0/0/0 = 0/0/{0}
     */
    INTERFACE_ID(Integer.class, Integer::valueOf),
    /**
     * Ethernet0/0/0 = {Ethernet0}/0/0
     */
    INTERFACE_NAME(String.class),
    /**
     * A special equivalent to {@link #INTERFACE_LIST_ENTRY} which is for a
     * virtual management port, not a physical one with a locally administered
     * MAC address.
     *
     * It is essentially the same as INTERFACE_LIST_ENTRY, but does not need to
     * contain a module, slot or ID number.
     *
     * This is a case for {@link TraitMap#convertTo(core.importmodule.Trait) },
     * where Ports that look like normal ports actually appear to be management
     * ports.
     */
    MANAGEMENT_INTERFACE_ENTRY(TraitMap.class),
    /**
     * A IPv4 address.
     */
    IPv4(Integer.class, Ip.Ip4::intHash),
    IPv4_SRC(Integer.class, Ip.Ip4::intHash),
    IPv4_DST(Integer.class, Ip.Ip4::intHash),
    CIDR(Integer.class, Integer::parseInt),
    /* test description of hardware info, such as MV96888 or M100 */
    HARDWARE(String.class),
    DESCRIPTION(String.class),
    PORT_DEPRICATE(Integer.class),
    MAC_TABLE(List.class),
    /**
     * A list of TraitMaps Each requires a
     * {@link #INTERFACE_NAME}, {@link #VLAN_LIST}, and {@link #PORT_DEPRICATE}.
     */
    CISCO_RUNNING_CONFIG(List.class),
    /**
     * Should be a List of TraitMaps with
     * {@link Trait#VLAN} {@link TraitMap#identity}s.
     */
    VLAN_LIST(List.class),
    /**
     * Normally seen with just a {@link #VLAN_ID} and {@link #DESCRIPTION}. Also
     * associated with {@link #IPv4} and {@link #IPv4_NETMASK};
     */
    VLAN(TraitMap.class),
    VLAN_ID(Integer.class, Integer::valueOf),
    /**
     * The configuration of a devices version, This can contain a
     * {@link #VERSION_NO} as version numbers tend to be canonical.
     */
    VERSION_CONFIG(TraitMap.class),
    /**
     * A version number from some device, such as 1.2.4 or 2.0-a1_2133
     */
    VERSION_NO(String.class),
    VERSION_MODEL(String.class),
    VERSION_SERIAL_NO(String.class),
    VERSION_SOFTWARE_IMAGE(String.class),
    /**
     * A list of names of processed commands.
     */
    PROCESSED_COMMANDS(List.class),

    MAC_ADDR(Long.class, Mac::strToLong), // all macs will be automatically converted to longs
    TIMESTAMP(Long.class), // Long
    OFFSET(Integer.class), //Integer
    PAYLOAD(Byte[].class), //Byte[]
    PORT_DST(String.class), //String
    PORT_SRC(String.class), //String
    ETHERTYPE(String.class), //String
    PROTO(String.class), //String
    FRAME_NO(int.class),
    // USERNAME_LIST of the form [ { prop:val } ]
    USERNAME_LIST(List.class),
    USERNAME_ENTRY(TraitMap.class),
    USERNAME(String.class),
    PASSWORD(String.class),
    MANUFACTURER(String.class), // String
    SERIALNUMBER(String.class), // String
    PACKET_SIZE(int.class),
    IPv4_NETMASK(Integer.class, Ip.Ip4::intHash), // int intHash
    DEVICE_NAME(String.class),
    HOST_NAME(String.class),
    // used to indicate either FastEthernet or GigabitEthernet or other names of port-types on switches
    // for a iface name such as "GigabitEthernet0/2" IF_PORT_INFO is the 0/2
    // here is the symantic meaning of the dash notation(x/y/z)
    // x - optional - sometime seen on GigE is the module No.
    // y - always seen on either GigE or FastE and is the slot No.
    // z - always seen on either GigE or FastE and is the port No.
    // we will likely store this as a list of Integers
    // a list of switch ports on this i-face
    // we will likely store this as a list of strings for now
    // interface switchport vlan list
    IF_SP_VLAN_LIST(List.class),
    IF_TUNNEL(String.class),
    // list of String comments
    COMMENT(List.class),
    /**
     * List of strings for each command processed in config files.
     */
    COMMANDS(List.class),
    // a list of List<Map<Trait,Object>> each map contains a PORT_DEPRICATE-MAC_ADDR-VLAN tuple
    // a list of hashmaps containg ARP entyies Proto,IPv4_Addr, Mac_addr, Iface, Port, Port_list
    ARP_TABLE(List.class),
    END;

    static {
        Trait.INTERFACE_LIST.setContents(Trait.INTERFACE_LIST_ENTRY);
        Trait.INTERFACE_LIST_ENTRY.setContents(INTERFACE_ID, INTERFACE_NAME, MAC_ADDR);
    }

    public final Class type;
    private Trait[] contents;
    /**
     * All classes required in a {@link TraitMap} of this type.
     */
    Set<Class> requiredClasses;
    private final Function<String, Object> parseMethod;

    /**
     * Default class to Object.class.
     */
    Trait() {
        this(Object.class, null);
    }

    Trait(Class type) {
        this(type, null);
    }

    /**
     * Constructs a Trait with a type that {@link Class#isAssignableFrom(java.lang.Class)
     * }
     * will check entries into a {@link TraitMap} against.
     *
     * @param type Type to check TraitMap entries against.
     */
    Trait(Class type, Function<String, Object> parseMethod) {
        this.type = type;
        this.parseMethod = parseMethod;
    }

    public Object parse(String string) {
        return this.hasParseMethod() ? this.parseMethod.apply(string) : null;
    }

    /**
     * Set the contents that are checked on each Trait in a TraitMap by the
     * {@link TraitMap#identity}. These traits make sure that it contains all of
     * its required contents.
     *
     * @param contents
     */
    private void setContents(Trait... contents) {
        this.contents = contents;
        Set<Class> requiredClasses = new HashSet<>();
        for( int i = 0; i < contents.length; i++ ) {
            Class c = contents[i].type;
            requiredClasses.add(c);
        }
        this.requiredClasses = Collections.unmodifiableSet(requiredClasses);
    }

    public boolean isSequence() {
        boolean val = this.type.isAssignableFrom(List.class);
        return val;
    }
    
    public boolean isString() {
        return this.type.equals(String.class);
    }

    public boolean isMap() {
        return this.type.equals(TraitMap.class);
    }
    
    public boolean hasParseMethod() {
        return parseMethod != null;
    }

    public boolean canValidate() {
        return this.contents != null;
    }

    /**
     * Constructs a new TraitMap for the root of a data-vector.
     *
     * @return New TraitMap with {@link TraitMap#identity} = {@link Trait#ROOT}.
     */
    public static TraitMap newRoot() {
        return new TraitMap(ROOT);
    }

}
