/*
 *  Copyright (C) 2011, 2012
 *  This file is part of GRASSMARLIN.
 */
package core.topology;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.lang3.ArrayUtils;

/**
 * Port name. A ports are physically addressed.
 */
public class Port extends Entities.BasicEntity {

    public static final String UNKNOWN_TYPE = "";
    public Mac mac;
    public final PhysicalNode owner;
    /* a port is reachable by one or many vlans */
    public final Set<Vlan> vlans;
    /* macs reachable by this port */
    private final Set<Mac> macs;
    /* can be concidered the same as vlans in most cases */
    private final Set<Integer> subInterfaces;
    /* string for the name of this Port */
    public final String name;
    /* the module sequence following the name */
    public final Integer[] config;

    public Port(PhysicalNode owner, String text, Mac mac) {
        super();
        this.name = parseName(text);
        this.config = parseConfig(text);
        this.vlans = new HashSet<>();
        this.macs = new HashSet<>();
        this.subInterfaces = new HashSet<>();
        this.owner = owner;
        this.mac = mac;
        this.setName(Port.getPrefferedText(text));
        this.setPrefferedName(text);
    }

    public Set<Vlan> getVlans() {
        return owner.getVlans(this);
    }

    public boolean addVlan(Vlan vlan) {
        return this.vlans.add(vlan);
    }

    public Mac getMac() {
        return mac;
    }

    public void setMac(Mac mac) {
        this.mac = mac;
    }

    public boolean add(Mac mac) {
        if (this.mac.equals(mac)) {
            return false;
        }
        return macs.add(mac);
    }

    public Set<Mac> macs() {
        return macs;
    }

    public String getDisplayText() {
        return String.format("SwitchPort: %s, Vendor: %s, Hardware Address: %s, VLAN: %s", getName(), getVendor(), mac, vlanString());
    }

    public String vlanString() {
        return this.getVlans().stream().map(v -> v.id).collect(Collectors.toSet()).toString().replaceAll("\\]|\\[", "");
    }

    public void addSubInterface(Integer subInterface) {
        this.subInterfaces.add(subInterface);
    }

    @Override
    public String toString() {
        return getName();/*String.format("%s-Port(%s)",
                this.getName(),
                this.mac.toString()
                );*/
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Port other = (Port) obj;
        return other.equalsConfig(this);
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 23 * hash + Arrays.hashCode(config);
        hash = 23 * hash + Objects.hashCode(this.owner);
        hash = 23 * hash + Objects.hashCode(this.name);
        return hash;
    }

    public boolean equalsConfig(Port other) {
        return Arrays.equals(this.config, other.config) && this.name.equalsIgnoreCase(other.name);
    }

    private static String parseName(String text) {
        int pos = getSplitIndex(text);
        if (pos == -1) {
            return "";
        }
        return text.substring(0, pos);
    }

    private static Integer[] parseConfig(String text) {
        int pos = getSplitIndex(text);
        if (pos == -1) {
            return ArrayUtils.EMPTY_INTEGER_OBJECT_ARRAY;
        }
        String[] parts = text.substring(pos).split("\\D+");
        Integer[] ints = new Integer[parts.length];

        for (int i = 0; i < parts.length; i++) {
            ints[i] = Integer.valueOf(parts[i]);
        }
        return ints;
    }

    private static int getSplitIndex(String text) {
        int pos = -1;
        for (int i = text.length() - 1; i > -1; --i) {
            char c = text.charAt(i);
            if (Character.isAlphabetic(c)) {
                pos = i + 1;
                break;
            }
        }
        return pos;
    }

    public static String getPrefferedText(String text) {
        return text.replaceAll("[a-z]+", "");
    }

}
