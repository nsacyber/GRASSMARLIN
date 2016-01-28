/*
 *  Copyright (C) 2011, 2012
 *  This file is part of GRASSMARLIN.
 */
package core.topology;

/**
 *
 */
public class VlanEx {
    private int id;
    private Ip ip;
    private Mac mac;
    
    public VlanEx(String id) {
        this(Integer.valueOf(id));
    }
    
    public VlanEx(int id) {
        this.id = id;
    }
    
    public void setIp(Ip ip) {
        this.ip = ip;
    }
    
    public Ip getIp() {
        return ip;
    }
    
    public int getId() {
        return id;
    }
    
    public boolean equalsId( VlanEx other ) {
        return equalsId( other.id );
    }
    
    public boolean equalsId( int id ) {
        return this.id == id;
    }

    public void setMac(Mac mac) {
        this.mac = mac;
    }
    
    public Mac getMac() {
        return mac;
    }
    
    @Override
    public String toString() {
        return String.format("%d", id);
    }
    
}
