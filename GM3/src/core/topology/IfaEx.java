/*
 *  Copyright (C) 2011, 2012
 *  This file is part of GRASSMARLIN.
 */
package core.topology;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import org.apache.commons.lang3.ArrayUtils;

/**
 * Representative switch-interface object.
 *
 * Maintains the canonical sequence of integers that specify the physical
 * configuration of an interface or sub-interface within a switch.
 */
public class IfaEx extends Entities.BasicEntity<IfaEx> {

    final int MODULE_INDEX = 0;
    final int SLOT_INDEX = 1;
    final int INTERFACE_INDEX = 2;

    public static final int UNKNOWN = -1;
    /**
     * Text displayed when the parser cannot record a valid format.
     */
    public static final String INVALID_NAME = "Invalid";

    Integer[] config;
    String name;
    Mac mac;
    Ip ip;
    private boolean isCandidate;
    Set<Integer> subInterfaces;
    Set<Integer> vlans;

    private IfaEx() {
        config = new Integer[]{UNKNOWN};
        subInterfaces = new HashSet<>();
        vlans = new HashSet<>();
        isCandidate = true;
    }

    public IfaEx(String ifaText) {
        this(IfaEx.parseName(ifaText), IfaEx.parseConfig(ifaText));
    }

    public IfaEx(String name, int... ints) {
        this();
        this.name = name;
        this.set(ints);
    }

    private void set(int[] ints) {
        int len = ints.length;
        if (len == 2) {
            config = new Integer[3];
            config[MODULE_INDEX] = UNKNOWN;
            config[SLOT_INDEX] = ints[0];
            config[INTERFACE_INDEX] = ints[1];
        } else if (len >= 3) {
            config = new Integer[3];
            config[MODULE_INDEX] = ints[0];
            config[SLOT_INDEX] = ints[1];
            config[INTERFACE_INDEX] = ints[2];
            for( int i = 3; i < ints.length; ++i  ) {
                this.addSubInterface(ints[i]);
            }
        } else {
        }
    }

    public IfaEx setMac(Mac mac) {
        this.mac = mac;
        return this;
    }

    public IfaEx setIp(Ip ip) {
        this.ip = ip;
        return this;
    }

    private String str(int i) {
        String s = "";
        int val = config[i];
        if (val != UNKNOWN) {
            s = Integer.toString(val);
            if ( i < config.length-1 ) {
                s = s.concat("/");
            }
        }
        return s;
    }

    public boolean missingMac() {
        return mac == null;
    }

    public boolean equalsConfig(IfaEx other) {
        return Arrays.equals(this.config, other.config) && this.name.equalsIgnoreCase(other.name);
    }

    public boolean isCandidate() {
        return isCandidate;
    }

    public void setCadidate(boolean isCandidate) {
        this.isCandidate = isCandidate;
    }

    public void addSubInterface(Integer ifa) {
        this.subInterfaces.add(ifa);
    }

    private String subInterfaces() {
        if (this.subInterfaces.isEmpty()) {
            return "";
        } else {
            return subInterfaces.toString();
        }
    }

    @Override
    public String toString() {
        return String.format("%s%s%s%s%s", this.name, str(MODULE_INDEX), str(SLOT_INDEX), str(INTERFACE_INDEX), subInterfaces());
    }

    public static String parseName(String text) {
        int pos = getSplitIndex(text);
        if (pos == -1) {
            return INVALID_NAME;
        }
        return text.substring(0, pos);
    }

    public static int[] parseConfig(String text) {
        int pos = getSplitIndex(text);
        if (pos == -1) {
            return ArrayUtils.EMPTY_INT_ARRAY;
        }
        String[] parts = text.substring(pos).split("\\D+");
        int[] ints = new int[parts.length];

        for (int i = 0; i < parts.length; i++) {
            ints[i] = Integer.valueOf(parts[i]);
        }
        return ints;
    }

    public static int getSplitIndex(String text) {
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

    public void addVlan(Integer vlanId) {
        vlans.add(vlanId);
    }

}
