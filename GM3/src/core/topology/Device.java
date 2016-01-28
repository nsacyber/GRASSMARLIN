/*
 *  Copyright (C) 2011, 2012
 *  This file is part of GRASSMARLIN.
 */
package core.topology;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/**
 * Device is an Entities.BasicEntity for a collection of interfaces.
 */
public class Device extends Entities.BasicEntity<Device> {
    
    public static final String INTERFACE_KEY = "Interfaces";
    
    List<Interface> interfaces;
    
    public Device(Entities entity) {
        super(entity);
        interfaces = new ArrayList<>();
        set(INTERFACE_KEY, interfaces);
    }
    
    public String getDisplayText() {
        String string;
        if( hasSpecifiedName() ) {
            string = String.format("%s: %s, Vendor: %s, Role: %s", getCategory().getText(), getName(), getVendor(), getRole() );
        } else {
            string = String.format("%s, Vendor: %s, Role: %s", getCategory().getText(), getVendor(), getRole() );
        }
        return string;
    }

    public List<Interface> getInterfaces() {
        return interfaces;
    }
    
    public void addInterface(Interface iface) {
        interfaces.add(iface);
    }
    
    public Stream<Interface> streamInterfaces() {
        return interfaces.stream();
    }
    
}
