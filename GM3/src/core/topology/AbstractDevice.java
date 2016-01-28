/*
 *  Copyright (C) 2011, 2012
 *  This file is part of GRASSMARLIN.
 */
package core.topology;

import core.importmodule.Trait;
import core.importmodule.TraitMap;
import java.util.List;
import org.apache.commons.lang3.RandomStringUtils;

/**
 * Class represents a collection of network interfaces. A device can be a switch
 * if it contains many interfaces and Vlans or it may be a router or host if it
 * contains a single interface.
 */
public class AbstractDevice extends Entities.BasicEntity<AbstractDevice> {

    private TraitMap sourceData;

    AbstractDevice() {
    }

    public AbstractDevice setSourceData(TraitMap sourceData) {
        this.sourceData = sourceData;
        return this;
    }

    public TraitMap getSourceData() {
        return sourceData;
    }

    public static AbstractDevice fromMap(TraitMap map) {
        AbstractDevice dev = new AbstractDevice();
        TraitMap populateMap = new TraitMap(map);
        dev.setSourceData(map);
        putAllInterfaces(dev, populateMap.getList(Trait.INTERFACE_LIST));
        setDeviceName(dev, populateMap.remove(Trait.HOST_NAME));
        return dev;
    }

    private static void putAllInterfaces(AbstractDevice dev, List<TraitMap> interfaces) {
        interfaces.forEach(entry
                -> putInterface(dev, entry)
        );
    }

    /**
     * Expects a TraitMap with an {@link Trait#INTERFACE_LIST_ENTRY} or
     * {@link Trait#MANAGEMENT_INTERFACE_ENTRY} to create an {@link Interface }
     * object from.
     *
     * @param dev Device being constructed.
     * @param iface Map containing construction data.
     */
    private static void putInterface(AbstractDevice dev, TraitMap iface) {
        /* True physical switch-ports. */
        Trait normal = Trait.INTERFACE_LIST_ENTRY;
        /* Virtual management ports used as the source of MID protocols. */
        Trait special = Trait.MANAGEMENT_INTERFACE_ENTRY;
        if (iface.is(normal)) {
//            AbstractInterface ai = AbstractInterface.fromMap(iface);
//            System.out.println(iface);
//            System.out.println(ai);
            
        } else if (iface.is(special)) {

        } else {
            throw new java.lang.IllegalArgumentException(String.format("Error, expected '%s' found '%s'.", normal, iface.getIdentity()));
        }
    }

    /**
     * Sets the {@link #setName(java.lang.String) } field of a device.
     *
     * @param dev Device being constructed.
     * @param map Map containing construction data.
     */
    private static void setDeviceName(AbstractDevice dev, Object name) {
        String host;
        if (name == null) {
            host = AbstractDevice.randomName();
        } else {
            host = (String) name;
        }
        dev.setName(host);
        dev.setPrefferedName(host);
    }

    private static String randomName() {
        String s = RandomStringUtils.random(8, true, true);
        System.err.printf("A device name was chosen for %s because {{HOST_NAME}} was missing.\n", s);
        return s;
    }

    /**
     * Devices have host names, this is a proxy for {@link #getName() }.
     *
     * @return value of {@link #getName() }.
     */
    public String getHostname() {
        return this.getName();
    }

}
