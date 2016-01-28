/*
 *  Copyright (C) 2011, 2012
 *  This file is part of GRASSMARLIN.
 */
package core.topology;

import core.importmodule.Trait;
import core.importmodule.TraitMap;

/**
 *
 */
public class AbstractInterface extends Entities.BasicEntity<AbstractInterface> {

    public final String HARDWARE_KEY = "dev.if.hardware";
    public final String MODULE_KEY = "dev.if.module";
    public final String SLOT_KEY = "dev.if.slot";
    public final String ID_KEY = "dev.if.id";
    public final String MAC_KEY = "dev.if.mac";

    IfaEx conf;
    
    public AbstractInterface() {

    }

    public void setMac(Mac mac) {
        this.set(MAC_KEY, mac);
    }

    public Mac getMac() {
        return (Mac) this.get(MAC_KEY);
    }

    public void setConf(IfaEx conf) {
        this.conf = conf;
    }
    
    public void setHardware(String hardware) {
        this.set(HARDWARE_KEY, this);
    }

    public String getHardware() {
        return (String) this.get(HARDWARE_KEY);
    }

    public IfaEx getConf() {
        return conf;
    }
    
    public static AbstractInterface fromMap(TraitMap map) {
        AbstractInterface ai = new AbstractInterface();

        String hardware = map.getString(Trait.HARDWARE);
        String name = map.getString(Trait.INTERFACE_NAME);
        Integer sub = map.getInteger(Trait.SUB_INTERFACE_ID, IfaEx.UNKNOWN);
        Integer id = map.getInteger(Trait.INTERFACE_ID, IfaEx.UNKNOWN);
        Integer slot = map.getInteger(Trait.INTERFACE_SLOT, IfaEx.UNKNOWN);
        Integer module = map.getInteger(Trait.INTERFACE_MODULE, IfaEx.UNKNOWN);

        Long l = map.getLong(Trait.MAC_ADDR);
//        if( l == null ) {
//            System.err.println("shit");
//        }
//        Mac mac = new Mac( l );
        
//        IfaEx conf = new IfaEx(module, slot, id, sub);

//        ai.setHardware(hardware);
//        ai.setName(name);
//        ai.setConf(conf);
//        ai.setMac(mac);
        
        return ai;
    }

    @Override
    public String toString() {
        return String.format("%s%s{%s}", this.getName(), this.getConf(), this.getMac());
    }
}
