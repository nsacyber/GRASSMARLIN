/*
 *  Copyright (C) 2011, 2012
 *  This file is part of GRASSMARLIN.
 */
package core.topology;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

/**
 * The VLAN within a switch.
 */
public class Vlan implements Serializable {
    
    public final int id;
    public final Set<Port> ports;
    public final Set<Mac> macs;
    
    public Vlan(int id) {
        this.ports = new HashSet<>();
        this.macs = new HashSet<>();
        this.id = id;
    }
    
    @Override
    public String toString() {
        return String.format("VLAN(%d) : ports(%d)",
                this.id,
                this.ports.size()
        );
    }
    
    @Override
    public int hashCode() {
        return this.id;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Vlan other = (Vlan) obj;
        if (this.id != other.id) {
            return false;
        }
        return true;
    }
    
    
    
}
