/*
 *  Copyright (C) 2011, 2012
 *  This file is part of GRASSMARLIN.
 */
package core.topology;

import java.util.function.Function;

/**
 * An Interface, represents a NIC or Switch-port ({@link core.topology.Port}) is a TopologyEntity with a single L2 MAC address and L3 IP address
 */
public class Interface extends Entities.BasicEntity<Interface> {
    
    static final String PORT_FIELD = "Interface.port";
    static final String PORT_ID_FIELD = "Interface.port.number";
    static final String MAC_FIELD = "Interface.mac";
    static final String IP_FIELD = "Interface.ip";
    
    Function<Interface,String> displayTextFunction;
        
    Interface(Port port, Mac mac, Ip ip, Function<Interface, String> displayTextFunctioon) {
        super(port);
        if( port != null ) {
            set(PORT_FIELD, port);
            set(PORT_ID_FIELD, port.config);
        }
        set(MAC_FIELD, mac);
        set(IP_FIELD, ip);
        String displayText;
        if( ip != null ) {
            displayText = ip.toString();
        } else {
            displayText = port.toString();
        }
        setName(displayText);
        setPrefferedName(displayText);
        this.displayTextFunction = displayTextFunctioon;
    }
    
    public Interface(Port port) {
        this(port,port.mac,null,Interface::portDisplayTextFunction);
    }
    
    public Interface(Mac mac, Ip ip) {
        this(null,mac,ip,Interface::nicDisplayTextFunction);
    }

    public Ip getIp() {
        return (Ip) get(IP_FIELD, Ip.class);
    }
    
    public Mac getMac() {
        return (Mac) get(MAC_FIELD, Mac.class);
    }
    
    public Port getPort() {
        return (Port) get(PORT_FIELD, Port.class);
    }

    public String getDisplayText() {
        return displayTextFunction.apply(this);
    }
    
    @Override
    public Interface setName(String name) {
        super.setName(name);
        return this;
    }
    
    @Override
    public String toString() {
        String string;
        if( canGet(PORT_FIELD) ) {
            Port p = getPort();
            String id = String.format("%s0/%d", p.getName());
            string = String.format("%s %s", id, p.mac );
        } else {
            string = String.format("NIC %s", getMac().toString());
        }
        return string;
    }
    
    /**
     * Pluggable toString for a Port and MAC address.
     * @param iface Interface to generate a string representation for.
     * @return String representing this object.
     */
    public static final String portDisplayTextFunction(Interface iface) {
        String string;
        if( iface.hasSpecifiedName() ) {
            string = String.format("%s %s", iface.getName(), iface.getPort().getDisplayText());
        } else {
            string = iface.getPort().getDisplayText();
        }
        return string;
    }
    
    /**
     * Pluggable toString for a MAC and IP address
     * @param iface Interface to generate a string representation for.
     * @return String representing this object.
     */
    public static final String nicDisplayTextFunction(Interface iface) {
        String string;
        if( iface.hasSpecifiedName() ) {
            string = String.format("NIC: %s, Address: %s, Hardware Address: %s", iface.getName(), iface.getIp(), iface.getMac());
        } else {
            string = String.format("NIC, Address: %s, Hardware Address: %s", iface.getIp(), iface.getMac());
        }
        return string;
    }

}
